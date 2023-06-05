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

"""
This migration denormalises the account_data table into an ignored users table.
"""

import logging
from io import StringIO

from synapse.storage._base import db_to_json
from synapse.storage.engines import BaseDatabaseEngine
from synapse.storage.prepare_database import execute_statements_from_stream
from synapse.storage.types import Cursor

logger = logging.getLogger(__name__)


def run_upgrade(cur: Cursor, database_engine: BaseDatabaseEngine, *args, **kwargs):
    pass


def run_create(cur: Cursor, database_engine: BaseDatabaseEngine, *args, **kwargs):
    logger.info("Creating ignored_users table")
    execute_statements_from_stream(cur, StringIO(_create_commands))

    # We now upgrade existing data, if any. We don't do this in `run_upgrade` as
    # we a) want to run these before adding constraints and b) `run_upgrade` is
    # not run on empty databases.
    insert_sql = """
    INSERT INTO ignored_users (ignorer_user_id, ignored_user_id) VALUES (?, ?)
    """

    logger.info("Converting existing ignore lists")
    cur.execute(
        "SELECT user_id, content FROM account_data WHERE account_data_type = 'm.ignored_user_list'"
    )
    for user_id, content_json in cur.fetchall():
        content = db_to_json(content_json)

        # The content should be the form of a dictionary with a key
        # "ignored_users" pointing to a dictionary with keys of ignored users.
        #
        # { "ignored_users": "@someone:example.org": {} }
        ignored_users = content.get("ignored_users", {})
        if isinstance(ignored_users, dict) and ignored_users:
            cur.execute_batch(insert_sql, [(user_id, u) for u in ignored_users])

    # Add indexes after inserting data for efficiency.
    logger.info("Adding constraints to ignored_users table")
    execute_statements_from_stream(cur, StringIO(_constraints_commands))


# there might be duplicates, so the easiest way to achieve this is to create a new
# table with the right data, and renaming it into place

_create_commands = """
-- Users which are ignored when calculating push notifications. This data is
-- denormalized from account data.
CREATE TABLE IF NOT EXISTS ignored_users(
    ignorer_user_id TEXT NOT NULL,  -- The user ID of the user who is ignoring another user. (This is a local user.)
    ignored_user_id TEXT NOT NULL  -- The user ID of the user who is being ignored. (This is a local or remote user.)
);
"""

_constraints_commands = """
CREATE UNIQUE INDEX ignored_users_uniqueness ON ignored_users (ignorer_user_id, ignored_user_id);

-- Add an index on ignored_users since look-ups are done to get all ignorers of an ignored user.
CREATE INDEX ignored_users_ignored_user_id ON ignored_users (ignored_user_id);
"""
