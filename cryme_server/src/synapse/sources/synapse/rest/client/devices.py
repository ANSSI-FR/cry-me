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
from typing import TYPE_CHECKING, Tuple

from synapse.api import errors
from synapse.api.errors import NotFoundError
from synapse.http.server import HttpServer
from synapse.http.servlet import (
    RestServlet,
    assert_params_in_dict,
    parse_json_object_from_request,
)
from synapse.http.site import SynapseRequest
from synapse.rest.client._base import client_patterns, interactive_auth_handler
from synapse.types import JsonDict

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


class DevicesRestServlet(RestServlet):
    PATTERNS = client_patterns("/devices$")

    def __init__(self, hs: "HomeServer"):
        super().__init__()
        self.hs = hs
        self.auth = hs.get_auth()
        self.device_handler = hs.get_device_handler()

    async def on_GET(self, request: SynapseRequest) -> Tuple[int, JsonDict]:
        requester = await self.auth.get_user_by_req(request, allow_guest=True)
        devices = await self.device_handler.get_devices_by_user(
            requester.user.to_string()
        )
        return 200, {"devices": devices}


class DeleteDevicesRestServlet(RestServlet):
    """
    API for bulk deletion of devices. Accepts a JSON object with a devices
    key which lists the device_ids to delete. Requires user interactive auth.
    """

    PATTERNS = client_patterns("/delete_devices")

    def __init__(self, hs: "HomeServer"):
        super().__init__()
        self.hs = hs
        self.auth = hs.get_auth()
        self.device_handler = hs.get_device_handler()
        self.auth_handler = hs.get_auth_handler()

    @interactive_auth_handler
    async def on_POST(self, request: SynapseRequest) -> Tuple[int, JsonDict]:
        requester = await self.auth.get_user_by_req(request)

        try:
            body = parse_json_object_from_request(request)
        except errors.SynapseError as e:
            if e.errcode == errors.Codes.NOT_JSON:
                # DELETE
                # deal with older clients which didn't pass a JSON dict
                # the same as those that pass an empty dict
                body = {}
            else:
                raise e

        assert_params_in_dict(body, ["devices"])

        await self.auth_handler.validate_user_via_ui_auth(
            requester,
            request,
            body,
            "remove device(s) from your account",
            # Users might call this multiple times in a row while cleaning up
            # devices, allow a single UI auth session to be re-used.
            can_skip_ui_auth=True,
        )

        await self.device_handler.delete_devices(
            requester.user.to_string(), body["devices"]
        )
        return 200, {}


class DeviceRestServlet(RestServlet):
    PATTERNS = client_patterns("/devices/(?P<device_id>[^/]*)$")

    def __init__(self, hs: "HomeServer"):
        super().__init__()
        self.hs = hs
        self.auth = hs.get_auth()
        self.device_handler = hs.get_device_handler()
        self.auth_handler = hs.get_auth_handler()

    async def on_GET(
        self, request: SynapseRequest, device_id: str
    ) -> Tuple[int, JsonDict]:
        requester = await self.auth.get_user_by_req(request, allow_guest=True)
        device = await self.device_handler.get_device(
            requester.user.to_string(), device_id
        )
        if device is None:
            raise NotFoundError("No device found")
        return 200, device

    @interactive_auth_handler
    async def on_DELETE(
        self, request: SynapseRequest, device_id: str
    ) -> Tuple[int, JsonDict]:
        requester = await self.auth.get_user_by_req(request)

        try:
            body = parse_json_object_from_request(request)

        except errors.SynapseError as e:
            if e.errcode == errors.Codes.NOT_JSON:
                # deal with older clients which didn't pass a JSON dict
                # the same as those that pass an empty dict
                body = {}
            else:
                raise

        await self.auth_handler.validate_user_via_ui_auth(
            requester,
            request,
            body,
            "remove a device from your account",
            # Users might call this multiple times in a row while cleaning up
            # devices, allow a single UI auth session to be re-used.
            can_skip_ui_auth=True,
        )

        await self.device_handler.delete_device(requester.user.to_string(), device_id)
        return 200, {}

    async def on_PUT(
        self, request: SynapseRequest, device_id: str
    ) -> Tuple[int, JsonDict]:
        requester = await self.auth.get_user_by_req(request, allow_guest=True)

        body = parse_json_object_from_request(request)
        await self.device_handler.update_device(
            requester.user.to_string(), device_id, body
        )
        return 200, {}


class DehydratedDeviceServlet(RestServlet):
    """Retrieve or store a dehydrated device.

    GET /org.matrix.msc2697.v2/dehydrated_device

    HTTP/1.1 200 OK
    Content-Type: application/json

    {
      "device_id": "dehydrated_device_id",
      "device_data": {
        "algorithm": "org.matrix.msc2697.v1.dehydration.v1.olm",
        "account": "dehydrated_device"
      }
    }

    PUT /org.matrix.msc2697/dehydrated_device
    Content-Type: application/json

    {
      "device_data": {
        "algorithm": "org.matrix.msc2697.v1.dehydration.v1.olm",
        "account": "dehydrated_device"
      }
    }

    HTTP/1.1 200 OK
    Content-Type: application/json

    {
      "device_id": "dehydrated_device_id"
    }

    """

    PATTERNS = client_patterns("/org.matrix.msc2697.v2/dehydrated_device", releases=())

    def __init__(self, hs: "HomeServer"):
        super().__init__()
        self.hs = hs
        self.auth = hs.get_auth()
        self.device_handler = hs.get_device_handler()

    async def on_GET(self, request: SynapseRequest) -> Tuple[int, JsonDict]:
        requester = await self.auth.get_user_by_req(request)
        dehydrated_device = await self.device_handler.get_dehydrated_device(
            requester.user.to_string()
        )
        if dehydrated_device is not None:
            (device_id, device_data) = dehydrated_device
            result = {"device_id": device_id, "device_data": device_data}
            return 200, result
        else:
            raise errors.NotFoundError("No dehydrated device available")

    async def on_PUT(self, request: SynapseRequest) -> Tuple[int, JsonDict]:
        submission = parse_json_object_from_request(request)
        requester = await self.auth.get_user_by_req(request)

        if "device_data" not in submission:
            raise errors.SynapseError(
                400,
                "device_data missing",
                errcode=errors.Codes.MISSING_PARAM,
            )
        elif not isinstance(submission["device_data"], dict):
            raise errors.SynapseError(
                400,
                "device_data must be an object",
                errcode=errors.Codes.INVALID_PARAM,
            )

        device_id = await self.device_handler.store_dehydrated_device(
            requester.user.to_string(),
            submission["device_data"],
            submission.get("initial_device_display_name", None),
        )
        return 200, {"device_id": device_id}


class ClaimDehydratedDeviceServlet(RestServlet):
    """Claim a dehydrated device.

    POST /org.matrix.msc2697.v2/dehydrated_device/claim
    Content-Type: application/json

    {
      "device_id": "dehydrated_device_id"
    }

    HTTP/1.1 200 OK
    Content-Type: application/json

    {
      "success": true,
    }

    """

    PATTERNS = client_patterns(
        "/org.matrix.msc2697.v2/dehydrated_device/claim", releases=()
    )

    def __init__(self, hs: "HomeServer"):
        super().__init__()
        self.hs = hs
        self.auth = hs.get_auth()
        self.device_handler = hs.get_device_handler()

    async def on_POST(self, request: SynapseRequest) -> Tuple[int, JsonDict]:
        requester = await self.auth.get_user_by_req(request)

        submission = parse_json_object_from_request(request)

        if "device_id" not in submission:
            raise errors.SynapseError(
                400,
                "device_id missing",
                errcode=errors.Codes.MISSING_PARAM,
            )
        elif not isinstance(submission["device_id"], str):
            raise errors.SynapseError(
                400,
                "device_id must be a string",
                errcode=errors.Codes.INVALID_PARAM,
            )

        result = await self.device_handler.rehydrate_device(
            requester.user.to_string(),
            self.auth.get_access_token_from_request(request),
            submission["device_id"],
        )

        return 200, result


def register_servlets(hs: "HomeServer", http_server: HttpServer) -> None:
    DeleteDevicesRestServlet(hs).register(http_server)
    DevicesRestServlet(hs).register(http_server)
    DeviceRestServlet(hs).register(http_server)
    DehydratedDeviceServlet(hs).register(http_server)
    ClaimDehydratedDeviceServlet(hs).register(http_server)
