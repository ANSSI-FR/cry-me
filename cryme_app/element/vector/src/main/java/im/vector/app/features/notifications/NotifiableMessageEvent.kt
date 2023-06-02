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
package im.vector.app.features.notifications

import android.net.Uri
import org.matrix.android.sdk.api.session.events.model.EventType

data class NotifiableMessageEvent(
        override val eventId: String,
        override val editedEventId: String?,
        override val canBeReplaced: Boolean,
        val noisy: Boolean,
        val timestamp: Long,
        val senderName: String?,
        val senderId: String?,
        val body: String?,
        val imageUri: Uri?,
        val roomId: String,
        val roomName: String?,
        val roomIsDirect: Boolean = false,
        val roomAvatarPath: String? = null,
        val senderAvatarPath: String? = null,
        val matrixID: String? = null,
        val soundName: String? = null,
        // This is used for >N notification, as the result of a smart reply
        val outGoingMessage: Boolean = false,
        val outGoingMessageFailed: Boolean = false,
        override val isRedacted: Boolean = false
) : NotifiableEvent {

    val type: String = EventType.MESSAGE
    val description: String = body ?: ""
    val title: String = senderName ?: ""
}
