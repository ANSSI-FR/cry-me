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
from typing import Any, Dict, List, Tuple

from synapse.config._base import Config, ConfigError
from synapse.util.module_loader import load_module


class ModulesConfig(Config):
    section = "modules"

    def read_config(self, config: dict, **kwargs):
        self.loaded_modules: List[Tuple[Any, Dict]] = []

        configured_modules = config.get("modules") or []
        for i, module in enumerate(configured_modules):
            config_path = ("modules", "<item %i>" % i)
            if not isinstance(module, dict):
                raise ConfigError("expected a mapping", config_path)

            self.loaded_modules.append(load_module(module, config_path))

    def generate_config_section(self, **kwargs):
        return """
            ## Modules ##

            # Server admins can expand Synapse's functionality with external modules.
            #
            # See https://matrix-org.github.io/synapse/latest/modules.html for more
            # documentation on how to configure or create custom modules for Synapse.
            #
            modules:
                # - module: my_super_module.MySuperClass
                #   config:
                #       do_thing: true
                # - module: my_other_super_module.SomeClass
                #   config: {}
            """
