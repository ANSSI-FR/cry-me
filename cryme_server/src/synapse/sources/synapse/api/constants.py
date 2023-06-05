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

"""Contains constants from the specification."""

from typing_extensions import Final

# the max size of a (canonical-json-encoded) event
MAX_PDU_SIZE = 65536

# the "depth" field on events is limited to 2**63 - 1
MAX_DEPTH = 2 ** 63 - 1

# the maximum length for a room alias is 255 characters
MAX_ALIAS_LENGTH = 255

# the maximum length for a user id is 255 characters
MAX_USERID_LENGTH = 255

# The maximum length for a group id is 255 characters
MAX_GROUPID_LENGTH = 255
MAX_GROUP_CATEGORYID_LENGTH = 255
MAX_GROUP_ROLEID_LENGTH = 255


class Membership:

    """Represents the membership states of a user in a room."""

    INVITE: Final = "invite"
    JOIN: Final = "join"
    KNOCK: Final = "knock"
    LEAVE: Final = "leave"
    BAN: Final = "ban"
    LIST: Final = (INVITE, JOIN, KNOCK, LEAVE, BAN)


class PresenceState:
    """Represents the presence state of a user."""

    OFFLINE: Final = "offline"
    UNAVAILABLE: Final = "unavailable"
    ONLINE: Final = "online"
    BUSY: Final = "org.matrix.msc3026.busy"


class JoinRules:
    PUBLIC: Final = "public"
    KNOCK: Final = "knock"
    INVITE: Final = "invite"
    PRIVATE: Final = "private"
    # As defined for MSC3083.
    RESTRICTED: Final = "restricted"


class RestrictedJoinRuleTypes:
    """Understood types for the allow rules in restricted join rules."""

    ROOM_MEMBERSHIP: Final = "m.room_membership"


class LoginType:
    PASSWORD: Final = "m.login.password"
    EMAIL_IDENTITY: Final = "m.login.email.identity"
    MSISDN: Final = "m.login.msisdn"
    RECAPTCHA: Final = "m.login.recaptcha"
    TERMS: Final = "m.login.terms"
    SSO: Final = "m.login.sso"
    DUMMY: Final = "m.login.dummy"
    REGISTRATION_TOKEN: Final = "org.matrix.msc3231.login.registration_token"


# This is used in the `type` parameter for /register when called by
# an appservice to register a new user.
APP_SERVICE_REGISTRATION_TYPE: Final = "m.login.application_service"


class EventTypes:
    Member: Final = "m.room.member"
    Create: Final = "m.room.create"
    Tombstone: Final = "m.room.tombstone"
    JoinRules: Final = "m.room.join_rules"
    PowerLevels: Final = "m.room.power_levels"
    Aliases: Final = "m.room.aliases"
    Redaction: Final = "m.room.redaction"
    ThirdPartyInvite: Final = "m.room.third_party_invite"
    RelatedGroups: Final = "m.room.related_groups"

    RoomHistoryVisibility: Final = "m.room.history_visibility"
    CanonicalAlias: Final = "m.room.canonical_alias"
    Encrypted: Final = "m.room.encrypted"
    RoomAvatar: Final = "m.room.avatar"
    RoomEncryption: Final = "m.room.encryption"
    GuestAccess: Final = "m.room.guest_access"

    # These are used for validation
    Message: Final = "m.room.message"
    Topic: Final = "m.room.topic"
    Name: Final = "m.room.name"

    ServerACL: Final = "m.room.server_acl"
    Pinned: Final = "m.room.pinned_events"

    Retention: Final = "m.room.retention"

    Dummy: Final = "org.matrix.dummy_event"

    SpaceChild: Final = "m.space.child"
    SpaceParent: Final = "m.space.parent"

    MSC2716_INSERTION: Final = "org.matrix.msc2716.insertion"
    MSC2716_BATCH: Final = "org.matrix.msc2716.batch"
    MSC2716_MARKER: Final = "org.matrix.msc2716.marker"


class ToDeviceEventTypes:
    RoomKeyRequest: Final = "m.room_key_request"


class DeviceKeyAlgorithms:
    """Spec'd algorithms for the generation of per-device keys"""

    WEISIG25519: Final = "weisig25519"
    WEI25519: Final = "wei25519"
    SIGNED_WEI25519: Final = "signed_wei25519"


class EduTypes:
    Presence: Final = "m.presence"


class RejectedReason:
    AUTH_ERROR: Final = "auth_error"


class RoomCreationPreset:
    PRIVATE_CHAT: Final = "private_chat"
    PUBLIC_CHAT: Final = "public_chat"
    TRUSTED_PRIVATE_CHAT: Final = "trusted_private_chat"


class ThirdPartyEntityKind:
    USER: Final = "user"
    LOCATION: Final = "location"


ServerNoticeMsgType: Final = "m.server_notice"
ServerNoticeLimitReached: Final = "m.server_notice.usage_limit_reached"


class UserTypes:
    """Allows for user type specific behaviour. With the benefit of hindsight
    'admin' and 'guest' users should also be UserTypes. Normal users are type None
    """

    SUPPORT: Final = "support"
    BOT: Final = "bot"
    ALL_USER_TYPES: Final = (SUPPORT, BOT)


class RelationTypes:
    """The types of relations known to this server."""

    ANNOTATION: Final = "m.annotation"
    REPLACE: Final = "m.replace"
    REFERENCE: Final = "m.reference"
    THREAD: Final = "io.element.thread"


class LimitBlockingTypes:
    """Reasons that a server may be blocked"""

    MONTHLY_ACTIVE_USER: Final = "monthly_active_user"
    HS_DISABLED: Final = "hs_disabled"


class EventContentFields:
    """Fields found in events' content, regardless of type."""

    # Labels for the event, cf https://github.com/matrix-org/matrix-doc/pull/2326
    LABELS: Final = "org.matrix.labels"

    # Timestamp to delete the event after
    # cf https://github.com/matrix-org/matrix-doc/pull/2228
    SELF_DESTRUCT_AFTER: Final = "org.matrix.self_destruct_after"

    # cf https://github.com/matrix-org/matrix-doc/pull/1772
    ROOM_TYPE: Final = "type"

    # Whether a room can federate.
    FEDERATE: Final = "m.federate"

    # The creator of the room, as used in `m.room.create` events.
    ROOM_CREATOR: Final = "creator"

    # Used in m.room.guest_access events.
    GUEST_ACCESS: Final = "guest_access"

    # Used on normal messages to indicate they were historically imported after the fact
    MSC2716_HISTORICAL: Final = "org.matrix.msc2716.historical"
    # For "insertion" events to indicate what the next batch ID should be in
    # order to connect to it
    MSC2716_NEXT_BATCH_ID: Final = "org.matrix.msc2716.next_batch_id"
    # Used on "batch" events to indicate which insertion event it connects to
    MSC2716_BATCH_ID: Final = "org.matrix.msc2716.batch_id"
    # For "marker" events
    MSC2716_MARKER_INSERTION: Final = "org.matrix.msc2716.marker.insertion"

    # The authorising user for joining a restricted room.
    AUTHORISING_USER: Final = "join_authorised_via_users_server"


class RoomTypes:
    """Understood values of the room_type field of m.room.create events."""

    SPACE: Final = "m.space"


class RoomEncryptionAlgorithms:
    MEGOLM_V1_AES_SHA2: Final = "m.megolm.v1.aes-sha"
    DEFAULT: Final = MEGOLM_V1_AES_SHA2


class AccountDataTypes:
    DIRECT: Final = "m.direct"
    IGNORED_USER_LIST: Final = "m.ignored_user_list"


class HistoryVisibility:
    INVITED: Final = "invited"
    JOINED: Final = "joined"
    SHARED: Final = "shared"
    WORLD_READABLE: Final = "world_readable"


class GuestAccess:
    CAN_JOIN: Final = "can_join"
    # anything that is not "can_join" is considered "forbidden", but for completeness:
    FORBIDDEN: Final = "forbidden"


class ReceiptTypes:
    READ: Final = "m.read"
    FAILURE: Final = "m.failure"


class ReadReceiptEventFields:
    MSC2285_HIDDEN: Final = "org.matrix.msc2285.hidden"
