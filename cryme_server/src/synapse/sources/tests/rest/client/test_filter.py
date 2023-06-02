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
# Copyright 2015, 2016 OpenMarket Ltd
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

from twisted.internet import defer

from synapse.api.errors import Codes
from synapse.rest.client import filter

from tests import unittest

PATH_PREFIX = "/_matrix/client/v2_alpha"


class FilterTestCase(unittest.HomeserverTestCase):

    user_id = "@apple:test"
    hijack_auth = True
    EXAMPLE_FILTER = {"room": {"timeline": {"types": ["m.room.message"]}}}
    EXAMPLE_FILTER_JSON = b'{"room": {"timeline": {"types": ["m.room.message"]}}}'
    servlets = [filter.register_servlets]

    def prepare(self, reactor, clock, hs):
        self.filtering = hs.get_filtering()
        self.store = hs.get_datastore()

    def test_add_filter(self):
        channel = self.make_request(
            "POST",
            "/_matrix/client/r0/user/%s/filter" % (self.user_id),
            self.EXAMPLE_FILTER_JSON,
        )

        self.assertEqual(channel.result["code"], b"200")
        self.assertEqual(channel.json_body, {"filter_id": "0"})
        filter = self.store.get_user_filter(user_localpart="apple", filter_id=0)
        self.pump()
        self.assertEquals(filter.result, self.EXAMPLE_FILTER)

    def test_add_filter_for_other_user(self):
        channel = self.make_request(
            "POST",
            "/_matrix/client/r0/user/%s/filter" % ("@watermelon:test"),
            self.EXAMPLE_FILTER_JSON,
        )

        self.assertEqual(channel.result["code"], b"403")
        self.assertEquals(channel.json_body["errcode"], Codes.FORBIDDEN)

    def test_add_filter_non_local_user(self):
        _is_mine = self.hs.is_mine
        self.hs.is_mine = lambda target_user: False
        channel = self.make_request(
            "POST",
            "/_matrix/client/r0/user/%s/filter" % (self.user_id),
            self.EXAMPLE_FILTER_JSON,
        )

        self.hs.is_mine = _is_mine
        self.assertEqual(channel.result["code"], b"403")
        self.assertEquals(channel.json_body["errcode"], Codes.FORBIDDEN)

    def test_get_filter(self):
        filter_id = defer.ensureDeferred(
            self.filtering.add_user_filter(
                user_localpart="apple", user_filter=self.EXAMPLE_FILTER
            )
        )
        self.reactor.advance(1)
        filter_id = filter_id.result
        channel = self.make_request(
            "GET", "/_matrix/client/r0/user/%s/filter/%s" % (self.user_id, filter_id)
        )

        self.assertEqual(channel.result["code"], b"200")
        self.assertEquals(channel.json_body, self.EXAMPLE_FILTER)

    def test_get_filter_non_existant(self):
        channel = self.make_request(
            "GET", "/_matrix/client/r0/user/%s/filter/12382148321" % (self.user_id)
        )

        self.assertEqual(channel.result["code"], b"404")
        self.assertEquals(channel.json_body["errcode"], Codes.NOT_FOUND)

    # Currently invalid params do not have an appropriate errcode
    # in errors.py
    def test_get_filter_invalid_id(self):
        channel = self.make_request(
            "GET", "/_matrix/client/r0/user/%s/filter/foobar" % (self.user_id)
        )

        self.assertEqual(channel.result["code"], b"400")

    # No ID also returns an invalid_id error
    def test_get_filter_no_id(self):
        channel = self.make_request(
            "GET", "/_matrix/client/r0/user/%s/filter/" % (self.user_id)
        )

        self.assertEqual(channel.result["code"], b"400")
