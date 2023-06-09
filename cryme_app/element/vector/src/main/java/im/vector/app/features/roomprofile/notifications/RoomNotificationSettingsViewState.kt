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
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.roomprofile.notifications

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.home.room.list.actions.RoomListActionsArgs
import im.vector.app.features.roomprofile.RoomProfileArgs
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState

data class RoomNotificationSettingsViewState(
        val roomId: String,
        val roomSummary: Async<RoomSummary> = Uninitialized,
        val isLoading: Boolean = false,
        val notificationState: Async<RoomNotificationState> = Uninitialized
)  : MavericksState {
    constructor(args: RoomProfileArgs) : this(roomId = args.roomId)
    constructor(args: RoomListActionsArgs) : this(roomId = args.roomId)
}

/**
 * Used to map this old room notification settings to the new options in v2.
 */
val RoomNotificationSettingsViewState.notificationStateMapped: Async<RoomNotificationState>
    get() {
        if ((roomSummary()?.isEncrypted == true && notificationState() == RoomNotificationState.MENTIONS_ONLY) ||
                notificationState() == RoomNotificationState.ALL_MESSAGES) {
            /** if in an encrypted room, mentions notifications are not supported so show "All Messages" as selected.
             * Also in the new settings there is no notion of notifications without sound so it maps to noisy also
             */
            return Success(RoomNotificationState.ALL_MESSAGES_NOISY)
        }
        return  notificationState
    }

/**
 * Used to enumerate the new settings in notification settings v2. Notifications without sound and mentions in encrypted rooms not supported.
 */
val RoomNotificationSettingsViewState.notificationOptions: List<RoomNotificationState>
    get() {
        return if (roomSummary()?.isEncrypted == true) {
            listOf(RoomNotificationState.ALL_MESSAGES_NOISY, RoomNotificationState.MUTE)
        } else {
            listOf(RoomNotificationState.ALL_MESSAGES_NOISY, RoomNotificationState.MENTIONS_ONLY, RoomNotificationState.MUTE)
        }
    }
