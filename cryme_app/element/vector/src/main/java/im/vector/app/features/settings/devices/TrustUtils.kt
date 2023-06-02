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
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.settings.devices

import org.matrix.android.sdk.api.crypto.RoomEncryptionTrustLevel
import org.matrix.android.sdk.internal.crypto.crosssigning.DeviceTrustLevel

object TrustUtils {

    fun shieldForTrust(currentDevice: Boolean,
                       trustMSK: Boolean,
                       legacyMode: Boolean,
                       deviceTrustLevel: DeviceTrustLevel?): RoomEncryptionTrustLevel {
        return when {
            currentDevice -> {
                if (legacyMode) {
                    // In legacy, current session is always trusted
                    RoomEncryptionTrustLevel.Trusted
                } else {
                    // If current session doesn't trust MSK, show red shield for current device
                    if (trustMSK) {
                        RoomEncryptionTrustLevel.Trusted
                    } else {
                        RoomEncryptionTrustLevel.Warning
                    }
                }
            }
            else          -> {
                if (legacyMode) {
                    // use local trust
                    if (deviceTrustLevel?.locallyVerified == true) {
                        RoomEncryptionTrustLevel.Trusted
                    } else {
                        RoomEncryptionTrustLevel.Warning
                    }
                } else {
                    if (trustMSK) {
                        // use cross sign trust, put locally trusted in black
                        when {
                            deviceTrustLevel?.crossSigningVerified == true -> RoomEncryptionTrustLevel.Trusted

                            deviceTrustLevel?.locallyVerified == true      -> RoomEncryptionTrustLevel.Default
                            else                                           -> RoomEncryptionTrustLevel.Warning
                        }
                    } else {
                        // The current session is untrusted, so displays others in black
                        // as we can't know the cross-signing state
                        RoomEncryptionTrustLevel.Default
                    }
                }
            }
        }
    }
}
