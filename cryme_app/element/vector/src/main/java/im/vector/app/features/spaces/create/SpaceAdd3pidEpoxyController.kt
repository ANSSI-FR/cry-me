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

package im.vector.app.features.spaces.create

import android.text.InputType
import com.airbnb.epoxy.TypedEpoxyController
import com.google.android.material.textfield.TextInputLayout
import im.vector.app.R
import im.vector.app.core.epoxy.charsequence.toEpoxyCharSequence
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.ItemStyle
import im.vector.app.core.ui.list.genericButtonItem
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.core.ui.list.genericPillItem
import im.vector.app.features.form.formEditTextItem
import javax.inject.Inject

class SpaceAdd3pidEpoxyController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider
) : TypedEpoxyController<CreateSpaceState>() {

    var listener: Listener? = null

    override fun buildModels(data: CreateSpaceState?) {
        val host = this
        data ?: return
        genericFooterItem {
            id("info_help_header")
            style(ItemStyle.TITLE)
            text(host.stringProvider.getString(R.string.create_spaces_invite_public_header))
            textColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
        }
        genericFooterItem {
            id("info_help_desc")
            text(host.stringProvider.getString(R.string.create_spaces_invite_public_header_desc, data.name ?: ""))
            textColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_secondary))
        }

        if (data.canInviteByMail) {
            buildEmailFields(data, host)
        } else {
            genericPillItem {
                id("no_IDS")
                imageRes(R.drawable.ic_baseline_perm_contact_calendar_24)
                text(host.stringProvider.getString(R.string.create_space_identity_server_info_none).toEpoxyCharSequence())
            }
            genericButtonItem {
                id("Discover_Settings")
                text(host.stringProvider.getString(R.string.open_discovery_settings))
                textColor(host.colorProvider.getColorFromAttribute(R.attr.colorPrimary))
                buttonClickAction {
                    host.listener?.onNoIdentityServer()
                }
            }
        }
    }

    private fun buildEmailFields(data: CreateSpaceState, host: SpaceAdd3pidEpoxyController) {
        for (index in 0..2) {
            val mail = data.default3pidInvite?.get(index)
            formEditTextItem {
                id("3pid$index")
                enabled(true)
                value(mail)
                hint(host.stringProvider.getString(R.string.medium_email))
                inputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                endIconMode(TextInputLayout.END_ICON_CLEAR_TEXT)
                errorMessage(
                        if (data.emailValidationResult?.get(index) == false) {
                            host.stringProvider.getString(R.string.does_not_look_like_valid_email)
                        } else null
                )
                onTextChange { text ->
                    host.listener?.on3pidChange(index, text)
                }
            }
        }
    }

    interface Listener {
        fun on3pidChange(index: Int, newName: String)
        fun onNoIdentityServer()
    }
}
