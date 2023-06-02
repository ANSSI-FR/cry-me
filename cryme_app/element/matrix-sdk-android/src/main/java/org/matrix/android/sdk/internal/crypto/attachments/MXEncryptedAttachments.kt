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

package org.matrix.android.sdk.internal.crypto.attachments

import android.util.Base64
import org.matrix.android.sdk.internal.crypto.crosssigning.fromBase64
import org.matrix.android.sdk.internal.crypto.model.rest.EncryptedFileInfo
import org.matrix.android.sdk.internal.crypto.model.rest.EncryptedFileKey
import org.matrix.android.sdk.internal.crypto.tools.withOlmEncryption
import org.matrix.android.sdk.internal.crypto.tools.withOlmUtility
import org.matrix.android.sdk.internal.util.base64ToBase64Url
import org.matrix.android.sdk.internal.util.base64ToUnpaddedBase64
import org.matrix.android.sdk.internal.util.base64UrlToBase64
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.matrix.olm.OlmAttachmentUtility
import org.matrix.olm.OlmAttachmentMessage
import java.nio.charset.Charset
import java.util.*

internal object MXEncryptedAttachments {
    private const val CRYPTO_BUFFER_SIZE = 32 * 1024
    private const val CIPHER_ALGORITHM = "AES/CTR/NoPadding"
    private const val SECRET_KEY_SPEC_ALGORITHM = "AES"
    private const val MESSAGE_DIGEST_ALGORITHM = "SHA-256"

    private fun printEncryptionMessage(msg: org.matrix.olm.OlmAttachmentMessage){
        Timber.i("ENCRYPTION MESSAGE : ")
        Timber.i("   cipher: ${msg.ciphertext}")
        Timber.i("   mac: ${msg.mac}")
        Timber.i("   keyAes: ${msg.keyAes}")
        Timber.i("   ivAes: ${msg.ivAes}")
        Timber.i("   keyMac: ${msg.keyMac}")
    }

    fun encrypt(clearStream: InputStream,
                outputFile: File,
                sessionKey: String ,
                progress: ((current: Int, total: Int) -> Unit)): EncryptedFileInfo {
        Timber.i("## Encrypt file MX : sessionKey = ${sessionKey}")
        val t0 = System.currentTimeMillis()

        var olmAttachmentMessage: OlmAttachmentMessage

        outputFile.outputStream().use { outputStream ->
            val data = ByteArray(CRYPTO_BUFFER_SIZE)
            var read: Int
            val byteStream = ByteArrayOutputStream()

            clearStream.use { inputStream ->
                val estimatedSize = inputStream.available()
                Timber.i("InputStream available = $estimatedSize")
                progress.invoke(0, estimatedSize)
                read = inputStream.read(data)
                var totalRead = read
                while (read != -1) {
                    byteStream.write(data)
                    progress.invoke(totalRead, estimatedSize)
                    read = inputStream.read(data)
                    totalRead += read
                }
            }

            val plaintext = byteStream.toByteArray()
            val encrypter = OlmAttachmentUtility()

            olmAttachmentMessage = encrypter.encryptAttachment(plaintext, sessionKey)

            //olmAttachmentMessage.ciphertext = olmAttachmentMessage.ciphertext.replace("\n", "").replace("=", "")

            Timber.i("CIPHERTEXT SIZE = ${olmAttachmentMessage.ciphertext.toByteArray(charset("UTF-8")).size}")
            printEncryptionMessage(olmAttachmentMessage)

            encrypter.releaseAttachmentUtility()
            outputStream.write(olmAttachmentMessage.ciphertext.toByteArray(charset("UTF-8")))
        }

        return EncryptedFileInfo(
                url = null,
                key = EncryptedFileKey(
                        alg = "A256CTR",
                        ext = true,
                        keyOps = listOf("encrypt", "decrypt"),
                        kty = "oct",
                        k = base64ToBase64Url(olmAttachmentMessage.keyAes.replace("\n", "").replace("=", "") +
                                                      olmAttachmentMessage.keyMac.replace("\n", "").replace("=", "") +
                                                      olmAttachmentMessage.ciphertextLength.replace("\n", "").replace("=", ""))
                ),
                iv = olmAttachmentMessage.ivAes.replace("\n", "").replace("=", ""),
                hashes = mapOf("sha256" to olmAttachmentMessage.mac.replace("\n", "").replace("=", "")),
                v = "v2"
        )
                .also { Timber.v("Encrypt in ${System.currentTimeMillis() - t0}ms") }
    }

    /***
     * Encrypt an attachment stream.
     * DO NOT USE for big files, it will load all in memory
     * @param attachmentStream the attachment stream. Will be closed after this method call.
     * @return the encryption file info
     */
    fun encryptAttachment(attachmentStream: InputStream, sessionKey: String): EncryptionResult {
        Timber.i("EncryptAttachment CALL ...")
        val t0 = System.currentTimeMillis()

        var olmAttachmentMessage: OlmAttachmentMessage

        val byteArrayOutputStream = ByteArrayOutputStream()
        byteArrayOutputStream.use { outputStream ->

            val data = ByteArray(CRYPTO_BUFFER_SIZE)
            var read: Int
            val byteStream = ByteArrayOutputStream()

            attachmentStream.use { inputStream ->
                read = inputStream.read(data)
                while (read != -1) {
                    byteStream.write(data)
                    read = inputStream.read(data)
                }
            }

            val plaintext = byteStream.toByteArray()
            val encrypter = OlmAttachmentUtility()

            olmAttachmentMessage = encrypter.encryptAttachment(plaintext, sessionKey)

            Timber.i("CIPHERTEXT SIZE = ${olmAttachmentMessage.ciphertext.toByteArray(charset("UTF-8")).size}")
            printEncryptionMessage(olmAttachmentMessage)

            encrypter.releaseAttachmentUtility()

            outputStream.write(olmAttachmentMessage.ciphertext.toByteArray(charset("UTF-8")))
        }

        return EncryptionResult(
                encryptedFileInfo = EncryptedFileInfo(
                        url = null,
                        key = EncryptedFileKey(
                                alg = "A256CTR",
                                ext = true,
                                keyOps = listOf("encrypt", "decrypt"),
                                kty = "oct",
                                k = base64ToBase64Url(olmAttachmentMessage.keyAes.replace("\n", "").replace("=", "") +
                                                            olmAttachmentMessage.keyMac.replace("\n", "").replace("=", "") +
                                                            olmAttachmentMessage.ciphertextLength.replace("\n", "").replace("=", ""))
                        ),
                        iv = olmAttachmentMessage.ivAes.replace("\n", "").replace("=", ""),
                        hashes = mapOf("sha256" to olmAttachmentMessage.mac.replace("\n", "").replace("=", "")),
                        v = "v2"
                ),
                encryptedByteArray = byteArrayOutputStream.toByteArray()
        )
                .also { Timber.v("Encrypt in ${System.currentTimeMillis() - t0}ms") }
    }

    /**
     * Decrypt an attachment
     *
     * @param attachmentStream the attachment stream. Will be closed after this method call.
     * @param elementToDecrypt the elementToDecrypt info
     * @param outputStream     the outputStream where the decrypted attachment will be write.
     * @return true in case of success, false in case of error
     */
    fun decryptAttachment(attachmentStream: InputStream?,
                          elementToDecrypt: ElementToDecrypt?,
                          outputStream: OutputStream): Boolean {

        Timber.i("## DECRYPTING Attachement")
        // sanity checks
        if (null == attachmentStream || elementToDecrypt == null) {
            Timber.e("## decryptAttachment() : null stream")
            return false
        }

        val t0 = System.currentTimeMillis()

        try {
            val keys = base64UrlToBase64(elementToDecrypt.k)
            val iv = elementToDecrypt.iv

            var read: Int
            val data = ByteArray(CRYPTO_BUFFER_SIZE)
            val byteStream = ByteArrayOutputStream()

            attachmentStream.use { inputStream ->
                read = inputStream.read(data)
                while (read != -1) {
                    byteStream.write(data)
                    read = inputStream.read(data)
                }
            }

            val ciphertext = byteStream.toByteArray()
            val decrypter = OlmAttachmentUtility()

            Timber.i("CIPHERTEXT SIZE = ${ciphertext.size}")

            val attachmentEncryptionMessage = decrypter.getAttachmentEncryptionInfo(keys, iv, ciphertext, elementToDecrypt.sha256)

            Timber.i("CIPHERTEXT SIZE NEW = ${attachmentEncryptionMessage.ciphertext.toByteArray(charset("UTF-8")).size}")

            printEncryptionMessage(attachmentEncryptionMessage)

            val plaintext = decrypter.decryptAttachment(attachmentEncryptionMessage)
            outputStream.write(plaintext)

            decrypter.releaseAttachmentUtility()

            return true.also { Timber.v("Decrypt in ${System.currentTimeMillis() - t0}ms") }

        } catch (oom: OutOfMemoryError) {
            Timber.e(oom, "## decryptAttachment() failed: OOM")
        } catch (e: Exception) {
            Timber.e(e, "## decryptAttachment() failed")
        }

        return false
    }
}
