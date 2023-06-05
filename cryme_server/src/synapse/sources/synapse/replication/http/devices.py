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
# Copyright 2018 New Vector Ltd
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

import logging
from typing import TYPE_CHECKING

from synapse.replication.http._base import ReplicationEndpoint

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


class ReplicationUserDevicesResyncRestServlet(ReplicationEndpoint):
    """Ask master to resync the device list for a user by contacting their
    server.

    This must happen on master so that the results can be correctly cached in
    the database and streamed to workers.

    Request format:

        POST /_synapse/replication/user_device_resync/:user_id

        {}

    Response is equivalent to ` /_matrix/federation/v1/user/devices/:user_id`
    response, e.g.:

        {
            "user_id": "@alice:example.org",
            "devices": [
                {
                    "device_id": "JLAFKJWSCS",
                    "keys": { ... },
                    "device_display_name": "Alice's Mobile Phone"
                }
            ]
        }
    """

    NAME = "user_device_resync"
    PATH_ARGS = ("user_id",)
    CACHE = False

    def __init__(self, hs: "HomeServer"):
        super().__init__(hs)

        self.device_list_updater = hs.get_device_handler().device_list_updater
        self.store = hs.get_datastore()
        self.clock = hs.get_clock()

    @staticmethod
    async def _serialize_payload(user_id):
        return {}

    async def _handle_request(self, request, user_id):
        user_devices = await self.device_list_updater.user_device_resync(user_id)

        return 200, user_devices


def register_servlets(hs: "HomeServer", http_server):
    ReplicationUserDevicesResyncRestServlet(hs).register(http_server)
