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

from synapse.federation.send_queue import EduRow
from synapse.replication.tcp.streams.federation import FederationStream

from tests.replication._base import BaseStreamTestCase


class FederationStreamTestCase(BaseStreamTestCase):
    def _get_worker_hs_config(self) -> dict:
        # enable federation sending on the worker
        config = super()._get_worker_hs_config()
        # TODO: make it so we don't need both of these
        config["send_federation"] = False
        config["worker_app"] = "synapse.app.federation_sender"
        return config

    def test_catchup(self):
        """Basic test of catchup on reconnect

        Makes sure that updates sent while we are offline are received later.
        """
        fed_sender = self.hs.get_federation_sender()
        received_rows = self.test_handler.received_rdata_rows

        fed_sender.build_and_send_edu("testdest", "m.test_edu", {"a": "b"})

        self.reconnect()
        self.reactor.advance(0)

        # check we're testing what we think we are: no rows should yet have been
        # received
        self.assertEqual(received_rows, [])

        # We should now see an attempt to connect to the master
        request = self.handle_http_replication_attempt()
        self.assert_request_is_get_repl_stream_updates(request, "federation")

        # we should have received an update row
        stream_name, token, row = received_rows.pop()
        self.assertEqual(stream_name, "federation")
        self.assertIsInstance(row, FederationStream.FederationStreamRow)
        self.assertEqual(row.type, EduRow.TypeId)
        edurow = EduRow.from_data(row.data)
        self.assertEqual(edurow.edu.edu_type, "m.test_edu")
        self.assertEqual(edurow.edu.origin, self.hs.hostname)
        self.assertEqual(edurow.edu.destination, "testdest")
        self.assertEqual(edurow.edu.content, {"a": "b"})

        self.assertEqual(received_rows, [])

        # additional updates should be transferred without an HTTP hit
        fed_sender.build_and_send_edu("testdest", "m.test1", {"c": "d"})
        self.reactor.advance(0)
        # there should be no http hit
        self.assertEqual(len(self.reactor.tcpClients), 0)
        # ... but we should have a row
        self.assertEqual(len(received_rows), 1)

        stream_name, token, row = received_rows.pop()
        self.assertEqual(stream_name, "federation")
        self.assertIsInstance(row, FederationStream.FederationStreamRow)
        self.assertEqual(row.type, EduRow.TypeId)
        edurow = EduRow.from_data(row.data)
        self.assertEqual(edurow.edu.edu_type, "m.test1")
        self.assertEqual(edurow.edu.origin, self.hs.hostname)
        self.assertEqual(edurow.edu.destination, "testdest")
        self.assertEqual(edurow.edu.content, {"c": "d"})
