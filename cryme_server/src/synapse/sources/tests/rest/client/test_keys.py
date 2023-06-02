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
#  Copyright 2021 The Matrix.org Foundation C.I.C.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License

from http import HTTPStatus

from synapse.api.errors import Codes
from synapse.rest import admin
from synapse.rest.client import keys, login

from tests import unittest


class KeyQueryTestCase(unittest.HomeserverTestCase):
    servlets = [
        keys.register_servlets,
        admin.register_servlets_for_client_rest_resource,
        login.register_servlets,
    ]

    def test_rejects_device_id_ice_key_outside_of_list(self):
        self.register_user("alice", "wonderland")
        alice_token = self.login("alice", "wonderland")
        bob = self.register_user("bob", "uncle")
        channel = self.make_request(
            "POST",
            "/_matrix/client/r0/keys/query",
            {
                "device_keys": {
                    bob: "device_id1",
                },
            },
            alice_token,
        )
        self.assertEqual(channel.code, HTTPStatus.BAD_REQUEST, channel.result)
        self.assertEqual(
            channel.json_body["errcode"],
            Codes.BAD_JSON,
            channel.result,
        )

    def test_rejects_device_key_given_as_map_to_bool(self):
        self.register_user("alice", "wonderland")
        alice_token = self.login("alice", "wonderland")
        bob = self.register_user("bob", "uncle")
        channel = self.make_request(
            "POST",
            "/_matrix/client/r0/keys/query",
            {
                "device_keys": {
                    bob: {
                        "device_id1": True,
                    },
                },
            },
            alice_token,
        )

        self.assertEqual(channel.code, HTTPStatus.BAD_REQUEST, channel.result)
        self.assertEqual(
            channel.json_body["errcode"],
            Codes.BAD_JSON,
            channel.result,
        )

    def test_requires_device_key(self):
        """`device_keys` is required. We should complain if it's missing."""
        self.register_user("alice", "wonderland")
        alice_token = self.login("alice", "wonderland")
        channel = self.make_request(
            "POST",
            "/_matrix/client/r0/keys/query",
            {},
            alice_token,
        )
        self.assertEqual(channel.code, HTTPStatus.BAD_REQUEST, channel.result)
        self.assertEqual(
            channel.json_body["errcode"],
            Codes.BAD_JSON,
            channel.result,
        )
