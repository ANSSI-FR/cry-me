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
import json
from contextlib import contextmanager
from typing import Generator

from twisted.enterprise.adbapi import ConnectionPool
from twisted.internet.defer import ensureDeferred
from twisted.test.proto_helpers import MemoryReactor

from synapse.api.room_versions import EventFormatVersions, RoomVersions
from synapse.logging.context import LoggingContext
from synapse.rest import admin
from synapse.rest.client import login, room
from synapse.server import HomeServer
from synapse.storage.databases.main.events_worker import (
    EVENT_QUEUE_THREADS,
    EventsWorkerStore,
)
from synapse.storage.types import Connection
from synapse.util import Clock
from synapse.util.async_helpers import yieldable_gather_results

from tests import unittest


class HaveSeenEventsTestCase(unittest.HomeserverTestCase):
    def prepare(self, reactor, clock, hs):
        self.store: EventsWorkerStore = hs.get_datastore()

        # insert some test data
        for rid in ("room1", "room2"):
            self.get_success(
                self.store.db_pool.simple_insert(
                    "rooms",
                    {"room_id": rid, "room_version": 4},
                )
            )

        for idx, (rid, eid) in enumerate(
            (
                ("room1", "event10"),
                ("room1", "event11"),
                ("room1", "event12"),
                ("room2", "event20"),
            )
        ):
            self.get_success(
                self.store.db_pool.simple_insert(
                    "events",
                    {
                        "event_id": eid,
                        "room_id": rid,
                        "topological_ordering": idx,
                        "stream_ordering": idx,
                        "type": "test",
                        "processed": True,
                        "outlier": False,
                    },
                )
            )
            self.get_success(
                self.store.db_pool.simple_insert(
                    "event_json",
                    {
                        "event_id": eid,
                        "room_id": rid,
                        "json": json.dumps({"type": "test", "room_id": rid}),
                        "internal_metadata": "{}",
                        "format_version": 3,
                    },
                )
            )

    def test_simple(self):
        with LoggingContext(name="test") as ctx:
            res = self.get_success(
                self.store.have_seen_events("room1", ["event10", "event19"])
            )
            self.assertEquals(res, {"event10"})

            # that should result in a single db query
            self.assertEquals(ctx.get_resource_usage().db_txn_count, 1)

        # a second lookup of the same events should cause no queries
        with LoggingContext(name="test") as ctx:
            res = self.get_success(
                self.store.have_seen_events("room1", ["event10", "event19"])
            )
            self.assertEquals(res, {"event10"})
            self.assertEquals(ctx.get_resource_usage().db_txn_count, 0)

    def test_query_via_event_cache(self):
        # fetch an event into the event cache
        self.get_success(self.store.get_event("event10"))

        # looking it up should now cause no db hits
        with LoggingContext(name="test") as ctx:
            res = self.get_success(self.store.have_seen_events("room1", ["event10"]))
            self.assertEquals(res, {"event10"})
            self.assertEquals(ctx.get_resource_usage().db_txn_count, 0)


class EventCacheTestCase(unittest.HomeserverTestCase):
    """Test that the various layers of event cache works."""

    servlets = [
        admin.register_servlets,
        room.register_servlets,
        login.register_servlets,
    ]

    def prepare(self, reactor, clock, hs):
        self.store: EventsWorkerStore = hs.get_datastore()

        self.user = self.register_user("user", "pass")
        self.token = self.login(self.user, "pass")

        self.room = self.helper.create_room_as(self.user, tok=self.token)

        res = self.helper.send(self.room, tok=self.token)
        self.event_id = res["event_id"]

        # Reset the event cache so the tests start with it empty
        self.store._get_event_cache.clear()

    def test_simple(self):
        """Test that we cache events that we pull from the DB."""

        with LoggingContext("test") as ctx:
            self.get_success(self.store.get_event(self.event_id))

            # We should have fetched the event from the DB
            self.assertEqual(ctx.get_resource_usage().evt_db_fetch_count, 1)

    def test_dedupe(self):
        """Test that if we request the same event multiple times we only pull it
        out once.
        """

        with LoggingContext("test") as ctx:
            d = yieldable_gather_results(
                self.store.get_event, [self.event_id, self.event_id]
            )
            self.get_success(d)

            # We should have fetched the event from the DB
            self.assertEqual(ctx.get_resource_usage().evt_db_fetch_count, 1)


class DatabaseOutageTestCase(unittest.HomeserverTestCase):
    """Test event fetching during a database outage."""

    def prepare(self, reactor: MemoryReactor, clock: Clock, hs: HomeServer):
        self.store: EventsWorkerStore = hs.get_datastore()

        self.room_id = f"!room:{hs.hostname}"
        self.event_ids = [f"event{i}" for i in range(20)]

        self._populate_events()

    def _populate_events(self) -> None:
        """Ensure that there are test events in the database.

        When testing with the in-memory SQLite database, all the events are lost during
        the simulated outage.

        To ensure consistency between `room_id`s and `event_id`s before and after the
        outage, rows are built and inserted manually.

        Upserts are used to handle the non-SQLite case where events are not lost.
        """
        self.get_success(
            self.store.db_pool.simple_upsert(
                "rooms",
                {"room_id": self.room_id},
                {"room_version": RoomVersions.V4.identifier},
            )
        )

        self.event_ids = [f"event{i}" for i in range(20)]
        for idx, event_id in enumerate(self.event_ids):
            self.get_success(
                self.store.db_pool.simple_upsert(
                    "events",
                    {"event_id": event_id},
                    {
                        "event_id": event_id,
                        "room_id": self.room_id,
                        "topological_ordering": idx,
                        "stream_ordering": idx,
                        "type": "test",
                        "processed": True,
                        "outlier": False,
                    },
                )
            )
            self.get_success(
                self.store.db_pool.simple_upsert(
                    "event_json",
                    {"event_id": event_id},
                    {
                        "room_id": self.room_id,
                        "json": json.dumps({"type": "test", "room_id": self.room_id}),
                        "internal_metadata": "{}",
                        "format_version": EventFormatVersions.V3,
                    },
                )
            )

    @contextmanager
    def _outage(self) -> Generator[None, None, None]:
        """Simulate a database outage.

        Returns:
            A context manager. While the context is active, any attempts to connect to
            the database will fail.
        """
        connection_pool = self.store.db_pool._db_pool

        # Close all connections and shut down the database `ThreadPool`.
        connection_pool.close()

        # Restart the database `ThreadPool`.
        connection_pool.start()

        original_connection_factory = connection_pool.connectionFactory

        def connection_factory(_pool: ConnectionPool) -> Connection:
            raise Exception("Could not connect to the database.")

        connection_pool.connectionFactory = connection_factory  # type: ignore[assignment]
        try:
            yield
        finally:
            connection_pool.connectionFactory = original_connection_factory

            # If the in-memory SQLite database is being used, all the events are gone.
            # Restore the test data.
            self._populate_events()

    def test_failure(self) -> None:
        """Test that event fetches do not get stuck during a database outage."""
        with self._outage():
            failure = self.get_failure(
                self.store.get_event(self.event_ids[0]), Exception
            )
            self.assertEqual(str(failure.value), "Could not connect to the database.")

    def test_recovery(self) -> None:
        """Test that event fetchers recover after a database outage."""
        with self._outage():
            # Kick off a bunch of event fetches but do not pump the reactor
            event_deferreds = []
            for event_id in self.event_ids:
                event_deferreds.append(ensureDeferred(self.store.get_event(event_id)))

            # We should have maxed out on event fetcher threads
            self.assertEqual(self.store._event_fetch_ongoing, EVENT_QUEUE_THREADS)

            # All the event fetchers will fail
            self.pump()
            self.assertEqual(self.store._event_fetch_ongoing, 0)

            for event_deferred in event_deferreds:
                failure = self.get_failure(event_deferred, Exception)
                self.assertEqual(
                    str(failure.value), "Could not connect to the database."
                )

        # This next event fetch should succeed
        self.get_success(self.store.get_event(self.event_ids[0]))
