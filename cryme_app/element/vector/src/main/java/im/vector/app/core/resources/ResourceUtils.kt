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
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.core.resources

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import im.vector.app.core.utils.getFileExtension
import org.matrix.android.sdk.api.util.MimeTypes
import org.matrix.android.sdk.api.util.MimeTypes.normalizeMimeType
import timber.log.Timber
import java.io.InputStream

data class Resource(
        var mContentStream: InputStream? = null,
        var mMimeType: String? = null
) {
    /**
     * Close the content stream.
     */
    fun close() {
        try {
            mMimeType = null

            mContentStream?.close()
            mContentStream = null
        } catch (e: Exception) {
            Timber.e(e, "Resource.close failed")
        }
    }

    /**
     * Tells if the opened resource is a jpeg one.
     *
     * @return true if the opened resource is a jpeg one.
     */
    fun isJpegResource(): Boolean {
        return mMimeType.normalizeMimeType() == MimeTypes.Jpeg
    }
}

/**
 * Get a resource stream and metadata about it given its URI returned from onActivityResult.
 *
 * @param context          the context.
 * @param uri              the URI
 * @param providedMimetype the mimetype
 * @return a [Resource] encapsulating the opened resource stream and associated metadata
 * or `null` if opening the resource stream failed.
 */
fun openResource(context: Context, uri: Uri, providedMimetype: String?): Resource? {
    var mimetype = providedMimetype
    try {
        // if the mime type is not provided, try to find it out
        if (mimetype.isNullOrEmpty()) {
            mimetype = context.contentResolver.getType(uri)

            // try to find the mimetype from the filename
            if (null == mimetype) {
                val extension = getFileExtension(uri.toString())
                if (extension != null) {
                    mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                }
            }
        }

        return Resource(context.contentResolver.openInputStream(uri), mimetype)
    } catch (e: Exception) {
        Timber.e(e, "Failed to open resource input stream")
    }

    return null
}
