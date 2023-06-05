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

""" Tests REST events for /events paths."""

from unittest.mock import Mock

import synapse.rest.admin
from synapse.rest.client import events, login, room

from tests import unittest


class EventStreamPermissionsTestCase(unittest.HomeserverTestCase):
    """Tests event streaming (GET /events)."""

    servlets = [
        events.register_servlets,
        room.register_servlets,
        synapse.rest.admin.register_servlets_for_client_rest_resource,
        login.register_servlets,
    ]

    def make_homeserver(self, reactor, clock):

        config = self.default_config()
        config["enable_registration_captcha"] = False
        config["enable_registration"] = True
        config["auto_join_rooms"] = []

        hs = self.setup_test_homeserver(config=config)

        hs.get_federation_handler = Mock()

        return hs

    def prepare(self, reactor, clock, hs):

        # register an account
        self.user_id = self.register_user("sid1", "pass")
        self.token = self.login(self.user_id, "pass")

        # register a 2nd account
        self.other_user = self.register_user("other2", "pass")
        self.other_token = self.login(self.other_user, "pass")

    def test_stream_basic_permissions(self):
        # invalid token, expect 401
        # note: this is in violation of the original v1 spec, which expected
        # 403. However, since the v1 spec no longer exists and the v1
        # implementation is now part of the r0 implementation, the newer
        # behaviour is used instead to be consistent with the r0 spec.
        # see issue #2602
        channel = self.make_request(
            "GET", "/events?access_token=%s" % ("invalid" + self.token,)
        )
        self.assertEquals(channel.code, 401, msg=channel.result)

        # valid token, expect content
        channel = self.make_request(
            "GET", "/events?access_token=%s&timeout=0" % (self.token,)
        )
        self.assertEquals(channel.code, 200, msg=channel.result)
        self.assertTrue("chunk" in channel.json_body)
        self.assertTrue("start" in channel.json_body)
        self.assertTrue("end" in channel.json_body)

    def test_stream_room_permissions(self):
        room_id = self.helper.create_room_as(self.other_user, tok=self.other_token)
        self.helper.send(room_id, tok=self.other_token)

        # invited to room (expect no content for room)
        self.helper.invite(
            room_id, src=self.other_user, targ=self.user_id, tok=self.other_token
        )

        # valid token, expect content
        channel = self.make_request(
            "GET", "/events?access_token=%s&timeout=0" % (self.token,)
        )
        self.assertEquals(channel.code, 200, msg=channel.result)

        # We may get a presence event for ourselves down
        self.assertEquals(
            0,
            len(
                [
                    c
                    for c in channel.json_body["chunk"]
                    if not (
                        c.get("type") == "m.presence"
                        and c["content"].get("user_id") == self.user_id
                    )
                ]
            ),
        )

        # joined room (expect all content for room)
        self.helper.join(room=room_id, user=self.user_id, tok=self.token)

        # left to room (expect no content for room)

    def TODO_test_stream_items(self):
        # new user, no content

        # join room, expect 1 item (join)

        # send message, expect 2 items (join,send)

        # set topic, expect 3 items (join,send,topic)

        # someone else join room, expect 4 (join,send,topic,join)

        # someone else send message, expect 5 (join,send.topic,join,send)

        # someone else set topic, expect 6 (join,send,topic,join,send,topic)
        pass


class GetEventsTestCase(unittest.HomeserverTestCase):
    servlets = [
        events.register_servlets,
        room.register_servlets,
        synapse.rest.admin.register_servlets_for_client_rest_resource,
        login.register_servlets,
    ]

    def prepare(self, hs, reactor, clock):

        # register an account
        self.user_id = self.register_user("sid1", "pass")
        self.token = self.login(self.user_id, "pass")

        self.room_id = self.helper.create_room_as(self.user_id, tok=self.token)

    def test_get_event_via_events(self):
        resp = self.helper.send(self.room_id, tok=self.token)
        event_id = resp["event_id"]

        channel = self.make_request(
            "GET",
            "/events/" + event_id,
            access_token=self.token,
        )
        self.assertEquals(channel.code, 200, msg=channel.result)
