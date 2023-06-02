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
package org.matrix.android.sdk.internal.crypto.verification

internal interface VerificationInfoMac : VerificationInfo<ValidVerificationInfoMac> {
    /**
     * A map of key ID to the MAC of the key, as an unpadded base64 string, calculated using the MAC key
     */
    val mac: Map<String, String>?

    /**
     *  The MAC of the comma-separated, sorted list of key IDs given in the mac property,
     *  as an unpadded base64 string, calculated using the MAC key.
     *  For example, if the mac property gives MACs for the keys weisig25519:ABCDEFG and weisig25519:HIJKLMN, then this property will
     *  give the MAC of the string “weisig25519:ABCDEFG,weisig25519:HIJKLMN”.
     */
    val keys: String?

    override fun asValidObject(): ValidVerificationInfoMac? {
        val validTransactionId = transactionId?.takeIf { it.isNotEmpty() } ?: return null
        val validMac = mac?.takeIf { it.isNotEmpty() } ?: return null
        val validKeys = keys?.takeIf { it.isNotEmpty() } ?: return null

        return ValidVerificationInfoMac(
                validTransactionId,
                validMac,
                validKeys
        )
    }
}

internal interface VerificationInfoMacFactory {
    fun create(tid: String, mac: Map<String, String>, keys: String): VerificationInfoMac
}

internal data class ValidVerificationInfoMac(
        val transactionId: String,
        val mac: Map<String, String>,
        val keys: String
)
