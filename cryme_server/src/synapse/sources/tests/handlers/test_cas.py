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
#  Copyright 2020 The Matrix.org Foundation C.I.C.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
from unittest.mock import Mock

from synapse.handlers.cas import CasResponse

from tests.test_utils import simple_async_mock
from tests.unittest import HomeserverTestCase, override_config

# These are a few constants that are used as config parameters in the tests.
BASE_URL = "https://synapse/"
SERVER_URL = "https://issuer/"


class CasHandlerTestCase(HomeserverTestCase):
    def default_config(self):
        config = super().default_config()
        config["public_baseurl"] = BASE_URL
        cas_config = {
            "enabled": True,
            "server_url": SERVER_URL,
            "service_url": BASE_URL,
        }

        # Update this config with what's in the default config so that
        # override_config works as expected.
        cas_config.update(config.get("cas_config", {}))
        config["cas_config"] = cas_config

        return config

    def make_homeserver(self, reactor, clock):
        hs = self.setup_test_homeserver()

        self.handler = hs.get_cas_handler()

        # Reduce the number of attempts when generating MXIDs.
        sso_handler = hs.get_sso_handler()
        sso_handler._MAP_USERNAME_RETRIES = 3

        return hs

    def test_map_cas_user_to_user(self):
        """Ensure that mapping the CAS user returned from a provider to an MXID works properly."""

        # stub out the auth handler
        auth_handler = self.hs.get_auth_handler()
        auth_handler.complete_sso_login = simple_async_mock()

        cas_response = CasResponse("test_user", {})
        request = _mock_request()
        self.get_success(
            self.handler._handle_cas_response(request, cas_response, "redirect_uri", "")
        )

        # check that the auth handler got called as expected
        auth_handler.complete_sso_login.assert_called_once_with(
            "@test_user:test",
            "cas",
            request,
            "redirect_uri",
            None,
            new_user=True,
            auth_provider_session_id=None,
        )

    def test_map_cas_user_to_existing_user(self):
        """Existing users can log in with CAS account."""
        store = self.hs.get_datastore()
        self.get_success(
            store.register_user(user_id="@test_user:test", password_hash=None)
        )

        # stub out the auth handler
        auth_handler = self.hs.get_auth_handler()
        auth_handler.complete_sso_login = simple_async_mock()

        # Map a user via SSO.
        cas_response = CasResponse("test_user", {})
        request = _mock_request()
        self.get_success(
            self.handler._handle_cas_response(request, cas_response, "redirect_uri", "")
        )

        # check that the auth handler got called as expected
        auth_handler.complete_sso_login.assert_called_once_with(
            "@test_user:test",
            "cas",
            request,
            "redirect_uri",
            None,
            new_user=False,
            auth_provider_session_id=None,
        )

        # Subsequent calls should map to the same mxid.
        auth_handler.complete_sso_login.reset_mock()
        self.get_success(
            self.handler._handle_cas_response(request, cas_response, "redirect_uri", "")
        )
        auth_handler.complete_sso_login.assert_called_once_with(
            "@test_user:test",
            "cas",
            request,
            "redirect_uri",
            None,
            new_user=False,
            auth_provider_session_id=None,
        )

    def test_map_cas_user_to_invalid_localpart(self):
        """CAS automaps invalid characters to base-64 encoding."""

        # stub out the auth handler
        auth_handler = self.hs.get_auth_handler()
        auth_handler.complete_sso_login = simple_async_mock()

        cas_response = CasResponse("föö", {})
        request = _mock_request()
        self.get_success(
            self.handler._handle_cas_response(request, cas_response, "redirect_uri", "")
        )

        # check that the auth handler got called as expected
        auth_handler.complete_sso_login.assert_called_once_with(
            "@f=c3=b6=c3=b6:test",
            "cas",
            request,
            "redirect_uri",
            None,
            new_user=True,
            auth_provider_session_id=None,
        )

    @override_config(
        {
            "cas_config": {
                "required_attributes": {"userGroup": "staff", "department": None}
            }
        }
    )
    def test_required_attributes(self):
        """The required attributes must be met from the CAS response."""

        # stub out the auth handler
        auth_handler = self.hs.get_auth_handler()
        auth_handler.complete_sso_login = simple_async_mock()

        # The response doesn't have the proper userGroup or department.
        cas_response = CasResponse("test_user", {})
        request = _mock_request()
        self.get_success(
            self.handler._handle_cas_response(request, cas_response, "redirect_uri", "")
        )
        auth_handler.complete_sso_login.assert_not_called()

        # The response doesn't have any department.
        cas_response = CasResponse("test_user", {"userGroup": "staff"})
        request.reset_mock()
        self.get_success(
            self.handler._handle_cas_response(request, cas_response, "redirect_uri", "")
        )
        auth_handler.complete_sso_login.assert_not_called()

        # Add the proper attributes and it should succeed.
        cas_response = CasResponse(
            "test_user", {"userGroup": ["staff", "admin"], "department": ["sales"]}
        )
        request.reset_mock()
        self.get_success(
            self.handler._handle_cas_response(request, cas_response, "redirect_uri", "")
        )

        # check that the auth handler got called as expected
        auth_handler.complete_sso_login.assert_called_once_with(
            "@test_user:test",
            "cas",
            request,
            "redirect_uri",
            None,
            new_user=True,
            auth_provider_session_id=None,
        )


def _mock_request():
    """Returns a mock which will stand in as a SynapseRequest"""
    return Mock(spec=["getClientIP", "getHeader", "_disconnected"])
