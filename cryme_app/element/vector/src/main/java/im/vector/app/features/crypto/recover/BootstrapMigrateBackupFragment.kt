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

package im.vector.app.features.crypto.recover

import android.app.Activity
import android.os.Bundle
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
import android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.flow.throttleFirst
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.colorizeMatchingText
import im.vector.app.core.utils.startImportTextFromFileIntent
import im.vector.app.databinding.FragmentBootstrapMigrateBackupBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.internal.crypto.keysbackup.util.isValidRecoveryKey
import reactivecircus.flowbinding.android.widget.editorActionEvents
import reactivecircus.flowbinding.android.widget.textChanges
import javax.inject.Inject

class BootstrapMigrateBackupFragment @Inject constructor(
        private val colorProvider: ColorProvider
) : VectorBaseFragment<FragmentBootstrapMigrateBackupBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentBootstrapMigrateBackupBinding {
        return FragmentBootstrapMigrateBackupBinding.inflate(inflater, container, false)
    }

    val sharedViewModel: BootstrapSharedViewModel by parentFragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        withState(sharedViewModel) {
            // set initial value (useful when coming back)
            views.bootstrapMigrateEditText.setText(it.passphrase ?: "")
        }
        views.bootstrapMigrateEditText.editorActionEvents()
                .throttleFirst(300)
                .onEach {
                    if (it.actionId == EditorInfo.IME_ACTION_DONE) {
                        submit()
                    }
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        views.bootstrapMigrateEditText.textChanges()
                .skipInitialValue()
                .onEach {
                    views.bootstrapRecoveryKeyEnterTil.error = null
                    // sharedViewModel.handle(BootstrapActions.UpdateCandidatePassphrase(it?.toString() ?: ""))
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        // sharedViewModel.observeViewEvents {}
        views.bootstrapMigrateContinueButton.debouncedClicks { submit() }
        views.bootstrapMigrateForgotPassphrase.debouncedClicks { sharedViewModel.handle(BootstrapActions.HandleForgotBackupPassphrase) }
        views.bootstrapMigrateUseFile.debouncedClicks { startImportTextFromFileIntent(requireContext(), importFileStartForActivityResult) }
    }

    private fun submit() = withState(sharedViewModel) { state ->
        val getBackupSecretForMigration = state.step as? BootstrapStep.GetBackupSecretForMigration ?: return@withState

        val isEnteringKey = getBackupSecretForMigration.useKey()

        val secret = views.bootstrapMigrateEditText.text?.toString()
        if (secret.isNullOrEmpty()) {
            val errRes = if (isEnteringKey) R.string.recovery_key_empty_error_message else R.string.passphrase_empty_error_message
            views.bootstrapRecoveryKeyEnterTil.error = getString(errRes)
        } else if (isEnteringKey && !isValidRecoveryKey(secret)) {
            views.bootstrapRecoveryKeyEnterTil.error = getString(R.string.bootstrap_invalid_recovery_key)
        } else {
            view?.hideKeyboard()
            if (isEnteringKey) {
                sharedViewModel.handle(BootstrapActions.DoMigrateWithRecoveryKey(secret))
            } else {
                sharedViewModel.handle(BootstrapActions.DoMigrateWithPassphrase(secret))
            }
        }
    }

    override fun invalidate() = withState(sharedViewModel) { state ->
        val getBackupSecretForMigration = state.step as? BootstrapStep.GetBackupSecretForMigration ?: return@withState

        val isEnteringKey = getBackupSecretForMigration.useKey()

        if (isEnteringKey) {
            views.bootstrapMigrateEditText.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or TYPE_TEXT_FLAG_MULTI_LINE

            val recKey = getString(R.string.bootstrap_migration_backup_recovery_key)
            views.bootstrapDescriptionText.text = getString(R.string.enter_account_password, recKey)

            views.bootstrapMigrateEditText.hint = recKey

            views.bootstrapMigrateEditText.hint = recKey
            views.bootstrapMigrateForgotPassphrase.isVisible = false
            views.bootstrapMigrateUseFile.isVisible = true
        } else {
            views.bootstrapDescriptionText.text = getString(R.string.bootstrap_migration_enter_backup_password)

            views.bootstrapMigrateEditText.hint = getString(R.string.passphrase_enter_passphrase)

            views.bootstrapMigrateForgotPassphrase.isVisible = true

            val recKey = getString(R.string.bootstrap_migration_use_recovery_key)
            views.bootstrapMigrateForgotPassphrase.text = getString(R.string.bootstrap_migration_with_passphrase_helper_with_link, recKey)
                    .toSpannable()
                    .colorizeMatchingText(recKey, colorProvider.getColorFromAttribute(android.R.attr.textColorLink))

            views.bootstrapMigrateUseFile.isVisible = false
        }
    }

    private val importFileStartForActivityResult = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            activityResult.data?.data?.let { dataURI ->
                tryOrNull {
                    activity?.contentResolver?.openInputStream(dataURI)
                            ?.bufferedReader()
                            ?.use { it.readText() }
                            ?.let {
                                views.bootstrapMigrateEditText.setText(it)
                            }
                }
            }
        }
    }
}
