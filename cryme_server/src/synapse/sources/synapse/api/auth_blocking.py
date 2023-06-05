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
# Copyright 2020 The Matrix.org Foundation C.I.C.
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
from typing import TYPE_CHECKING, Optional

from synapse.api.constants import LimitBlockingTypes, UserTypes
from synapse.api.errors import Codes, ResourceLimitError
from synapse.config.server import is_threepid_reserved
from synapse.types import Requester

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


class AuthBlocking:
    def __init__(self, hs: "HomeServer"):
        self.store = hs.get_datastore()

        self._server_notices_mxid = hs.config.servernotices.server_notices_mxid
        self._hs_disabled = hs.config.server.hs_disabled
        self._hs_disabled_message = hs.config.server.hs_disabled_message
        self._admin_contact = hs.config.server.admin_contact
        self._max_mau_value = hs.config.server.max_mau_value
        self._limit_usage_by_mau = hs.config.server.limit_usage_by_mau
        self._mau_limits_reserved_threepids = (
            hs.config.server.mau_limits_reserved_threepids
        )
        self._server_name = hs.hostname
        self._track_appservice_user_ips = hs.config.appservice.track_appservice_user_ips

    async def check_auth_blocking(
        self,
        user_id: Optional[str] = None,
        threepid: Optional[dict] = None,
        user_type: Optional[str] = None,
        requester: Optional[Requester] = None,
    ) -> None:
        """Checks if the user should be rejected for some external reason,
        such as monthly active user limiting or global disable flag

        Args:
            user_id: If present, checks for presence against existing
                MAU cohort

            threepid: If present, checks for presence against configured
                reserved threepid. Used in cases where the user is trying register
                with a MAU blocked server, normally they would be rejected but their
                threepid is on the reserved list. user_id and
                threepid should never be set at the same time.

            user_type: If present, is used to decide whether to check against
                certain blocking reasons like MAU.

            requester: If present, and the authenticated entity is a user, checks for
                presence against existing MAU cohort. Passing in both a `user_id` and
                `requester` is an error.
        """
        if requester and user_id:
            raise Exception(
                "Passed in both 'user_id' and 'requester' to 'check_auth_blocking'"
            )

        if requester:
            if requester.authenticated_entity.startswith("@"):
                user_id = requester.authenticated_entity
            elif requester.authenticated_entity == self._server_name:
                # We never block the server from doing actions on behalf of
                # users.
                return
            if requester.app_service and not self._track_appservice_user_ips:
                # If we're authenticated as an appservice then we only block
                # auth if `track_appservice_user_ips` is set, as that option
                # implicitly means that application services are part of MAU
                # limits.
                return

        # Never fail an auth check for the server notices users or support user
        # This can be a problem where event creation is prohibited due to blocking
        if user_id is not None:
            if user_id == self._server_notices_mxid:
                return
            if await self.store.is_support_user(user_id):
                return

        if self._hs_disabled:
            raise ResourceLimitError(
                403,
                self._hs_disabled_message,
                errcode=Codes.RESOURCE_LIMIT_EXCEEDED,
                admin_contact=self._admin_contact,
                limit_type=LimitBlockingTypes.HS_DISABLED,
            )
        if self._limit_usage_by_mau is True:
            assert not (user_id and threepid)

            # If the user is already part of the MAU cohort or a trial user
            if user_id:
                timestamp = await self.store.user_last_seen_monthly_active(user_id)
                if timestamp:
                    return

                is_trial = await self.store.is_trial_user(user_id)
                if is_trial:
                    return
            elif threepid:
                # If the user does not exist yet, but is signing up with a
                # reserved threepid then pass auth check
                if is_threepid_reserved(self._mau_limits_reserved_threepids, threepid):
                    return
            elif user_type == UserTypes.SUPPORT:
                # If the user does not exist yet and is of type "support",
                # allow registration. Support users are excluded from MAU checks.
                return
            # Else if there is no room in the MAU bucket, bail
            current_mau = await self.store.get_monthly_active_count()
            if current_mau >= self._max_mau_value:
                raise ResourceLimitError(
                    403,
                    "Monthly Active User Limit Exceeded",
                    admin_contact=self._admin_contact,
                    errcode=Codes.RESOURCE_LIMIT_EXCEEDED,
                    limit_type=LimitBlockingTypes.MONTHLY_ACTIVE_USER,
                )
