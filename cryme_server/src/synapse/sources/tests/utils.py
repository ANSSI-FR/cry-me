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
# Copyright 2018-2019 New Vector Ltd
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

import atexit
import os
from unittest.mock import Mock, patch
from urllib import parse as urlparse

from twisted.internet import defer

from synapse.api.constants import EventTypes
from synapse.api.errors import CodeMessageException, cs_error
from synapse.api.room_versions import RoomVersions
from synapse.config.homeserver import HomeServerConfig
from synapse.config.server import DEFAULT_ROOM_VERSION
from synapse.logging.context import current_context, set_current_context
from synapse.storage.database import LoggingDatabaseConnection
from synapse.storage.engines import create_engine
from synapse.storage.prepare_database import prepare_database

# set this to True to run the tests against postgres instead of sqlite.
#
# When running under postgres, we first create a base database with the name
# POSTGRES_BASE_DB and update it to the current schema. Then, for each test case, we
# create another unique database, using the base database as a template.
USE_POSTGRES_FOR_TESTS = os.environ.get("SYNAPSE_POSTGRES", False)
LEAVE_DB = os.environ.get("SYNAPSE_LEAVE_DB", False)
POSTGRES_USER = os.environ.get("SYNAPSE_POSTGRES_USER", None)
POSTGRES_HOST = os.environ.get("SYNAPSE_POSTGRES_HOST", None)
POSTGRES_PASSWORD = os.environ.get("SYNAPSE_POSTGRES_PASSWORD", None)
POSTGRES_BASE_DB = "_synapse_unit_tests_base_%s" % (os.getpid(),)

# the dbname we will connect to in order to create the base database.
POSTGRES_DBNAME_FOR_INITIAL_CREATE = "postgres"


def setupdb():
    # If we're using PostgreSQL, set up the db once
    if USE_POSTGRES_FOR_TESTS:
        # create a PostgresEngine
        db_engine = create_engine({"name": "psycopg2", "args": {}})

        # connect to postgres to create the base database.
        db_conn = db_engine.module.connect(
            user=POSTGRES_USER,
            host=POSTGRES_HOST,
            password=POSTGRES_PASSWORD,
            dbname=POSTGRES_DBNAME_FOR_INITIAL_CREATE,
        )
        db_conn.autocommit = True
        cur = db_conn.cursor()
        cur.execute("DROP DATABASE IF EXISTS %s;" % (POSTGRES_BASE_DB,))
        cur.execute(
            "CREATE DATABASE %s ENCODING 'UTF8' LC_COLLATE='C' LC_CTYPE='C' "
            "template=template0;" % (POSTGRES_BASE_DB,)
        )
        cur.close()
        db_conn.close()

        # Set up in the db
        db_conn = db_engine.module.connect(
            database=POSTGRES_BASE_DB,
            user=POSTGRES_USER,
            host=POSTGRES_HOST,
            password=POSTGRES_PASSWORD,
        )
        db_conn = LoggingDatabaseConnection(db_conn, db_engine, "tests")
        prepare_database(db_conn, db_engine, None)
        db_conn.close()

        def _cleanup():
            db_conn = db_engine.module.connect(
                user=POSTGRES_USER,
                host=POSTGRES_HOST,
                password=POSTGRES_PASSWORD,
                dbname=POSTGRES_DBNAME_FOR_INITIAL_CREATE,
            )
            db_conn.autocommit = True
            cur = db_conn.cursor()
            cur.execute("DROP DATABASE IF EXISTS %s;" % (POSTGRES_BASE_DB,))
            cur.close()
            db_conn.close()

        atexit.register(_cleanup)


def default_config(name, parse=False):
    """
    Create a reasonable test config.
    """
    config_dict = {
        "server_name": name,
        "send_federation": False,
        "media_store_path": "media",
        # the test signing key is just an arbitrary ed25519 key to keep the config
        # parser happy
        "signing_key": "ed25519 a_lPym qvioDNmfExFBRPgdTU+wtFYKq4JfwFRv7sYVgWvmgJg",
        "event_cache_size": 1,
        "enable_registration": True,
        "enable_registration_captcha": False,
        "macaroon_secret_key": "not even a little secret",
        "password_providers": [],
        "worker_replication_url": "",
        "worker_app": None,
        "block_non_admin_invites": False,
        "federation_domain_whitelist": None,
        "filter_timeline_limit": 5000,
        "user_directory_search_all_users": False,
        "user_consent_server_notice_content": None,
        "block_events_without_consent_error": None,
        "user_consent_at_registration": False,
        "user_consent_policy_name": "Privacy Policy",
        "media_storage_providers": [],
        "autocreate_auto_join_rooms": True,
        "auto_join_rooms": [],
        "limit_usage_by_mau": False,
        "hs_disabled": False,
        "hs_disabled_message": "",
        "max_mau_value": 50,
        "mau_trial_days": 0,
        "mau_stats_only": False,
        "mau_limits_reserved_threepids": [],
        "admin_contact": None,
        "rc_message": {"per_second": 10000, "burst_count": 10000},
        "rc_registration": {"per_second": 10000, "burst_count": 10000},
        "rc_login": {
            "address": {"per_second": 10000, "burst_count": 10000},
            "account": {"per_second": 10000, "burst_count": 10000},
            "failed_attempts": {"per_second": 10000, "burst_count": 10000},
        },
        "rc_joins": {
            "local": {"per_second": 10000, "burst_count": 10000},
            "remote": {"per_second": 10000, "burst_count": 10000},
        },
        "rc_invites": {
            "per_room": {"per_second": 10000, "burst_count": 10000},
            "per_user": {"per_second": 10000, "burst_count": 10000},
        },
        "rc_3pid_validation": {"per_second": 10000, "burst_count": 10000},
        "saml2_enabled": False,
        "public_baseurl": None,
        "default_identity_server": None,
        "key_refresh_interval": 24 * 60 * 60 * 1000,
        "old_signing_keys": {},
        "tls_fingerprints": [],
        "use_frozen_dicts": False,
        # We need a sane default_room_version, otherwise attempts to create
        # rooms will fail.
        "default_room_version": DEFAULT_ROOM_VERSION,
        # disable user directory updates, because they get done in the
        # background, which upsets the test runner.
        "update_user_directory": False,
        "caches": {"global_factor": 1},
        "listeners": [{"port": 0, "type": "http"}],
    }

    if parse:
        config = HomeServerConfig()
        config.parse_config_dict(config_dict, "", "")
        return config

    return config_dict


def mock_getRawHeaders(headers=None):
    headers = headers if headers is not None else {}

    def getRawHeaders(name, default=None):
        return headers.get(name, default)

    return getRawHeaders


# This is a mock /resource/ not an entire server
class MockHttpResource:
    def __init__(self, prefix=""):
        self.callbacks = []  # 3-tuple of method/pattern/function
        self.prefix = prefix

    def trigger_get(self, path):
        return self.trigger(b"GET", path, None)

    @patch("twisted.web.http.Request")
    @defer.inlineCallbacks
    def trigger(
        self, http_method, path, content, mock_request, federation_auth_origin=None
    ):
        """Fire an HTTP event.

        Args:
            http_method : The HTTP method
            path : The HTTP path
            content : The HTTP body
            mock_request : Mocked request to pass to the event so it can get
                           content.
            federation_auth_origin (bytes|None): domain to authenticate as, for federation
        Returns:
            A tuple of (code, response)
        Raises:
            KeyError If no event is found which will handle the path.
        """
        path = self.prefix + path

        # annoyingly we return a twisted http request which has chained calls
        # to get at the http content, hence mock it here.
        mock_content = Mock()
        config = {"read.return_value": content}
        mock_content.configure_mock(**config)
        mock_request.content = mock_content

        mock_request.method = http_method.encode("ascii")
        mock_request.uri = path.encode("ascii")

        mock_request.getClientIP.return_value = "-"

        headers = {}
        if federation_auth_origin is not None:
            headers[b"Authorization"] = [
                b"X-Matrix origin=%s,key=,sig=" % (federation_auth_origin,)
            ]
        mock_request.requestHeaders.getRawHeaders = mock_getRawHeaders(headers)

        # return the right path if the event requires it
        mock_request.path = path

        # add in query params to the right place
        try:
            mock_request.args = urlparse.parse_qs(path.split("?")[1])
            mock_request.path = path.split("?")[0]
            path = mock_request.path
        except Exception:
            pass

        if isinstance(path, bytes):
            path = path.decode("utf8")

        for (method, pattern, func) in self.callbacks:
            if http_method != method:
                continue

            matcher = pattern.match(path)
            if matcher:
                try:
                    args = [urlparse.unquote(u) for u in matcher.groups()]

                    (code, response) = yield defer.ensureDeferred(
                        func(mock_request, *args)
                    )
                    return code, response
                except CodeMessageException as e:
                    return e.code, cs_error(e.msg, code=e.errcode)

        raise KeyError("No event can handle %s" % path)

    def register_paths(self, method, path_patterns, callback, servlet_name):
        for path_pattern in path_patterns:
            self.callbacks.append((method, path_pattern, callback))


class MockKey:
    alg = "mock_alg"
    version = "mock_version"
    signature = b"\x9a\x87$"

    @property
    def verify_key(self):
        return self

    def sign(self, message):
        return self

    def verify(self, message, sig):
        assert sig == b"\x9a\x87$"

    def encode(self):
        return b"<fake_encoded_key>"


class MockClock:
    now = 1000

    def __init__(self):
        # list of lists of [absolute_time, callback, expired] in no particular
        # order
        self.timers = []
        self.loopers = []

    def time(self):
        return self.now

    def time_msec(self):
        return self.time() * 1000

    def call_later(self, delay, callback, *args, **kwargs):
        ctx = current_context()

        def wrapped_callback():
            set_current_context(ctx)
            callback(*args, **kwargs)

        t = [self.now + delay, wrapped_callback, False]
        self.timers.append(t)

        return t

    def looping_call(self, function, interval, *args, **kwargs):
        self.loopers.append([function, interval / 1000.0, self.now, args, kwargs])

    def cancel_call_later(self, timer, ignore_errs=False):
        if timer[2]:
            if not ignore_errs:
                raise Exception("Cannot cancel an expired timer")

        timer[2] = True
        self.timers = [t for t in self.timers if t != timer]

    # For unit testing
    def advance_time(self, secs):
        self.now += secs

        timers = self.timers
        self.timers = []

        for t in timers:
            time, callback, expired = t

            if expired:
                raise Exception("Timer already expired")

            if self.now >= time:
                t[2] = True
                callback()
            else:
                self.timers.append(t)

        for looped in self.loopers:
            func, interval, last, args, kwargs = looped
            if last + interval < self.now:
                func(*args, **kwargs)
                looped[2] = self.now

    def advance_time_msec(self, ms):
        self.advance_time(ms / 1000.0)

    def time_bound_deferred(self, d, *args, **kwargs):
        # We don't bother timing things out for now.
        return d


async def create_room(hs, room_id: str, creator_id: str):
    """Creates and persist a creation event for the given room"""

    persistence_store = hs.get_storage().persistence
    store = hs.get_datastore()
    event_builder_factory = hs.get_event_builder_factory()
    event_creation_handler = hs.get_event_creation_handler()

    await store.store_room(
        room_id=room_id,
        room_creator_user_id=creator_id,
        is_public=False,
        room_version=RoomVersions.V1,
    )

    builder = event_builder_factory.for_room_version(
        RoomVersions.V1,
        {
            "type": EventTypes.Create,
            "state_key": "",
            "sender": creator_id,
            "room_id": room_id,
            "content": {},
        },
    )

    event, context = await event_creation_handler.create_new_client_event(builder)

    await persistence_store.persist_event(event, context)
