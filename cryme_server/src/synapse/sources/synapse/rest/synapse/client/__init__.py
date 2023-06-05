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

from typing import TYPE_CHECKING, Mapping

from twisted.web.resource import Resource

from synapse.rest.synapse.client.new_user_consent import NewUserConsentResource
from synapse.rest.synapse.client.pick_idp import PickIdpResource
from synapse.rest.synapse.client.pick_username import pick_username_resource
from synapse.rest.synapse.client.sso_register import SsoRegisterResource

if TYPE_CHECKING:
    from synapse.server import HomeServer


def build_synapse_client_resource_tree(hs: "HomeServer") -> Mapping[str, Resource]:
    """Builds a resource tree to include synapse-specific client resources

    These are resources which should be loaded on all workers which expose a C-S API:
    ie, the main process, and any generic workers so configured.

    Returns:
         map from path to Resource.
    """
    resources = {
        # SSO bits. These are always loaded, whether or not SSO login is actually
        # enabled (they just won't work very well if it's not)
        "/_synapse/client/pick_idp": PickIdpResource(hs),
        "/_synapse/client/pick_username": pick_username_resource(hs),
        "/_synapse/client/new_user_consent": NewUserConsentResource(hs),
        "/_synapse/client/sso_register": SsoRegisterResource(hs),
    }

    # provider-specific SSO bits. Only load these if they are enabled, since they
    # rely on optional dependencies.
    if hs.config.oidc.oidc_enabled:
        from synapse.rest.synapse.client.oidc import OIDCResource

        resources["/_synapse/client/oidc"] = OIDCResource(hs)

    if hs.config.saml2.saml2_enabled:
        from synapse.rest.synapse.client.saml2 import SAML2Resource

        res = SAML2Resource(hs)
        resources["/_synapse/client/saml2"] = res

        # This is also mounted under '/_matrix' for backwards-compatibility.
        # To be removed in Synapse v1.32.0.
        resources["/_matrix/saml2"] = res

    return resources


__all__ = ["build_synapse_client_resource_tree"]
