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
import collections.abc
import re
from typing import (
    TYPE_CHECKING,
    Any,
    Callable,
    Dict,
    Iterable,
    List,
    Mapping,
    Optional,
    Union,
)

from frozendict import frozendict

from synapse.api.constants import EventContentFields, EventTypes, RelationTypes
from synapse.api.errors import Codes, SynapseError
from synapse.api.room_versions import RoomVersion
from synapse.types import JsonDict
from synapse.util.async_helpers import yieldable_gather_results
from synapse.util.frozenutils import unfreeze

from . import EventBase

if TYPE_CHECKING:
    from synapse.server import HomeServer

# Split strings on "." but not "\." This uses a negative lookbehind assertion for '\'
# (?<!stuff) matches if the current position in the string is not preceded
# by a match for 'stuff'.
# TODO: This is fast, but fails to handle "foo\\.bar" which should be treated as
#       the literal fields "foo\" and "bar" but will instead be treated as "foo\\.bar"
SPLIT_FIELD_REGEX = re.compile(r"(?<!\\)\.")

CANONICALJSON_MAX_INT = (2 ** 53) - 1
CANONICALJSON_MIN_INT = -CANONICALJSON_MAX_INT


def prune_event(event: EventBase) -> EventBase:
    """Returns a pruned version of the given event, which removes all keys we
    don't know about or think could potentially be dodgy.

    This is used when we "redact" an event. We want to remove all fields that
    the user has specified, but we do want to keep necessary information like
    type, state_key etc.
    """
    pruned_event_dict = prune_event_dict(event.room_version, event.get_dict())

    from . import make_event_from_dict

    pruned_event = make_event_from_dict(
        pruned_event_dict, event.room_version, event.internal_metadata.get_dict()
    )

    # copy the internal fields
    pruned_event.internal_metadata.stream_ordering = (
        event.internal_metadata.stream_ordering
    )

    pruned_event.internal_metadata.outlier = event.internal_metadata.outlier

    # Mark the event as redacted
    pruned_event.internal_metadata.redacted = True

    return pruned_event


def prune_event_dict(room_version: RoomVersion, event_dict: JsonDict) -> JsonDict:
    """Redacts the event_dict in the same way as `prune_event`, except it
    operates on dicts rather than event objects

    Returns:
        A copy of the pruned event dict
    """

    allowed_keys = [
        "event_id",
        "sender",
        "room_id",
        "hashes",
        "signatures",
        "content",
        "type",
        "state_key",
        "depth",
        "prev_events",
        "auth_events",
        "origin",
        "origin_server_ts",
    ]

    # Room versions from before MSC2176 had additional allowed keys.
    if not room_version.msc2176_redaction_rules:
        allowed_keys.extend(["prev_state", "membership"])

    event_type = event_dict["type"]

    new_content = {}

    def add_fields(*fields: str) -> None:
        for field in fields:
            if field in event_dict["content"]:
                new_content[field] = event_dict["content"][field]

    if event_type == EventTypes.Member:
        add_fields("membership")
        if room_version.msc3375_redaction_rules:
            add_fields(EventContentFields.AUTHORISING_USER)
    elif event_type == EventTypes.Create:
        # MSC2176 rules state that create events cannot be redacted.
        if room_version.msc2176_redaction_rules:
            return event_dict

        add_fields("creator")
    elif event_type == EventTypes.JoinRules:
        add_fields("join_rule")
        if room_version.msc3083_join_rules:
            add_fields("allow")
    elif event_type == EventTypes.PowerLevels:
        add_fields(
            "users",
            "users_default",
            "events",
            "events_default",
            "state_default",
            "ban",
            "kick",
            "redact",
        )

        if room_version.msc2176_redaction_rules:
            add_fields("invite")

        if room_version.msc2716_historical:
            add_fields("historical")

    elif event_type == EventTypes.Aliases and room_version.special_case_aliases_auth:
        add_fields("aliases")
    elif event_type == EventTypes.RoomHistoryVisibility:
        add_fields("history_visibility")
    elif event_type == EventTypes.Redaction and room_version.msc2176_redaction_rules:
        add_fields("redacts")
    elif room_version.msc2716_redactions and event_type == EventTypes.MSC2716_INSERTION:
        add_fields(EventContentFields.MSC2716_NEXT_BATCH_ID)
    elif room_version.msc2716_redactions and event_type == EventTypes.MSC2716_BATCH:
        add_fields(EventContentFields.MSC2716_BATCH_ID)
    elif room_version.msc2716_redactions and event_type == EventTypes.MSC2716_MARKER:
        add_fields(EventContentFields.MSC2716_MARKER_INSERTION)

    allowed_fields = {k: v for k, v in event_dict.items() if k in allowed_keys}

    allowed_fields["content"] = new_content

    unsigned: JsonDict = {}
    allowed_fields["unsigned"] = unsigned

    event_unsigned = event_dict.get("unsigned", {})

    if "age_ts" in event_unsigned:
        unsigned["age_ts"] = event_unsigned["age_ts"]
    if "replaces_state" in event_unsigned:
        unsigned["replaces_state"] = event_unsigned["replaces_state"]

    return allowed_fields


def _copy_field(src: JsonDict, dst: JsonDict, field: List[str]) -> None:
    """Copy the field in 'src' to 'dst'.

    For example, if src={"foo":{"bar":5}} and dst={}, and field=["foo","bar"]
    then dst={"foo":{"bar":5}}.

    Args:
        src: The dict to read from.
        dst: The dict to modify.
        field: List of keys to drill down to in 'src'.
    """
    if len(field) == 0:  # this should be impossible
        return
    if len(field) == 1:  # common case e.g. 'origin_server_ts'
        if field[0] in src:
            dst[field[0]] = src[field[0]]
        return

    # Else is a nested field e.g. 'content.body'
    # Pop the last field as that's the key to move across and we need the
    # parent dict in order to access the data. Drill down to the right dict.
    key_to_move = field.pop(-1)
    sub_dict = src
    for sub_field in field:  # e.g. sub_field => "content"
        if sub_field in sub_dict and type(sub_dict[sub_field]) in [dict, frozendict]:
            sub_dict = sub_dict[sub_field]
        else:
            return

    if key_to_move not in sub_dict:
        return

    # Insert the key into the output dictionary, creating nested objects
    # as required. We couldn't do this any earlier or else we'd need to delete
    # the empty objects if the key didn't exist.
    sub_out_dict = dst
    for sub_field in field:
        sub_out_dict = sub_out_dict.setdefault(sub_field, {})
    sub_out_dict[key_to_move] = sub_dict[key_to_move]


def only_fields(dictionary: JsonDict, fields: List[str]) -> JsonDict:
    """Return a new dict with only the fields in 'dictionary' which are present
    in 'fields'.

    If there are no event fields specified then all fields are included.
    The entries may include '.' characters to indicate sub-fields.
    So ['content.body'] will include the 'body' field of the 'content' object.
    A literal '.' character in a field name may be escaped using a '\'.

    Args:
        dictionary: The dictionary to read from.
        fields: A list of fields to copy over. Only shallow refs are
        taken.
    Returns:
        A new dictionary with only the given fields. If fields was empty,
        the same dictionary is returned.
    """
    if len(fields) == 0:
        return dictionary

    # for each field, convert it:
    # ["content.body.thing\.with\.dots"] => [["content", "body", "thing\.with\.dots"]]
    split_fields = [SPLIT_FIELD_REGEX.split(f) for f in fields]

    # for each element of the output array of arrays:
    # remove escaping so we can use the right key names.
    split_fields[:] = [
        [f.replace(r"\.", r".") for f in field_array] for field_array in split_fields
    ]

    output: JsonDict = {}
    for field_array in split_fields:
        _copy_field(dictionary, output, field_array)
    return output


def format_event_raw(d: JsonDict) -> JsonDict:
    return d


def format_event_for_client_v1(d: JsonDict) -> JsonDict:
    d = format_event_for_client_v2(d)

    sender = d.get("sender")
    if sender is not None:
        d["user_id"] = sender

    copy_keys = (
        "age",
        "redacted_because",
        "replaces_state",
        "prev_content",
        "invite_room_state",
        "knock_room_state",
    )
    for key in copy_keys:
        if key in d["unsigned"]:
            d[key] = d["unsigned"][key]

    return d


def format_event_for_client_v2(d: JsonDict) -> JsonDict:
    drop_keys = (
        "auth_events",
        "prev_events",
        "hashes",
        "signatures",
        "depth",
        "origin",
        "prev_state",
    )
    for key in drop_keys:
        d.pop(key, None)
    return d


def format_event_for_client_v2_without_room_id(d: JsonDict) -> JsonDict:
    d = format_event_for_client_v2(d)
    d.pop("room_id", None)
    return d


def serialize_event(
    e: Union[JsonDict, EventBase],
    time_now_ms: int,
    *,
    as_client_event: bool = True,
    event_format: Callable[[JsonDict], JsonDict] = format_event_for_client_v1,
    token_id: Optional[str] = None,
    only_event_fields: Optional[List[str]] = None,
    include_stripped_room_state: bool = False,
) -> JsonDict:
    """Serialize event for clients

    Args:
        e
        time_now_ms
        as_client_event
        event_format
        token_id
        only_event_fields
        include_stripped_room_state: Some events can have stripped room state
            stored in the `unsigned` field. This is required for invite and knock
            functionality. If this option is False, that state will be removed from the
            event before it is returned. Otherwise, it will be kept.

    Returns:
        The serialized event dictionary.
    """

    # FIXME(erikj): To handle the case of presence events and the like
    if not isinstance(e, EventBase):
        return e

    time_now_ms = int(time_now_ms)

    # Should this strip out None's?
    d = {k: v for k, v in e.get_dict().items()}

    d["event_id"] = e.event_id

    if "age_ts" in d["unsigned"]:
        d["unsigned"]["age"] = time_now_ms - d["unsigned"]["age_ts"]
        del d["unsigned"]["age_ts"]

    if "redacted_because" in e.unsigned:
        d["unsigned"]["redacted_because"] = serialize_event(
            e.unsigned["redacted_because"], time_now_ms, event_format=event_format
        )

    if token_id is not None:
        if token_id == getattr(e.internal_metadata, "token_id", None):
            txn_id = getattr(e.internal_metadata, "txn_id", None)
            if txn_id is not None:
                d["unsigned"]["transaction_id"] = txn_id

    # invite_room_state and knock_room_state are a list of stripped room state events
    # that are meant to provide metadata about a room to an invitee/knocker. They are
    # intended to only be included in specific circumstances, such as down sync, and
    # should not be included in any other case.
    if not include_stripped_room_state:
        d["unsigned"].pop("invite_room_state", None)
        d["unsigned"].pop("knock_room_state", None)

    if as_client_event:
        d = event_format(d)

    if only_event_fields:
        if not isinstance(only_event_fields, list) or not all(
            isinstance(f, str) for f in only_event_fields
        ):
            raise TypeError("only_event_fields must be a list of strings")
        d = only_fields(d, only_event_fields)

    return d


class EventClientSerializer:
    """Serializes events that are to be sent to clients.

    This is used for bundling extra information with any events to be sent to
    clients.
    """

    def __init__(self, hs: "HomeServer"):
        self.store = hs.get_datastore()
        self._msc1849_enabled = hs.config.experimental.msc1849_enabled
        self._msc3440_enabled = hs.config.experimental.msc3440_enabled

    async def serialize_event(
        self,
        event: Union[JsonDict, EventBase],
        time_now: int,
        *,
        bundle_aggregations: bool = False,
        **kwargs: Any,
    ) -> JsonDict:
        """Serializes a single event.

        Args:
            event: The event being serialized.
            time_now: The current time in milliseconds
            bundle_aggregations: Whether to include the bundled aggregations for this
                event. Only applies to non-state events. (State events never include
                bundled aggregations.)
            **kwargs: Arguments to pass to `serialize_event`

        Returns:
            The serialized event
        """
        # To handle the case of presence events and the like
        if not isinstance(event, EventBase):
            return event

        serialized_event = serialize_event(event, time_now, **kwargs)

        # Check if there are any bundled aggregations to include with the event.
        #
        # Do not bundle aggregations if any of the following at true:
        #
        # * Support is disabled via the configuration or the caller.
        # * The event is a state event.
        # * The event has been redacted.
        if (
            self._msc1849_enabled
            and bundle_aggregations
            and not event.is_state()
            and not event.internal_metadata.is_redacted()
        ):
            await self._injected_bundled_aggregations(event, time_now, serialized_event)

        return serialized_event

    async def _injected_bundled_aggregations(
        self, event: EventBase, time_now: int, serialized_event: JsonDict
    ) -> None:
        """Potentially injects bundled aggregations into the unsigned portion of the serialized event.

        Args:
            event: The event being serialized.
            time_now: The current time in milliseconds
            serialized_event: The serialized event which may be modified.

        """
        # Do not bundle aggregations for an event which represents an edit or an
        # annotation. It does not make sense for them to have related events.
        relates_to = event.content.get("m.relates_to")
        if isinstance(relates_to, (dict, frozendict)):
            relation_type = relates_to.get("rel_type")
            if relation_type in (RelationTypes.ANNOTATION, RelationTypes.REPLACE):
                return

        event_id = event.event_id
        room_id = event.room_id

        # The bundled aggregations to include.
        aggregations = {}

        annotations = await self.store.get_aggregation_groups_for_event(
            event_id, room_id
        )
        if annotations.chunk:
            aggregations[RelationTypes.ANNOTATION] = annotations.to_dict()

        references = await self.store.get_relations_for_event(
            event_id, room_id, RelationTypes.REFERENCE, direction="f"
        )
        if references.chunk:
            aggregations[RelationTypes.REFERENCE] = references.to_dict()

        edit = None
        if event.type == EventTypes.Message:
            edit = await self.store.get_applicable_edit(event_id, room_id)

        if edit:
            # If there is an edit replace the content, preserving existing
            # relations.

            # Ensure we take copies of the edit content, otherwise we risk modifying
            # the original event.
            edit_content = edit.content.copy()

            # Unfreeze the event content if necessary, so that we may modify it below
            edit_content = unfreeze(edit_content)
            serialized_event["content"] = edit_content.get("m.new_content", {})

            # Check for existing relations
            relates_to = event.content.get("m.relates_to")
            if relates_to:
                # Keep the relations, ensuring we use a dict copy of the original
                serialized_event["content"]["m.relates_to"] = relates_to.copy()
            else:
                serialized_event["content"].pop("m.relates_to", None)

            aggregations[RelationTypes.REPLACE] = {
                "event_id": edit.event_id,
                "origin_server_ts": edit.origin_server_ts,
                "sender": edit.sender,
            }

        # If this event is the start of a thread, include a summary of the replies.
        if self._msc3440_enabled:
            (
                thread_count,
                latest_thread_event,
            ) = await self.store.get_thread_summary(event_id, room_id)
            if latest_thread_event:
                aggregations[RelationTypes.THREAD] = {
                    # Don't bundle aggregations as this could recurse forever.
                    "latest_event": await self.serialize_event(
                        latest_thread_event, time_now, bundle_aggregations=False
                    ),
                    "count": thread_count,
                }

        # If any bundled aggregations were found, include them.
        if aggregations:
            serialized_event["unsigned"].setdefault("m.relations", {}).update(
                aggregations
            )

    async def serialize_events(
        self, events: Iterable[Union[JsonDict, EventBase]], time_now: int, **kwargs: Any
    ) -> List[JsonDict]:
        """Serializes multiple events.

        Args:
            event
            time_now: The current time in milliseconds
            **kwargs: Arguments to pass to `serialize_event`

        Returns:
            The list of serialized events
        """
        return await yieldable_gather_results(
            self.serialize_event, events, time_now=time_now, **kwargs
        )


def copy_power_levels_contents(
    old_power_levels: Mapping[str, Union[int, Mapping[str, int]]]
) -> Dict[str, Union[int, Dict[str, int]]]:
    """Copy the content of a power_levels event, unfreezing frozendicts along the way

    Raises:
        TypeError if the input does not look like a valid power levels event content
    """
    if not isinstance(old_power_levels, collections.abc.Mapping):
        raise TypeError("Not a valid power-levels content: %r" % (old_power_levels,))

    power_levels: Dict[str, Union[int, Dict[str, int]]] = {}
    for k, v in old_power_levels.items():

        if isinstance(v, int):
            power_levels[k] = v
            continue

        if isinstance(v, collections.abc.Mapping):
            h: Dict[str, int] = {}
            power_levels[k] = h
            for k1, v1 in v.items():
                # we should only have one level of nesting
                if not isinstance(v1, int):
                    raise TypeError(
                        "Invalid power_levels value for %s.%s: %r" % (k, k1, v1)
                    )
                h[k1] = v1
            continue

        raise TypeError("Invalid power_levels value for %s: %r" % (k, v))

    return power_levels


def validate_canonicaljson(value: Any) -> None:
    """
    Ensure that the JSON object is valid according to the rules of canonical JSON.

    See the appendix section 3.1: Canonical JSON.

    This rejects JSON that has:
    * An integer outside the range of [-2 ^ 53 + 1, 2 ^ 53 - 1]
    * Floats
    * NaN, Infinity, -Infinity
    """
    if isinstance(value, int):
        if value < CANONICALJSON_MIN_INT or CANONICALJSON_MAX_INT < value:
            raise SynapseError(400, "JSON integer out of range", Codes.BAD_JSON)

    elif isinstance(value, float):
        # Note that Infinity, -Infinity, and NaN are also considered floats.
        raise SynapseError(400, "Bad JSON value: float", Codes.BAD_JSON)

    elif isinstance(value, (dict, frozendict)):
        for v in value.values():
            validate_canonicaljson(v)

    elif isinstance(value, (list, tuple)):
        for i in value:
            validate_canonicaljson(i)

    elif not isinstance(value, (bool, str)) and value is not None:
        # Other potential JSON values (bool, None, str) are safe.
        raise SynapseError(400, "Unknown JSON value", Codes.BAD_JSON)
