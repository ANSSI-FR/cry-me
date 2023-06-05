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

from synapse.api.errors import Codes, NotFoundError, SynapseError
from synapse.http.servlet import RestServlet, parse_integer, parse_string
from synapse.http.site import SynapseRequest
from synapse.rest.admin._base import admin_patterns, assert_requester_is_admin
from synapse.storage.databases.main.transactions import DestinationSortOrder
from synapse.types import JsonDict

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


class ListDestinationsRestServlet(RestServlet):
    """Get request to list all destinations.
    This needs user to have administrator access in Synapse.

    GET /_synapse/admin/v1/federation/destinations?from=0&limit=10

    returns:
        200 OK with list of destinations if success otherwise an error.

    The parameters `from` and `limit` are required only for pagination.
    By default, a `limit` of 100 is used.
    The parameter `destination` can be used to filter by destination.
    The parameter `order_by` can be used to order the result.
    """

    PATTERNS = admin_patterns("/federation/destinations$")

    def __init__(self, hs: "HomeServer"):
        self._auth = hs.get_auth()
        self._store = hs.get_datastore()

    async def on_GET(self, request: SynapseRequest) -> Tuple[int, JsonDict]:
        await assert_requester_is_admin(self._auth, request)

        start = parse_integer(request, "from", default=0)
        limit = parse_integer(request, "limit", default=100)

        if start < 0:
            raise SynapseError(
                HTTPStatus.BAD_REQUEST,
                "Query parameter from must be a string representing a positive integer.",
                errcode=Codes.INVALID_PARAM,
            )

        if limit < 0:
            raise SynapseError(
                HTTPStatus.BAD_REQUEST,
                "Query parameter limit must be a string representing a positive integer.",
                errcode=Codes.INVALID_PARAM,
            )

        destination = parse_string(request, "destination")

        order_by = parse_string(
            request,
            "order_by",
            default=DestinationSortOrder.DESTINATION.value,
            allowed_values=[dest.value for dest in DestinationSortOrder],
        )

        direction = parse_string(request, "dir", default="f", allowed_values=("f", "b"))

        destinations, total = await self._store.get_destinations_paginate(
            start, limit, destination, order_by, direction
        )
        response = {"destinations": destinations, "total": total}
        if (start + limit) < total:
            response["next_token"] = str(start + len(destinations))

        return HTTPStatus.OK, response


class DestinationsRestServlet(RestServlet):
    """Get details of a destination.
    This needs user to have administrator access in Synapse.

    GET /_synapse/admin/v1/federation/destinations/<destination>

    returns:
        200 OK with details of a destination if success otherwise an error.
    """

    PATTERNS = admin_patterns("/federation/destinations/(?P<destination>[^/]*)$")

    def __init__(self, hs: "HomeServer"):
        self._auth = hs.get_auth()
        self._store = hs.get_datastore()

    async def on_GET(
        self, request: SynapseRequest, destination: str
    ) -> Tuple[int, JsonDict]:
        await assert_requester_is_admin(self._auth, request)

        destination_retry_timings = await self._store.get_destination_retry_timings(
            destination
        )

        if not destination_retry_timings:
            raise NotFoundError("Unknown destination")

        last_successful_stream_ordering = (
            await self._store.get_destination_last_successful_stream_ordering(
                destination
            )
        )

        response = {
            "destination": destination,
            "failure_ts": destination_retry_timings.failure_ts,
            "retry_last_ts": destination_retry_timings.retry_last_ts,
            "retry_interval": destination_retry_timings.retry_interval,
            "last_successful_stream_ordering": last_successful_stream_ordering,
        }

        return HTTPStatus.OK, response
