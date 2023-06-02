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

package im.vector.app.test.fixtures

import im.vector.app.features.notifications.InviteNotifiableEvent
import im.vector.app.features.notifications.NotifiableMessageEvent
import im.vector.app.features.notifications.SimpleNotifiableEvent

fun aSimpleNotifiableEvent(
        eventId: String = "simple-event-id",
        type: String? = null,
        isRedacted: Boolean = false,
        canBeReplaced: Boolean = false,
        editedEventId: String? = null
) = SimpleNotifiableEvent(
        matrixID = null,
        eventId = eventId,
        editedEventId = editedEventId,
        noisy = false,
        title = "title",
        description = "description",
        type = type,
        timestamp = 0,
        soundName = null,
        canBeReplaced = canBeReplaced,
        isRedacted = isRedacted
)

fun anInviteNotifiableEvent(
        roomId: String = "an-invite-room-id",
        eventId: String = "invite-event-id",
        isRedacted: Boolean = false
) = InviteNotifiableEvent(
        matrixID = null,
        eventId = eventId,
        roomId = roomId,
        roomName = "a room name",
        editedEventId = null,
        noisy = false,
        title = "title",
        description = "description",
        type = null,
        timestamp = 0,
        soundName = null,
        canBeReplaced = false,
        isRedacted = isRedacted
)

fun aNotifiableMessageEvent(
        eventId: String = "a-message-event-id",
        roomId: String = "a-message-room-id",
        isRedacted: Boolean = false
) = NotifiableMessageEvent(
        eventId = eventId,
        editedEventId = null,
        noisy = false,
        timestamp = 0,
        senderName = "sender-name",
        senderId = "sending-id",
        body = "message-body",
        roomId = roomId,
        roomName = "room-name",
        roomIsDirect = false,
        canBeReplaced = false,
        isRedacted = isRedacted,
        imageUri = null
)
