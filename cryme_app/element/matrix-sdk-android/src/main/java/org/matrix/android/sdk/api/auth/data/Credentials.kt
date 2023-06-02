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

package org.matrix.android.sdk.api.auth.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.internal.util.md5

/**
 * This data class hold credentials user data.
 * You shouldn't have to instantiate it.
 * The access token should be use to authenticate user in all server requests.
 * Ref: https://matrix.org/docs/spec/client_server/latest#post-matrix-client-r0-login
 */
@JsonClass(generateAdapter = true)
data class Credentials(
        /**
         * The fully-qualified Matrix ID that has been registered.
         */
        @Json(name = "user_id") val userId: String,
        /**
         * An access token for the account. This access token can then be used to authorize other requests.
         */
        @Json(name = "access_token") val accessToken: String,
        /**
         * Not documented
         */
        @Json(name = "refresh_token") val refreshToken: String?,
        /**
         * The server_name of the homeserver on which the account has been registered.
         * @Deprecated. Clients should extract the server_name from user_id (by splitting at the first colon)
         * if they require it. Note also that homeserver is not spelt this way.
         */
        @Json(name = "home_server") val homeServer: String?,
        /**
         * ID of the logged-in device. Will be the same as the corresponding parameter in the request, if one was specified.
         */
        @Json(name = "device_id") val deviceId: String?,
        /**
         * Optional client configuration provided by the server. If present, clients SHOULD use the provided object to
         * reconfigure themselves, optionally validating the URLs within.
         * This object takes the same form as the one returned from .well-known autodiscovery.
         */
        @Json(name = "well_known") val discoveryInformation: DiscoveryInformation? = null
)

internal fun Credentials.sessionId(): String {
    return (if (deviceId.isNullOrBlank()) userId else "$userId|$deviceId").md5()
}
