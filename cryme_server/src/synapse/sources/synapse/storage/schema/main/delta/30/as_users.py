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

from synapse.config.appservice import load_appservices

logger = logging.getLogger(__name__)


def run_create(cur, database_engine, *args, **kwargs):
    # NULL indicates user was not registered by an appservice.
    try:
        cur.execute("ALTER TABLE users ADD COLUMN appservice_id TEXT")
    except Exception:
        # Maybe we already added the column? Hope so...
        pass


def run_upgrade(cur, database_engine, config, *args, **kwargs):
    cur.execute("SELECT name FROM users")
    rows = cur.fetchall()

    config_files = []
    try:
        config_files = config.appservice.app_service_config_files
    except AttributeError:
        logger.warning("Could not get app_service_config_files from config")
        pass

    appservices = load_appservices(config.server.server_name, config_files)

    owned = {}

    for row in rows:
        user_id = row[0]
        for appservice in appservices:
            if appservice.is_exclusive_user(user_id):
                if user_id in owned.keys():
                    logger.error(
                        "user_id %s was owned by more than one application"
                        " service (IDs %s and %s); assigning arbitrarily to %s"
                        % (user_id, owned[user_id], appservice.id, owned[user_id])
                    )
                owned.setdefault(appservice.id, []).append(user_id)

    for as_id, user_ids in owned.items():
        n = 100
        user_chunks = (user_ids[i : i + 100] for i in range(0, len(user_ids), n))
        for chunk in user_chunks:
            cur.execute(
                "UPDATE users SET appservice_id = ? WHERE name IN (%s)"
                % (",".join("?" for _ in chunk),),
                [as_id] + chunk,
            )
