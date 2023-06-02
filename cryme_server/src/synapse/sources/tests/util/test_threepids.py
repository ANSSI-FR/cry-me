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
# Copyright 2020 Dirk Klimpel
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

from synapse.util.threepids import canonicalise_email

from tests.unittest import HomeserverTestCase


class CanonicaliseEmailTests(HomeserverTestCase):
    def test_no_at(self):
        with self.assertRaises(ValueError):
            canonicalise_email("address-without-at.bar")

    def test_two_at(self):
        with self.assertRaises(ValueError):
            canonicalise_email("foo@foo@test.bar")

    def test_bad_format(self):
        with self.assertRaises(ValueError):
            canonicalise_email("user@bad.example.net@good.example.com")

    def test_valid_format(self):
        self.assertEqual(canonicalise_email("foo@test.bar"), "foo@test.bar")

    def test_domain_to_lower(self):
        self.assertEqual(canonicalise_email("foo@TEST.BAR"), "foo@test.bar")

    def test_domain_with_umlaut(self):
        self.assertEqual(canonicalise_email("foo@Öumlaut.com"), "foo@öumlaut.com")

    def test_address_casefold(self):
        self.assertEqual(
            canonicalise_email("Strauß@Example.com"), "strauss@example.com"
        )

    def test_address_trim(self):
        self.assertEqual(canonicalise_email(" foo@test.bar "), "foo@test.bar")
