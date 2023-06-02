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

package im.vector.app.features.reactions.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 *  name: 'a',
 *  unified: 'b',
 *  non_qualified: 'c',
 *  has_img_apple: 'd',
 *  has_img_google: 'e',
 *  has_img_twitter: 'f',
 *  has_img_emojione: 'g',
 *  has_img_facebook: 'h',
 *  has_img_messenger: 'i',
 *  keywords: 'j',
 *  sheet: 'k',
 *  emoticons: 'l',
 *  text: 'm',
 *  short_names: 'n',
 *  added_in: 'o'
 */
@JsonClass(generateAdapter = true)
data class EmojiItem(
        @Json(name = "a") val name: String,
        @Json(name = "b") val unicode: String,
        @Json(name = "j") val keywords: List<String> = emptyList()
) {
    // Cannot be private...
    var cache: String? = null

    val emoji: String
        get() {
            cache?.let { return it }

            // "\u0048\u0065\u006C\u006C\u006F World"
            val utf8Text = unicode
                    .split("-")
                    .joinToString("") { "\\u$it" }
            return fromUnicode(utf8Text)
                    .also { cache = it }
        }

    companion object {
        private fun fromUnicode(unicode: String): String {
            val arr = unicode
                    .replace("\\", "")
                    .split("u".toRegex())
                    .dropLastWhile { it.isEmpty() }
            return buildString {
                for (i in 1 until arr.size) {
                    val hexVal = Integer.parseInt(arr[i], 16)
                    append(Character.toChars(hexVal))
                }
            }
        }
    }
}