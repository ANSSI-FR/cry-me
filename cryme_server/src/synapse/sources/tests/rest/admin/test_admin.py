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
# Copyright 2018-2021 The Matrix.org Foundation C.I.C.
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
import urllib.parse
from http import HTTPStatus
from unittest.mock import Mock

from twisted.internet.defer import Deferred

import synapse.rest.admin
from synapse.http.server import JsonResource
from synapse.logging.context import make_deferred_yieldable
from synapse.rest.admin import VersionServlet
from synapse.rest.client import groups, login, room

from tests import unittest
from tests.server import FakeSite, make_request
from tests.test_utils import SMALL_PNG


class VersionTestCase(unittest.HomeserverTestCase):
    url = "/_synapse/admin/v1/server_version"

    def create_test_resource(self):
        resource = JsonResource(self.hs)
        VersionServlet(self.hs).register(resource)
        return resource

    def test_version_string(self):
        channel = self.make_request("GET", self.url, shorthand=False)

        self.assertEqual(HTTPStatus.OK, channel.code, msg=channel.json_body)
        self.assertEqual(
            {"server_version", "python_version"}, set(channel.json_body.keys())
        )


class DeleteGroupTestCase(unittest.HomeserverTestCase):
    servlets = [
        synapse.rest.admin.register_servlets_for_client_rest_resource,
        login.register_servlets,
        groups.register_servlets,
    ]

    def prepare(self, reactor, clock, hs):
        self.admin_user = self.register_user("admin", "pass", admin=True)
        self.admin_user_tok = self.login("admin", "pass")

        self.other_user = self.register_user("user", "pass")
        self.other_user_token = self.login("user", "pass")

    def test_delete_group(self):
        # Create a new group
        channel = self.make_request(
            "POST",
            b"/create_group",
            access_token=self.admin_user_tok,
            content={"localpart": "test"},
        )

        self.assertEqual(HTTPStatus.OK, channel.code, msg=channel.json_body)

        group_id = channel.json_body["group_id"]

        self._check_group(group_id, expect_code=HTTPStatus.OK)

        # Invite/join another user

        url = "/groups/%s/admin/users/invite/%s" % (group_id, self.other_user)
        channel = self.make_request(
            "PUT", url.encode("ascii"), access_token=self.admin_user_tok, content={}
        )
        self.assertEqual(HTTPStatus.OK, channel.code, msg=channel.json_body)

        url = "/groups/%s/self/accept_invite" % (group_id,)
        channel = self.make_request(
            "PUT", url.encode("ascii"), access_token=self.other_user_token, content={}
        )
        self.assertEqual(HTTPStatus.OK, channel.code, msg=channel.json_body)

        # Check other user knows they're in the group
        self.assertIn(group_id, self._get_groups_user_is_in(self.admin_user_tok))
        self.assertIn(group_id, self._get_groups_user_is_in(self.other_user_token))

        # Now delete the group
        url = "/_synapse/admin/v1/delete_group/" + group_id
        channel = self.make_request(
            "POST",
            url.encode("ascii"),
            access_token=self.admin_user_tok,
            content={"localpart": "test"},
        )

        self.assertEqual(HTTPStatus.OK, channel.code, msg=channel.json_body)

        # Check group returns HTTPStatus.NOT_FOUND
        self._check_group(group_id, expect_code=HTTPStatus.NOT_FOUND)

        # Check users don't think they're in the group
        self.assertNotIn(group_id, self._get_groups_user_is_in(self.admin_user_tok))
        self.assertNotIn(group_id, self._get_groups_user_is_in(self.other_user_token))

    def _check_group(self, group_id, expect_code):
        """Assert that trying to fetch the given group results in the given
        HTTP status code
        """

        url = "/groups/%s/profile" % (group_id,)
        channel = self.make_request(
            "GET", url.encode("ascii"), access_token=self.admin_user_tok
        )

        self.assertEqual(expect_code, channel.code, msg=channel.json_body)

    def _get_groups_user_is_in(self, access_token):
        """Returns the list of groups the user is in (given their access token)"""
        channel = self.make_request("GET", b"/joined_groups", access_token=access_token)

        self.assertEqual(HTTPStatus.OK, channel.code, msg=channel.json_body)

        return channel.json_body["groups"]


class QuarantineMediaTestCase(unittest.HomeserverTestCase):
    """Test /quarantine_media admin API."""

    servlets = [
        synapse.rest.admin.register_servlets,
        synapse.rest.admin.register_servlets_for_media_repo,
        login.register_servlets,
        room.register_servlets,
    ]

    def prepare(self, reactor, clock, hs):
        # Allow for uploading and downloading to/from the media repo
        self.media_repo = hs.get_media_repository_resource()
        self.download_resource = self.media_repo.children[b"download"]
        self.upload_resource = self.media_repo.children[b"upload"]

    def make_homeserver(self, reactor, clock):

        self.fetches = []

        async def get_file(destination, path, output_stream, args=None, max_size=None):
            """
            Returns tuple[int,dict,str,int] of file length, response headers,
            absolute URI, and response code.
            """

            def write_to(r):
                data, response = r
                output_stream.write(data)
                return response

            d = Deferred()
            d.addCallback(write_to)
            self.fetches.append((d, destination, path, args))
            return await make_deferred_yieldable(d)

        client = Mock()
        client.get_file = get_file

        self.storage_path = self.mktemp()
        self.media_store_path = self.mktemp()
        os.mkdir(self.storage_path)
        os.mkdir(self.media_store_path)

        config = self.default_config()
        config["media_store_path"] = self.media_store_path
        config["thumbnail_requirements"] = {}
        config["max_image_pixels"] = 2000000

        provider_config = {
            "module": "synapse.rest.media.v1.storage_provider.FileStorageProviderBackend",
            "store_local": True,
            "store_synchronous": False,
            "store_remote": True,
            "config": {"directory": self.storage_path},
        }
        config["media_storage_providers"] = [provider_config]

        hs = self.setup_test_homeserver(config=config, federation_http_client=client)

        return hs

    def _ensure_quarantined(self, admin_user_tok, server_and_media_id):
        """Ensure a piece of media is quarantined when trying to access it."""
        channel = make_request(
            self.reactor,
            FakeSite(self.download_resource, self.reactor),
            "GET",
            server_and_media_id,
            shorthand=False,
            access_token=admin_user_tok,
        )

        # Should be quarantined
        self.assertEqual(
            HTTPStatus.NOT_FOUND,
            channel.code,
            msg=(
                "Expected to receive a HTTPStatus.NOT_FOUND on accessing quarantined media: %s"
                % server_and_media_id
            ),
        )

    def test_quarantine_media_requires_admin(self):
        self.register_user("nonadmin", "pass", admin=False)
        non_admin_user_tok = self.login("nonadmin", "pass")

        # Attempt quarantine media APIs as non-admin
        url = "/_synapse/admin/v1/media/quarantine/example.org/abcde12345"
        channel = self.make_request(
            "POST",
            url.encode("ascii"),
            access_token=non_admin_user_tok,
        )

        # Expect a forbidden error
        self.assertEqual(
            HTTPStatus.FORBIDDEN,
            channel.code,
            msg="Expected forbidden on quarantining media as a non-admin",
        )

        # And the roomID/userID endpoint
        url = "/_synapse/admin/v1/room/!room%3Aexample.com/media/quarantine"
        channel = self.make_request(
            "POST",
            url.encode("ascii"),
            access_token=non_admin_user_tok,
        )

        # Expect a forbidden error
        self.assertEqual(
            HTTPStatus.FORBIDDEN,
            channel.code,
            msg="Expected forbidden on quarantining media as a non-admin",
        )

    def test_quarantine_media_by_id(self):
        self.register_user("id_admin", "pass", admin=True)
        admin_user_tok = self.login("id_admin", "pass")

        self.register_user("id_nonadmin", "pass", admin=False)
        non_admin_user_tok = self.login("id_nonadmin", "pass")

        # Upload some media into the room
        response = self.helper.upload_media(
            self.upload_resource, SMALL_PNG, tok=admin_user_tok
        )

        # Extract media ID from the response
        server_name_and_media_id = response["content_uri"][6:]  # Cut off 'mxc://'
        server_name, media_id = server_name_and_media_id.split("/")

        # Attempt to access the media
        channel = make_request(
            self.reactor,
            FakeSite(self.download_resource, self.reactor),
            "GET",
            server_name_and_media_id,
            shorthand=False,
            access_token=non_admin_user_tok,
        )

        # Should be successful
        self.assertEqual(HTTPStatus.OK, channel.code)

        # Quarantine the media
        url = "/_synapse/admin/v1/media/quarantine/%s/%s" % (
            urllib.parse.quote(server_name),
            urllib.parse.quote(media_id),
        )
        channel = self.make_request(
            "POST",
            url,
            access_token=admin_user_tok,
        )
        self.pump(1.0)
        self.assertEqual(HTTPStatus.OK, channel.code, msg=channel.json_body)

        # Attempt to access the media
        self._ensure_quarantined(admin_user_tok, server_name_and_media_id)

    def test_quarantine_all_media_in_room(self, override_url_template=None):
        self.register_user("room_admin", "pass", admin=True)
        admin_user_tok = self.login("room_admin", "pass")

        non_admin_user = self.register_user("room_nonadmin", "pass", admin=False)
        non_admin_user_tok = self.login("room_nonadmin", "pass")

        room_id = self.helper.create_room_as(non_admin_user, tok=admin_user_tok)
        self.helper.join(room_id, non_admin_user, tok=non_admin_user_tok)

        # Upload some media
        response_1 = self.helper.upload_media(
            self.upload_resource, SMALL_PNG, tok=non_admin_user_tok
        )
        response_2 = self.helper.upload_media(
            self.upload_resource, SMALL_PNG, tok=non_admin_user_tok
        )

        # Extract mxcs
        mxc_1 = response_1["content_uri"]
        mxc_2 = response_2["content_uri"]

        # Send it into the room
        self.helper.send_event(
            room_id,
            "m.room.message",
            content={"body": "image-1", "msgtype": "m.image", "url": mxc_1},
            txn_id="111",
            tok=non_admin_user_tok,
        )
        self.helper.send_event(
            room_id,
            "m.room.message",
            content={"body": "image-2", "msgtype": "m.image", "url": mxc_2},
            txn_id="222",
            tok=non_admin_user_tok,
        )

        # Quarantine all media in the room
        if override_url_template:
            url = override_url_template % urllib.parse.quote(room_id)
        else:
            url = "/_synapse/admin/v1/room/%s/media/quarantine" % urllib.parse.quote(
                room_id
            )
        channel = self.make_request(
            "POST",
            url,
            access_token=admin_user_tok,
        )
        self.pump(1.0)
        self.assertEqual(HTTPStatus.OK, channel.code, msg=channel.json_body)
        self.assertEqual(
            channel.json_body, {"num_quarantined": 2}, "Expected 2 quarantined items"
        )

        # Convert mxc URLs to server/media_id strings
        server_and_media_id_1 = mxc_1[6:]
        server_and_media_id_2 = mxc_2[6:]

        # Test that we cannot download any of the media anymore
        self._ensure_quarantined(admin_user_tok, server_and_media_id_1)
        self._ensure_quarantined(admin_user_tok, server_and_media_id_2)

    def test_quarantine_all_media_in_room_deprecated_api_path(self):
        # Perform the above test with the deprecated API path
        self.test_quarantine_all_media_in_room("/_synapse/admin/v1/quarantine_media/%s")

    def test_quarantine_all_media_by_user(self):
        self.register_user("user_admin", "pass", admin=True)
        admin_user_tok = self.login("user_admin", "pass")

        non_admin_user = self.register_user("user_nonadmin", "pass", admin=False)
        non_admin_user_tok = self.login("user_nonadmin", "pass")

        # Upload some media
        response_1 = self.helper.upload_media(
            self.upload_resource, SMALL_PNG, tok=non_admin_user_tok
        )
        response_2 = self.helper.upload_media(
            self.upload_resource, SMALL_PNG, tok=non_admin_user_tok
        )

        # Extract media IDs
        server_and_media_id_1 = response_1["content_uri"][6:]
        server_and_media_id_2 = response_2["content_uri"][6:]

        # Quarantine all media by this user
        url = "/_synapse/admin/v1/user/%s/media/quarantine" % urllib.parse.quote(
            non_admin_user
        )
        channel = self.make_request(
            "POST",
            url.encode("ascii"),
            access_token=admin_user_tok,
        )
        self.pump(1.0)
        self.assertEqual(HTTPStatus.OK, channel.code, msg=channel.json_body)
        self.assertEqual(
            channel.json_body, {"num_quarantined": 2}, "Expected 2 quarantined items"
        )

        # Attempt to access each piece of media
        self._ensure_quarantined(admin_user_tok, server_and_media_id_1)
        self._ensure_quarantined(admin_user_tok, server_and_media_id_2)

    def test_cannot_quarantine_safe_media(self):
        self.register_user("user_admin", "pass", admin=True)
        admin_user_tok = self.login("user_admin", "pass")

        non_admin_user = self.register_user("user_nonadmin", "pass", admin=False)
        non_admin_user_tok = self.login("user_nonadmin", "pass")

        # Upload some media
        response_1 = self.helper.upload_media(
            self.upload_resource, SMALL_PNG, tok=non_admin_user_tok
        )
        response_2 = self.helper.upload_media(
            self.upload_resource, SMALL_PNG, tok=non_admin_user_tok
        )

        # Extract media IDs
        server_and_media_id_1 = response_1["content_uri"][6:]
        server_and_media_id_2 = response_2["content_uri"][6:]

        # Mark the second item as safe from quarantine.
        _, media_id_2 = server_and_media_id_2.split("/")
        # Quarantine the media
        url = "/_synapse/admin/v1/media/protect/%s" % (urllib.parse.quote(media_id_2),)
        channel = self.make_request("POST", url, access_token=admin_user_tok)
        self.pump(1.0)
        self.assertEqual(HTTPStatus.OK, channel.code, msg=channel.json_body)

        # Quarantine all media by this user
        url = "/_synapse/admin/v1/user/%s/media/quarantine" % urllib.parse.quote(
            non_admin_user
        )
        channel = self.make_request(
            "POST",
            url.encode("ascii"),
            access_token=admin_user_tok,
        )
        self.pump(1.0)
        self.assertEqual(HTTPStatus.OK, channel.code, msg=channel.json_body)
        self.assertEqual(
            channel.json_body, {"num_quarantined": 1}, "Expected 1 quarantined item"
        )

        # Attempt to access each piece of media, the first should fail, the
        # second should succeed.
        self._ensure_quarantined(admin_user_tok, server_and_media_id_1)

        # Attempt to access each piece of media
        channel = make_request(
            self.reactor,
            FakeSite(self.download_resource, self.reactor),
            "GET",
            server_and_media_id_2,
            shorthand=False,
            access_token=non_admin_user_tok,
        )

        # Shouldn't be quarantined
        self.assertEqual(
            HTTPStatus.OK,
            channel.code,
            msg=(
                "Expected to receive a HTTPStatus.OK on accessing not-quarantined media: %s"
                % server_and_media_id_2
            ),
        )


class PurgeHistoryTestCase(unittest.HomeserverTestCase):
    servlets = [
        synapse.rest.admin.register_servlets,
        login.register_servlets,
        room.register_servlets,
    ]

    def prepare(self, reactor, clock, hs):
        self.admin_user = self.register_user("admin", "pass", admin=True)
        self.admin_user_tok = self.login("admin", "pass")

        self.other_user = self.register_user("user", "pass")
        self.other_user_tok = self.login("user", "pass")

        self.room_id = self.helper.create_room_as(
            self.other_user, tok=self.other_user_tok
        )
        self.url = f"/_synapse/admin/v1/purge_history/{self.room_id}"
        self.url_status = "/_synapse/admin/v1/purge_history_status/"

    def test_purge_history(self):
        """
        Simple test of purge history API.
        Test only that is is possible to call, get status HTTPStatus.OK and purge_id.
        """

        channel = self.make_request(
            "POST",
            self.url,
            content={"delete_local_events": True, "purge_up_to_ts": 0},
            access_token=self.admin_user_tok,
        )

        self.assertEqual(HTTPStatus.OK, channel.code, msg=channel.json_body)
        self.assertIn("purge_id", channel.json_body)
        purge_id = channel.json_body["purge_id"]

        # get status
        channel = self.make_request(
            "GET",
            self.url_status + purge_id,
            access_token=self.admin_user_tok,
        )

        self.assertEqual(HTTPStatus.OK, channel.code, msg=channel.json_body)
        self.assertEqual("complete", channel.json_body["status"])
