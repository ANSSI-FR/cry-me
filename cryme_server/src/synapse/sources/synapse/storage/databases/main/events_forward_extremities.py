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

import logging
from typing import Any, Dict, List

from synapse.api.errors import SynapseError
from synapse.storage.database import LoggingTransaction
from synapse.storage.databases.main import CacheInvalidationWorkerStore
from synapse.storage.databases.main.event_federation import EventFederationWorkerStore

logger = logging.getLogger(__name__)


class EventForwardExtremitiesStore(
    EventFederationWorkerStore,
    CacheInvalidationWorkerStore,
):
    async def delete_forward_extremities_for_room(self, room_id: str) -> int:
        """Delete any extra forward extremities for a room.

        Invalidates the "get_latest_event_ids_in_room" cache if any forward
        extremities were deleted.

        Returns count deleted.
        """

        def delete_forward_extremities_for_room_txn(txn: LoggingTransaction) -> int:
            # First we need to get the event_id to not delete
            sql = """
                SELECT event_id FROM event_forward_extremities
                INNER JOIN events USING (room_id, event_id)
                WHERE room_id = ?
                ORDER BY stream_ordering DESC
                LIMIT 1
            """
            txn.execute(sql, (room_id,))
            rows = txn.fetchall()
            try:
                event_id = rows[0][0]
                logger.debug(
                    "Found event_id %s as the forward extremity to keep for room %s",
                    event_id,
                    room_id,
                )
            except KeyError:
                msg = "No forward extremity event found for room %s" % room_id
                logger.warning(msg)
                raise SynapseError(400, msg)

            # Now delete the extra forward extremities
            sql = """
                DELETE FROM event_forward_extremities
                WHERE event_id != ? AND room_id = ?
            """

            txn.execute(sql, (event_id, room_id))
            logger.info(
                "Deleted %s extra forward extremities for room %s",
                txn.rowcount,
                room_id,
            )

            if txn.rowcount > 0:
                # Invalidate the cache
                self._invalidate_cache_and_stream(
                    txn,
                    self.get_latest_event_ids_in_room,
                    (room_id,),
                )

            return txn.rowcount

        return await self.db_pool.runInteraction(
            "delete_forward_extremities_for_room",
            delete_forward_extremities_for_room_txn,
        )

    async def get_forward_extremities_for_room(
        self, room_id: str
    ) -> List[Dict[str, Any]]:
        """Get list of forward extremities for a room."""

        def get_forward_extremities_for_room_txn(
            txn: LoggingTransaction,
        ) -> List[Dict[str, Any]]:
            sql = """
                SELECT event_id, state_group, depth, received_ts
                FROM event_forward_extremities
                INNER JOIN event_to_state_groups USING (event_id)
                INNER JOIN events USING (room_id, event_id)
                WHERE room_id = ?
            """

            txn.execute(sql, (room_id,))
            return self.db_pool.cursor_to_dict(txn)

        return await self.db_pool.runInteraction(
            "get_forward_extremities_for_room",
            get_forward_extremities_for_room_txn,
        )
