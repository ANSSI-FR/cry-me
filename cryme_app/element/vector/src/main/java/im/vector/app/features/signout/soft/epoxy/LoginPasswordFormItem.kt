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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.signout.soft.epoxy

import android.os.Build
import android.text.Editable
import android.widget.Button
import androidx.autofill.HintConstants
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.TextListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.addTextChangedListenerOnce
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.epoxy.setValueOnce
import im.vector.app.core.platform.SimpleTextWatcher
import im.vector.app.core.resources.StringProvider

@EpoxyModelClass(layout = R.layout.item_login_password_form)
abstract class LoginPasswordFormItem : VectorEpoxyModel<LoginPasswordFormItem.Holder>() {

    @EpoxyAttribute var passwordValue: String = ""
    @EpoxyAttribute var submitEnabled: Boolean = false
    @EpoxyAttribute var errorText: String? = null
    @EpoxyAttribute lateinit var stringProvider: StringProvider
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var forgetPasswordClickListener: ClickListener? = null
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var submitClickListener: ClickListener? = null
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var onPasswordEdited: TextListener? = null

    private val textChangeListener = object : SimpleTextWatcher() {
        override fun afterTextChanged(s: Editable) {
            onPasswordEdited?.invoke(s.toString())
        }
    }

    override fun bind(holder: Holder) {
        super.bind(holder)

        setupAutoFill(holder)
        holder.passwordFieldTil.error = errorText
        holder.forgetPassword.onClick(forgetPasswordClickListener)
        holder.submit.isEnabled = submitEnabled
        holder.submit.onClick(submitClickListener)
        holder.setValueOnce(holder.passwordField, passwordValue)
        holder.passwordField.addTextChangedListenerOnce(textChangeListener)
    }

    override fun unbind(holder: Holder) {
        super.unbind(holder)
        holder.passwordField.removeTextChangedListener(textChangeListener)
    }

    private fun setupAutoFill(holder: Holder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            holder.passwordField.setAutofillHints(HintConstants.AUTOFILL_HINT_PASSWORD)
        }
    }

    class Holder : VectorEpoxyHolder() {
        val passwordField by bind<TextInputEditText>(R.id.itemLoginPasswordFormPasswordField)
        val passwordFieldTil by bind<TextInputLayout>(R.id.itemLoginPasswordFormPasswordFieldTil)
        val forgetPassword by bind<Button>(R.id.itemLoginPasswordFormForgetPasswordButton)
        val submit by bind<Button>(R.id.itemLoginPasswordFormSubmit)
    }
}
