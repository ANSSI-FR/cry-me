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

from ._base import Config


class CaptchaConfig(Config):
    section = "captcha"

    def read_config(self, config, **kwargs):
        self.recaptcha_private_key = config.get("recaptcha_private_key")
        self.recaptcha_public_key = config.get("recaptcha_public_key")
        self.enable_registration_captcha = config.get(
            "enable_registration_captcha", False
        )
        self.recaptcha_siteverify_api = config.get(
            "recaptcha_siteverify_api",
            "https://www.recaptcha.net/recaptcha/api/siteverify",
        )
        self.recaptcha_template = self.read_template("recaptcha.html")

    def generate_config_section(self, **kwargs):
        return """\
        ## Captcha ##
        # See docs/CAPTCHA_SETUP.md for full details of configuring this.

        # This homeserver's ReCAPTCHA public key. Must be specified if
        # enable_registration_captcha is enabled.
        #
        #recaptcha_public_key: "YOUR_PUBLIC_KEY"

        # This homeserver's ReCAPTCHA private key. Must be specified if
        # enable_registration_captcha is enabled.
        #
        #recaptcha_private_key: "YOUR_PRIVATE_KEY"

        # Uncomment to enable ReCaptcha checks when registering, preventing signup
        # unless a captcha is answered. Requires a valid ReCaptcha
        # public/private key. Defaults to 'false'.
        #
        #enable_registration_captcha: true

        # The API endpoint to use for verifying m.login.recaptcha responses.
        # Defaults to "https://www.recaptcha.net/recaptcha/api/siteverify".
        #
        #recaptcha_siteverify_api: "https://my.recaptcha.site"
        """
