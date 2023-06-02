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

package org.matrix.android.sdk.internal.crypto.store.db.mapper

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.realm.RealmList
import org.matrix.android.sdk.internal.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.internal.crypto.model.CryptoCrossSigningKey
import org.matrix.android.sdk.internal.crypto.store.db.model.KeyInfoEntity
import timber.log.Timber
import javax.inject.Inject

internal class CrossSigningKeysMapper @Inject constructor(moshi: Moshi) {

    private val signaturesAdapter = moshi.adapter<Map<String, Map<String, String>>>(Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Any::class.java
    ))

    fun update(keyInfo: KeyInfoEntity, cryptoCrossSigningKey: CryptoCrossSigningKey) {
        // update signatures?
        keyInfo.signatures = serializeSignatures(cryptoCrossSigningKey.signatures)
        keyInfo.usages = cryptoCrossSigningKey.usages?.toTypedArray()?.let { RealmList(*it) }
                ?: RealmList()
    }

    fun map(userId: String?, keyInfo: KeyInfoEntity?): CryptoCrossSigningKey? {
        val pubKey = keyInfo?.publicKeyBase64 ?: return null
        return CryptoCrossSigningKey(
                userId = userId ?: "",
                keys = mapOf("weisig25519:$pubKey" to pubKey),
                usages = keyInfo.usages.toList(),
                signatures = deserializeSignatures(keyInfo.signatures),
                trustLevel = keyInfo.trustLevelEntity?.let {
                    DeviceTrustLevel(
                            crossSigningVerified = it.crossSignedVerified ?: false,
                            locallyVerified = it.locallyVerified ?: false
                    )
                }
        )
    }

    fun map(keyInfo: CryptoCrossSigningKey): KeyInfoEntity {
        return KeyInfoEntity().apply {
            publicKeyBase64 = keyInfo.unpaddedBase64PublicKey
            usages = keyInfo.usages?.let { RealmList(*it.toTypedArray()) } ?: RealmList()
            signatures = serializeSignatures(keyInfo.signatures)
            // TODO how to handle better, check if same keys?
            // reset trust
            trustLevelEntity = null
        }
    }

    fun serializeSignatures(signatures: Map<String, Map<String, String>>?): String {
        return signaturesAdapter.toJson(signatures)
    }

    fun deserializeSignatures(signatures: String?): Map<String, Map<String, String>>? {
        if (signatures == null) {
            return null
        }
        return try {
            signaturesAdapter.fromJson(signatures)
        } catch (failure: Throwable) {
            Timber.e(failure)
            null
        }
    }
}
