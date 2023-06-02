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

# type: ignore

from unittest.mock import Mock

from synapse.replication.tcp.streams._base import ReceiptsStream

from tests.replication._base import BaseStreamTestCase

USER_ID = "@feeling:blue"


class ReceiptsStreamTestCase(BaseStreamTestCase):
    def _build_replication_data_handler(self):
        return Mock(wraps=super()._build_replication_data_handler())

    def test_receipt(self):
        self.reconnect()

        # tell the master to send a new receipt
        self.get_success(
            self.hs.get_datastore().insert_receipt(
                "!room:blue", "m.read", USER_ID, ["$event:blue"], {"a": 1}
            )
        )
        self.replicate()

        # there should be one RDATA command
        self.test_handler.on_rdata.assert_called_once()
        stream_name, _, token, rdata_rows = self.test_handler.on_rdata.call_args[0]
        self.assertEqual(stream_name, "receipts")
        self.assertEqual(1, len(rdata_rows))
        row: ReceiptsStream.ReceiptsStreamRow = rdata_rows[0]
        self.assertEqual("!room:blue", row.room_id)
        self.assertEqual("m.read", row.receipt_type)
        self.assertEqual(USER_ID, row.user_id)
        self.assertEqual("$event:blue", row.event_id)
        self.assertEqual({"a": 1}, row.data)

        # Now let's disconnect and insert some data.
        self.disconnect()

        self.test_handler.on_rdata.reset_mock()

        self.get_success(
            self.hs.get_datastore().insert_receipt(
                "!room2:blue", "m.read", USER_ID, ["$event2:foo"], {"a": 2}
            )
        )
        self.replicate()

        # Nothing should have happened as we are disconnected
        self.test_handler.on_rdata.assert_not_called()

        self.reconnect()
        self.pump(0.1)

        # We should now have caught up and get the missing data
        self.test_handler.on_rdata.assert_called_once()
        stream_name, _, token, rdata_rows = self.test_handler.on_rdata.call_args[0]
        self.assertEqual(stream_name, "receipts")
        self.assertEqual(token, 3)
        self.assertEqual(1, len(rdata_rows))

        row: ReceiptsStream.ReceiptsStreamRow = rdata_rows[0]
        self.assertEqual("!room2:blue", row.room_id)
        self.assertEqual("m.read", row.receipt_type)
        self.assertEqual(USER_ID, row.user_id)
        self.assertEqual("$event2:foo", row.event_id)
        self.assertEqual({"a": 2}, row.data)
