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
# Copyright 2019 New Vector Ltd
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

from unittest.mock import Mock

from synapse.util.caches.ttlcache import TTLCache

from tests import unittest


class CacheTestCase(unittest.TestCase):
    def setUp(self):
        self.mock_timer = Mock(side_effect=lambda: 100.0)
        self.cache = TTLCache("test_cache", self.mock_timer)

    def test_get(self):
        """simple set/get tests"""
        self.cache.set("one", "1", 10)
        self.cache.set("two", "2", 20)
        self.cache.set("three", "3", 30)

        self.assertEqual(len(self.cache), 3)

        self.assertTrue("one" in self.cache)
        self.assertEqual(self.cache.get("one"), "1")
        self.assertEqual(self.cache["one"], "1")
        self.assertEqual(self.cache.get_with_expiry("one"), ("1", 110, 10))
        self.assertEqual(self.cache._metrics.hits, 3)
        self.assertEqual(self.cache._metrics.misses, 0)

        self.cache.set("two", "2.5", 20)
        self.assertEqual(self.cache["two"], "2.5")
        self.assertEqual(self.cache._metrics.hits, 4)

        # non-existent-item tests
        self.assertEqual(self.cache.get("four", "4"), "4")
        self.assertIs(self.cache.get("four", None), None)

        with self.assertRaises(KeyError):
            self.cache["four"]

        with self.assertRaises(KeyError):
            self.cache.get("four")

        with self.assertRaises(KeyError):
            self.cache.get_with_expiry("four")

        self.assertEqual(self.cache._metrics.hits, 4)
        self.assertEqual(self.cache._metrics.misses, 5)

    def test_expiry(self):
        self.cache.set("one", "1", 10)
        self.cache.set("two", "2", 20)
        self.cache.set("three", "3", 30)

        self.assertEqual(len(self.cache), 3)
        self.assertEqual(self.cache["one"], "1")
        self.assertEqual(self.cache["two"], "2")

        # enough for the first entry to expire, but not the rest
        self.mock_timer.side_effect = lambda: 110.0

        self.assertEqual(len(self.cache), 2)
        self.assertFalse("one" in self.cache)
        self.assertEqual(self.cache["two"], "2")
        self.assertEqual(self.cache["three"], "3")

        self.assertEqual(self.cache.get_with_expiry("two"), ("2", 120, 20))

        self.assertEqual(self.cache._metrics.hits, 5)
        self.assertEqual(self.cache._metrics.misses, 0)
