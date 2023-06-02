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

import org.matrix.android.sdk.api.session.crypto.verification.SasMode
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_RECIPROCATE
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_SAS
import timber.log.Timber

internal interface VerificationInfoStart : VerificationInfo<ValidVerificationInfoStart> {

    val method: String?

    /**
     * Alice’s device ID
     */
    val fromDevice: String?

    /**
     * An array of key agreement protocols that Alice’s client understands.
     * Must include “wei25519”.
     * Other methods may be defined in the future
     */
    val keyAgreementProtocols: List<String>?

    /**
     * An array of hashes that Alice’s client understands.
     * Must include “sha256”.  Other methods may be defined in the future.
     */
    val hashes: List<String>?

    /**
     * An array of message authentication codes that Alice’s client understands.
     * Must include “hkdf-hmac-sha256”.
     * Other methods may be defined in the future.
     */
    val messageAuthenticationCodes: List<String>?

    /**
     * An array of short authentication string methods that Alice’s client (and Alice) understands.
     * Must include “decimal”.
     * This document also describes the “emoji” method.
     * Other methods may be defined in the future
     */
    val shortAuthenticationStrings: List<String>?

    /**
     * Shared secret, when starting verification with QR code
     */
    val sharedSecret: String?

    fun toCanonicalJson(): String

    override fun asValidObject(): ValidVerificationInfoStart? {
        val validTransactionId = transactionId?.takeIf { it.isNotEmpty() } ?: return null
        val validFromDevice = fromDevice?.takeIf { it.isNotEmpty() } ?: return null

        return when (method) {
            VERIFICATION_METHOD_SAS         -> {
                val validKeyAgreementProtocols = keyAgreementProtocols?.takeIf { it.isNotEmpty() } ?: return null
                val validHashes = hashes?.takeIf { it.contains("sha") } ?: return null
                val validMessageAuthenticationCodes = messageAuthenticationCodes
                        ?.takeIf {
                            it.contains(SASDefaultVerificationTransaction.SAS_MAC_SHA)
                                    //|| it.contains(SASDefaultVerificationTransaction.SAS_MAC_SHA256_LONGKDF)
                        }
                        ?: return null
                val validShortAuthenticationStrings = shortAuthenticationStrings?.takeIf { it.contains(SasMode.DECIMAL) } ?: return null

                ValidVerificationInfoStart.SasVerificationInfoStart(
                        validTransactionId,
                        validFromDevice,
                        validKeyAgreementProtocols,
                        validHashes,
                        validMessageAuthenticationCodes,
                        validShortAuthenticationStrings,
                        canonicalJson = toCanonicalJson()
                )
            }
            VERIFICATION_METHOD_RECIPROCATE -> {
                val validSharedSecret = sharedSecret?.takeIf { it.isNotEmpty() } ?: return null

                ValidVerificationInfoStart.ReciprocateVerificationInfoStart(
                        validTransactionId,
                        validFromDevice,
                        validSharedSecret
                )
            }
            else                            -> null
        }
    }
}

sealed class ValidVerificationInfoStart(
        open val transactionId: String,
        open val fromDevice: String) {
    data class SasVerificationInfoStart(
            override val transactionId: String,
            override val fromDevice: String,
            val keyAgreementProtocols: List<String>,
            val hashes: List<String>,
            val messageAuthenticationCodes: List<String>,
            val shortAuthenticationStrings: List<String>,
            val canonicalJson: String
    ) : ValidVerificationInfoStart(transactionId, fromDevice)

    data class ReciprocateVerificationInfoStart(
            override val transactionId: String,
            override val fromDevice: String,
            val sharedSecret: String
    ) : ValidVerificationInfoStart(transactionId, fromDevice)
}
