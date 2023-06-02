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
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.image

import im.vector.app.features.media.ImageContentRenderer
import org.matrix.android.sdk.api.session.events.model.isImageMessage
import org.matrix.android.sdk.api.session.events.model.isVideoMessage
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageImageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVideoContent
import org.matrix.android.sdk.api.session.room.model.message.getFileUrl
import org.matrix.android.sdk.api.session.room.model.message.getThumbnailUrl
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.internal.crypto.attachments.toElementToDecrypt

fun TimelineEvent.buildImageContentRendererData(maxHeight: Int): ImageContentRenderer.Data? {
    return when {
        root.isImageMessage() -> root.getClearContent().toModel<MessageImageContent>()
                ?.let { messageImageContent ->
                    ImageContentRenderer.Data(
                            eventId = eventId,
                            filename = messageImageContent.body,
                            mimeType = messageImageContent.mimeType,
                            url = messageImageContent.getFileUrl(),
                            elementToDecrypt = messageImageContent.encryptedFileInfo?.toElementToDecrypt(),
                            height = messageImageContent.info?.height,
                            maxHeight = maxHeight,
                            width = messageImageContent.info?.width,
                            maxWidth = maxHeight * 2,
                            allowNonMxcUrls = false
                    )
                }
        root.isVideoMessage() -> root.getClearContent().toModel<MessageVideoContent>()
                ?.let { messageVideoContent ->
                    val videoInfo = messageVideoContent.videoInfo
                    ImageContentRenderer.Data(
                            eventId = eventId,
                            filename = messageVideoContent.body,
                            mimeType = videoInfo?.thumbnailInfo?.mimeType,
                            url = videoInfo?.getThumbnailUrl(),
                            elementToDecrypt = videoInfo?.thumbnailFile?.toElementToDecrypt(),
                            height = videoInfo?.thumbnailInfo?.height,
                            maxHeight = maxHeight,
                            width = videoInfo?.thumbnailInfo?.width,
                            maxWidth = maxHeight * 2,
                            allowNonMxcUrls = false
                    )
                }
        else                  -> null
    }
}
