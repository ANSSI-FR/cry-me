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

import logging

from synapse.storage.engines import PostgresEngine
from synapse.storage.prepare_database import get_statements

logger = logging.getLogger(__name__)


# This stream is used to notify replication slaves that some caches have
# been invalidated that they cannot infer from the other streams.
CREATE_TABLE = """
CREATE TABLE cache_invalidation_stream (
    stream_id       BIGINT,
    cache_func      TEXT,
    keys            TEXT[],
    invalidation_ts BIGINT
);

CREATE INDEX cache_invalidation_stream_id ON cache_invalidation_stream(stream_id);
"""


def run_create(cur, database_engine, *args, **kwargs):
    if not isinstance(database_engine, PostgresEngine):
        return

    for statement in get_statements(CREATE_TABLE.splitlines()):
        cur.execute(statement)


def run_upgrade(cur, database_engine, *args, **kwargs):
    pass
