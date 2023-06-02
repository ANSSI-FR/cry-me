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
# Copyright 2017 Vector Creations Ltd
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

import phonenumbers

from synapse.api.errors import SynapseError


def phone_number_to_msisdn(country: str, number: str) -> str:
    """
    Takes an ISO-3166-1 2 letter country code and phone number and
    returns an msisdn representing the canonical version of that
    phone number.
    Args:
        country: ISO-3166-1 2 letter country code
        number: Phone number in a national or international format

    Returns:
        The canonical form of the phone number, as an msisdn
    Raises:
        SynapseError if the number could not be parsed.
    """
    try:
        phoneNumber = phonenumbers.parse(number, country)
    except phonenumbers.NumberParseException:
        raise SynapseError(400, "Unable to parse phone number")
    return phonenumbers.format_number(phoneNumber, phonenumbers.PhoneNumberFormat.E164)[
        1:
    ]
