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

package org.matrix.android.sdk.internal.auth.registration

import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.auth.registration.RegisterThreePid

/**
 * Container to store the data when a three pid is in validation step
 */
@JsonClass(generateAdapter = true)
internal data class ThreePidData(
        val email: String,
        val msisdn: String,
        val country: String,
        val addThreePidRegistrationResponse: AddThreePidRegistrationResponse,
        val registrationParams: RegistrationParams
) {
    val threePid: RegisterThreePid
        get() {
            return if (email.isNotBlank()) {
                RegisterThreePid.Email(email)
            } else {
                RegisterThreePid.Msisdn(msisdn, country)
            }
        }

    companion object {
        fun from(threePid: RegisterThreePid,
                 addThreePidRegistrationResponse: AddThreePidRegistrationResponse,
                 registrationParams: RegistrationParams): ThreePidData {
            return when (threePid) {
                is RegisterThreePid.Email  ->
                    ThreePidData(threePid.email, "", "", addThreePidRegistrationResponse, registrationParams)
                is RegisterThreePid.Msisdn ->
                    ThreePidData("", threePid.msisdn, threePid.countryCode, addThreePidRegistrationResponse, registrationParams)
            }
        }
    }
}
