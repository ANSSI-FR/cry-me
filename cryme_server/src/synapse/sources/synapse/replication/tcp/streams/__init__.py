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
# Copyright 2017 Vector Creations Ltd
# Copyright 2019 New Vector Ltd
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

"""Defines all the valid streams that clients can subscribe to, and the format
of the rows returned by each stream.

Each stream is defined by the following information:

    stream name:        The name of the stream
    row type:           The type that is used to serialise/deserialse the row
    current_token:      The function that returns the current token for the stream
    update_function:    The function that returns a list of updates between two tokens
"""

from synapse.replication.tcp.streams._base import (
    AccountDataStream,
    BackfillStream,
    CachesStream,
    DeviceListsStream,
    GroupServerStream,
    PresenceFederationStream,
    PresenceStream,
    PushersStream,
    PushRulesStream,
    ReceiptsStream,
    Stream,
    TagAccountDataStream,
    ToDeviceStream,
    TypingStream,
    UserSignatureStream,
)
from synapse.replication.tcp.streams.events import EventsStream
from synapse.replication.tcp.streams.federation import FederationStream

STREAMS_MAP = {
    stream.NAME: stream
    for stream in (
        EventsStream,
        BackfillStream,
        PresenceStream,
        PresenceFederationStream,
        TypingStream,
        ReceiptsStream,
        PushRulesStream,
        PushersStream,
        CachesStream,
        DeviceListsStream,
        ToDeviceStream,
        FederationStream,
        TagAccountDataStream,
        AccountDataStream,
        GroupServerStream,
        UserSignatureStream,
    )
}

__all__ = [
    "STREAMS_MAP",
    "Stream",
    "BackfillStream",
    "PresenceStream",
    "PresenceFederationStream",
    "TypingStream",
    "ReceiptsStream",
    "PushRulesStream",
    "PushersStream",
    "CachesStream",
    "DeviceListsStream",
    "ToDeviceStream",
    "TagAccountDataStream",
    "AccountDataStream",
    "GroupServerStream",
    "UserSignatureStream",
]
