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

package org.matrix.android.sdk.internal.crypto.actions

import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.internal.crypto.MXCRYPTO_ALGORITHM_OLM
import org.matrix.android.sdk.internal.crypto.MXOlmDevice
import org.matrix.android.sdk.internal.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.internal.crypto.model.rest.EncryptedMessage
import org.matrix.android.sdk.internal.di.DeviceId
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.util.JsonCanonicalizer
import org.matrix.android.sdk.internal.util.convertToUTF8
import timber.log.Timber
import javax.inject.Inject

internal class MessageEncrypter @Inject constructor(
        @UserId
        private val userId: String,
        @DeviceId
        private val deviceId: String?,
        private val olmDevice: MXOlmDevice) {
    /**
     * Encrypt an event payload for a list of devices.
     * This method must be called from the getCryptoHandler() thread.
     *
     * @param payloadFields fields to include in the encrypted payload.
     * @param deviceInfos   list of device infos to encrypt for.
     * @return the content for an m.room.encrypted event.
     */
    fun encryptMessage(payloadFields: Content, deviceInfos: List<CryptoDeviceInfo>): EncryptedMessage {
        val deviceInfoParticipantKey = deviceInfos.associateBy { it.identityKey()!! }

        val payloadJson = payloadFields.toMutableMap()

        payloadJson["sender"] = userId
        payloadJson["sender_device"] = deviceId!!

        // Include the WeiSig25519 key so that the recipient knows what
        // device this message came from.
        // We don't need to include the wei25519 key since the
        // recipient will already know this from the olm headers.
        // When combined with the device keys retrieved from the
        // homeserver signed by the weisig25519 key this proves that
        // the wei25519 key and the weisig25519 key are owned by
        // the same device.
        payloadJson["keys"] = mapOf("weisig25519" to olmDevice.deviceWeiSig25519Key!!)

        val ciphertext = mutableMapOf<String, Any>()

        for ((deviceKey, deviceInfo) in deviceInfoParticipantKey) {
            val sessionId = olmDevice.getSessionId(deviceKey)

            if (!sessionId.isNullOrEmpty()) {
                Timber.v("Using sessionid $sessionId for device $deviceKey")

                payloadJson["recipient"] = deviceInfo.userId
                payloadJson["recipient_keys"] = mapOf("weisig25519" to deviceInfo.fingerprint()!!)

                val payloadString = convertToUTF8(JsonCanonicalizer.getCanonicalJson(Map::class.java, payloadJson))
                ciphertext[deviceKey] = olmDevice.encryptMessage(deviceKey, sessionId, payloadString)!!
            }
        }

        return EncryptedMessage(
                algorithm = MXCRYPTO_ALGORITHM_OLM,
                senderKey = olmDevice.deviceWei25519Key,
                cipherText = ciphertext
        )
    }
}
