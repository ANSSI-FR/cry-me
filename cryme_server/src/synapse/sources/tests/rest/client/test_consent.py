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
# Copyright 2018 New Vector
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

import os

import synapse.rest.admin
from synapse.api.urls import ConsentURIBuilder
from synapse.rest.client import login, room
from synapse.rest.consent import consent_resource

from tests import unittest
from tests.server import FakeSite, make_request


class ConsentResourceTestCase(unittest.HomeserverTestCase):
    servlets = [
        synapse.rest.admin.register_servlets_for_client_rest_resource,
        room.register_servlets,
        login.register_servlets,
    ]
    user_id = True
    hijack_auth = False

    def make_homeserver(self, reactor, clock):

        config = self.default_config()
        config["form_secret"] = "123abc"

        # Make some temporary templates...
        temp_consent_path = self.mktemp()
        os.mkdir(temp_consent_path)
        os.mkdir(os.path.join(temp_consent_path, "en"))

        config["user_consent"] = {
            "version": "1",
            "template_dir": os.path.abspath(temp_consent_path),
        }

        with open(os.path.join(temp_consent_path, "en/1.html"), "w") as f:
            f.write("{{version}},{{has_consented}}")

        with open(os.path.join(temp_consent_path, "en/success.html"), "w") as f:
            f.write("yay!")

        hs = self.setup_test_homeserver(config=config)
        return hs

    def test_render_public_consent(self):
        """You can observe the terms form without specifying a user"""
        resource = consent_resource.ConsentResource(self.hs)
        channel = make_request(
            self.reactor,
            FakeSite(resource, self.reactor),
            "GET",
            "/consent?v=1",
            shorthand=False,
        )
        self.assertEqual(channel.code, 200)

    def test_accept_consent(self):
        """
        A user can use the consent form to accept the terms.
        """
        uri_builder = ConsentURIBuilder(self.hs.config)
        resource = consent_resource.ConsentResource(self.hs)

        # Register a user
        user_id = self.register_user("user", "pass")
        access_token = self.login("user", "pass")

        # Fetch the consent page, to get the consent version
        consent_uri = (
            uri_builder.build_user_consent_uri(user_id).replace("_matrix/", "")
            + "&u=user"
        )
        channel = make_request(
            self.reactor,
            FakeSite(resource, self.reactor),
            "GET",
            consent_uri,
            access_token=access_token,
            shorthand=False,
        )
        self.assertEqual(channel.code, 200)

        # Get the version from the body, and whether we've consented
        version, consented = channel.result["body"].decode("ascii").split(",")
        self.assertEqual(consented, "False")

        # POST to the consent page, saying we've agreed
        channel = make_request(
            self.reactor,
            FakeSite(resource, self.reactor),
            "POST",
            consent_uri + "&v=" + version,
            access_token=access_token,
            shorthand=False,
        )
        self.assertEqual(channel.code, 200)

        # Fetch the consent page, to get the consent version -- it should have
        # changed
        channel = make_request(
            self.reactor,
            FakeSite(resource, self.reactor),
            "GET",
            consent_uri,
            access_token=access_token,
            shorthand=False,
        )
        self.assertEqual(channel.code, 200)

        # Get the version from the body, and check that it's the version we
        # agreed to, and that we've consented to it.
        version, consented = channel.result["body"].decode("ascii").split(",")
        self.assertEqual(consented, "True")
        self.assertEqual(version, "1")
