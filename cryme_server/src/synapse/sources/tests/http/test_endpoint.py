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
from synapse.util.stringutils import parse_and_validate_server_name, parse_server_name

from tests import unittest


class ServerNameTestCase(unittest.TestCase):
    def test_parse_server_name(self):
        test_data = {
            "localhost": ("localhost", None),
            "my-example.com:1234": ("my-example.com", 1234),
            "1.2.3.4": ("1.2.3.4", None),
            "[0abc:1def::1234]": ("[0abc:1def::1234]", None),
            "1.2.3.4:1": ("1.2.3.4", 1),
            "[0abc:1def::1234]:8080": ("[0abc:1def::1234]", 8080),
        }

        for i, o in test_data.items():
            self.assertEqual(parse_server_name(i), o)

    def test_validate_bad_server_names(self):
        test_data = [
            "",  # empty
            "localhost:http",  # non-numeric port
            "1234]",  # smells like ipv6 literal but isn't
            "[1234",
            "[1.2.3.4]",
            "underscore_.com",
            "percent%65.com",
            "newline.com\n",
            ".empty-label.com",
            "1234:5678:80",  # too many colons
        ]
        for i in test_data:
            try:
                parse_and_validate_server_name(i)
                self.fail(
                    "Expected parse_and_validate_server_name('%s') to throw" % (i,)
                )
            except ValueError:
                pass
