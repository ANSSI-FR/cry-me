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
# Copyright 2020 Dirk Klimpel
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

from synapse.api.errors import Codes, NotFoundError, SynapseError
from synapse.http.servlet import RestServlet, parse_integer, parse_string
from synapse.http.site import SynapseRequest
from synapse.rest.admin._base import admin_patterns, assert_requester_is_admin
from synapse.types import JsonDict

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


class EventReportsRestServlet(RestServlet):
    """
    List all reported events that are known to the homeserver. Results are returned
    in a dictionary containing report information. Supports pagination.
    The requester must have administrator access in Synapse.

    GET /_synapse/admin/v1/event_reports
    returns:
        200 OK with list of reports if success otherwise an error.

    Args:
        The parameters `from` and `limit` are required only for pagination.
        By default, a `limit` of 100 is used.
        The parameter `dir` can be used to define the order of results.
        The parameter `user_id` can be used to filter by user id.
        The parameter `room_id` can be used to filter by room id.
    Returns:
        A list of reported events and an integer representing the total number of
        reported events that exist given this query
    """

    PATTERNS = admin_patterns("/event_reports$")

    def __init__(self, hs: "HomeServer"):
        self.auth = hs.get_auth()
        self.store = hs.get_datastore()

    async def on_GET(self, request: SynapseRequest) -> Tuple[int, JsonDict]:
        await assert_requester_is_admin(self.auth, request)

        start = parse_integer(request, "from", default=0)
        limit = parse_integer(request, "limit", default=100)
        direction = parse_string(request, "dir", default="b")
        user_id = parse_string(request, "user_id")
        room_id = parse_string(request, "room_id")

        if start < 0:
            raise SynapseError(
                HTTPStatus.BAD_REQUEST,
                "The start parameter must be a positive integer.",
                errcode=Codes.INVALID_PARAM,
            )

        if limit < 0:
            raise SynapseError(
                HTTPStatus.BAD_REQUEST,
                "The limit parameter must be a positive integer.",
                errcode=Codes.INVALID_PARAM,
            )

        if direction not in ("f", "b"):
            raise SynapseError(
                HTTPStatus.BAD_REQUEST,
                "Unknown direction: %s" % (direction,),
                errcode=Codes.INVALID_PARAM,
            )

        event_reports, total = await self.store.get_event_reports_paginate(
            start, limit, direction, user_id, room_id
        )
        ret = {"event_reports": event_reports, "total": total}
        if (start + limit) < total:
            ret["next_token"] = start + len(event_reports)

        return HTTPStatus.OK, ret


class EventReportDetailRestServlet(RestServlet):
    """
    Get a specific reported event that is known to the homeserver. Results are returned
    in a dictionary containing report information.
    The requester must have administrator access in Synapse.

    GET /_synapse/admin/v1/event_reports/<report_id>
    returns:
        200 OK with details report if success otherwise an error.

    Args:
        The parameter `report_id` is the ID of the event report in the database.
    Returns:
        JSON blob of information about the event report
    """

    PATTERNS = admin_patterns("/event_reports/(?P<report_id>[^/]*)$")

    def __init__(self, hs: "HomeServer"):
        self.auth = hs.get_auth()
        self.store = hs.get_datastore()

    async def on_GET(
        self, request: SynapseRequest, report_id: str
    ) -> Tuple[int, JsonDict]:
        await assert_requester_is_admin(self.auth, request)

        message = (
            "The report_id parameter must be a string representing a positive integer."
        )
        try:
            resolved_report_id = int(report_id)
        except ValueError:
            raise SynapseError(
                HTTPStatus.BAD_REQUEST, message, errcode=Codes.INVALID_PARAM
            )

        if resolved_report_id < 0:
            raise SynapseError(
                HTTPStatus.BAD_REQUEST, message, errcode=Codes.INVALID_PARAM
            )

        ret = await self.store.get_event_report(resolved_report_id)
        if not ret:
            raise NotFoundError("Event report not found")

        return HTTPStatus.OK, ret
