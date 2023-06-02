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
 * Copyright (c) 2020 The Matrix.org Foundation C.I.C.
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

import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.session.call.CallIdGenerator
import org.matrix.android.sdk.api.session.call.MxCall
import org.matrix.android.sdk.api.session.room.model.call.CallCapabilities
import org.matrix.android.sdk.api.session.room.model.call.CallInviteContent
import org.matrix.android.sdk.api.session.room.model.call.CallSignalingContent
import org.matrix.android.sdk.internal.di.DeviceId
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.call.model.MxCallImpl
import org.matrix.android.sdk.internal.session.profile.GetProfileInfoTask
import org.matrix.android.sdk.internal.session.room.send.LocalEchoEventFactory
import org.matrix.android.sdk.internal.session.room.send.queue.EventSenderProcessor
import javax.inject.Inject

internal class MxCallFactory @Inject constructor(
        @DeviceId private val deviceId: String?,
        private val localEchoEventFactory: LocalEchoEventFactory,
        private val eventSenderProcessor: EventSenderProcessor,
        private val matrixConfiguration: MatrixConfiguration,
        private val getProfileInfoTask: GetProfileInfoTask,
        @UserId private val userId: String
) {

    fun createIncomingCall(roomId: String, opponentUserId: String, content: CallInviteContent): MxCall? {
        content.callId ?: return null
        return MxCallImpl(
                callId = content.callId,
                isOutgoing = false,
                roomId = roomId,
                userId = userId,
                ourPartyId = deviceId ?: "",
                isVideoCall = content.isVideo(),
                localEchoEventFactory = localEchoEventFactory,
                eventSenderProcessor = eventSenderProcessor,
                matrixConfiguration = matrixConfiguration,
                getProfileInfoTask = getProfileInfoTask
        ).apply {
            updateOpponentData(opponentUserId, content, content.capabilities)
        }
    }

    fun createOutgoingCall(roomId: String, opponentUserId: String, isVideoCall: Boolean): MxCall {
        return MxCallImpl(
                callId = CallIdGenerator.generate(),
                isOutgoing = true,
                roomId = roomId,
                userId = userId,
                ourPartyId = deviceId ?: "",
                isVideoCall = isVideoCall,
                localEchoEventFactory = localEchoEventFactory,
                eventSenderProcessor = eventSenderProcessor,
                matrixConfiguration = matrixConfiguration,
                getProfileInfoTask = getProfileInfoTask
        ).apply {
            // Setup with this userId, might be updated when processing the Answer event
            this.opponentUserId = opponentUserId
        }
    }

    fun updateOutgoingCallWithOpponentData(call: MxCall,
                                           userId: String,
                                           content: CallSignalingContent,
                                           callCapabilities: CallCapabilities?) {
        (call as? MxCallImpl)?.updateOpponentData(userId, content, callCapabilities)
    }
}
