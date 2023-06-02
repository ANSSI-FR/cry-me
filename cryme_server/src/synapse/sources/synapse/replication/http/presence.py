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
# Copyright 2020 The Matrix.org Foundation C.I.C.
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

from synapse.http.servlet import parse_json_object_from_request
from synapse.replication.http._base import ReplicationEndpoint
from synapse.types import UserID

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


class ReplicationBumpPresenceActiveTime(ReplicationEndpoint):
    """We've seen the user do something that indicates they're interacting
    with the app.

    The POST looks like:

        POST /_synapse/replication/bump_presence_active_time/<user_id>

        200 OK

        {}
    """

    NAME = "bump_presence_active_time"
    PATH_ARGS = ("user_id",)
    METHOD = "POST"
    CACHE = False

    def __init__(self, hs: "HomeServer"):
        super().__init__(hs)

        self._presence_handler = hs.get_presence_handler()

    @staticmethod
    async def _serialize_payload(user_id):
        return {}

    async def _handle_request(self, request, user_id):
        await self._presence_handler.bump_presence_active_time(
            UserID.from_string(user_id)
        )

        return (
            200,
            {},
        )


class ReplicationPresenceSetState(ReplicationEndpoint):
    """Set the presence state for a user.

    The POST looks like:

        POST /_synapse/replication/presence_set_state/<user_id>

        {
            "state": { ... },
            "ignore_status_msg": false,
            "force_notify": false
        }

        200 OK

        {}
    """

    NAME = "presence_set_state"
    PATH_ARGS = ("user_id",)
    METHOD = "POST"
    CACHE = False

    def __init__(self, hs: "HomeServer"):
        super().__init__(hs)

        self._presence_handler = hs.get_presence_handler()

    @staticmethod
    async def _serialize_payload(
        user_id, state, ignore_status_msg=False, force_notify=False
    ):
        return {
            "state": state,
            "ignore_status_msg": ignore_status_msg,
            "force_notify": force_notify,
        }

    async def _handle_request(self, request, user_id):
        content = parse_json_object_from_request(request)

        await self._presence_handler.set_state(
            UserID.from_string(user_id),
            content["state"],
            content["ignore_status_msg"],
            content["force_notify"],
        )

        return (
            200,
            {},
        )


def register_servlets(hs: "HomeServer", http_server):
    ReplicationBumpPresenceActiveTime(hs).register(http_server)
    ReplicationPresenceSetState(hs).register(http_server)
