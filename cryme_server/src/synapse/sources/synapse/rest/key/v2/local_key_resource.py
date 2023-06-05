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
# Copyright 2014-2016 OpenMarket Ltd
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

import logging
from typing import TYPE_CHECKING, Optional

from canonicaljson import encode_canonical_json
from signedjson.sign import sign_json
from unpaddedbase64 import encode_base64

from twisted.web.resource import Resource
from twisted.web.server import Request

from synapse.http.server import respond_with_json_bytes
from synapse.types import JsonDict

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


class LocalKey(Resource):
    """HTTP resource containing encoding the TLS X.509 certificate and NACL
    signature verification keys for this server::

        GET /_matrix/key/v2/server/a.key.id HTTP/1.1

        HTTP/1.1 200 OK
        Content-Type: application/json
        {
            "valid_until_ts": # integer posix timestamp when this result expires.
            "server_name": "this.server.example.com"
            "verify_keys": {
                "algorithm:version": {
                    "key": # base64 encoded NACL verification key.
                }
            },
            "old_verify_keys": {
                "algorithm:version": {
                    "expired_ts": # integer posix timestamp when the key expired.
                    "key": # base64 encoded NACL verification key.
                }
            },
            "signatures": {
                "this.server.example.com": {
                   "algorithm:version": # NACL signature for this server
                }
            }
        }
    """

    isLeaf = True

    def __init__(self, hs: "HomeServer"):
        self.config = hs.config
        self.clock = hs.get_clock()
        self.update_response_body(self.clock.time_msec())
        Resource.__init__(self)

    def update_response_body(self, time_now_msec: int) -> None:
        refresh_interval = self.config.key.key_refresh_interval
        self.valid_until_ts = int(time_now_msec + refresh_interval)
        self.response_body = encode_canonical_json(self.response_json_object())

    def response_json_object(self) -> JsonDict:
        verify_keys = {}
        for key in self.config.key.signing_key:
            verify_key_bytes = key.verify_key.encode()
            key_id = "%s:%s" % (key.alg, key.version)
            verify_keys[key_id] = {"key": encode_base64(verify_key_bytes)}

        old_verify_keys = {}
        for key_id, key in self.config.key.old_signing_keys.items():
            verify_key_bytes = key.encode()
            old_verify_keys[key_id] = {
                "key": encode_base64(verify_key_bytes),
                "expired_ts": key.expired_ts,
            }

        json_object = {
            "valid_until_ts": self.valid_until_ts,
            "server_name": self.config.server.server_name,
            "verify_keys": verify_keys,
            "old_verify_keys": old_verify_keys,
        }
        for key in self.config.key.signing_key:
            json_object = sign_json(json_object, self.config.server.server_name, key)
        return json_object

    def render_GET(self, request: Request) -> Optional[int]:
        time_now = self.clock.time_msec()
        # Update the expiry time if less than half the interval remains.
        if time_now + self.config.key.key_refresh_interval / 2 > self.valid_until_ts:
            self.update_response_body(time_now)
        return respond_with_json_bytes(request, 200, self.response_body)
