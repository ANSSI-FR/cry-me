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
package im.vector.app.features.settings.account.deactivation

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.auth.ReAuthActivity
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.UserPasswordAuth
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.failure.isInvalidUIAAuth
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.internal.crypto.crosssigning.fromBase64
import org.matrix.android.sdk.internal.crypto.model.rest.DefaultBaseAuth
import timber.log.Timber
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class DeactivateAccountViewState(
        val dummy: Boolean = false
) : MavericksState

class DeactivateAccountViewModel @AssistedInject constructor(@Assisted private val initialState: DeactivateAccountViewState,
                                                             private val session: Session) :
    VectorViewModel<DeactivateAccountViewState, DeactivateAccountAction, DeactivateAccountViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<DeactivateAccountViewModel, DeactivateAccountViewState> {
        override fun create(initialState: DeactivateAccountViewState): DeactivateAccountViewModel
    }

    var uiaContinuation: Continuation<UIABaseAuth>? = null
    var pendingAuth: UIABaseAuth? = null

    override fun handle(action: DeactivateAccountAction) {
        when (action) {
            is DeactivateAccountAction.DeactivateAccount -> handleDeactivateAccount(action)
            DeactivateAccountAction.SsoAuthDone -> {
                Timber.d("## UIA - FallBack success")
                if (pendingAuth != null) {
                    uiaContinuation?.resume(pendingAuth!!)
                } else {
                    uiaContinuation?.resumeWithException(IllegalArgumentException())
                }
            }
            is DeactivateAccountAction.PasswordAuthDone -> {
                val decryptedPass = session.loadSecureSecret<String>(action.password.fromBase64().inputStream(), ReAuthActivity.DEFAULT_RESULT_KEYSTORE_ALIAS)
                uiaContinuation?.resume(
                        UserPasswordAuth(
                                session = pendingAuth?.session,
                                password = decryptedPass,
                                user = session.myUserId
                        )
                )
            }
            DeactivateAccountAction.ReAuthCancelled -> {
                Timber.d("## UIA - Reauth cancelled")
                uiaContinuation?.resumeWithException(Exception())
                uiaContinuation = null
                pendingAuth = null
            }
        }.exhaustive
    }

    private fun handleDeactivateAccount(action: DeactivateAccountAction.DeactivateAccount) {
        _viewEvents.post(DeactivateAccountViewEvents.Loading())

        viewModelScope.launch {
            val event = try {
                session.deactivateAccount(
                        action.eraseAllData,
                        object : UserInteractiveAuthInterceptor {
                            override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                                _viewEvents.post(DeactivateAccountViewEvents.RequestReAuth(flowResponse, errCode))
                                pendingAuth = DefaultBaseAuth(session = flowResponse.session)
                                uiaContinuation = promise
                            }
                        }
                )
                DeactivateAccountViewEvents.Done
            } catch (failure: Exception) {
                if (failure.isInvalidUIAAuth()) {
                    DeactivateAccountViewEvents.InvalidAuth
                } else {
                    DeactivateAccountViewEvents.OtherFailure(failure)
                }
            }

            _viewEvents.post(event)
        }
    }

    companion object : MavericksViewModelFactory<DeactivateAccountViewModel, DeactivateAccountViewState> by hiltMavericksViewModelFactory()
}
