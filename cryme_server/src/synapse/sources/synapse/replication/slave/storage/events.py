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
# Copyright 2016 OpenMarket Ltd
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
from typing import TYPE_CHECKING

from synapse.storage.database import DatabasePool, LoggingDatabaseConnection
from synapse.storage.databases.main.event_federation import EventFederationWorkerStore
from synapse.storage.databases.main.event_push_actions import (
    EventPushActionsWorkerStore,
)
from synapse.storage.databases.main.events_worker import EventsWorkerStore
from synapse.storage.databases.main.relations import RelationsWorkerStore
from synapse.storage.databases.main.roommember import RoomMemberWorkerStore
from synapse.storage.databases.main.signatures import SignatureWorkerStore
from synapse.storage.databases.main.state import StateGroupWorkerStore
from synapse.storage.databases.main.stream import StreamWorkerStore
from synapse.storage.databases.main.user_erasure_store import UserErasureWorkerStore
from synapse.util.caches.stream_change_cache import StreamChangeCache

from ._base import BaseSlavedStore

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


# So, um, we want to borrow a load of functions intended for reading from
# a DataStore, but we don't want to take functions that either write to the
# DataStore or are cached and don't have cache invalidation logic.
#
# Rather than write duplicate versions of those functions, or lift them to
# a common base class, we going to grab the underlying __func__ object from
# the method descriptor on the DataStore and chuck them into our class.


class SlavedEventStore(
    EventFederationWorkerStore,
    RoomMemberWorkerStore,
    EventPushActionsWorkerStore,
    StreamWorkerStore,
    StateGroupWorkerStore,
    EventsWorkerStore,
    SignatureWorkerStore,
    UserErasureWorkerStore,
    RelationsWorkerStore,
    BaseSlavedStore,
):
    def __init__(
        self,
        database: DatabasePool,
        db_conn: LoggingDatabaseConnection,
        hs: "HomeServer",
    ):
        super().__init__(database, db_conn, hs)

        events_max = self._stream_id_gen.get_current_token()
        curr_state_delta_prefill, min_curr_state_delta_id = self.db_pool.get_cache_dict(
            db_conn,
            "current_state_delta_stream",
            entity_column="room_id",
            stream_column="stream_id",
            max_value=events_max,  # As we share the stream id with events token
            limit=1000,
        )
        self._curr_state_delta_stream_cache = StreamChangeCache(
            "_curr_state_delta_stream_cache",
            min_curr_state_delta_id,
            prefilled_cache=curr_state_delta_prefill,
        )
