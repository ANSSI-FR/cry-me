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
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.session.room.timeline

import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import kotlin.random.Random

object RoomDataHelper {

    private const val FAKE_TEST_SENDER = "@sender:test.org"
    private val EVENT_FACTORIES = hashMapOf(
            0 to { createFakeMessageEvent() },
            1 to { createFakeRoomMemberEvent() }
    )

    fun createFakeListOfEvents(size: Int = 10): List<Event> {
        return (0 until size).mapNotNull {
            val nextInt = Random.nextInt(EVENT_FACTORIES.size)
            EVENT_FACTORIES[nextInt]?.invoke()
        }
    }

    private fun createFakeEvent(type: String,
                                content: Content? = null,
                                prevContent: Content? = null,
                                sender: String = FAKE_TEST_SENDER,
                                stateKey: String? = null
    ): Event {
        return Event(
                type = type,
                eventId = Random.nextLong().toString(),
                content = content,
                prevContent = prevContent,
                senderId = sender,
                stateKey = stateKey
        )
    }

    fun createFakeMessageEvent(): Event {
        val message = MessageTextContent(MessageType.MSGTYPE_TEXT, "Fake message #${Random.nextLong()}").toContent()
        return createFakeEvent(EventType.MESSAGE, message)
    }

    private fun createFakeRoomMemberEvent(): Event {
        val roomMember = RoomMemberContent(Membership.JOIN, "Fake name #${Random.nextLong()}").toContent()
        return createFakeEvent(EventType.STATE_ROOM_MEMBER, roomMember, stateKey = FAKE_TEST_SENDER)
    }
}
