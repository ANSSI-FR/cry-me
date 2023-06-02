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
package im.vector.app.features.crypto.verification.choose

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoints
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.SingletonEntryPoint
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.crypto.verification.VerificationBottomSheet
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.QrCodeVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction

data class VerificationChooseMethodViewState(
        val otherUserId: String = "",
        val transactionId: String = "",
        val otherCanShowQrCode: Boolean = false,
        val otherCanScanQrCode: Boolean = false,
        val qrCodeText: String? = null,
        val sasModeAvailable: Boolean = false,
        val isMe: Boolean = false,
        val canCrossSign: Boolean = false
) : MavericksState

class VerificationChooseMethodViewModel @AssistedInject constructor(
        @Assisted initialState: VerificationChooseMethodViewState,
        private val session: Session
) : VectorViewModel<VerificationChooseMethodViewState, EmptyAction, EmptyViewEvents>(initialState), VerificationService.Listener {

    init {
        session.cryptoService().verificationService().addListener(this)
    }

    override fun transactionCreated(tx: VerificationTransaction) {
        transactionUpdated(tx)
    }

    override fun transactionUpdated(tx: VerificationTransaction) = withState { state ->
        if (tx.transactionId == state.transactionId && tx is QrCodeVerificationTransaction) {
            setState {
                copy(
                        qrCodeText = tx.qrCodeText
                )
            }
        }
    }

    override fun verificationRequestCreated(pr: PendingVerificationRequest) {
        verificationRequestUpdated(pr)
    }

    override fun verificationRequestUpdated(pr: PendingVerificationRequest) = withState { state ->
        val pvr = session.cryptoService().verificationService().getExistingVerificationRequest(state.otherUserId, state.transactionId)

        setState {
            copy(
                    otherCanShowQrCode = pvr?.otherCanShowQrCode().orFalse(),
                    otherCanScanQrCode = pvr?.otherCanScanQrCode().orFalse(),
                    sasModeAvailable = pvr?.isSasSupported().orFalse()
            )
        }
    }

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<VerificationChooseMethodViewModel, VerificationChooseMethodViewState> {
        override fun create(initialState: VerificationChooseMethodViewState): VerificationChooseMethodViewModel
    }

    companion object : MavericksViewModelFactory<VerificationChooseMethodViewModel, VerificationChooseMethodViewState> by hiltMavericksViewModelFactory() {

        override fun initialState(viewModelContext: ViewModelContext): VerificationChooseMethodViewState {
            val args: VerificationBottomSheet.VerificationArgs = viewModelContext.args()
            val session = EntryPoints.get(viewModelContext.app(), SingletonEntryPoint::class.java).activeSessionHolder().getActiveSession()
            val verificationService = session.cryptoService().verificationService()
            val pvr = verificationService.getExistingVerificationRequest(args.otherUserId, args.verificationId)

            // Get the QR code now, because transaction is already created, so transactionCreated() will not be called
            val qrCodeVerificationTransaction = verificationService.getExistingTransaction(args.otherUserId, args.verificationId ?: "")

            return VerificationChooseMethodViewState(otherUserId = args.otherUserId,
                    isMe = session.myUserId == pvr?.otherUserId,
                    canCrossSign = session.cryptoService().crossSigningService().canCrossSign(),
                    transactionId = args.verificationId ?: "",
                    otherCanShowQrCode = pvr?.otherCanShowQrCode().orFalse(),
                    otherCanScanQrCode = pvr?.otherCanScanQrCode().orFalse(),
                    qrCodeText = (qrCodeVerificationTransaction as? QrCodeVerificationTransaction)?.qrCodeText,
                    sasModeAvailable = pvr?.isSasSupported().orFalse()
            )
        }
    }

    override fun onCleared() {
        session.cryptoService().verificationService().removeListener(this)
        super.onCleared()
    }

    override fun handle(action: EmptyAction) {}
}
