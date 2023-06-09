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
package org.matrix.android.sdk.internal.session.profile

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AddMsisdnBody(
        /**
         * Required. A unique string generated by the client, and used to identify the validation attempt.
         * It must be a string consisting of the characters [0-9a-zA-Z.=_-]. Its length must not exceed
         * 255 characters and it must not be empty.
         */
        @Json(name = "client_secret")
        val clientSecret: String,

        /**
         * Required. The two-letter uppercase ISO-3166-1 alpha-2 country code that the number in
         * phone_number should be parsed as if it were dialled from.
         */
        @Json(name = "country")
        val country: String,

        /**
         * Required. The phone number to validate.
         */
        @Json(name = "phone_number")
        val phoneNumber: String,

        /**
         * Required. The server will only send an SMS if the send_attempt is a number greater than the most
         * recent one which it has seen, scoped to that country + phone_number + client_secret triple. This
         * is to avoid repeatedly sending the same SMS in the case of request retries between the POSTing user
         * and the identity server. The client should increment this value if they desire a new SMS (e.g. a
         * reminder) to be sent.
         */
        @Json(name = "send_attempt")
        val sendAttempt: Int
)
