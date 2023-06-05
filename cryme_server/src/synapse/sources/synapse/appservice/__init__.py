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
from enum import Enum
from typing import TYPE_CHECKING, Dict, Iterable, List, Optional, Pattern

import attr
from netaddr import IPSet

from synapse.api.constants import EventTypes
from synapse.events import EventBase
from synapse.types import GroupID, JsonDict, UserID, get_domain_from_id
from synapse.util.caches.descriptors import _CacheContext, cached

if TYPE_CHECKING:
    from synapse.appservice.api import ApplicationServiceApi
    from synapse.storage.databases.main import DataStore

logger = logging.getLogger(__name__)


class ApplicationServiceState(Enum):
    DOWN = "down"
    UP = "up"


@attr.s(slots=True, frozen=True, auto_attribs=True)
class Namespace:
    exclusive: bool
    group_id: Optional[str]
    regex: Pattern[str]


class ApplicationService:
    """Defines an application service. This definition is mostly what is
    provided to the /register AS API.

    Provides methods to check if this service is "interested" in events.
    """

    NS_USERS = "users"
    NS_ALIASES = "aliases"
    NS_ROOMS = "rooms"
    # The ordering here is important as it is used to map database values (which
    # are stored as ints representing the position in this list) to namespace
    # values.
    NS_LIST = [NS_USERS, NS_ALIASES, NS_ROOMS]

    def __init__(
        self,
        token: str,
        hostname: str,
        id: str,
        sender: str,
        url: Optional[str] = None,
        namespaces: Optional[JsonDict] = None,
        hs_token: Optional[str] = None,
        protocols: Optional[Iterable[str]] = None,
        rate_limited: bool = True,
        ip_range_whitelist: Optional[IPSet] = None,
        supports_ephemeral: bool = False,
    ):
        self.token = token
        self.url = (
            url.rstrip("/") if isinstance(url, str) else None
        )  # url must not end with a slash
        self.hs_token = hs_token
        self.sender = sender
        self.server_name = hostname
        self.namespaces = self._check_namespaces(namespaces)
        self.id = id
        self.ip_range_whitelist = ip_range_whitelist
        self.supports_ephemeral = supports_ephemeral

        if "|" in self.id:
            raise Exception("application service ID cannot contain '|' character")

        # .protocols is a publicly visible field
        if protocols:
            self.protocols = set(protocols)
        else:
            self.protocols = set()

        self.rate_limited = rate_limited

    def _check_namespaces(
        self, namespaces: Optional[JsonDict]
    ) -> Dict[str, List[Namespace]]:
        # Sanity check that it is of the form:
        # {
        #   users: [ {regex: "[A-z]+.*", exclusive: true}, ...],
        #   aliases: [ {regex: "[A-z]+.*", exclusive: true}, ...],
        #   rooms: [ {regex: "[A-z]+.*", exclusive: true}, ...],
        # }
        if namespaces is None:
            namespaces = {}

        result: Dict[str, List[Namespace]] = {}

        for ns in ApplicationService.NS_LIST:
            result[ns] = []

            if ns not in namespaces:
                continue

            if not isinstance(namespaces[ns], list):
                raise ValueError("Bad namespace value for '%s'" % ns)
            for regex_obj in namespaces[ns]:
                if not isinstance(regex_obj, dict):
                    raise ValueError("Expected dict regex for ns '%s'" % ns)
                exclusive = regex_obj.get("exclusive")
                if not isinstance(exclusive, bool):
                    raise ValueError("Expected bool for 'exclusive' in ns '%s'" % ns)
                group_id = regex_obj.get("group_id")
                if group_id:
                    if not isinstance(group_id, str):
                        raise ValueError(
                            "Expected string for 'group_id' in ns '%s'" % ns
                        )
                    try:
                        GroupID.from_string(group_id)
                    except Exception:
                        raise ValueError(
                            "Expected valid group ID for 'group_id' in ns '%s'" % ns
                        )

                    if get_domain_from_id(group_id) != self.server_name:
                        raise ValueError(
                            "Expected 'group_id' to be this host in ns '%s'" % ns
                        )

                regex = regex_obj.get("regex")
                if not isinstance(regex, str):
                    raise ValueError("Expected string for 'regex' in ns '%s'" % ns)

                # Pre-compile regex.
                result[ns].append(Namespace(exclusive, group_id, re.compile(regex)))

        return result

    def _matches_regex(
        self, namespace_key: str, test_string: str
    ) -> Optional[Namespace]:
        for namespace in self.namespaces[namespace_key]:
            if namespace.regex.match(test_string):
                return namespace
        return None

    def _is_exclusive(self, namespace_key: str, test_string: str) -> bool:
        namespace = self._matches_regex(namespace_key, test_string)
        if namespace:
            return namespace.exclusive
        return False

    async def _matches_user(
        self, event: Optional[EventBase], store: Optional["DataStore"] = None
    ) -> bool:
        if not event:
            return False

        if self.is_interested_in_user(event.sender):
            return True
        # also check m.room.member state key
        if event.type == EventTypes.Member and self.is_interested_in_user(
            event.state_key
        ):
            return True

        if not store:
            return False

        does_match = await self.matches_user_in_member_list(event.room_id, store)
        return does_match

    @cached(num_args=1, cache_context=True)
    async def matches_user_in_member_list(
        self,
        room_id: str,
        store: "DataStore",
        cache_context: _CacheContext,
    ) -> bool:
        """Check if this service is interested a room based upon it's membership

        Args:
            room_id: The room to check.
            store: The datastore to query.

        Returns:
            True if this service would like to know about this room.
        """
        member_list = await store.get_users_in_room(
            room_id, on_invalidate=cache_context.invalidate
        )

        # check joined member events
        for user_id in member_list:
            if self.is_interested_in_user(user_id):
                return True
        return False

    def _matches_room_id(self, event: EventBase) -> bool:
        if hasattr(event, "room_id"):
            return self.is_interested_in_room(event.room_id)
        return False

    async def _matches_aliases(
        self, event: EventBase, store: Optional["DataStore"] = None
    ) -> bool:
        if not store or not event:
            return False

        alias_list = await store.get_aliases_for_room(event.room_id)
        for alias in alias_list:
            if self.is_interested_in_alias(alias):
                return True
        return False

    async def is_interested(
        self, event: EventBase, store: Optional["DataStore"] = None
    ) -> bool:
        """Check if this service is interested in this event.

        Args:
            event: The event to check.
            store: The datastore to query.

        Returns:
            True if this service would like to know about this event.
        """
        # Do cheap checks first
        if self._matches_room_id(event):
            return True

        # This will check the namespaces first before
        # checking the store, so should be run before _matches_aliases
        if await self._matches_user(event, store):
            return True

        # This will check the store, so should be run last
        if await self._matches_aliases(event, store):
            return True

        return False

    @cached(num_args=1)
    async def is_interested_in_presence(
        self, user_id: UserID, store: "DataStore"
    ) -> bool:
        """Check if this service is interested a user's presence

        Args:
            user_id: The user to check.
            store: The datastore to query.

        Returns:
            True if this service would like to know about presence for this user.
        """
        # Find all the rooms the sender is in
        if self.is_interested_in_user(user_id.to_string()):
            return True
        room_ids = await store.get_rooms_for_user(user_id.to_string())

        # Then find out if the appservice is interested in any of those rooms
        for room_id in room_ids:
            if await self.matches_user_in_member_list(room_id, store):
                return True
        return False

    def is_interested_in_user(self, user_id: str) -> bool:
        return (
            bool(self._matches_regex(ApplicationService.NS_USERS, user_id))
            or user_id == self.sender
        )

    def is_interested_in_alias(self, alias: str) -> bool:
        return bool(self._matches_regex(ApplicationService.NS_ALIASES, alias))

    def is_interested_in_room(self, room_id: str) -> bool:
        return bool(self._matches_regex(ApplicationService.NS_ROOMS, room_id))

    def is_exclusive_user(self, user_id: str) -> bool:
        return (
            self._is_exclusive(ApplicationService.NS_USERS, user_id)
            or user_id == self.sender
        )

    def is_interested_in_protocol(self, protocol: str) -> bool:
        return protocol in self.protocols

    def is_exclusive_alias(self, alias: str) -> bool:
        return self._is_exclusive(ApplicationService.NS_ALIASES, alias)

    def is_exclusive_room(self, room_id: str) -> bool:
        return self._is_exclusive(ApplicationService.NS_ROOMS, room_id)

    def get_exclusive_user_regexes(self) -> List[Pattern[str]]:
        """Get the list of regexes used to determine if a user is exclusively
        registered by the AS
        """
        return [
            namespace.regex
            for namespace in self.namespaces[ApplicationService.NS_USERS]
            if namespace.exclusive
        ]

    def get_groups_for_user(self, user_id: str) -> Iterable[str]:
        """Get the groups that this user is associated with by this AS

        Args:
            user_id: The ID of the user.

        Returns:
            An iterable that yields group_id strings.
        """
        return (
            namespace.group_id
            for namespace in self.namespaces[ApplicationService.NS_USERS]
            if namespace.group_id and namespace.regex.match(user_id)
        )

    def is_rate_limited(self) -> bool:
        return self.rate_limited

    def __str__(self) -> str:
        # copy dictionary and redact token fields so they don't get logged
        dict_copy = self.__dict__.copy()
        dict_copy["token"] = "<redacted>"
        dict_copy["hs_token"] = "<redacted>"
        return "ApplicationService: %s" % (dict_copy,)


class AppServiceTransaction:
    """Represents an application service transaction."""

    def __init__(
        self,
        service: ApplicationService,
        id: int,
        events: List[EventBase],
        ephemeral: List[JsonDict],
    ):
        self.service = service
        self.id = id
        self.events = events
        self.ephemeral = ephemeral

    async def send(self, as_api: "ApplicationServiceApi") -> bool:
        """Sends this transaction using the provided AS API interface.

        Args:
            as_api: The API to use to send.
        Returns:
            True if the transaction was sent.
        """
        return await as_api.push_bulk(
            service=self.service,
            events=self.events,
            ephemeral=self.ephemeral,
            txn_id=self.id,
        )

    async def complete(self, store: "DataStore") -> None:
        """Completes this transaction as successful.

        Marks this transaction ID on the application service and removes the
        transaction contents from the database.

        Args:
            store: The database store to operate on.
        """
        await store.complete_appservice_txn(service=self.service, txn_id=self.id)
