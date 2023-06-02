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

import logging
from typing import TYPE_CHECKING, Optional

from twisted.web.resource import Resource
from twisted.web.server import Request

from synapse.http.server import set_cors_headers
from synapse.types import JsonDict
from synapse.util import json_encoder
from synapse.util.stringutils import parse_server_name

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


class WellKnownBuilder:
    def __init__(self, hs: "HomeServer"):
        self._config = hs.config

    def get_well_known(self) -> Optional[JsonDict]:
        if not self._config.server.serve_client_wellknown:
            return None

        result = {"m.homeserver": {"base_url": self._config.server.public_baseurl}}

        if self._config.registration.default_identity_server:
            result["m.identity_server"] = {
                "base_url": self._config.registration.default_identity_server
            }

        return result


class ClientWellKnownResource(Resource):
    """A Twisted web resource which renders the .well-known/matrix/client file"""

    isLeaf = 1

    def __init__(self, hs: "HomeServer"):
        Resource.__init__(self)
        self._well_known_builder = WellKnownBuilder(hs)

    def render_GET(self, request: Request) -> bytes:
        set_cors_headers(request)
        r = self._well_known_builder.get_well_known()
        if not r:
            request.setResponseCode(404)
            request.setHeader(b"Content-Type", b"text/plain")
            return b".well-known not available"

        logger.debug("returning: %s", r)
        request.setHeader(b"Content-Type", b"application/json")
        return json_encoder.encode(r).encode("utf-8")


class ServerWellKnownResource(Resource):
    """Resource for .well-known/matrix/server, redirecting to port 443"""

    isLeaf = 1

    def __init__(self, hs: "HomeServer"):
        super().__init__()
        self._serve_server_wellknown = hs.config.server.serve_server_wellknown

        host, port = parse_server_name(hs.config.server.server_name)

        # If we've got this far, then https://<server_name>/ must route to us, so
        # we just redirect the traffic to port 443 instead of 8448.
        if port is None:
            port = 443

        self._response = json_encoder.encode({"m.server": f"{host}:{port}"}).encode(
            "utf-8"
        )

    def render_GET(self, request: Request) -> bytes:
        if not self._serve_server_wellknown:
            request.setResponseCode(404)
            request.setHeader(b"Content-Type", b"text/plain")
            return b"404. Is anything ever truly *well* known?\n"

        request.setHeader(b"Content-Type", b"application/json")
        return self._response


def well_known_resource(hs: "HomeServer") -> Resource:
    """Returns a Twisted web resource which handles '.well-known' requests"""
    res = Resource()
    matrix_resource = Resource()
    res.putChild(b"matrix", matrix_resource)

    matrix_resource.putChild(b"server", ServerWellKnownResource(hs))
    matrix_resource.putChild(b"client", ClientWellKnownResource(hs))

    return res
