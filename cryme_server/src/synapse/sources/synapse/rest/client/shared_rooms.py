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
# Copyright 2020 Half-Shot
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

from synapse.api.errors import Codes, SynapseError
from synapse.http.server import HttpServer
from synapse.http.servlet import RestServlet
from synapse.http.site import SynapseRequest
from synapse.types import JsonDict, UserID

from ._base import client_patterns

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


class UserSharedRoomsServlet(RestServlet):
    """
    GET /uk.half-shot.msc2666/user/shared_rooms/{user_id} HTTP/1.1
    """

    PATTERNS = client_patterns(
        "/uk.half-shot.msc2666/user/shared_rooms/(?P<user_id>[^/]*)",
        releases=(),  # This is an unstable feature
    )

    def __init__(self, hs: "HomeServer"):
        super().__init__()
        self.auth = hs.get_auth()
        self.store = hs.get_datastore()
        self.user_directory_active = hs.config.server.update_user_directory

    async def on_GET(
        self, request: SynapseRequest, user_id: str
    ) -> Tuple[int, JsonDict]:

        if not self.user_directory_active:
            raise SynapseError(
                code=400,
                msg="The user directory is disabled on this server. Cannot determine shared rooms.",
                errcode=Codes.FORBIDDEN,
            )

        UserID.from_string(user_id)

        requester = await self.auth.get_user_by_req(request)
        if user_id == requester.user.to_string():
            raise SynapseError(
                code=400,
                msg="You cannot request a list of shared rooms with yourself",
                errcode=Codes.FORBIDDEN,
            )
        rooms = await self.store.get_shared_rooms_for_users(
            requester.user.to_string(), user_id
        )

        return 200, {"joined": list(rooms)}


def register_servlets(hs: "HomeServer", http_server: HttpServer) -> None:
    UserSharedRoomsServlet(hs).register(http_server)
