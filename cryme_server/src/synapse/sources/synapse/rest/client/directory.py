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
# Copyright 2014-2016 OpenMarket Ltd
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

from twisted.web.server import Request

from synapse.api.errors import (
    AuthError,
    Codes,
    InvalidClientCredentialsError,
    NotFoundError,
    SynapseError,
)
from synapse.http.server import HttpServer
from synapse.http.servlet import RestServlet, parse_json_object_from_request
from synapse.http.site import SynapseRequest
from synapse.rest.client._base import client_patterns
from synapse.types import JsonDict, RoomAlias

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


def register_servlets(hs: "HomeServer", http_server: HttpServer) -> None:
    ClientDirectoryServer(hs).register(http_server)
    ClientDirectoryListServer(hs).register(http_server)
    ClientAppserviceDirectoryListServer(hs).register(http_server)


class ClientDirectoryServer(RestServlet):
    PATTERNS = client_patterns("/directory/room/(?P<room_alias>[^/]*)$", v1=True)

    def __init__(self, hs: "HomeServer"):
        super().__init__()
        self.store = hs.get_datastore()
        self.directory_handler = hs.get_directory_handler()
        self.auth = hs.get_auth()

    async def on_GET(self, request: Request, room_alias: str) -> Tuple[int, JsonDict]:
        room_alias_obj = RoomAlias.from_string(room_alias)

        res = await self.directory_handler.get_association(room_alias_obj)

        return 200, res

    async def on_PUT(
        self, request: SynapseRequest, room_alias: str
    ) -> Tuple[int, JsonDict]:
        room_alias_obj = RoomAlias.from_string(room_alias)

        content = parse_json_object_from_request(request)
        if "room_id" not in content:
            raise SynapseError(
                400, 'Missing params: ["room_id"]', errcode=Codes.BAD_JSON
            )

        logger.debug("Got content: %s", content)
        logger.debug("Got room name: %s", room_alias_obj.to_string())

        room_id = content["room_id"]
        servers = content["servers"] if "servers" in content else None

        logger.debug("Got room_id: %s", room_id)
        logger.debug("Got servers: %s", servers)

        # TODO(erikj): Check types.

        room = await self.store.get_room(room_id)
        if room is None:
            raise SynapseError(400, "Room does not exist")

        requester = await self.auth.get_user_by_req(request)

        await self.directory_handler.create_association(
            requester, room_alias_obj, room_id, servers
        )

        return 200, {}

    async def on_DELETE(
        self, request: SynapseRequest, room_alias: str
    ) -> Tuple[int, JsonDict]:
        room_alias_obj = RoomAlias.from_string(room_alias)

        try:
            service = self.auth.get_appservice_by_req(request)
            await self.directory_handler.delete_appservice_association(
                service, room_alias_obj
            )
            logger.info(
                "Application service at %s deleted alias %s",
                service.url,
                room_alias_obj.to_string(),
            )
            return 200, {}
        except InvalidClientCredentialsError:
            # fallback to default user behaviour if they aren't an AS
            pass

        requester = await self.auth.get_user_by_req(request)
        user = requester.user

        await self.directory_handler.delete_association(requester, room_alias_obj)

        logger.info(
            "User %s deleted alias %s", user.to_string(), room_alias_obj.to_string()
        )

        return 200, {}


class ClientDirectoryListServer(RestServlet):
    PATTERNS = client_patterns("/directory/list/room/(?P<room_id>[^/]*)$", v1=True)

    def __init__(self, hs: "HomeServer"):
        super().__init__()
        self.store = hs.get_datastore()
        self.directory_handler = hs.get_directory_handler()
        self.auth = hs.get_auth()

    async def on_GET(self, request: Request, room_id: str) -> Tuple[int, JsonDict]:
        room = await self.store.get_room(room_id)
        if room is None:
            raise NotFoundError("Unknown room")

        return 200, {"visibility": "public" if room["is_public"] else "private"}

    async def on_PUT(
        self, request: SynapseRequest, room_id: str
    ) -> Tuple[int, JsonDict]:
        requester = await self.auth.get_user_by_req(request)

        content = parse_json_object_from_request(request)
        visibility = content.get("visibility", "public")

        await self.directory_handler.edit_published_room_list(
            requester, room_id, visibility
        )

        return 200, {}

    async def on_DELETE(
        self, request: SynapseRequest, room_id: str
    ) -> Tuple[int, JsonDict]:
        requester = await self.auth.get_user_by_req(request)

        await self.directory_handler.edit_published_room_list(
            requester, room_id, "private"
        )

        return 200, {}


class ClientAppserviceDirectoryListServer(RestServlet):
    PATTERNS = client_patterns(
        "/directory/list/appservice/(?P<network_id>[^/]*)/(?P<room_id>[^/]*)$", v1=True
    )

    def __init__(self, hs: "HomeServer"):
        super().__init__()
        self.store = hs.get_datastore()
        self.directory_handler = hs.get_directory_handler()
        self.auth = hs.get_auth()

    async def on_PUT(
        self, request: SynapseRequest, network_id: str, room_id: str
    ) -> Tuple[int, JsonDict]:
        content = parse_json_object_from_request(request)
        visibility = content.get("visibility", "public")
        return await self._edit(request, network_id, room_id, visibility)

    async def on_DELETE(
        self, request: SynapseRequest, network_id: str, room_id: str
    ) -> Tuple[int, JsonDict]:
        return await self._edit(request, network_id, room_id, "private")

    async def _edit(
        self, request: SynapseRequest, network_id: str, room_id: str, visibility: str
    ) -> Tuple[int, JsonDict]:
        requester = await self.auth.get_user_by_req(request)
        if not requester.app_service:
            raise AuthError(
                403, "Only appservices can edit the appservice published room list"
            )

        await self.directory_handler.edit_published_appservice_room_list(
            requester.app_service.id, network_id, room_id, visibility
        )

        return 200, {}
