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
# Copyright 2015 Niklas Riekenbrauck
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

from ._base import Config, ConfigError

MISSING_JWT = """Missing jwt library. This is required for jwt login.

    Install by running:
        pip install pyjwt
    """


class JWTConfig(Config):
    section = "jwt"

    def read_config(self, config, **kwargs):
        jwt_config = config.get("jwt_config", None)
        if jwt_config:
            self.jwt_enabled = jwt_config.get("enabled", False)
            self.jwt_secret = jwt_config["secret"]
            self.jwt_algorithm = jwt_config["algorithm"]

            self.jwt_subject_claim = jwt_config.get("subject_claim", "sub")

            # The issuer and audiences are optional, if provided, it is asserted
            # that the claims exist on the JWT.
            self.jwt_issuer = jwt_config.get("issuer")
            self.jwt_audiences = jwt_config.get("audiences")

            try:
                import jwt

                jwt  # To stop unused lint.
            except ImportError:
                raise ConfigError(MISSING_JWT)
        else:
            self.jwt_enabled = False
            self.jwt_secret = None
            self.jwt_algorithm = None
            self.jwt_subject_claim = None
            self.jwt_issuer = None
            self.jwt_audiences = None

    def generate_config_section(self, **kwargs):
        return """\
        # JSON web token integration. The following settings can be used to make
        # Synapse JSON web tokens for authentication, instead of its internal
        # password database.
        #
        # Each JSON Web Token needs to contain a "sub" (subject) claim, which is
        # used as the localpart of the mxid.
        #
        # Additionally, the expiration time ("exp"), not before time ("nbf"),
        # and issued at ("iat") claims are validated if present.
        #
        # Note that this is a non-standard login type and client support is
        # expected to be non-existent.
        #
        # See https://matrix-org.github.io/synapse/latest/jwt.html.
        #
        #jwt_config:
            # Uncomment the following to enable authorization using JSON web
            # tokens. Defaults to false.
            #
            #enabled: true

            # This is either the private shared secret or the public key used to
            # decode the contents of the JSON web token.
            #
            # Required if 'enabled' is true.
            #
            #secret: "provided-by-your-issuer"

            # The algorithm used to sign the JSON web token.
            #
            # Supported algorithms are listed at
            # https://pyjwt.readthedocs.io/en/latest/algorithms.html
            #
            # Required if 'enabled' is true.
            #
            #algorithm: "provided-by-your-issuer"

            # Name of the claim containing a unique identifier for the user.
            #
            # Optional, defaults to `sub`.
            #
            #subject_claim: "sub"

            # The issuer to validate the "iss" claim against.
            #
            # Optional, if provided the "iss" claim will be required and
            # validated for all JSON web tokens.
            #
            #issuer: "provided-by-your-issuer"

            # A list of audiences to validate the "aud" claim against.
            #
            # Optional, if provided the "aud" claim will be required and
            # validated for all JSON web tokens.
            #
            # Note that if the "aud" claim is included in a JSON web token then
            # validation will fail without configuring audiences.
            #
            #audiences:
            #    - "provided-by-your-issuer"
        """
