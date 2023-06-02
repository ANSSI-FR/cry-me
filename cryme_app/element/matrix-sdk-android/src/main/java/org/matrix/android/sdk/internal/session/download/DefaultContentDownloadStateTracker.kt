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

package org.matrix.android.sdk.internal.session.download

import android.os.Handler
import android.os.Looper
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.file.ContentDownloadStateTracker
import org.matrix.android.sdk.internal.session.SessionScope
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class DefaultContentDownloadStateTracker @Inject constructor() : ProgressListener, ContentDownloadStateTracker {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val states = mutableMapOf<String, ContentDownloadStateTracker.State>()
    private val listeners = mutableMapOf<String, MutableList<ContentDownloadStateTracker.UpdateListener>>()

    override fun track(key: String, updateListener: ContentDownloadStateTracker.UpdateListener) {
        val listeners = listeners.getOrPut(key) { ArrayList() }
        if (!listeners.contains(updateListener)) {
            listeners.add(updateListener)
        }
        val currentState = states[key] ?: ContentDownloadStateTracker.State.Idle
        mainHandler.post {
            try {
                updateListener.onDownloadStateUpdate(currentState)
            } catch (e: Exception) {
                Timber.e(e, "## ContentUploadStateTracker.onUpdate() failed")
            }
        }
    }

    override fun unTrack(key: String, updateListener: ContentDownloadStateTracker.UpdateListener) {
        listeners[key]?.apply {
            remove(updateListener)
        }
    }

    override fun clear() {
        states.clear()
        listeners.clear()
    }

//    private fun URL.toKey() = toString()

    override fun update(url: String, bytesRead: Long, contentLength: Long, done: Boolean) {
        mainHandler.post {
            Timber.v("## DL Progress url:$url read:$bytesRead total:$contentLength done:$done")
            if (done) {
                updateState(url, ContentDownloadStateTracker.State.Success)
            } else {
                updateState(url, ContentDownloadStateTracker.State.Downloading(bytesRead, contentLength, contentLength == -1L))
            }
        }
    }

    override fun error(url: String, errorCode: Int) {
        mainHandler.post {
            Timber.v("## DL Progress Error code:$errorCode")
            updateState(url, ContentDownloadStateTracker.State.Failure(errorCode))
            listeners[url]?.forEach {
                tryOrNull { it.onDownloadStateUpdate(ContentDownloadStateTracker.State.Failure(errorCode)) }
            }
        }
    }

    private fun updateState(url: String, state: ContentDownloadStateTracker.State) {
        states[url] = state
        listeners[url]?.forEach {
            tryOrNull { it.onDownloadStateUpdate(state) }
        }
    }
}
