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

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes

/**
 * Open class, parent to all possible authentication parameters
 */
@JsonClass(generateAdapter = true)
internal data class AuthParams(
        @Json(name = "type")
        val type: String,

        /**
         * Note: session can be null for reset password request
         */
        @Json(name = "session")
        val session: String?,

        /**
         * parameter for "m.login.recaptcha" type
         */
        @Json(name = "response")
        val captchaResponse: String? = null,

        /**
         * parameter for "m.login.email.identity" type
         */
        @Json(name = "threepid_creds")
        val threePidCredentials: ThreePidCredentials? = null
) {

    companion object {
        fun createForCaptcha(session: String, captchaResponse: String): AuthParams {
            return AuthParams(
                    type = LoginFlowTypes.RECAPTCHA,
                    session = session,
                    captchaResponse = captchaResponse
            )
        }

        fun createForEmailIdentity(session: String, threePidCredentials: ThreePidCredentials): AuthParams {
            return AuthParams(
                    type = LoginFlowTypes.EMAIL_IDENTITY,
                    session = session,
                    threePidCredentials = threePidCredentials
            )
        }

        /**
         * Note that there is a bug in Synapse (I have to investigate where), but if we pass LoginFlowTypes.MSISDN,
         * the homeserver answer with the login flow with MatrixError fields and not with a simple MatrixError 401.
         */
        fun createForMsisdnIdentity(session: String, threePidCredentials: ThreePidCredentials): AuthParams {
            return AuthParams(
                    type = LoginFlowTypes.MSISDN,
                    session = session,
                    threePidCredentials = threePidCredentials
            )
        }

        fun createForResetPassword(clientSecret: String, sid: String): AuthParams {
            return AuthParams(
                    type = LoginFlowTypes.EMAIL_IDENTITY,
                    session = null,
                    threePidCredentials = ThreePidCredentials(
                            clientSecret = clientSecret,
                            sid = sid
                    )
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class ThreePidCredentials(
        @Json(name = "client_secret")
        val clientSecret: String? = null,

        @Json(name = "id_server")
        val idServer: String? = null,

        @Json(name = "sid")
        val sid: String? = null
)
