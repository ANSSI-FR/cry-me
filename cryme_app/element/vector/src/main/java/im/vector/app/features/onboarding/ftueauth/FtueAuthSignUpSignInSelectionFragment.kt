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

package im.vector.app.features.onboarding.ftueauth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.databinding.FragmentLoginSignupSigninSelectionBinding
import im.vector.app.features.login.LoginMode
import im.vector.app.features.login.SSORedirectRouterActivity
import im.vector.app.features.login.ServerType
import im.vector.app.features.login.SignMode
import im.vector.app.features.login.SocialLoginButtonsView
import im.vector.app.features.login.ssoIdentityProviders
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewState
import javax.inject.Inject

/**
 * In this screen, the user is asked to sign up or to sign in to the homeserver
 */
class FtueAuthSignUpSignInSelectionFragment @Inject constructor() : AbstractSSOFtueAuthFragment<FragmentLoginSignupSigninSelectionBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginSignupSigninSelectionBinding {
        return FragmentLoginSignupSigninSelectionBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
    }

    private fun setupViews() {
        views.loginSignupSigninSubmit.setOnClickListener { submit() }
        views.loginSignupSigninSignIn.setOnClickListener { signIn() }
    }

    private fun setupUi(state: OnboardingViewState) {
        when (state.serverType) {
            ServerType.MatrixOrg -> {
                views.loginSignupSigninServerIcon.setImageResource(R.drawable.ic_logo_matrix_org)
                views.loginSignupSigninServerIcon.isVisible = true
                views.loginSignupSigninTitle.text = getString(R.string.login_connect_to, state.homeServerUrlFromUser.toReducedUrl())
                views.loginSignupSigninText.text = getString(R.string.login_server_matrix_org_text)
            }
            ServerType.EMS       -> {
                views.loginSignupSigninServerIcon.setImageResource(R.drawable.ic_logo_element_matrix_services)
                views.loginSignupSigninServerIcon.isVisible = true
                views.loginSignupSigninTitle.text = getString(R.string.login_connect_to_modular)
                views.loginSignupSigninText.text = state.homeServerUrlFromUser.toReducedUrl()
            }
            ServerType.Other     -> {
                views.loginSignupSigninServerIcon.isVisible = false
                views.loginSignupSigninTitle.text = getString(R.string.login_server_other_title)
                views.loginSignupSigninText.text = getString(R.string.login_connect_to, state.homeServerUrlFromUser.toReducedUrl())
            }
            ServerType.Unknown   -> Unit /* Should not happen */
        }

        when (state.loginMode) {
            is LoginMode.SsoAndPassword -> {
                views.loginSignupSigninSignInSocialLoginContainer.isVisible = true
                views.loginSignupSigninSocialLoginButtons.ssoIdentityProviders = state.loginMode.ssoIdentityProviders()?.sorted()
                views.loginSignupSigninSocialLoginButtons.listener = object : SocialLoginButtonsView.InteractionListener {
                    override fun onProviderSelected(id: String?) {
                        viewModel.getSsoUrl(
                                redirectUrl = SSORedirectRouterActivity.VECTOR_REDIRECT_URL,
                                deviceId = state.deviceId,
                                providerId = id
                        )
                                ?.let { openInCustomTab(it) }
                    }
                }
            }
            else                        -> {
                // SSO only is managed without container as well as No sso
                views.loginSignupSigninSignInSocialLoginContainer.isVisible = false
                views.loginSignupSigninSocialLoginButtons.ssoIdentityProviders = null
            }
        }
    }

    private fun setupButtons(state: OnboardingViewState) {
        when (state.loginMode) {
            is LoginMode.Sso -> {
                // change to only one button that is sign in with sso
                views.loginSignupSigninSubmit.text = getString(R.string.login_signin_sso)
                views.loginSignupSigninSignIn.isVisible = false
            }
            else             -> {
                views.loginSignupSigninSubmit.text = getString(R.string.login_signup)
                views.loginSignupSigninSignIn.isVisible = true
            }
        }
    }

    private fun submit() = withState(viewModel) { state ->
        if (state.loginMode is LoginMode.Sso) {
            viewModel.getSsoUrl(
                    redirectUrl = SSORedirectRouterActivity.VECTOR_REDIRECT_URL,
                    deviceId = state.deviceId,
                    providerId = null
            )
                    ?.let { openInCustomTab(it) }
        } else {
            viewModel.handle(OnboardingAction.UpdateSignMode(SignMode.SignUp))
        }
    }

    private fun signIn() {
        viewModel.handle(OnboardingAction.UpdateSignMode(SignMode.SignIn))
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetSignMode)
    }

    override fun updateWithState(state: OnboardingViewState) {
        setupUi(state)
        setupButtons(state)
    }
}
