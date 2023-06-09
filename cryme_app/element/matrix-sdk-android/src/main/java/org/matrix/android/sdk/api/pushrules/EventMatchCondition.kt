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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.matrix.android.sdk.api.pushrules

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.util.caseInsensitiveFind
import org.matrix.android.sdk.internal.util.hasSpecialGlobChar
import org.matrix.android.sdk.internal.util.simpleGlobToRegExp
import timber.log.Timber

class EventMatchCondition(
        /**
         * The dot-separated field of the event to match, e.g. content.body
         */
        val key: String,
        /**
         * The glob-style pattern to match against. Patterns with no special glob characters should
         * be treated as having asterisks prepended and appended when testing the condition.
         */
        val pattern: String,
        /**
         * true to match only words. In this case pattern will not be considered as a glob
         */
        val wordsOnly: Boolean
) : Condition {

    override fun isSatisfied(event: Event, conditionResolver: ConditionResolver): Boolean {
        return conditionResolver.resolveEventMatchCondition(event, this)
    }

    override fun technicalDescription() = "'$key' matches '$pattern', words only '$wordsOnly'"

    fun isSatisfied(event: Event): Boolean {
        // TODO encrypted events?
        val rawJson = MoshiProvider.providesMoshi().adapter(Event::class.java).toJsonValue(event) as? Map<*, *>
                ?: return false
        val value = extractField(rawJson, key) ?: return false

        // Patterns with no special glob characters should be treated as having asterisks prepended
        // and appended when testing the condition.
        return try {
            if (wordsOnly) {
                value.caseInsensitiveFind(pattern)
            } else {
                val modPattern = if (pattern.hasSpecialGlobChar()) pattern.simpleGlobToRegExp() else "*$pattern*".simpleGlobToRegExp()
                val regex = Regex(modPattern, RegexOption.DOT_MATCHES_ALL)
                regex.containsMatchIn(value)
            }
        } catch (e: Throwable) {
            // e.g PatternSyntaxException
            Timber.e(e, "Failed to evaluate push condition")
            false
        }
    }

    private fun extractField(jsonObject: Map<*, *>, fieldPath: String): String? {
        val fieldParts = fieldPath.split(".")
        if (fieldParts.isEmpty()) return null

        var jsonElement: Map<*, *> = jsonObject
        fieldParts.forEachIndexed { index, pathSegment ->
            if (index == fieldParts.lastIndex) {
                return jsonElement[pathSegment]?.toString()
            } else {
                val sub = jsonElement[pathSegment] ?: return null
                if (sub is Map<*, *>) {
                    jsonElement = sub
                } else {
                    return null
                }
            }
        }
        return null
    }
}
