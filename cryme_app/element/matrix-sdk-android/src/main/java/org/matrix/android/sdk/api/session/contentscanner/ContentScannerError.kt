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
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.contentscanner

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ContentScannerError(
        @Json(name = "info") val info: String? = null,
        @Json(name = "reason") val reason: String? = null
) {
    companion object {
        // 502 The server failed to request media from the media repo.
        const val REASON_MCS_MEDIA_REQUEST_FAILED = "MCS_MEDIA_REQUEST_FAILED"

        /* 400 The server failed to decrypt the encrypted media downloaded from the media repo.*/
        const val REASON_MCS_MEDIA_FAILED_TO_DECRYPT = "MCS_MEDIA_FAILED_TO_DECRYPT"

        /* 403 The server scanned the downloaded media but the antivirus script returned a non-zero exit code.*/
        const val REASON_MCS_MEDIA_NOT_CLEAN = "MCS_MEDIA_NOT_CLEAN"

        /* 403 The provided encrypted_body could not be decrypted. The client should request the public key of the server and then retry (once).*/
        const val REASON_MCS_BAD_DECRYPTION = "MCS_BAD_DECRYPTION"

        /* 400 The request body contains malformed JSON.*/
        const val REASON_MCS_MALFORMED_JSON = "MCS_MALFORMED_JSON"
    }
}

class ScanFailure(val error: ContentScannerError, val httpCode: Int, cause: Throwable? = null) : Throwable(cause = cause)

// For Glide, which deals with Exception and not with Throwable
fun ScanFailure.toException() = Exception(this)
fun Throwable.toScanFailure() = this.cause as? ScanFailure
