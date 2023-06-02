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
# Copyright 2017 OpenMarket Ltd
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


from synapse.util.caches.expiringcache import ExpiringCache

from tests.utils import MockClock

from .. import unittest


class ExpiringCacheTestCase(unittest.HomeserverTestCase):
    def test_get_set(self):
        clock = MockClock()
        cache = ExpiringCache("test", clock, max_len=1)

        cache["key"] = "value"
        self.assertEquals(cache.get("key"), "value")
        self.assertEquals(cache["key"], "value")

    def test_eviction(self):
        clock = MockClock()
        cache = ExpiringCache("test", clock, max_len=2)

        cache["key"] = "value"
        cache["key2"] = "value2"
        self.assertEquals(cache.get("key"), "value")
        self.assertEquals(cache.get("key2"), "value2")

        cache["key3"] = "value3"
        self.assertEquals(cache.get("key"), None)
        self.assertEquals(cache.get("key2"), "value2")
        self.assertEquals(cache.get("key3"), "value3")

    def test_iterable_eviction(self):
        clock = MockClock()
        cache = ExpiringCache("test", clock, max_len=5, iterable=True)

        cache["key"] = [1]
        cache["key2"] = [2, 3]
        cache["key3"] = [4, 5]

        self.assertEquals(cache.get("key"), [1])
        self.assertEquals(cache.get("key2"), [2, 3])
        self.assertEquals(cache.get("key3"), [4, 5])

        cache["key4"] = [6, 7]
        self.assertEquals(cache.get("key"), None)
        self.assertEquals(cache.get("key2"), None)
        self.assertEquals(cache.get("key3"), [4, 5])
        self.assertEquals(cache.get("key4"), [6, 7])

    def test_time_eviction(self):
        clock = MockClock()
        cache = ExpiringCache("test", clock, expiry_ms=1000)

        cache["key"] = 1
        clock.advance_time(0.5)
        cache["key2"] = 2

        self.assertEquals(cache.get("key"), 1)
        self.assertEquals(cache.get("key2"), 2)

        clock.advance_time(0.9)
        self.assertEquals(cache.get("key"), None)
        self.assertEquals(cache.get("key2"), 2)

        clock.advance_time(1)
        self.assertEquals(cache.get("key"), None)
        self.assertEquals(cache.get("key2"), None)
