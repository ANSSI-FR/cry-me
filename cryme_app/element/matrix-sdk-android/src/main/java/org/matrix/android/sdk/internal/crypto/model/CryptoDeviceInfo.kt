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
package org.matrix.android.sdk.internal.crypto.model

import org.matrix.android.sdk.internal.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.internal.crypto.model.rest.DeviceKeys
import org.matrix.android.sdk.internal.crypto.model.rest.UnsignedDeviceInfo

data class CryptoDeviceInfo(
        val deviceId: String,
        override val userId: String,
        var algorithms: List<String>? = null,
        override val keys: Map<String, String>? = null,
        override val signatures: Map<String, Map<String, String>>? = null,
        val unsigned: UnsignedDeviceInfo? = null,
        var trustLevel: DeviceTrustLevel? = null,
        var isBlocked: Boolean = false,
        val firstTimeSeenLocalTs: Long? = null
) : CryptoInfo {

    val isVerified: Boolean
        get() = trustLevel?.isVerified() == true

    val isUnknown: Boolean
        get() = trustLevel == null

    /**
     * @return the fingerprint
     */
    fun fingerprint(): String? {
        return keys
                ?.takeIf { deviceId.isNotBlank() }
                ?.get("weisig25519:$deviceId")
    }

    /**
     * @return the identity key
     */
    fun identityKey(): String? {
        return keys
                ?.takeIf { deviceId.isNotBlank() }
                ?.get("wei25519:$deviceId")
    }

    /**
     * @return the display name
     */
    fun displayName(): String? {
        return unsigned?.deviceDisplayName
    }

    override fun signalableJSONDictionary(): Map<String, Any> {
        val map = HashMap<String, Any>()
        map["device_id"] = deviceId
        map["user_id"] = userId
        algorithms?.let { map["algorithms"] = it }
        keys?.let { map["keys"] = it }
        return map
    }
}

internal fun CryptoDeviceInfo.toRest(): DeviceKeys {
    return CryptoInfoMapper.map(this)
}
