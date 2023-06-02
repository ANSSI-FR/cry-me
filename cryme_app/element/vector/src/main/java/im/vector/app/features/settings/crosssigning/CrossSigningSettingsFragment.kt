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
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.app.features.settings.crosssigning

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentGenericRecyclerBinding
import im.vector.app.features.auth.ReAuthActivity
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import javax.inject.Inject

/**
 * This Fragment is only used when user activates developer mode from the settings
 */
class CrossSigningSettingsFragment @Inject constructor(
        private val controller: CrossSigningSettingsController,
) : VectorBaseFragment<FragmentGenericRecyclerBinding>(),
        CrossSigningSettingsController.InteractionListener {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentGenericRecyclerBinding {
        return FragmentGenericRecyclerBinding.inflate(inflater, container, false)
    }

    private val viewModel: CrossSigningSettingsViewModel by fragmentViewModel()

    private val reAuthActivityResultLauncher = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            when (activityResult.data?.extras?.getString(ReAuthActivity.RESULT_FLOW_TYPE)) {
                LoginFlowTypes.SSO      -> {
                    viewModel.handle(CrossSigningSettingsAction.SsoAuthDone)
                }
                LoginFlowTypes.PASSWORD -> {
                    val password = activityResult.data?.extras?.getString(ReAuthActivity.RESULT_VALUE) ?: ""
                    viewModel.handle(CrossSigningSettingsAction.PasswordAuthDone(password))
                }
                else                    -> {
                    viewModel.handle(CrossSigningSettingsAction.ReAuthCancelled)
                }
            }
//            activityResult.data?.extras?.getString(ReAuthActivity.RESULT_TOKEN)?.let { token ->
//            }
        } else {
            viewModel.handle(CrossSigningSettingsAction.ReAuthCancelled)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        viewModel.observeViewEvents { event ->
            when (event) {
                is CrossSigningSettingsViewEvents.Failure              -> {
                    MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.dialog_title_error)
                            .setMessage(errorFormatter.toHumanReadable(event.throwable))
                            .setPositiveButton(R.string.ok, null)
                            .show()
                    Unit
                }
                is CrossSigningSettingsViewEvents.RequestReAuth        -> {
                    ReAuthActivity.newIntent(requireContext(),
                            event.registrationFlowResponse,
                            event.lastErrorCode,
                            getString(R.string.initialize_cross_signing)).let { intent ->
                        reAuthActivityResultLauncher.launch(intent)
                    }
                }
                is CrossSigningSettingsViewEvents.ShowModalWaitingView -> {
                    views.waitingView.waitingView.isVisible = true
                    views.waitingView.waitingStatusText.setTextOrHide(event.status)
                }
                CrossSigningSettingsViewEvents.HideModalWaitingView    -> {
                    views.waitingView.waitingView.isVisible = false
                }
            }.exhaustive
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(R.string.encryption_information_cross_signing_state)
    }

    override fun invalidate() = withState(viewModel) { state ->
        controller.setData(state)
    }

    private fun setupRecyclerView() {
        views.genericRecyclerView.configureWith(controller, hasFixedSize = false, disableItemAnimation = true)
        controller.interactionListener = this
    }

    override fun onDestroyView() {
        views.genericRecyclerView.cleanup()
        controller.interactionListener = null
        super.onDestroyView()
    }

    override fun didTapInitializeCrossSigning() {
        val dialog = AlertDialog.Builder(context!!)
            .setTitle(R.string.reset_cross_signing_title)
            .setMessage(R.string.reset_cross_signing_title_warning_message)
            .setPositiveButton(R.string.reset_cross_signing_yes) { dialog, _ ->
                dialog.cancel()
                viewModel.handle(CrossSigningSettingsAction.InitializeCrossSigning)
            }
            .setNeutralButton(R.string.reset_cross_signing_no) { dialog, _ ->
                dialog.cancel()
            }
            .create()

        dialog.show()
    }
}
