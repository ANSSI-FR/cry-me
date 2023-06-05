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

import logging
import threading
from typing import (
    TYPE_CHECKING,
    Any,
    Collection,
    Container,
    Dict,
    Iterable,
    List,
    NoReturn,
    Optional,
    Set,
    Tuple,
    cast,
    overload,
)

import attr
from constantly import NamedConstant, Names
from prometheus_client import Gauge
from typing_extensions import Literal

from twisted.internet import defer

from synapse.api.constants import EventTypes
from synapse.api.errors import NotFoundError, SynapseError
from synapse.api.room_versions import (
    KNOWN_ROOM_VERSIONS,
    EventFormatVersions,
    RoomVersion,
    RoomVersions,
)
from synapse.events import EventBase, make_event_from_dict
from synapse.events.snapshot import EventContext
from synapse.events.utils import prune_event
from synapse.logging.context import (
    PreserveLoggingContext,
    current_context,
    make_deferred_yieldable,
)
from synapse.metrics.background_process_metrics import (
    run_as_background_process,
    wrap_as_background_process,
)
from synapse.replication.slave.storage._slaved_id_tracker import SlavedIdTracker
from synapse.replication.tcp.streams import BackfillStream
from synapse.replication.tcp.streams.events import EventsStream
from synapse.storage._base import SQLBaseStore, db_to_json, make_in_list_sql_clause
from synapse.storage.database import (
    DatabasePool,
    LoggingDatabaseConnection,
    LoggingTransaction,
)
from synapse.storage.engines import PostgresEngine
from synapse.storage.types import Cursor
from synapse.storage.util.id_generators import (
    AbstractStreamIdTracker,
    MultiWriterIdGenerator,
    StreamIdGenerator,
)
from synapse.storage.util.sequence import build_sequence_generator
from synapse.types import JsonDict, get_domain_from_id
from synapse.util import unwrapFirstError
from synapse.util.async_helpers import ObservableDeferred
from synapse.util.caches.descriptors import cached, cachedList
from synapse.util.caches.lrucache import LruCache
from synapse.util.iterutils import batch_iter
from synapse.util.metrics import Measure

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


# These values are used in the `enqueue_event` and `_fetch_loop` methods to
# control how we batch/bulk fetch events from the database.
# The values are plucked out of thing air to make initial sync run faster
# on jki.re
# TODO: Make these configurable.
EVENT_QUEUE_THREADS = 3  # Max number of threads that will fetch events
EVENT_QUEUE_ITERATIONS = 3  # No. times we block waiting for requests for events
EVENT_QUEUE_TIMEOUT_S = 0.1  # Timeout when waiting for requests for events


event_fetch_ongoing_gauge = Gauge(
    "synapse_event_fetch_ongoing",
    "The number of event fetchers that are running",
)


@attr.s(slots=True, auto_attribs=True)
class EventCacheEntry:
    event: EventBase
    redacted_event: Optional[EventBase]


@attr.s(slots=True, frozen=True, auto_attribs=True)
class _EventRow:
    """
    An event, as pulled from the database.

    Properties:
        event_id: The event ID of the event.

        stream_ordering: stream ordering for this event

        json: json-encoded event structure

        internal_metadata: json-encoded internal metadata dict

        format_version: The format of the event. Hopefully one of EventFormatVersions.
            'None' means the event predates EventFormatVersions (so the event is format V1).

        room_version_id: The version of the room which contains the event. Hopefully
            one of RoomVersions.

           Due to historical reasons, there may be a few events in the database which
           do not have an associated room; in this case None will be returned here.

        rejected_reason: if the event was rejected, the reason why.

        redactions: a list of event-ids which (claim to) redact this event.

        outlier: True if this event is an outlier.
    """

    event_id: str
    stream_ordering: int
    json: str
    internal_metadata: str
    format_version: Optional[int]
    room_version_id: Optional[str]
    rejected_reason: Optional[str]
    redactions: List[str]
    outlier: bool


class EventRedactBehaviour(Names):
    """
    What to do when retrieving a redacted event from the database.
    """

    AS_IS = NamedConstant()
    REDACT = NamedConstant()
    BLOCK = NamedConstant()


class EventsWorkerStore(SQLBaseStore):
    # Whether to use dedicated DB threads for event fetching. This is only used
    # if there are multiple DB threads available. When used will lock the DB
    # thread for periods of time (so unit tests want to disable this when they
    # run DB transactions on the main thread). See EVENT_QUEUE_* for more
    # options controlling this.
    USE_DEDICATED_DB_THREADS_FOR_EVENT_FETCHING = True

    def __init__(
        self,
        database: DatabasePool,
        db_conn: LoggingDatabaseConnection,
        hs: "HomeServer",
    ):
        super().__init__(database, db_conn, hs)

        self._stream_id_gen: AbstractStreamIdTracker
        self._backfill_id_gen: AbstractStreamIdTracker
        if isinstance(database.engine, PostgresEngine):
            # If we're using Postgres than we can use `MultiWriterIdGenerator`
            # regardless of whether this process writes to the streams or not.
            self._stream_id_gen = MultiWriterIdGenerator(
                db_conn=db_conn,
                db=database,
                stream_name="events",
                instance_name=hs.get_instance_name(),
                tables=[("events", "instance_name", "stream_ordering")],
                sequence_name="events_stream_seq",
                writers=hs.config.worker.writers.events,
            )
            self._backfill_id_gen = MultiWriterIdGenerator(
                db_conn=db_conn,
                db=database,
                stream_name="backfill",
                instance_name=hs.get_instance_name(),
                tables=[("events", "instance_name", "stream_ordering")],
                sequence_name="events_backfill_stream_seq",
                positive=False,
                writers=hs.config.worker.writers.events,
            )
        else:
            # We shouldn't be running in worker mode with SQLite, but its useful
            # to support it for unit tests.
            #
            # If this process is the writer than we need to use
            # `StreamIdGenerator`, otherwise we use `SlavedIdTracker` which gets
            # updated over replication. (Multiple writers are not supported for
            # SQLite).
            if hs.get_instance_name() in hs.config.worker.writers.events:
                self._stream_id_gen = StreamIdGenerator(
                    db_conn,
                    "events",
                    "stream_ordering",
                )
                self._backfill_id_gen = StreamIdGenerator(
                    db_conn,
                    "events",
                    "stream_ordering",
                    step=-1,
                    extra_tables=[("ex_outlier_stream", "event_stream_ordering")],
                )
            else:
                self._stream_id_gen = SlavedIdTracker(
                    db_conn, "events", "stream_ordering"
                )
                self._backfill_id_gen = SlavedIdTracker(
                    db_conn, "events", "stream_ordering", step=-1
                )

        if hs.config.worker.run_background_tasks:
            # We periodically clean out old transaction ID mappings
            self._clock.looping_call(
                self._cleanup_old_transaction_ids,
                5 * 60 * 1000,
            )

        self._get_event_cache: LruCache[Tuple[str], EventCacheEntry] = LruCache(
            cache_name="*getEvent*",
            max_size=hs.config.caches.event_cache_size,
        )

        # Map from event ID to a deferred that will result in a map from event
        # ID to cache entry. Note that the returned dict may not have the
        # requested event in it if the event isn't in the DB.
        self._current_event_fetches: Dict[
            str, ObservableDeferred[Dict[str, EventCacheEntry]]
        ] = {}

        self._event_fetch_lock = threading.Condition()
        self._event_fetch_list: List[
            Tuple[Iterable[str], "defer.Deferred[Dict[str, _EventRow]]"]
        ] = []
        self._event_fetch_ongoing = 0
        event_fetch_ongoing_gauge.set(self._event_fetch_ongoing)

        # We define this sequence here so that it can be referenced from both
        # the DataStore and PersistEventStore.
        def get_chain_id_txn(txn: Cursor) -> int:
            txn.execute("SELECT COALESCE(max(chain_id), 0) FROM event_auth_chains")
            return cast(Tuple[int], txn.fetchone())[0]

        self.event_chain_id_gen = build_sequence_generator(
            db_conn,
            database.engine,
            get_chain_id_txn,
            "event_auth_chain_id",
            table="event_auth_chains",
            id_column="chain_id",
        )

    def process_replication_rows(
        self,
        stream_name: str,
        instance_name: str,
        token: int,
        rows: Iterable[Any],
    ) -> None:
        if stream_name == EventsStream.NAME:
            self._stream_id_gen.advance(instance_name, token)
        elif stream_name == BackfillStream.NAME:
            self._backfill_id_gen.advance(instance_name, -token)

        super().process_replication_rows(stream_name, instance_name, token, rows)

    async def get_received_ts(self, event_id: str) -> Optional[int]:
        """Get received_ts (when it was persisted) for the event.

        Raises an exception for unknown events.

        Args:
            event_id: The event ID to query.

        Returns:
            Timestamp in milliseconds, or None for events that were persisted
            before received_ts was implemented.
        """
        return await self.db_pool.simple_select_one_onecol(
            table="events",
            keyvalues={"event_id": event_id},
            retcol="received_ts",
            desc="get_received_ts",
        )

    # Inform mypy that if allow_none is False (the default) then get_event
    # always returns an EventBase.
    @overload
    async def get_event(
        self,
        event_id: str,
        redact_behaviour: EventRedactBehaviour = EventRedactBehaviour.REDACT,
        get_prev_content: bool = ...,
        allow_rejected: bool = ...,
        allow_none: Literal[False] = ...,
        check_room_id: Optional[str] = ...,
    ) -> EventBase:
        ...

    @overload
    async def get_event(
        self,
        event_id: str,
        redact_behaviour: EventRedactBehaviour = EventRedactBehaviour.REDACT,
        get_prev_content: bool = ...,
        allow_rejected: bool = ...,
        allow_none: Literal[True] = ...,
        check_room_id: Optional[str] = ...,
    ) -> Optional[EventBase]:
        ...

    async def get_event(
        self,
        event_id: str,
        redact_behaviour: EventRedactBehaviour = EventRedactBehaviour.REDACT,
        get_prev_content: bool = False,
        allow_rejected: bool = False,
        allow_none: bool = False,
        check_room_id: Optional[str] = None,
    ) -> Optional[EventBase]:
        """Get an event from the database by event_id.

        Args:
            event_id: The event_id of the event to fetch

            redact_behaviour: Determine what to do with a redacted event. Possible values:
                * AS_IS - Return the full event body with no redacted content
                * REDACT - Return the event but with a redacted body
                * DISALLOW - Do not return redacted events (behave as per allow_none
                    if the event is redacted)

            get_prev_content: If True and event is a state event,
                include the previous states content in the unsigned field.

            allow_rejected: If True, return rejected events. Otherwise,
                behave as per allow_none.

            allow_none: If True, return None if no event found, if
                False throw a NotFoundError

            check_room_id: if not None, check the room of the found event.
                If there is a mismatch, behave as per allow_none.

        Returns:
            The event, or None if the event was not found.
        """
        if not isinstance(event_id, str):
            raise TypeError("Invalid event event_id %r" % (event_id,))

        events = await self.get_events_as_list(
            [event_id],
            redact_behaviour=redact_behaviour,
            get_prev_content=get_prev_content,
            allow_rejected=allow_rejected,
        )

        event = events[0] if events else None

        if event is not None and check_room_id is not None:
            if event.room_id != check_room_id:
                event = None

        if event is None and not allow_none:
            raise NotFoundError("Could not find event %s" % (event_id,))

        return event

    async def get_events(
        self,
        event_ids: Collection[str],
        redact_behaviour: EventRedactBehaviour = EventRedactBehaviour.REDACT,
        get_prev_content: bool = False,
        allow_rejected: bool = False,
    ) -> Dict[str, EventBase]:
        """Get events from the database

        Args:
            event_ids: The event_ids of the events to fetch

            redact_behaviour: Determine what to do with a redacted event. Possible
                values:
                * AS_IS - Return the full event body with no redacted content
                * REDACT - Return the event but with a redacted body
                * DISALLOW - Do not return redacted events (omit them from the response)

            get_prev_content: If True and event is a state event,
                include the previous states content in the unsigned field.

            allow_rejected: If True, return rejected events. Otherwise,
                omits rejeted events from the response.

        Returns:
            A mapping from event_id to event.
        """
        events = await self.get_events_as_list(
            event_ids,
            redact_behaviour=redact_behaviour,
            get_prev_content=get_prev_content,
            allow_rejected=allow_rejected,
        )

        return {e.event_id: e for e in events}

    async def get_events_as_list(
        self,
        event_ids: Collection[str],
        redact_behaviour: EventRedactBehaviour = EventRedactBehaviour.REDACT,
        get_prev_content: bool = False,
        allow_rejected: bool = False,
    ) -> List[EventBase]:
        """Get events from the database and return in a list in the same order
        as given by `event_ids` arg.

        Unknown events will be omitted from the response.

        Args:
            event_ids: The event_ids of the events to fetch

            redact_behaviour: Determine what to do with a redacted event. Possible values:
                * AS_IS - Return the full event body with no redacted content
                * REDACT - Return the event but with a redacted body
                * DISALLOW - Do not return redacted events (omit them from the response)

            get_prev_content: If True and event is a state event,
                include the previous states content in the unsigned field.

            allow_rejected: If True, return rejected events. Otherwise,
                omits rejected events from the response.

        Returns:
            List of events fetched from the database. The events are in the same
            order as `event_ids` arg.

            Note that the returned list may be smaller than the list of event
            IDs if not all events could be fetched.
        """

        if not event_ids:
            return []

        # there may be duplicates so we cast the list to a set
        event_entry_map = await self._get_events_from_cache_or_db(
            set(event_ids), allow_rejected=allow_rejected
        )

        events = []
        for event_id in event_ids:
            entry = event_entry_map.get(event_id, None)
            if not entry:
                continue

            if not allow_rejected:
                assert not entry.event.rejected_reason, (
                    "rejected event returned from _get_events_from_cache_or_db despite "
                    "allow_rejected=False"
                )

            # We may not have had the original event when we received a redaction, so
            # we have to recheck auth now.

            if not allow_rejected and entry.event.type == EventTypes.Redaction:
                if entry.event.redacts is None:
                    # A redacted redaction doesn't have a `redacts` key, in
                    # which case lets just withhold the event.
                    #
                    # Note: Most of the time if the redactions has been
                    # redacted we still have the un-redacted event in the DB
                    # and so we'll still see the `redacts` key. However, this
                    # isn't always true e.g. if we have censored the event.
                    logger.debug(
                        "Withholding redaction event %s as we don't have redacts key",
                        event_id,
                    )
                    continue

                redacted_event_id = entry.event.redacts
                event_map = await self._get_events_from_cache_or_db([redacted_event_id])
                original_event_entry = event_map.get(redacted_event_id)
                if not original_event_entry:
                    # we don't have the redacted event (or it was rejected).
                    #
                    # We assume that the redaction isn't authorized for now; if the
                    # redacted event later turns up, the redaction will be re-checked,
                    # and if it is found valid, the original will get redacted before it
                    # is served to the client.
                    logger.debug(
                        "Withholding redaction event %s since we don't (yet) have the "
                        "original %s",
                        event_id,
                        redacted_event_id,
                    )
                    continue

                original_event = original_event_entry.event
                if original_event.type == EventTypes.Create:
                    # we never serve redactions of Creates to clients.
                    logger.info(
                        "Withholding redaction %s of create event %s",
                        event_id,
                        redacted_event_id,
                    )
                    continue

                if original_event.room_id != entry.event.room_id:
                    logger.info(
                        "Withholding redaction %s of event %s from a different room",
                        event_id,
                        redacted_event_id,
                    )
                    continue

                if entry.event.internal_metadata.need_to_check_redaction():
                    original_domain = get_domain_from_id(original_event.sender)
                    redaction_domain = get_domain_from_id(entry.event.sender)
                    if original_domain != redaction_domain:
                        # the senders don't match, so this is forbidden
                        logger.info(
                            "Withholding redaction %s whose sender domain %s doesn't "
                            "match that of redacted event %s %s",
                            event_id,
                            redaction_domain,
                            redacted_event_id,
                            original_domain,
                        )
                        continue

                    # Update the cache to save doing the checks again.
                    entry.event.internal_metadata.recheck_redaction = False

            event = entry.event

            if entry.redacted_event:
                if redact_behaviour == EventRedactBehaviour.BLOCK:
                    # Skip this event
                    continue
                elif redact_behaviour == EventRedactBehaviour.REDACT:
                    event = entry.redacted_event

            events.append(event)

            if get_prev_content:
                if "replaces_state" in event.unsigned:
                    prev = await self.get_event(
                        event.unsigned["replaces_state"],
                        get_prev_content=False,
                        allow_none=True,
                    )
                    if prev:
                        event.unsigned = dict(event.unsigned)
                        event.unsigned["prev_content"] = prev.content
                        event.unsigned["prev_sender"] = prev.sender

        return events

    async def _get_events_from_cache_or_db(
        self, event_ids: Iterable[str], allow_rejected: bool = False
    ) -> Dict[str, EventCacheEntry]:
        """Fetch a bunch of events from the cache or the database.

        If events are pulled from the database, they will be cached for future lookups.

        Unknown events are omitted from the response.

        Args:

            event_ids: The event_ids of the events to fetch

            allow_rejected: Whether to include rejected events. If False,
                rejected events are omitted from the response.

        Returns:
            map from event id to result
        """
        event_entry_map = self._get_events_from_cache(
            event_ids,
        )

        missing_events_ids = {e for e in event_ids if e not in event_entry_map}

        # We now look up if we're already fetching some of the events in the DB,
        # if so we wait for those lookups to finish instead of pulling the same
        # events out of the DB multiple times.
        #
        # Note: we might get the same `ObservableDeferred` back for multiple
        # events we're already fetching, so we deduplicate the deferreds to
        # avoid extraneous work (if we don't do this we can end up in a n^2 mode
        # when we wait on the same Deferred N times, then try and merge the
        # same dict into itself N times).
        already_fetching_ids: Set[str] = set()
        already_fetching_deferreds: Set[
            ObservableDeferred[Dict[str, EventCacheEntry]]
        ] = set()

        for event_id in missing_events_ids:
            deferred = self._current_event_fetches.get(event_id)
            if deferred is not None:
                # We're already pulling the event out of the DB. Add the deferred
                # to the collection of deferreds to wait on.
                already_fetching_ids.add(event_id)
                already_fetching_deferreds.add(deferred)

        missing_events_ids.difference_update(already_fetching_ids)

        if missing_events_ids:
            log_ctx = current_context()
            log_ctx.record_event_fetch(len(missing_events_ids))

            # Add entries to `self._current_event_fetches` for each event we're
            # going to pull from the DB. We use a single deferred that resolves
            # to all the events we pulled from the DB (this will result in this
            # function returning more events than requested, but that can happen
            # already due to `_get_events_from_db`).
            fetching_deferred: ObservableDeferred[
                Dict[str, EventCacheEntry]
            ] = ObservableDeferred(defer.Deferred(), consumeErrors=True)
            for event_id in missing_events_ids:
                self._current_event_fetches[event_id] = fetching_deferred

            # Note that _get_events_from_db is also responsible for turning db rows
            # into FrozenEvents (via _get_event_from_row), which involves seeing if
            # the events have been redacted, and if so pulling the redaction event out
            # of the database to check it.
            #
            try:
                missing_events = await self._get_events_from_db(
                    missing_events_ids,
                )

                event_entry_map.update(missing_events)
            except Exception as e:
                with PreserveLoggingContext():
                    fetching_deferred.errback(e)
                raise e
            finally:
                # Ensure that we mark these events as no longer being fetched.
                for event_id in missing_events_ids:
                    self._current_event_fetches.pop(event_id, None)

            with PreserveLoggingContext():
                fetching_deferred.callback(missing_events)

        if already_fetching_deferreds:
            # Wait for the other event requests to finish and add their results
            # to ours.
            results = await make_deferred_yieldable(
                defer.gatherResults(
                    (d.observe() for d in already_fetching_deferreds),
                    consumeErrors=True,
                )
            ).addErrback(unwrapFirstError)

            for result in results:
                # We filter out events that we haven't asked for as we might get
                # a *lot* of superfluous events back, and there is no point
                # going through and inserting them all (which can take time).
                event_entry_map.update(
                    (event_id, entry)
                    for event_id, entry in result.items()
                    if event_id in already_fetching_ids
                )

        if not allow_rejected:
            event_entry_map = {
                event_id: entry
                for event_id, entry in event_entry_map.items()
                if not entry.event.rejected_reason
            }

        return event_entry_map

    def _invalidate_get_event_cache(self, event_id: str) -> None:
        self._get_event_cache.invalidate((event_id,))

    def _get_events_from_cache(
        self, events: Iterable[str], update_metrics: bool = True
    ) -> Dict[str, EventCacheEntry]:
        """Fetch events from the caches.

        May return rejected events.

        Args:
            events: list of event_ids to fetch
            update_metrics: Whether to update the cache hit ratio metrics
        """
        event_map = {}

        for event_id in events:
            ret = self._get_event_cache.get(
                (event_id,), None, update_metrics=update_metrics
            )
            if not ret:
                continue

            event_map[event_id] = ret

        return event_map

    async def get_stripped_room_state_from_event_context(
        self,
        context: EventContext,
        state_types_to_include: Container[str],
        membership_user_id: Optional[str] = None,
    ) -> List[JsonDict]:
        """
        Retrieve the stripped state from a room, given an event context to retrieve state
        from as well as the state types to include. Optionally, include the membership
        events from a specific user.

        "Stripped" state means that only the `type`, `state_key`, `content` and `sender` keys
        are included from each state event.

        Args:
            context: The event context to retrieve state of the room from.
            state_types_to_include: The type of state events to include.
            membership_user_id: An optional user ID to include the stripped membership state
                events of. This is useful when generating the stripped state of a room for
                invites. We want to send membership events of the inviter, so that the
                invitee can display the inviter's profile information if the room lacks any.

        Returns:
            A list of dictionaries, each representing a stripped state event from the room.
        """
        current_state_ids = await context.get_current_state_ids()

        # We know this event is not an outlier, so this must be
        # non-None.
        assert current_state_ids is not None

        # The state to include
        state_to_include_ids = [
            e_id
            for k, e_id in current_state_ids.items()
            if k[0] in state_types_to_include
            or (membership_user_id and k == (EventTypes.Member, membership_user_id))
        ]

        state_to_include = await self.get_events(state_to_include_ids)

        return [
            {
                "type": e.type,
                "state_key": e.state_key,
                "content": e.content,
                "sender": e.sender,
            }
            for e in state_to_include.values()
        ]

    def _maybe_start_fetch_thread(self) -> None:
        """Starts an event fetch thread if we are not yet at the maximum number."""
        with self._event_fetch_lock:
            if (
                self._event_fetch_list
                and self._event_fetch_ongoing < EVENT_QUEUE_THREADS
            ):
                self._event_fetch_ongoing += 1
                event_fetch_ongoing_gauge.set(self._event_fetch_ongoing)
                # `_event_fetch_ongoing` is decremented in `_fetch_thread`.
                should_start = True
            else:
                should_start = False

        if should_start:
            run_as_background_process("fetch_events", self._fetch_thread)

    async def _fetch_thread(self) -> None:
        """Services requests for events from `_event_fetch_list`."""
        exc = None
        try:
            await self.db_pool.runWithConnection(self._fetch_loop)
        except BaseException as e:
            exc = e
            raise
        finally:
            should_restart = False
            event_fetches_to_fail = []
            with self._event_fetch_lock:
                self._event_fetch_ongoing -= 1
                event_fetch_ongoing_gauge.set(self._event_fetch_ongoing)

                # There may still be work remaining in `_event_fetch_list` if we
                # failed, or it was added in between us deciding to exit and
                # decrementing `_event_fetch_ongoing`.
                if self._event_fetch_list:
                    if exc is None:
                        # We decided to exit, but then some more work was added
                        # before `_event_fetch_ongoing` was decremented.
                        # If a new event fetch thread was not started, we should
                        # restart ourselves since the remaining event fetch threads
                        # may take a while to get around to the new work.
                        #
                        # Unfortunately it is not possible to tell whether a new
                        # event fetch thread was started, so we restart
                        # unconditionally. If we are unlucky, we will end up with
                        # an idle fetch thread, but it will time out after
                        # `EVENT_QUEUE_ITERATIONS * EVENT_QUEUE_TIMEOUT_S` seconds
                        # in any case.
                        #
                        # Note that multiple fetch threads may run down this path at
                        # the same time.
                        should_restart = True
                    elif isinstance(exc, Exception):
                        if self._event_fetch_ongoing == 0:
                            # We were the last remaining fetcher and failed.
                            # Fail any outstanding fetches since no one else will
                            # handle them.
                            event_fetches_to_fail = self._event_fetch_list
                            self._event_fetch_list = []
                        else:
                            # We weren't the last remaining fetcher, so another
                            # fetcher will pick up the work. This will either happen
                            # after their existing work, however long that takes,
                            # or after at most `EVENT_QUEUE_TIMEOUT_S` seconds if
                            # they are idle.
                            pass
                    else:
                        # The exception is a `SystemExit`, `KeyboardInterrupt` or
                        # `GeneratorExit`. Don't try to do anything clever here.
                        pass

            if should_restart:
                # We exited cleanly but noticed more work.
                self._maybe_start_fetch_thread()

            if event_fetches_to_fail:
                # We were the last remaining fetcher and failed.
                # Fail any outstanding fetches since no one else will handle them.
                assert exc is not None
                with PreserveLoggingContext():
                    for _, deferred in event_fetches_to_fail:
                        deferred.errback(exc)

    def _fetch_loop(self, conn: LoggingDatabaseConnection) -> None:
        """Takes a database connection and waits for requests for events from
        the _event_fetch_list queue.
        """
        i = 0
        while True:
            with self._event_fetch_lock:
                event_list = self._event_fetch_list
                self._event_fetch_list = []

                if not event_list:
                    # There are no requests waiting. If we haven't yet reached the
                    # maximum iteration limit, wait for some more requests to turn up.
                    # Otherwise, bail out.
                    single_threaded = self.database_engine.single_threaded
                    if (
                        not self.USE_DEDICATED_DB_THREADS_FOR_EVENT_FETCHING
                        or single_threaded
                        or i > EVENT_QUEUE_ITERATIONS
                    ):
                        return

                    self._event_fetch_lock.wait(EVENT_QUEUE_TIMEOUT_S)
                    i += 1
                    continue
                i = 0

            self._fetch_event_list(conn, event_list)

    def _fetch_event_list(
        self,
        conn: LoggingDatabaseConnection,
        event_list: List[Tuple[Iterable[str], "defer.Deferred[Dict[str, _EventRow]]"]],
    ) -> None:
        """Handle a load of requests from the _event_fetch_list queue

        Args:
            conn: database connection

            event_list:
                The fetch requests. Each entry consists of a list of event
                ids to be fetched, and a deferred to be completed once the
                events have been fetched.

                The deferreds are callbacked with a dictionary mapping from event id
                to event row. Note that it may well contain additional events that
                were not part of this request.
        """
        with Measure(self._clock, "_fetch_event_list"):
            try:
                events_to_fetch = {
                    event_id for events, _ in event_list for event_id in events
                }

                row_dict = self.db_pool.new_transaction(
                    conn, "do_fetch", [], [], self._fetch_event_rows, events_to_fetch
                )

                # We only want to resolve deferreds from the main thread
                def fire() -> None:
                    for _, d in event_list:
                        d.callback(row_dict)

                with PreserveLoggingContext():
                    self.hs.get_reactor().callFromThread(fire)
            except Exception as e:
                logger.exception("do_fetch")

                # We only want to resolve deferreds from the main thread
                def fire_errback(exc: Exception) -> None:
                    for _, d in event_list:
                        d.errback(exc)

                with PreserveLoggingContext():
                    self.hs.get_reactor().callFromThread(fire_errback, e)

    async def _get_events_from_db(
        self, event_ids: Collection[str]
    ) -> Dict[str, EventCacheEntry]:
        """Fetch a bunch of events from the database.

        May return rejected events.

        Returned events will be added to the cache for future lookups.

        Unknown events are omitted from the response.

        Args:
            event_ids: The event_ids of the events to fetch

        Returns:
            map from event id to result. May return extra events which
            weren't asked for.
        """
        fetched_event_ids: Set[str] = set()
        fetched_events: Dict[str, _EventRow] = {}
        events_to_fetch = event_ids

        while events_to_fetch:
            row_map = await self._enqueue_events(events_to_fetch)

            # we need to recursively fetch any redactions of those events
            redaction_ids: Set[str] = set()
            for event_id in events_to_fetch:
                row = row_map.get(event_id)
                fetched_event_ids.add(event_id)
                if row:
                    fetched_events[event_id] = row
                    redaction_ids.update(row.redactions)

            events_to_fetch = redaction_ids.difference(fetched_event_ids)
            if events_to_fetch:
                logger.debug("Also fetching redaction events %s", events_to_fetch)

        # build a map from event_id to EventBase
        event_map: Dict[str, EventBase] = {}
        for event_id, row in fetched_events.items():
            assert row.event_id == event_id

            rejected_reason = row.rejected_reason

            # If the event or metadata cannot be parsed, log the error and act
            # as if the event is unknown.
            try:
                d = db_to_json(row.json)
            except ValueError:
                logger.error("Unable to parse json from event: %s", event_id)
                continue
            try:
                internal_metadata = db_to_json(row.internal_metadata)
            except ValueError:
                logger.error(
                    "Unable to parse internal_metadata from event: %s", event_id
                )
                continue

            format_version = row.format_version
            if format_version is None:
                # This means that we stored the event before we had the concept
                # of a event format version, so it must be a V1 event.
                format_version = EventFormatVersions.V1

            room_version_id = row.room_version_id

            room_version: Optional[RoomVersion]
            if not room_version_id:
                # this should only happen for out-of-band membership events which
                # arrived before #6983 landed. For all other events, we should have
                # an entry in the 'rooms' table.
                #
                # However, the 'out_of_band_membership' flag is unreliable for older
                # invites, so just accept it for all membership events.
                #
                if d["type"] != EventTypes.Member:
                    raise Exception(
                        "Room %s for event %s is unknown" % (d["room_id"], event_id)
                    )

                # so, assuming this is an out-of-band-invite that arrived before #6983
                # landed, we know that the room version must be v5 or earlier (because
                # v6 hadn't been invented at that point, so invites from such rooms
                # would have been rejected.)
                #
                # The main reason we need to know the room version here (other than
                # choosing the right python Event class) is in case the event later has
                # to be redacted - and all the room versions up to v5 used the same
                # redaction algorithm.
                #
                # So, the following approximations should be adequate.

                if format_version == EventFormatVersions.V1:
                    # if it's event format v1 then it must be room v1 or v2
                    room_version = RoomVersions.V1
                elif format_version == EventFormatVersions.V2:
                    # if it's event format v2 then it must be room v3
                    room_version = RoomVersions.V3
                else:
                    # if it's event format v3 then it must be room v4 or v5
                    room_version = RoomVersions.V5
            else:
                room_version = KNOWN_ROOM_VERSIONS.get(room_version_id)
                if not room_version:
                    logger.warning(
                        "Event %s in room %s has unknown room version %s",
                        event_id,
                        d["room_id"],
                        room_version_id,
                    )
                    continue

                if room_version.event_format != format_version:
                    logger.error(
                        "Event %s in room %s with version %s has wrong format: "
                        "expected %s, was %s",
                        event_id,
                        d["room_id"],
                        room_version_id,
                        room_version.event_format,
                        format_version,
                    )
                    continue

            original_ev = make_event_from_dict(
                event_dict=d,
                room_version=room_version,
                internal_metadata_dict=internal_metadata,
                rejected_reason=rejected_reason,
            )
            original_ev.internal_metadata.stream_ordering = row.stream_ordering
            original_ev.internal_metadata.outlier = row.outlier

            event_map[event_id] = original_ev

        # finally, we can decide whether each one needs redacting, and build
        # the cache entries.
        result_map: Dict[str, EventCacheEntry] = {}
        for event_id, original_ev in event_map.items():
            redactions = fetched_events[event_id].redactions
            redacted_event = self._maybe_redact_event_row(
                original_ev, redactions, event_map
            )

            cache_entry = EventCacheEntry(
                event=original_ev, redacted_event=redacted_event
            )

            self._get_event_cache.set((event_id,), cache_entry)
            result_map[event_id] = cache_entry

        return result_map

    async def _enqueue_events(self, events: Collection[str]) -> Dict[str, _EventRow]:
        """Fetches events from the database using the _event_fetch_list. This
        allows batch and bulk fetching of events - it allows us to fetch events
        without having to create a new transaction for each request for events.

        Args:
            events: events to be fetched.

        Returns:
            A map from event id to row data from the database. May contain events
            that weren't requested.
        """

        events_d: "defer.Deferred[Dict[str, _EventRow]]" = defer.Deferred()
        with self._event_fetch_lock:
            self._event_fetch_list.append((events, events_d))
            self._event_fetch_lock.notify()

        self._maybe_start_fetch_thread()

        logger.debug("Loading %d events: %s", len(events), events)
        with PreserveLoggingContext():
            row_map = await events_d
        logger.debug("Loaded %d events (%d rows)", len(events), len(row_map))

        return row_map

    def _fetch_event_rows(
        self, txn: LoggingTransaction, event_ids: Iterable[str]
    ) -> Dict[str, _EventRow]:
        """Fetch event rows from the database

        Events which are not found are omitted from the result.

        Args:
            txn: The database transaction.
            event_ids: event IDs to fetch

        Returns:
            A map from event id to event info.
        """
        event_dict = {}
        for evs in batch_iter(event_ids, 200):
            sql = """\
                SELECT
                  e.event_id,
                  e.stream_ordering,
                  ej.internal_metadata,
                  ej.json,
                  ej.format_version,
                  r.room_version,
                  rej.reason,
                  e.outlier
                FROM events AS e
                  JOIN event_json AS ej USING (event_id)
                  LEFT JOIN rooms r ON r.room_id = e.room_id
                  LEFT JOIN rejections as rej USING (event_id)
                WHERE """

            clause, args = make_in_list_sql_clause(
                txn.database_engine, "e.event_id", evs
            )

            txn.execute(sql + clause, args)

            for row in txn:
                event_id = row[0]
                event_dict[event_id] = _EventRow(
                    event_id=event_id,
                    stream_ordering=row[1],
                    internal_metadata=row[2],
                    json=row[3],
                    format_version=row[4],
                    room_version_id=row[5],
                    rejected_reason=row[6],
                    redactions=[],
                    outlier=row[7],
                )

            # check for redactions
            redactions_sql = "SELECT event_id, redacts FROM redactions WHERE "

            clause, args = make_in_list_sql_clause(txn.database_engine, "redacts", evs)

            txn.execute(redactions_sql + clause, args)

            for (redacter, redacted) in txn:
                d = event_dict.get(redacted)
                if d:
                    d.redactions.append(redacter)

        return event_dict

    def _maybe_redact_event_row(
        self,
        original_ev: EventBase,
        redactions: Iterable[str],
        event_map: Dict[str, EventBase],
    ) -> Optional[EventBase]:
        """Given an event object and a list of possible redacting event ids,
        determine whether to honour any of those redactions and if so return a redacted
        event.

        Args:
             original_ev: The original event.
             redactions: list of event ids of potential redaction events
             event_map: other events which have been fetched, in which we can
                look up the redaaction events. Map from event id to event.

        Returns:
            If the event should be redacted, a pruned event object. Otherwise, None.
        """
        if original_ev.type == "m.room.create":
            # we choose to ignore redactions of m.room.create events.
            return None

        for redaction_id in redactions:
            redaction_event = event_map.get(redaction_id)
            if not redaction_event or redaction_event.rejected_reason:
                # we don't have the redaction event, or the redaction event was not
                # authorized.
                logger.debug(
                    "%s was redacted by %s but redaction not found/authed",
                    original_ev.event_id,
                    redaction_id,
                )
                continue

            if redaction_event.room_id != original_ev.room_id:
                logger.debug(
                    "%s was redacted by %s but redaction was in a different room!",
                    original_ev.event_id,
                    redaction_id,
                )
                continue

            # Starting in room version v3, some redactions need to be
            # rechecked if we didn't have the redacted event at the
            # time, so we recheck on read instead.
            if redaction_event.internal_metadata.need_to_check_redaction():
                expected_domain = get_domain_from_id(original_ev.sender)
                if get_domain_from_id(redaction_event.sender) == expected_domain:
                    # This redaction event is allowed. Mark as not needing a recheck.
                    redaction_event.internal_metadata.recheck_redaction = False
                else:
                    # Senders don't match, so the event isn't actually redacted
                    logger.debug(
                        "%s was redacted by %s but the senders don't match",
                        original_ev.event_id,
                        redaction_id,
                    )
                    continue

            logger.debug("Redacting %s due to %s", original_ev.event_id, redaction_id)

            # we found a good redaction event. Redact!
            redacted_event = prune_event(original_ev)
            redacted_event.unsigned["redacted_by"] = redaction_id

            # It's fine to add the event directly, since get_pdu_json
            # will serialise this field correctly
            redacted_event.unsigned["redacted_because"] = redaction_event

            return redacted_event

        # no valid redaction found for this event
        return None

    async def have_events_in_timeline(self, event_ids: Iterable[str]) -> Set[str]:
        """Given a list of event ids, check if we have already processed and
        stored them as non outliers.
        """
        rows = await self.db_pool.simple_select_many_batch(
            table="events",
            retcols=("event_id",),
            column="event_id",
            iterable=list(event_ids),
            keyvalues={"outlier": False},
            desc="have_events_in_timeline",
        )

        return {r["event_id"] for r in rows}

    async def have_seen_events(
        self, room_id: str, event_ids: Iterable[str]
    ) -> Set[str]:
        """Given a list of event ids, check if we have already processed them.

        The room_id is only used to structure the cache (so that it can later be
        invalidated by room_id) - there is no guarantee that the events are actually
        in the room in question.

        Args:
            room_id: Room we are polling
            event_ids: events we are looking for

        Returns:
            The set of events we have already seen.
        """
        res = await self._have_seen_events_dict(
            (room_id, event_id) for event_id in event_ids
        )
        return {eid for ((_rid, eid), have_event) in res.items() if have_event}

    @cachedList("have_seen_event", "keys")
    async def _have_seen_events_dict(
        self, keys: Iterable[Tuple[str, str]]
    ) -> Dict[Tuple[str, str], bool]:
        """Helper for have_seen_events

        Returns:
             a dict {(room_id, event_id)-> bool}
        """
        # if the event cache contains the event, obviously we've seen it.

        cache_results = {
            (rid, eid) for (rid, eid) in keys if self._get_event_cache.contains((eid,))
        }
        results = {x: True for x in cache_results}

        def have_seen_events_txn(
            txn: LoggingTransaction, chunk: Tuple[Tuple[str, str], ...]
        ) -> None:
            # we deliberately do *not* query the database for room_id, to make the
            # query an index-only lookup on `events_event_id_key`.
            #
            # We therefore pull the events from the database into a set...

            sql = "SELECT event_id FROM events AS e WHERE "
            clause, args = make_in_list_sql_clause(
                txn.database_engine, "e.event_id", [eid for (_rid, eid) in chunk]
            )
            txn.execute(sql + clause, args)
            found_events = {eid for eid, in txn}

            # ... and then we can update the results for each row in the batch
            results.update({(rid, eid): (eid in found_events) for (rid, eid) in chunk})

        # each batch requires its own index scan, so we make the batches as big as
        # possible.
        for chunk in batch_iter((k for k in keys if k not in cache_results), 500):
            await self.db_pool.runInteraction(
                "have_seen_events", have_seen_events_txn, chunk
            )

        return results

    @cached(max_entries=100000, tree=True)
    async def have_seen_event(self, room_id: str, event_id: str) -> NoReturn:
        # this only exists for the benefit of the @cachedList descriptor on
        # _have_seen_events_dict
        raise NotImplementedError()

    def _get_current_state_event_counts_txn(
        self, txn: LoggingTransaction, room_id: str
    ) -> int:
        """
        See get_current_state_event_counts.
        """
        sql = "SELECT COUNT(*) FROM current_state_events WHERE room_id=?"
        txn.execute(sql, (room_id,))
        row = txn.fetchone()
        return row[0] if row else 0

    async def get_current_state_event_counts(self, room_id: str) -> int:
        """
        Gets the current number of state events in a room.

        Args:
            room_id: The room ID to query.

        Returns:
            The current number of state events.
        """
        return await self.db_pool.runInteraction(
            "get_current_state_event_counts",
            self._get_current_state_event_counts_txn,
            room_id,
        )

    async def get_room_complexity(self, room_id: str) -> Dict[str, float]:
        """
        Get a rough approximation of the complexity of the room. This is used by
        remote servers to decide whether they wish to join the room or not.
        Higher complexity value indicates that being in the room will consume
        more resources.

        Args:
            room_id: The room ID to query.

        Returns:
            dict[str:float] of complexity version to complexity.
        """
        state_events = await self.get_current_state_event_counts(room_id)

        # Call this one "v1", so we can introduce new ones as we want to develop
        # it.
        complexity_v1 = round(state_events / 500, 2)

        return {"v1": complexity_v1}

    async def get_all_new_forward_event_rows(
        self, instance_name: str, last_id: int, current_id: int, limit: int
    ) -> List[Tuple[int, str, str, str, str, str, str, str, str]]:
        """Returns new events, for the Events replication stream

        Args:
            last_id: the last stream_id from the previous batch.
            current_id: the maximum stream_id to return up to
            limit: the maximum number of rows to return

        Returns:
            a list of events stream rows. Each tuple consists of a stream id as
            the first element, followed by fields suitable for casting into an
            EventsStreamRow.
        """

        def get_all_new_forward_event_rows(
            txn: LoggingTransaction,
        ) -> List[Tuple[int, str, str, str, str, str, str, str, str]]:
            sql = (
                "SELECT e.stream_ordering, e.event_id, e.room_id, e.type,"
                " se.state_key, redacts, relates_to_id, membership, rejections.reason IS NOT NULL"
                " FROM events AS e"
                " LEFT JOIN redactions USING (event_id)"
                " LEFT JOIN state_events AS se USING (event_id)"
                " LEFT JOIN event_relations USING (event_id)"
                " LEFT JOIN room_memberships USING (event_id)"
                " LEFT JOIN rejections USING (event_id)"
                " WHERE ? < stream_ordering AND stream_ordering <= ?"
                " AND instance_name = ?"
                " ORDER BY stream_ordering ASC"
                " LIMIT ?"
            )
            txn.execute(sql, (last_id, current_id, instance_name, limit))
            return cast(
                List[Tuple[int, str, str, str, str, str, str, str, str]], txn.fetchall()
            )

        return await self.db_pool.runInteraction(
            "get_all_new_forward_event_rows", get_all_new_forward_event_rows
        )

    async def get_ex_outlier_stream_rows(
        self, instance_name: str, last_id: int, current_id: int
    ) -> List[Tuple[int, str, str, str, str, str, str, str, str]]:
        """Returns de-outliered events, for the Events replication stream

        Args:
            last_id: the last stream_id from the previous batch.
            current_id: the maximum stream_id to return up to

        Returns:
            a list of events stream rows. Each tuple consists of a stream id as
            the first element, followed by fields suitable for casting into an
            EventsStreamRow.
        """

        def get_ex_outlier_stream_rows_txn(
            txn: LoggingTransaction,
        ) -> List[Tuple[int, str, str, str, str, str, str, str, str]]:
            sql = (
                "SELECT event_stream_ordering, e.event_id, e.room_id, e.type,"
                " se.state_key, redacts, relates_to_id, membership, rejections.reason IS NOT NULL"
                " FROM events AS e"
                " INNER JOIN ex_outlier_stream AS out USING (event_id)"
                " LEFT JOIN redactions USING (event_id)"
                " LEFT JOIN state_events AS se USING (event_id)"
                " LEFT JOIN event_relations USING (event_id)"
                " LEFT JOIN room_memberships USING (event_id)"
                " LEFT JOIN rejections USING (event_id)"
                " WHERE ? < event_stream_ordering"
                " AND event_stream_ordering <= ?"
                " AND out.instance_name = ?"
                " ORDER BY event_stream_ordering ASC"
            )

            txn.execute(sql, (last_id, current_id, instance_name))
            return cast(
                List[Tuple[int, str, str, str, str, str, str, str, str]], txn.fetchall()
            )

        return await self.db_pool.runInteraction(
            "get_ex_outlier_stream_rows", get_ex_outlier_stream_rows_txn
        )

    async def get_all_new_backfill_event_rows(
        self, instance_name: str, last_id: int, current_id: int, limit: int
    ) -> Tuple[List[Tuple[int, Tuple[str, str, str, str, str, str]]], int, bool]:
        """Get updates for backfill replication stream, including all new
        backfilled events and events that have gone from being outliers to not.

        NOTE: The IDs given here are from replication, and so should be
        *positive*.

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

        def get_all_new_backfill_event_rows(
            txn: LoggingTransaction,
        ) -> Tuple[List[Tuple[int, Tuple[str, str, str, str, str, str]]], int, bool]:
            sql = (
                "SELECT -e.stream_ordering, e.event_id, e.room_id, e.type,"
                " se.state_key, redacts, relates_to_id"
                " FROM events AS e"
                " LEFT JOIN redactions USING (event_id)"
                " LEFT JOIN state_events AS se USING (event_id)"
                " LEFT JOIN event_relations USING (event_id)"
                " WHERE ? > stream_ordering AND stream_ordering >= ?"
                "  AND instance_name = ?"
                " ORDER BY stream_ordering ASC"
                " LIMIT ?"
            )
            txn.execute(sql, (-last_id, -current_id, instance_name, limit))
            new_event_updates: List[
                Tuple[int, Tuple[str, str, str, str, str, str]]
            ] = []
            row: Tuple[int, str, str, str, str, str, str]
            # Type safety: iterating over `txn` yields `Tuple`, i.e.
            # `Tuple[Any, ...]` of arbitrary length. Mypy detects assigning a
            # variadic tuple to a fixed length tuple and flags it up as an error.
            for row in txn:  # type: ignore[assignment]
                new_event_updates.append((row[0], row[1:]))

            limited = False
            if len(new_event_updates) == limit:
                upper_bound = new_event_updates[-1][0]
                limited = True
            else:
                upper_bound = current_id

            sql = (
                "SELECT -event_stream_ordering, e.event_id, e.room_id, e.type,"
                " se.state_key, redacts, relates_to_id"
                " FROM events AS e"
                " INNER JOIN ex_outlier_stream AS out USING (event_id)"
                " LEFT JOIN redactions USING (event_id)"
                " LEFT JOIN state_events AS se USING (event_id)"
                " LEFT JOIN event_relations USING (event_id)"
                " WHERE ? > event_stream_ordering"
                " AND event_stream_ordering >= ?"
                " AND out.instance_name = ?"
                " ORDER BY event_stream_ordering DESC"
            )
            txn.execute(sql, (-last_id, -upper_bound, instance_name))
            # Type safety: iterating over `txn` yields `Tuple`, i.e.
            # `Tuple[Any, ...]` of arbitrary length. Mypy detects assigning a
            # variadic tuple to a fixed length tuple and flags it up as an error.
            for row in txn:  # type: ignore[assignment]
                new_event_updates.append((row[0], row[1:]))

            if len(new_event_updates) >= limit:
                upper_bound = new_event_updates[-1][0]
                limited = True

            return new_event_updates, upper_bound, limited

        return await self.db_pool.runInteraction(
            "get_all_new_backfill_event_rows", get_all_new_backfill_event_rows
        )

    async def get_all_updated_current_state_deltas(
        self, instance_name: str, from_token: int, to_token: int, target_row_count: int
    ) -> Tuple[List[Tuple[int, str, str, str, str]], int, bool]:
        """Fetch updates from current_state_delta_stream

        Args:
            from_token: The previous stream token. Updates from this stream id will
                be excluded.

            to_token: The current stream token (ie the upper limit). Updates up to this
                stream id will be included (modulo the 'limit' param)

            target_row_count: The number of rows to try to return. If more rows are
                available, we will set 'limited' in the result. In the event of a large
                batch, we may return more rows than this.
        Returns:
            A triplet `(updates, new_last_token, limited)`, where:
               * `updates` is a list of database tuples.
               * `new_last_token` is the new position in stream.
               * `limited` is whether there are more updates to fetch.
        """

        def get_all_updated_current_state_deltas_txn(
            txn: LoggingTransaction,
        ) -> List[Tuple[int, str, str, str, str]]:
            sql = """
                SELECT stream_id, room_id, type, state_key, event_id
                FROM current_state_delta_stream
                WHERE ? < stream_id AND stream_id <= ?
                    AND instance_name = ?
                ORDER BY stream_id ASC LIMIT ?
            """
            txn.execute(sql, (from_token, to_token, instance_name, target_row_count))
            return cast(List[Tuple[int, str, str, str, str]], txn.fetchall())

        def get_deltas_for_stream_id_txn(
            txn: LoggingTransaction, stream_id: int
        ) -> List[Tuple[int, str, str, str, str]]:
            sql = """
                SELECT stream_id, room_id, type, state_key, event_id
                FROM current_state_delta_stream
                WHERE stream_id = ?
            """
            txn.execute(sql, [stream_id])
            return cast(List[Tuple[int, str, str, str, str]], txn.fetchall())

        # we need to make sure that, for every stream id in the results, we get *all*
        # the rows with that stream id.

        rows: List[Tuple[int, str, str, str, str]] = await self.db_pool.runInteraction(
            "get_all_updated_current_state_deltas",
            get_all_updated_current_state_deltas_txn,
        )

        # if we've got fewer rows than the limit, we're good
        if len(rows) < target_row_count:
            return rows, to_token, False

        # we hit the limit, so reduce the upper limit so that we exclude the stream id
        # of the last row in the result.
        assert rows[-1][0] <= to_token
        to_token = rows[-1][0] - 1

        # search backwards through the list for the point to truncate
        for idx in range(len(rows) - 1, 0, -1):
            if rows[idx - 1][0] <= to_token:
                return rows[:idx], to_token, True

        # bother. We didn't get a full set of changes for even a single
        # stream id. let's run the query again, without a row limit, but for
        # just one stream id.
        to_token += 1
        rows = await self.db_pool.runInteraction(
            "get_deltas_for_stream_id", get_deltas_for_stream_id_txn, to_token
        )

        return rows, to_token, True

    async def is_event_after(self, event_id1: str, event_id2: str) -> bool:
        """Returns True if event_id1 is after event_id2 in the stream"""
        to_1, so_1 = await self.get_event_ordering(event_id1)
        to_2, so_2 = await self.get_event_ordering(event_id2)
        return (to_1, so_1) > (to_2, so_2)

    @cached(max_entries=5000)
    async def get_event_ordering(self, event_id: str) -> Tuple[int, int]:
        res = await self.db_pool.simple_select_one(
            table="events",
            retcols=["topological_ordering", "stream_ordering"],
            keyvalues={"event_id": event_id},
            allow_none=True,
        )

        if not res:
            raise SynapseError(404, "Could not find event %s" % (event_id,))

        return int(res["topological_ordering"]), int(res["stream_ordering"])

    async def get_next_event_to_expire(self) -> Optional[Tuple[str, int]]:
        """Retrieve the entry with the lowest expiry timestamp in the event_expiry
        table, or None if there's no more event to expire.

        Returns:
            A tuple containing the event ID as its first element and an expiry timestamp
            as its second one, if there's at least one row in the event_expiry table.
            None otherwise.
        """

        def get_next_event_to_expire_txn(
            txn: LoggingTransaction,
        ) -> Optional[Tuple[str, int]]:
            txn.execute(
                """
                SELECT event_id, expiry_ts FROM event_expiry
                ORDER BY expiry_ts ASC LIMIT 1
                """
            )

            return cast(Optional[Tuple[str, int]], txn.fetchone())

        return await self.db_pool.runInteraction(
            desc="get_next_event_to_expire", func=get_next_event_to_expire_txn
        )

    async def get_event_id_from_transaction_id(
        self, room_id: str, user_id: str, token_id: int, txn_id: str
    ) -> Optional[str]:
        """Look up if we have already persisted an event for the transaction ID,
        returning the event ID if so.
        """
        return await self.db_pool.simple_select_one_onecol(
            table="event_txn_id",
            keyvalues={
                "room_id": room_id,
                "user_id": user_id,
                "token_id": token_id,
                "txn_id": txn_id,
            },
            retcol="event_id",
            allow_none=True,
            desc="get_event_id_from_transaction_id",
        )

    async def get_already_persisted_events(
        self, events: Iterable[EventBase]
    ) -> Dict[str, str]:
        """Look up if we have already persisted an event for the transaction ID,
        returning a mapping from event ID in the given list to the event ID of
        an existing event.

        Also checks if there are duplicates in the given events, if there are
        will map duplicates to the *first* event.
        """

        mapping = {}
        txn_id_to_event: Dict[Tuple[str, int, str], str] = {}

        for event in events:
            token_id = getattr(event.internal_metadata, "token_id", None)
            txn_id = getattr(event.internal_metadata, "txn_id", None)

            if token_id and txn_id:
                # Check if this is a duplicate of an event in the given events.
                existing = txn_id_to_event.get((event.room_id, token_id, txn_id))
                if existing:
                    mapping[event.event_id] = existing
                    continue

                # Check if this is a duplicate of an event we've already
                # persisted.
                existing = await self.get_event_id_from_transaction_id(
                    event.room_id, event.sender, token_id, txn_id
                )
                if existing:
                    mapping[event.event_id] = existing
                    txn_id_to_event[(event.room_id, token_id, txn_id)] = existing
                else:
                    txn_id_to_event[(event.room_id, token_id, txn_id)] = event.event_id

        return mapping

    @wrap_as_background_process("_cleanup_old_transaction_ids")
    async def _cleanup_old_transaction_ids(self) -> None:
        """Cleans out transaction id mappings older than 24hrs."""

        def _cleanup_old_transaction_ids_txn(txn: LoggingTransaction) -> None:
            sql = """
                DELETE FROM event_txn_id
                WHERE inserted_ts < ?
            """
            one_day_ago = self._clock.time_msec() - 24 * 60 * 60 * 1000
            txn.execute(sql, (one_day_ago,))

        return await self.db_pool.runInteraction(
            "_cleanup_old_transaction_ids",
            _cleanup_old_transaction_ids_txn,
        )

    async def is_event_next_to_backward_gap(self, event: EventBase) -> bool:
        """Check if the given event is next to a backward gap of missing events.
        <latest messages> A(False)--->B(False)--->C(True)--->  <gap, unknown events> <oldest messages>

        Args:
            room_id: room where the event lives
            event_id: event to check

        Returns:
            Boolean indicating whether it's an extremity
        """

        def is_event_next_to_backward_gap_txn(txn: LoggingTransaction) -> bool:
            # If the event in question has any of its prev_events listed as a
            # backward extremity, it's next to a gap.
            #
            # We can't just check the backward edges in `event_edges` because
            # when we persist events, we will also record the prev_events as
            # edges to the event in question regardless of whether we have those
            # prev_events yet. We need to check whether those prev_events are
            # backward extremities, also known as gaps, that need to be
            # backfilled.
            backward_extremity_query = """
                SELECT 1 FROM event_backward_extremities
                WHERE
                    room_id = ?
                    AND %s
                LIMIT 1
            """

            # If the event in question is a backward extremity or has any of its
            # prev_events listed as a backward extremity, it's next to a
            # backward gap.
            clause, args = make_in_list_sql_clause(
                self.database_engine,
                "event_id",
                [event.event_id] + list(event.prev_event_ids()),
            )

            txn.execute(backward_extremity_query % (clause,), [event.room_id] + args)
            backward_extremities = txn.fetchall()

            # We consider any backward extremity as a backward gap
            if len(backward_extremities):
                return True

            return False

        return await self.db_pool.runInteraction(
            "is_event_next_to_backward_gap_txn",
            is_event_next_to_backward_gap_txn,
        )

    async def is_event_next_to_forward_gap(self, event: EventBase) -> bool:
        """Check if the given event is next to a forward gap of missing events.
        The gap in front of the latest events is not considered a gap.
        <latest messages> A(False)--->B(False)--->C(False)--->  <gap, unknown events> <oldest messages>
        <latest messages> A(False)--->B(False)--->  <gap, unknown events>  --->D(True)--->E(False) <oldest messages>

        Args:
            room_id: room where the event lives
            event_id: event to check

        Returns:
            Boolean indicating whether it's an extremity
        """

        def is_event_next_to_gap_txn(txn: LoggingTransaction) -> bool:
            # If the event in question is a forward extremity, we will just
            # consider any potential forward gap as not a gap since it's one of
            # the latest events in the room.
            #
            # `event_forward_extremities` does not include backfilled or outlier
            # events so we can't rely on it to find forward gaps. We can only
            # use it to determine whether a message is the latest in the room.
            #
            # We can't combine this query with the `forward_edge_query` below
            # because if the event in question has no forward edges (isn't
            # referenced by any other event's prev_events) but is in
            # `event_forward_extremities`, we don't want to return 0 rows and
            # say it's next to a gap.
            forward_extremity_query = """
                SELECT 1 FROM event_forward_extremities
                WHERE
                    room_id = ?
                    AND event_id = ?
                LIMIT 1
            """

            # Check to see whether the event in question is already referenced
            # by another event. If we don't see any edges, we're next to a
            # forward gap.
            forward_edge_query = """
                SELECT 1 FROM event_edges
                /* Check to make sure the event referencing our event in question is not rejected */
                LEFT JOIN rejections ON event_edges.event_id == rejections.event_id
                WHERE
                    event_edges.room_id = ?
                    AND event_edges.prev_event_id = ?
                    /* It's not a valid edge if the event referencing our event in
                     * question is rejected.
                     */
                    AND rejections.event_id IS NULL
                LIMIT 1
            """

            # We consider any forward extremity as the latest in the room and
            # not a forward gap.
            #
            # To expand, even though there is technically a gap at the front of
            # the room where the forward extremities are, we consider those the
            # latest messages in the room so asking other homeservers for more
            # is useless. The new latest messages will just be federated as
            # usual.
            txn.execute(forward_extremity_query, (event.room_id, event.event_id))
            forward_extremities = txn.fetchall()
            if len(forward_extremities):
                return False

            # If there are no forward edges to the event in question (another
            # event hasn't referenced this event in their prev_events), then we
            # assume there is a forward gap in the history.
            txn.execute(forward_edge_query, (event.room_id, event.event_id))
            forward_edges = txn.fetchall()
            if not len(forward_edges):
                return True

            return False

        return await self.db_pool.runInteraction(
            "is_event_next_to_gap_txn",
            is_event_next_to_gap_txn,
        )

    async def get_event_id_for_timestamp(
        self, room_id: str, timestamp: int, direction: str
    ) -> Optional[str]:
        """Find the closest event to the given timestamp in the given direction.

        Args:
            room_id: Room to fetch the event from
            timestamp: The point in time (inclusive) we should navigate from in
                the given direction to find the closest event.
            direction: ["f"|"b"] to indicate whether we should navigate forward
                or backward from the given timestamp to find the closest event.

        Returns:
            The closest event_id otherwise None if we can't find any event in
            the given direction.
        """

        sql_template = """
            SELECT event_id FROM events
            LEFT JOIN rejections USING (event_id)
            WHERE
                origin_server_ts %s ?
                AND room_id = ?
                /* Make sure event is not rejected */
                AND rejections.event_id IS NULL
            ORDER BY origin_server_ts %s
            LIMIT 1;
        """

        def get_event_id_for_timestamp_txn(txn: LoggingTransaction) -> Optional[str]:
            if direction == "b":
                # Find closest event *before* a given timestamp. We use descending
                # (which gives values largest to smallest) because we want the
                # largest possible timestamp *before* the given timestamp.
                comparison_operator = "<="
                order = "DESC"
            else:
                # Find closest event *after* a given timestamp. We use ascending
                # (which gives values smallest to largest) because we want the
                # closest possible timestamp *after* the given timestamp.
                comparison_operator = ">="
                order = "ASC"

            txn.execute(
                sql_template % (comparison_operator, order), (timestamp, room_id)
            )
            row = txn.fetchone()
            if row:
                (event_id,) = row
                return event_id

            return None

        if direction not in ("f", "b"):
            raise ValueError("Unknown direction: %s" % (direction,))

        return await self.db_pool.runInteraction(
            "get_event_id_for_timestamp_txn",
            get_event_id_for_timestamp_txn,
        )
