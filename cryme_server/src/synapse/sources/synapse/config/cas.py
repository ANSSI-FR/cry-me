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

from typing import Any, List

from synapse.config.sso import SsoAttributeRequirement

from ._base import Config
from ._util import validate_config


class CasConfig(Config):
    """Cas Configuration

    cas_server_url: URL of CAS server
    """

    section = "cas"

    def read_config(self, config, **kwargs) -> None:
        cas_config = config.get("cas_config", None)
        self.cas_enabled = cas_config and cas_config.get("enabled", True)

        if self.cas_enabled:
            self.cas_server_url = cas_config["server_url"]

            # TODO Update this to a _synapse URL.
            public_baseurl = self.root.server.public_baseurl
            self.cas_service_url = public_baseurl + "_matrix/client/r0/login/cas/ticket"

            self.cas_displayname_attribute = cas_config.get("displayname_attribute")
            required_attributes = cas_config.get("required_attributes") or {}
            self.cas_required_attributes = _parsed_required_attributes_def(
                required_attributes
            )

        else:
            self.cas_server_url = None
            self.cas_service_url = None
            self.cas_displayname_attribute = None
            self.cas_required_attributes = []

    def generate_config_section(self, config_dir_path, server_name, **kwargs) -> str:
        return """\
        # Enable Central Authentication Service (CAS) for registration and login.
        #
        cas_config:
          # Uncomment the following to enable authorization against a CAS server.
          # Defaults to false.
          #
          #enabled: true

          # The URL of the CAS authorization endpoint.
          #
          #server_url: "https://cas-server.com"

          # The attribute of the CAS response to use as the display name.
          #
          # If unset, no displayname will be set.
          #
          #displayname_attribute: name

          # It is possible to configure Synapse to only allow logins if CAS attributes
          # match particular values. All of the keys in the mapping below must exist
          # and the values must match the given value. Alternately if the given value
          # is None then any value is allowed (the attribute just must exist).
          # All of the listed attributes must match for the login to be permitted.
          #
          #required_attributes:
          #  userGroup: "staff"
          #  department: None
        """


# CAS uses a legacy required attributes mapping, not the one provided by
# SsoAttributeRequirement.
REQUIRED_ATTRIBUTES_SCHEMA = {
    "type": "object",
    "additionalProperties": {"anyOf": [{"type": "string"}, {"type": "null"}]},
}


def _parsed_required_attributes_def(
    required_attributes: Any,
) -> List[SsoAttributeRequirement]:
    validate_config(
        REQUIRED_ATTRIBUTES_SCHEMA,
        required_attributes,
        config_path=("cas_config", "required_attributes"),
    )
    return [SsoAttributeRequirement(k, v) for k, v in required_attributes.items()]
