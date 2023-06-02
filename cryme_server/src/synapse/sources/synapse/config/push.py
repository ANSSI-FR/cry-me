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
# Copyright 2015, 2016 OpenMarket Ltd
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


class PushConfig(Config):
    section = "push"

    def read_config(self, config, **kwargs):
        push_config = config.get("push") or {}
        self.push_include_content = push_config.get("include_content", True)
        self.push_group_unread_count_by_room = push_config.get(
            "group_unread_count_by_room", True
        )

        # There was a a 'redact_content' setting but mistakenly read from the
        # 'email'section'. Check for the flag in the 'push' section, and log,
        # but do not honour it to avoid nasty surprises when people upgrade.
        if push_config.get("redact_content") is not None:
            print(
                "The push.redact_content content option has never worked. "
                "Please set push.include_content if you want this behaviour"
            )

        # Now check for the one in the 'email' section and honour it,
        # with a warning.
        push_config = config.get("email") or {}
        redact_content = push_config.get("redact_content")
        if redact_content is not None:
            print(
                "The 'email.redact_content' option is deprecated: "
                "please set push.include_content instead"
            )
            self.push_include_content = not redact_content

    def generate_config_section(self, config_dir_path, server_name, **kwargs):
        return """
        ## Push ##

        push:
          # Clients requesting push notifications can either have the body of
          # the message sent in the notification poke along with other details
          # like the sender, or just the event ID and room ID (`event_id_only`).
          # If clients choose the former, this option controls whether the
          # notification request includes the content of the event (other details
          # like the sender are still included). For `event_id_only` push, it
          # has no effect.
          #
          # For modern android devices the notification content will still appear
          # because it is loaded by the app. iPhone, however will send a
          # notification saying only that a message arrived and who it came from.
          #
          # The default value is "true" to include message details. Uncomment to only
          # include the event ID and room ID in push notification payloads.
          #
          #include_content: false

          # When a push notification is received, an unread count is also sent.
          # This number can either be calculated as the number of unread messages
          # for the user, or the number of *rooms* the user has unread messages in.
          #
          # The default value is "true", meaning push clients will see the number of
          # rooms with unread messages in them. Uncomment to instead send the number
          # of unread messages.
          #
          #group_unread_count_by_room: false
        """
