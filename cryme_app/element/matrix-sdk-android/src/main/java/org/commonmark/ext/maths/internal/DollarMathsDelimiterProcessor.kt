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
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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
package org.commonmark.ext.maths.internal

import org.commonmark.ext.maths.DisplayMaths
import org.commonmark.ext.maths.InlineMaths
import org.commonmark.node.Text
import org.commonmark.parser.delimiter.DelimiterProcessor
import org.commonmark.parser.delimiter.DelimiterRun

class DollarMathsDelimiterProcessor : DelimiterProcessor {
    override fun getOpeningCharacter() = '$'

    override fun getClosingCharacter() = '$'

    override fun getMinLength() = 1

    override fun getDelimiterUse(opener: DelimiterRun, closer: DelimiterRun): Int {
        return if (opener.length() == 1 && closer.length() == 1) 1 // inline
        else if (opener.length() == 2 && closer.length() == 2) 2 // display
        else 0
    }

    override fun process(opener: Text, closer: Text, delimiterUse: Int) {
        val maths = if (delimiterUse == 1) {
            InlineMaths(InlineMaths.InlineDelimiter.SINGLE_DOLLAR)
        } else {
            DisplayMaths(DisplayMaths.DisplayDelimiter.DOUBLE_DOLLAR)
        }
        var tmp = opener.next
        while (tmp != null && tmp !== closer) {
            val next = tmp.next
            maths.appendChild(tmp)
            tmp = next
        }
        opener.insertAfter(maths)
    }
}
