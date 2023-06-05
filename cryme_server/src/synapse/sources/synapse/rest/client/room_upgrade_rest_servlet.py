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
# Copyright 2016 OpenMarket Ltd
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
from typing import TYPE_CHECKING, Tuple

from synapse.api.errors import Codes, ShadowBanError, SynapseError
from synapse.api.room_versions import KNOWN_ROOM_VERSIONS
from synapse.http.server import HttpServer
from synapse.http.servlet import (
    RestServlet,
    assert_params_in_dict,
    parse_json_object_from_request,
)
from synapse.http.site import SynapseRequest
from synapse.types import JsonDict
from synapse.util import stringutils

from ._base import client_patterns

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


class RoomUpgradeRestServlet(RestServlet):
    """Handler for room upgrade requests.

    Handles requests of the form:

        POST /_matrix/client/r0/rooms/$roomid/upgrade HTTP/1.1
        Content-Type: application/json

        {
            "new_version": "2",
        }

    Creates a new room and shuts down the old one. Returns the ID of the new room.
    """

    PATTERNS = client_patterns(
        # /rooms/$roomid/upgrade
        "/rooms/(?P<room_id>[^/]*)/upgrade$"
    )

    def __init__(self, hs: "HomeServer"):
        super().__init__()
        self._hs = hs
        self._room_creation_handler = hs.get_room_creation_handler()
        self._auth = hs.get_auth()

    async def on_POST(
        self, request: SynapseRequest, room_id: str
    ) -> Tuple[int, JsonDict]:
        requester = await self._auth.get_user_by_req(request)

        content = parse_json_object_from_request(request)
        assert_params_in_dict(content, ("new_version",))

        new_version = KNOWN_ROOM_VERSIONS.get(content["new_version"])
        if new_version is None:
            raise SynapseError(
                400,
                "Your homeserver does not support this room version",
                Codes.UNSUPPORTED_ROOM_VERSION,
            )

        try:
            new_room_id = await self._room_creation_handler.upgrade_room(
                requester, room_id, new_version
            )
        except ShadowBanError:
            # Generate a random room ID.
            new_room_id = stringutils.random_string(18)

        ret = {"replacement_room": new_room_id}

        return 200, ret


def register_servlets(hs: "HomeServer", http_server: HttpServer) -> None:
    RoomUpgradeRestServlet(hs).register(http_server)
