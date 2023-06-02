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
from synapse.config import ConfigError
from synapse.config.homeserver import HomeServerConfig

from tests.unittest import TestCase
from tests.utils import default_config


class RegistrationConfigTestCase(TestCase):
    def test_session_lifetime_must_not_be_exceeded_by_smaller_lifetimes(self):
        """
        session_lifetime should logically be larger than, or at least as large as,
        all the different token lifetimes.
        Test that the user is faced with configuration errors if they make it
        smaller, as that configuration doesn't make sense.
        """
        config_dict = default_config("test")

        # First test all the error conditions
        with self.assertRaises(ConfigError):
            HomeServerConfig().parse_config_dict(
                {
                    "session_lifetime": "30m",
                    "nonrefreshable_access_token_lifetime": "31m",
                    **config_dict,
                }
            )

        with self.assertRaises(ConfigError):
            HomeServerConfig().parse_config_dict(
                {
                    "session_lifetime": "30m",
                    "refreshable_access_token_lifetime": "31m",
                    **config_dict,
                }
            )

        with self.assertRaises(ConfigError):
            HomeServerConfig().parse_config_dict(
                {
                    "session_lifetime": "30m",
                    "refresh_token_lifetime": "31m",
                    **config_dict,
                }
            )

        # Then test all the fine conditions
        HomeServerConfig().parse_config_dict(
            {
                "session_lifetime": "31m",
                "nonrefreshable_access_token_lifetime": "31m",
                **config_dict,
            }
        )

        HomeServerConfig().parse_config_dict(
            {
                "session_lifetime": "31m",
                "refreshable_access_token_lifetime": "31m",
                **config_dict,
            }
        )

        HomeServerConfig().parse_config_dict(
            {"session_lifetime": "31m", "refresh_token_lifetime": "31m", **config_dict}
        )
