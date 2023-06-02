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
# Copyright 2018 New Vector Ltd
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

from ._base import Config

ROOM_STATS_DISABLED_WARN = """\
WARNING: room/user statistics have been disabled via the stats.enabled
configuration setting. This means that certain features (such as the room
directory) will not operate correctly. Future versions of Synapse may ignore
this setting.

To fix this warning, remove the stats.enabled setting from your configuration
file.
--------------------------------------------------------------------------------"""

logger = logging.getLogger(__name__)


class StatsConfig(Config):
    """Stats Configuration
    Configuration for the behaviour of synapse's stats engine
    """

    section = "stats"

    def read_config(self, config, **kwargs):
        self.stats_enabled = True
        stats_config = config.get("stats", None)
        if stats_config:
            self.stats_enabled = stats_config.get("enabled", self.stats_enabled)
        if not self.stats_enabled:
            logger.warning(ROOM_STATS_DISABLED_WARN)

    def generate_config_section(self, config_dir_path, server_name, **kwargs):
        return """
        # Settings for local room and user statistics collection. See
        # https://matrix-org.github.io/synapse/latest/room_and_user_statistics.html.
        #
        stats:
          # Uncomment the following to disable room and user statistics. Note that doing
          # so may cause certain features (such as the room directory) not to work
          # correctly.
          #
          #enabled: false
        """
