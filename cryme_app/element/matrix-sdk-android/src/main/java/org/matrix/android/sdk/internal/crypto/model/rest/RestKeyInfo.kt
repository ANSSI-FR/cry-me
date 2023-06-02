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
package org.matrix.android.sdk.internal.crypto.model.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.internal.crypto.model.CryptoCrossSigningKey
import org.matrix.android.sdk.internal.crypto.model.CryptoInfoMapper

@JsonClass(generateAdapter = true)
internal data class RestKeyInfo(
        /**
         * The user who owns the key
         */
        @Json(name = "user_id")
        val userId: String,

        /**
         * Allowed uses for the key.
         * Must contain "master" for master keys, "self_signing" for self-signing keys, and "user_signing" for user-signing keys.
         * See CrossSigningKeyInfo#KEY_USAGE_* constants
         */
        @Json(name = "usage")
        val usages: List<String>?,

        /**
         * An object that must have one entry,
         * whose name is "weisig25519:" followed by the unpadded base64 encoding of the public key,
         * and whose value is the unpadded base64 encoding of the public key.
         */
        @Json(name = "keys")
        val keys: Map<String, String>?,

        /**
         *  Signatures of the key.
         *  A self-signing or user-signing key must be signed by the master key.
         *  A master key may be signed by a device.
         */
        @Json(name = "signatures")
        val signatures: Map<String, Map<String, String>>? = null
) {
    fun toCryptoModel(): CryptoCrossSigningKey {
        return CryptoInfoMapper.map(this)
    }
}
