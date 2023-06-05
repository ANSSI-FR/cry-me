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
# Copyright 2017 New Vector Ltd
# Copyright 2020 The Matrix.org Foundation C.I.C.
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
import email.utils
import logging
from typing import (
    TYPE_CHECKING,
    Any,
    Callable,
    Dict,
    Generator,
    Iterable,
    List,
    Optional,
    Tuple,
    TypeVar,
    Union,
)

import attr
import jinja2

from twisted.internet import defer
from twisted.web.resource import Resource

from synapse.api.errors import SynapseError
from synapse.events import EventBase
from synapse.events.presence_router import (
    GET_INTERESTED_USERS_CALLBACK,
    GET_USERS_FOR_STATES_CALLBACK,
    PresenceRouter,
)
from synapse.events.spamcheck import (
    CHECK_EVENT_FOR_SPAM_CALLBACK,
    CHECK_MEDIA_FILE_FOR_SPAM_CALLBACK,
    CHECK_REGISTRATION_FOR_SPAM_CALLBACK,
    CHECK_USERNAME_FOR_SPAM_CALLBACK,
    USER_MAY_CREATE_ROOM_ALIAS_CALLBACK,
    USER_MAY_CREATE_ROOM_CALLBACK,
    USER_MAY_CREATE_ROOM_WITH_INVITES_CALLBACK,
    USER_MAY_INVITE_CALLBACK,
    USER_MAY_JOIN_ROOM_CALLBACK,
    USER_MAY_PUBLISH_ROOM_CALLBACK,
    USER_MAY_SEND_3PID_INVITE_CALLBACK,
)
from synapse.events.third_party_rules import (
    CHECK_EVENT_ALLOWED_CALLBACK,
    CHECK_THREEPID_CAN_BE_INVITED_CALLBACK,
    CHECK_VISIBILITY_CAN_BE_MODIFIED_CALLBACK,
    ON_CREATE_ROOM_CALLBACK,
    ON_NEW_EVENT_CALLBACK,
)
from synapse.handlers.account_validity import (
    IS_USER_EXPIRED_CALLBACK,
    ON_LEGACY_ADMIN_REQUEST,
    ON_LEGACY_RENEW_CALLBACK,
    ON_LEGACY_SEND_MAIL_CALLBACK,
    ON_USER_REGISTRATION_CALLBACK,
)
from synapse.handlers.auth import (
    CHECK_3PID_AUTH_CALLBACK,
    CHECK_AUTH_CALLBACK,
    ON_LOGGED_OUT_CALLBACK,
    AuthHandler,
)
from synapse.http.client import SimpleHttpClient
from synapse.http.server import (
    DirectServeHtmlResource,
    DirectServeJsonResource,
    respond_with_html,
)
from synapse.http.servlet import parse_json_object_from_request
from synapse.http.site import SynapseRequest
from synapse.logging.context import (
    defer_to_thread,
    make_deferred_yieldable,
    run_in_background,
)
from synapse.metrics.background_process_metrics import run_as_background_process
from synapse.rest.client.login import LoginResponse
from synapse.storage import DataStore
from synapse.storage.background_updates import (
    DEFAULT_BATCH_SIZE_CALLBACK,
    MIN_BATCH_SIZE_CALLBACK,
    ON_UPDATE_CALLBACK,
)
from synapse.storage.database import DatabasePool, LoggingTransaction
from synapse.storage.databases.main.roommember import ProfileInfo
from synapse.storage.state import StateFilter
from synapse.types import (
    DomainSpecificString,
    JsonDict,
    Requester,
    StateMap,
    UserID,
    UserInfo,
    create_requester,
)
from synapse.util import Clock
from synapse.util.async_helpers import maybe_awaitable
from synapse.util.caches.descriptors import cached

if TYPE_CHECKING:
    from synapse.app.generic_worker import GenericWorkerSlavedStore
    from synapse.server import HomeServer


T = TypeVar("T")

"""
This package defines the 'stable' API which can be used by extension modules which
are loaded into Synapse.
"""

PRESENCE_ALL_USERS = PresenceRouter.ALL_USERS

__all__ = [
    "errors",
    "make_deferred_yieldable",
    "parse_json_object_from_request",
    "respond_with_html",
    "run_in_background",
    "cached",
    "UserID",
    "DatabasePool",
    "LoggingTransaction",
    "DirectServeHtmlResource",
    "DirectServeJsonResource",
    "ModuleApi",
    "PRESENCE_ALL_USERS",
    "LoginResponse",
    "JsonDict",
    "EventBase",
    "StateMap",
]

logger = logging.getLogger(__name__)


@attr.s(auto_attribs=True)
class UserIpAndAgent:
    """
    An IP address and user agent used by a user to connect to this homeserver.
    """

    ip: str
    user_agent: str
    # The time at which this user agent/ip was last seen.
    last_seen: int


class ModuleApi:
    """A proxy object that gets passed to various plugin modules so they
    can register new users etc if necessary.
    """

    def __init__(self, hs: "HomeServer", auth_handler: AuthHandler) -> None:
        self._hs = hs

        # TODO: Fix this type hint once the types for the data stores have been ironed
        #       out.
        self._store: Union[DataStore, "GenericWorkerSlavedStore"] = hs.get_datastore()
        self._auth = hs.get_auth()
        self._auth_handler = auth_handler
        self._server_name = hs.hostname
        self._presence_stream = hs.get_event_sources().sources.presence
        self._state = hs.get_state_handler()
        self._clock: Clock = hs.get_clock()
        self._send_email_handler = hs.get_send_email_handler()
        self.custom_template_dir = hs.config.server.custom_template_directory

        try:
            app_name = self._hs.config.email.email_app_name

            self._from_string = self._hs.config.email.email_notif_from % {
                "app": app_name
            }
        except (KeyError, TypeError):
            # If substitution failed (which can happen if the string contains
            # placeholders other than just "app", or if the type of the placeholder is
            # not a string), fall back to the bare strings.
            self._from_string = self._hs.config.email.email_notif_from

        self._raw_from = email.utils.parseaddr(self._from_string)[1]

        # We expose these as properties below in order to attach a helpful docstring.
        self._http_client: SimpleHttpClient = hs.get_simple_http_client()
        self._public_room_list_manager = PublicRoomListManager(hs)

        self._spam_checker = hs.get_spam_checker()
        self._account_validity_handler = hs.get_account_validity_handler()
        self._third_party_event_rules = hs.get_third_party_event_rules()
        self._password_auth_provider = hs.get_password_auth_provider()
        self._presence_router = hs.get_presence_router()

    #################################################################################
    # The following methods should only be called during the module's initialisation.

    def register_spam_checker_callbacks(
        self,
        check_event_for_spam: Optional[CHECK_EVENT_FOR_SPAM_CALLBACK] = None,
        user_may_join_room: Optional[USER_MAY_JOIN_ROOM_CALLBACK] = None,
        user_may_invite: Optional[USER_MAY_INVITE_CALLBACK] = None,
        user_may_send_3pid_invite: Optional[USER_MAY_SEND_3PID_INVITE_CALLBACK] = None,
        user_may_create_room: Optional[USER_MAY_CREATE_ROOM_CALLBACK] = None,
        user_may_create_room_with_invites: Optional[
            USER_MAY_CREATE_ROOM_WITH_INVITES_CALLBACK
        ] = None,
        user_may_create_room_alias: Optional[
            USER_MAY_CREATE_ROOM_ALIAS_CALLBACK
        ] = None,
        user_may_publish_room: Optional[USER_MAY_PUBLISH_ROOM_CALLBACK] = None,
        check_username_for_spam: Optional[CHECK_USERNAME_FOR_SPAM_CALLBACK] = None,
        check_registration_for_spam: Optional[
            CHECK_REGISTRATION_FOR_SPAM_CALLBACK
        ] = None,
        check_media_file_for_spam: Optional[CHECK_MEDIA_FILE_FOR_SPAM_CALLBACK] = None,
    ) -> None:
        """Registers callbacks for spam checking capabilities.

        Added in Synapse v1.37.0.
        """
        return self._spam_checker.register_callbacks(
            check_event_for_spam=check_event_for_spam,
            user_may_join_room=user_may_join_room,
            user_may_invite=user_may_invite,
            user_may_send_3pid_invite=user_may_send_3pid_invite,
            user_may_create_room=user_may_create_room,
            user_may_create_room_with_invites=user_may_create_room_with_invites,
            user_may_create_room_alias=user_may_create_room_alias,
            user_may_publish_room=user_may_publish_room,
            check_username_for_spam=check_username_for_spam,
            check_registration_for_spam=check_registration_for_spam,
            check_media_file_for_spam=check_media_file_for_spam,
        )

    def register_account_validity_callbacks(
        self,
        is_user_expired: Optional[IS_USER_EXPIRED_CALLBACK] = None,
        on_user_registration: Optional[ON_USER_REGISTRATION_CALLBACK] = None,
        on_legacy_send_mail: Optional[ON_LEGACY_SEND_MAIL_CALLBACK] = None,
        on_legacy_renew: Optional[ON_LEGACY_RENEW_CALLBACK] = None,
        on_legacy_admin_request: Optional[ON_LEGACY_ADMIN_REQUEST] = None,
    ) -> None:
        """Registers callbacks for account validity capabilities.

        Added in Synapse v1.39.0.
        """
        return self._account_validity_handler.register_account_validity_callbacks(
            is_user_expired=is_user_expired,
            on_user_registration=on_user_registration,
            on_legacy_send_mail=on_legacy_send_mail,
            on_legacy_renew=on_legacy_renew,
            on_legacy_admin_request=on_legacy_admin_request,
        )

    def register_third_party_rules_callbacks(
        self,
        check_event_allowed: Optional[CHECK_EVENT_ALLOWED_CALLBACK] = None,
        on_create_room: Optional[ON_CREATE_ROOM_CALLBACK] = None,
        check_threepid_can_be_invited: Optional[
            CHECK_THREEPID_CAN_BE_INVITED_CALLBACK
        ] = None,
        check_visibility_can_be_modified: Optional[
            CHECK_VISIBILITY_CAN_BE_MODIFIED_CALLBACK
        ] = None,
        on_new_event: Optional[ON_NEW_EVENT_CALLBACK] = None,
    ) -> None:
        """Registers callbacks for third party event rules capabilities.

        Added in Synapse v1.39.0.
        """
        return self._third_party_event_rules.register_third_party_rules_callbacks(
            check_event_allowed=check_event_allowed,
            on_create_room=on_create_room,
            check_threepid_can_be_invited=check_threepid_can_be_invited,
            check_visibility_can_be_modified=check_visibility_can_be_modified,
            on_new_event=on_new_event,
        )

    def register_presence_router_callbacks(
        self,
        get_users_for_states: Optional[GET_USERS_FOR_STATES_CALLBACK] = None,
        get_interested_users: Optional[GET_INTERESTED_USERS_CALLBACK] = None,
    ) -> None:
        """Registers callbacks for presence router capabilities.

        Added in Synapse v1.42.0.
        """
        return self._presence_router.register_presence_router_callbacks(
            get_users_for_states=get_users_for_states,
            get_interested_users=get_interested_users,
        )

    def register_password_auth_provider_callbacks(
        self,
        check_3pid_auth: Optional[CHECK_3PID_AUTH_CALLBACK] = None,
        on_logged_out: Optional[ON_LOGGED_OUT_CALLBACK] = None,
        auth_checkers: Optional[
            Dict[Tuple[str, Tuple[str, ...]], CHECK_AUTH_CALLBACK]
        ] = None,
    ) -> None:
        """Registers callbacks for password auth provider capabilities.

        Added in Synapse v1.46.0.
        """
        return self._password_auth_provider.register_password_auth_provider_callbacks(
            check_3pid_auth=check_3pid_auth,
            on_logged_out=on_logged_out,
            auth_checkers=auth_checkers,
        )

    def register_background_update_controller_callbacks(
        self,
        on_update: ON_UPDATE_CALLBACK,
        default_batch_size: Optional[DEFAULT_BATCH_SIZE_CALLBACK] = None,
        min_batch_size: Optional[MIN_BATCH_SIZE_CALLBACK] = None,
    ) -> None:
        """Registers background update controller callbacks.

        Added in Synapse v1.49.0.
        """

        for db in self._hs.get_datastores().databases:
            db.updates.register_update_controller_callbacks(
                on_update=on_update,
                default_batch_size=default_batch_size,
                min_batch_size=min_batch_size,
            )

    def register_web_resource(self, path: str, resource: Resource) -> None:
        """Registers a web resource to be served at the given path.

        This function should be called during initialisation of the module.

        If multiple modules register a resource for the same path, the module that
        appears the highest in the configuration file takes priority.

        Added in Synapse v1.37.0.

        Args:
            path: The path to register the resource for.
            resource: The resource to attach to this path.
        """
        self._hs.register_module_web_resource(path, resource)

    #########################################################################
    # The following methods can be called by the module at any point in time.

    @property
    def http_client(self) -> SimpleHttpClient:
        """Allows making outbound HTTP requests to remote resources.

        An instance of synapse.http.client.SimpleHttpClient

        Added in Synapse v1.22.0.
        """
        return self._http_client

    @property
    def public_room_list_manager(self) -> "PublicRoomListManager":
        """Allows adding to, removing from and checking the status of rooms in the
        public room list.

        An instance of synapse.module_api.PublicRoomListManager

        Added in Synapse v1.22.0.
        """
        return self._public_room_list_manager

    @property
    def public_baseurl(self) -> str:
        """The configured public base URL for this homeserver.

        Added in Synapse v1.39.0.
        """
        return self._hs.config.server.public_baseurl

    @property
    def email_app_name(self) -> str:
        """The application name configured in the homeserver's configuration.

        Added in Synapse v1.39.0.
        """
        return self._hs.config.email.email_app_name

    async def get_userinfo_by_id(self, user_id: str) -> Optional[UserInfo]:
        """Get user info by user_id

        Added in Synapse v1.41.0.

        Args:
            user_id: Fully qualified user id.
        Returns:
            UserInfo object if a user was found, otherwise None
        """
        return await self._store.get_userinfo_by_id(user_id)

    async def get_user_by_req(
        self,
        req: SynapseRequest,
        allow_guest: bool = False,
        allow_expired: bool = False,
    ) -> Requester:
        """Check the access_token provided for a request

        Added in Synapse v1.39.0.

        Args:
            req: Incoming HTTP request
            allow_guest: True if guest users should be allowed. If this
                is False, and the access token is for a guest user, an
                AuthError will be thrown
            allow_expired: True if expired users should be allowed. If this
                is False, and the access token is for an expired user, an
                AuthError will be thrown

        Returns:
            The requester for this request

        Raises:
            InvalidClientCredentialsError: if no user by that token exists,
                or the token is invalid.
        """
        return await self._auth.get_user_by_req(
            req,
            allow_guest,
            allow_expired=allow_expired,
        )

    async def is_user_admin(self, user_id: str) -> bool:
        """Checks if a user is a server admin.

        Added in Synapse v1.39.0.

        Args:
            user_id: The Matrix ID of the user to check.

        Returns:
            True if the user is a server admin, False otherwise.
        """
        return await self._store.is_server_admin(UserID.from_string(user_id))

    def get_qualified_user_id(self, username: str) -> str:
        """Qualify a user id, if necessary

        Takes a user id provided by the user and adds the @ and :domain to
        qualify it, if necessary

        Added in Synapse v0.25.0.

        Args:
            username: provided user id

        Returns:
            qualified @user:id
        """
        if username.startswith("@"):
            return username
        return UserID(username, self._hs.hostname).to_string()

    async def get_profile_for_user(self, localpart: str) -> ProfileInfo:
        """Look up the profile info for the user with the given localpart.

        Added in Synapse v1.39.0.

        Args:
            localpart: The localpart to look up profile information for.

        Returns:
            The profile information (i.e. display name and avatar URL).
        """
        return await self._store.get_profileinfo(localpart)

    async def get_threepids_for_user(self, user_id: str) -> List[Dict[str, str]]:
        """Look up the threepids (email addresses and phone numbers) associated with the
        given Matrix user ID.

        Added in Synapse v1.39.0.

        Args:
            user_id: The Matrix user ID to look up threepids for.

        Returns:
            A list of threepids, each threepid being represented by a dictionary
            containing a "medium" key which value is "email" for email addresses and
            "msisdn" for phone numbers, and an "address" key which value is the
            threepid's address.
        """
        return await self._store.user_get_threepids(user_id)

    def check_user_exists(self, user_id: str) -> "defer.Deferred[Optional[str]]":
        """Check if user exists.

        Added in Synapse v0.25.0.

        Args:
            user_id: Complete @user:id

        Returns:
            Canonical (case-corrected) user_id, or None
               if the user is not registered.
        """
        return defer.ensureDeferred(self._auth_handler.check_user_exists(user_id))

    @defer.inlineCallbacks
    def register(
        self,
        localpart: str,
        displayname: Optional[str] = None,
        emails: Optional[List[str]] = None,
    ) -> Generator["defer.Deferred[Any]", Any, Tuple[str, str]]:
        """Registers a new user with given localpart and optional displayname, emails.

        Also returns an access token for the new user.

        Deprecated: avoid this, as it generates a new device with no way to
        return that device to the user. Prefer separate calls to register_user and
        register_device.

        Added in Synapse v0.25.0.

        Args:
            localpart: The localpart of the new user.
            displayname: The displayname of the new user.
            emails: Emails to bind to the new user.

        Returns:
            a 2-tuple of (user_id, access_token)
        """
        logger.warning(
            "Using deprecated ModuleApi.register which creates a dummy user device."
        )
        user_id = yield self.register_user(localpart, displayname, emails or [])
        _, access_token, _, _ = yield self.register_device(user_id)
        return user_id, access_token

    def register_user(
        self,
        localpart: str,
        displayname: Optional[str] = None,
        emails: Optional[List[str]] = None,
    ) -> "defer.Deferred[str]":
        """Registers a new user with given localpart and optional displayname, emails.

        Added in Synapse v1.2.0.

        Args:
            localpart: The localpart of the new user.
            displayname: The displayname of the new user.
            emails: Emails to bind to the new user.

        Raises:
            SynapseError if there is an error performing the registration. Check the
                'errcode' property for more information on the reason for failure

        Returns:
            user_id
        """
        return defer.ensureDeferred(
            self._hs.get_registration_handler().register_user(
                localpart=localpart,
                default_display_name=displayname,
                bind_emails=emails or [],
            )
        )

    def register_device(
        self,
        user_id: str,
        device_id: Optional[str] = None,
        initial_display_name: Optional[str] = None,
    ) -> "defer.Deferred[Tuple[str, str, Optional[int], Optional[str]]]":
        """Register a device for a user and generate an access token.

        Added in Synapse v1.2.0.

        Args:
            user_id: full canonical @user:id
            device_id: The device ID to check, or None to generate
                a new one.
            initial_display_name: An optional display name for the
                device.

        Returns:
            Tuple of device ID, access token, access token expiration time and refresh token
        """
        return defer.ensureDeferred(
            self._hs.get_registration_handler().register_device(
                user_id=user_id,
                device_id=device_id,
                initial_display_name=initial_display_name,
            )
        )

    def record_user_external_id(
        self, auth_provider_id: str, remote_user_id: str, registered_user_id: str
    ) -> defer.Deferred:
        """Record a mapping from an external user id to a mxid

        Added in Synapse v1.9.0.

        Args:
            auth_provider: identifier for the remote auth provider
            external_id: id on that system
            user_id: complete mxid that it is mapped to
        """
        return defer.ensureDeferred(
            self._store.record_user_external_id(
                auth_provider_id, remote_user_id, registered_user_id
            )
        )

    def generate_short_term_login_token(
        self,
        user_id: str,
        duration_in_ms: int = (2 * 60 * 1000),
        auth_provider_id: str = "",
        auth_provider_session_id: Optional[str] = None,
    ) -> str:
        """Generate a login token suitable for m.login.token authentication

        Added in Synapse v1.9.0.

        Args:
            user_id: gives the ID of the user that the token is for

            duration_in_ms: the time that the token will be valid for

            auth_provider_id: the ID of the SSO IdP that the user used to authenticate
               to get this token, if any. This is encoded in the token so that
               /login can report stats on number of successful logins by IdP.
        """
        return self._hs.get_macaroon_generator().generate_short_term_login_token(
            user_id,
            auth_provider_id,
            auth_provider_session_id,
            duration_in_ms,
        )

    @defer.inlineCallbacks
    def invalidate_access_token(
        self, access_token: str
    ) -> Generator["defer.Deferred[Any]", Any, None]:
        """Invalidate an access token for a user

        Added in Synapse v0.25.0.

        Args:
            access_token(str): access token

        Returns:
            twisted.internet.defer.Deferred - resolves once the access token
               has been removed.

        Raises:
            synapse.api.errors.AuthError: the access token is invalid
        """
        # see if the access token corresponds to a device
        user_info = yield defer.ensureDeferred(
            self._auth.get_user_by_access_token(access_token)
        )
        device_id = user_info.get("device_id")
        user_id = user_info["user"].to_string()
        if device_id:
            # delete the device, which will also delete its access tokens
            yield defer.ensureDeferred(
                self._hs.get_device_handler().delete_device(user_id, device_id)
            )
        else:
            # no associated device. Just delete the access token.
            yield defer.ensureDeferred(
                self._auth_handler.delete_access_token(access_token)
            )

    def run_db_interaction(
        self,
        desc: str,
        func: Callable[..., T],
        *args: Any,
        **kwargs: Any,
    ) -> "defer.Deferred[T]":
        """Run a function with a database connection

        Added in Synapse v0.25.0.

        Args:
            desc: description for the transaction, for metrics etc
            func: function to be run. Passed a database cursor object
                as well as *args and **kwargs
            *args: positional args to be passed to func
            **kwargs: named args to be passed to func

        Returns:
            Deferred[object]: result of func
        """
        return defer.ensureDeferred(
            self._store.db_pool.runInteraction(desc, func, *args, **kwargs)
        )

    def complete_sso_login(
        self, registered_user_id: str, request: SynapseRequest, client_redirect_url: str
    ) -> None:
        """Complete a SSO login by redirecting the user to a page to confirm whether they
        want their access token sent to `client_redirect_url`, or redirect them to that
        URL with a token directly if the URL matches with one of the whitelisted clients.

        This is deprecated in favor of complete_sso_login_async.

        Added in Synapse v1.11.1.

        Args:
            registered_user_id: The MXID that has been registered as a previous step of
                of this SSO login.
            request: The request to respond to.
            client_redirect_url: The URL to which to offer to redirect the user (or to
                redirect them directly if whitelisted).
        """
        self._auth_handler._complete_sso_login(
            registered_user_id,
            "<unknown>",
            request,
            client_redirect_url,
        )

    async def complete_sso_login_async(
        self,
        registered_user_id: str,
        request: SynapseRequest,
        client_redirect_url: str,
        new_user: bool = False,
        auth_provider_id: str = "<unknown>",
    ) -> None:
        """Complete a SSO login by redirecting the user to a page to confirm whether they
        want their access token sent to `client_redirect_url`, or redirect them to that
        URL with a token directly if the URL matches with one of the whitelisted clients.

        Added in Synapse v1.13.0.

        Args:
            registered_user_id: The MXID that has been registered as a previous step of
                of this SSO login.
            request: The request to respond to.
            client_redirect_url: The URL to which to offer to redirect the user (or to
                redirect them directly if whitelisted).
            new_user: set to true to use wording for the consent appropriate to a user
                who has just registered.
            auth_provider_id: the ID of the SSO IdP which was used to log in. This
                is used to track counts of sucessful logins by IdP.
        """
        await self._auth_handler.complete_sso_login(
            registered_user_id,
            auth_provider_id,
            request,
            client_redirect_url,
            new_user=new_user,
        )

    @defer.inlineCallbacks
    def get_state_events_in_room(
        self, room_id: str, types: Iterable[Tuple[str, Optional[str]]]
    ) -> Generator[defer.Deferred, Any, Iterable[EventBase]]:
        """Gets current state events for the given room.

        (This is exposed for compatibility with the old SpamCheckerApi. We should
        probably deprecate it and replace it with an async method in a subclass.)

        Added in Synapse v1.22.0.

        Args:
            room_id: The room ID to get state events in.
            types: The event type and state key (using None
                to represent 'any') of the room state to acquire.

        Returns:
            twisted.internet.defer.Deferred[list(synapse.events.FrozenEvent)]:
                The filtered state events in the room.
        """
        state_ids = yield defer.ensureDeferred(
            self._store.get_filtered_current_state_ids(
                room_id=room_id, state_filter=StateFilter.from_types(types)
            )
        )
        state = yield defer.ensureDeferred(self._store.get_events(state_ids.values()))
        return state.values()

    async def update_room_membership(
        self,
        sender: str,
        target: str,
        room_id: str,
        new_membership: str,
        content: Optional[JsonDict] = None,
    ) -> EventBase:
        """Updates the membership of a user to the given value.

        Added in Synapse v1.46.0.

        Args:
            sender: The user performing the membership change. Must be a user local to
                this homeserver.
            target: The user whose membership is changing. This is often the same value
                as `sender`, but it might differ in some cases (e.g. when kicking a user,
                the `sender` is the user performing the kick and the `target` is the user
                being kicked).
            room_id: The room in which to change the membership.
            new_membership: The new membership state of `target` after this operation. See
                https://spec.matrix.org/unstable/client-server-api/#mroommember for the
                list of allowed values.
            content: Additional values to include in the resulting event's content.

        Returns:
            The newly created membership event.

        Raises:
            RuntimeError if the `sender` isn't a local user.
            ShadowBanError if a shadow-banned requester attempts to send an invite.
            SynapseError if the module attempts to send a membership event that isn't
                allowed, either by the server's configuration (e.g. trying to set a
                per-room display name that's too long) or by the validation rules around
                membership updates (e.g. the `membership` value is invalid).
        """
        if not self.is_mine(sender):
            raise RuntimeError(
                "Tried to send an event as a user that isn't local to this homeserver",
            )

        requester = create_requester(sender)
        target_user_id = UserID.from_string(target)

        if content is None:
            content = {}

        # Set the profile if not already done by the module.
        if "avatar_url" not in content or "displayname" not in content:
            try:
                # Try to fetch the user's profile.
                profile = await self._hs.get_profile_handler().get_profile(
                    target_user_id.to_string(),
                )
            except SynapseError as e:
                # If the profile couldn't be found, use default values.
                profile = {
                    "displayname": target_user_id.localpart,
                    "avatar_url": None,
                }

                if e.code != 404:
                    # If the error isn't 404, it means we tried to fetch the profile over
                    # federation but the remote server responded with a non-standard
                    # status code.
                    logger.error(
                        "Got non-404 error status when fetching profile for %s",
                        target_user_id.to_string(),
                    )

            # Set the profile where it needs to be set.
            if "avatar_url" not in content:
                content["avatar_url"] = profile["avatar_url"]

            if "displayname" not in content:
                content["displayname"] = profile["displayname"]

        event_id, _ = await self._hs.get_room_member_handler().update_membership(
            requester=requester,
            target=target_user_id,
            room_id=room_id,
            action=new_membership,
            content=content,
        )

        # Try to retrieve the resulting event.
        event = await self._hs.get_datastore().get_event(event_id)

        # update_membership is supposed to always return after the event has been
        # successfully persisted.
        assert event is not None

        return event

    async def create_and_send_event_into_room(self, event_dict: JsonDict) -> EventBase:
        """Create and send an event into a room.

        Membership events are not supported by this method. To update a user's membership
        in a room, please use the `update_room_membership` method instead.

        Added in Synapse v1.22.0.

        Args:
            event_dict: A dictionary representing the event to send.
                Required keys are `type`, `room_id`, `sender` and `content`.

        Returns:
            The event that was sent. If state event deduplication happened, then
                the previous, duplicate event instead.

        Raises:
            SynapseError if the event was not allowed.
        """
        # Create a requester object
        requester = create_requester(
            event_dict["sender"], authenticated_entity=self._server_name
        )

        # Create and send the event
        (
            event,
            _,
        ) = await self._hs.get_event_creation_handler().create_and_send_nonmember_event(
            requester,
            event_dict,
            ratelimit=False,
            ignore_shadow_ban=True,
        )

        return event

    async def send_local_online_presence_to(self, users: Iterable[str]) -> None:
        """
        Forces the equivalent of a presence initial_sync for a set of local or remote
        users. The users will receive presence for all currently online users that they
        are considered interested in.

        Updates to remote users will be sent immediately, whereas local users will receive
        them on their next sync attempt.

        Note that this method can only be run on the process that is configured to write to the
        presence stream. By default this is the main process.

        Added in Synapse v1.32.0.
        """
        if self._hs._instance_name not in self._hs.config.worker.writers.presence:
            raise Exception(
                "send_local_online_presence_to can only be run "
                "on the process that is configured to write to the "
                "presence stream (by default this is the main process)",
            )

        local_users = set()
        remote_users = set()
        for user in users:
            if self._hs.is_mine_id(user):
                local_users.add(user)
            else:
                remote_users.add(user)

        # We pull out the presence handler here to break a cyclic
        # dependency between the presence router and module API.
        presence_handler = self._hs.get_presence_handler()

        if local_users:
            # Force a presence initial_sync for these users next time they sync.
            await presence_handler.send_full_presence_to_users(local_users)

        for user in remote_users:
            # Retrieve presence state for currently online users that this user
            # is considered interested in.
            presence_events, _ = await self._presence_stream.get_new_events(
                UserID.from_string(user), from_key=None, include_offline=False
            )

            # Send to remote destinations.
            destination = UserID.from_string(user).domain
            presence_handler.get_federation_queue().send_presence_to_destinations(
                presence_events, destination
            )

    def looping_background_call(
        self,
        f: Callable,
        msec: float,
        *args: object,
        desc: Optional[str] = None,
        run_on_all_instances: bool = False,
        **kwargs: object,
    ) -> None:
        """Wraps a function as a background process and calls it repeatedly.

        NOTE: Will only run on the instance that is configured to run
        background processes (which is the main process by default), unless
        `run_on_all_workers` is set.

        Waits `msec` initially before calling `f` for the first time.

        Added in Synapse v1.39.0.

        Args:
            f: The function to call repeatedly. f can be either synchronous or
                asynchronous, and must follow Synapse's logcontext rules.
                More info about logcontexts is available at
                https://matrix-org.github.io/synapse/latest/log_contexts.html
            msec: How long to wait between calls in milliseconds.
            *args: Positional arguments to pass to function.
            desc: The background task's description. Default to the function's name.
            run_on_all_instances: Whether to run this on all instances, rather
                than just the instance configured to run background tasks.
            **kwargs: Key arguments to pass to function.
        """
        if desc is None:
            desc = f.__name__

        if self._hs.config.worker.run_background_tasks or run_on_all_instances:
            self._clock.looping_call(
                run_as_background_process,
                msec,
                desc,
                lambda: maybe_awaitable(f(*args, **kwargs)),
            )
        else:
            logger.warning(
                "Not running looping call %s as the configuration forbids it",
                f,
            )

    async def sleep(self, seconds: float) -> None:
        """Sleeps for the given number of seconds."""

        await self._clock.sleep(seconds)

    async def send_mail(
        self,
        recipient: str,
        subject: str,
        html: str,
        text: str,
    ) -> None:
        """Send an email on behalf of the homeserver.

        Added in Synapse v1.39.0.

        Args:
            recipient: The email address for the recipient.
            subject: The email's subject.
            html: The email's HTML content.
            text: The email's text content.
        """
        await self._send_email_handler.send_email(
            email_address=recipient,
            subject=subject,
            app_name=self.email_app_name,
            html=html,
            text=text,
        )

    def read_templates(
        self,
        filenames: List[str],
        custom_template_directory: Optional[str] = None,
    ) -> List[jinja2.Template]:
        """Read and load the content of the template files at the given location.
        By default, Synapse will look for these templates in its configured template
        directory, but another directory to search in can be provided.

        Added in Synapse v1.39.0.

        Args:
            filenames: The name of the template files to look for.
            custom_template_directory: An additional directory to look for the files in.

        Returns:
            A list containing the loaded templates, with the orders matching the one of
            the filenames parameter.
        """
        return self._hs.config.server.read_templates(
            filenames,
            (td for td in (self.custom_template_dir, custom_template_directory) if td),
        )

    def is_mine(self, id: Union[str, DomainSpecificString]) -> bool:
        """
        Checks whether an ID (user id, room, ...) comes from this homeserver.

        Added in Synapse v1.44.0.

        Args:
            id: any Matrix id (e.g. user id, room id, ...), either as a raw id,
                e.g. string "@user:example.com" or as a parsed UserID, RoomID, ...
        Returns:
            True if id comes from this homeserver, False otherwise.
        """
        if isinstance(id, DomainSpecificString):
            return self._hs.is_mine(id)
        else:
            return self._hs.is_mine_id(id)

    async def get_user_ip_and_agents(
        self, user_id: str, since_ts: int = 0
    ) -> List[UserIpAndAgent]:
        """
        Return the list of user IPs and agents for a user.

        Added in Synapse v1.44.0.

        Args:
            user_id: the id of a user, local or remote
            since_ts: a timestamp in seconds since the epoch,
                or the epoch itself if not specified.
        Returns:
            The list of all UserIpAndAgent that the user has
            used to connect to this homeserver since `since_ts`.
            If the user is remote, this list is empty.
        """
        # Don't hit the db if this is not a local user.
        is_mine = False
        try:
            # Let's be defensive against ill-formed strings.
            if self.is_mine(user_id):
                is_mine = True
        except Exception:
            pass

        if is_mine:
            raw_data = await self._store.get_user_ip_and_agents(
                UserID.from_string(user_id), since_ts
            )
            # Sanitize some of the data. We don't want to return tokens.
            return [
                UserIpAndAgent(
                    ip=data["ip"],
                    user_agent=data["user_agent"],
                    last_seen=data["last_seen"],
                )
                for data in raw_data
            ]
        else:
            return []

    async def get_room_state(
        self,
        room_id: str,
        event_filter: Optional[Iterable[Tuple[str, Optional[str]]]] = None,
    ) -> StateMap[EventBase]:
        """Returns the current state of the given room.

        The events are returned as a mapping, in which the key for each event is a tuple
        which first element is the event's type and the second one is its state key.

        Added in Synapse v1.47.0

        Args:
            room_id: The ID of the room to get state from.
            event_filter: A filter to apply when retrieving events. None if no filter
                should be applied. If provided, must be an iterable of tuples. A tuple's
                first element is the event type and the second is the state key, or is
                None if the state key should not be filtered on.
                An example of a filter is:
                    [
                        ("m.room.member", "@alice:example.com"),  # Member event for @alice:example.com
                        ("org.matrix.some_event", ""),  # State event of type "org.matrix.some_event"
                                                        # with an empty string as its state key
                        ("org.matrix.some_other_event", None),  # State events of type "org.matrix.some_other_event"
                                                                # regardless of their state key
                    ]
        """
        if event_filter:
            # If a filter was provided, turn it into a StateFilter and retrieve a filtered
            # view of the state.
            state_filter = StateFilter.from_types(event_filter)
            state_ids = await self._store.get_filtered_current_state_ids(
                room_id,
                state_filter,
            )
        else:
            # If no filter was provided, get the whole state. We could also reuse the call
            # to get_filtered_current_state_ids above, with `state_filter = StateFilter.all()`,
            # but get_filtered_current_state_ids isn't cached and `get_current_state_ids`
            # is, so using the latter when we can is better for perf.
            state_ids = await self._store.get_current_state_ids(room_id)

        state_events = await self._store.get_events(state_ids.values())

        return {key: state_events[event_id] for key, event_id in state_ids.items()}

    async def defer_to_thread(
        self,
        f: Callable[..., T],
        *args: Any,
        **kwargs: Any,
    ) -> T:
        """Runs the given function in a separate thread from Synapse's thread pool.

        Added in Synapse v1.49.0.

        Args:
            f: The function to run.
            args: The function's arguments.
            kwargs: The function's keyword arguments.

        Returns:
            The return value of the function once ran in a thread.
        """
        return await defer_to_thread(self._hs.get_reactor(), f, *args, **kwargs)


class PublicRoomListManager:
    """Contains methods for adding to, removing from and querying whether a room
    is in the public room list.
    """

    def __init__(self, hs: "HomeServer"):
        self._store = hs.get_datastore()

    async def room_is_in_public_room_list(self, room_id: str) -> bool:
        """Checks whether a room is in the public room list.

        Added in Synapse v1.22.0.

        Args:
            room_id: The ID of the room.

        Returns:
            Whether the room is in the public room list. Returns False if the room does
            not exist.
        """
        room = await self._store.get_room(room_id)
        if not room:
            return False

        return room.get("is_public", False)

    async def add_room_to_public_room_list(self, room_id: str) -> None:
        """Publishes a room to the public room list.

        Added in Synapse v1.22.0.

        Args:
            room_id: The ID of the room.
        """
        await self._store.set_room_is_public(room_id, True)

    async def remove_room_from_public_room_list(self, room_id: str) -> None:
        """Removes a room from the public room list.

        Added in Synapse v1.22.0.

        Args:
            room_id: The ID of the room.
        """
        await self._store.set_room_is_public(room_id, False)
