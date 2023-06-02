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

package im.vector.app.features.call

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.call.audio.CallAudioManager
import org.matrix.android.sdk.api.session.call.CallState
import org.matrix.android.sdk.api.util.MatrixItem

data class VectorCallViewState(
        val callId: String,
        val roomId: String,
        val isVideoCall: Boolean,
        val isRemoteOnHold: Boolean = false,
        val isLocalOnHold: Boolean = false,
        val isAudioMuted: Boolean = false,
        val isVideoEnabled: Boolean = true,
        val isVideoCaptureInError: Boolean = false,
        val isHD: Boolean = false,
        val isFrontCamera: Boolean = true,
        val canSwitchCamera: Boolean = true,
        val device: CallAudioManager.Device = CallAudioManager.Device.Phone,
        val availableDevices: Set<CallAudioManager.Device> = emptySet(),
        val callState: Async<CallState> = Uninitialized,
        val otherKnownCallInfo: CallInfo? = null,
        val callInfo: CallInfo? = null,
        val formattedDuration: String = "",
        val canOpponentBeTransferred: Boolean = false,
        val transferee: TransfereeState = TransfereeState.NoTransferee
) : MavericksState {

    sealed class TransfereeState {
        object NoTransferee : TransfereeState()
        data class KnownTransferee(val name: String) : TransfereeState()
        object UnknownTransferee : TransfereeState()
    }

    data class CallInfo(
            val callId: String,
            val opponentUserItem: MatrixItem? = null
    )

    constructor(callArgs: CallArgs) : this(
            callId = callArgs.callId,
            roomId = callArgs.signalingRoomId,
            isVideoCall = callArgs.isVideoCall
    )
}
