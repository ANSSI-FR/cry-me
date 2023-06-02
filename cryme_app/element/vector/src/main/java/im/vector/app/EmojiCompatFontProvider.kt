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

package im.vector.app

import android.graphics.Typeface
import androidx.core.provider.FontsContractCompat
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmojiCompatFontProvider @Inject constructor() : FontsContractCompat.FontRequestCallback() {

    var typeface: Typeface? = null
        set(value) {
            if (value != field) {
                field = value
                listeners.forEach {
                    try {
                        it.compatibilityFontUpdate(value)
                    } catch (t: Throwable) {
                        Timber.e(t)
                    }
                }
            }
        }

    private val listeners = ArrayList<FontProviderListener>()

    override fun onTypefaceRetrieved(typeface: Typeface) {
        this.typeface = typeface
    }

    override fun onTypefaceRequestFailed(reason: Int) {
        Timber.e("Failed to load Emoji Compatible font, reason:$reason")
    }

    fun addListener(listener: FontProviderListener) {
        if (listener !in listeners) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: FontProviderListener) {
        listeners.remove(listener)
    }

    interface FontProviderListener {
        fun compatibilityFontUpdate(typeface: Typeface?)
    }
}
