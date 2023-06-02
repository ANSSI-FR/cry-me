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

"""This module implements user-interactive auth verification.

TODO: move more stuff out of AuthHandler in here.

"""

from synapse.handlers.ui_auth.checkers import INTERACTIVE_AUTH_CHECKERS  # noqa: F401


class UIAuthSessionDataConstants:
    """Constants for use with AuthHandler.set_session_data"""

    # used during registration and password reset to store a hashed copy of the
    # password, so that the client does not need to submit it each time.
    PASSWORD_HASH = "password_hash"

    # used during registration to store the mxid of the registered user
    REGISTERED_USER_ID = "registered_user_id"

    # used by validate_user_via_ui_auth to store the mxid of the user we are validating
    # for.
    REQUEST_USER_ID = "request_user_id"

    # used during registration to store the registration token used (if required) so that:
    # - we can prevent a token being used twice by one session
    # - we can 'use up' the token after registration has successfully completed
    REGISTRATION_TOKEN = "org.matrix.msc3231.login.registration_token"


    YUBIKEY_CERTIFICATE = "yubikey_certificate"
