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
# Copyright 2017 Vector Creations Ltd
# Copyright 2018-2019 New Vector Ltd
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

from unittest.mock import patch

import jsonschema

from synapse.api.constants import EventContentFields
from synapse.api.errors import SynapseError
from synapse.api.filtering import Filter
from synapse.events import make_event_from_dict

from tests import unittest

user_localpart = "test_user"


def MockEvent(**kwargs):
    if "event_id" not in kwargs:
        kwargs["event_id"] = "fake_event_id"
    if "type" not in kwargs:
        kwargs["type"] = "fake_type"
    return make_event_from_dict(kwargs)


class FilteringTestCase(unittest.HomeserverTestCase):
    def prepare(self, reactor, clock, hs):
        self.filtering = hs.get_filtering()
        self.datastore = hs.get_datastore()

    def test_errors_on_invalid_filters(self):
        invalid_filters = [
            {"boom": {}},
            {"account_data": "Hello World"},
            {"event_fields": [r"\\foo"]},
            {"room": {"timeline": {"limit": 0}, "state": {"not_bars": ["*"]}}},
            {"event_format": "other"},
            {"room": {"not_rooms": ["#foo:pik-test"]}},
            {"presence": {"senders": ["@bar;pik.test.com"]}},
        ]
        for filter in invalid_filters:
            with self.assertRaises(SynapseError):
                self.filtering.check_valid_filter(filter)

    def test_valid_filters(self):
        valid_filters = [
            {
                "room": {
                    "timeline": {"limit": 20},
                    "state": {"not_types": ["m.room.member"]},
                    "ephemeral": {"limit": 0, "not_types": ["*"]},
                    "include_leave": False,
                    "rooms": ["!dee:pik-test"],
                    "not_rooms": ["!gee:pik-test"],
                    "account_data": {"limit": 0, "types": ["*"]},
                }
            },
            {
                "room": {
                    "state": {
                        "types": ["m.room.*"],
                        "not_rooms": ["!726s6s6q:example.com"],
                    },
                    "timeline": {
                        "limit": 10,
                        "types": ["m.room.message"],
                        "not_rooms": ["!726s6s6q:example.com"],
                        "not_senders": ["@spam:example.com"],
                        "org.matrix.labels": ["#fun"],
                        "org.matrix.not_labels": ["#work"],
                    },
                    "ephemeral": {
                        "types": ["m.receipt", "m.typing"],
                        "not_rooms": ["!726s6s6q:example.com"],
                        "not_senders": ["@spam:example.com"],
                    },
                },
                "presence": {
                    "types": ["m.presence"],
                    "not_senders": ["@alice:example.com"],
                },
                "event_format": "client",
                "event_fields": ["type", "content", "sender"],
            },
            # a single backslash should be permitted (though it is debatable whether
            # it should be permitted before anything other than `.`, and what that
            # actually means)
            #
            # (note that event_fields is implemented in
            # synapse.events.utils.serialize_event, and so whether this actually works
            # is tested elsewhere. We just want to check that it is allowed through the
            # filter validation)
            {"event_fields": [r"foo\.bar"]},
        ]
        for filter in valid_filters:
            try:
                self.filtering.check_valid_filter(filter)
            except jsonschema.ValidationError as e:
                self.fail(e)

    def test_limits_are_applied(self):
        # TODO
        pass

    def test_definition_types_works_with_literals(self):
        definition = {"types": ["m.room.message", "org.matrix.foo.bar"]}
        event = MockEvent(sender="@foo:bar", type="m.room.message", room_id="!foo:bar")

        self.assertTrue(Filter(self.hs, definition)._check(event))

    def test_definition_types_works_with_wildcards(self):
        definition = {"types": ["m.*", "org.matrix.foo.bar"]}
        event = MockEvent(sender="@foo:bar", type="m.room.message", room_id="!foo:bar")
        self.assertTrue(Filter(self.hs, definition)._check(event))

    def test_definition_types_works_with_unknowns(self):
        definition = {"types": ["m.room.message", "org.matrix.foo.bar"]}
        event = MockEvent(
            sender="@foo:bar",
            type="now.for.something.completely.different",
            room_id="!foo:bar",
        )
        self.assertFalse(Filter(self.hs, definition)._check(event))

    def test_definition_not_types_works_with_literals(self):
        definition = {"not_types": ["m.room.message", "org.matrix.foo.bar"]}
        event = MockEvent(sender="@foo:bar", type="m.room.message", room_id="!foo:bar")
        self.assertFalse(Filter(self.hs, definition)._check(event))

    def test_definition_not_types_works_with_wildcards(self):
        definition = {"not_types": ["m.room.message", "org.matrix.*"]}
        event = MockEvent(
            sender="@foo:bar", type="org.matrix.custom.event", room_id="!foo:bar"
        )
        self.assertFalse(Filter(self.hs, definition)._check(event))

    def test_definition_not_types_works_with_unknowns(self):
        definition = {"not_types": ["m.*", "org.*"]}
        event = MockEvent(sender="@foo:bar", type="com.nom.nom.nom", room_id="!foo:bar")
        self.assertTrue(Filter(self.hs, definition)._check(event))

    def test_definition_not_types_takes_priority_over_types(self):
        definition = {
            "not_types": ["m.*", "org.*"],
            "types": ["m.room.message", "m.room.topic"],
        }
        event = MockEvent(sender="@foo:bar", type="m.room.topic", room_id="!foo:bar")
        self.assertFalse(Filter(self.hs, definition)._check(event))

    def test_definition_senders_works_with_literals(self):
        definition = {"senders": ["@flibble:wibble"]}
        event = MockEvent(
            sender="@flibble:wibble", type="com.nom.nom.nom", room_id="!foo:bar"
        )
        self.assertTrue(Filter(self.hs, definition)._check(event))

    def test_definition_senders_works_with_unknowns(self):
        definition = {"senders": ["@flibble:wibble"]}
        event = MockEvent(
            sender="@challenger:appears", type="com.nom.nom.nom", room_id="!foo:bar"
        )
        self.assertFalse(Filter(self.hs, definition)._check(event))

    def test_definition_not_senders_works_with_literals(self):
        definition = {"not_senders": ["@flibble:wibble"]}
        event = MockEvent(
            sender="@flibble:wibble", type="com.nom.nom.nom", room_id="!foo:bar"
        )
        self.assertFalse(Filter(self.hs, definition)._check(event))

    def test_definition_not_senders_works_with_unknowns(self):
        definition = {"not_senders": ["@flibble:wibble"]}
        event = MockEvent(
            sender="@challenger:appears", type="com.nom.nom.nom", room_id="!foo:bar"
        )
        self.assertTrue(Filter(self.hs, definition)._check(event))

    def test_definition_not_senders_takes_priority_over_senders(self):
        definition = {
            "not_senders": ["@misspiggy:muppets"],
            "senders": ["@kermit:muppets", "@misspiggy:muppets"],
        }
        event = MockEvent(
            sender="@misspiggy:muppets", type="m.room.topic", room_id="!foo:bar"
        )
        self.assertFalse(Filter(self.hs, definition)._check(event))

    def test_definition_rooms_works_with_literals(self):
        definition = {"rooms": ["!secretbase:unknown"]}
        event = MockEvent(
            sender="@foo:bar", type="m.room.message", room_id="!secretbase:unknown"
        )
        self.assertTrue(Filter(self.hs, definition)._check(event))

    def test_definition_rooms_works_with_unknowns(self):
        definition = {"rooms": ["!secretbase:unknown"]}
        event = MockEvent(
            sender="@foo:bar",
            type="m.room.message",
            room_id="!anothersecretbase:unknown",
        )
        self.assertFalse(Filter(self.hs, definition)._check(event))

    def test_definition_not_rooms_works_with_literals(self):
        definition = {"not_rooms": ["!anothersecretbase:unknown"]}
        event = MockEvent(
            sender="@foo:bar",
            type="m.room.message",
            room_id="!anothersecretbase:unknown",
        )
        self.assertFalse(Filter(self.hs, definition)._check(event))

    def test_definition_not_rooms_works_with_unknowns(self):
        definition = {"not_rooms": ["!secretbase:unknown"]}
        event = MockEvent(
            sender="@foo:bar",
            type="m.room.message",
            room_id="!anothersecretbase:unknown",
        )
        self.assertTrue(Filter(self.hs, definition)._check(event))

    def test_definition_not_rooms_takes_priority_over_rooms(self):
        definition = {
            "not_rooms": ["!secretbase:unknown"],
            "rooms": ["!secretbase:unknown"],
        }
        event = MockEvent(
            sender="@foo:bar", type="m.room.message", room_id="!secretbase:unknown"
        )
        self.assertFalse(Filter(self.hs, definition)._check(event))

    def test_definition_combined_event(self):
        definition = {
            "not_senders": ["@misspiggy:muppets"],
            "senders": ["@kermit:muppets"],
            "rooms": ["!stage:unknown"],
            "not_rooms": ["!piggyshouse:muppets"],
            "types": ["m.room.message", "muppets.kermit.*"],
            "not_types": ["muppets.misspiggy.*"],
        }
        event = MockEvent(
            sender="@kermit:muppets",  # yup
            type="m.room.message",  # yup
            room_id="!stage:unknown",  # yup
        )
        self.assertTrue(Filter(self.hs, definition)._check(event))

    def test_definition_combined_event_bad_sender(self):
        definition = {
            "not_senders": ["@misspiggy:muppets"],
            "senders": ["@kermit:muppets"],
            "rooms": ["!stage:unknown"],
            "not_rooms": ["!piggyshouse:muppets"],
            "types": ["m.room.message", "muppets.kermit.*"],
            "not_types": ["muppets.misspiggy.*"],
        }
        event = MockEvent(
            sender="@misspiggy:muppets",  # nope
            type="m.room.message",  # yup
            room_id="!stage:unknown",  # yup
        )
        self.assertFalse(Filter(self.hs, definition)._check(event))

    def test_definition_combined_event_bad_room(self):
        definition = {
            "not_senders": ["@misspiggy:muppets"],
            "senders": ["@kermit:muppets"],
            "rooms": ["!stage:unknown"],
            "not_rooms": ["!piggyshouse:muppets"],
            "types": ["m.room.message", "muppets.kermit.*"],
            "not_types": ["muppets.misspiggy.*"],
        }
        event = MockEvent(
            sender="@kermit:muppets",  # yup
            type="m.room.message",  # yup
            room_id="!piggyshouse:muppets",  # nope
        )
        self.assertFalse(Filter(self.hs, definition)._check(event))

    def test_definition_combined_event_bad_type(self):
        definition = {
            "not_senders": ["@misspiggy:muppets"],
            "senders": ["@kermit:muppets"],
            "rooms": ["!stage:unknown"],
            "not_rooms": ["!piggyshouse:muppets"],
            "types": ["m.room.message", "muppets.kermit.*"],
            "not_types": ["muppets.misspiggy.*"],
        }
        event = MockEvent(
            sender="@kermit:muppets",  # yup
            type="muppets.misspiggy.kisses",  # nope
            room_id="!stage:unknown",  # yup
        )
        self.assertFalse(Filter(self.hs, definition)._check(event))

    def test_filter_labels(self):
        definition = {"org.matrix.labels": ["#fun"]}
        event = MockEvent(
            sender="@foo:bar",
            type="m.room.message",
            room_id="!secretbase:unknown",
            content={EventContentFields.LABELS: ["#fun"]},
        )

        self.assertTrue(Filter(self.hs, definition)._check(event))

        event = MockEvent(
            sender="@foo:bar",
            type="m.room.message",
            room_id="!secretbase:unknown",
            content={EventContentFields.LABELS: ["#notfun"]},
        )

        self.assertFalse(Filter(self.hs, definition)._check(event))

    def test_filter_not_labels(self):
        definition = {"org.matrix.not_labels": ["#fun"]}
        event = MockEvent(
            sender="@foo:bar",
            type="m.room.message",
            room_id="!secretbase:unknown",
            content={EventContentFields.LABELS: ["#fun"]},
        )

        self.assertFalse(Filter(self.hs, definition)._check(event))

        event = MockEvent(
            sender="@foo:bar",
            type="m.room.message",
            room_id="!secretbase:unknown",
            content={EventContentFields.LABELS: ["#notfun"]},
        )

        self.assertTrue(Filter(self.hs, definition)._check(event))

    def test_filter_presence_match(self):
        user_filter_json = {"presence": {"types": ["m.*"]}}
        filter_id = self.get_success(
            self.datastore.add_user_filter(
                user_localpart=user_localpart, user_filter=user_filter_json
            )
        )
        event = MockEvent(sender="@foo:bar", type="m.profile")
        events = [event]

        user_filter = self.get_success(
            self.filtering.get_user_filter(
                user_localpart=user_localpart, filter_id=filter_id
            )
        )

        results = self.get_success(user_filter.filter_presence(events=events))
        self.assertEquals(events, results)

    def test_filter_presence_no_match(self):
        user_filter_json = {"presence": {"types": ["m.*"]}}

        filter_id = self.get_success(
            self.datastore.add_user_filter(
                user_localpart=user_localpart + "2", user_filter=user_filter_json
            )
        )
        event = MockEvent(
            event_id="$asdasd:localhost",
            sender="@foo:bar",
            type="custom.avatar.3d.crazy",
        )
        events = [event]

        user_filter = self.get_success(
            self.filtering.get_user_filter(
                user_localpart=user_localpart + "2", filter_id=filter_id
            )
        )

        results = self.get_success(user_filter.filter_presence(events=events))
        self.assertEquals([], results)

    def test_filter_room_state_match(self):
        user_filter_json = {"room": {"state": {"types": ["m.*"]}}}
        filter_id = self.get_success(
            self.datastore.add_user_filter(
                user_localpart=user_localpart, user_filter=user_filter_json
            )
        )
        event = MockEvent(sender="@foo:bar", type="m.room.topic", room_id="!foo:bar")
        events = [event]

        user_filter = self.get_success(
            self.filtering.get_user_filter(
                user_localpart=user_localpart, filter_id=filter_id
            )
        )

        results = self.get_success(user_filter.filter_room_state(events=events))
        self.assertEquals(events, results)

    def test_filter_room_state_no_match(self):
        user_filter_json = {"room": {"state": {"types": ["m.*"]}}}
        filter_id = self.get_success(
            self.datastore.add_user_filter(
                user_localpart=user_localpart, user_filter=user_filter_json
            )
        )
        event = MockEvent(
            sender="@foo:bar", type="org.matrix.custom.event", room_id="!foo:bar"
        )
        events = [event]

        user_filter = self.get_success(
            self.filtering.get_user_filter(
                user_localpart=user_localpart, filter_id=filter_id
            )
        )

        results = self.get_success(user_filter.filter_room_state(events))
        self.assertEquals([], results)

    def test_filter_rooms(self):
        definition = {
            "rooms": ["!allowed:example.com", "!excluded:example.com"],
            "not_rooms": ["!excluded:example.com"],
        }

        room_ids = [
            "!allowed:example.com",  # Allowed because in rooms and not in not_rooms.
            "!excluded:example.com",  # Disallowed because in not_rooms.
            "!not_included:example.com",  # Disallowed because not in rooms.
        ]

        filtered_room_ids = list(Filter(self.hs, definition).filter_rooms(room_ids))

        self.assertEquals(filtered_room_ids, ["!allowed:example.com"])

    @unittest.override_config({"experimental_features": {"msc3440_enabled": True}})
    def test_filter_relations(self):
        events = [
            # An event without a relation.
            MockEvent(
                event_id="$no_relation",
                sender="@foo:bar",
                type="org.matrix.custom.event",
                room_id="!foo:bar",
            ),
            # An event with a relation.
            MockEvent(
                event_id="$with_relation",
                sender="@foo:bar",
                type="org.matrix.custom.event",
                room_id="!foo:bar",
            ),
            # Non-EventBase objects get passed through.
            {},
        ]

        # For the following tests we patch the datastore method (intead of injecting
        # events). This is a bit cheeky, but tests the logic of _check_event_relations.

        # Filter for a particular sender.
        definition = {
            "io.element.relation_senders": ["@foo:bar"],
        }

        async def events_have_relations(*args, **kwargs):
            return ["$with_relation"]

        with patch.object(
            self.datastore, "events_have_relations", new=events_have_relations
        ):
            filtered_events = list(
                self.get_success(
                    Filter(self.hs, definition)._check_event_relations(events)
                )
            )
        self.assertEquals(filtered_events, events[1:])

    def test_add_filter(self):
        user_filter_json = {"room": {"state": {"types": ["m.*"]}}}

        filter_id = self.get_success(
            self.filtering.add_user_filter(
                user_localpart=user_localpart, user_filter=user_filter_json
            )
        )

        self.assertEquals(filter_id, 0)
        self.assertEquals(
            user_filter_json,
            (
                self.get_success(
                    self.datastore.get_user_filter(
                        user_localpart=user_localpart, filter_id=0
                    )
                )
            ),
        )

    def test_get_filter(self):
        user_filter_json = {"room": {"state": {"types": ["m.*"]}}}

        filter_id = self.get_success(
            self.datastore.add_user_filter(
                user_localpart=user_localpart, user_filter=user_filter_json
            )
        )

        filter = self.get_success(
            self.filtering.get_user_filter(
                user_localpart=user_localpart, filter_id=filter_id
            )
        )

        self.assertEquals(filter.get_filter_json(), user_filter_json)

        self.assertRegexpMatches(repr(filter), r"<FilterCollection \{.*\}>")
