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
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.format

import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import javax.inject.Inject

class RoomHistoryVisibilityFormatter @Inject constructor(
        private val stringProvider: StringProvider
) {
    fun getNoticeSuffix(roomHistoryVisibility: RoomHistoryVisibility): String {
        return stringProvider.getString(when (roomHistoryVisibility) {
            RoomHistoryVisibility.WORLD_READABLE -> R.string.notice_room_visibility_world_readable
            RoomHistoryVisibility.SHARED         -> R.string.notice_room_visibility_shared
            RoomHistoryVisibility.INVITED        -> R.string.notice_room_visibility_invited
            RoomHistoryVisibility.JOINED         -> R.string.notice_room_visibility_joined
        })
    }

    fun getSetting(roomHistoryVisibility: RoomHistoryVisibility): String {
        return stringProvider.getString(when (roomHistoryVisibility) {
            RoomHistoryVisibility.WORLD_READABLE -> R.string.room_settings_read_history_entry_anyone
            RoomHistoryVisibility.SHARED         -> R.string.room_settings_read_history_entry_members_only_option_time_shared
            RoomHistoryVisibility.INVITED        -> R.string.room_settings_read_history_entry_members_only_invited
            RoomHistoryVisibility.JOINED         -> R.string.room_settings_read_history_entry_members_only_joined
        })
    }
}
