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

package org.matrix.android.sdk.internal.session.media

import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.GET
import retrofit2.http.Query

internal interface MediaAPI {
    /**
     * Retrieve the configuration of the content repository
     * Ref: https://matrix.org/docs/spec/client_server/r0.6.1#get-matrix-media-r0-config
     */
    @GET(NetworkConstants.URI_API_MEDIA_PREFIX_PATH_R0 + "config")
    suspend fun getMediaConfig(): GetMediaConfigResult

    /**
     * Get information about a URL for the client. Typically this is called when a client
     * sees a URL in a message and wants to render a preview for the user.
     * Ref: https://matrix.org/docs/spec/client_server/r0.6.1#get-matrix-media-r0-preview-url
     * @param url Required. The URL to get a preview of.
     * @param ts The preferred point in time to return a preview for. The server may return a newer version
     * if it does not have the requested version available.
     */
    @GET(NetworkConstants.URI_API_MEDIA_PREFIX_PATH_R0 + "preview_url")
    suspend fun getPreviewUrlData(@Query("url") url: String, @Query("ts") ts: Long?): JsonDict
}
