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

package org.matrix.android.sdk.internal.crypto.model.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeviceKeysWithUnsigned(
        /**
         * Required. The ID of the user the device belongs to. Must match the user ID used when logging in.
         */
        @Json(name = "user_id")
        val userId: String,

        /**
         * Required. The ID of the device these keys belong to. Must match the device ID used when logging in.
         */
        @Json(name = "device_id")
        val deviceId: String,

        /**
         * Required. The encryption algorithms supported by this device.
         */
        @Json(name = "algorithms")
        val algorithms: List<String>?,

        /**
         * Required. Public identity keys. The names of the properties should be in the format <algorithm>:<device_id>.
         * The keys themselves should be encoded as specified by the key algorithm.
         */
        @Json(name = "keys")
        val keys: Map<String, String>?,

        /**
         * Required. Signatures for the device key object. A map from user ID, to a map from <algorithm>:<device_id> to the signature.
         * The signature is calculated using the process described at https://matrix.org/docs/spec/appendices.html#signing-json.
         */
        @Json(name = "signatures")
        val signatures: Map<String, Map<String, String>>?,

        /**
         * Additional data added to the device key information by intermediate servers, and not covered by the signatures.
         */
        @Json(name = "unsigned")
        val unsigned: UnsignedDeviceInfo? = null
)
