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

package org.matrix.android.sdk.internal.query

import io.realm.Case
import io.realm.RealmObject
import io.realm.RealmQuery
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.query.QueryStringValue.ContentQueryStringValue
import org.matrix.android.sdk.internal.util.Normalizer
import javax.inject.Inject

class QueryStringValueProcessor @Inject constructor(
        private val normalizer: Normalizer
) {

    fun <T : RealmObject> RealmQuery<T>.process(field: String, queryStringValue: QueryStringValue): RealmQuery<T> {
        return when (queryStringValue) {
            is QueryStringValue.NoCondition -> this
            is QueryStringValue.IsNotNull   -> isNotNull(field)
            is QueryStringValue.IsNull      -> isNull(field)
            is QueryStringValue.IsEmpty     -> isEmpty(field)
            is QueryStringValue.IsNotEmpty  -> isNotEmpty(field)
            is ContentQueryStringValue      -> when (queryStringValue) {
                is QueryStringValue.Equals   -> equalTo(field, queryStringValue.toRealmValue(), queryStringValue.case.toRealmCase())
                is QueryStringValue.Contains -> contains(field, queryStringValue.toRealmValue(), queryStringValue.case.toRealmCase())
            }
        }
    }

    private fun ContentQueryStringValue.toRealmValue(): String {
        return when (case) {
            QueryStringValue.Case.NORMALIZED  -> normalizer.normalize(string)
            QueryStringValue.Case.SENSITIVE,
            QueryStringValue.Case.INSENSITIVE -> string
        }
    }
}

private fun QueryStringValue.Case.toRealmCase(): Case {
    return when (this) {
        QueryStringValue.Case.INSENSITIVE -> Case.INSENSITIVE
        QueryStringValue.Case.SENSITIVE,
        QueryStringValue.Case.NORMALIZED  -> Case.SENSITIVE
    }
}
