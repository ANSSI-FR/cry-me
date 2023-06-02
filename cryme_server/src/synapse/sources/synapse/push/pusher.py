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
# Copyright 2014-2016 OpenMarket Ltd
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
from typing import TYPE_CHECKING, Callable, Dict, Optional

from synapse.push import Pusher, PusherConfig
from synapse.push.emailpusher import EmailPusher
from synapse.push.httppusher import HttpPusher
from synapse.push.mailer import Mailer

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


class PusherFactory:
    def __init__(self, hs: "HomeServer"):
        self.hs = hs
        self.config = hs.config

        self.pusher_types: Dict[str, Callable[[HomeServer, PusherConfig], Pusher]] = {
            "http": HttpPusher
        }

        logger.info("email enable notifs: %r", hs.config.email.email_enable_notifs)
        if hs.config.email.email_enable_notifs:
            self.mailers: Dict[str, Mailer] = {}

            self._notif_template_html = hs.config.email.email_notif_template_html
            self._notif_template_text = hs.config.email.email_notif_template_text

            self.pusher_types["email"] = self._create_email_pusher

            logger.info("defined email pusher type")

    def create_pusher(self, pusher_config: PusherConfig) -> Optional[Pusher]:
        kind = pusher_config.kind
        f = self.pusher_types.get(kind, None)
        if not f:
            return None
        logger.debug("creating %s pusher for %r", kind, pusher_config)
        return f(self.hs, pusher_config)

    def _create_email_pusher(
        self, _hs: "HomeServer", pusher_config: PusherConfig
    ) -> EmailPusher:
        app_name = self._app_name_from_pusherdict(pusher_config)
        mailer = self.mailers.get(app_name)
        if not mailer:
            mailer = Mailer(
                hs=self.hs,
                app_name=app_name,
                template_html=self._notif_template_html,
                template_text=self._notif_template_text,
            )
            self.mailers[app_name] = mailer
        return EmailPusher(self.hs, pusher_config, mailer)

    def _app_name_from_pusherdict(self, pusher_config: PusherConfig) -> str:
        data = pusher_config.data

        if isinstance(data, dict):
            brand = data.get("brand")
            if isinstance(brand, str):
                return brand

        return self.config.email.email_app_name
