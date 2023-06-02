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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.room.membership.leaving

import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContent
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.session.room.membership.RoomChangeMembershipStateDataSource
import org.matrix.android.sdk.internal.session.room.state.StateEventDataSource
import org.matrix.android.sdk.internal.session.room.summary.RoomSummaryDataSource
import org.matrix.android.sdk.internal.task.Task
import timber.log.Timber
import javax.inject.Inject

internal interface LeaveRoomTask : Task<LeaveRoomTask.Params, Unit> {
    data class Params(
            val roomId: String,
            val reason: String?
    )
}

internal class DefaultLeaveRoomTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val globalErrorReceiver: GlobalErrorReceiver,
        private val stateEventDataSource: StateEventDataSource,
        private val roomSummaryDataSource: RoomSummaryDataSource,
        private val roomChangeMembershipStateDataSource: RoomChangeMembershipStateDataSource
) : LeaveRoomTask {

    override suspend fun execute(params: LeaveRoomTask.Params) {
        leaveRoom(params.roomId, params.reason)
    }

    private suspend fun leaveRoom(roomId: String, reason: String?) {
        val roomSummary = roomSummaryDataSource.getRoomSummary(roomId)
        if (roomSummary?.membership?.isActive() == false) {
            Timber.v("Room $roomId is not joined so can't be left")
            return
        }
        roomChangeMembershipStateDataSource.updateState(roomId, ChangeMembershipState.Leaving)
        val roomCreateStateEvent = stateEventDataSource.getStateEvent(
                roomId = roomId,
                eventType = EventType.STATE_ROOM_CREATE,
                stateKey = QueryStringValue.NoCondition
        )
        // Server is not cleaning predecessor rooms, so we also try to left them
        val predecessorRoomId = roomCreateStateEvent?.getClearContent()?.toModel<RoomCreateContent>()?.predecessor?.roomId
        if (predecessorRoomId != null) {
            leaveRoom(predecessorRoomId, reason)
        }
        try {
            executeRequest(globalErrorReceiver) {
                roomAPI.leave(roomId, mapOf("reason" to reason))
            }
        } catch (failure: Throwable) {
            roomChangeMembershipStateDataSource.updateState(roomId, ChangeMembershipState.FailedLeaving(failure))
            throw failure
        }
    }
}
