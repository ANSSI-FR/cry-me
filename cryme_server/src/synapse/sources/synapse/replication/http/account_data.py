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
# Copyright 2021 The Matrix.org Foundation C.I.C.
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

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


class ReplicationUserAccountDataRestServlet(ReplicationEndpoint):
    """Add user account data on the appropriate account data worker.

    Request format:

        POST /_synapse/replication/add_user_account_data/:user_id/:type

        {
            "content": { ... },
        }

    """

    NAME = "add_user_account_data"
    PATH_ARGS = ("user_id", "account_data_type")
    CACHE = False

    def __init__(self, hs: "HomeServer"):
        super().__init__(hs)

        self.handler = hs.get_account_data_handler()
        self.clock = hs.get_clock()

    @staticmethod
    async def _serialize_payload(user_id, account_data_type, content):
        payload = {
            "content": content,
        }

        return payload

    async def _handle_request(self, request, user_id, account_data_type):
        content = parse_json_object_from_request(request)

        max_stream_id = await self.handler.add_account_data_for_user(
            user_id, account_data_type, content["content"]
        )

        return 200, {"max_stream_id": max_stream_id}


class ReplicationRoomAccountDataRestServlet(ReplicationEndpoint):
    """Add room account data on the appropriate account data worker.

    Request format:

        POST /_synapse/replication/add_room_account_data/:user_id/:room_id/:account_data_type

        {
            "content": { ... },
        }

    """

    NAME = "add_room_account_data"
    PATH_ARGS = ("user_id", "room_id", "account_data_type")
    CACHE = False

    def __init__(self, hs: "HomeServer"):
        super().__init__(hs)

        self.handler = hs.get_account_data_handler()
        self.clock = hs.get_clock()

    @staticmethod
    async def _serialize_payload(user_id, room_id, account_data_type, content):
        payload = {
            "content": content,
        }

        return payload

    async def _handle_request(self, request, user_id, room_id, account_data_type):
        content = parse_json_object_from_request(request)

        max_stream_id = await self.handler.add_account_data_to_room(
            user_id, room_id, account_data_type, content["content"]
        )

        return 200, {"max_stream_id": max_stream_id}


class ReplicationAddTagRestServlet(ReplicationEndpoint):
    """Add tag on the appropriate account data worker.

    Request format:

        POST /_synapse/replication/add_tag/:user_id/:room_id/:tag

        {
            "content": { ... },
        }

    """

    NAME = "add_tag"
    PATH_ARGS = ("user_id", "room_id", "tag")
    CACHE = False

    def __init__(self, hs: "HomeServer"):
        super().__init__(hs)

        self.handler = hs.get_account_data_handler()
        self.clock = hs.get_clock()

    @staticmethod
    async def _serialize_payload(user_id, room_id, tag, content):
        payload = {
            "content": content,
        }

        return payload

    async def _handle_request(self, request, user_id, room_id, tag):
        content = parse_json_object_from_request(request)

        max_stream_id = await self.handler.add_tag_to_room(
            user_id, room_id, tag, content["content"]
        )

        return 200, {"max_stream_id": max_stream_id}


class ReplicationRemoveTagRestServlet(ReplicationEndpoint):
    """Remove tag on the appropriate account data worker.

    Request format:

        POST /_synapse/replication/remove_tag/:user_id/:room_id/:tag

        {}

    """

    NAME = "remove_tag"
    PATH_ARGS = (
        "user_id",
        "room_id",
        "tag",
    )
    CACHE = False

    def __init__(self, hs: "HomeServer"):
        super().__init__(hs)

        self.handler = hs.get_account_data_handler()
        self.clock = hs.get_clock()

    @staticmethod
    async def _serialize_payload(user_id, room_id, tag):

        return {}

    async def _handle_request(self, request, user_id, room_id, tag):
        max_stream_id = await self.handler.remove_tag_from_room(
            user_id,
            room_id,
            tag,
        )

        return 200, {"max_stream_id": max_stream_id}


def register_servlets(hs: "HomeServer", http_server):
    ReplicationUserAccountDataRestServlet(hs).register(http_server)
    ReplicationRoomAccountDataRestServlet(hs).register(http_server)
    ReplicationAddTagRestServlet(hs).register(http_server)
    ReplicationRemoveTagRestServlet(hs).register(http_server)
