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

package im.vector.app.features.call.conference

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import im.vector.app.R
import im.vector.app.databinding.ViewRemoveJitsiWidgetBinding
import im.vector.app.features.home.room.detail.RoomDetailViewState
import org.matrix.android.sdk.api.session.room.model.Membership

@SuppressLint("ClickableViewAccessibility") class RemoveJitsiWidgetView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private sealed class State {
        object Unmount : State()
        object Idle : State()
        data class Sliding(val initialX: Float, val translationX: Float, val hasReachedActivationThreshold: Boolean) : State()
        object Progress : State()
    }

    private val views: ViewRemoveJitsiWidgetBinding
    private var state: State = State.Unmount
    var onCompleteSliding: (() -> Unit)? = null

    init {
        inflate(context, R.layout.view_remove_jitsi_widget, this)
        views = ViewRemoveJitsiWidgetBinding.bind(this)
        views.removeJitsiSlidingContainer.setOnTouchListener { _, event ->
            val currentState = state
            return@setOnTouchListener when (event.action) {
                MotionEvent.ACTION_DOWN   -> {
                    if (currentState == State.Idle) {
                        val initialX = views.removeJitsiSlidingContainer.x - event.rawX
                        updateState(State.Sliding(initialX, 0f, false))
                    }
                    true
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    if (currentState is State.Sliding) {
                        if (currentState.hasReachedActivationThreshold) {
                            updateState(State.Progress)
                        } else {
                            updateState(State.Idle)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_MOVE   -> {
                    if (currentState is State.Sliding) {
                        val translationX = (currentState.initialX + event.rawX).coerceAtLeast(0f)
                        val hasReachedActivationThreshold = translationX >= views.root.width / 4
                        updateState(State.Sliding(currentState.initialX, translationX, hasReachedActivationThreshold))
                    }
                    true
                }
                else                      -> false
            }
        }
        renderInternalState(state)
    }

    fun render(roomDetailViewState: RoomDetailViewState) {
        val summary = roomDetailViewState.asyncRoomSummary()
        val newState = if (summary?.membership != Membership.JOIN ||
                roomDetailViewState.isWebRTCCallOptionAvailable() ||
                !roomDetailViewState.isAllowedToManageWidgets ||
                roomDetailViewState.jitsiState.widgetId == null) {
            State.Unmount
        } else if (roomDetailViewState.jitsiState.deleteWidgetInProgress) {
            State.Progress
        } else {
            State.Idle
        }
        // Don't force Idle if we are already sliding
        if (state is State.Sliding && newState is State.Idle) {
            return
        } else {
            updateState(newState)
        }
    }

    private fun updateState(newState: State) {
        if (newState == state) {
            return
        }
        renderInternalState(newState)
        state = newState
        if (state == State.Progress) {
            onCompleteSliding?.invoke()
        }
    }

    private fun renderInternalState(state: State) {
        isVisible = state != State.Unmount
        when (state) {
            State.Progress   -> {
                isVisible = true
                views.updateVisibilities(true)
                views.updateHangupColors(true)
            }
            State.Idle       -> {
                isVisible = true
                views.updateVisibilities(false)
                views.removeJitsiSlidingContainer.translationX = 0f
                views.updateHangupColors(false)
            }
            is State.Sliding -> {
                isVisible = true
                views.updateVisibilities(false)
                views.removeJitsiSlidingContainer.translationX = state.translationX
                views.updateHangupColors(state.hasReachedActivationThreshold)
            }
            else             -> Unit
        }
    }

    private fun ViewRemoveJitsiWidgetBinding.updateVisibilities(isProgress: Boolean) {
        removeJitsiProgressContainer.isVisible = isProgress
        removeJitsiHangupContainer.isVisible = !isProgress
        removeJitsiSlidingContainer.isVisible = !isProgress
    }

    private fun ViewRemoveJitsiWidgetBinding.updateHangupColors(activated: Boolean) {
        val iconTintColor: Int
        val bgColor: Int
        if (activated) {
            bgColor = ContextCompat.getColor(context, R.color.palette_vermilion)
            iconTintColor = ContextCompat.getColor(context, R.color.palette_white)
        } else {
            bgColor = ContextCompat.getColor(context, android.R.color.transparent)
            iconTintColor = ContextCompat.getColor(context, R.color.palette_vermilion)
        }
        removeJitsiHangupContainer.setBackgroundColor(bgColor)
        ImageViewCompat.setImageTintList(removeJitsiHangupIcon, ColorStateList.valueOf(iconTintColor))
    }
}
