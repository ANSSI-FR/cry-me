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
from synapse.types import UserID

from ._base import Config

DEFAULT_CONFIG = """\
# Server Notices room configuration
#
# Uncomment this section to enable a room which can be used to send notices
# from the server to users. It is a special room which cannot be left; notices
# come from a special "notices" user id.
#
# If you uncomment this section, you *must* define the system_mxid_localpart
# setting, which defines the id of the user which will be used to send the
# notices.
#
# It's also possible to override the room name, the display name of the
# "notices" user, and the avatar for the user.
#
#server_notices:
#  system_mxid_localpart: notices
#  system_mxid_display_name: "Server Notices"
#  system_mxid_avatar_url: "mxc://server.com/oumMVlgDnLYFaPVkExemNVVZ"
#  room_name: "Server Notices"
"""


class ServerNoticesConfig(Config):
    """Configuration for the server notices room.

    Attributes:
        server_notices_mxid (str|None):
            The MXID to use for server notices.
            None if server notices are not enabled.

        server_notices_mxid_display_name (str|None):
            The display name to use for the server notices user.
            None if server notices are not enabled.

        server_notices_mxid_avatar_url (str|None):
            The MXC URL for the avatar of the server notices user.
            None if server notices are not enabled.

        server_notices_room_name (str|None):
            The name to use for the server notices room.
            None if server notices are not enabled.
    """

    section = "servernotices"

    def __init__(self, *args):
        super().__init__(*args)
        self.server_notices_mxid = None
        self.server_notices_mxid_display_name = None
        self.server_notices_mxid_avatar_url = None
        self.server_notices_room_name = None

    def read_config(self, config, **kwargs):
        c = config.get("server_notices")
        if c is None:
            return

        mxid_localpart = c["system_mxid_localpart"]
        self.server_notices_mxid = UserID(
            mxid_localpart, self.root.server.server_name
        ).to_string()
        self.server_notices_mxid_display_name = c.get("system_mxid_display_name", None)
        self.server_notices_mxid_avatar_url = c.get("system_mxid_avatar_url", None)
        # todo: i18n
        self.server_notices_room_name = c.get("room_name", "Server Notices")

    def generate_config_section(self, **kwargs):
        return DEFAULT_CONFIG
