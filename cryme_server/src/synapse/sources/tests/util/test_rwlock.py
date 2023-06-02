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
# Copyright 2016 OpenMarket Ltd
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

from synapse.util.async_helpers import ReadWriteLock

from tests import unittest


class ReadWriteLockTestCase(unittest.TestCase):
    def _assert_called_before_not_after(self, lst, first_false):
        for i, d in enumerate(lst[:first_false]):
            self.assertTrue(d.called, msg="%d was unexpectedly false" % i)

        for i, d in enumerate(lst[first_false:]):
            self.assertFalse(
                d.called, msg="%d was unexpectedly true" % (i + first_false)
            )

    def test_rwlock(self):
        rwlock = ReadWriteLock()

        key = object()

        ds = [
            rwlock.read(key),  # 0
            rwlock.read(key),  # 1
            rwlock.write(key),  # 2
            rwlock.write(key),  # 3
            rwlock.read(key),  # 4
            rwlock.read(key),  # 5
            rwlock.write(key),  # 6
        ]
        ds = [defer.ensureDeferred(d) for d in ds]

        self._assert_called_before_not_after(ds, 2)

        with ds[0].result:
            self._assert_called_before_not_after(ds, 2)
        self._assert_called_before_not_after(ds, 2)

        with ds[1].result:
            self._assert_called_before_not_after(ds, 2)
        self._assert_called_before_not_after(ds, 3)

        with ds[2].result:
            self._assert_called_before_not_after(ds, 3)
        self._assert_called_before_not_after(ds, 4)

        with ds[3].result:
            self._assert_called_before_not_after(ds, 4)
        self._assert_called_before_not_after(ds, 6)

        with ds[5].result:
            self._assert_called_before_not_after(ds, 6)
        self._assert_called_before_not_after(ds, 6)

        with ds[4].result:
            self._assert_called_before_not_after(ds, 6)
        self._assert_called_before_not_after(ds, 7)

        with ds[6].result:
            pass

        d = defer.ensureDeferred(rwlock.write(key))
        self.assertTrue(d.called)
        with d.result:
            pass

        d = defer.ensureDeferred(rwlock.read(key))
        self.assertTrue(d.called)
        with d.result:
            pass
