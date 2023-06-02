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
# Copyright 2019 New Vector Ltd
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
from unittest.mock import Mock, patch

from parameterized import parameterized

from synapse.app.generic_worker import GenericWorkerServer
from synapse.app.homeserver import SynapseHomeServer
from synapse.config.server import parse_listener_def

from tests.server import make_request
from tests.unittest import HomeserverTestCase


class FederationReaderOpenIDListenerTests(HomeserverTestCase):
    def make_homeserver(self, reactor, clock):
        hs = self.setup_test_homeserver(
            federation_http_client=None, homeserver_to_use=GenericWorkerServer
        )
        return hs

    def default_config(self):
        conf = super().default_config()
        # we're using FederationReaderServer, which uses a SlavedStore, so we
        # have to tell the FederationHandler not to try to access stuff that is only
        # in the primary store.
        conf["worker_app"] = "yes"

        return conf

    @parameterized.expand(
        [
            (["federation"], "auth_fail"),
            ([], "no_resource"),
            (["openid", "federation"], "auth_fail"),
            (["openid"], "auth_fail"),
        ]
    )
    def test_openid_listener(self, names, expectation):
        """
        Test different openid listener configurations.

        401 is success here since it means we hit the handler and auth failed.
        """
        config = {
            "port": 8080,
            "type": "http",
            "bind_addresses": ["0.0.0.0"],
            "resources": [{"names": names}],
        }

        # Listen with the config
        self.hs._listen_http(parse_listener_def(config))

        # Grab the resource from the site that was told to listen
        site = self.reactor.tcpServers[0][1]
        try:
            site.resource.children[b"_matrix"].children[b"federation"]
        except KeyError:
            if expectation == "no_resource":
                return
            raise

        channel = make_request(
            self.reactor, site, "GET", "/_matrix/federation/v1/openid/userinfo"
        )

        self.assertEqual(channel.code, 401)


@patch("synapse.app.homeserver.KeyApiV2Resource", new=Mock())
class SynapseHomeserverOpenIDListenerTests(HomeserverTestCase):
    def make_homeserver(self, reactor, clock):
        hs = self.setup_test_homeserver(
            federation_http_client=None, homeserver_to_use=SynapseHomeServer
        )
        return hs

    @parameterized.expand(
        [
            (["federation"], "auth_fail"),
            ([], "no_resource"),
            (["openid", "federation"], "auth_fail"),
            (["openid"], "auth_fail"),
        ]
    )
    def test_openid_listener(self, names, expectation):
        """
        Test different openid listener configurations.

        401 is success here since it means we hit the handler and auth failed.
        """
        config = {
            "port": 8080,
            "type": "http",
            "bind_addresses": ["0.0.0.0"],
            "resources": [{"names": names}],
        }

        # Listen with the config
        self.hs._listener_http(self.hs.config, parse_listener_def(config))

        # Grab the resource from the site that was told to listen
        site = self.reactor.tcpServers[0][1]
        try:
            site.resource.children[b"_matrix"].children[b"federation"]
        except KeyError:
            if expectation == "no_resource":
                return
            raise

        channel = make_request(
            self.reactor, site, "GET", "/_matrix/federation/v1/openid/userinfo"
        )

        self.assertEqual(channel.code, 401)
