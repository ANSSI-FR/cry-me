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
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.home.room.detail

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import im.vector.app.core.utils.Debouncer

/**
 * Show or hide the jumpToBottomView, depending on the scrolling and if the timeline is displaying the more recent event
 * - When user scrolls up (i.e. going to the past): hide
 * - When user scrolls down: show if not displaying last event
 * - When user stops scrolling: show if not displaying last event
 */
class JumpToBottomViewVisibilityManager(
        private val jumpToBottomView: FloatingActionButton,
        private val debouncer: Debouncer,
        recyclerView: RecyclerView,
        private val layoutManager: LinearLayoutManager) {

    init {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                debouncer.cancel("jump_to_bottom_visibility")

                val scrollingToPast = dy < 0

                if (scrollingToPast) {
                    jumpToBottomView.hide()
                } else {
                    maybeShowJumpToBottomViewVisibility()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE     -> {
                        maybeShowJumpToBottomViewVisibilityWithDelay()
                    }
                    RecyclerView.SCROLL_STATE_DRAGGING,
                    RecyclerView.SCROLL_STATE_SETTLING -> Unit
                }
            }
        })
    }

    fun maybeShowJumpToBottomViewVisibilityWithDelay() {
        debouncer.debounce("jump_to_bottom_visibility", 250) {
            maybeShowJumpToBottomViewVisibility()
        }
    }

    private fun maybeShowJumpToBottomViewVisibility() {
        if (layoutManager.findFirstVisibleItemPosition() > 1) {
            jumpToBottomView.show()
        } else {
            jumpToBottomView.hide()
        }
    }
}
