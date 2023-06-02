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

package org.matrix.android.sdk.api.session.content

import org.matrix.android.sdk.internal.crypto.attachments.ElementToDecrypt

/**
 * This interface defines methods for accessing content from the current session.
 */
interface ContentUrlResolver {

    enum class ThumbnailMethod(val value: String) {
        CROP("crop"),
        SCALE("scale")
    }

    /**
     * URL to use to upload content
     */
    val uploadUrl: String

    /**
     * Get the actual URL for accessing the full-size image of a Matrix media content URI.
     *
     * @param contentUrl  the Matrix media content URI (in the form of "mxc://...").
     * @return the URL to access the described resource, or null if the url is invalid.
     */
    fun resolveFullSize(contentUrl: String?): String?

    /**
     * Get the ResolvedMethod to download a URL
     *
     * @param contentUrl  the Matrix media content URI (in the form of "mxc://...").
     * @param elementToDecrypt Encryption data may be required if you use a content scanner
     * @return the Method to access resource, or null if invalid
     */
    fun resolveForDownload(contentUrl: String?, elementToDecrypt: ElementToDecrypt? = null): ResolvedMethod?

    /**
     * Get the actual URL for accessing the thumbnail image of a given Matrix media content URI.
     *
     * @param contentUrl the Matrix media content URI (in the form of "mxc://...").
     * @param width      the desired width
     * @param height     the desired height
     * @param method     the desired method (METHOD_CROP or METHOD_SCALE)
     * @return the URL to access the described resource, or null if the url is invalid.
     */
    fun resolveThumbnail(contentUrl: String?, width: Int, height: Int, method: ThumbnailMethod): String?

    sealed class ResolvedMethod {
        data class GET(val url: String) : ResolvedMethod()
        data class POST(val url: String, val jsonBody: String) : ResolvedMethod()
    }
}
