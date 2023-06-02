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

package im.vector.app.features.home.room.detail.timeline.helper

import android.graphics.drawable.Drawable
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import dagger.hilt.android.scopes.ActivityScoped
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.features.home.room.detail.timeline.MessageColorProvider
import im.vector.app.features.home.room.detail.timeline.item.MessageFileItem
import org.matrix.android.sdk.api.session.file.ContentDownloadStateTracker
import javax.inject.Inject

@ActivityScoped
class ContentDownloadStateTrackerBinder @Inject constructor(private val activeSessionHolder: ActiveSessionHolder,
                                                            private val messageColorProvider: MessageColorProvider,
                                                            private val errorFormatter: ErrorFormatter) {

    private val updateListeners = mutableMapOf<String, ContentDownloadUpdater>()

    fun bind(mxcUrl: String,
             holder: MessageFileItem.Holder) {
        activeSessionHolder.getSafeActiveSession()?.also { session ->
            val downloadStateTracker = session.contentDownloadProgressTracker()
            val updateListener = ContentDownloadUpdater(holder, messageColorProvider, errorFormatter)
            updateListeners[mxcUrl] = updateListener
            downloadStateTracker.track(mxcUrl, updateListener)
        }
    }

    fun unbind(mxcUrl: String) {
        activeSessionHolder.getSafeActiveSession()?.also { session ->
            val downloadStateTracker = session.contentDownloadProgressTracker()
            updateListeners[mxcUrl]?.also {
                it.stop()
                downloadStateTracker.unTrack(mxcUrl, it)
            }
        }
    }

    fun clear() {
        activeSessionHolder.getSafeActiveSession()?.also {
            it.contentDownloadProgressTracker().clear()
        }
    }
}

private class ContentDownloadUpdater(private val holder: MessageFileItem.Holder,
                                     private val messageColorProvider: MessageColorProvider,
                                     private val errorFormatter: ErrorFormatter) : ContentDownloadStateTracker.UpdateListener {

    override fun onDownloadStateUpdate(state: ContentDownloadStateTracker.State) {
        when (state) {
            ContentDownloadStateTracker.State.Idle           -> handleIdle()
            is ContentDownloadStateTracker.State.Downloading -> handleProgress(state)
            ContentDownloadStateTracker.State.Decrypting     -> handleDecrypting()
            ContentDownloadStateTracker.State.Success        -> handleSuccess()
            is ContentDownloadStateTracker.State.Failure     -> handleFailure()
        }
    }

    private var animatedDrawable: AnimatedVectorDrawableCompat? = null
    private var animationLoopCallback = object : Animatable2Compat.AnimationCallback() {
        override fun onAnimationEnd(drawable: Drawable?) {
            animatedDrawable?.start()
        }
    }

    fun stop() {
        animatedDrawable?.unregisterAnimationCallback(animationLoopCallback)
        animatedDrawable?.stop()
        animatedDrawable = null
    }

    private fun handleIdle() {
        holder.fileDownloadProgress.progress = 0
        holder.fileDownloadProgress.isIndeterminate = false
    }

    private fun handleDecrypting() {
        holder.fileDownloadProgress.isIndeterminate = true
    }

    private fun handleProgress(state: ContentDownloadStateTracker.State.Downloading) {
        doHandleProgress(state.current, state.total)
    }

    private fun doHandleProgress(current: Long, total: Long) {
        val percent = 100L * (current.toFloat() / total.toFloat())
        holder.fileDownloadProgress.isIndeterminate = false
        holder.fileDownloadProgress.progress = percent.toInt()
        if (animatedDrawable == null) {
            animatedDrawable = AnimatedVectorDrawableCompat.create(holder.view.context, R.drawable.ic_download_anim)
            holder.fileImageView.setImageDrawable(animatedDrawable)
            animatedDrawable?.start()
            animatedDrawable?.registerAnimationCallback(animationLoopCallback)
        }
    }

    private fun handleFailure() {
        stop()
        holder.fileDownloadProgress.isIndeterminate = false
        holder.fileDownloadProgress.progress = 0
        holder.fileImageView.setImageResource(R.drawable.ic_close_round)
    }

    private fun handleSuccess() {
        stop()
        holder.fileDownloadProgress.isIndeterminate = false
        holder.fileDownloadProgress.progress = 100
        holder.fileImageView.setImageResource(R.drawable.ic_paperclip)
    }
}
