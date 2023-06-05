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
# Copyright 2019 New Vector Ltd
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
import json
from http import HTTPStatus

from twisted.test.proto_helpers import MemoryReactor

from synapse.rest import admin
from synapse.rest.client import directory, login, room
from synapse.server import HomeServer
from synapse.types import RoomAlias
from synapse.util import Clock
from synapse.util.stringutils import random_string

from tests import unittest
from tests.unittest import override_config


class DirectoryTestCase(unittest.HomeserverTestCase):

    servlets = [
        admin.register_servlets_for_client_rest_resource,
        directory.register_servlets,
        login.register_servlets,
        room.register_servlets,
    ]

    def make_homeserver(self, reactor: MemoryReactor, clock: Clock) -> HomeServer:
        config = self.default_config()
        config["require_membership_for_aliases"] = True

        self.hs = self.setup_test_homeserver(config=config)

        return self.hs

    def prepare(
        self, reactor: MemoryReactor, clock: Clock, homeserver: HomeServer
    ) -> None:
        """Create two local users and access tokens for them.
        One of them creates a room."""
        self.room_owner = self.register_user("room_owner", "test")
        self.room_owner_tok = self.login("room_owner", "test")

        self.room_id = self.helper.create_room_as(
            self.room_owner, tok=self.room_owner_tok
        )

        self.user = self.register_user("user", "test")
        self.user_tok = self.login("user", "test")

    def test_state_event_not_in_room(self) -> None:
        self.ensure_user_left_room()
        self.set_alias_via_state_event(HTTPStatus.FORBIDDEN)

    def test_directory_endpoint_not_in_room(self) -> None:
        self.ensure_user_left_room()
        self.set_alias_via_directory(HTTPStatus.FORBIDDEN)

    def test_state_event_in_room_too_long(self) -> None:
        self.ensure_user_joined_room()
        self.set_alias_via_state_event(HTTPStatus.BAD_REQUEST, alias_length=256)

    def test_directory_in_room_too_long(self) -> None:
        self.ensure_user_joined_room()
        self.set_alias_via_directory(HTTPStatus.BAD_REQUEST, alias_length=256)

    @override_config({"default_room_version": 5})
    def test_state_event_user_in_v5_room(self) -> None:
        """Test that a regular user can add alias events before room v6"""
        self.ensure_user_joined_room()
        self.set_alias_via_state_event(HTTPStatus.OK)

    @override_config({"default_room_version": 6})
    def test_state_event_v6_room(self) -> None:
        """Test that a regular user can *not* add alias events from room v6"""
        self.ensure_user_joined_room()
        self.set_alias_via_state_event(HTTPStatus.FORBIDDEN)

    def test_directory_in_room(self) -> None:
        self.ensure_user_joined_room()
        self.set_alias_via_directory(HTTPStatus.OK)

    def test_room_creation_too_long(self) -> None:
        url = "/_matrix/client/r0/createRoom"

        # We use deliberately a localpart under the length threshold so
        # that we can make sure that the check is done on the whole alias.
        data = {"room_alias_name": random_string(256 - len(self.hs.hostname))}
        request_data = json.dumps(data)
        channel = self.make_request(
            "POST", url, request_data, access_token=self.user_tok
        )
        self.assertEqual(channel.code, HTTPStatus.BAD_REQUEST, channel.result)

    def test_room_creation(self) -> None:
        url = "/_matrix/client/r0/createRoom"

        # Check with an alias of allowed length. There should already be
        # a test that ensures it works in test_register.py, but let's be
        # as cautious as possible here.
        data = {"room_alias_name": random_string(5)}
        request_data = json.dumps(data)
        channel = self.make_request(
            "POST", url, request_data, access_token=self.user_tok
        )
        self.assertEqual(channel.code, HTTPStatus.OK, channel.result)

    def test_deleting_alias_via_directory(self) -> None:
        # Add an alias for the room. We must be joined to do so.
        self.ensure_user_joined_room()
        alias = self.set_alias_via_directory(HTTPStatus.OK)

        # Then try to remove the alias
        channel = self.make_request(
            "DELETE",
            f"/_matrix/client/r0/directory/room/{alias}",
            access_token=self.user_tok,
        )
        self.assertEqual(channel.code, HTTPStatus.OK, channel.result)

    def test_deleting_nonexistant_alias(self) -> None:
        # Check that no alias exists
        alias = "#potato:test"
        channel = self.make_request(
            "GET",
            f"/_matrix/client/r0/directory/room/{alias}",
            access_token=self.user_tok,
        )
        self.assertEqual(channel.code, HTTPStatus.NOT_FOUND, channel.result)
        self.assertIn("error", channel.json_body, channel.json_body)
        self.assertEqual(channel.json_body["errcode"], "M_NOT_FOUND", channel.json_body)

        # Then try to remove the alias
        channel = self.make_request(
            "DELETE",
            f"/_matrix/client/r0/directory/room/{alias}",
            access_token=self.user_tok,
        )
        self.assertEqual(channel.code, HTTPStatus.NOT_FOUND, channel.result)
        self.assertIn("error", channel.json_body, channel.json_body)
        self.assertEqual(channel.json_body["errcode"], "M_NOT_FOUND", channel.json_body)

    def set_alias_via_state_event(
        self, expected_code: HTTPStatus, alias_length: int = 5
    ) -> None:
        url = "/_matrix/client/r0/rooms/%s/state/m.room.aliases/%s" % (
            self.room_id,
            self.hs.hostname,
        )

        data = {"aliases": [self.random_alias(alias_length)]}
        request_data = json.dumps(data)

        channel = self.make_request(
            "PUT", url, request_data, access_token=self.user_tok
        )
        self.assertEqual(channel.code, expected_code, channel.result)

    def set_alias_via_directory(
        self, expected_code: HTTPStatus, alias_length: int = 5
    ) -> str:
        alias = self.random_alias(alias_length)
        url = "/_matrix/client/r0/directory/room/%s" % alias
        data = {"room_id": self.room_id}
        request_data = json.dumps(data)

        channel = self.make_request(
            "PUT", url, request_data, access_token=self.user_tok
        )
        self.assertEqual(channel.code, expected_code, channel.result)
        return alias

    def random_alias(self, length: int) -> str:
        return RoomAlias(random_string(length), self.hs.hostname).to_string()

    def ensure_user_left_room(self) -> None:
        self.ensure_membership("leave")

    def ensure_user_joined_room(self) -> None:
        self.ensure_membership("join")

    def ensure_membership(self, membership: str) -> None:
        try:
            if membership == "leave":
                self.helper.leave(room=self.room_id, user=self.user, tok=self.user_tok)
            if membership == "join":
                self.helper.join(room=self.room_id, user=self.user, tok=self.user_tok)
        except AssertionError:
            # We don't care whether the leave request didn't return a 200 (e.g.
            # if the user isn't already in the room), because we only want to
            # make sure the user isn't in the room.
            pass
