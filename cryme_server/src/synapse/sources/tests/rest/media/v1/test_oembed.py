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
#  limitations under the License.

import json

from twisted.test.proto_helpers import MemoryReactor

from synapse.rest.media.v1.oembed import OEmbedProvider
from synapse.server import HomeServer
from synapse.types import JsonDict
from synapse.util import Clock

from tests.unittest import HomeserverTestCase


class OEmbedTests(HomeserverTestCase):
    def prepare(self, reactor: MemoryReactor, clock: Clock, homeserver: HomeServer):
        self.oembed = OEmbedProvider(homeserver)

    def parse_response(self, response: JsonDict):
        return self.oembed.parse_oembed_response(
            "https://test", json.dumps(response).encode("utf-8")
        )

    def test_version(self):
        """Accept versions that are similar to 1.0 as a string or int (or missing)."""
        for version in ("1.0", 1.0, 1):
            result = self.parse_response({"version": version, "type": "link"})
            # An empty Open Graph response is an error, ensure the URL is included.
            self.assertIn("og:url", result.open_graph_result)

        # A missing version should be treated as 1.0.
        result = self.parse_response({"type": "link"})
        self.assertIn("og:url", result.open_graph_result)

        # Invalid versions should be rejected.
        for version in ("2.0", "1", 1.1, 0, None, {}, []):
            result = self.parse_response({"version": version, "type": "link"})
            # An empty Open Graph response is an error, ensure the URL is included.
            self.assertEqual({}, result.open_graph_result)
