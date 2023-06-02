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
# Copyright 2019 The Matrix.org Foundation C.I.C.
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

from synapse.storage.databases.main.e2e_room_keys import RoomKey

from tests import unittest

# sample room_key data for use in the tests
room_key: RoomKey = {
    "first_message_index": 1,
    "forwarded_count": 1,
    "is_verified": False,
    "session_data": "SSBBTSBBIEZJU0gK",
}


class E2eRoomKeysHandlerTestCase(unittest.HomeserverTestCase):
    def make_homeserver(self, reactor, clock):
        hs = self.setup_test_homeserver("server", federation_http_client=None)
        self.store = hs.get_datastore()
        return hs

    def test_room_keys_version_delete(self):
        # test that deleting a room key backup deletes the keys
        version1 = self.get_success(
            self.store.create_e2e_room_keys_version(
                "user_id", {"algorithm": "rot13", "auth_data": {}}
            )
        )

        self.get_success(
            self.store.add_e2e_room_keys(
                "user_id", version1, [("room", "session", room_key)]
            )
        )

        version2 = self.get_success(
            self.store.create_e2e_room_keys_version(
                "user_id", {"algorithm": "rot13", "auth_data": {}}
            )
        )

        self.get_success(
            self.store.add_e2e_room_keys(
                "user_id", version2, [("room", "session", room_key)]
            )
        )

        # make sure the keys were stored properly
        keys = self.get_success(self.store.get_e2e_room_keys("user_id", version1))
        self.assertEqual(len(keys["rooms"]), 1)

        keys = self.get_success(self.store.get_e2e_room_keys("user_id", version2))
        self.assertEqual(len(keys["rooms"]), 1)

        # delete version1
        self.get_success(self.store.delete_e2e_room_keys_version("user_id", version1))

        # make sure the key from version1 is gone, and the key from version2 is
        # still there
        keys = self.get_success(self.store.get_e2e_room_keys("user_id", version1))
        self.assertEqual(len(keys["rooms"]), 0)

        keys = self.get_success(self.store.get_e2e_room_keys("user_id", version2))
        self.assertEqual(len(keys["rooms"]), 1)
