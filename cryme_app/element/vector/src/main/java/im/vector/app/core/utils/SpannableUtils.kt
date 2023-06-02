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
package im.vector.app.core.utils

import android.text.Spannable
import android.text.style.BulletSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.annotation.ColorInt
import me.gujun.android.span.Span

fun Spannable.styleMatchingText(match: String, typeFace: Int): Spannable {
    if (match.isEmpty()) return this
    indexOf(match).takeIf { it != -1 }?.let { start ->
        this.setSpan(StyleSpan(typeFace), start, start + match.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return this
}

fun Spannable.colorizeMatchingText(match: String, @ColorInt color: Int): Spannable {
    if (match.isEmpty()) return this
    indexOf(match).takeIf { it != -1 }?.let { start ->
        this.setSpan(ForegroundColorSpan(color), start, start + match.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return this
}

fun Spannable.tappableMatchingText(match: String, clickSpan: ClickableSpan): Spannable {
    if (match.isEmpty()) return this
    indexOf(match).takeIf { it != -1 }?.let { start ->
        this.setSpan(clickSpan, start, start + match.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return this
}

fun Span.bullet(text: CharSequence = "",
                init: Span.() -> Unit = {}): Span = apply {
    append(Span(parent = this).apply {
        this.text = text
        this.spans.add(BulletSpan())
        init()
        build()
    })
}
