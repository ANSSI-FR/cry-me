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
import logging
from http import HTTPStatus
from typing import TYPE_CHECKING, Tuple

from synapse.api.errors import SynapseError
from synapse.http.servlet import (
    RestServlet,
    assert_params_in_dict,
    parse_json_object_from_request,
)
from synapse.http.site import SynapseRequest
from synapse.rest.admin._base import admin_patterns, assert_requester_is_admin
from synapse.types import JsonDict

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


class BackgroundUpdateEnabledRestServlet(RestServlet):
    """Allows temporarily disabling background updates"""

    PATTERNS = admin_patterns("/background_updates/enabled$")

    def __init__(self, hs: "HomeServer"):
        self._auth = hs.get_auth()
        self._data_stores = hs.get_datastores()

    async def on_GET(self, request: SynapseRequest) -> Tuple[int, JsonDict]:
        await assert_requester_is_admin(self._auth, request)

        # We need to check that all configured databases have updates enabled.
        # (They *should* all be in sync.)
        enabled = all(db.updates.enabled for db in self._data_stores.databases)

        return HTTPStatus.OK, {"enabled": enabled}

    async def on_POST(self, request: SynapseRequest) -> Tuple[int, JsonDict]:
        await assert_requester_is_admin(self._auth, request)

        body = parse_json_object_from_request(request)

        enabled = body.get("enabled", True)

        if not isinstance(enabled, bool):
            raise SynapseError(
                HTTPStatus.BAD_REQUEST, "'enabled' parameter must be a boolean"
            )

        for db in self._data_stores.databases:
            db.updates.enabled = enabled

            # If we're re-enabling them ensure that we start the background
            # process again.
            if enabled:
                db.updates.start_doing_background_updates()

        return HTTPStatus.OK, {"enabled": enabled}


class BackgroundUpdateRestServlet(RestServlet):
    """Fetch information about background updates"""

    PATTERNS = admin_patterns("/background_updates/status$")

    def __init__(self, hs: "HomeServer"):
        self._auth = hs.get_auth()
        self._data_stores = hs.get_datastores()

    async def on_GET(self, request: SynapseRequest) -> Tuple[int, JsonDict]:
        await assert_requester_is_admin(self._auth, request)

        # We need to check that all configured databases have updates enabled.
        # (They *should* all be in sync.)
        enabled = all(db.updates.enabled for db in self._data_stores.databases)

        current_updates = {}

        for db in self._data_stores.databases:
            update = db.updates.get_current_update()
            if not update:
                continue

            current_updates[db.name()] = {
                "name": update.name,
                "total_item_count": update.total_item_count,
                "total_duration_ms": update.total_duration_ms,
                "average_items_per_ms": update.average_items_per_ms(),
            }

        return HTTPStatus.OK, {"enabled": enabled, "current_updates": current_updates}


class BackgroundUpdateStartJobRestServlet(RestServlet):
    """Allows to start specific background updates"""

    PATTERNS = admin_patterns("/background_updates/start_job$")

    def __init__(self, hs: "HomeServer"):
        self._auth = hs.get_auth()
        self._store = hs.get_datastore()

    async def on_POST(self, request: SynapseRequest) -> Tuple[int, JsonDict]:
        await assert_requester_is_admin(self._auth, request)

        body = parse_json_object_from_request(request)
        assert_params_in_dict(body, ["job_name"])

        job_name = body["job_name"]

        if job_name == "populate_stats_process_rooms":
            jobs = [
                {
                    "update_name": "populate_stats_process_rooms",
                    "progress_json": "{}",
                },
            ]
        elif job_name == "regenerate_directory":
            jobs = [
                {
                    "update_name": "populate_user_directory_createtables",
                    "progress_json": "{}",
                    "depends_on": "",
                },
                {
                    "update_name": "populate_user_directory_process_rooms",
                    "progress_json": "{}",
                    "depends_on": "populate_user_directory_createtables",
                },
                {
                    "update_name": "populate_user_directory_process_users",
                    "progress_json": "{}",
                    "depends_on": "populate_user_directory_process_rooms",
                },
                {
                    "update_name": "populate_user_directory_cleanup",
                    "progress_json": "{}",
                    "depends_on": "populate_user_directory_process_users",
                },
            ]
        else:
            raise SynapseError(HTTPStatus.BAD_REQUEST, "Invalid job_name")

        try:
            await self._store.db_pool.simple_insert_many(
                table="background_updates",
                values=jobs,
                desc=f"admin_api_run_{job_name}",
            )
        except self._store.db_pool.engine.module.IntegrityError:
            raise SynapseError(
                HTTPStatus.BAD_REQUEST,
                "Job %s is already in queue of background updates." % (job_name,),
            )

        self._store.db_pool.updates.start_doing_background_updates()

        return HTTPStatus.OK, {}
