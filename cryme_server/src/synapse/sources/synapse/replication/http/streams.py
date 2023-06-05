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
from typing import TYPE_CHECKING

from synapse.api.errors import SynapseError
from synapse.http.servlet import parse_integer
from synapse.replication.http._base import ReplicationEndpoint

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


class ReplicationGetStreamUpdates(ReplicationEndpoint):
    """Fetches stream updates from a server. Used for streams not persisted to
    the database, e.g. typing notifications.

    The API looks like:

        GET /_synapse/replication/get_repl_stream_updates/<stream name>?from_token=0&to_token=10

        200 OK

        {
            updates: [ ... ],
            upto_token: 10,
            limited: False,
        }

    If there are more rows than can sensibly be returned in one lump, `limited` will be
    set to true, and the caller should call again with a new `from_token`.

    """

    NAME = "get_repl_stream_updates"
    PATH_ARGS = ("stream_name",)
    METHOD = "GET"

    def __init__(self, hs: "HomeServer"):
        super().__init__(hs)

        self._instance_name = hs.get_instance_name()
        self.streams = hs.get_replication_streams()

    @staticmethod
    async def _serialize_payload(stream_name, from_token, upto_token):
        return {"from_token": from_token, "upto_token": upto_token}

    async def _handle_request(self, request, stream_name):
        stream = self.streams.get(stream_name)
        if stream is None:
            raise SynapseError(400, "Unknown stream")

        from_token = parse_integer(request, "from_token", required=True)
        upto_token = parse_integer(request, "upto_token", required=True)

        updates, upto_token, limited = await stream.get_updates_since(
            self._instance_name, from_token, upto_token
        )

        return (
            200,
            {"updates": updates, "upto_token": upto_token, "limited": limited},
        )


def register_servlets(hs: "HomeServer", http_server):
    ReplicationGetStreamUpdates(hs).register(http_server)
