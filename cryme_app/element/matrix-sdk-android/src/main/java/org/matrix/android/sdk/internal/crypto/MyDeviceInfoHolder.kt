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

package org.matrix.android.sdk.internal.crypto

import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.internal.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.internal.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject

@SessionScope
internal class MyDeviceInfoHolder @Inject constructor(
        // The credentials,
        credentials: Credentials,
        // the crypto store
        cryptoStore: IMXCryptoStore,
        // Olm device
        olmDevice: MXOlmDevice
) {
    // Our device keys
    /**
     * my device info
     */
    val myDevice: CryptoDeviceInfo

    init {

        val keys = HashMap<String, String>()

// TODO it's a bit strange, why not load from DB?
        if (!olmDevice.deviceWeiSig25519Key.isNullOrEmpty()) {
            keys["weisig25519:" + credentials.deviceId] = olmDevice.deviceWeiSig25519Key!!
        }

        if (!olmDevice.deviceWei25519Key.isNullOrEmpty()) {
            keys["wei25519:" + credentials.deviceId] = olmDevice.deviceWei25519Key!!
        }

//        myDevice.keys = keys
//
//        myDevice.algorithms = MXCryptoAlgorithms.supportedAlgorithms()

        // TODO hwo to really check cross signed status?
        //
        val crossSigned = cryptoStore.getMyCrossSigningInfo()?.masterKey()?.trustLevel?.locallyVerified ?: false
//        myDevice.trustLevel = DeviceTrustLevel(crossSigned, true)

        myDevice = CryptoDeviceInfo(
                credentials.deviceId!!,
                credentials.userId,
                keys = keys,
                algorithms = MXCryptoAlgorithms.supportedAlgorithms(),
                trustLevel = DeviceTrustLevel(crossSigned, true)
        )

        // Add our own deviceinfo to the store
        val endToEndDevicesForUser = cryptoStore.getUserDevices(credentials.userId)

        val myDevices = endToEndDevicesForUser.orEmpty().toMutableMap()

        myDevices[myDevice.deviceId] = myDevice

        cryptoStore.storeUserDevices(credentials.userId, myDevices)
    }
}
