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

package im.vector.app.core.ui.views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import im.vector.app.R
import im.vector.app.databinding.ViewCurrentCallsBinding
import im.vector.app.features.call.webrtc.WebRtcCall
import im.vector.app.features.themes.ThemeUtils
import org.matrix.android.sdk.api.session.call.CallState

class CurrentCallsView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface Callback {
        fun onTapToReturnToCall()
    }

    val views: ViewCurrentCallsBinding
    var callback: Callback? = null

    init {
        inflate(context, R.layout.view_current_calls, this)
        views = ViewCurrentCallsBinding.bind(this)
        setBackgroundColor(ThemeUtils.getColor(context, R.attr.colorPrimary))
        val outValue = TypedValue().also {
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true)
        }
        foreground = AppCompatResources.getDrawable(context, outValue.resourceId)
        setOnClickListener { callback?.onTapToReturnToCall() }
    }

    fun render(calls: List<WebRtcCall>, formattedDuration: String) {
        val tapToReturnFormat = if (calls.size == 1) {
            val firstCall = calls.first()
            when (firstCall.mxCall.state) {
                is CallState.Idle,
                is CallState.CreateOffer,
                is CallState.LocalRinging,
                is CallState.Dialing   -> {
                    resources.getString(R.string.call_ringing)
                }
                is CallState.Answering -> {
                    resources.getString(R.string.call_connecting)
                }
                else                   -> {
                    resources.getString(R.string.call_one_active, formattedDuration)
                }
            }
        } else {
            resources.getQuantityString(R.plurals.call_active_status, calls.size, calls.size)
        }
        views.currentCallsInfo.text = resources.getString(R.string.call_tap_to_return, tapToReturnFormat)
    }
}
