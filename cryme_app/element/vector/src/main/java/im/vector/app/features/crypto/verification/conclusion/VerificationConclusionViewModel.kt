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
package im.vector.app.features.crypto.verification.conclusion

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.safeValueOf

data class VerificationConclusionViewState(
        val conclusionState: ConclusionState = ConclusionState.CANCELLED,
        val isSelfVerification: Boolean = false
) : MavericksState

enum class ConclusionState {
    SUCCESS,
    WARNING,
    CANCELLED
}

class VerificationConclusionViewModel(initialState: VerificationConclusionViewState) :
    VectorViewModel<VerificationConclusionViewState, EmptyAction, EmptyViewEvents>(initialState) {

    companion object : MavericksViewModelFactory<VerificationConclusionViewModel, VerificationConclusionViewState> {

        override fun initialState(viewModelContext: ViewModelContext): VerificationConclusionViewState? {
            val args = viewModelContext.args<VerificationConclusionFragment.Args>()

            return when (safeValueOf(args.cancelReason)) {
                CancelCode.QrCodeInvalid,
                CancelCode.MismatchedUser,
                CancelCode.MismatchedSas,
                CancelCode.MismatchedCommitment,
                CancelCode.MismatchedKeys -> {
                    VerificationConclusionViewState(ConclusionState.WARNING, args.isMe)
                }
                else                      -> {
                    VerificationConclusionViewState(
                            if (args.isSuccessFull) ConclusionState.SUCCESS else ConclusionState.CANCELLED,
                            args.isMe
                    )
                }
            }
        }
    }

    override fun handle(action: EmptyAction) {}
}
