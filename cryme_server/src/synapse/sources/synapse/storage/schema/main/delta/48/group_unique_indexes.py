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

from synapse.storage.engines import PostgresEngine
from synapse.storage.prepare_database import get_statements

FIX_INDEXES = """
-- rebuild indexes as uniques
DROP INDEX groups_invites_g_idx;
CREATE UNIQUE INDEX group_invites_g_idx ON group_invites(group_id, user_id);
DROP INDEX groups_users_g_idx;
CREATE UNIQUE INDEX group_users_g_idx ON group_users(group_id, user_id);

-- rename other indexes to actually match their table names..
DROP INDEX groups_users_u_idx;
CREATE INDEX group_users_u_idx ON group_users(user_id);
DROP INDEX groups_invites_u_idx;
CREATE INDEX group_invites_u_idx ON group_invites(user_id);
DROP INDEX groups_rooms_g_idx;
CREATE UNIQUE INDEX group_rooms_g_idx ON group_rooms(group_id, room_id);
DROP INDEX groups_rooms_r_idx;
CREATE INDEX group_rooms_r_idx ON group_rooms(room_id);
"""


def run_create(cur, database_engine, *args, **kwargs):
    rowid = "ctid" if isinstance(database_engine, PostgresEngine) else "rowid"

    # remove duplicates from group_users & group_invites tables
    cur.execute(
        """
        DELETE FROM group_users WHERE %s NOT IN (
           SELECT min(%s) FROM group_users GROUP BY group_id, user_id
        );
    """
        % (rowid, rowid)
    )
    cur.execute(
        """
        DELETE FROM group_invites WHERE %s NOT IN (
           SELECT min(%s) FROM group_invites GROUP BY group_id, user_id
        );
    """
        % (rowid, rowid)
    )

    for statement in get_statements(FIX_INDEXES.splitlines()):
        cur.execute(statement)


def run_upgrade(*args, **kwargs):
    pass
