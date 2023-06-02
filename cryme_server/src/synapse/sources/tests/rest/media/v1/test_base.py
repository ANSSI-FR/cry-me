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

from synapse.rest.media.v1._base import get_filename_from_headers

from tests import unittest


class GetFileNameFromHeadersTests(unittest.TestCase):
    # input -> expected result
    TEST_CASES = {
        b"inline; filename=abc.txt": "abc.txt",
        b'inline; filename="azerty"': "azerty",
        b'inline; filename="aze%20rty"': "aze%20rty",
        b'inline; filename="aze"rty"': 'aze"rty',
        b'inline; filename="azer;ty"': "azer;ty",
        b"inline; filename*=utf-8''foo%C2%A3bar": "foo£bar",
    }

    def tests(self):
        for hdr, expected in self.TEST_CASES.items():
            res = get_filename_from_headers({b"Content-Disposition": [hdr]})
            self.assertEqual(
                res,
                expected,
                "expected output for %s to be %s but was %s" % (hdr, expected, res),
            )
