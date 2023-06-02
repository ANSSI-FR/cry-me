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
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.crypto.verification

import im.vector.app.core.hardware.HardwareInfo
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import timber.log.Timber
import javax.inject.Inject

class SupportedVerificationMethodsProvider @Inject constructor(
        private val hardwareInfo: HardwareInfo
) {
    /**
     * Provide the list of supported method by Element, with or without the QR_CODE_SCAN, depending if a back camera
     * is available
     */
    fun provide(): List<VerificationMethod> {
        return mutableListOf(
                // Element supports SAS verification
                VerificationMethod.SAS,
                // Element is able to show QR codes
                VerificationMethod.QR_CODE_SHOW)
                .apply {
                    if (hardwareInfo.hasBackCamera()) {
                        // Element is able to scan QR codes, and a Camera is available
                        add(VerificationMethod.QR_CODE_SCAN)
                    } else {
                        // This quite uncommon
                        Timber.w("No back Camera detected")
                    }
                }
    }
}
