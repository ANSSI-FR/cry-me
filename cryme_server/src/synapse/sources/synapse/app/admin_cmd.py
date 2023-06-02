#/*************************** The CRY.ME project (2023) *************************************************
# *
# *  This file is part of the CRY.ME project (https://github.com/ANSSI-FR/cry-me).
# *  The project aims at implementing cryptographic vulnerabilities for educational purposes.
# *  Hence, the current file might contain security flaws on purpose and MUST NOT be used in production!
# *  Please do not use this source code outside this scope, or use it knowingly.
# *
# *  Many files come from the Android element (https://github.com/vector-im/element-android), the
# *  Matrix SDK (https://github.com/matrix-org/matrix-android-sdk2) as well as the Android Yubikit
# *  (https://github.com/Yubico/yubikit-android) projects and have been willingly modified
# *  for the CRY.ME project purposes. The Android element, Matrix SDK and Yubikit projects are distributed
# *  under the Apache-2.0 license, and so is the CRY.ME project.
# *
# ***************************  (END OF CRY.ME HEADER)   *************************************************/
#
# Copyright 2019 Matrix.org Foundation C.I.C.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
import argparse
import json
import logging
import os
import sys
import tempfile
from typing import List, Optional

from twisted.internet import defer, task

import synapse
from synapse.app import _base
from synapse.config._base import ConfigError
from synapse.config.homeserver import HomeServerConfig
from synapse.config.logger import setup_logging
from synapse.events import EventBase
from synapse.handlers.admin import ExfiltrationWriter
from synapse.replication.slave.storage._base import BaseSlavedStore
from synapse.replication.slave.storage.account_data import SlavedAccountDataStore
from synapse.replication.slave.storage.appservice import SlavedApplicationServiceStore
from synapse.replication.slave.storage.client_ips import SlavedClientIpStore
from synapse.replication.slave.storage.deviceinbox import SlavedDeviceInboxStore
from synapse.replication.slave.storage.devices import SlavedDeviceStore
from synapse.replication.slave.storage.events import SlavedEventStore
from synapse.replication.slave.storage.filtering import SlavedFilteringStore
from synapse.replication.slave.storage.groups import SlavedGroupServerStore
from synapse.replication.slave.storage.push_rule import SlavedPushRuleStore
from synapse.replication.slave.storage.receipts import SlavedReceiptsStore
from synapse.replication.slave.storage.registration import SlavedRegistrationStore
from synapse.server import HomeServer
from synapse.storage.databases.main.room import RoomWorkerStore
from synapse.types import StateMap
from synapse.util.logcontext import LoggingContext
from synapse.util.versionstring import get_version_string

logger = logging.getLogger("synapse.app.admin_cmd")


class AdminCmdSlavedStore(
    SlavedReceiptsStore,
    SlavedAccountDataStore,
    SlavedApplicationServiceStore,
    SlavedRegistrationStore,
    SlavedFilteringStore,
    SlavedGroupServerStore,
    SlavedDeviceInboxStore,
    SlavedDeviceStore,
    SlavedPushRuleStore,
    SlavedEventStore,
    SlavedClientIpStore,
    BaseSlavedStore,
    RoomWorkerStore,
):
    pass


class AdminCmdServer(HomeServer):
    DATASTORE_CLASS = AdminCmdSlavedStore  # type: ignore


async def export_data_command(hs: HomeServer, args: argparse.Namespace) -> None:
    """Export data for a user."""

    user_id = args.user_id
    directory = args.output_directory

    res = await hs.get_admin_handler().export_user_data(
        user_id, FileExfiltrationWriter(user_id, directory=directory)
    )
    print(res)


class FileExfiltrationWriter(ExfiltrationWriter):
    """An ExfiltrationWriter that writes the users data to a directory.
    Returns the directory location on completion.

    Note: This writes to disk on the main reactor thread.

    Args:
        user_id: The user whose data is being exfiltrated.
        directory: The directory to write the data to, if None then will write
            to a temporary directory.
    """

    def __init__(self, user_id: str, directory: Optional[str] = None):
        self.user_id = user_id

        if directory:
            self.base_directory = directory
        else:
            self.base_directory = tempfile.mkdtemp(
                prefix="synapse-exfiltrate__%s__" % (user_id,)
            )

        os.makedirs(self.base_directory, exist_ok=True)
        if list(os.listdir(self.base_directory)):
            raise Exception("Directory must be empty")

    def write_events(self, room_id: str, events: List[EventBase]) -> None:
        room_directory = os.path.join(self.base_directory, "rooms", room_id)
        os.makedirs(room_directory, exist_ok=True)
        events_file = os.path.join(room_directory, "events")

        with open(events_file, "a") as f:
            for event in events:
                print(json.dumps(event.get_pdu_json()), file=f)

    def write_state(
        self, room_id: str, event_id: str, state: StateMap[EventBase]
    ) -> None:
        room_directory = os.path.join(self.base_directory, "rooms", room_id)
        state_directory = os.path.join(room_directory, "state")
        os.makedirs(state_directory, exist_ok=True)

        event_file = os.path.join(state_directory, event_id)

        with open(event_file, "a") as f:
            for event in state.values():
                print(json.dumps(event.get_pdu_json()), file=f)

    def write_invite(
        self, room_id: str, event: EventBase, state: StateMap[EventBase]
    ) -> None:
        self.write_events(room_id, [event])

        # We write the invite state somewhere else as they aren't full events
        # and are only a subset of the state at the event.
        room_directory = os.path.join(self.base_directory, "rooms", room_id)
        os.makedirs(room_directory, exist_ok=True)

        invite_state = os.path.join(room_directory, "invite_state")

        with open(invite_state, "a") as f:
            for event in state.values():
                print(json.dumps(event), file=f)

    def write_knock(
        self, room_id: str, event: EventBase, state: StateMap[EventBase]
    ) -> None:
        self.write_events(room_id, [event])

        # We write the knock state somewhere else as they aren't full events
        # and are only a subset of the state at the event.
        room_directory = os.path.join(self.base_directory, "rooms", room_id)
        os.makedirs(room_directory, exist_ok=True)

        knock_state = os.path.join(room_directory, "knock_state")

        with open(knock_state, "a") as f:
            for event in state.values():
                print(json.dumps(event), file=f)

    def finished(self) -> str:
        return self.base_directory


def start(config_options: List[str]) -> None:
    parser = argparse.ArgumentParser(description="Synapse Admin Command")
    HomeServerConfig.add_arguments_to_parser(parser)

    subparser = parser.add_subparsers(
        title="Admin Commands",
        required=True,
        dest="command",
        metavar="<admin_command>",
        help="The admin command to perform.",
    )
    export_data_parser = subparser.add_parser(
        "export-data", help="Export all data for a user"
    )
    export_data_parser.add_argument("user_id", help="User to extra data from")
    export_data_parser.add_argument(
        "--output-directory",
        action="store",
        metavar="DIRECTORY",
        required=False,
        help="The directory to store the exported data in. Must be empty. Defaults"
        " to creating a temp directory.",
    )
    export_data_parser.set_defaults(func=export_data_command)

    try:
        config, args = HomeServerConfig.load_config_with_parser(parser, config_options)
    except ConfigError as e:
        sys.stderr.write("\n" + str(e) + "\n")
        sys.exit(1)

    if config.worker.worker_app is not None:
        assert config.worker.worker_app == "synapse.app.admin_cmd"

    # Update the config with some basic overrides so that don't have to specify
    # a full worker config.
    config.worker.worker_app = "synapse.app.admin_cmd"

    if not config.worker.worker_daemonize and not config.worker.worker_log_config:
        # Since we're meant to be run as a "command" let's not redirect stdio
        # unless we've actually set log config.
        config.logging.no_redirect_stdio = True

    # Explicitly disable background processes
    config.server.update_user_directory = False
    config.worker.run_background_tasks = False
    config.worker.start_pushers = False
    config.worker.pusher_shard_config.instances = []
    config.worker.send_federation = False
    config.worker.federation_shard_config.instances = []

    synapse.events.USE_FROZEN_DICTS = config.server.use_frozen_dicts

    ss = AdminCmdServer(
        config.server.server_name,
        config=config,
        version_string="Synapse/" + get_version_string(synapse),
    )

    setup_logging(ss, config, use_worker_options=True)

    ss.setup()

    # We use task.react as the basic run command as it correctly handles tearing
    # down the reactor when the deferreds resolve and setting the return value.
    # We also make sure that `_base.start` gets run before we actually run the
    # command.

    async def run() -> None:
        with LoggingContext("command"):
            await _base.start(ss)
            await args.func(ss, args)

    _base.start_worker_reactor(
        "synapse-admin-cmd",
        config,
        run_command=lambda: task.react(lambda _reactor: defer.ensureDeferred(run())),
    )


if __name__ == "__main__":
    with LoggingContext("main"):
        start(sys.argv[1:])
