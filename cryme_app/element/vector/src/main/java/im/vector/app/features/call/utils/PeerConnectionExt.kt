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
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.call.utils

import im.vector.app.features.call.webrtc.SdpObserverAdapter
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun PeerConnection.awaitCreateOffer(mediaConstraints: MediaConstraints): SessionDescription? = suspendCoroutine { cont ->
    createOffer(object : SdpObserverAdapter() {
        override fun onCreateSuccess(p0: SessionDescription?) {
            super.onCreateSuccess(p0)
            cont.resume(p0)
        }

        override fun onCreateFailure(p0: String?) {
            super.onCreateFailure(p0)
            cont.resumeWithException(IllegalStateException(p0))
        }
    }, mediaConstraints)
}

suspend fun PeerConnection.awaitCreateAnswer(mediaConstraints: MediaConstraints): SessionDescription? = suspendCoroutine { cont ->
    createAnswer(object : SdpObserverAdapter() {
        override fun onCreateSuccess(p0: SessionDescription?) {
            super.onCreateSuccess(p0)
            cont.resume(p0)
        }

        override fun onCreateFailure(p0: String?) {
            super.onCreateFailure(p0)
            cont.resumeWithException(IllegalStateException(p0))
        }
    }, mediaConstraints)
}

suspend fun PeerConnection.awaitSetLocalDescription(sessionDescription: SessionDescription): Unit = suspendCoroutine { cont ->
    setLocalDescription(object : SdpObserverAdapter() {
        override fun onSetFailure(p0: String?) {
            super.onSetFailure(p0)
            cont.resumeWithException(IllegalStateException(p0))
        }

        override fun onSetSuccess() {
            super.onSetSuccess()
            cont.resume(Unit)
        }
    }, sessionDescription)
}

suspend fun PeerConnection.awaitSetRemoteDescription(sessionDescription: SessionDescription): Unit = suspendCoroutine { cont ->
    setRemoteDescription(object : SdpObserverAdapter() {
        override fun onSetFailure(p0: String?) {
            super.onSetFailure(p0)
            cont.resumeWithException(IllegalStateException(p0))
        }

        override fun onSetSuccess() {
            super.onSetSuccess()
            cont.resume(Unit)
        }
    }, sessionDescription)
}
