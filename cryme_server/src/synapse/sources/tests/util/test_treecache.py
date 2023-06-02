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


from synapse.util.caches.treecache import TreeCache, iterate_tree_cache_entry

from .. import unittest


class TreeCacheTestCase(unittest.TestCase):
    def test_get_set_onelevel(self):
        cache = TreeCache()
        cache[("a",)] = "A"
        cache[("b",)] = "B"
        self.assertEquals(cache.get(("a",)), "A")
        self.assertEquals(cache.get(("b",)), "B")
        self.assertEquals(len(cache), 2)

    def test_pop_onelevel(self):
        cache = TreeCache()
        cache[("a",)] = "A"
        cache[("b",)] = "B"
        self.assertEquals(cache.pop(("a",)), "A")
        self.assertEquals(cache.pop(("a",)), None)
        self.assertEquals(cache.get(("b",)), "B")
        self.assertEquals(len(cache), 1)

    def test_get_set_twolevel(self):
        cache = TreeCache()
        cache[("a", "a")] = "AA"
        cache[("a", "b")] = "AB"
        cache[("b", "a")] = "BA"
        self.assertEquals(cache.get(("a", "a")), "AA")
        self.assertEquals(cache.get(("a", "b")), "AB")
        self.assertEquals(cache.get(("b", "a")), "BA")
        self.assertEquals(len(cache), 3)

    def test_pop_twolevel(self):
        cache = TreeCache()
        cache[("a", "a")] = "AA"
        cache[("a", "b")] = "AB"
        cache[("b", "a")] = "BA"
        self.assertEquals(cache.pop(("a", "a")), "AA")
        self.assertEquals(cache.get(("a", "a")), None)
        self.assertEquals(cache.get(("a", "b")), "AB")
        self.assertEquals(cache.pop(("b", "a")), "BA")
        self.assertEquals(cache.pop(("b", "a")), None)
        self.assertEquals(len(cache), 1)

    def test_pop_mixedlevel(self):
        cache = TreeCache()
        cache[("a", "a")] = "AA"
        cache[("a", "b")] = "AB"
        cache[("b", "a")] = "BA"
        self.assertEquals(cache.get(("a", "a")), "AA")
        popped = cache.pop(("a",))
        self.assertEquals(cache.get(("a", "a")), None)
        self.assertEquals(cache.get(("a", "b")), None)
        self.assertEquals(cache.get(("b", "a")), "BA")
        self.assertEquals(len(cache), 1)

        self.assertEquals({"AA", "AB"}, set(iterate_tree_cache_entry(popped)))

    def test_clear(self):
        cache = TreeCache()
        cache[("a",)] = "A"
        cache[("b",)] = "B"
        cache.clear()
        self.assertEquals(len(cache), 0)

    def test_contains(self):
        cache = TreeCache()
        cache[("a",)] = "A"
        self.assertTrue(("a",) in cache)
        self.assertFalse(("b",) in cache)
