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

package org.matrix.android.sdk.internal.session.identity

import org.matrix.android.sdk.api.session.openid.OpenIdToken
import org.matrix.android.sdk.internal.network.NetworkConstants
import org.matrix.android.sdk.internal.session.identity.model.IdentityRegisterResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Ref: https://matrix.org/docs/spec/identity_service/latest
 * This contain the requests which do not need an identity server token
 */
internal interface IdentityAuthAPI {

    /**
     * https://matrix.org/docs/spec/client_server/r0.4.0.html#server-discovery
     * Simple ping call to check if server exists and is alive
     *
     * Ref: https://matrix.org/docs/spec/identity_service/unstable#status-check
     * https://matrix.org/docs/spec/identity_service/latest#get-matrix-identity-v2
     *
     * @return 200 in case of success
     */
    @GET(NetworkConstants.URI_IDENTITY_PREFIX_PATH)
    suspend fun ping()

    /**
     * Ping v1 will be used to check outdated identity server
     */
    @GET("_matrix/identity/api/v1")
    suspend fun pingV1()

    /**
     * Exchanges an OpenID token from the homeserver for an access token to access the identity server.
     * The request body is the same as the values returned by /openid/request_token in the Client-Server API.
     */
    @POST(NetworkConstants.URI_IDENTITY_PATH_V2 + "account/register")
    suspend fun register(@Body openIdToken: OpenIdToken): IdentityRegisterResponse
}
