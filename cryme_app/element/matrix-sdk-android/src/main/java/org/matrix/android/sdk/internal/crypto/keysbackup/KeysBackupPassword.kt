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

/**
 * Utility to compute a backup private key from a password and vice-versa.
 */
package org.matrix.android.sdk.internal.crypto.keysbackup

import androidx.annotation.WorkerThread
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.internal.crypto.crosssigning.toBase64NoPadding
import timber.log.Timber
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor
import org.matrix.olm.OlmUtility
import org.matrix.olm.OlmPRGUtility
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

private const val SALT_LENGTH = 16
private const val DEFAULT_ITERATION = 1048576  //2^20 iterations

data class GeneratePrivateKeyResult(
        // The private key
        val privateKey: ByteArray,
        // the salt used to generate the private key
        val salt: String,
        // number of key derivations done on the generated private key.
        val iterations: Int)

/**
 * Compute a private key from a password.
 *
 * @param password the password to use.
 *
 * @return a {privateKey, salt, iterations} tuple.
 */
@WorkerThread
fun generatePrivateKeyWithPassword(password: String): GeneratePrivateKeyResult {
    val salt = generateSalt()
    val iterations = DEFAULT_ITERATION
    val privateKey = deriveKey(password, salt, iterations)

    return GeneratePrivateKeyResult(privateKey, salt, iterations)
}

/**
 * Retrieve a private key from {password, salt, iterations}
 *
 * @param password the password used to generated the private key.
 * @param salt the salt.
 * @param iterations number of key derivations.
 * @param progressListener the progress listener
 *
 * @return a private key.
 */
@WorkerThread
fun retrievePrivateKeyWithPassword(password: String,
                                   salt: String,
                                   iterations: Int): ByteArray {
    return deriveKey(password, salt, iterations)
}

fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

/**
 * Compute a private key by deriving a password and a salt strings.
 *
 * @param password the password.
 * @param salt the salt.
 * @param iterations number of derivations.
 * @param progressListener a listener to follow progress.
 *
 * @return a private key.
 */
@WorkerThread
fun deriveKey(password: String,
              salt: String,
              iterations: Int): ByteArray {

    val olmUtility = OlmUtility()

    val DK = olmUtility.computePbkdf2(password, salt, 32, iterations)

    olmUtility.releaseUtility()

    return DK
}

/**
 * Generate a SALT_LENGTH bytes salt
 */
private fun generateSalt(): String {
    val prg = OlmPRGUtility.getInstance()
    val salt = prg.getRandomBytes(SALT_LENGTH).toHexString()
    return salt
}
