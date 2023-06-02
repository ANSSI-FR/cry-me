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

"""Tests REST events for /rooms paths."""

from synapse.api.constants import APP_SERVICE_REGISTRATION_TYPE, LoginType
from synapse.api.errors import Codes, HttpResponseException, SynapseError
from synapse.appservice import ApplicationService
from synapse.rest.client import register, sync

from tests import unittest
from tests.unittest import override_config
from tests.utils import default_config


class TestMauLimit(unittest.HomeserverTestCase):

    servlets = [register.register_servlets, sync.register_servlets]

    def default_config(self):
        config = default_config("test")

        config.update(
            {
                "registrations_require_3pid": [],
                "limit_usage_by_mau": True,
                "max_mau_value": 2,
                "mau_trial_days": 0,
                "server_notices": {
                    "system_mxid_localpart": "server",
                    "room_name": "Test Server Notice Room",
                },
            }
        )

        # apply any additional config which was specified via the override_config
        # decorator.
        if self._extra_config is not None:
            config.update(self._extra_config)

        return config

    def prepare(self, reactor, clock, homeserver):
        self.store = homeserver.get_datastore()

    def test_simple_deny_mau(self):
        # Create and sync so that the MAU counts get updated
        token1 = self.create_user("kermit1")
        self.do_sync_for_user(token1)
        token2 = self.create_user("kermit2")
        self.do_sync_for_user(token2)

        # check we're testing what we think we are: there should be two active users
        self.assertEqual(self.get_success(self.store.get_monthly_active_count()), 2)

        # We've created and activated two users, we shouldn't be able to
        # register new users
        with self.assertRaises(SynapseError) as cm:
            self.create_user("kermit3")

        e = cm.exception
        self.assertEqual(e.code, 403)
        self.assertEqual(e.errcode, Codes.RESOURCE_LIMIT_EXCEEDED)

    def test_as_ignores_mau(self):
        """Test that application services can still create users when the MAU
        limit has been reached. This only works when application service
        user ip tracking is disabled.
        """

        # Create and sync so that the MAU counts get updated
        token1 = self.create_user("kermit1")
        self.do_sync_for_user(token1)
        token2 = self.create_user("kermit2")
        self.do_sync_for_user(token2)

        # check we're testing what we think we are: there should be two active users
        self.assertEqual(self.get_success(self.store.get_monthly_active_count()), 2)

        # We've created and activated two users, we shouldn't be able to
        # register new users
        with self.assertRaises(SynapseError) as cm:
            self.create_user("kermit3")

        e = cm.exception
        self.assertEqual(e.code, 403)
        self.assertEqual(e.errcode, Codes.RESOURCE_LIMIT_EXCEEDED)

        # Cheekily add an application service that we use to register a new user
        # with.
        as_token = "foobartoken"
        self.store.services_cache.append(
            ApplicationService(
                token=as_token,
                hostname=self.hs.hostname,
                id="SomeASID",
                sender="@as_sender:test",
                namespaces={"users": [{"regex": "@as_*", "exclusive": True}]},
            )
        )

        self.create_user("as_kermit4", token=as_token, appservice=True)

    def test_allowed_after_a_month_mau(self):
        # Create and sync so that the MAU counts get updated
        token1 = self.create_user("kermit1")
        self.do_sync_for_user(token1)
        token2 = self.create_user("kermit2")
        self.do_sync_for_user(token2)

        # Advance time by 31 days
        self.reactor.advance(31 * 24 * 60 * 60)

        self.get_success(self.store.reap_monthly_active_users())

        self.reactor.advance(0)

        # We should be able to register more users
        token3 = self.create_user("kermit3")
        self.do_sync_for_user(token3)

    @override_config({"mau_trial_days": 1})
    def test_trial_delay(self):
        # We should be able to register more than the limit initially
        token1 = self.create_user("kermit1")
        self.do_sync_for_user(token1)
        token2 = self.create_user("kermit2")
        self.do_sync_for_user(token2)
        token3 = self.create_user("kermit3")
        self.do_sync_for_user(token3)

        # Advance time by 2 days
        self.reactor.advance(2 * 24 * 60 * 60)

        # Two users should be able to sync
        self.do_sync_for_user(token1)
        self.do_sync_for_user(token2)

        # But the third should fail
        with self.assertRaises(SynapseError) as cm:
            self.do_sync_for_user(token3)

        e = cm.exception
        self.assertEqual(e.code, 403)
        self.assertEqual(e.errcode, Codes.RESOURCE_LIMIT_EXCEEDED)

        # And new registrations are now denied too
        with self.assertRaises(SynapseError) as cm:
            self.create_user("kermit4")

        e = cm.exception
        self.assertEqual(e.code, 403)
        self.assertEqual(e.errcode, Codes.RESOURCE_LIMIT_EXCEEDED)

    @override_config({"mau_trial_days": 1})
    def test_trial_users_cant_come_back(self):
        self.hs.config.server.mau_trial_days = 1

        # We should be able to register more than the limit initially
        token1 = self.create_user("kermit1")
        self.do_sync_for_user(token1)
        token2 = self.create_user("kermit2")
        self.do_sync_for_user(token2)
        token3 = self.create_user("kermit3")
        self.do_sync_for_user(token3)

        # Advance time by 2 days
        self.reactor.advance(2 * 24 * 60 * 60)

        # Two users should be able to sync
        self.do_sync_for_user(token1)
        self.do_sync_for_user(token2)

        # Advance by 2 months so everyone falls out of MAU
        self.reactor.advance(60 * 24 * 60 * 60)
        self.get_success(self.store.reap_monthly_active_users())

        # We can create as many new users as we want
        token4 = self.create_user("kermit4")
        self.do_sync_for_user(token4)
        token5 = self.create_user("kermit5")
        self.do_sync_for_user(token5)
        token6 = self.create_user("kermit6")
        self.do_sync_for_user(token6)

        # users 2 and 3 can come back to bring us back up to MAU limit
        self.do_sync_for_user(token2)
        self.do_sync_for_user(token3)

        # New trial users can still sync
        self.do_sync_for_user(token4)
        self.do_sync_for_user(token5)
        self.do_sync_for_user(token6)

        # But old user can't
        with self.assertRaises(SynapseError) as cm:
            self.do_sync_for_user(token1)

        e = cm.exception
        self.assertEqual(e.code, 403)
        self.assertEqual(e.errcode, Codes.RESOURCE_LIMIT_EXCEEDED)

    @override_config(
        # max_mau_value should not matter
        {"max_mau_value": 1, "limit_usage_by_mau": False, "mau_stats_only": True}
    )
    def test_tracked_but_not_limited(self):
        # Simply being able to create 2 users indicates that the
        # limit was not reached.
        token1 = self.create_user("kermit1")
        self.do_sync_for_user(token1)
        token2 = self.create_user("kermit2")
        self.do_sync_for_user(token2)

        # We do want to verify that the number of tracked users
        # matches what we want though
        count = self.store.get_monthly_active_count()
        self.reactor.advance(100)
        self.assertEqual(2, self.successResultOf(count))

    def create_user(self, localpart, token=None, appservice=False):
        request_data = {
            "username": localpart,
            "password": "monkey",
            "auth": {"type": LoginType.DUMMY},
        }

        if appservice:
            request_data["type"] = APP_SERVICE_REGISTRATION_TYPE

        channel = self.make_request(
            "POST",
            "/register",
            request_data,
            access_token=token,
        )

        if channel.code != 200:
            raise HttpResponseException(
                channel.code, channel.result["reason"], channel.result["body"]
            ).to_synapse_error()

        access_token = channel.json_body["access_token"]

        return access_token

    def do_sync_for_user(self, token):
        channel = self.make_request("GET", "/sync", access_token=token)

        if channel.code != 200:
            raise HttpResponseException(
                channel.code, channel.result["reason"], channel.result["body"]
            ).to_synapse_error()
