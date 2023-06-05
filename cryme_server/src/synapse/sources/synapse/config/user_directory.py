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

from ._base import Config


class UserDirectoryConfig(Config):
    """User Directory Configuration
    Configuration for the behaviour of the /user_directory API
    """

    section = "userdirectory"

    def read_config(self, config, **kwargs):
        user_directory_config = config.get("user_directory") or {}
        self.user_directory_search_enabled = user_directory_config.get("enabled", True)
        self.user_directory_search_all_users = user_directory_config.get(
            "search_all_users", False
        )
        self.user_directory_search_prefer_local_users = user_directory_config.get(
            "prefer_local_users", False
        )

    def generate_config_section(self, config_dir_path, server_name, **kwargs):
        return """
        # User Directory configuration
        #
        user_directory:
            # Defines whether users can search the user directory. If false then
            # empty responses are returned to all queries. Defaults to true.
            #
            # Uncomment to disable the user directory.
            #
            #enabled: false

            # Defines whether to search all users visible to your HS when searching
            # the user directory. If false, search results will only contain users
            # visible in public rooms and users sharing a room with the requester.
            # Defaults to false.
            #
            # NB. If you set this to true, and the last time the user_directory search
            # indexes were (re)built was before Synapse 1.44, you'll have to
            # rebuild the indexes in order to search through all known users.
            # These indexes are built the first time Synapse starts; admins can
            # manually trigger a rebuild via API following the instructions at
            #     https://matrix-org.github.io/synapse/latest/usage/administration/admin_api/background_updates.html#run
            #
            # Uncomment to return search results containing all known users, even if that
            # user does not share a room with the requester.
            #
            #search_all_users: true

            # Defines whether to prefer local users in search query results.
            # If True, local users are more likely to appear above remote users
            # when searching the user directory. Defaults to false.
            #
            # Uncomment to prefer local over remote users in user directory search
            # results.
            #
            #prefer_local_users: true
        """
