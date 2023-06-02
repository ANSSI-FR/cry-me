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
# Copyright 2019 Matrix.org Federation C.I.C
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

from parameterized import parameterized

from synapse.events import make_event_from_dict
from synapse.federation.federation_server import server_matches_acl_event
from synapse.rest import admin
from synapse.rest.client import login, room

from tests import unittest


class FederationServerTests(unittest.FederatingHomeserverTestCase):

    servlets = [
        admin.register_servlets,
        room.register_servlets,
        login.register_servlets,
    ]

    @parameterized.expand([(b"",), (b"foo",), (b'{"limit": Infinity}',)])
    def test_bad_request(self, query_content):
        """
        Querying with bad data returns a reasonable error code.
        """
        u1 = self.register_user("u1", "pass")
        u1_token = self.login("u1", "pass")

        room_1 = self.helper.create_room_as(u1, tok=u1_token)
        self.inject_room_member(room_1, "@user:other.example.com", "join")

        "/get_missing_events/(?P<room_id>[^/]*)/?"

        channel = self.make_request(
            "POST",
            "/_matrix/federation/v1/get_missing_events/%s" % (room_1,),
            query_content,
        )
        self.assertEquals(400, channel.code, channel.result)
        self.assertEqual(channel.json_body["errcode"], "M_NOT_JSON")


class ServerACLsTestCase(unittest.TestCase):
    def test_blacklisted_server(self):
        e = _create_acl_event({"allow": ["*"], "deny": ["evil.com"]})
        logging.info("ACL event: %s", e.content)

        self.assertFalse(server_matches_acl_event("evil.com", e))
        self.assertFalse(server_matches_acl_event("EVIL.COM", e))

        self.assertTrue(server_matches_acl_event("evil.com.au", e))
        self.assertTrue(server_matches_acl_event("honestly.not.evil.com", e))

    def test_block_ip_literals(self):
        e = _create_acl_event({"allow_ip_literals": False, "allow": ["*"]})
        logging.info("ACL event: %s", e.content)

        self.assertFalse(server_matches_acl_event("1.2.3.4", e))
        self.assertTrue(server_matches_acl_event("1a.2.3.4", e))
        self.assertFalse(server_matches_acl_event("[1:2::]", e))
        self.assertTrue(server_matches_acl_event("1:2:3:4", e))

    def test_wildcard_matching(self):
        e = _create_acl_event({"allow": ["good*.com"]})
        self.assertTrue(
            server_matches_acl_event("good.com", e),
            "* matches 0 characters",
        )
        self.assertTrue(
            server_matches_acl_event("GOOD.COM", e),
            "pattern is case-insensitive",
        )
        self.assertTrue(
            server_matches_acl_event("good.aa.com", e),
            "* matches several characters, including '.'",
        )
        self.assertFalse(
            server_matches_acl_event("ishgood.com", e),
            "pattern does not allow prefixes",
        )


class StateQueryTests(unittest.FederatingHomeserverTestCase):

    servlets = [
        admin.register_servlets,
        room.register_servlets,
        login.register_servlets,
    ]

    def test_without_event_id(self):
        """
        Querying v1/state/<room_id> without an event ID will return the current
        known state.
        """
        u1 = self.register_user("u1", "pass")
        u1_token = self.login("u1", "pass")

        room_1 = self.helper.create_room_as(u1, tok=u1_token)
        self.inject_room_member(room_1, "@user:other.example.com", "join")

        channel = self.make_request(
            "GET", "/_matrix/federation/v1/state/%s" % (room_1,)
        )
        self.assertEquals(200, channel.code, channel.result)

        self.assertEqual(
            channel.json_body["room_version"],
            self.hs.config.server.default_room_version.identifier,
        )

        members = set(
            map(
                lambda x: x["state_key"],
                filter(
                    lambda x: x["type"] == "m.room.member", channel.json_body["pdus"]
                ),
            )
        )

        self.assertEqual(members, {"@user:other.example.com", u1})
        self.assertEqual(len(channel.json_body["pdus"]), 6)

    def test_needs_to_be_in_room(self):
        """
        Querying v1/state/<room_id> requires the server
        be in the room to provide data.
        """
        u1 = self.register_user("u1", "pass")
        u1_token = self.login("u1", "pass")

        room_1 = self.helper.create_room_as(u1, tok=u1_token)

        channel = self.make_request(
            "GET", "/_matrix/federation/v1/state/%s" % (room_1,)
        )
        self.assertEquals(403, channel.code, channel.result)
        self.assertEqual(channel.json_body["errcode"], "M_FORBIDDEN")


def _create_acl_event(content):
    return make_event_from_dict(
        {
            "room_id": "!a:b",
            "event_id": "$a:b",
            "type": "m.room.server_acls",
            "sender": "@a:b",
            "content": content,
        }
    )
