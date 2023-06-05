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
import logging
from io import StringIO

from synapse.storage.engines import PostgresEngine
from synapse.storage.prepare_database import execute_statements_from_stream

logger = logging.getLogger(__name__)


"""
This migration updates the user_filters table as follows:

 - drops any (user_id, filter_id) duplicates
 - makes the columns NON-NULLable
 - turns the index into a UNIQUE index
"""


def run_upgrade(cur, database_engine, *args, **kwargs):
    pass


def run_create(cur, database_engine, *args, **kwargs):
    if isinstance(database_engine, PostgresEngine):
        select_clause = """
            SELECT DISTINCT ON (user_id, filter_id) user_id, filter_id, filter_json
            FROM user_filters
        """
    else:
        select_clause = """
            SELECT * FROM user_filters GROUP BY user_id, filter_id
        """
    sql = """
            DROP TABLE IF EXISTS user_filters_migration;
            DROP INDEX IF EXISTS user_filters_unique;
            CREATE TABLE user_filters_migration (
                user_id TEXT NOT NULL,
                filter_id BIGINT NOT NULL,
                filter_json BYTEA NOT NULL
            );
            INSERT INTO user_filters_migration (user_id, filter_id, filter_json)
                %s;
            CREATE UNIQUE INDEX user_filters_unique ON user_filters_migration
                (user_id, filter_id);
            DROP TABLE user_filters;
            ALTER TABLE user_filters_migration RENAME TO user_filters;
        """ % (
        select_clause,
    )

    execute_statements_from_stream(cur, StringIO(sql))
