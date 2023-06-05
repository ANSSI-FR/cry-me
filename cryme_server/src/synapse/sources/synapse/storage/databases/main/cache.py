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
# Copyright 2019 The Matrix.org Foundation C.I.C.
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


import itertools
import logging
from typing import TYPE_CHECKING, Any, Iterable, List, Optional, Tuple

from synapse.api.constants import EventTypes
from synapse.replication.tcp.streams import BackfillStream, CachesStream
from synapse.replication.tcp.streams.events import (
    EventsStream,
    EventsStreamCurrentStateRow,
    EventsStreamEventRow,
)
from synapse.storage._base import SQLBaseStore
from synapse.storage.database import DatabasePool, LoggingDatabaseConnection
from synapse.storage.engines import PostgresEngine
from synapse.util.iterutils import batch_iter

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


# This is a special cache name we use to batch multiple invalidations of caches
# based on the current state when notifying workers over replication.
CURRENT_STATE_CACHE_NAME = "cs_cache_fake"


class CacheInvalidationWorkerStore(SQLBaseStore):
    def __init__(
        self,
        database: DatabasePool,
        db_conn: LoggingDatabaseConnection,
        hs: "HomeServer",
    ):
        super().__init__(database, db_conn, hs)

        self._instance_name = hs.get_instance_name()

    async def get_all_updated_caches(
        self, instance_name: str, last_id: int, current_id: int, limit: int
    ) -> Tuple[List[Tuple[int, tuple]], int, bool]:
        """Get updates for caches replication stream.

        Args:
            instance_name: The writer we want to fetch updates from. Unused
                here since there is only ever one writer.
            last_id: The token to fetch updates from. Exclusive.
            current_id: The token to fetch updates up to. Inclusive.
            limit: The requested limit for the number of rows to return. The
                function may return more or fewer rows.

        Returns:
            A tuple consisting of: the updates, a token to use to fetch
            subsequent updates, and whether we returned fewer rows than exists
            between the requested tokens due to the limit.

            The token returned can be used in a subsequent call to this
            function to get further updatees.

            The updates are a list of 2-tuples of stream ID and the row data
        """

        if last_id == current_id:
            return [], current_id, False

        def get_all_updated_caches_txn(txn):
            # We purposefully don't bound by the current token, as we want to
            # send across cache invalidations as quickly as possible. Cache
            # invalidations are idempotent, so duplicates are fine.
            sql = """
                SELECT stream_id, cache_func, keys, invalidation_ts
                FROM cache_invalidation_stream_by_instance
                WHERE stream_id > ? AND instance_name = ?
                ORDER BY stream_id ASC
                LIMIT ?
            """
            txn.execute(sql, (last_id, instance_name, limit))
            updates = [(row[0], row[1:]) for row in txn]
            limited = False
            upto_token = current_id
            if len(updates) >= limit:
                upto_token = updates[-1][0]
                limited = True

            return updates, upto_token, limited

        return await self.db_pool.runInteraction(
            "get_all_updated_caches", get_all_updated_caches_txn
        )

    def process_replication_rows(self, stream_name, instance_name, token, rows):
        if stream_name == EventsStream.NAME:
            for row in rows:
                self._process_event_stream_row(token, row)
        elif stream_name == BackfillStream.NAME:
            for row in rows:
                self._invalidate_caches_for_event(
                    -token,
                    row.event_id,
                    row.room_id,
                    row.type,
                    row.state_key,
                    row.redacts,
                    row.relates_to,
                    backfilled=True,
                )
        elif stream_name == CachesStream.NAME:
            if self._cache_id_gen:
                self._cache_id_gen.advance(instance_name, token)

            for row in rows:
                if row.cache_func == CURRENT_STATE_CACHE_NAME:
                    if row.keys is None:
                        raise Exception(
                            "Can't send an 'invalidate all' for current state cache"
                        )

                    room_id = row.keys[0]
                    members_changed = set(row.keys[1:])
                    self._invalidate_state_caches(room_id, members_changed)
                else:
                    self._attempt_to_invalidate_cache(row.cache_func, row.keys)

        super().process_replication_rows(stream_name, instance_name, token, rows)

    def _process_event_stream_row(self, token, row):
        data = row.data

        if row.type == EventsStreamEventRow.TypeId:
            self._invalidate_caches_for_event(
                token,
                data.event_id,
                data.room_id,
                data.type,
                data.state_key,
                data.redacts,
                data.relates_to,
                backfilled=False,
            )
        elif row.type == EventsStreamCurrentStateRow.TypeId:
            self._curr_state_delta_stream_cache.entity_has_changed(
                row.data.room_id, token
            )

            if data.type == EventTypes.Member:
                self.get_rooms_for_user_with_stream_ordering.invalidate(
                    (data.state_key,)
                )
        else:
            raise Exception("Unknown events stream row type %s" % (row.type,))

    def _invalidate_caches_for_event(
        self,
        stream_ordering,
        event_id,
        room_id,
        etype,
        state_key,
        redacts,
        relates_to,
        backfilled,
    ):
        self._invalidate_get_event_cache(event_id)
        self.have_seen_event.invalidate((room_id, event_id))

        self.get_latest_event_ids_in_room.invalidate((room_id,))

        self.get_unread_event_push_actions_by_room_for_user.invalidate((room_id,))

        if not backfilled:
            self._events_stream_cache.entity_has_changed(room_id, stream_ordering)

        if redacts:
            self._invalidate_get_event_cache(redacts)

        if etype == EventTypes.Member:
            self._membership_stream_cache.entity_has_changed(state_key, stream_ordering)
            self.get_invited_rooms_for_local_user.invalidate((state_key,))

        if relates_to:
            self.get_relations_for_event.invalidate((relates_to,))
            self.get_aggregation_groups_for_event.invalidate((relates_to,))
            self.get_applicable_edit.invalidate((relates_to,))

    async def invalidate_cache_and_stream(self, cache_name: str, keys: Tuple[Any, ...]):
        """Invalidates the cache and adds it to the cache stream so slaves
        will know to invalidate their caches.

        This should only be used to invalidate caches where slaves won't
        otherwise know from other replication streams that the cache should
        be invalidated.
        """
        cache_func = getattr(self, cache_name, None)
        if not cache_func:
            return

        cache_func.invalidate(keys)
        await self.db_pool.runInteraction(
            "invalidate_cache_and_stream",
            self._send_invalidation_to_replication,
            cache_func.__name__,
            keys,
        )

    def _invalidate_cache_and_stream(self, txn, cache_func, keys):
        """Invalidates the cache and adds it to the cache stream so slaves
        will know to invalidate their caches.

        This should only be used to invalidate caches where slaves won't
        otherwise know from other replication streams that the cache should
        be invalidated.
        """
        txn.call_after(cache_func.invalidate, keys)
        self._send_invalidation_to_replication(txn, cache_func.__name__, keys)

    def _invalidate_all_cache_and_stream(self, txn, cache_func):
        """Invalidates the entire cache and adds it to the cache stream so slaves
        will know to invalidate their caches.
        """

        txn.call_after(cache_func.invalidate_all)
        self._send_invalidation_to_replication(txn, cache_func.__name__, None)

    def _invalidate_state_caches_and_stream(self, txn, room_id, members_changed):
        """Special case invalidation of caches based on current state.

        We special case this so that we can batch the cache invalidations into a
        single replication poke.

        Args:
            txn
            room_id (str): Room where state changed
            members_changed (iterable[str]): The user_ids of members that have changed
        """
        txn.call_after(self._invalidate_state_caches, room_id, members_changed)

        if members_changed:
            # We need to be careful that the size of the `members_changed` list
            # isn't so large that it causes problems sending over replication, so we
            # send them in chunks.
            # Max line length is 16K, and max user ID length is 255, so 50 should
            # be safe.
            for chunk in batch_iter(members_changed, 50):
                keys = itertools.chain([room_id], chunk)
                self._send_invalidation_to_replication(
                    txn, CURRENT_STATE_CACHE_NAME, keys
                )
        else:
            # if no members changed, we still need to invalidate the other caches.
            self._send_invalidation_to_replication(
                txn, CURRENT_STATE_CACHE_NAME, [room_id]
            )

    def _send_invalidation_to_replication(
        self, txn, cache_name: str, keys: Optional[Iterable[Any]]
    ):
        """Notifies replication that given cache has been invalidated.

        Note that this does *not* invalidate the cache locally.

        Args:
            txn
            cache_name
            keys: Entry to invalidate. If None will invalidate all.
        """

        if cache_name == CURRENT_STATE_CACHE_NAME and keys is None:
            raise Exception(
                "Can't stream invalidate all with magic current state cache"
            )

        if isinstance(self.database_engine, PostgresEngine):
            # get_next() returns a context manager which is designed to wrap
            # the transaction. However, we want to only get an ID when we want
            # to use it, here, so we need to call __enter__ manually, and have
            # __exit__ called after the transaction finishes.
            stream_id = self._cache_id_gen.get_next_txn(txn)
            txn.call_after(self.hs.get_notifier().on_new_replication_data)

            if keys is not None:
                keys = list(keys)

            self.db_pool.simple_insert_txn(
                txn,
                table="cache_invalidation_stream_by_instance",
                values={
                    "stream_id": stream_id,
                    "instance_name": self._instance_name,
                    "cache_func": cache_name,
                    "keys": keys,
                    "invalidation_ts": self.clock.time_msec(),
                },
            )

    def get_cache_stream_token_for_writer(self, instance_name: str) -> int:
        if self._cache_id_gen:
            return self._cache_id_gen.get_current_token_for_writer(instance_name)
        else:
            return 0
