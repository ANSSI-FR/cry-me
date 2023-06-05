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

from synapse.api.errors import Codes
from synapse.events.utils import CANONICALJSON_MAX_INT, CANONICALJSON_MIN_INT
from synapse.rest import admin
from synapse.rest.client import login, room, sync

from tests.unittest import HomeserverTestCase


class PowerLevelsTestCase(HomeserverTestCase):
    """Tests that power levels are enforced in various situations"""

    servlets = [
        admin.register_servlets,
        room.register_servlets,
        login.register_servlets,
        sync.register_servlets,
    ]

    def make_homeserver(self, reactor, clock):
        config = self.default_config()

        return self.setup_test_homeserver(config=config)

    def prepare(self, reactor, clock, hs):
        # register a room admin, moderator and regular user
        self.admin_user_id = self.register_user("admin", "pass")
        self.admin_access_token = self.login("admin", "pass")
        self.mod_user_id = self.register_user("mod", "pass")
        self.mod_access_token = self.login("mod", "pass")
        self.user_user_id = self.register_user("user", "pass")
        self.user_access_token = self.login("user", "pass")

        # Create a room
        self.room_id = self.helper.create_room_as(
            self.admin_user_id, tok=self.admin_access_token
        )

        # Invite the other users
        self.helper.invite(
            room=self.room_id,
            src=self.admin_user_id,
            tok=self.admin_access_token,
            targ=self.mod_user_id,
        )
        self.helper.invite(
            room=self.room_id,
            src=self.admin_user_id,
            tok=self.admin_access_token,
            targ=self.user_user_id,
        )

        # Make the other users join the room
        self.helper.join(
            room=self.room_id, user=self.mod_user_id, tok=self.mod_access_token
        )
        self.helper.join(
            room=self.room_id, user=self.user_user_id, tok=self.user_access_token
        )

        # Mod the mod
        room_power_levels = self.helper.get_state(
            self.room_id,
            "m.room.power_levels",
            tok=self.admin_access_token,
        )

        # Update existing power levels with mod at PL50
        room_power_levels["users"].update({self.mod_user_id: 50})

        self.helper.send_state(
            self.room_id,
            "m.room.power_levels",
            room_power_levels,
            tok=self.admin_access_token,
        )

    def test_non_admins_cannot_enable_room_encryption(self):
        # have the mod try to enable room encryption
        self.helper.send_state(
            self.room_id,
            "m.room.encryption",
            {"algorithm": "m.megolm.v1.aes-sha2"},
            tok=self.mod_access_token,
            expect_code=403,  # expect failure
        )

        # have the user try to enable room encryption
        self.helper.send_state(
            self.room_id,
            "m.room.encryption",
            {"algorithm": "m.megolm.v1.aes-sha2"},
            tok=self.user_access_token,
            expect_code=403,  # expect failure
        )

    def test_non_admins_cannot_send_server_acl(self):
        # have the mod try to send a server ACL
        self.helper.send_state(
            self.room_id,
            "m.room.server_acl",
            {
                "allow": ["*"],
                "allow_ip_literals": False,
                "deny": ["*.evil.com", "evil.com"],
            },
            tok=self.mod_access_token,
            expect_code=403,  # expect failure
        )

        # have the user try to send a server ACL
        self.helper.send_state(
            self.room_id,
            "m.room.server_acl",
            {
                "allow": ["*"],
                "allow_ip_literals": False,
                "deny": ["*.evil.com", "evil.com"],
            },
            tok=self.user_access_token,
            expect_code=403,  # expect failure
        )

    def test_non_admins_cannot_tombstone_room(self):
        # Create another room that will serve as our "upgraded room"
        self.upgraded_room_id = self.helper.create_room_as(
            self.admin_user_id, tok=self.admin_access_token
        )

        # have the mod try to send a tombstone event
        self.helper.send_state(
            self.room_id,
            "m.room.tombstone",
            {
                "body": "This room has been replaced",
                "replacement_room": self.upgraded_room_id,
            },
            tok=self.mod_access_token,
            expect_code=403,  # expect failure
        )

        # have the user try to send a tombstone event
        self.helper.send_state(
            self.room_id,
            "m.room.tombstone",
            {
                "body": "This room has been replaced",
                "replacement_room": self.upgraded_room_id,
            },
            tok=self.user_access_token,
            expect_code=403,  # expect failure
        )

    def test_admins_can_enable_room_encryption(self):
        # have the admin try to enable room encryption
        self.helper.send_state(
            self.room_id,
            "m.room.encryption",
            {"algorithm": "m.megolm.v1.aes-sha2"},
            tok=self.admin_access_token,
            expect_code=200,  # expect success
        )

    def test_admins_can_send_server_acl(self):
        # have the admin try to send a server ACL
        self.helper.send_state(
            self.room_id,
            "m.room.server_acl",
            {
                "allow": ["*"],
                "allow_ip_literals": False,
                "deny": ["*.evil.com", "evil.com"],
            },
            tok=self.admin_access_token,
            expect_code=200,  # expect success
        )

    def test_admins_can_tombstone_room(self):
        # Create another room that will serve as our "upgraded room"
        self.upgraded_room_id = self.helper.create_room_as(
            self.admin_user_id, tok=self.admin_access_token
        )

        # have the admin try to send a tombstone event
        self.helper.send_state(
            self.room_id,
            "m.room.tombstone",
            {
                "body": "This room has been replaced",
                "replacement_room": self.upgraded_room_id,
            },
            tok=self.admin_access_token,
            expect_code=200,  # expect success
        )

    def test_cannot_set_string_power_levels(self):
        room_power_levels = self.helper.get_state(
            self.room_id,
            "m.room.power_levels",
            tok=self.admin_access_token,
        )

        # Update existing power levels with user at PL "0"
        room_power_levels["users"].update({self.user_user_id: "0"})

        body = self.helper.send_state(
            self.room_id,
            "m.room.power_levels",
            room_power_levels,
            tok=self.admin_access_token,
            expect_code=400,  # expect failure
        )

        self.assertEqual(
            body["errcode"],
            Codes.BAD_JSON,
            body,
        )

    def test_cannot_set_unsafe_large_power_levels(self):
        room_power_levels = self.helper.get_state(
            self.room_id,
            "m.room.power_levels",
            tok=self.admin_access_token,
        )

        # Update existing power levels with user at PL above the max safe integer
        room_power_levels["users"].update(
            {self.user_user_id: CANONICALJSON_MAX_INT + 1}
        )

        body = self.helper.send_state(
            self.room_id,
            "m.room.power_levels",
            room_power_levels,
            tok=self.admin_access_token,
            expect_code=400,  # expect failure
        )

        self.assertEqual(
            body["errcode"],
            Codes.BAD_JSON,
            body,
        )

    def test_cannot_set_unsafe_small_power_levels(self):
        room_power_levels = self.helper.get_state(
            self.room_id,
            "m.room.power_levels",
            tok=self.admin_access_token,
        )

        # Update existing power levels with user at PL below the minimum safe integer
        room_power_levels["users"].update(
            {self.user_user_id: CANONICALJSON_MIN_INT - 1}
        )

        body = self.helper.send_state(
            self.room_id,
            "m.room.power_levels",
            room_power_levels,
            tok=self.admin_access_token,
            expect_code=400,  # expect failure
        )

        self.assertEqual(
            body["errcode"],
            Codes.BAD_JSON,
            body,
        )
