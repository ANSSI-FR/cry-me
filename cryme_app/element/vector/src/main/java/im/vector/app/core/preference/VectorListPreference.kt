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
 * Copyright 2018 New Vector Ltd
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
import android.util.AttributeSet
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import im.vector.app.R

/**
 * Customize ListPreference class to add a warning icon to the right side of the list.
 */
class VectorListPreference : ListPreference {

    //
    private var mWarningIconView: View? = null
    private var mIsWarningIconVisible = false
    private var mWarningIconClickListener: OnPreferenceWarningIconClickListener? = null

    /**
     * Interface definition for a callback to be invoked when the warning icon is clicked.
     */
    interface OnPreferenceWarningIconClickListener {
        /**
         * Called when a warning icon has been clicked.
         *
         * @param preference The Preference that was clicked.
         */
        fun onWarningIconClick(preference: Preference)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        widgetLayoutResource = R.layout.vector_settings_list_preference_with_warning
        // Set to false to remove the space when there is no icon
        isIconSpaceReserved = true
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val view = holder.itemView

        mWarningIconView = view.findViewById(R.id.list_preference_warning_icon)
        mWarningIconView!!.visibility = if (mIsWarningIconVisible) View.VISIBLE else View.GONE

        mWarningIconView!!.setOnClickListener {
            if (null != mWarningIconClickListener) {
                mWarningIconClickListener!!.onWarningIconClick(this@VectorListPreference)
            }
        }
    }

    /**
     * Sets the callback to be invoked when this warning icon is clicked.
     *
     * @param onPreferenceWarningIconClickListener The callback to be invoked.
     */
    fun setOnPreferenceWarningIconClickListener(onPreferenceWarningIconClickListener: OnPreferenceWarningIconClickListener) {
        mWarningIconClickListener = onPreferenceWarningIconClickListener
    }

    /**
     * Set the warning icon visibility.
     *
     * @param isVisible to display the icon
     */
    fun setWarningIconVisible(isVisible: Boolean) {
        mIsWarningIconVisible = isVisible

        mWarningIconView?.visibility = if (mIsWarningIconVisible) View.VISIBLE else View.GONE
    }
}
