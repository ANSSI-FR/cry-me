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
# Copyright 2019 The Matrix.org Foundation C.I.C.
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
from typing import TYPE_CHECKING, Tuple

from twisted.web.server import Request

from synapse.http.server import HttpServer
from synapse.http.servlet import RestServlet
from synapse.types import JsonDict

from ._base import client_patterns

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


class PasswordPolicyServlet(RestServlet):
    PATTERNS = client_patterns("/password_policy$")

    def __init__(self, hs: "HomeServer"):
        super().__init__()

        self.policy = hs.config.auth.password_policy
        self.enabled = hs.config.auth.password_policy_enabled

    def on_GET(self, request: Request) -> Tuple[int, JsonDict]:
        if not self.enabled or not self.policy:
            return 200, {}

        policy = {}

        for param in [
            "minimum_length",
            "require_digit",
            "require_symbol",
            "require_lowercase",
            "require_uppercase",
        ]:
            if param in self.policy:
                policy["m.%s" % param] = self.policy[param]

        return 200, policy


def register_servlets(hs: "HomeServer", http_server: HttpServer) -> None:
    PasswordPolicyServlet(hs).register(http_server)
