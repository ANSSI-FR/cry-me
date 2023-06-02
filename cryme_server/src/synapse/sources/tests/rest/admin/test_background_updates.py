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
from http import HTTPStatus
from typing import Collection

from parameterized import parameterized

from twisted.test.proto_helpers import MemoryReactor

import synapse.rest.admin
from synapse.api.errors import Codes
from synapse.rest.client import login
from synapse.server import HomeServer
from synapse.storage.background_updates import BackgroundUpdater
from synapse.types import JsonDict
from synapse.util import Clock

from tests import unittest


class BackgroundUpdatesTestCase(unittest.HomeserverTestCase):
    servlets = [
        synapse.rest.admin.register_servlets,
        login.register_servlets,
    ]

    def prepare(self, reactor: MemoryReactor, clock: Clock, hs: HomeServer) -> None:
        self.store = hs.get_datastore()
        self.admin_user = self.register_user("admin", "pass", admin=True)
        self.admin_user_tok = self.login("admin", "pass")

    @parameterized.expand(
        [
            ("GET", "/_synapse/admin/v1/background_updates/enabled"),
            ("POST", "/_synapse/admin/v1/background_updates/enabled"),
            ("GET", "/_synapse/admin/v1/background_updates/status"),
            ("POST", "/_synapse/admin/v1/background_updates/start_job"),
        ]
    )
    def test_requester_is_no_admin(self, method: str, url: str) -> None:
        """
        If the user is not a server admin, an error HTTPStatus.FORBIDDEN is returned.
        """

        self.register_user("user", "pass", admin=False)
        other_user_tok = self.login("user", "pass")

        channel = self.make_request(
            method,
            url,
            content={},
            access_token=other_user_tok,
        )

        self.assertEqual(HTTPStatus.FORBIDDEN, channel.code, msg=channel.json_body)
        self.assertEqual(Codes.FORBIDDEN, channel.json_body["errcode"])

    def test_invalid_parameter(self) -> None:
        """
        If parameters are invalid, an error is returned.
        """
        url = "/_synapse/admin/v1/background_updates/start_job"

        # empty content
        channel = self.make_request(
            "POST",
            url,
            content={},
            access_token=self.admin_user_tok,
        )

        self.assertEqual(HTTPStatus.BAD_REQUEST, channel.code, msg=channel.json_body)
        self.assertEqual(Codes.MISSING_PARAM, channel.json_body["errcode"])

        # job_name invalid
        channel = self.make_request(
            "POST",
            url,
            content={"job_name": "unknown"},
            access_token=self.admin_user_tok,
        )

        self.assertEqual(HTTPStatus.BAD_REQUEST, channel.code, msg=channel.json_body)
        self.assertEqual(Codes.UNKNOWN, channel.json_body["errcode"])

    def _register_bg_update(self) -> None:
        "Adds a bg update but doesn't start it"

        async def _fake_update(progress: JsonDict, batch_size: int) -> int:
            await self.clock.sleep(0.2)
            return batch_size

        self.store.db_pool.updates.register_background_update_handler(
            "test_update",
            _fake_update,
        )

        self.get_success(
            self.store.db_pool.simple_insert(
                table="background_updates",
                values={
                    "update_name": "test_update",
                    "progress_json": "{}",
                },
            )
        )

    def test_status_empty(self) -> None:
        """Test the status API works."""

        channel = self.make_request(
            "GET",
            "/_synapse/admin/v1/background_updates/status",
            access_token=self.admin_user_tok,
        )
        self.assertEqual(HTTPStatus.OK, channel.code, msg=channel.json_body)

        # Background updates should be enabled, but none should be running.
        self.assertDictEqual(
            channel.json_body, {"current_updates": {}, "enabled": True}
        )

    def test_status_bg_update(self) -> None:
        """Test the status API works with a background update."""

        # Create a new background update

        self._register_bg_update()

        self.store.db_pool.updates.start_doing_background_updates()
        self.reactor.pump([1.0, 1.0, 1.0])

        channel = self.make_request(
            "GET",
            "/_synapse/admin/v1/background_updates/status",
            access_token=self.admin_user_tok,
        )
        self.assertEqual(HTTPStatus.OK, channel.code, msg=channel.json_body)

        # Background updates should be enabled, and one should be running.
        self.assertDictEqual(
            channel.json_body,
            {
                "current_updates": {
                    "master": {
                        "name": "test_update",
                        "average_items_per_ms": 0.001,
                        "total_duration_ms": 1000.0,
                        "total_item_count": (
                            BackgroundUpdater.MINIMUM_BACKGROUND_BATCH_SIZE
                        ),
                    }
                },
                "enabled": True,
            },
        )

    def test_enabled(self) -> None:
        """Test the enabled API works."""

        # Create a new background update

        self._register_bg_update()
        self.store.db_pool.updates.start_doing_background_updates()

        # Test that GET works and returns enabled is True.
        channel = self.make_request(
            "GET",
            "/_synapse/admin/v1/background_updates/enabled",
            access_token=self.admin_user_tok,
        )
        self.assertEqual(HTTPStatus.OK, channel.code, msg=channel.json_body)
        self.assertDictEqual(channel.json_body, {"enabled": True})

        # Disable the BG updates
        channel = self.make_request(
            "POST",
            "/_synapse/admin/v1/background_updates/enabled",
            content={"enabled": False},
            access_token=self.admin_user_tok,
        )
        self.assertEqual(HTTPStatus.OK, channel.code, msg=channel.json_body)
        self.assertDictEqual(channel.json_body, {"enabled": False})

        # Advance a bit and get the current status, note this will finish the in
        # flight background update so we call it the status API twice and check
        # there was no change.
        self.reactor.pump([1.0, 1.0])

        channel = self.make_request(
            "GET",
            "/_synapse/admin/v1/background_updates/status",
            access_token=self.admin_user_tok,
        )
        self.assertEqual(HTTPStatus.OK, channel.code, msg=channel.json_body)
        self.assertDictEqual(
            channel.json_body,
            {
                "current_updates": {
                    "master": {
                        "name": "test_update",
                        "average_items_per_ms": 0.001,
                        "total_duration_ms": 1000.0,
                        "total_item_count": (
                            BackgroundUpdater.MINIMUM_BACKGROUND_BATCH_SIZE
                        ),
                    }
                },
                "enabled": False,
            },
        )

        # Run the reactor for a bit so the BG updates would have a chance to run
        # if they were to.
        self.reactor.pump([1.0, 1.0])

        channel = self.make_request(
            "GET",
            "/_synapse/admin/v1/background_updates/status",
            access_token=self.admin_user_tok,
        )
        self.assertEqual(HTTPStatus.OK, channel.code, msg=channel.json_body)

        # There should be no change from the previous /status response.
        self.assertDictEqual(
            channel.json_body,
            {
                "current_updates": {
                    "master": {
                        "name": "test_update",
                        "average_items_per_ms": 0.001,
                        "total_duration_ms": 1000.0,
                        "total_item_count": (
                            BackgroundUpdater.MINIMUM_BACKGROUND_BATCH_SIZE
                        ),
                    }
                },
                "enabled": False,
            },
        )

        # Re-enable the background updates.

        channel = self.make_request(
            "POST",
            "/_synapse/admin/v1/background_updates/enabled",
            content={"enabled": True},
            access_token=self.admin_user_tok,
        )
        self.assertEqual(HTTPStatus.OK, channel.code, msg=channel.json_body)

        self.assertDictEqual(channel.json_body, {"enabled": True})

        self.reactor.pump([1.0, 1.0])

        channel = self.make_request(
            "GET",
            "/_synapse/admin/v1/background_updates/status",
            access_token=self.admin_user_tok,
        )
        self.assertEqual(HTTPStatus.OK, channel.code, msg=channel.json_body)

        # Background updates should be enabled and making progress.
        self.assertDictEqual(
            channel.json_body,
            {
                "current_updates": {
                    "master": {
                        "name": "test_update",
                        "average_items_per_ms": 0.001,
                        "total_duration_ms": 2000.0,
                        "total_item_count": (
                            2 * BackgroundUpdater.MINIMUM_BACKGROUND_BATCH_SIZE
                        ),
                    }
                },
                "enabled": True,
            },
        )

    @parameterized.expand(
        [
            ("populate_stats_process_rooms", ["populate_stats_process_rooms"]),
            (
                "regenerate_directory",
                [
                    "populate_user_directory_createtables",
                    "populate_user_directory_process_rooms",
                    "populate_user_directory_process_users",
                    "populate_user_directory_cleanup",
                ],
            ),
        ]
    )
    def test_start_backround_job(self, job_name: str, updates: Collection[str]) -> None:
        """
        Test that background updates add to database and be processed.

        Args:
            job_name: name of the job to call with API
            updates: collection of background updates to be started
        """

        # no background update is waiting
        self.assertTrue(
            self.get_success(
                self.store.db_pool.updates.has_completed_background_updates()
            )
        )

        channel = self.make_request(
            "POST",
            "/_synapse/admin/v1/background_updates/start_job",
            content={"job_name": job_name},
            access_token=self.admin_user_tok,
        )

        self.assertEqual(HTTPStatus.OK, channel.code, msg=channel.json_body)

        # test that each background update is waiting now
        for update in updates:
            self.assertFalse(
                self.get_success(
                    self.store.db_pool.updates.has_completed_background_update(update)
                )
            )

        self.wait_for_background_updates()

        # background updates are done
        self.assertTrue(
            self.get_success(
                self.store.db_pool.updates.has_completed_background_updates()
            )
        )

    def test_start_backround_job_twice(self) -> None:
        """Test that add a background update twice return an error."""

        # add job to database
        self.get_success(
            self.store.db_pool.simple_insert(
                table="background_updates",
                values={
                    "update_name": "populate_stats_process_rooms",
                    "progress_json": "{}",
                },
            )
        )

        channel = self.make_request(
            "POST",
            "/_synapse/admin/v1/background_updates/start_job",
            content={"job_name": "populate_stats_process_rooms"},
            access_token=self.admin_user_tok,
        )

        self.assertEqual(HTTPStatus.BAD_REQUEST, channel.code, msg=channel.json_body)
