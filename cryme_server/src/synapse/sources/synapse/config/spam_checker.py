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
# Copyright 2017 New Vector Ltd
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

import logging
from typing import Any, Dict, List, Tuple

from synapse.config import ConfigError
from synapse.util.module_loader import load_module

from ._base import Config

logger = logging.getLogger(__name__)

LEGACY_SPAM_CHECKER_WARNING = """
This server is using a spam checker module that is implementing the deprecated spam
checker interface. Please check with the module's maintainer to see if a new version
supporting Synapse's generic modules system is available.
For more information, please see https://matrix-org.github.io/synapse/latest/modules.html
---------------------------------------------------------------------------------------"""


class SpamCheckerConfig(Config):
    section = "spamchecker"

    def read_config(self, config, **kwargs):
        self.spam_checkers: List[Tuple[Any, Dict]] = []

        spam_checkers = config.get("spam_checker") or []
        if isinstance(spam_checkers, dict):
            # The spam_checker config option used to only support one
            # spam checker, and thus was simply a dictionary with module
            # and config keys. Support this old behaviour by checking
            # to see if the option resolves to a dictionary
            self.spam_checkers.append(load_module(spam_checkers, ("spam_checker",)))
        elif isinstance(spam_checkers, list):
            for i, spam_checker in enumerate(spam_checkers):
                config_path = ("spam_checker", "<item %i>" % i)
                if not isinstance(spam_checker, dict):
                    raise ConfigError("expected a mapping", config_path)

                self.spam_checkers.append(load_module(spam_checker, config_path))
        else:
            raise ConfigError("spam_checker syntax is incorrect")

        # If this configuration is being used in any way, warn the admin that it is going
        # away soon.
        if self.spam_checkers:
            logger.warning(LEGACY_SPAM_CHECKER_WARNING)
