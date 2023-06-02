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


class VoipConfig(Config):
    section = "voip"

    def read_config(self, config, **kwargs):
        self.turn_uris = config.get("turn_uris", [])
        self.turn_shared_secret = config.get("turn_shared_secret")
        self.turn_username = config.get("turn_username")
        self.turn_password = config.get("turn_password")
        self.turn_user_lifetime = self.parse_duration(
            config.get("turn_user_lifetime", "1h")
        )
        self.turn_allow_guests = config.get("turn_allow_guests", True)

    def generate_config_section(self, **kwargs):
        return """\
        ## TURN ##

        # The public URIs of the TURN server to give to clients
        #
        #turn_uris: []

        # The shared secret used to compute passwords for the TURN server
        #
        #turn_shared_secret: "YOUR_SHARED_SECRET"

        # The Username and password if the TURN server needs them and
        # does not use a token
        #
        #turn_username: "TURNSERVER_USERNAME"
        #turn_password: "TURNSERVER_PASSWORD"

        # How long generated TURN credentials last
        #
        #turn_user_lifetime: 1h

        # Whether guests should be allowed to use the TURN server.
        # This defaults to True, otherwise VoIP will be unreliable for guests.
        # However, it does introduce a slight security risk as it allows users to
        # connect to arbitrary endpoints without having first signed up for a
        # valid account (e.g. by passing a CAPTCHA).
        #
        #turn_allow_guests: true
        """
