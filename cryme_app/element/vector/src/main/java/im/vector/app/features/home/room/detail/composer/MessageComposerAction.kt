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

package im.vector.app.features.home.room.detail.composer

import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.features.home.room.detail.composer.voice.VoiceMessageRecorderView
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioContent

sealed class MessageComposerAction : VectorViewModelAction {
    data class SendMessage(val text: CharSequence, val autoMarkdown: Boolean) : MessageComposerAction()
    data class EnterEditMode(val eventId: String, val text: String) : MessageComposerAction()
    data class EnterQuoteMode(val eventId: String, val text: String) : MessageComposerAction()
    data class EnterReplyMode(val eventId: String, val text: String) : MessageComposerAction()
    data class EnterRegularMode(val text: String, val fromSharing: Boolean) : MessageComposerAction()
    data class UserIsTyping(val isTyping: Boolean) : MessageComposerAction()
    data class OnTextChanged(val text: CharSequence) : MessageComposerAction()
    data class OnEntersBackground(val composerText: String) : MessageComposerAction()

    // Voice Message
    data class InitializeVoiceRecorder(val attachmentData: ContentAttachmentData) : MessageComposerAction()
    data class OnVoiceRecordingUiStateChanged(val uiState: VoiceMessageRecorderView.RecordingUiState) : MessageComposerAction()
    object StartRecordingVoiceMessage : MessageComposerAction()
    data class EndRecordingVoiceMessage(val isCancelled: Boolean) : MessageComposerAction()
    object PauseRecordingVoiceMessage : MessageComposerAction()
    data class PlayOrPauseVoicePlayback(val eventId: String, val messageAudioContent: MessageAudioContent) : MessageComposerAction()
    object PlayOrPauseRecordingPlayback : MessageComposerAction()
    data class EndAllVoiceActions(val deleteRecord: Boolean = true) : MessageComposerAction()
}
