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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home.room.list.actions

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import im.vector.app.R
import im.vector.app.core.platform.VectorSharedAction

sealed class RoomListQuickActionsSharedAction(
        @StringRes val titleRes: Int,
        @DrawableRes val iconResId: Int?,
        val destructive: Boolean = false) :
    VectorSharedAction {

    data class NotificationsAllNoisy(val roomId: String) : RoomListQuickActionsSharedAction(
            R.string.room_list_quick_actions_notifications_all_noisy,
            R.drawable.ic_room_actions_notifications_all_noisy
    )

    data class NotificationsAll(val roomId: String) : RoomListQuickActionsSharedAction(
            R.string.room_list_quick_actions_notifications_all,
            R.drawable.ic_room_actions_notifications_all
    )

    data class NotificationsMentionsOnly(val roomId: String) : RoomListQuickActionsSharedAction(
            R.string.room_list_quick_actions_notifications_mentions,
            R.drawable.ic_room_actions_notifications_mentions
    )

    data class NotificationsMute(val roomId: String) : RoomListQuickActionsSharedAction(
            R.string.room_list_quick_actions_notifications_mute,
            R.drawable.ic_room_actions_notifications_mutes
    )

    data class Settings(val roomId: String) : RoomListQuickActionsSharedAction(
            R.string.room_list_quick_actions_settings,
            R.drawable.ic_room_actions_settings
    )

    data class LowPriority(val roomId: String) : RoomListQuickActionsSharedAction(
            R.string.room_list_quick_actions_low_priority_add,
            R.drawable.ic_low_priority_24)

    data class Favorite(val roomId: String) : RoomListQuickActionsSharedAction(
            R.string.room_list_quick_actions_favorite_add,
            R.drawable.ic_star_24dp)

    data class Leave(val roomId: String, val showIcon: Boolean = true) : RoomListQuickActionsSharedAction(
            R.string.room_list_quick_actions_leave,
            if (showIcon) R.drawable.ic_room_actions_leave else null,
            true
    )
}
