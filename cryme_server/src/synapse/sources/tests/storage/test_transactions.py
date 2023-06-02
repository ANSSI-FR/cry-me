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
# Copyright 2018 New Vector Ltd
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

from synapse.storage.databases.main.transactions import DestinationRetryTimings
from synapse.util.retryutils import MAX_RETRY_INTERVAL

from tests.unittest import HomeserverTestCase


class TransactionStoreTestCase(HomeserverTestCase):
    def prepare(self, reactor, clock, homeserver):
        self.store = homeserver.get_datastore()

    def test_get_set_transactions(self):
        """Tests that we can successfully get a non-existent entry for
        destination retries, as well as testing tht we can set and get
        correctly.
        """
        d = self.store.get_destination_retry_timings("example.com")
        r = self.get_success(d)
        self.assertIsNone(r)

        d = self.store.set_destination_retry_timings("example.com", 1000, 50, 100)
        self.get_success(d)

        d = self.store.get_destination_retry_timings("example.com")
        r = self.get_success(d)

        self.assertEqual(
            DestinationRetryTimings(
                retry_last_ts=50, retry_interval=100, failure_ts=1000
            ),
            r,
        )

    def test_initial_set_transactions(self):
        """Tests that we can successfully set the destination retries (there
        was a bug around invalidating the cache that broke this)
        """
        d = self.store.set_destination_retry_timings("example.com", 1000, 50, 100)
        self.get_success(d)

    def test_large_destination_retry(self):
        d = self.store.set_destination_retry_timings(
            "example.com", MAX_RETRY_INTERVAL, MAX_RETRY_INTERVAL, MAX_RETRY_INTERVAL
        )
        self.get_success(d)

        d = self.store.get_destination_retry_timings("example.com")
        self.get_success(d)
