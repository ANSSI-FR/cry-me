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

package org.matrix.android.sdk.api.session.call

import org.matrix.android.sdk.api.session.room.model.call.CallAnswerContent
import org.matrix.android.sdk.api.session.room.model.call.CallAssertedIdentityContent
import org.matrix.android.sdk.api.session.room.model.call.CallCandidatesContent
import org.matrix.android.sdk.api.session.room.model.call.CallHangupContent
import org.matrix.android.sdk.api.session.room.model.call.CallInviteContent
import org.matrix.android.sdk.api.session.room.model.call.CallNegotiateContent
import org.matrix.android.sdk.api.session.room.model.call.CallRejectContent
import org.matrix.android.sdk.api.session.room.model.call.CallSelectAnswerContent

interface CallListener {
    /**
     * Called when there is an incoming call within the room.
     */
    fun onCallInviteReceived(mxCall: MxCall, callInviteContent: CallInviteContent)

    fun onCallIceCandidateReceived(mxCall: MxCall, iceCandidatesContent: CallCandidatesContent)

    /**
     * An outgoing call is started.
     */
    fun onCallAnswerReceived(callAnswerContent: CallAnswerContent)

    /**
     * Called when a called has been hung up
     */
    fun onCallHangupReceived(callHangupContent: CallHangupContent)

    /**
     * Called when a called has been rejected
     */
    fun onCallRejectReceived(callRejectContent: CallRejectContent)

    /**
     * Called when an answer has been selected
     */
    fun onCallSelectAnswerReceived(callSelectAnswerContent: CallSelectAnswerContent)

    /**
     * Called when a negotiation is sent
     */
    fun onCallNegotiateReceived(callNegotiateContent: CallNegotiateContent)

    /**
     * Called when the call has been managed by an other session
     */
    fun onCallManagedByOtherSession(callId: String)

    /**
     * Called when an asserted identity event is received
     */
    fun onCallAssertedIdentityReceived(callAssertedIdentityContent: CallAssertedIdentityContent)
}
