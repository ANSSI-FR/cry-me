/*************************** The CRY.ME project (2023) *************************************************
 *
 *  This file is part of the CRY.ME project (https://github.com/ANSSI-FR/cry-me).
 *  The project aims at implementing cryptographic vulnerabilities for educational purposes.
 *  Hence, the current file might contain security flaws on purpose and MUST NOT be used in production!
 *  Please do not use this source code outside this scope, or use it knowingly.
 *
 *  Many files come from the Android element (https://github.com/vector-im/element-android), the
 *  Matrix SDK (https://github.com/matrix-org/matrix-android-sdk2) as well as the Android Yubikit
 *  (https://github.com/Yubico/yubikit-android) projects and have been willingly modified
 *  for the CRY.ME project purposes. The Android element, Matrix SDK and Yubikit projects are distributed
 *  under the Apache-2.0 license, and so is the CRY.ME project.
 *
 ***************************  (END OF CRY.ME HEADER)   *************************************************/

/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.ui

import android.content.SharedPreferences
import androidx.core.content.edit
import im.vector.app.features.home.RoomListDisplayMode
import im.vector.app.features.settings.VectorPreferences
import javax.inject.Inject

/**
 * This class is used to persist UI state across application restart
 */
class SharedPreferencesUiStateRepository @Inject constructor(
        private val sharedPreferences: SharedPreferences,
        private val vectorPreferences: VectorPreferences
) : UiStateRepository {

    override fun reset() {
        sharedPreferences.edit {
            remove(KEY_DISPLAY_MODE)
        }
    }

    override fun getDisplayMode(): RoomListDisplayMode {
        return when (sharedPreferences.getInt(KEY_DISPLAY_MODE, VALUE_DISPLAY_MODE_CATCHUP)) {
            VALUE_DISPLAY_MODE_PEOPLE -> RoomListDisplayMode.PEOPLE
            else                      -> if (vectorPreferences.labAddNotificationTab()) {
                RoomListDisplayMode.NOTIFICATIONS
            } else {
                RoomListDisplayMode.PEOPLE
            }
        }
    }

    override fun storeDisplayMode(displayMode: RoomListDisplayMode) {
        sharedPreferences.edit {
            putInt(KEY_DISPLAY_MODE,
                    when (displayMode) {
                        RoomListDisplayMode.PEOPLE -> VALUE_DISPLAY_MODE_PEOPLE
                        else                       -> VALUE_DISPLAY_MODE_CATCHUP
                    })
        }
    }

    override fun storeSelectedSpace(spaceId: String?, sessionId: String) {
        sharedPreferences.edit {
            putString("$KEY_SELECTED_SPACE@$sessionId", spaceId)
        }
    }

    override fun storeSelectedGroup(groupId: String?, sessionId: String) {
        sharedPreferences.edit {
            putString("$KEY_SELECTED_GROUP@$sessionId", groupId)
        }
    }

    override fun storeGroupingMethod(isSpace: Boolean, sessionId: String) {
        sharedPreferences.edit {
            putBoolean("$KEY_SELECTED_METHOD@$sessionId", isSpace)
        }
    }

    override fun getSelectedGroup(sessionId: String): String? {
        return sharedPreferences.getString("$KEY_SELECTED_GROUP@$sessionId", null)
    }

    override fun getSelectedSpace(sessionId: String): String? {
        return sharedPreferences.getString("$KEY_SELECTED_SPACE@$sessionId", null)
    }

    override fun isGroupingMethodSpace(sessionId: String): Boolean {
        return sharedPreferences.getBoolean("$KEY_SELECTED_METHOD@$sessionId", true)
    }

    override fun setCustomRoomDirectoryHomeservers(sessionId: String, servers: Set<String>) {
        sharedPreferences.edit {
            putStringSet("$KEY_CUSTOM_DIRECTORY_HOMESERVER@$sessionId", servers)
        }
    }

    override fun getCustomRoomDirectoryHomeservers(sessionId: String): Set<String> {
        return sharedPreferences.getStringSet("$KEY_CUSTOM_DIRECTORY_HOMESERVER@$sessionId", null)
                .orEmpty()
                .toSet()
    }

    companion object {
        private const val KEY_DISPLAY_MODE = "UI_STATE_DISPLAY_MODE"
        private const val VALUE_DISPLAY_MODE_CATCHUP = 0
        private const val VALUE_DISPLAY_MODE_PEOPLE = 1
        private const val VALUE_DISPLAY_MODE_ROOMS = 2

        private const val KEY_SELECTED_SPACE = "UI_STATE_SELECTED_SPACE"
        private const val KEY_SELECTED_GROUP = "UI_STATE_SELECTED_GROUP"
        private const val KEY_SELECTED_METHOD = "UI_STATE_SELECTED_METHOD"

        private const val KEY_CUSTOM_DIRECTORY_HOMESERVER = "KEY_CUSTOM_DIRECTORY_HOMESERVER"
    }
}
