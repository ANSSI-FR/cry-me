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

from synapse.replication.slave.storage._base import BaseSlavedStore
from synapse.replication.slave.storage._slaved_id_tracker import SlavedIdTracker
from synapse.replication.tcp.streams._base import DeviceListsStream, UserSignatureStream
from synapse.storage.database import DatabasePool, LoggingDatabaseConnection
from synapse.storage.databases.main.devices import DeviceWorkerStore
from synapse.storage.databases.main.end_to_end_keys import EndToEndKeyWorkerStore
from synapse.util.caches.stream_change_cache import StreamChangeCache

if TYPE_CHECKING:
    from synapse.server import HomeServer


class SlavedDeviceStore(EndToEndKeyWorkerStore, DeviceWorkerStore, BaseSlavedStore):
    def __init__(
        self,
        database: DatabasePool,
        db_conn: LoggingDatabaseConnection,
        hs: "HomeServer",
    ):
        super().__init__(database, db_conn, hs)

        self.hs = hs

        self._device_list_id_gen = SlavedIdTracker(
            db_conn,
            "device_lists_stream",
            "stream_id",
            extra_tables=[
                ("user_signature_stream", "stream_id"),
                ("device_lists_outbound_pokes", "stream_id"),
            ],
        )
        device_list_max = self._device_list_id_gen.get_current_token()
        self._device_list_stream_cache = StreamChangeCache(
            "DeviceListStreamChangeCache", device_list_max
        )
        self._user_signature_stream_cache = StreamChangeCache(
            "UserSignatureStreamChangeCache", device_list_max
        )
        self._device_list_federation_stream_cache = StreamChangeCache(
            "DeviceListFederationStreamChangeCache", device_list_max
        )

    def get_device_stream_token(self) -> int:
        return self._device_list_id_gen.get_current_token()

    def process_replication_rows(self, stream_name, instance_name, token, rows):
        if stream_name == DeviceListsStream.NAME:
            self._device_list_id_gen.advance(instance_name, token)
            self._invalidate_caches_for_devices(token, rows)
        elif stream_name == UserSignatureStream.NAME:
            self._device_list_id_gen.advance(instance_name, token)
            for row in rows:
                self._user_signature_stream_cache.entity_has_changed(row.user_id, token)
        return super().process_replication_rows(stream_name, instance_name, token, rows)

    def _invalidate_caches_for_devices(self, token, rows):
        for row in rows:
            # The entities are either user IDs (starting with '@') whose devices
            # have changed, or remote servers that we need to tell about
            # changes.
            if row.entity.startswith("@"):
                self._device_list_stream_cache.entity_has_changed(row.entity, token)
                self.get_cached_devices_for_user.invalidate((row.entity,))
                self._get_cached_user_device.invalidate((row.entity,))
                self.get_device_list_last_stream_id_for_remote.invalidate((row.entity,))

            else:
                self._device_list_federation_stream_cache.entity_has_changed(
                    row.entity, token
                )
