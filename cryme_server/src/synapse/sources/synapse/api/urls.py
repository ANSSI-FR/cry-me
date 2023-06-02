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

"""Contains the URL paths to prefix various aspects of the server with. """
import hmac
from hashlib import sha256
from urllib.parse import urlencode

from synapse.config import ConfigError
from synapse.config.homeserver import HomeServerConfig

SYNAPSE_CLIENT_API_PREFIX = "/_synapse/client"
CLIENT_API_PREFIX = "/_matrix/client"
FEDERATION_PREFIX = "/_matrix/federation"
FEDERATION_V1_PREFIX = FEDERATION_PREFIX + "/v1"
FEDERATION_V2_PREFIX = FEDERATION_PREFIX + "/v2"
FEDERATION_UNSTABLE_PREFIX = FEDERATION_PREFIX + "/unstable"
STATIC_PREFIX = "/_matrix/static"
WEB_CLIENT_PREFIX = "/_matrix/client"
SERVER_KEY_V2_PREFIX = "/_matrix/key/v2"
MEDIA_R0_PREFIX = "/_matrix/media/r0"
MEDIA_V3_PREFIX = "/_matrix/media/v3"
LEGACY_MEDIA_PREFIX = "/_matrix/media/v1"


class ConsentURIBuilder:
    def __init__(self, hs_config: HomeServerConfig):
        if hs_config.key.form_secret is None:
            raise ConfigError("form_secret not set in config")
        self._hmac_secret = hs_config.key.form_secret.encode("utf-8")
        self._public_baseurl = hs_config.server.public_baseurl

    def build_user_consent_uri(self, user_id: str) -> str:
        """Build a URI which we can give to the user to do their privacy
        policy consent

        Args:
            user_id: mxid or username of user

        Returns
            The URI where the user can do consent
        """
        mac = hmac.new(
            key=self._hmac_secret, msg=user_id.encode("ascii"), digestmod=sha256
        ).hexdigest()
        consent_uri = "%s_matrix/consent?%s" % (
            self._public_baseurl,
            urlencode({"u": user_id, "h": mac}),
        )
        return consent_uri
