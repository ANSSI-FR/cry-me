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

package org.matrix.android.sdk.internal.crypto.secrets

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.session.accountdata.SessionAccountDataService
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.securestorage.EncryptedSecretContent
import org.matrix.android.sdk.api.session.securestorage.IntegrityResult
import org.matrix.android.sdk.api.session.securestorage.KeyInfo
import org.matrix.android.sdk.api.session.securestorage.KeyInfoResult
import org.matrix.android.sdk.api.session.securestorage.KeySigner
import org.matrix.android.sdk.api.session.securestorage.RawBytesKeySpec
import org.matrix.android.sdk.api.session.securestorage.SecretStorageKeyContent
import org.matrix.android.sdk.api.session.securestorage.SharedSecretStorageError
import org.matrix.android.sdk.api.session.securestorage.SharedSecretStorageService
import org.matrix.android.sdk.api.session.securestorage.SsssKeyCreationInfo
import org.matrix.android.sdk.api.session.securestorage.SsssKeySpec
import org.matrix.android.sdk.api.session.securestorage.SsssPassphrase
import org.matrix.android.sdk.internal.crypto.OutgoingGossipingRequestManager
import org.matrix.android.sdk.internal.crypto.SSSS_ALGORITHM_AES_HMAC_SHA2
import org.matrix.android.sdk.internal.crypto.SSSS_ALGORITHM_WEI25519_AES_SHA2
import org.matrix.android.sdk.internal.crypto.crosssigning.fromBase64
import org.matrix.android.sdk.internal.crypto.crosssigning.toBase64NoPadding
import org.matrix.android.sdk.internal.crypto.keysbackup.generatePrivateKeyWithPassword
import org.matrix.android.sdk.internal.crypto.keysbackup.toHexString
import org.matrix.android.sdk.internal.crypto.keysbackup.util.computeRecoveryKey
import org.matrix.android.sdk.internal.crypto.tools.HkdfSha256
import org.matrix.android.sdk.internal.crypto.tools.withOlmDecryption
import org.matrix.android.sdk.internal.crypto.tools.withOlmUtility
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.olm.OlmBackupKeyEncryption
import org.matrix.olm.OlmPkMessage
import org.matrix.olm.OlmUtility
import org.matrix.olm.OlmException
import timber.log.Timber
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import kotlin.experimental.and

internal class DefaultSharedSecretStorageService @Inject constructor(
        @UserId private val userId: String,
        private val accountDataService: SessionAccountDataService,
        private val outgoingGossipingRequestManager: OutgoingGossipingRequestManager,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val cryptoCoroutineScope: CoroutineScope
) : SharedSecretStorageService {

    override suspend fun generateKey(keyId: String,
                                     key: SsssKeySpec?,
                                     keyName: String,
                                     keySigner: KeySigner?): SsssKeyCreationInfo {
        Timber.i("## DefaultSSSS: generateKey GENERATE KEY RNG")
        return withContext(cryptoCoroutineScope.coroutineContext + coroutineDispatchers.main) {
            val bytes = (key as? RawBytesKeySpec)?.privateKey
                    ?: ByteArray(32).also {
                        SecureRandom().nextBytes(it)
                    }

            val storageKeyContent = SecretStorageKeyContent(
                    name = keyName,
                    algorithm = SSSS_ALGORITHM_AES_HMAC_SHA2,
                    passphrase = null
            )

            val signedContent = keySigner?.sign(storageKeyContent.canonicalSignable())?.let {
                storageKeyContent.copy(
                        signatures = it
                )
            } ?: storageKeyContent

            accountDataService.updateUserAccountData("$KEY_ID_BASE.$keyId", signedContent.toContent())
            SsssKeyCreationInfo(
                    keyId = keyId,
                    content = storageKeyContent,
                    recoveryKey = computeRecoveryKey(bytes),
                    keySpec = RawBytesKeySpec(bytes)
            )
        }
    }

    override suspend fun generateKeyWithPassphrase(keyId: String,
                                                   keyName: String,
                                                   passphrase: String,
                                                   keySigner: KeySigner): SsssKeyCreationInfo {
        return withContext(cryptoCoroutineScope.coroutineContext + coroutineDispatchers.main) {
            val privatePart = generatePrivateKeyWithPassword(passphrase)

            val storageKeyContent = SecretStorageKeyContent(
                    algorithm = SSSS_ALGORITHM_AES_HMAC_SHA2,
                    passphrase = SsssPassphrase(algorithm = "m.pbkdf2", iterations = privatePart.iterations, salt = privatePart.salt)
            )

            val signedContent = keySigner.sign(storageKeyContent.canonicalSignable())?.let {
                storageKeyContent.copy(
                        signatures = it
                )
            } ?: storageKeyContent

            accountDataService.updateUserAccountData(
                    "$KEY_ID_BASE.$keyId",
                    signedContent.toContent()
            )
            SsssKeyCreationInfo(
                    keyId = keyId,
                    content = storageKeyContent,
                    recoveryKey = computeRecoveryKey(privatePart.privateKey),
                    keySpec = RawBytesKeySpec(privatePart.privateKey)
            )
        }
    }

    override fun hasKey(keyId: String): Boolean {
        return accountDataService.getUserAccountDataEvent("$KEY_ID_BASE.$keyId") != null
    }

    override fun getKey(keyId: String): KeyInfoResult {
        val accountData = accountDataService.getUserAccountDataEvent("$KEY_ID_BASE.$keyId")
                ?: return KeyInfoResult.Error(SharedSecretStorageError.UnknownKey(keyId))
        return SecretStorageKeyContent.fromJson(accountData.content)?.let {
            KeyInfoResult.Success(
                    KeyInfo(id = keyId, content = it)
            )
        } ?: KeyInfoResult.Error(SharedSecretStorageError.UnknownAlgorithm(keyId))
    }

    override suspend fun setDefaultKey(keyId: String) {
        val existingKey = getKey(keyId)
        if (existingKey is KeyInfoResult.Success) {
            accountDataService.updateUserAccountData(DEFAULT_KEY_ID, mapOf("key" to keyId))
        } else {
            throw SharedSecretStorageError.UnknownKey(keyId)
        }
    }

    override fun getDefaultKey(): KeyInfoResult {
        val accountData = accountDataService.getUserAccountDataEvent(DEFAULT_KEY_ID)
                ?: return KeyInfoResult.Error(SharedSecretStorageError.UnknownKey(DEFAULT_KEY_ID))
        val keyId = accountData.content["key"] as? String
                ?: return KeyInfoResult.Error(SharedSecretStorageError.UnknownKey(DEFAULT_KEY_ID))
        return getKey(keyId)
    }

    override suspend fun storeSecret(name: String, secretBase64: String, keys: List<SharedSecretStorageService.KeyRef>) {
        withContext(cryptoCoroutineScope.coroutineContext + coroutineDispatchers.main) {
            val encryptedContents = HashMap<String, EncryptedSecretContent>()
            keys.forEach {
                val keyId = it.keyId
                // encrypt the content
                when (val key = keyId?.let { getKey(keyId) } ?: getDefaultKey()) {
                    is KeyInfoResult.Success -> {
                        if (key.keyInfo.content.algorithm == SSSS_ALGORITHM_AES_HMAC_SHA2) {
                            encryptAesHmacSha2(it.keySpec!!, name, secretBase64).let {
                                encryptedContents[key.keyInfo.id] = it
                            }
                        } else {
                            // Unknown algorithm
                            throw SharedSecretStorageError.UnknownAlgorithm(key.keyInfo.content.algorithm ?: "")
                        }
                    }
                    is KeyInfoResult.Error -> throw key.error
                }
            }

            accountDataService.updateUserAccountData(
                    type = name,
                    content = mapOf("encrypted" to encryptedContents)
            )
        }
    }

    @Throws
    private fun encryptAesHmacSha2(secretKey: SsssKeySpec, secretName: String, clearDataBase64: String): EncryptedSecretContent {
        secretKey as RawBytesKeySpec

        val olmUtility = OlmUtility()
        val backupEncryption = olmUtility.encryptBackupKey(clearDataBase64.toByteArray().toHexString(), secretKey.privateKey.toHexString(), secretName)
        olmUtility.releaseUtility()

        val cipher = OlmUtility.hexStringToByteArray(backupEncryption.keyCiphertext)
        val mac = OlmUtility.hexStringToByteArray(backupEncryption.keyMac)
        val iv = OlmUtility.hexStringToByteArray(backupEncryption.keyIv)


        return EncryptedSecretContent(
                ciphertext = cipher.toBase64NoPadding(),
                initializationVector = iv.toBase64NoPadding(),
                mac = mac.toBase64NoPadding()
        )
    }

    private fun decryptAesHmacSha2(secretKey: SsssKeySpec, secretName: String, cipherContent: EncryptedSecretContent): String {
        secretKey as RawBytesKeySpec

        val cipherRawBytes = cipherContent.ciphertext?.fromBase64() ?: throw SharedSecretStorageError.BadCipherText
        val cipher = OlmUtility.byteArraytoHexString(cipherRawBytes)

        val mac = OlmUtility.byteArraytoHexString(cipherContent.mac?.fromBase64())

        val ivBytes = cipherContent.initializationVector?.fromBase64()
        if((ivBytes == null) || (ivBytes.size != 16)){
            throw OlmException(OlmException.EXCEPTION_CODE_UTILITY_BACKUP_DECRYPTION_ERROR, "wrong iv !")
        }
        val iv = OlmUtility.byteArraytoHexString(ivBytes)

        val msg = OlmBackupKeyEncryption()
        msg.keyCiphertext = cipher
        msg.keyMac = mac
        msg.keyIv = iv

        val olmUtility = OlmUtility()
        val decryptedSecret = OlmUtility.hexStringToByteArray(olmUtility.decryptBackupKey(msg, secretKey.privateKey.toHexString() , secretName))
        olmUtility.releaseUtility()

        require(decryptedSecret.isNotEmpty())

        return String(decryptedSecret, Charsets.UTF_8)
    }

    override fun getAlgorithmsForSecret(name: String): List<KeyInfoResult> {
        val accountData = accountDataService.getUserAccountDataEvent(name)
                ?: return listOf(KeyInfoResult.Error(SharedSecretStorageError.UnknownSecret(name)))
        val encryptedContent = accountData.content[ENCRYPTED] as? Map<*, *>
                ?: return listOf(KeyInfoResult.Error(SharedSecretStorageError.SecretNotEncrypted(name)))

        val results = ArrayList<KeyInfoResult>()
        encryptedContent.keys.forEach {
            (it as? String)?.let { keyId ->
                results.add(getKey(keyId))
            }
        }
        return results
    }

    override suspend fun getSecret(name: String, keyId: String?, secretKey: SsssKeySpec): String {
        val accountData = accountDataService.getUserAccountDataEvent(name) ?: throw SharedSecretStorageError.UnknownSecret(name)
        val encryptedContent = accountData.content[ENCRYPTED] as? Map<*, *> ?: throw SharedSecretStorageError.SecretNotEncrypted(name)
        val key = keyId?.let { getKey(it) } as? KeyInfoResult.Success ?: getDefaultKey() as? KeyInfoResult.Success
        ?: throw SharedSecretStorageError.UnknownKey(name)

        val encryptedForKey = encryptedContent[key.keyInfo.id] ?: throw SharedSecretStorageError.SecretNotEncryptedWithKey(name, key.keyInfo.id)

        val secretContent = EncryptedSecretContent.fromJson(encryptedForKey)
                ?: throw SharedSecretStorageError.ParsingError

        val algorithm = key.keyInfo.content
        if (SSSS_ALGORITHM_WEI25519_AES_SHA2 == algorithm.algorithm) {
            Timber.i("## backup getSecret: retrieving megolm backup from DefaultSSSS ?????")
            throw SharedSecretStorageError.UnsupportedAlgorithm(algorithm.algorithm)
            //val keySpec = secretKey as? RawBytesKeySpec ?: throw SharedSecretStorageError.BadKeyFormat
            /*return withContext(cryptoCoroutineScope.coroutineContext + coroutineDispatchers.main) {
                // decrypt from recovery key
                withOlmDecryption { olmPkDecryption ->
                    olmPkDecryption.setPrivateKey(keySpec.privateKey)
                    olmPkDecryption.decrypt(OlmPkMessage()
                            .apply {
                                mCipherText = secretContent.ciphertext
                                mEphemeralKey = secretContent.ephemeral
                                mMac = secretContent.mac
                            }
                    )
                }
            }*/
        } else if (SSSS_ALGORITHM_AES_HMAC_SHA2 == algorithm.algorithm) {
            val keySpec = secretKey as? RawBytesKeySpec ?: throw SharedSecretStorageError.BadKeyFormat
            return withContext(cryptoCoroutineScope.coroutineContext + coroutineDispatchers.main) {
                decryptAesHmacSha2(keySpec, name, secretContent)
            }
        } else {
            throw SharedSecretStorageError.UnsupportedAlgorithm(algorithm.algorithm ?: "")
        }
    }

    companion object {
        const val KEY_ID_BASE = "m.secret_storage.key"
        const val ENCRYPTED = "encrypted"
        const val DEFAULT_KEY_ID = "m.secret_storage.default_key"
    }

    override fun checkShouldBeAbleToAccessSecrets(secretNames: List<String>, keyId: String?): IntegrityResult {
        if (secretNames.isEmpty()) {
            return IntegrityResult.Error(SharedSecretStorageError.UnknownSecret("none"))
        }

        val keyInfoResult = if (keyId == null) {
            getDefaultKey()
        } else {
            getKey(keyId)
        }

        val keyInfo = (keyInfoResult as? KeyInfoResult.Success)?.keyInfo
                ?: return IntegrityResult.Error(SharedSecretStorageError.UnknownKey(keyId ?: ""))

        if (keyInfo.content.algorithm != SSSS_ALGORITHM_AES_HMAC_SHA2 &&
                keyInfo.content.algorithm != SSSS_ALGORITHM_WEI25519_AES_SHA2) {
            // Unsupported algorithm
            return IntegrityResult.Error(
                    SharedSecretStorageError.UnsupportedAlgorithm(keyInfo.content.algorithm ?: "")
            )
        }

        secretNames.forEach { secretName ->
            val secretEvent = accountDataService.getUserAccountDataEvent(secretName)
                    ?: return IntegrityResult.Error(SharedSecretStorageError.UnknownSecret(secretName))
            if ((secretEvent.content["encrypted"] as? Map<*, *>)?.get(keyInfo.id) == null) {
                return IntegrityResult.Error(SharedSecretStorageError.SecretNotEncryptedWithKey(secretName, keyInfo.id))
            }
        }

        return IntegrityResult.Success(keyInfo.content.passphrase != null)
    }

    override fun requestSecret(name: String, myOtherDeviceId: String) {
        outgoingGossipingRequestManager.sendSecretShareRequest(
                name,
                mapOf(userId to listOf(myOtherDeviceId))
        )
    }
}
