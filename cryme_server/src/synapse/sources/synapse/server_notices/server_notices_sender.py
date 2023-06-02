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
from typing import TYPE_CHECKING, Iterable, Union

from synapse.server_notices.consent_server_notices import ConsentServerNotices
from synapse.server_notices.resource_limits_server_notices import (
    ResourceLimitsServerNotices,
)
from synapse.server_notices.worker_server_notices_sender import (
    WorkerServerNoticesSender,
)

if TYPE_CHECKING:
    from synapse.server import HomeServer


class ServerNoticesSender(WorkerServerNoticesSender):
    """A centralised place which sends server notices automatically when
    Certain Events take place
    """

    def __init__(self, hs: "HomeServer"):
        super().__init__(hs)
        self._server_notices: Iterable[
            Union[ConsentServerNotices, ResourceLimitsServerNotices]
        ] = (
            ConsentServerNotices(hs),
            ResourceLimitsServerNotices(hs),
        )

    async def on_user_syncing(self, user_id: str) -> None:
        """Called when the user performs a sync operation.

        Args:
            user_id: mxid of user who synced
        """
        for sn in self._server_notices:
            await sn.maybe_send_server_notice_to_user(user_id)

    async def on_user_ip(self, user_id: str) -> None:
        """Called on the master when a worker process saw a client request.

        Args:
            user_id: mxid
        """
        # The synchrotrons use a stubbed version of ServerNoticesSender, so
        # we check for notices to send to the user in on_user_ip as well as
        # in on_user_syncing
        for sn in self._server_notices:
            await sn.maybe_send_server_notice_to_user(user_id)
