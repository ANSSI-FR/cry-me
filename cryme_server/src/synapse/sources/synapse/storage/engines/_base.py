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
import abc
from typing import Generic, TypeVar

from synapse.storage.types import Connection


class IncorrectDatabaseSetup(RuntimeError):
    pass


ConnectionType = TypeVar("ConnectionType", bound=Connection)


class BaseDatabaseEngine(Generic[ConnectionType], metaclass=abc.ABCMeta):
    def __init__(self, module, database_config: dict):
        self.module = module

    @property
    @abc.abstractmethod
    def single_threaded(self) -> bool:
        ...

    @property
    @abc.abstractmethod
    def can_native_upsert(self) -> bool:
        """
        Do we support native UPSERTs?
        """
        ...

    @property
    @abc.abstractmethod
    def supports_using_any_list(self) -> bool:
        """
        Do we support using `a = ANY(?)` and passing a list
        """
        ...

    @property
    @abc.abstractmethod
    def supports_returning(self) -> bool:
        """Do we support the `RETURNING` clause in insert/update/delete?"""
        ...

    @abc.abstractmethod
    def check_database(
        self, db_conn: ConnectionType, allow_outdated_version: bool = False
    ) -> None:
        ...

    @abc.abstractmethod
    def check_new_database(self, txn) -> None:
        """Gets called when setting up a brand new database. This allows us to
        apply stricter checks on new databases versus existing database.
        """
        ...

    @abc.abstractmethod
    def convert_param_style(self, sql: str) -> str:
        ...

    @abc.abstractmethod
    def on_new_connection(self, db_conn: ConnectionType) -> None:
        ...

    @abc.abstractmethod
    def is_deadlock(self, error: Exception) -> bool:
        ...

    @abc.abstractmethod
    def is_connection_closed(self, conn: ConnectionType) -> bool:
        ...

    @abc.abstractmethod
    def lock_table(self, txn, table: str) -> None:
        ...

    @property
    @abc.abstractmethod
    def server_version(self) -> str:
        """Gets a string giving the server version. For example: '3.22.0'"""
        ...

    @abc.abstractmethod
    def in_transaction(self, conn: Connection) -> bool:
        """Whether the connection is currently in a transaction."""
        ...

    @abc.abstractmethod
    def attempt_to_set_autocommit(self, conn: Connection, autocommit: bool):
        """Attempt to set the connections autocommit mode.

        When True queries are run outside of transactions.

        Note: This has no effect on SQLite3, so callers still need to
        commit/rollback the connections.
        """
        ...
