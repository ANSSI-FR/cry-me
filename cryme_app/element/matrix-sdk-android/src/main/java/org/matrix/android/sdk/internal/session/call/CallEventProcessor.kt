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

package org.matrix.android.sdk.internal.session.call

import io.realm.Realm
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.session.EventInsertLiveProcessor
import org.matrix.android.sdk.internal.session.SessionScope
import timber.log.Timber
import javax.inject.Inject

private val loggerTag = LoggerTag("CallEventProcessor", LoggerTag.VOIP)

@SessionScope
internal class CallEventProcessor @Inject constructor(private val callSignalingHandler: CallSignalingHandler) :
    EventInsertLiveProcessor {

    private val allowedTypes = listOf(
            EventType.CALL_ANSWER,
            EventType.CALL_SELECT_ANSWER,
            EventType.CALL_REJECT,
            EventType.CALL_NEGOTIATE,
            EventType.CALL_CANDIDATES,
            EventType.CALL_INVITE,
            EventType.CALL_HANGUP,
            EventType.ENCRYPTED,
            EventType.CALL_ASSERTED_IDENTITY,
            EventType.CALL_ASSERTED_IDENTITY_PREFIX
    )

    private val eventsToPostProcess = mutableListOf<Event>()

    override fun shouldProcess(eventId: String, eventType: String, insertType: EventInsertType): Boolean {
        if (insertType != EventInsertType.INCREMENTAL_SYNC) {
            return false
        }
        return allowedTypes.contains(eventType)
    }

    override suspend fun process(realm: Realm, event: Event) {
        eventsToPostProcess.add(event)
    }

    fun shouldProcessFastLane(eventType: String): Boolean {
        return eventType == EventType.CALL_INVITE
    }

    fun processFastLane(event: Event) {
        dispatchToCallSignalingHandlerIfNeeded(event)
    }

    override suspend fun onPostProcess() {
        eventsToPostProcess.forEach {
            dispatchToCallSignalingHandlerIfNeeded(it)
        }
        eventsToPostProcess.clear()
    }

    private fun dispatchToCallSignalingHandlerIfNeeded(event: Event) {
        event.roomId ?: return Unit.also {
            Timber.tag(loggerTag.value).w("Event with no room id ${event.eventId}")
        }
        callSignalingHandler.onCallEvent(event)
    }
}
