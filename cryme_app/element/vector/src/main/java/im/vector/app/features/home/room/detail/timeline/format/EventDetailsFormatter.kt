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
 * Copyright 2021 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.format

import android.content.Context
import im.vector.app.core.utils.TextUtils
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.isAudioMessage
import org.matrix.android.sdk.api.session.events.model.isFileMessage
import org.matrix.android.sdk.api.session.events.model.isImageMessage
import org.matrix.android.sdk.api.session.events.model.isVideoMessage
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioContent
import org.matrix.android.sdk.api.session.room.model.message.MessageFileContent
import org.matrix.android.sdk.api.session.room.model.message.MessageImageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVideoContent
import org.threeten.bp.Duration
import javax.inject.Inject

class EventDetailsFormatter @Inject constructor(
        private val context: Context
) {

    fun format(event: Event?): CharSequence? {
        event ?: return null

        if (event.isRedacted()) {
            return null
        }

        if (event.isEncrypted() && event.mxDecryptionResult == null) {
            return null
        }

        return when {
            event.isImageMessage() -> formatForImageMessage(event)
            event.isVideoMessage() -> formatForVideoMessage(event)
            event.isAudioMessage() -> formatForAudioMessage(event)
            event.isFileMessage()  -> formatForFileMessage(event)
            else                   -> null
        }
    }

    /**
     * Example: "1024 x 720 - 670 kB"
     */
    private fun formatForImageMessage(event: Event): CharSequence? {
        return event.getClearContent().toModel<MessageImageContent>()?.info
                ?.let { "${it.width} x ${it.height} - ${it.size.asFileSize()}" }
    }

    /**
     * Example: "02:45 - 1024 x 720 - 670 kB"
     */
    private fun formatForVideoMessage(event: Event): CharSequence? {
        return event.getClearContent().toModel<MessageVideoContent>()?.videoInfo
                ?.let { "${it.duration.asDuration()} - ${it.width} x ${it.height} - ${it.size.asFileSize()}" }
    }

    /**
     * Example: "02:45 - 670 kB"
     */
    private fun formatForAudioMessage(event: Event): CharSequence? {
        return event.getClearContent().toModel<MessageAudioContent>()?.audioInfo
                ?.let { audioInfo ->
                    listOfNotNull(audioInfo.duration?.asDuration(), audioInfo.size?.asFileSize())
                            .joinToString(" - ")
                            .takeIf { it.isNotEmpty() }
                }
    }

    /**
     * Example: "670 kB - application/pdf"
     */
    private fun formatForFileMessage(event: Event): CharSequence? {
        return event.getClearContent().toModel<MessageFileContent>()?.info
                ?.let { "${it.size.asFileSize()} - ${it.mimeType}" }
    }

    private fun Long.asFileSize() = TextUtils.formatFileSize(context, this)
    private fun Int.asDuration() = TextUtils.formatDuration(Duration.ofMillis(toLong()))
}
