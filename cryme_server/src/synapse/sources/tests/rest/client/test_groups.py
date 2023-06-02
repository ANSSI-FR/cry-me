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

from synapse.rest.client import groups, room

from tests import unittest
from tests.unittest import override_config


class GroupsTestCase(unittest.HomeserverTestCase):
    user_id = "@alice:test"
    room_creator_user_id = "@bob:test"

    servlets = [room.register_servlets, groups.register_servlets]

    @override_config({"enable_group_creation": True})
    def test_rooms_limited_by_visibility(self):
        group_id = "+spqr:test"

        # Alice creates a group
        channel = self.make_request("POST", "/create_group", {"localpart": "spqr"})
        self.assertEquals(channel.code, 200, msg=channel.text_body)
        self.assertEquals(channel.json_body, {"group_id": group_id})

        # Bob creates a private room
        room_id = self.helper.create_room_as(self.room_creator_user_id, is_public=False)
        self.helper.auth_user_id = self.room_creator_user_id
        self.helper.send_state(
            room_id, "m.room.name", {"name": "bob's secret room"}, tok=None
        )
        self.helper.auth_user_id = self.user_id

        # Alice adds the room to her group.
        channel = self.make_request(
            "PUT", f"/groups/{group_id}/admin/rooms/{room_id}", {}
        )
        self.assertEquals(channel.code, 200, msg=channel.text_body)
        self.assertEquals(channel.json_body, {})

        # Alice now tries to retrieve the room list of the space.
        channel = self.make_request("GET", f"/groups/{group_id}/rooms")
        self.assertEquals(channel.code, 200, msg=channel.text_body)
        self.assertEquals(
            channel.json_body, {"chunk": [], "total_room_count_estimate": 0}
        )
