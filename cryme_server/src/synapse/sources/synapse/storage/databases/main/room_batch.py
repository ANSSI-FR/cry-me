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

from typing import Optional

from synapse.storage._base import SQLBaseStore


class RoomBatchStore(SQLBaseStore):
    async def get_insertion_event_id_by_batch_id(
        self, room_id: str, batch_id: str
    ) -> Optional[str]:
        """Retrieve a insertion event ID.

        Args:
            batch_id: The batch ID of the insertion event to retrieve.

        Returns:
            The event_id of an insertion event, or None if there is no known
            insertion event for the given insertion event.
        """
        return await self.db_pool.simple_select_one_onecol(
            table="insertion_events",
            keyvalues={"room_id": room_id, "next_batch_id": batch_id},
            retcol="event_id",
            allow_none=True,
        )

    async def store_state_group_id_for_event_id(
        self, event_id: str, state_group_id: int
    ) -> None:
        await self.db_pool.simple_upsert(
            table="event_to_state_groups",
            keyvalues={"event_id": event_id},
            values={"state_group": state_group_id, "event_id": event_id},
            # Unique constraint on event_id so we don't have to lock
            lock=False,
        )
