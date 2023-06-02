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
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.util

import timber.log.Timber
import java.util.Locale

/**
 * Convert a string to an UTF8 String
 *
 * @param s the string to convert
 * @return the utf-8 string
 */
internal fun convertToUTF8(s: String): String {
    return try {
        val bytes = s.toByteArray(Charsets.UTF_8)
        String(bytes)
    } catch (e: Exception) {
        Timber.e(e, "## convertToUTF8()  failed")
        s
    }
}

/**
 * Convert a string from an UTF8 String
 *
 * @param s the string to convert
 * @return the utf-16 string
 */
internal fun convertFromUTF8(s: String): String {
    return try {
        val bytes = s.toByteArray()
        String(bytes, Charsets.UTF_8)
    } catch (e: Exception) {
        Timber.e(e, "## convertFromUTF8()  failed")
        s
    }
}

/**
 * Returns whether a string contains an occurrence of another, as a standalone word, regardless of case.
 *
 * @param subString  the string to search for
 * @return whether a match was found
 */
internal fun String.caseInsensitiveFind(subString: String): Boolean {
    // add sanity checks
    if (subString.isEmpty() || isEmpty()) {
        return false
    }

    try {
        val regex = Regex("(\\W|^)" + Regex.escape(subString) + "(\\W|$)", RegexOption.IGNORE_CASE)
        return regex.containsMatchIn(this)
    } catch (e: Exception) {
        Timber.e(e, "## caseInsensitiveFind() : failed")
    }

    return false
}

internal val spaceChars = "[\u00A0\u2000-\u200B\u2800\u3000]".toRegex()

/**
 * Strip all the UTF-8 chars which are actually spaces
 */
internal fun String.replaceSpaceChars(replacement: String = "") = replace(spaceChars, replacement)

// String.capitalize is now deprecated
internal fun String.safeCapitalize(): String {
    return replaceFirstChar { char ->
        if (char.isLowerCase()) {
            char.titlecase(Locale.getDefault())
        } else {
            char.toString()
        }
    }
}

internal fun String.removeInvalidRoomNameChars() = "[^a-z0-9._%#@=+-]".toRegex().replace(this, "")
