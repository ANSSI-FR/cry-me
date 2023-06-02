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
# Copyright 2014-2021 The Matrix.org Foundation C.I.C.
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

from synapse.types import RoomAlias, RoomID

from tests.unittest import HomeserverTestCase


class DirectoryStoreTestCase(HomeserverTestCase):
    def prepare(self, reactor, clock, hs):
        self.store = hs.get_datastore()

        self.room = RoomID.from_string("!abcde:test")
        self.alias = RoomAlias.from_string("#my-room:test")

    def test_room_to_alias(self):
        self.get_success(
            self.store.create_room_alias_association(
                room_alias=self.alias, room_id=self.room.to_string(), servers=["test"]
            )
        )

        self.assertEquals(
            ["#my-room:test"],
            (self.get_success(self.store.get_aliases_for_room(self.room.to_string()))),
        )

    def test_alias_to_room(self):
        self.get_success(
            self.store.create_room_alias_association(
                room_alias=self.alias, room_id=self.room.to_string(), servers=["test"]
            )
        )

        self.assertObjectHasAttributes(
            {"room_id": self.room.to_string(), "servers": ["test"]},
            (self.get_success(self.store.get_association_from_room_alias(self.alias))),
        )

    def test_delete_alias(self):
        self.get_success(
            self.store.create_room_alias_association(
                room_alias=self.alias, room_id=self.room.to_string(), servers=["test"]
            )
        )

        room_id = self.get_success(self.store.delete_room_alias(self.alias))
        self.assertEqual(self.room.to_string(), room_id)

        self.assertIsNone(
            self.get_success(self.store.get_association_from_room_alias(self.alias))
        )
