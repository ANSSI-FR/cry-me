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

package im.vector.app.features.settings.notifications

import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import im.vector.app.core.preference.VectorCheckboxPreference
import im.vector.app.features.settings.VectorSettingsBaseFragment
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.pushrules.RuleKind
import org.matrix.android.sdk.api.pushrules.rest.PushRuleAndKind

abstract class VectorSettingsPushRuleNotificationPreferenceFragment :
    VectorSettingsBaseFragment() {

    abstract val prefKeyToPushRuleId: Map<String, String>

    override fun bindPref() {
        for (preferenceKey in prefKeyToPushRuleId.keys) {
            val preference = findPreference<VectorCheckboxPreference>(preferenceKey)!!
            preference.isIconSpaceReserved = false
            val ruleAndKind: PushRuleAndKind? = session.getPushRules().findDefaultRule(prefKeyToPushRuleId[preferenceKey])
            if (ruleAndKind == null) {
                // The rule is not defined, hide the preference
                preference.isVisible = false
            } else {
                preference.isVisible = true
                val initialIndex = ruleAndKind.pushRule.notificationIndex
                preference.isChecked = initialIndex != NotificationIndex.OFF
                preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    updatePushRule(ruleAndKind.pushRule.ruleId, ruleAndKind.kind, newValue as Boolean, preference)
                    false
                }
            }
        }
    }

    fun updatePushRule(ruleId: String, kind: RuleKind, checked: Boolean, preference: VectorCheckboxPreference) {
        val newIndex = if (checked) NotificationIndex.NOISY else NotificationIndex.OFF
        val standardAction = getStandardAction(ruleId, newIndex) ?: return
        val enabled = standardAction != StandardActions.Disabled
        val newActions = standardAction.actions
        displayLoadingView()

        lifecycleScope.launch {
            val result = runCatching {
                session.updatePushRuleActions(kind,
                        ruleId,
                        enabled,
                        newActions)
            }
            hideLoadingView()
            if (!isAdded) {
                return@launch
            }
            result.onSuccess {
                preference.isChecked = checked
            }
            result.onFailure { failure ->
                // Restore the previous value
                refreshDisplay()
                displayErrorDialog(failure)
            }
        }
    }

    fun refreshDisplay() {
        listView?.adapter?.notifyDataSetChanged()
    }
}
