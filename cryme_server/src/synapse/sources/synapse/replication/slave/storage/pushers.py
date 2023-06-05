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
from typing import TYPE_CHECKING

from synapse.replication.tcp.streams import PushersStream
from synapse.storage.database import DatabasePool, LoggingDatabaseConnection
from synapse.storage.databases.main.pusher import PusherWorkerStore

from ._base import BaseSlavedStore
from ._slaved_id_tracker import SlavedIdTracker

if TYPE_CHECKING:
    from synapse.server import HomeServer


class SlavedPusherStore(PusherWorkerStore, BaseSlavedStore):
    def __init__(
        self,
        database: DatabasePool,
        db_conn: LoggingDatabaseConnection,
        hs: "HomeServer",
    ):
        super().__init__(database, db_conn, hs)
        self._pushers_id_gen = SlavedIdTracker(  # type: ignore
            db_conn, "pushers", "id", extra_tables=[("deleted_pushers", "stream_id")]
        )

    def get_pushers_stream_token(self) -> int:
        return self._pushers_id_gen.get_current_token()

    def process_replication_rows(
        self, stream_name: str, instance_name: str, token, rows
    ) -> None:
        if stream_name == PushersStream.NAME:
            self._pushers_id_gen.advance(instance_name, token)  # type: ignore
        return super().process_replication_rows(stream_name, instance_name, token, rows)
