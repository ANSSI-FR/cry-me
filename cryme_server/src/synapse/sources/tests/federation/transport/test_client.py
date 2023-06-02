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

import json

from synapse.api.room_versions import RoomVersions
from synapse.federation.transport.client import SendJoinParser

from tests.unittest import TestCase


class SendJoinParserTestCase(TestCase):
    def test_two_writes(self) -> None:
        """Test that the parser can sensibly deserialise an input given in two slices."""
        parser = SendJoinParser(RoomVersions.V1, True)
        parent_event = {
            "content": {
                "see_room_version_spec": "The event format changes depending on the room version."
            },
            "event_id": "$authparent",
            "room_id": "!somewhere:example.org",
            "type": "m.room.minimal_pdu",
        }
        state = {
            "content": {
                "see_room_version_spec": "The event format changes depending on the room version."
            },
            "event_id": "$DoNotThinkAboutTheEvent",
            "room_id": "!somewhere:example.org",
            "type": "m.room.minimal_pdu",
        }
        response = [
            200,
            {
                "auth_chain": [parent_event],
                "origin": "matrix.org",
                "state": [state],
            },
        ]
        serialised_response = json.dumps(response).encode()

        # Send data to the parser
        parser.write(serialised_response[:100])
        parser.write(serialised_response[100:])

        # Retrieve the parsed SendJoinResponse
        parsed_response = parser.finish()

        # Sanity check the parsing gave us sensible data.
        self.assertEqual(len(parsed_response.auth_events), 1, parsed_response)
        self.assertEqual(len(parsed_response.state), 1, parsed_response)
        self.assertEqual(parsed_response.event_dict, {}, parsed_response)
        self.assertIsNone(parsed_response.event, parsed_response)
