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

from unittest.mock import Mock

from twisted.internet import defer

from synapse.handlers.appservice import ApplicationServicesHandler
from synapse.types import RoomStreamToken

from tests.test_utils import make_awaitable
from tests.utils import MockClock

from .. import unittest


class AppServiceHandlerTestCase(unittest.TestCase):
    """Tests the ApplicationServicesHandler."""

    def setUp(self):
        self.mock_store = Mock()
        self.mock_as_api = Mock()
        self.mock_scheduler = Mock()
        hs = Mock()
        hs.get_datastore.return_value = self.mock_store
        self.mock_store.get_received_ts.return_value = make_awaitable(0)
        self.mock_store.set_appservice_last_pos.return_value = make_awaitable(None)
        hs.get_application_service_api.return_value = self.mock_as_api
        hs.get_application_service_scheduler.return_value = self.mock_scheduler
        hs.get_clock.return_value = MockClock()
        self.handler = ApplicationServicesHandler(hs)
        self.event_source = hs.get_event_sources()

    def test_notify_interested_services(self):
        interested_service = self._mkservice(is_interested=True)
        services = [
            self._mkservice(is_interested=False),
            interested_service,
            self._mkservice(is_interested=False),
        ]

        self.mock_as_api.query_user.return_value = make_awaitable(True)
        self.mock_store.get_app_services.return_value = services
        self.mock_store.get_user_by_id.return_value = make_awaitable([])

        event = Mock(
            sender="@someone:anywhere", type="m.room.message", room_id="!foo:bar"
        )
        self.mock_store.get_new_events_for_appservice.side_effect = [
            make_awaitable((0, [])),
            make_awaitable((1, [event])),
        ]
        self.handler.notify_interested_services(RoomStreamToken(None, 1))

        self.mock_scheduler.submit_event_for_as.assert_called_once_with(
            interested_service, event
        )

    def test_query_user_exists_unknown_user(self):
        user_id = "@someone:anywhere"
        services = [self._mkservice(is_interested=True)]
        services[0].is_interested_in_user.return_value = True
        self.mock_store.get_app_services.return_value = services
        self.mock_store.get_user_by_id.return_value = make_awaitable(None)

        event = Mock(sender=user_id, type="m.room.message", room_id="!foo:bar")
        self.mock_as_api.query_user.return_value = make_awaitable(True)
        self.mock_store.get_new_events_for_appservice.side_effect = [
            make_awaitable((0, [event])),
        ]

        self.handler.notify_interested_services(RoomStreamToken(None, 0))

        self.mock_as_api.query_user.assert_called_once_with(services[0], user_id)

    def test_query_user_exists_known_user(self):
        user_id = "@someone:anywhere"
        services = [self._mkservice(is_interested=True)]
        services[0].is_interested_in_user.return_value = True
        self.mock_store.get_app_services.return_value = services
        self.mock_store.get_user_by_id.return_value = make_awaitable({"name": user_id})

        event = Mock(sender=user_id, type="m.room.message", room_id="!foo:bar")
        self.mock_as_api.query_user.return_value = make_awaitable(True)
        self.mock_store.get_new_events_for_appservice.side_effect = [
            make_awaitable((0, [event])),
        ]

        self.handler.notify_interested_services(RoomStreamToken(None, 0))

        self.assertFalse(
            self.mock_as_api.query_user.called,
            "query_user called when it shouldn't have been.",
        )

    def test_query_room_alias_exists(self):
        room_alias_str = "#foo:bar"
        room_alias = Mock()
        room_alias.to_string.return_value = room_alias_str

        room_id = "!alpha:bet"
        servers = ["aperture"]
        interested_service = self._mkservice_alias(is_interested_in_alias=True)
        services = [
            self._mkservice_alias(is_interested_in_alias=False),
            interested_service,
            self._mkservice_alias(is_interested_in_alias=False),
        ]

        self.mock_as_api.query_alias.return_value = make_awaitable(True)
        self.mock_store.get_app_services.return_value = services
        self.mock_store.get_association_from_room_alias.return_value = make_awaitable(
            Mock(room_id=room_id, servers=servers)
        )

        result = self.successResultOf(
            defer.ensureDeferred(self.handler.query_room_alias_exists(room_alias))
        )

        self.mock_as_api.query_alias.assert_called_once_with(
            interested_service, room_alias_str
        )
        self.assertEquals(result.room_id, room_id)
        self.assertEquals(result.servers, servers)

    def test_get_3pe_protocols_no_appservices(self):
        self.mock_store.get_app_services.return_value = []
        response = self.successResultOf(
            defer.ensureDeferred(self.handler.get_3pe_protocols("my-protocol"))
        )
        self.mock_as_api.get_3pe_protocol.assert_not_called()
        self.assertEquals(response, {})

    def test_get_3pe_protocols_no_protocols(self):
        service = self._mkservice(False, [])
        self.mock_store.get_app_services.return_value = [service]
        response = self.successResultOf(
            defer.ensureDeferred(self.handler.get_3pe_protocols())
        )
        self.mock_as_api.get_3pe_protocol.assert_not_called()
        self.assertEquals(response, {})

    def test_get_3pe_protocols_protocol_no_response(self):
        service = self._mkservice(False, ["my-protocol"])
        self.mock_store.get_app_services.return_value = [service]
        self.mock_as_api.get_3pe_protocol.return_value = make_awaitable(None)
        response = self.successResultOf(
            defer.ensureDeferred(self.handler.get_3pe_protocols())
        )
        self.mock_as_api.get_3pe_protocol.assert_called_once_with(
            service, "my-protocol"
        )
        self.assertEquals(response, {})

    def test_get_3pe_protocols_select_one_protocol(self):
        service = self._mkservice(False, ["my-protocol"])
        self.mock_store.get_app_services.return_value = [service]
        self.mock_as_api.get_3pe_protocol.return_value = make_awaitable(
            {"x-protocol-data": 42, "instances": []}
        )
        response = self.successResultOf(
            defer.ensureDeferred(self.handler.get_3pe_protocols("my-protocol"))
        )
        self.mock_as_api.get_3pe_protocol.assert_called_once_with(
            service, "my-protocol"
        )
        self.assertEquals(
            response, {"my-protocol": {"x-protocol-data": 42, "instances": []}}
        )

    def test_get_3pe_protocols_one_protocol(self):
        service = self._mkservice(False, ["my-protocol"])
        self.mock_store.get_app_services.return_value = [service]
        self.mock_as_api.get_3pe_protocol.return_value = make_awaitable(
            {"x-protocol-data": 42, "instances": []}
        )
        response = self.successResultOf(
            defer.ensureDeferred(self.handler.get_3pe_protocols())
        )
        self.mock_as_api.get_3pe_protocol.assert_called_once_with(
            service, "my-protocol"
        )
        self.assertEquals(
            response, {"my-protocol": {"x-protocol-data": 42, "instances": []}}
        )

    def test_get_3pe_protocols_multiple_protocol(self):
        service_one = self._mkservice(False, ["my-protocol"])
        service_two = self._mkservice(False, ["other-protocol"])
        self.mock_store.get_app_services.return_value = [service_one, service_two]
        self.mock_as_api.get_3pe_protocol.return_value = make_awaitable(
            {"x-protocol-data": 42, "instances": []}
        )
        response = self.successResultOf(
            defer.ensureDeferred(self.handler.get_3pe_protocols())
        )
        self.mock_as_api.get_3pe_protocol.assert_called()
        self.assertEquals(
            response,
            {
                "my-protocol": {"x-protocol-data": 42, "instances": []},
                "other-protocol": {"x-protocol-data": 42, "instances": []},
            },
        )

    def test_get_3pe_protocols_multiple_info(self):
        service_one = self._mkservice(False, ["my-protocol"])
        service_two = self._mkservice(False, ["my-protocol"])

        async def get_3pe_protocol(service, unusedProtocol):
            if service == service_one:
                return {
                    "x-protocol-data": 42,
                    "instances": [{"desc": "Alice's service"}],
                }
            if service == service_two:
                return {
                    "x-protocol-data": 36,
                    "x-not-used": 45,
                    "instances": [{"desc": "Bob's service"}],
                }
            raise Exception("Unexpected service")

        self.mock_store.get_app_services.return_value = [service_one, service_two]
        self.mock_as_api.get_3pe_protocol = get_3pe_protocol
        response = self.successResultOf(
            defer.ensureDeferred(self.handler.get_3pe_protocols())
        )
        # It's expected that the second service's data doesn't appear in the response
        self.assertEquals(
            response,
            {
                "my-protocol": {
                    "x-protocol-data": 42,
                    "instances": [
                        {
                            "desc": "Alice's service",
                        },
                        {"desc": "Bob's service"},
                    ],
                },
            },
        )

    def test_notify_interested_services_ephemeral(self):
        """
        Test sending ephemeral events to the appservice handler are scheduled
        to be pushed out to interested appservices, and that the stream ID is
        updated accordingly.
        """
        interested_service = self._mkservice(is_interested=True)
        services = [interested_service]

        self.mock_store.get_app_services.return_value = services
        self.mock_store.get_type_stream_id_for_appservice.return_value = make_awaitable(
            579
        )

        event = Mock(event_id="event_1")
        self.event_source.sources.receipt.get_new_events_as.return_value = (
            make_awaitable(([event], None))
        )

        self.handler.notify_interested_services_ephemeral(
            "receipt_key", 580, ["@fakerecipient:example.com"]
        )
        self.mock_scheduler.submit_ephemeral_events_for_as.assert_called_once_with(
            interested_service, [event]
        )
        self.mock_store.set_type_stream_id_for_appservice.assert_called_once_with(
            interested_service,
            "read_receipt",
            580,
        )

    def test_notify_interested_services_ephemeral_out_of_order(self):
        """
        Test sending out of order ephemeral events to the appservice handler
        are ignored.
        """
        interested_service = self._mkservice(is_interested=True)
        services = [interested_service]

        self.mock_store.get_app_services.return_value = services
        self.mock_store.get_type_stream_id_for_appservice.return_value = make_awaitable(
            580
        )

        event = Mock(event_id="event_1")
        self.event_source.sources.receipt.get_new_events_as.return_value = (
            make_awaitable(([event], None))
        )

        self.handler.notify_interested_services_ephemeral(
            "receipt_key", 580, ["@fakerecipient:example.com"]
        )
        self.mock_scheduler.submit_ephemeral_events_for_as.assert_not_called()

    def _mkservice(self, is_interested, protocols=None):
        service = Mock()
        service.is_interested.return_value = make_awaitable(is_interested)
        service.token = "mock_service_token"
        service.url = "mock_service_url"
        service.protocols = protocols
        return service

    def _mkservice_alias(self, is_interested_in_alias):
        service = Mock()
        service.is_interested_in_alias.return_value = is_interested_in_alias
        service.token = "mock_service_token"
        service.url = "mock_service_url"
        return service
