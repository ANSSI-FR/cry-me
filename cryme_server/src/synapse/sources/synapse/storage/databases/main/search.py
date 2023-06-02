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
# Copyright 2015, 2016 OpenMarket Ltd
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

import logging
import re
from typing import TYPE_CHECKING, Collection, Iterable, List, Optional, Set

import attr

from synapse.api.errors import SynapseError
from synapse.events import EventBase
from synapse.storage._base import SQLBaseStore, db_to_json, make_in_list_sql_clause
from synapse.storage.database import (
    DatabasePool,
    LoggingDatabaseConnection,
    LoggingTransaction,
)
from synapse.storage.databases.main.events_worker import EventRedactBehaviour
from synapse.storage.engines import PostgresEngine, Sqlite3Engine

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


@attr.s(slots=True, frozen=True, auto_attribs=True)
class SearchEntry:
    key: str
    value: str
    event_id: str
    room_id: str
    stream_ordering: Optional[int]
    origin_server_ts: int


def _clean_value_for_search(value: str) -> str:
    """
    Replaces any null code points in the string with spaces as
    Postgres and SQLite do not like the insertion of strings with
    null code points into the full-text search tables.
    """
    return value.replace("\u0000", " ")


class SearchWorkerStore(SQLBaseStore):
    def store_search_entries_txn(
        self, txn: LoggingTransaction, entries: Iterable[SearchEntry]
    ) -> None:
        """Add entries to the search table

        Args:
            txn:
            entries: entries to be added to the table
        """
        if not self.hs.config.server.enable_search:
            return
        if isinstance(self.database_engine, PostgresEngine):
            sql = (
                "INSERT INTO event_search"
                " (event_id, room_id, key, vector, stream_ordering, origin_server_ts)"
                " VALUES (?,?,?,to_tsvector('english', ?),?,?)"
            )

            args = (
                (
                    entry.event_id,
                    entry.room_id,
                    entry.key,
                    _clean_value_for_search(entry.value),
                    entry.stream_ordering,
                    entry.origin_server_ts,
                )
                for entry in entries
            )

            txn.execute_batch(sql, args)

        elif isinstance(self.database_engine, Sqlite3Engine):
            sql = (
                "INSERT INTO event_search (event_id, room_id, key, value)"
                " VALUES (?,?,?,?)"
            )
            args = (
                (
                    entry.event_id,
                    entry.room_id,
                    entry.key,
                    _clean_value_for_search(entry.value),
                )
                for entry in entries
            )
            txn.execute_batch(sql, args)

        else:
            # This should be unreachable.
            raise Exception("Unrecognized database engine")


class SearchBackgroundUpdateStore(SearchWorkerStore):

    EVENT_SEARCH_UPDATE_NAME = "event_search"
    EVENT_SEARCH_ORDER_UPDATE_NAME = "event_search_order"
    EVENT_SEARCH_USE_GIST_POSTGRES_NAME = "event_search_postgres_gist"
    EVENT_SEARCH_USE_GIN_POSTGRES_NAME = "event_search_postgres_gin"

    def __init__(
        self,
        database: DatabasePool,
        db_conn: LoggingDatabaseConnection,
        hs: "HomeServer",
    ):
        super().__init__(database, db_conn, hs)

        if not hs.config.server.enable_search:
            return

        self.db_pool.updates.register_background_update_handler(
            self.EVENT_SEARCH_UPDATE_NAME, self._background_reindex_search
        )
        self.db_pool.updates.register_background_update_handler(
            self.EVENT_SEARCH_ORDER_UPDATE_NAME, self._background_reindex_search_order
        )

        # we used to have a background update to turn the GIN index into a
        # GIST one; we no longer do that (obviously) because we actually want
        # a GIN index. However, it's possible that some people might still have
        # the background update queued, so we register a handler to clear the
        # background update.
        self.db_pool.updates.register_noop_background_update(
            self.EVENT_SEARCH_USE_GIST_POSTGRES_NAME
        )

        self.db_pool.updates.register_background_update_handler(
            self.EVENT_SEARCH_USE_GIN_POSTGRES_NAME, self._background_reindex_gin_search
        )

    async def _background_reindex_search(self, progress, batch_size):
        # we work through the events table from highest stream id to lowest
        target_min_stream_id = progress["target_min_stream_id_inclusive"]
        max_stream_id = progress["max_stream_id_exclusive"]
        rows_inserted = progress.get("rows_inserted", 0)

        TYPES = ["m.room.name", "m.room.message", "m.room.topic"]

        def reindex_search_txn(txn):
            sql = (
                "SELECT stream_ordering, event_id, room_id, type, json, "
                " origin_server_ts FROM events"
                " JOIN event_json USING (room_id, event_id)"
                " WHERE ? <= stream_ordering AND stream_ordering < ?"
                " AND (%s)"
                " ORDER BY stream_ordering DESC"
                " LIMIT ?"
            ) % (" OR ".join("type = '%s'" % (t,) for t in TYPES),)

            txn.execute(sql, (target_min_stream_id, max_stream_id, batch_size))

            # we could stream straight from the results into
            # store_search_entries_txn with a generator function, but that
            # would mean having two cursors open on the database at once.
            # Instead we just build a list of results.
            rows = self.db_pool.cursor_to_dict(txn)
            if not rows:
                return 0

            min_stream_id = rows[-1]["stream_ordering"]

            event_search_rows = []
            for row in rows:
                try:
                    event_id = row["event_id"]
                    room_id = row["room_id"]
                    etype = row["type"]
                    stream_ordering = row["stream_ordering"]
                    origin_server_ts = row["origin_server_ts"]
                    try:
                        event_json = db_to_json(row["json"])
                        content = event_json["content"]
                    except Exception:
                        continue

                    if etype == "m.room.message":
                        key = "content.body"
                        value = content["body"]
                    elif etype == "m.room.topic":
                        key = "content.topic"
                        value = content["topic"]
                    elif etype == "m.room.name":
                        key = "content.name"
                        value = content["name"]
                    else:
                        raise Exception("unexpected event type %s" % etype)
                except (KeyError, AttributeError):
                    # If the event is missing a necessary field then
                    # skip over it.
                    continue

                if not isinstance(value, str):
                    # If the event body, name or topic isn't a string
                    # then skip over it
                    continue

                event_search_rows.append(
                    SearchEntry(
                        key=key,
                        value=value,
                        event_id=event_id,
                        room_id=room_id,
                        stream_ordering=stream_ordering,
                        origin_server_ts=origin_server_ts,
                    )
                )

            self.store_search_entries_txn(txn, event_search_rows)

            progress = {
                "target_min_stream_id_inclusive": target_min_stream_id,
                "max_stream_id_exclusive": min_stream_id,
                "rows_inserted": rows_inserted + len(event_search_rows),
            }

            self.db_pool.updates._background_update_progress_txn(
                txn, self.EVENT_SEARCH_UPDATE_NAME, progress
            )

            return len(event_search_rows)

        result = await self.db_pool.runInteraction(
            self.EVENT_SEARCH_UPDATE_NAME, reindex_search_txn
        )

        if not result:
            await self.db_pool.updates._end_background_update(
                self.EVENT_SEARCH_UPDATE_NAME
            )

        return result

    async def _background_reindex_gin_search(self, progress, batch_size):
        """This handles old synapses which used GIST indexes, if any;
        converting them back to be GIN as per the actual schema.
        """

        def create_index(conn):
            conn.rollback()

            # we have to set autocommit, because postgres refuses to
            # CREATE INDEX CONCURRENTLY without it.
            conn.set_session(autocommit=True)

            try:
                c = conn.cursor()

                # if we skipped the conversion to GIST, we may already/still
                # have an event_search_fts_idx; unfortunately postgres 9.4
                # doesn't support CREATE INDEX IF EXISTS so we just catch the
                # exception and ignore it.
                import psycopg2

                try:
                    c.execute(
                        "CREATE INDEX CONCURRENTLY event_search_fts_idx"
                        " ON event_search USING GIN (vector)"
                    )
                except psycopg2.ProgrammingError as e:
                    logger.warning(
                        "Ignoring error %r when trying to switch from GIST to GIN", e
                    )

                # we should now be able to delete the GIST index.
                c.execute("DROP INDEX IF EXISTS event_search_fts_idx_gist")
            finally:
                conn.set_session(autocommit=False)

        if isinstance(self.database_engine, PostgresEngine):
            await self.db_pool.runWithConnection(create_index)

        await self.db_pool.updates._end_background_update(
            self.EVENT_SEARCH_USE_GIN_POSTGRES_NAME
        )
        return 1

    async def _background_reindex_search_order(self, progress, batch_size):
        target_min_stream_id = progress["target_min_stream_id_inclusive"]
        max_stream_id = progress["max_stream_id_exclusive"]
        rows_inserted = progress.get("rows_inserted", 0)
        have_added_index = progress["have_added_indexes"]

        if not have_added_index:

            def create_index(conn):
                conn.rollback()
                conn.set_session(autocommit=True)
                c = conn.cursor()

                # We create with NULLS FIRST so that when we search *backwards*
                # we get the ones with non null origin_server_ts *first*
                c.execute(
                    "CREATE INDEX CONCURRENTLY event_search_room_order ON event_search("
                    "room_id, origin_server_ts NULLS FIRST, stream_ordering NULLS FIRST)"
                )
                c.execute(
                    "CREATE INDEX CONCURRENTLY event_search_order ON event_search("
                    "origin_server_ts NULLS FIRST, stream_ordering NULLS FIRST)"
                )
                conn.set_session(autocommit=False)

            await self.db_pool.runWithConnection(create_index)

            pg = dict(progress)
            pg["have_added_indexes"] = True

            await self.db_pool.runInteraction(
                self.EVENT_SEARCH_ORDER_UPDATE_NAME,
                self.db_pool.updates._background_update_progress_txn,
                self.EVENT_SEARCH_ORDER_UPDATE_NAME,
                pg,
            )

        def reindex_search_txn(txn):
            sql = (
                "UPDATE event_search AS es SET stream_ordering = e.stream_ordering,"
                " origin_server_ts = e.origin_server_ts"
                " FROM events AS e"
                " WHERE e.event_id = es.event_id"
                " AND ? <= e.stream_ordering AND e.stream_ordering < ?"
                " RETURNING es.stream_ordering"
            )

            min_stream_id = max_stream_id - batch_size
            txn.execute(sql, (min_stream_id, max_stream_id))
            rows = txn.fetchall()

            if min_stream_id < target_min_stream_id:
                # We've recached the end.
                return len(rows), False

            progress = {
                "target_min_stream_id_inclusive": target_min_stream_id,
                "max_stream_id_exclusive": min_stream_id,
                "rows_inserted": rows_inserted + len(rows),
                "have_added_indexes": True,
            }

            self.db_pool.updates._background_update_progress_txn(
                txn, self.EVENT_SEARCH_ORDER_UPDATE_NAME, progress
            )

            return len(rows), True

        num_rows, finished = await self.db_pool.runInteraction(
            self.EVENT_SEARCH_ORDER_UPDATE_NAME, reindex_search_txn
        )

        if not finished:
            await self.db_pool.updates._end_background_update(
                self.EVENT_SEARCH_ORDER_UPDATE_NAME
            )

        return num_rows


class SearchStore(SearchBackgroundUpdateStore):
    def __init__(
        self,
        database: DatabasePool,
        db_conn: LoggingDatabaseConnection,
        hs: "HomeServer",
    ):
        super().__init__(database, db_conn, hs)

    async def search_msgs(self, room_ids, search_term, keys):
        """Performs a full text search over events with given keys.

        Args:
            room_ids (list): List of room ids to search in
            search_term (str): Search term to search for
            keys (list): List of keys to search in, currently supports
                "content.body", "content.name", "content.topic"

        Returns:
            list of dicts
        """
        clauses = []

        search_query = _parse_query(self.database_engine, search_term)

        args = []

        # Make sure we don't explode because the person is in too many rooms.
        # We filter the results below regardless.
        if len(room_ids) < 500:
            clause, args = make_in_list_sql_clause(
                self.database_engine, "room_id", room_ids
            )
            clauses = [clause]

        local_clauses = []
        for key in keys:
            local_clauses.append("key = ?")
            args.append(key)

        clauses.append("(%s)" % (" OR ".join(local_clauses),))

        count_args = args
        count_clauses = clauses

        if isinstance(self.database_engine, PostgresEngine):
            sql = (
                "SELECT ts_rank_cd(vector, to_tsquery('english', ?)) AS rank,"
                " room_id, event_id"
                " FROM event_search"
                " WHERE vector @@ to_tsquery('english', ?)"
            )
            args = [search_query, search_query] + args

            count_sql = (
                "SELECT room_id, count(*) as count FROM event_search"
                " WHERE vector @@ to_tsquery('english', ?)"
            )
            count_args = [search_query] + count_args
        elif isinstance(self.database_engine, Sqlite3Engine):
            sql = (
                "SELECT rank(matchinfo(event_search)) as rank, room_id, event_id"
                " FROM event_search"
                " WHERE value MATCH ?"
            )
            args = [search_query] + args

            count_sql = (
                "SELECT room_id, count(*) as count FROM event_search"
                " WHERE value MATCH ?"
            )
            count_args = [search_term] + count_args
        else:
            # This should be unreachable.
            raise Exception("Unrecognized database engine")

        for clause in clauses:
            sql += " AND " + clause

        for clause in count_clauses:
            count_sql += " AND " + clause

        # We add an arbitrary limit here to ensure we don't try to pull the
        # entire table from the database.
        sql += " ORDER BY rank DESC LIMIT 500"

        results = await self.db_pool.execute(
            "search_msgs", self.db_pool.cursor_to_dict, sql, *args
        )

        results = list(filter(lambda row: row["room_id"] in room_ids, results))

        # We set redact_behaviour to BLOCK here to prevent redacted events being returned in
        # search results (which is a data leak)
        events = await self.get_events_as_list(
            [r["event_id"] for r in results],
            redact_behaviour=EventRedactBehaviour.BLOCK,
        )

        event_map = {ev.event_id: ev for ev in events}

        highlights = None
        if isinstance(self.database_engine, PostgresEngine):
            highlights = await self._find_highlights_in_postgres(search_query, events)

        count_sql += " GROUP BY room_id"

        count_results = await self.db_pool.execute(
            "search_rooms_count", self.db_pool.cursor_to_dict, count_sql, *count_args
        )

        count = sum(row["count"] for row in count_results if row["room_id"] in room_ids)

        return {
            "results": [
                {"event": event_map[r["event_id"]], "rank": r["rank"]}
                for r in results
                if r["event_id"] in event_map
            ],
            "highlights": highlights,
            "count": count,
        }

    async def search_rooms(
        self,
        room_ids: Collection[str],
        search_term: str,
        keys: List[str],
        limit,
        pagination_token: Optional[str] = None,
    ) -> List[dict]:
        """Performs a full text search over events with given keys.

        Args:
            room_ids: The room_ids to search in
            search_term: Search term to search for
            keys: List of keys to search in, currently supports "content.body",
                "content.name", "content.topic"
            pagination_token: A pagination token previously returned

        Returns:
            Each match as a dictionary.
        """
        clauses = []

        search_query = _parse_query(self.database_engine, search_term)

        args = []

        # Make sure we don't explode because the person is in too many rooms.
        # We filter the results below regardless.
        if len(room_ids) < 500:
            clause, args = make_in_list_sql_clause(
                self.database_engine, "room_id", room_ids
            )
            clauses = [clause]

        local_clauses = []
        for key in keys:
            local_clauses.append("key = ?")
            args.append(key)

        clauses.append("(%s)" % (" OR ".join(local_clauses),))

        # take copies of the current args and clauses lists, before adding
        # pagination clauses to main query.
        count_args = list(args)
        count_clauses = list(clauses)

        if pagination_token:
            try:
                origin_server_ts, stream = pagination_token.split(",")
                origin_server_ts = int(origin_server_ts)
                stream = int(stream)
            except Exception:
                raise SynapseError(400, "Invalid pagination token")

            clauses.append(
                "(origin_server_ts < ?"
                " OR (origin_server_ts = ? AND stream_ordering < ?))"
            )
            args.extend([origin_server_ts, origin_server_ts, stream])

        if isinstance(self.database_engine, PostgresEngine):
            sql = (
                "SELECT ts_rank_cd(vector, to_tsquery('english', ?)) as rank,"
                " origin_server_ts, stream_ordering, room_id, event_id"
                " FROM event_search"
                " WHERE vector @@ to_tsquery('english', ?) AND "
            )
            args = [search_query, search_query] + args

            count_sql = (
                "SELECT room_id, count(*) as count FROM event_search"
                " WHERE vector @@ to_tsquery('english', ?) AND "
            )
            count_args = [search_query] + count_args
        elif isinstance(self.database_engine, Sqlite3Engine):
            # We use CROSS JOIN here to ensure we use the right indexes.
            # https://sqlite.org/optoverview.html#crossjoin
            #
            # We want to use the full text search index on event_search to
            # extract all possible matches first, then lookup those matches
            # in the events table to get the topological ordering. We need
            # to use the indexes in this order because sqlite refuses to
            # MATCH unless it uses the full text search index
            sql = (
                "SELECT rank(matchinfo) as rank, room_id, event_id,"
                " origin_server_ts, stream_ordering"
                " FROM (SELECT key, event_id, matchinfo(event_search) as matchinfo"
                " FROM event_search"
                " WHERE value MATCH ?"
                " )"
                " CROSS JOIN events USING (event_id)"
                " WHERE "
            )
            args = [search_query] + args

            count_sql = (
                "SELECT room_id, count(*) as count FROM event_search"
                " WHERE value MATCH ? AND "
            )
            count_args = [search_term] + count_args
        else:
            # This should be unreachable.
            raise Exception("Unrecognized database engine")

        sql += " AND ".join(clauses)
        count_sql += " AND ".join(count_clauses)

        # We add an arbitrary limit here to ensure we don't try to pull the
        # entire table from the database.
        if isinstance(self.database_engine, PostgresEngine):
            sql += (
                " ORDER BY origin_server_ts DESC NULLS LAST,"
                " stream_ordering DESC NULLS LAST LIMIT ?"
            )
        elif isinstance(self.database_engine, Sqlite3Engine):
            sql += " ORDER BY origin_server_ts DESC, stream_ordering DESC LIMIT ?"
        else:
            raise Exception("Unrecognized database engine")

        args.append(limit)

        results = await self.db_pool.execute(
            "search_rooms", self.db_pool.cursor_to_dict, sql, *args
        )

        results = list(filter(lambda row: row["room_id"] in room_ids, results))

        # We set redact_behaviour to BLOCK here to prevent redacted events being returned in
        # search results (which is a data leak)
        events = await self.get_events_as_list(
            [r["event_id"] for r in results],
            redact_behaviour=EventRedactBehaviour.BLOCK,
        )

        event_map = {ev.event_id: ev for ev in events}

        highlights = None
        if isinstance(self.database_engine, PostgresEngine):
            highlights = await self._find_highlights_in_postgres(search_query, events)

        count_sql += " GROUP BY room_id"

        count_results = await self.db_pool.execute(
            "search_rooms_count", self.db_pool.cursor_to_dict, count_sql, *count_args
        )

        count = sum(row["count"] for row in count_results if row["room_id"] in room_ids)

        return {
            "results": [
                {
                    "event": event_map[r["event_id"]],
                    "rank": r["rank"],
                    "pagination_token": "%s,%s"
                    % (r["origin_server_ts"], r["stream_ordering"]),
                }
                for r in results
                if r["event_id"] in event_map
            ],
            "highlights": highlights,
            "count": count,
        }

    async def _find_highlights_in_postgres(
        self, search_query: str, events: List[EventBase]
    ) -> Set[str]:
        """Given a list of events and a search term, return a list of words
        that match from the content of the event.

        This is used to give a list of words that clients can match against to
        highlight the matching parts.

        Args:
            search_query
            events: A list of events

        Returns:
            A set of strings.
        """

        def f(txn):
            highlight_words = set()
            for event in events:
                # As a hack we simply join values of all possible keys. This is
                # fine since we're only using them to find possible highlights.
                values = []
                for key in ("body", "name", "topic"):
                    v = event.content.get(key, None)
                    if v:
                        v = _clean_value_for_search(v)
                        values.append(v)

                if not values:
                    continue

                value = " ".join(values)

                # We need to find some values for StartSel and StopSel that
                # aren't in the value so that we can pick results out.
                start_sel = "<"
                stop_sel = ">"

                while start_sel in value:
                    start_sel += "<"
                while stop_sel in value:
                    stop_sel += ">"

                query = "SELECT ts_headline(?, to_tsquery('english', ?), %s)" % (
                    _to_postgres_options(
                        {
                            "StartSel": start_sel,
                            "StopSel": stop_sel,
                            "MaxFragments": "50",
                        }
                    )
                )
                txn.execute(query, (value, search_query))
                (headline,) = txn.fetchall()[0]

                # Now we need to pick the possible highlights out of the haedline
                # result.
                matcher_regex = "%s(.*?)%s" % (
                    re.escape(start_sel),
                    re.escape(stop_sel),
                )

                res = re.findall(matcher_regex, headline)
                highlight_words.update([r.lower() for r in res])

            return highlight_words

        return await self.db_pool.runInteraction("_find_highlights", f)


def _to_postgres_options(options_dict):
    return "'%s'" % (",".join("%s=%s" % (k, v) for k, v in options_dict.items()),)


def _parse_query(database_engine, search_term):
    """Takes a plain unicode string from the user and converts it into a form
    that can be passed to database.
    We use this so that we can add prefix matching, which isn't something
    that is supported by default.
    """

    # Pull out the individual words, discarding any non-word characters.
    results = re.findall(r"([\w\-]+)", search_term, re.UNICODE)

    if isinstance(database_engine, PostgresEngine):
        return " & ".join(result + ":*" for result in results)
    elif isinstance(database_engine, Sqlite3Engine):
        return " & ".join(result + "*" for result in results)
    else:
        # This should be unreachable.
        raise Exception("Unrecognized database engine")
