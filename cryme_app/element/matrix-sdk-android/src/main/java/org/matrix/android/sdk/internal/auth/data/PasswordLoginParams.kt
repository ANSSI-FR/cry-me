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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.auth.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes

/**
 * Ref:
 * - https://matrix.org/docs/spec/client_server/r0.5.0#password-based
 * - https://matrix.org/docs/spec/client_server/r0.5.0#identifier-types
 */
@JsonClass(generateAdapter = true)
internal data class PasswordLoginParams(
        @Json(name = "identifier") val identifier: Map<String, String>,
        @Json(name = "password") val password: String,
        @Json(name = "signature") val signature: String?,
        @Json(name = "type") override val type: String,
        @Json(name = "initial_device_display_name") val deviceDisplayName: String?,
        @Json(name = "device_id") val deviceId: String?) : LoginParams {

    companion object {
        private const val IDENTIFIER_KEY_TYPE = "type"

        private const val IDENTIFIER_KEY_TYPE_USER = "m.id.user"
        private const val IDENTIFIER_KEY_USER = "user"

        private const val IDENTIFIER_KEY_TYPE_THIRD_PARTY = "m.id.thirdparty"
        private const val IDENTIFIER_KEY_MEDIUM = "medium"
        private const val IDENTIFIER_KEY_ADDRESS = "address"

        private const val IDENTIFIER_KEY_TYPE_PHONE = "m.id.phone"
        private const val IDENTIFIER_KEY_COUNTRY = "country"
        private const val IDENTIFIER_KEY_PHONE = "phone"
        const val EMPTY_SIGNATURE = "empty"

        fun userIdentifier(user: String,
                           password: String,
                           signature: String,
                           deviceDisplayName: String?,
                           deviceId: String?): PasswordLoginParams {
            return PasswordLoginParams(
                    identifier = mapOf(
                            IDENTIFIER_KEY_TYPE to IDENTIFIER_KEY_TYPE_USER,
                            IDENTIFIER_KEY_USER to user
                    ),
                    password = password,
                    signature = signature,
                    type = LoginFlowTypes.PASSWORD,
                    deviceDisplayName = deviceDisplayName,
                    deviceId = deviceId
            )
        }

        fun thirdPartyIdentifier(medium: String,
                                 address: String,
                                 password: String,
                                 signature: String?,
                                 deviceDisplayName: String?,
                                 deviceId: String?): PasswordLoginParams {
            return PasswordLoginParams(
                    identifier = mapOf(
                            IDENTIFIER_KEY_TYPE to IDENTIFIER_KEY_TYPE_THIRD_PARTY,
                            IDENTIFIER_KEY_MEDIUM to medium,
                            IDENTIFIER_KEY_ADDRESS to address
                    ),
                    password = password,
                    signature = signature,
                    type = LoginFlowTypes.PASSWORD,
                    deviceDisplayName = deviceDisplayName,
                    deviceId = deviceId
            )
        }

        fun phoneIdentifier(country: String,
                            phone: String,
                            password: String,
                            signature: String?,
                            deviceDisplayName: String?,
                            deviceId: String?): PasswordLoginParams {
            return PasswordLoginParams(
                    identifier = mapOf(
                            IDENTIFIER_KEY_TYPE to IDENTIFIER_KEY_TYPE_PHONE,
                            IDENTIFIER_KEY_COUNTRY to country,
                            IDENTIFIER_KEY_PHONE to phone
                    ),
                    password = password,
                    signature = signature,
                    type = LoginFlowTypes.PASSWORD,
                    deviceDisplayName = deviceDisplayName,
                    deviceId = deviceId
            )
        }
    }
}
