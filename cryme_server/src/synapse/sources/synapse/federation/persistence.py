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

""" This module contains all the persistence actions done by the federation
package.

These actions are mostly only used by the :py:mod:`.replication` module.
"""

import logging
from typing import Optional, Tuple

from synapse.federation.units import Transaction
from synapse.logging.utils import log_function
from synapse.storage.databases.main import DataStore
from synapse.types import JsonDict

logger = logging.getLogger(__name__)


class TransactionActions:
    """Defines persistence actions that relate to handling Transactions."""

    def __init__(self, datastore: DataStore):
        self.store = datastore

    @log_function
    async def have_responded(
        self, origin: str, transaction: Transaction
    ) -> Optional[Tuple[int, JsonDict]]:
        """Have we already responded to a transaction with the same id and
        origin?

        Returns:
            `None` if we have not previously responded to this transaction or a
            2-tuple of `(int, dict)` representing the response code and response body.
        """
        transaction_id = transaction.transaction_id
        if not transaction_id:
            raise RuntimeError("Cannot persist a transaction with no transaction_id")

        return await self.store.get_received_txn_response(transaction_id, origin)

    @log_function
    async def set_response(
        self, origin: str, transaction: Transaction, code: int, response: JsonDict
    ) -> None:
        """Persist how we responded to a transaction."""
        transaction_id = transaction.transaction_id
        if not transaction_id:
            raise RuntimeError("Cannot persist a transaction with no transaction_id")

        await self.store.set_received_txn_response(
            transaction_id, origin, code, response
        )
