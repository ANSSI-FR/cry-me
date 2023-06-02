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
from synapse.config.homeserver import HomeServerConfig

from tests.unittest import TestCase
from tests.utils import default_config


class RatelimitConfigTestCase(TestCase):
    def test_parse_rc_federation(self):
        config_dict = default_config("test")
        config_dict["rc_federation"] = {
            "window_size": 20000,
            "sleep_limit": 693,
            "sleep_delay": 252,
            "reject_limit": 198,
            "concurrent": 7,
        }

        config = HomeServerConfig()
        config.parse_config_dict(config_dict, "", "")
        config_obj = config.ratelimiting.rc_federation

        self.assertEqual(config_obj.window_size, 20000)
        self.assertEqual(config_obj.sleep_limit, 693)
        self.assertEqual(config_obj.sleep_delay, 252)
        self.assertEqual(config_obj.reject_limit, 198)
        self.assertEqual(config_obj.concurrent, 7)
