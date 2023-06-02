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

package org.matrix.android.sdk.internal.network.ssl

import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * Implements a TrustManager that checks Certificates against an explicit list of known
 * fingerprints.
 */

/**
 * @param fingerprints        Not empty array of SHA256 cert fingerprints
 * @param defaultTrustManager Optional trust manager to fall back on if cert does not match
 * any of the fingerprints. Can be null.
 */
internal class PinnedTrustManager(private val fingerprints: List<Fingerprint>,
                                  private val defaultTrustManager: X509TrustManager?) : X509TrustManager {

    @Throws(CertificateException::class)
    override fun checkClientTrusted(chain: Array<X509Certificate>, s: String) {
        try {
            if (defaultTrustManager != null) {
                defaultTrustManager.checkClientTrusted(chain, s)
                return
            }
        } catch (e: CertificateException) {
            // If there is an exception we fall back to checking fingerprints
            if (fingerprints.isEmpty()) {
                throw UnrecognizedCertificateException(chain[0], Fingerprint.newSha256Fingerprint(chain[0]), e.cause)
            }
        }

        checkTrusted(chain)
    }

    @Throws(CertificateException::class)
    override fun checkServerTrusted(chain: Array<X509Certificate>, s: String) {
        try {
            if (defaultTrustManager != null) {
                defaultTrustManager.checkServerTrusted(chain, s)
                return
            }
        } catch (e: CertificateException) {
            // If there is an exception we fall back to checking fingerprints
            if (fingerprints.isEmpty()) {
                throw UnrecognizedCertificateException(chain[0], Fingerprint.newSha256Fingerprint(chain[0]), e.cause /* BMA: Shouldn't be `e` ? */)
            }
        }

        checkTrusted(chain)
    }

    @Throws(CertificateException::class)
    private fun checkTrusted(chain: Array<X509Certificate>) {
        val cert = chain[0]

        if (!fingerprints.any { it.matchesCert(cert) }) {
            throw UnrecognizedCertificateException(cert, Fingerprint.newSha256Fingerprint(cert), null)
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return defaultTrustManager?.acceptedIssuers ?: emptyArray()
    }
}
