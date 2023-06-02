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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.attachments

import im.vector.lib.multipicker.entity.MultiPickerAudioType
import im.vector.lib.multipicker.entity.MultiPickerBaseMediaType
import im.vector.lib.multipicker.entity.MultiPickerBaseType
import im.vector.lib.multipicker.entity.MultiPickerContactType
import im.vector.lib.multipicker.entity.MultiPickerFileType
import im.vector.lib.multipicker.entity.MultiPickerImageType
import im.vector.lib.multipicker.entity.MultiPickerVideoType
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeAudio
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeImage
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeVideo
import timber.log.Timber

fun MultiPickerContactType.toContactAttachment(): ContactAttachment {
    return ContactAttachment(
            displayName = displayName,
            photoUri = photoUri,
            emails = emailList.toList(),
            phones = phoneNumberList.toList()
    )
}

fun MultiPickerFileType.toContentAttachmentData(): ContentAttachmentData {
    if (mimeType == null) Timber.w("No mimeType")
    return ContentAttachmentData(
            mimeType = mimeType,
            type = mapType(),
            size = size,
            name = displayName,
            queryUri = contentUri
    )
}

fun MultiPickerAudioType.toContentAttachmentData(isVoiceMessage: Boolean): ContentAttachmentData {
    if (mimeType == null) Timber.w("No mimeType")
    return ContentAttachmentData(
            mimeType = mimeType,
            type = if (isVoiceMessage) ContentAttachmentData.Type.VOICE_MESSAGE else mapType(),
            size = size,
            name = displayName,
            duration = duration,
            queryUri = contentUri,
            waveform = waveform
    )
}

private fun MultiPickerBaseType.mapType(): ContentAttachmentData.Type {
    return when {
        mimeType?.isMimeTypeImage() == true -> ContentAttachmentData.Type.IMAGE
        mimeType?.isMimeTypeVideo() == true -> ContentAttachmentData.Type.VIDEO
        mimeType?.isMimeTypeAudio() == true -> ContentAttachmentData.Type.AUDIO
        else                                -> ContentAttachmentData.Type.FILE
    }
}

fun MultiPickerBaseType.toContentAttachmentData(): ContentAttachmentData {
    return when (this) {
        is MultiPickerImageType -> toContentAttachmentData()
        is MultiPickerVideoType -> toContentAttachmentData()
        is MultiPickerAudioType -> toContentAttachmentData(isVoiceMessage = false)
        is MultiPickerFileType  -> toContentAttachmentData()
        else                    -> throw IllegalStateException("Unknown file type")
    }
}

fun MultiPickerBaseMediaType.toContentAttachmentData(): ContentAttachmentData {
    return when (this) {
        is MultiPickerImageType -> toContentAttachmentData()
        is MultiPickerVideoType -> toContentAttachmentData()
        else                    -> throw IllegalStateException("Unknown media type")
    }
}

fun MultiPickerImageType.toContentAttachmentData(): ContentAttachmentData {
    if (mimeType == null) Timber.w("No mimeType")
    return ContentAttachmentData(
            mimeType = mimeType,
            type = mapType(),
            name = displayName,
            size = size,
            height = height.toLong(),
            width = width.toLong(),
            exifOrientation = orientation,
            queryUri = contentUri
    )
}

fun MultiPickerVideoType.toContentAttachmentData(): ContentAttachmentData {
    if (mimeType == null) Timber.w("No mimeType")
    return ContentAttachmentData(
            mimeType = mimeType,
            type = ContentAttachmentData.Type.VIDEO,
            size = size,
            height = height.toLong(),
            width = width.toLong(),
            duration = duration,
            name = displayName,
            queryUri = contentUri
    )
}
