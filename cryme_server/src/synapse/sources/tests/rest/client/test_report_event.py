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
# Copyright 2021 Callum Brown
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

import json

import synapse.rest.admin
from synapse.rest.client import login, report_event, room

from tests import unittest


class ReportEventTestCase(unittest.HomeserverTestCase):
    servlets = [
        synapse.rest.admin.register_servlets,
        login.register_servlets,
        room.register_servlets,
        report_event.register_servlets,
    ]

    def prepare(self, reactor, clock, hs):
        self.admin_user = self.register_user("admin", "pass", admin=True)
        self.admin_user_tok = self.login("admin", "pass")
        self.other_user = self.register_user("user", "pass")
        self.other_user_tok = self.login("user", "pass")

        self.room_id = self.helper.create_room_as(
            self.other_user, tok=self.other_user_tok, is_public=True
        )
        self.helper.join(self.room_id, user=self.admin_user, tok=self.admin_user_tok)
        resp = self.helper.send(self.room_id, tok=self.admin_user_tok)
        self.event_id = resp["event_id"]
        self.report_path = f"rooms/{self.room_id}/report/{self.event_id}"

    def test_reason_str_and_score_int(self):
        data = {"reason": "this makes me sad", "score": -100}
        self._assert_status(200, data)

    def test_no_reason(self):
        data = {"score": 0}
        self._assert_status(200, data)

    def test_no_score(self):
        data = {"reason": "this makes me sad"}
        self._assert_status(200, data)

    def test_no_reason_and_no_score(self):
        data = {}
        self._assert_status(200, data)

    def test_reason_int_and_score_str(self):
        data = {"reason": 10, "score": "string"}
        self._assert_status(400, data)

    def test_reason_zero_and_score_blank(self):
        data = {"reason": 0, "score": ""}
        self._assert_status(400, data)

    def test_reason_and_score_null(self):
        data = {"reason": None, "score": None}
        self._assert_status(400, data)

    def _assert_status(self, response_status, data):
        channel = self.make_request(
            "POST",
            self.report_path,
            json.dumps(data),
            access_token=self.other_user_tok,
        )
        self.assertEqual(
            response_status, int(channel.result["code"]), msg=channel.result["body"]
        )
