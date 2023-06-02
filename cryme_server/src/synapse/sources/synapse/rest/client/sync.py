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
import itertools
import logging
from collections import defaultdict
from typing import (
    TYPE_CHECKING,
    Any,
    Awaitable,
    Callable,
    Dict,
    Iterable,
    List,
    Optional,
    Tuple,
    Union,
)

from synapse.api.constants import Membership, PresenceState
from synapse.api.errors import Codes, StoreError, SynapseError
from synapse.api.filtering import FilterCollection
from synapse.api.presence import UserPresenceState
from synapse.events import EventBase
from synapse.events.utils import (
    format_event_for_client_v2_without_room_id,
    format_event_raw,
)
from synapse.handlers.presence import format_user_presence_state
from synapse.handlers.sync import (
    ArchivedSyncResult,
    InvitedSyncResult,
    JoinedSyncResult,
    KnockedSyncResult,
    SyncConfig,
    SyncResult,
)
from synapse.http.server import HttpServer
from synapse.http.servlet import RestServlet, parse_boolean, parse_integer, parse_string
from synapse.http.site import SynapseRequest
from synapse.logging.opentracing import trace
from synapse.types import JsonDict, StreamToken
from synapse.util import json_decoder

from ._base import client_patterns, set_timeline_upper_limit

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


class SyncRestServlet(RestServlet):
    """

    GET parameters::
        timeout(int): How long to wait for new events in milliseconds.
        since(batch_token): Batch token when asking for incremental deltas.
        set_presence(str): What state the device presence should be set to.
            default is "online".
        filter(filter_id): A filter to apply to the events returned.

    Response JSON::
        {
          "next_batch": // batch token for the next /sync
          "presence": // presence data for the user.
          "rooms": {
            "join": { // Joined rooms being updated.
              "${room_id}": { // Id of the room being updated
                "event_map": // Map of EventID -> event JSON.
                "timeline": { // The recent events in the room if gap is "true"
                  "limited": // Was the per-room event limit exceeded?
                             // otherwise the next events in the room.
                  "events": [] // list of EventIDs in the "event_map".
                  "prev_batch": // back token for getting previous events.
                }
                "state": {"events": []} // list of EventIDs updating the
                                        // current state to be what it should
                                        // be at the end of the batch.
                "ephemeral": {"events": []} // list of event objects
              }
            },
            "invite": {}, // Invited rooms being updated.
            "leave": {} // Archived rooms being updated.
          }
        }
    """

    PATTERNS = client_patterns("/sync$")
    ALLOWED_PRESENCE = {"online", "offline", "unavailable"}

    def __init__(self, hs: "HomeServer"):
        super().__init__()
        self.hs = hs
        self.auth = hs.get_auth()
        self.store = hs.get_datastore()
        self.sync_handler = hs.get_sync_handler()
        self.clock = hs.get_clock()
        self.filtering = hs.get_filtering()
        self.presence_handler = hs.get_presence_handler()
        self._server_notices_sender = hs.get_server_notices_sender()
        self._event_serializer = hs.get_event_client_serializer()

    async def on_GET(self, request: SynapseRequest) -> Tuple[int, JsonDict]:
        # This will always be set by the time Twisted calls us.
        assert request.args is not None

        if b"from" in request.args:
            # /events used to use 'from', but /sync uses 'since'.
            # Lets be helpful and whine if we see a 'from'.
            raise SynapseError(
                400, "'from' is not a valid query parameter. Did you mean 'since'?"
            )

        requester = await self.auth.get_user_by_req(request, allow_guest=True)
        user = requester.user
        device_id = requester.device_id

        timeout = parse_integer(request, "timeout", default=0)
        since = parse_string(request, "since")
        set_presence = parse_string(
            request,
            "set_presence",
            default="online",
            allowed_values=self.ALLOWED_PRESENCE,
        )
        filter_id = parse_string(request, "filter")
        full_state = parse_boolean(request, "full_state", default=False)

        logger.debug(
            "/sync: user=%r, timeout=%r, since=%r, "
            "set_presence=%r, filter_id=%r, device_id=%r",
            user,
            timeout,
            since,
            set_presence,
            filter_id,
            device_id,
        )

        request_key = (user, timeout, since, filter_id, full_state, device_id)

        if filter_id is None:
            filter_collection = self.filtering.DEFAULT_FILTER_COLLECTION
        elif filter_id.startswith("{"):
            try:
                filter_object = json_decoder.decode(filter_id)
                set_timeline_upper_limit(
                    filter_object, self.hs.config.server.filter_timeline_limit
                )
            except Exception:
                raise SynapseError(400, "Invalid filter JSON")
            self.filtering.check_valid_filter(filter_object)
            filter_collection = FilterCollection(self.hs, filter_object)
        else:
            try:
                filter_collection = await self.filtering.get_user_filter(
                    user.localpart, filter_id
                )
            except StoreError as err:
                if err.code != 404:
                    raise
                # fix up the description and errcode to be more useful
                raise SynapseError(400, "No such filter", errcode=Codes.INVALID_PARAM)

        sync_config = SyncConfig(
            user=user,
            filter_collection=filter_collection,
            is_guest=requester.is_guest,
            request_key=request_key,
            device_id=device_id,
        )

        since_token = None
        if since is not None:
            since_token = await StreamToken.from_string(self.store, since)

        # send any outstanding server notices to the user.
        await self._server_notices_sender.on_user_syncing(user.to_string())

        affect_presence = set_presence != PresenceState.OFFLINE

        if affect_presence:
            await self.presence_handler.set_state(
                user, {"presence": set_presence}, True
            )

        context = await self.presence_handler.user_syncing(
            user.to_string(), affect_presence=affect_presence
        )
        with context:
            sync_result = await self.sync_handler.wait_for_sync_for_user(
                requester,
                sync_config,
                since_token=since_token,
                timeout=timeout,
                full_state=full_state,
            )

        # the client may have disconnected by now; don't bother to serialize the
        # response if so.
        if request._disconnected:
            logger.info("Client has disconnected; not serializing response.")
            return 200, {}

        time_now = self.clock.time_msec()
        # We know that the the requester has an access token since appservices
        # cannot use sync.
        response_content = await self.encode_response(
            time_now, sync_result, requester.access_token_id, filter_collection
        )

        logger.debug("Event formatting complete")
        return 200, response_content

    @trace(opname="sync.encode_response")
    async def encode_response(
        self,
        time_now: int,
        sync_result: SyncResult,
        access_token_id: Optional[int],
        filter: FilterCollection,
    ) -> JsonDict:
        logger.debug("Formatting events in sync response")
        if filter.event_format == "client":
            event_formatter = format_event_for_client_v2_without_room_id
        elif filter.event_format == "federation":
            event_formatter = format_event_raw
        else:
            raise Exception("Unknown event format %s" % (filter.event_format,))

        joined = await self.encode_joined(
            sync_result.joined,
            time_now,
            access_token_id,
            filter.event_fields,
            event_formatter,
        )

        invited = await self.encode_invited(
            sync_result.invited, time_now, access_token_id, event_formatter
        )

        knocked = await self.encode_knocked(
            sync_result.knocked, time_now, access_token_id, event_formatter
        )

        archived = await self.encode_archived(
            sync_result.archived,
            time_now,
            access_token_id,
            filter.event_fields,
            event_formatter,
        )

        logger.debug("building sync response dict")

        response: JsonDict = defaultdict(dict)
        response["next_batch"] = await sync_result.next_batch.to_string(self.store)

        if sync_result.account_data:
            response["account_data"] = {"events": sync_result.account_data}
        if sync_result.presence:
            response["presence"] = SyncRestServlet.encode_presence(
                sync_result.presence, time_now
            )

        if sync_result.to_device:
            response["to_device"] = {"events": sync_result.to_device}

        if sync_result.device_lists.changed:
            response["device_lists"]["changed"] = list(sync_result.device_lists.changed)
        if sync_result.device_lists.left:
            response["device_lists"]["left"] = list(sync_result.device_lists.left)

        # We always include this because https://github.com/vector-im/element-android/issues/3725
        # The spec isn't terribly clear on when this can be omitted and how a client would tell
        # the difference between "no keys present" and "nothing changed" in terms of whole field
        # absent / individual key type entry absent
        # Corresponding synapse issue: https://github.com/matrix-org/synapse/issues/10456
        response["device_one_time_keys_count"] = sync_result.device_one_time_keys_count

        # https://github.com/matrix-org/matrix-doc/blob/54255851f642f84a4f1aaf7bc063eebe3d76752b/proposals/2732-olm-fallback-keys.md
        # states that this field should always be included, as long as the server supports the feature.
        response[
            "org.matrix.msc2732.device_unused_fallback_key_types"
        ] = sync_result.device_unused_fallback_key_types
        response[
            "device_unused_fallback_key_types"
        ] = sync_result.device_unused_fallback_key_types

        if joined:
            response["rooms"][Membership.JOIN] = joined
        if invited:
            response["rooms"][Membership.INVITE] = invited
        if knocked:
            response["rooms"][Membership.KNOCK] = knocked
        if archived:
            response["rooms"][Membership.LEAVE] = archived

        # By the time we get here groups is no longer optional.
        assert sync_result.groups is not None
        if sync_result.groups.join:
            response["groups"][Membership.JOIN] = sync_result.groups.join
        if sync_result.groups.invite:
            response["groups"][Membership.INVITE] = sync_result.groups.invite
        if sync_result.groups.leave:
            response["groups"][Membership.LEAVE] = sync_result.groups.leave

        return response

    @staticmethod
    def encode_presence(events: List[UserPresenceState], time_now: int) -> JsonDict:
        return {
            "events": [
                {
                    "type": "m.presence",
                    "sender": event.user_id,
                    "content": format_user_presence_state(
                        event, time_now, include_user_id=False
                    ),
                }
                for event in events
            ]
        }

    @trace(opname="sync.encode_joined")
    async def encode_joined(
        self,
        rooms: List[JoinedSyncResult],
        time_now: int,
        token_id: Optional[int],
        event_fields: List[str],
        event_formatter: Callable[[JsonDict], JsonDict],
    ) -> JsonDict:
        """
        Encode the joined rooms in a sync result

        Args:
            rooms: list of sync results for rooms this user is joined to
            time_now: current time - used as a baseline for age calculations
            token_id: ID of the user's auth token - used for namespacing
                of transaction IDs
            event_fields: List of event fields to include. If empty,
                all fields will be returned.
            event_formatter: function to convert from federation format
                to client format
        Returns:
            The joined rooms list, in our response format
        """
        joined = {}
        for room in rooms:
            joined[room.room_id] = await self.encode_room(
                room,
                time_now,
                token_id,
                joined=True,
                only_fields=event_fields,
                event_formatter=event_formatter,
            )

        return joined

    @trace(opname="sync.encode_invited")
    async def encode_invited(
        self,
        rooms: List[InvitedSyncResult],
        time_now: int,
        token_id: Optional[int],
        event_formatter: Callable[[JsonDict], JsonDict],
    ) -> JsonDict:
        """
        Encode the invited rooms in a sync result

        Args:
            rooms: list of sync results for rooms this user is invited to
            time_now: current time - used as a baseline for age calculations
            token_id: ID of the user's auth token - used for namespacing
                of transaction IDs
            event_formatter: function to convert from federation format
                to client format

        Returns:
            The invited rooms list, in our response format
        """
        invited = {}
        for room in rooms:
            invite = await self._event_serializer.serialize_event(
                room.invite,
                time_now,
                token_id=token_id,
                event_format=event_formatter,
                include_stripped_room_state=True,
            )
            unsigned = dict(invite.get("unsigned", {}))
            invite["unsigned"] = unsigned
            invited_state = list(unsigned.pop("invite_room_state", []))
            invited_state.append(invite)
            invited[room.room_id] = {"invite_state": {"events": invited_state}}

        return invited

    @trace(opname="sync.encode_knocked")
    async def encode_knocked(
        self,
        rooms: List[KnockedSyncResult],
        time_now: int,
        token_id: Optional[int],
        event_formatter: Callable[[Dict], Dict],
    ) -> Dict[str, Dict[str, Any]]:
        """
        Encode the rooms we've knocked on in a sync result.

        Args:
            rooms: list of sync results for rooms this user is knocking on
            time_now: current time - used as a baseline for age calculations
            token_id: ID of the user's auth token - used for namespacing of transaction IDs
            event_formatter: function to convert from federation format to client format

        Returns:
            The list of rooms the user has knocked on, in our response format.
        """
        knocked = {}
        for room in rooms:
            knock = await self._event_serializer.serialize_event(
                room.knock,
                time_now,
                token_id=token_id,
                event_format=event_formatter,
                include_stripped_room_state=True,
            )

            # Extract the `unsigned` key from the knock event.
            # This is where we (cheekily) store the knock state events
            unsigned = knock.setdefault("unsigned", {})

            # Duplicate the dictionary in order to avoid modifying the original
            unsigned = dict(unsigned)

            # Extract the stripped room state from the unsigned dict
            # This is for clients to get a little bit of information about
            # the room they've knocked on, without revealing any sensitive information
            knocked_state = list(unsigned.pop("knock_room_state", []))

            # Append the actual knock membership event itself as well. This provides
            # the client with:
            #
            # * A knock state event that they can use for easier internal tracking
            # * The rough timestamp of when the knock occurred contained within the event
            knocked_state.append(knock)

            # Build the `knock_state` dictionary, which will contain the state of the
            # room that the client has knocked on
            knocked[room.room_id] = {"knock_state": {"events": knocked_state}}

        return knocked

    @trace(opname="sync.encode_archived")
    async def encode_archived(
        self,
        rooms: List[ArchivedSyncResult],
        time_now: int,
        token_id: Optional[int],
        event_fields: List[str],
        event_formatter: Callable[[JsonDict], JsonDict],
    ) -> JsonDict:
        """
        Encode the archived rooms in a sync result

        Args:
            rooms: list of sync results for rooms this user is joined to
            time_now: current time - used as a baseline for age calculations
            token_id: ID of the user's auth token - used for namespacing
                of transaction IDs
            event_fields: List of event fields to include. If empty,
                all fields will be returned.
            event_formatter: function to convert from federation format to client format
        Returns:
            The archived rooms list, in our response format
        """
        joined = {}
        for room in rooms:
            joined[room.room_id] = await self.encode_room(
                room,
                time_now,
                token_id,
                joined=False,
                only_fields=event_fields,
                event_formatter=event_formatter,
            )

        return joined

    async def encode_room(
        self,
        room: Union[JoinedSyncResult, ArchivedSyncResult],
        time_now: int,
        token_id: Optional[int],
        joined: bool,
        only_fields: Optional[List[str]],
        event_formatter: Callable[[JsonDict], JsonDict],
    ) -> JsonDict:
        """
        Args:
            room: sync result for a single room
            time_now: current time - used as a baseline for age calculations
            token_id: ID of the user's auth token - used for namespacing
                of transaction IDs
            joined: True if the user is joined to this room - will mean
                we handle ephemeral events
            only_fields: Optional. The list of event fields to include.
            event_formatter: function to convert from federation format
                to client format
        Returns:
            The room, encoded in our response format
        """

        def serialize(events: Iterable[EventBase]) -> Awaitable[List[JsonDict]]:
            return self._event_serializer.serialize_events(
                events,
                time_now=time_now,
                # Don't bother to bundle aggregations if the timeline is unlimited,
                # as clients will have all the necessary information.
                # bundle_aggregations=room.timeline.limited,
                #
                # richvdh 2021-12-15: disable this temporarily as it has too high an
                # overhead for initialsyncs. We need to figure out a way that the
                # bundling can be done *before* the events are stored in the
                # SyncResponseCache so that this part can be synchronous.
                #
                # Ensure to re-enable the test at tests/rest/client/test_relations.py::RelationsTestCase.test_bundled_aggregations.
                bundle_aggregations=False,
                token_id=token_id,
                event_format=event_formatter,
                only_event_fields=only_fields,
            )

        state_dict = room.state
        timeline_events = room.timeline.events

        state_events = state_dict.values()

        for event in itertools.chain(state_events, timeline_events):
            # We've had bug reports that events were coming down under the
            # wrong room.
            if event.room_id != room.room_id:
                logger.warning(
                    "Event %r is under room %r instead of %r",
                    event.event_id,
                    room.room_id,
                    event.room_id,
                )

        serialized_state = await serialize(state_events)
        serialized_timeline = await serialize(timeline_events)

        account_data = room.account_data

        result: JsonDict = {
            "timeline": {
                "events": serialized_timeline,
                "prev_batch": await room.timeline.prev_batch.to_string(self.store),
                "limited": room.timeline.limited,
            },
            "state": {"events": serialized_state},
            "account_data": {"events": account_data},
        }

        if joined:
            assert isinstance(room, JoinedSyncResult)
            ephemeral_events = room.ephemeral
            result["ephemeral"] = {"events": ephemeral_events}
            result["unread_notifications"] = room.unread_notifications
            result["summary"] = room.summary
            result["org.matrix.msc2654.unread_count"] = room.unread_count

        return result


def register_servlets(hs: "HomeServer", http_server: HttpServer) -> None:
    SyncRestServlet(hs).register(http_server)
