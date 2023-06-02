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

import json
from unittest.mock import Mock

from twisted.test.proto_helpers import MemoryReactorClock

from synapse.rest.client.register import register_servlets
from synapse.util import Clock

from tests import unittest


class TermsTestCase(unittest.HomeserverTestCase):
    servlets = [register_servlets]

    def default_config(self):
        config = super().default_config()
        config.update(
            {
                "public_baseurl": "https://example.org/",
                "user_consent": {
                    "version": "1.0",
                    "policy_name": "My Cool Privacy Policy",
                    "template_dir": "/",
                    "require_at_registration": True,
                },
            }
        )
        return config

    def prepare(self, reactor, clock, hs):
        self.clock = MemoryReactorClock()
        self.hs_clock = Clock(self.clock)
        self.url = "/_matrix/client/r0/register"
        self.registration_handler = Mock()
        self.auth_handler = Mock()
        self.device_handler = Mock()

    def test_ui_auth(self):
        # Do a UI auth request
        request_data = json.dumps({"username": "kermit", "password": "monkey"})
        channel = self.make_request(b"POST", self.url, request_data)

        self.assertEquals(channel.result["code"], b"401", channel.result)

        self.assertTrue(channel.json_body is not None)
        self.assertIsInstance(channel.json_body["session"], str)

        self.assertIsInstance(channel.json_body["flows"], list)
        for flow in channel.json_body["flows"]:
            self.assertIsInstance(flow["stages"], list)
            self.assertTrue(len(flow["stages"]) > 0)
            self.assertTrue("m.login.terms" in flow["stages"])

        expected_params = {
            "m.login.terms": {
                "policies": {
                    "privacy_policy": {
                        "en": {
                            "name": "My Cool Privacy Policy",
                            "url": "https://example.org/_matrix/consent?v=1.0",
                        },
                        "version": "1.0",
                    }
                }
            }
        }
        self.assertIsInstance(channel.json_body["params"], dict)
        self.assertDictContainsSubset(channel.json_body["params"], expected_params)

        # We have to complete the dummy auth stage before completing the terms stage
        request_data = json.dumps(
            {
                "username": "kermit",
                "password": "monkey",
                "auth": {
                    "session": channel.json_body["session"],
                    "type": "m.login.dummy",
                },
            }
        )

        self.registration_handler.check_username = Mock(return_value=True)

        channel = self.make_request(b"POST", self.url, request_data)

        # We don't bother checking that the response is correct - we'll leave that to
        # other tests. We just want to make sure we're on the right path.
        self.assertEquals(channel.result["code"], b"401", channel.result)

        # Finish the UI auth for terms
        request_data = json.dumps(
            {
                "username": "kermit",
                "password": "monkey",
                "auth": {
                    "session": channel.json_body["session"],
                    "type": "m.login.terms",
                },
            }
        )
        channel = self.make_request(b"POST", self.url, request_data)

        # We're interested in getting a response that looks like a successful
        # registration, not so much that the details are exactly what we want.

        self.assertEquals(channel.result["code"], b"200", channel.result)

        self.assertTrue(channel.json_body is not None)
        self.assertIsInstance(channel.json_body["user_id"], str)
        self.assertIsInstance(channel.json_body["access_token"], str)
        self.assertIsInstance(channel.json_body["device_id"], str)
