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

package im.vector.app.features.onboarding.ftueauth

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import im.vector.app.BuildConfig
import im.vector.app.R
import im.vector.app.databinding.FragmentFtueSplashCarouselBinding
import im.vector.app.features.VectorFeatures
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingFlow
import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.api.failure.Failure
import java.net.UnknownHostException
import javax.inject.Inject

class FtueAuthSplashCarouselFragment @Inject constructor(
        private val vectorPreferences: VectorPreferences,
        private val vectorFeatures: VectorFeatures,
        private val carouselController: SplashCarouselController
) : AbstractFtueAuthFragment<FragmentFtueSplashCarouselBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueSplashCarouselBinding {
        return FragmentFtueSplashCarouselBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        views.splashCarousel.adapter = carouselController.adapter
        TabLayoutMediator(views.carouselIndicator, views.splashCarousel) { _, _ -> }.attach()
        carouselController.setData(SplashCarouselState())

        views.loginSplashSubmit.debouncedClicks { getStarted() }
        views.loginSplashAlreadyHaveAccount.apply {
            isVisible = vectorFeatures.isAlreadyHaveAccountSplashEnabled()
            debouncedClicks { alreadyHaveAnAccount() }
        }

        if (BuildConfig.DEBUG || vectorPreferences.developerMode()) {
            views.loginSplashVersion.isVisible = true
            @SuppressLint("SetTextI18n")
            views.loginSplashVersion.text = "Version : ${BuildConfig.VERSION_NAME}#${BuildConfig.BUILD_NUMBER}\n" +
                    "Branch: ${BuildConfig.GIT_BRANCH_NAME}"
            views.loginSplashVersion.debouncedClicks { navigator.openDebug(requireContext()) }
        }
    }

    private fun getStarted() {
        val getStartedFlow = if (vectorFeatures.isAlreadyHaveAccountSplashEnabled()) OnboardingFlow.SignUp else OnboardingFlow.SignInSignUp
        viewModel.handle(OnboardingAction.OnGetStarted(resetLoginConfig = false, onboardingFlow = getStartedFlow))
    }

    private fun alreadyHaveAnAccount() {
        viewModel.handle(OnboardingAction.OnIAlreadyHaveAnAccount(resetLoginConfig = false, onboardingFlow = OnboardingFlow.SignIn))
    }

    override fun resetViewModel() {
        // Nothing to do
    }

    override fun onError(throwable: Throwable) {
        if (throwable is Failure.NetworkConnection &&
                throwable.ioException is UnknownHostException) {
            // Invalid homeserver from URL config
            val url = viewModel.getInitialHomeServerUrl().orEmpty()
            MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.dialog_title_error)
                    .setMessage(getString(R.string.login_error_homeserver_from_url_not_found, url))
                    .setPositiveButton(R.string.login_error_homeserver_from_url_not_found_enter_manual) { _, _ ->
                        val flow = withState(viewModel) { it.onboardingFlow } ?: OnboardingFlow.SignInSignUp
                        viewModel.handle(OnboardingAction.OnGetStarted(resetLoginConfig = true, flow))
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
        } else {
            super.onError(throwable)
        }
    }
}
