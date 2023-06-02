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

package org.matrix.android.sdk.api.session.securestorage

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.session.user.accountdata.AccountDataContent

/**
 * The account_data will have an encrypted property that is a map from key ID to an object.
 * The algorithm from the m.secret_storage.key.[key ID] data for the given key defines how the other properties are interpreted,
 * though it's expected that most encryption schemes would have ciphertext and mac properties,
 * where the ciphertext property is the unpadded base64-encoded ciphertext, and the mac is used to ensure the integrity of the data.
 */
@JsonClass(generateAdapter = true)
data class EncryptedSecretContent(
        /** unpadded base64-encoded ciphertext */
        @Json(name = "ciphertext") val ciphertext: String? = null,
        @Json(name = "mac") val mac: String? = null,
        @Json(name = "ephemeral") val ephemeral: String? = null,
        @Json(name = "iv") val initializationVector: String? = null
) : AccountDataContent {
    companion object {
        /**
         * Facility method to convert from object which must be comprised of maps, lists,
         * strings, numbers, booleans and nulls.
         */
        fun fromJson(obj: Any?): EncryptedSecretContent? {
            return MoshiProvider.providesMoshi()
                    .adapter(EncryptedSecretContent::class.java)
                    .fromJsonValue(obj)
        }
    }
}
