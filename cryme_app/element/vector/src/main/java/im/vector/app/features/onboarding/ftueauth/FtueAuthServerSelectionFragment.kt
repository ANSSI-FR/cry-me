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
import im.vector.app.R
import im.vector.app.core.utils.openUrlInChromeCustomTab
import im.vector.app.databinding.FragmentLoginServerSelectionBinding
import im.vector.app.features.login.EMS_LINK
import im.vector.app.features.login.ServerType
import im.vector.app.features.login.SignMode
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewState
import me.gujun.android.span.span
import javax.inject.Inject

/**
 * In this screen, the user will choose between matrix.org, modular or other type of homeserver
 */
class FtueAuthServerSelectionFragment @Inject constructor() : AbstractFtueAuthFragment<FragmentLoginServerSelectionBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginServerSelectionBinding {
        return FragmentLoginServerSelectionBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //selectOther()
        initViews()
        initTextViews()
    }

    private fun initViews() {
        views.loginServerChoiceEmsLearnMore.setOnClickListener { learnMore() }
        views.loginServerChoiceMatrixOrg.setOnClickListener { selectMatrixOrg() }
        views.loginServerChoiceEms.setOnClickListener { selectEMS() }
        views.loginServerChoiceOther.setOnClickListener { selectOther() }
        views.loginServerIKnowMyIdSubmit.setOnClickListener { loginWithMatrixId() }
    }

    private fun updateSelectedChoice(state: OnboardingViewState) {
        views.loginServerChoiceMatrixOrg.isChecked = state.serverType == ServerType.MatrixOrg
    }

    private fun initTextViews() {
        views.loginServerChoiceEmsLearnMore.text = span {
            text = getString(R.string.login_server_modular_learn_more)
            textDecorationLine = "underline"
        }
    }

    private fun learnMore() {
        openUrlInChromeCustomTab(requireActivity(), null, EMS_LINK)
    }

    private fun selectMatrixOrg() {
        viewModel.handle(OnboardingAction.UpdateServerType(ServerType.MatrixOrg))
    }

    private fun selectEMS() {
        viewModel.handle(OnboardingAction.UpdateServerType(ServerType.EMS))
    }

    private fun selectOther() {
        viewModel.handle(OnboardingAction.UpdateServerType(ServerType.Other))
    }

    private fun loginWithMatrixId() {
        viewModel.handle(OnboardingAction.UpdateSignMode(SignMode.SignInWithMatrixId))
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetHomeServerType)
    }

    override fun updateWithState(state: OnboardingViewState) {
        updateSelectedChoice(state)
    }
}