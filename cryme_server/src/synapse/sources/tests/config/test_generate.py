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
# Copyright 2016 OpenMarket Ltd
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

import os.path
import re
import shutil
import tempfile
from contextlib import redirect_stdout
from io import StringIO

from synapse.config.homeserver import HomeServerConfig

from tests import unittest


class ConfigGenerationTestCase(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.mkdtemp()
        self.file = os.path.join(self.dir, "homeserver.yaml")

    def tearDown(self):
        shutil.rmtree(self.dir)

    def test_generate_config_generates_files(self):
        with redirect_stdout(StringIO()):
            HomeServerConfig.load_or_generate_config(
                "",
                [
                    "--generate-config",
                    "-c",
                    self.file,
                    "--report-stats=yes",
                    "-H",
                    "lemurs.win",
                ],
            )

        self.assertSetEqual(
            {"homeserver.yaml", "lemurs.win.log.config", "lemurs.win.signing.key"},
            set(os.listdir(self.dir)),
        )

        self.assert_log_filename_is(
            os.path.join(self.dir, "lemurs.win.log.config"),
            os.path.join(os.getcwd(), "homeserver.log"),
        )

    def assert_log_filename_is(self, log_config_file, expected):
        with open(log_config_file) as f:
            config = f.read()
            # find the 'filename' line
            matches = re.findall(r"^\s*filename:\s*(.*)$", config, re.M)
            self.assertEqual(1, len(matches))
            self.assertEqual(matches[0], expected)
