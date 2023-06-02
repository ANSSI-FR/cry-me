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
import os
import shutil
import tempfile
import unittest
from contextlib import redirect_stdout
from io import StringIO

from synapse.config.homeserver import HomeServerConfig


class ConfigFileTestCase(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.mkdtemp()
        self.config_file = os.path.join(self.dir, "homeserver.yaml")

    def tearDown(self):
        shutil.rmtree(self.dir)

    def generate_config(self):
        with redirect_stdout(StringIO()):
            HomeServerConfig.load_or_generate_config(
                "",
                [
                    "--generate-config",
                    "-c",
                    self.config_file,
                    "--report-stats=yes",
                    "-H",
                    "lemurs.win",
                ],
            )

    def generate_config_and_remove_lines_containing(self, needle):
        self.generate_config()

        with open(self.config_file) as f:
            contents = f.readlines()
        contents = [line for line in contents if needle not in line]
        with open(self.config_file, "w") as f:
            f.write("".join(contents))

    def add_lines_to_config(self, lines):
        with open(self.config_file, "a") as f:
            for line in lines:
                f.write(line + "\n")
