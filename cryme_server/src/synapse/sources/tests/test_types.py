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

from synapse.api.errors import SynapseError
from synapse.types import GroupID, RoomAlias, UserID, map_username_to_mxid_localpart

from tests import unittest


class UserIDTestCase(unittest.HomeserverTestCase):
    def test_parse(self):
        user = UserID.from_string("@1234abcd:test")

        self.assertEquals("1234abcd", user.localpart)
        self.assertEquals("test", user.domain)
        self.assertEquals(True, self.hs.is_mine(user))

    def test_pase_empty(self):
        with self.assertRaises(SynapseError):
            UserID.from_string("")

    def test_build(self):
        user = UserID("5678efgh", "my.domain")

        self.assertEquals(user.to_string(), "@5678efgh:my.domain")

    def test_compare(self):
        userA = UserID.from_string("@userA:my.domain")
        userAagain = UserID.from_string("@userA:my.domain")
        userB = UserID.from_string("@userB:my.domain")

        self.assertTrue(userA == userAagain)
        self.assertTrue(userA != userB)


class RoomAliasTestCase(unittest.HomeserverTestCase):
    def test_parse(self):
        room = RoomAlias.from_string("#channel:test")

        self.assertEquals("channel", room.localpart)
        self.assertEquals("test", room.domain)
        self.assertEquals(True, self.hs.is_mine(room))

    def test_build(self):
        room = RoomAlias("channel", "my.domain")

        self.assertEquals(room.to_string(), "#channel:my.domain")

    def test_validate(self):
        id_string = "#test:domain,test"
        self.assertFalse(RoomAlias.is_valid(id_string))


class GroupIDTestCase(unittest.TestCase):
    def test_parse(self):
        group_id = GroupID.from_string("+group/=_-.123:my.domain")
        self.assertEqual("group/=_-.123", group_id.localpart)
        self.assertEqual("my.domain", group_id.domain)

    def test_validate(self):
        bad_ids = ["$badsigil:domain", "+:empty"] + [
            "+group" + c + ":domain" for c in "A%?æ£"
        ]
        for id_string in bad_ids:
            try:
                GroupID.from_string(id_string)
                self.fail("Parsing '%s' should raise exception" % id_string)
            except SynapseError as exc:
                self.assertEqual(400, exc.code)
                self.assertEqual("M_INVALID_PARAM", exc.errcode)


class MapUsernameTestCase(unittest.TestCase):
    def testPassThrough(self):
        self.assertEqual(map_username_to_mxid_localpart("test1234"), "test1234")

    def testUpperCase(self):
        self.assertEqual(map_username_to_mxid_localpart("tEST_1234"), "test_1234")
        self.assertEqual(
            map_username_to_mxid_localpart("tEST_1234", case_sensitive=True),
            "t_e_s_t__1234",
        )

    def testSymbols(self):
        self.assertEqual(
            map_username_to_mxid_localpart("test=$?_1234"), "test=3d=24=3f_1234"
        )

    def testLeadingUnderscore(self):
        self.assertEqual(map_username_to_mxid_localpart("_test_1234"), "=5ftest_1234")

    def testNonAscii(self):
        # this should work with either a unicode or a bytes
        self.assertEqual(map_username_to_mxid_localpart("têst"), "t=c3=aast")
        self.assertEqual(map_username_to_mxid_localpart("têst".encode()), "t=c3=aast")
