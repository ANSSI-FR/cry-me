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
# Copyright 2018 New Vector
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

"""Tests REST events for /rooms paths."""

from unittest.mock import Mock

from synapse.rest.client import room
from synapse.types import UserID

from tests import unittest

PATH_PREFIX = "/_matrix/client/api/v1"


class RoomTypingTestCase(unittest.HomeserverTestCase):
    """Tests /rooms/$room_id/typing/$user_id REST API."""

    user_id = "@sid:red"

    user = UserID.from_string(user_id)
    servlets = [room.register_servlets]

    def make_homeserver(self, reactor, clock):

        hs = self.setup_test_homeserver(
            "red",
            federation_http_client=None,
            federation_client=Mock(),
        )

        self.event_source = hs.get_event_sources().sources.typing

        hs.get_federation_handler = Mock()

        async def get_user_by_access_token(token=None, allow_guest=False):
            return {
                "user": UserID.from_string(self.auth_user_id),
                "token_id": 1,
                "is_guest": False,
            }

        hs.get_auth().get_user_by_access_token = get_user_by_access_token

        async def _insert_client_ip(*args, **kwargs):
            return None

        hs.get_datastore().insert_client_ip = _insert_client_ip

        return hs

    def prepare(self, reactor, clock, hs):
        self.room_id = self.helper.create_room_as(self.user_id)
        # Need another user to make notifications actually work
        self.helper.join(self.room_id, user="@jim:red")

    def test_set_typing(self):
        channel = self.make_request(
            "PUT",
            "/rooms/%s/typing/%s" % (self.room_id, self.user_id),
            b'{"typing": true, "timeout": 30000}',
        )
        self.assertEquals(200, channel.code)

        self.assertEquals(self.event_source.get_current_key(), 1)
        events = self.get_success(
            self.event_source.get_new_events(
                user=UserID.from_string(self.user_id),
                from_key=0,
                limit=None,
                room_ids=[self.room_id],
                is_guest=False,
            )
        )
        self.assertEquals(
            events[0],
            [
                {
                    "type": "m.typing",
                    "room_id": self.room_id,
                    "content": {"user_ids": [self.user_id]},
                }
            ],
        )

    def test_set_not_typing(self):
        channel = self.make_request(
            "PUT",
            "/rooms/%s/typing/%s" % (self.room_id, self.user_id),
            b'{"typing": false}',
        )
        self.assertEquals(200, channel.code)

    def test_typing_timeout(self):
        channel = self.make_request(
            "PUT",
            "/rooms/%s/typing/%s" % (self.room_id, self.user_id),
            b'{"typing": true, "timeout": 30000}',
        )
        self.assertEquals(200, channel.code)

        self.assertEquals(self.event_source.get_current_key(), 1)

        self.reactor.advance(36)

        self.assertEquals(self.event_source.get_current_key(), 2)

        channel = self.make_request(
            "PUT",
            "/rooms/%s/typing/%s" % (self.room_id, self.user_id),
            b'{"typing": true, "timeout": 30000}',
        )
        self.assertEquals(200, channel.code)

        self.assertEquals(self.event_source.get_current_key(), 3)
