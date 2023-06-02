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
# Copyright 2017, 2018 New Vector Ltd
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
from typing import TYPE_CHECKING, Optional, Tuple

from synapse.api.errors import Codes, NotFoundError, SynapseError
from synapse.http.server import HttpServer
from synapse.http.servlet import (
    RestServlet,
    parse_json_object_from_request,
    parse_string,
)
from synapse.http.site import SynapseRequest
from synapse.types import JsonDict

from ._base import client_patterns

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


class RoomKeysServlet(RestServlet):
    PATTERNS = client_patterns(
        "/room_keys/keys(/(?P<room_id>[^/]+))?(/(?P<session_id>[^/]+))?$"
    )

    def __init__(self, hs: "HomeServer"):
        super().__init__()
        self.auth = hs.get_auth()
        self.e2e_room_keys_handler = hs.get_e2e_room_keys_handler()

    async def on_PUT(
        self, request: SynapseRequest, room_id: Optional[str], session_id: Optional[str]
    ) -> Tuple[int, JsonDict]:
        """
        Uploads one or more encrypted E2E room keys for backup purposes.
        room_id: the ID of the room the keys are for (optional)
        session_id: the ID for the E2E room keys for the room (optional)
        version: the version of the user's backup which this data is for.
        the version must already have been created via the /room_keys/version API.

        Each session has:
         * first_message_index: a numeric index indicating the oldest message
           encrypted by this session.
         * forwarded_count: how many times the uploading client claims this key
           has been shared (forwarded)
         * is_verified: whether the client that uploaded the keys claims they
           were sent by a device which they've verified
         * session_data: base64-encrypted data describing the session.

        Returns 200 OK on success with body {}
        Returns 403 Forbidden if the version in question is not the most recently
        created version (i.e. if this is an old client trying to write to a stale backup)
        Returns 404 Not Found if the version in question doesn't exist

        The API is designed to be otherwise agnostic to the room_key encryption
        algorithm being used.  Sessions are merged with existing ones in the
        backup using the heuristics:
         * is_verified sessions always win over unverified sessions
         * older first_message_index always win over newer sessions
         * lower forwarded_count always wins over higher forwarded_count

        We trust the clients not to lie and corrupt their own backups.
        It also means that if your access_token is stolen, the attacker could
        delete your backup.

        POST /room_keys/keys/!abc:matrix.org/c0ff33?version=1 HTTP/1.1
        Content-Type: application/json

        {
            "first_message_index": 1,
            "forwarded_count": 1,
            "is_verified": false,
            "session_data": "SSBBTSBBIEZJU0gK"
        }

        Or...

        POST /room_keys/keys/!abc:matrix.org?version=1 HTTP/1.1
        Content-Type: application/json

        {
            "sessions": {
                "c0ff33": {
                    "first_message_index": 1,
                    "forwarded_count": 1,
                    "is_verified": false,
                    "session_data": "SSBBTSBBIEZJU0gK"
                }
            }
        }

        Or...

        POST /room_keys/keys?version=1 HTTP/1.1
        Content-Type: application/json

        {
            "rooms": {
                "!abc:matrix.org": {
                    "sessions": {
                        "c0ff33": {
                            "first_message_index": 1,
                            "forwarded_count": 1,
                            "is_verified": false,
                            "session_data": "SSBBTSBBIEZJU0gK"
                        }
                    }
                }
            }
        }
        """
        requester = await self.auth.get_user_by_req(request, allow_guest=False)
        user_id = requester.user.to_string()
        body = parse_json_object_from_request(request)
        version = parse_string(request, "version")

        if session_id:
            body = {"sessions": {session_id: body}}

        if room_id:
            body = {"rooms": {room_id: body}}

        ret = await self.e2e_room_keys_handler.upload_room_keys(user_id, version, body)
        return 200, ret

    async def on_GET(
        self, request: SynapseRequest, room_id: Optional[str], session_id: Optional[str]
    ) -> Tuple[int, JsonDict]:
        """
        Retrieves one or more encrypted E2E room keys for backup purposes.
        Symmetric with the PUT version of the API.

        room_id: the ID of the room to retrieve the keys for (optional)
        session_id: the ID for the E2E room keys to retrieve the keys for (optional)
        version: the version of the user's backup which this data is for.
        the version must already have been created via the /change_secret API.

        Returns as follows:

        GET /room_keys/keys/!abc:matrix.org/c0ff33?version=1 HTTP/1.1
        {
            "first_message_index": 1,
            "forwarded_count": 1,
            "is_verified": false,
            "session_data": "SSBBTSBBIEZJU0gK"
        }

        Or...

        GET /room_keys/keys/!abc:matrix.org?version=1 HTTP/1.1
        {
            "sessions": {
                "c0ff33": {
                    "first_message_index": 1,
                    "forwarded_count": 1,
                    "is_verified": false,
                    "session_data": "SSBBTSBBIEZJU0gK"
                }
            }
        }

        Or...

        GET /room_keys/keys?version=1 HTTP/1.1
        {
            "rooms": {
                "!abc:matrix.org": {
                    "sessions": {
                        "c0ff33": {
                            "first_message_index": 1,
                            "forwarded_count": 1,
                            "is_verified": false,
                            "session_data": "SSBBTSBBIEZJU0gK"
                        }
                    }
                }
            }
        }
        """
        requester = await self.auth.get_user_by_req(request, allow_guest=False)
        user_id = requester.user.to_string()
        version = parse_string(request, "version", required=True)

        room_keys = await self.e2e_room_keys_handler.get_room_keys(
            user_id, version, room_id, session_id
        )

        # Convert room_keys to the right format to return.
        if session_id:
            # If the client requests a specific session, but that session was
            # not backed up, then return an M_NOT_FOUND.
            if room_keys["rooms"] == {}:
                raise NotFoundError("No room_keys found")
            else:
                room_keys = room_keys["rooms"][room_id]["sessions"][session_id]
        elif room_id:
            # If the client requests all sessions from a room, but no sessions
            # are found, then return an empty result rather than an error, so
            # that clients don't have to handle an error condition, and an
            # empty result is valid.  (Similarly if the client requests all
            # sessions from the backup, but in that case, room_keys is already
            # in the right format, so we don't need to do anything about it.)
            if room_keys["rooms"] == {}:
                room_keys = {"sessions": {}}
            else:
                room_keys = room_keys["rooms"][room_id]

        return 200, room_keys

    async def on_DELETE(
        self, request: SynapseRequest, room_id: Optional[str], session_id: Optional[str]
    ) -> Tuple[int, JsonDict]:
        """
        Deletes one or more encrypted E2E room keys for a user for backup purposes.

        DELETE /room_keys/keys/!abc:matrix.org/c0ff33?version=1
        HTTP/1.1 200 OK
        {}

        room_id: the ID of the room whose keys to delete (optional)
        session_id: the ID for the E2E session to delete (optional)
        version: the version of the user's backup which this data is for.
        the version must already have been created via the /change_secret API.
        """

        requester = await self.auth.get_user_by_req(request, allow_guest=False)
        user_id = requester.user.to_string()
        version = parse_string(request, "version")

        ret = await self.e2e_room_keys_handler.delete_room_keys(
            user_id, version, room_id, session_id
        )
        return 200, ret


class RoomKeysNewVersionServlet(RestServlet):
    PATTERNS = client_patterns("/room_keys/version$")

    def __init__(self, hs: "HomeServer"):
        super().__init__()
        self.auth = hs.get_auth()
        self.e2e_room_keys_handler = hs.get_e2e_room_keys_handler()

    async def on_POST(self, request: SynapseRequest) -> Tuple[int, JsonDict]:
        """
        Create a new backup version for this user's room_keys with the given
        info.  The version is allocated by the server and returned to the user
        in the response.  This API is intended to be used whenever the user
        changes the encryption key for their backups, ensuring that backups
        encrypted with different keys don't collide.

        It takes out an exclusive lock on this user's room_key backups, to ensure
        clients only upload to the current backup.

        The algorithm passed in the version info is a reverse-DNS namespaced
        identifier to describe the format of the encrypted backupped keys.

        The auth_data is { user_id: "user_id", nonce: <random string> }
        encrypted using the algorithm and current encryption key described above.

        POST /room_keys/version
        Content-Type: application/json
        {
            "algorithm": "m.megolm_backup.v1",
            "auth_data": "dGhpcyBzaG91bGQgYWN0dWFsbHkgYmUgZW5jcnlwdGVkIGpzb24K"
        }

        HTTP/1.1 200 OK
        Content-Type: application/json
        {
            "version": 12345
        }
        """
        requester = await self.auth.get_user_by_req(request, allow_guest=False)
        user_id = requester.user.to_string()
        info = parse_json_object_from_request(request)

        new_version = await self.e2e_room_keys_handler.create_version(user_id, info)
        return 200, {"version": new_version}

    # we deliberately don't have a PUT /version, as these things really should
    # be immutable to avoid people footgunning


class RoomKeysVersionServlet(RestServlet):
    PATTERNS = client_patterns("/room_keys/version(/(?P<version>[^/]+))?$")

    def __init__(self, hs: "HomeServer"):
        super().__init__()
        self.auth = hs.get_auth()
        self.e2e_room_keys_handler = hs.get_e2e_room_keys_handler()

    async def on_GET(
        self, request: SynapseRequest, version: Optional[str]
    ) -> Tuple[int, JsonDict]:
        """
        Retrieve the version information about a given version of the user's
        room_keys backup.  If the version part is missing, returns info about the
        most current backup version (if any)

        It takes out an exclusive lock on this user's room_key backups, to ensure
        clients only upload to the current backup.

        Returns 404 if the given version does not exist.

        GET /room_keys/version/12345 HTTP/1.1
        {
            "version": "12345",
            "algorithm": "m.megolm_backup.v1",
            "auth_data": "dGhpcyBzaG91bGQgYWN0dWFsbHkgYmUgZW5jcnlwdGVkIGpzb24K"
        }
        """
        requester = await self.auth.get_user_by_req(request, allow_guest=False)
        user_id = requester.user.to_string()

        try:
            info = await self.e2e_room_keys_handler.get_version_info(user_id, version)
        except SynapseError as e:
            if e.code == 404:
                raise SynapseError(404, "No backup found", Codes.NOT_FOUND)
        return 200, info

    async def on_DELETE(
        self, request: SynapseRequest, version: Optional[str]
    ) -> Tuple[int, JsonDict]:
        """
        Delete the information about a given version of the user's
        room_keys backup.  If the version part is missing, deletes the most
        current backup version (if any). Doesn't delete the actual room data.

        DELETE /room_keys/version/12345 HTTP/1.1
        HTTP/1.1 200 OK
        {}
        """
        if version is None:
            raise SynapseError(400, "No version specified to delete", Codes.NOT_FOUND)

        requester = await self.auth.get_user_by_req(request, allow_guest=False)
        user_id = requester.user.to_string()

        await self.e2e_room_keys_handler.delete_version(user_id, version)
        return 200, {}

    async def on_PUT(
        self, request: SynapseRequest, version: Optional[str]
    ) -> Tuple[int, JsonDict]:
        """
        Update the information about a given version of the user's room_keys backup.

        POST /room_keys/version/12345 HTTP/1.1
        Content-Type: application/json
        {
            "algorithm": "m.megolm_backup.v1",
            "auth_data": {
                "public_key": "abcdefg",
                "signatures": {
                    "weisig25519:something": "hijklmnop"
                }
            },
            "version": "12345"
        }

        HTTP/1.1 200 OK
        Content-Type: application/json
        {}
        """
        requester = await self.auth.get_user_by_req(request, allow_guest=False)
        user_id = requester.user.to_string()
        info = parse_json_object_from_request(request)

        if version is None:
            raise SynapseError(
                400, "No version specified to update", Codes.MISSING_PARAM
            )

        await self.e2e_room_keys_handler.update_version(user_id, version, info)
        return 200, {}


def register_servlets(hs: "HomeServer", http_server: HttpServer) -> None:
    RoomKeysServlet(hs).register(http_server)
    RoomKeysVersionServlet(hs).register(http_server)
    RoomKeysNewVersionServlet(hs).register(http_server)
