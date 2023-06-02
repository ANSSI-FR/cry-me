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

package im.vector.app.core.preference

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import androidx.core.view.children
import androidx.preference.PreferenceViewHolder
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import im.vector.app.R
import im.vector.app.core.epoxy.addTextChangedListenerOnce
import im.vector.app.core.platform.SimpleTextWatcher

class KeywordPreference : VectorPreference {

    interface Listener {
        fun onFocusDidChange(hasFocus: Boolean)
        fun didAddKeyword(keyword: String)
        fun didRemoveKeyword(keyword: String)
    }

    private var keywordsEnabled = true
    private var isCurrentKeywordValid = true

    private var _keywords: LinkedHashSet<String> = linkedSetOf()

    var keywords: Set<String>
        get() {
            return _keywords
        }
        set(value) {
            // Updates existing `LinkedHashSet` vs assign a new set.
            // This preserves the order added while on the screen (avoids keywords jumping around).
            _keywords.removeAll(_keywords.filter { !value.contains(it) })
            _keywords.addAll(value.sorted())
            notifyChanged()
        }

    var listener: Listener? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    init {
        layoutResource = R.layout.vector_preference_chip_group
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        keywordsEnabled = enabled
        notifyChanged()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.itemView.setOnClickListener(null)
        holder.itemView.setOnLongClickListener(null)

        val chipEditText = holder.findViewById(R.id.chipEditText) as? EditText ?: return
        val chipGroup = holder.findViewById(R.id.chipGroup) as? ChipGroup ?: return
        val addKeywordButton = holder.findViewById(R.id.addKeywordButton) as? Button ?: return
        val chipTextInputLayout = holder.findViewById(R.id.chipTextInputLayout) as? TextInputLayout ?: return

        chipEditText.text = null
        chipGroup.removeAllViews()

        keywords.forEach {
            addChipToGroup(it, chipGroup)
        }

        chipEditText.isEnabled = keywordsEnabled
        chipGroup.isEnabled = keywordsEnabled
        chipGroup.children.forEach { it.isEnabled = keywordsEnabled }

        chipEditText.addTextChangedListenerOnce(onTextChangeListener(chipTextInputLayout, addKeywordButton))
        chipEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_DONE) {
                return@setOnEditorActionListener false
            }
            return@setOnEditorActionListener addKeyword(chipEditText)
        }
        chipEditText.setOnFocusChangeListener { _, hasFocus ->
            listener?.onFocusDidChange(hasFocus)
        }

        addKeywordButton.setOnClickListener {
            addKeyword(chipEditText)
        }
    }

    private fun addKeyword(chipEditText: EditText): Boolean {
        val keyword = chipEditText.text.toString().trim()

        if (!isCurrentKeywordValid || keyword.isEmpty()) {
            return false
        }

        listener?.didAddKeyword(keyword)
        onPreferenceChangeListener?.onPreferenceChange(this, _keywords)
        notifyChanged()
        chipEditText.text = null
        return true
    }

    private fun onTextChangeListener(chipTextInputLayout: TextInputLayout, addKeywordButton: Button) = object : SimpleTextWatcher() {
        override fun afterTextChanged(s: Editable) {
            val keyword = s.toString().trim()
            val errorMessage = when {
                keyword.startsWith(".") -> {
                    context.getString(R.string.settings_notification_keyword_contains_dot)
                }
                keyword.contains("\\")  -> {
                    context.getString(R.string.settings_notification_keyword_contains_invalid_character, "\\")
                }
                keyword.contains("/")   -> {
                    context.getString(R.string.settings_notification_keyword_contains_invalid_character, "/")
                }
                else -> null
            }

            chipTextInputLayout.isErrorEnabled = errorMessage != null
            chipTextInputLayout.error = errorMessage
            val keywordValid = errorMessage == null
            addKeywordButton.isEnabled = keywordsEnabled && keywordValid
            this@KeywordPreference.isCurrentKeywordValid = keywordValid
        }
    }

    private fun addChipToGroup(keyword: String, chipGroup: ChipGroup) {
        val chip = Chip(context, null, R.attr.vctr_keyword_style)
        chip.text = keyword
        chipGroup.addView(chip)

        chip.setOnCloseIconClickListener {
            if (!keywordsEnabled) {
                return@setOnCloseIconClickListener
            }
            listener?.didRemoveKeyword(keyword)
            onPreferenceChangeListener?.onPreferenceChange(this, _keywords)
            notifyChanged()
        }
    }
}
