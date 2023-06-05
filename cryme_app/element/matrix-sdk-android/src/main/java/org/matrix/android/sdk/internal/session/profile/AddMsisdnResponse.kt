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
internal data class AddMsisdnResponse(
        /**
         * Required. The session ID. Session IDs are opaque strings that must consist entirely of the characters [0-9a-zA-Z.=_-].
         * Their length must not exceed 255 characters and they must not be empty.
         */
        @Json(name = "sid")
        val sid: String,

        /**
         * An optional field containing a URL where the client must submit the validation token to, with identical parameters to the Identity
         * Service API's POST /validate/email/submitToken endpoint (without the requirement for an access token).
         * The homeserver must send this token to the user (if applicable), who should then be prompted to provide it to the client.
         *
         * If this field is not present, the client can assume that verification will happen without the client's involvement provided
         * the homeserver advertises this specification version in the /versions response (ie: r0.5.0).
         */
        @Json(name = "submit_url")
        val submitUrl: String? = null,

        /* ==========================================================================================
         * It seems that the homeserver is sending more data, we may need it
         * ========================================================================================== */

        @Json(name = "msisdn")
        val msisdn: String? = null,

        @Json(name = "intl_fmt")
        val formattedMsisdn: String? = null,

        @Json(name = "success")
        val success: Boolean? = null
)
