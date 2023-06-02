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

from synapse.server import HomeServer
from synapse.storage.databases.main.lock import _LOCK_TIMEOUT_MS

from tests import unittest


class LockTestCase(unittest.HomeserverTestCase):
    def prepare(self, reactor, clock, hs: HomeServer):
        self.store = hs.get_datastore()

    def test_simple_lock(self):
        """Test that we can take out a lock and that while we hold it nobody
        else can take it out.
        """
        # First to acquire this lock, so it should complete
        lock = self.get_success(self.store.try_acquire_lock("name", "key"))
        self.assertIsNotNone(lock)

        # Enter the context manager
        self.get_success(lock.__aenter__())

        # Attempting to acquire the lock again fails.
        lock2 = self.get_success(self.store.try_acquire_lock("name", "key"))
        self.assertIsNone(lock2)

        # Calling `is_still_valid` reports true.
        self.assertTrue(self.get_success(lock.is_still_valid()))

        # Drop the lock
        self.get_success(lock.__aexit__(None, None, None))

        # We can now acquire the lock again.
        lock3 = self.get_success(self.store.try_acquire_lock("name", "key"))
        self.assertIsNotNone(lock3)
        self.get_success(lock3.__aenter__())
        self.get_success(lock3.__aexit__(None, None, None))

    def test_maintain_lock(self):
        """Test that we don't time out locks while they're still active"""

        lock = self.get_success(self.store.try_acquire_lock("name", "key"))
        self.assertIsNotNone(lock)

        self.get_success(lock.__aenter__())

        # Wait for ages with the lock, we should not be able to get the lock.
        self.reactor.advance(5 * _LOCK_TIMEOUT_MS / 1000)

        lock2 = self.get_success(self.store.try_acquire_lock("name", "key"))
        self.assertIsNone(lock2)

        self.get_success(lock.__aexit__(None, None, None))

    def test_timeout_lock(self):
        """Test that we time out locks if they're not updated for ages"""

        lock = self.get_success(self.store.try_acquire_lock("name", "key"))
        self.assertIsNotNone(lock)

        self.get_success(lock.__aenter__())

        # We simulate the process getting stuck by cancelling the looping call
        # that keeps the lock active.
        lock._looping_call.stop()

        # Wait for the lock to timeout.
        self.reactor.advance(2 * _LOCK_TIMEOUT_MS / 1000)

        lock2 = self.get_success(self.store.try_acquire_lock("name", "key"))
        self.assertIsNotNone(lock2)

        self.assertFalse(self.get_success(lock.is_still_valid()))

    def test_drop(self):
        """Test that dropping the context manager means we stop renewing the lock"""

        lock = self.get_success(self.store.try_acquire_lock("name", "key"))
        self.assertIsNotNone(lock)

        del lock

        # Wait for the lock to timeout.
        self.reactor.advance(2 * _LOCK_TIMEOUT_MS / 1000)

        lock2 = self.get_success(self.store.try_acquire_lock("name", "key"))
        self.assertIsNotNone(lock2)

    def test_shutdown(self):
        """Test that shutting down Synapse releases the locks"""
        # Acquire two locks
        lock = self.get_success(self.store.try_acquire_lock("name", "key1"))
        self.assertIsNotNone(lock)
        lock2 = self.get_success(self.store.try_acquire_lock("name", "key2"))
        self.assertIsNotNone(lock2)

        # Now call the shutdown code
        self.get_success(self.store._on_shutdown())

        self.assertEqual(self.store._live_tokens, {})
