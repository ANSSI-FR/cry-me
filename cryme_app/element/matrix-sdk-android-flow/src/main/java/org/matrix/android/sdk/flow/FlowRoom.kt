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

package org.matrix.android.sdk.flow

import androidx.lifecycle.asFlow
import kotlinx.coroutines.flow.Flow
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.members.RoomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.EventAnnotationsSummary
import org.matrix.android.sdk.api.session.room.model.ReadReceipt
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import org.matrix.android.sdk.api.session.room.send.UserDraft
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional

class FlowRoom(private val room: Room) {

    fun liveRoomSummary(): Flow<Optional<RoomSummary>> {
        return room.getRoomSummaryLive().asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.roomSummary().toOptional()
                }
    }

    fun liveRoomMembers(queryParams: RoomMemberQueryParams): Flow<List<RoomMemberSummary>> {
        return room.getRoomMembersLive(queryParams).asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.getRoomMembers(queryParams)
                }
    }

    fun liveAnnotationSummary(eventId: String): Flow<Optional<EventAnnotationsSummary>> {
        return room.getEventAnnotationsSummaryLive(eventId).asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.getEventAnnotationsSummary(eventId).toOptional()
                }
    }

    fun liveTimelineEvent(eventId: String): Flow<Optional<TimelineEvent>> {
        return room.getTimeLineEventLive(eventId).asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.getTimeLineEvent(eventId).toOptional()
                }
    }

    fun liveStateEvent(eventType: String, stateKey: QueryStringValue): Flow<Optional<Event>> {
        return room.getStateEventLive(eventType, stateKey).asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.getStateEvent(eventType, stateKey).toOptional()
                }
    }

    fun liveStateEvents(eventTypes: Set<String>): Flow<List<Event>> {
        return room.getStateEventsLive(eventTypes).asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.getStateEvents(eventTypes)
                }
    }

    fun liveReadMarker(): Flow<Optional<String>> {
        return room.getReadMarkerLive().asFlow()
    }

    fun liveReadReceipt(): Flow<Optional<String>> {
        return room.getMyReadReceiptLive().asFlow()
    }

    fun liveEventReadReceipts(eventId: String): Flow<List<ReadReceipt>> {
        return room.getEventReadReceiptsLive(eventId).asFlow()
    }

    fun liveDraft(): Flow<Optional<UserDraft>> {
        return room.getDraftLive().asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.getDraft().toOptional()
                }
    }

    fun liveNotificationState(): Flow<RoomNotificationState> {
        return room.getLiveRoomNotificationState().asFlow()
    }
}

fun Room.flow(): FlowRoom {
    return FlowRoom(this)
}
