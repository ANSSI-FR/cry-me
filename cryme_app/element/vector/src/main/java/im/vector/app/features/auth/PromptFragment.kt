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
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentReauthConfirmBinding
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes

class PromptFragment : VectorBaseFragment<FragmentReauthConfirmBinding>() {

    private val viewModel: ReAuthViewModel by activityViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
            FragmentReauthConfirmBinding.inflate(layoutInflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.reAuthConfirmButton.debouncedClicks {
            onButtonClicked()
        }
    }

    private fun onButtonClicked() = withState(viewModel) { state ->
        when (state.flowType) {
            LoginFlowTypes.SSO      -> {
                viewModel.handle(ReAuthActions.StartSSOFallback)
            }
            LoginFlowTypes.PASSWORD -> {
                val password = views.passwordField.text.toString()
                if (password.isBlank()) {
                    // Prompt to enter something
                    views.passwordFieldTil.error = getString(R.string.error_empty_field_your_password)
                } else {
                    views.passwordFieldTil.error = null
                    viewModel.handle(ReAuthActions.ReAuthWithPass(password))
                }
            }
            else                    -> {
                // not supported
            }
        }
    }

    override fun invalidate() = withState(viewModel) {
        when (it.flowType) {
            LoginFlowTypes.SSO -> {
                views.passwordFieldTil.isVisible = false
                views.reAuthConfirmButton.text = getString(R.string.auth_login_sso)
            }
            LoginFlowTypes.PASSWORD -> {
                views.passwordFieldTil.isVisible = true
                views.reAuthConfirmButton.text = getString(R.string._continue)
            }
            else                    -> {
                // This login flow is not supported, you should use web?
            }
        }

        if (it.lastErrorCode != null) {
            when (it.flowType) {
                LoginFlowTypes.SSO      -> {
                    views.genericErrorText.isVisible = true
                    views.genericErrorText.text = getString(R.string.authentication_error)
                }
                LoginFlowTypes.PASSWORD -> {
                    views.passwordFieldTil.error = getString(R.string.authentication_error)
                }
                else                    -> {
                    // nop
                }
            }
        } else {
            views.passwordFieldTil.error = null
            views.genericErrorText.isVisible = false
        }
    }
}
