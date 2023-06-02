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


from synapse.util.caches.dictionary_cache import DictionaryCache

from tests import unittest


class DictCacheTestCase(unittest.TestCase):
    def setUp(self):
        self.cache = DictionaryCache("foobar")

    def test_simple_cache_hit_full(self):
        key = "test_simple_cache_hit_full"

        v = self.cache.get(key)
        self.assertIs(v.full, False)
        self.assertEqual(v.known_absent, set())
        self.assertEqual({}, v.value)

        seq = self.cache.sequence
        test_value = {"test": "test_simple_cache_hit_full"}
        self.cache.update(seq, key, test_value)

        c = self.cache.get(key)
        self.assertEqual(test_value, c.value)

    def test_simple_cache_hit_partial(self):
        key = "test_simple_cache_hit_partial"

        seq = self.cache.sequence
        test_value = {"test": "test_simple_cache_hit_partial"}
        self.cache.update(seq, key, test_value)

        c = self.cache.get(key, ["test"])
        self.assertEqual(test_value, c.value)

    def test_simple_cache_miss_partial(self):
        key = "test_simple_cache_miss_partial"

        seq = self.cache.sequence
        test_value = {"test": "test_simple_cache_miss_partial"}
        self.cache.update(seq, key, test_value)

        c = self.cache.get(key, ["test2"])
        self.assertEqual({}, c.value)

    def test_simple_cache_hit_miss_partial(self):
        key = "test_simple_cache_hit_miss_partial"

        seq = self.cache.sequence
        test_value = {
            "test": "test_simple_cache_hit_miss_partial",
            "test2": "test_simple_cache_hit_miss_partial2",
            "test3": "test_simple_cache_hit_miss_partial3",
        }
        self.cache.update(seq, key, test_value)

        c = self.cache.get(key, ["test2"])
        self.assertEqual({"test2": "test_simple_cache_hit_miss_partial2"}, c.value)

    def test_multi_insert(self):
        key = "test_simple_cache_hit_miss_partial"

        seq = self.cache.sequence
        test_value_1 = {"test": "test_simple_cache_hit_miss_partial"}
        self.cache.update(seq, key, test_value_1, fetched_keys=set("test"))

        seq = self.cache.sequence
        test_value_2 = {"test2": "test_simple_cache_hit_miss_partial2"}
        self.cache.update(seq, key, test_value_2, fetched_keys=set("test2"))

        c = self.cache.get(key)
        self.assertEqual(
            {
                "test": "test_simple_cache_hit_miss_partial",
                "test2": "test_simple_cache_hit_miss_partial2",
            },
            c.value,
        )
