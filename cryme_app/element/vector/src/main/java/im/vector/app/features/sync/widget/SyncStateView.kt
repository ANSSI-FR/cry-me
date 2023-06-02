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

package im.vector.app.features.sync.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.utils.isAirplaneModeOn
import im.vector.app.databinding.ViewSyncStateBinding
import org.matrix.android.sdk.api.session.initsync.SyncStatusService
import org.matrix.android.sdk.api.session.sync.SyncState

class SyncStateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    LinearLayout(context, attrs, defStyle) {

    private val views: ViewSyncStateBinding

    init {
        inflate(context, R.layout.view_sync_state, this)
        views = ViewSyncStateBinding.bind(this)
        orientation = VERTICAL
    }

    @SuppressLint("SetTextI18n")
    fun render(newState: SyncState,
               incrementalSyncStatus: SyncStatusService.Status.IncrementalSyncStatus,
               pushCounter: Int,
               showDebugInfo: Boolean
    ) {
        views.syncStateDebugInfo.isVisible = showDebugInfo
        if (showDebugInfo) {
            views.syncStateDebugInfoText.text =
                    "Sync thread : ${newState.toHumanReadable()}\nSync request: ${incrementalSyncStatus.toHumanReadable()}"
            views.syncStateDebugInfoPushCounter.text =
                    "Push: $pushCounter"
        }
        views.syncStateProgressBar.isVisible = newState is SyncState.Running && newState.afterPause

        if (newState == SyncState.NoNetwork) {
            val isAirplaneModeOn = isAirplaneModeOn(context)
            views.syncStateNoNetwork.isVisible = isAirplaneModeOn.not()
            views.syncStateNoNetworkAirplane.isVisible = isAirplaneModeOn
        } else {
            views.syncStateNoNetwork.isVisible = false
            views.syncStateNoNetworkAirplane.isVisible = false
        }
    }

    private fun SyncState.toHumanReadable(): String {
        return when (this) {
            SyncState.Idle         -> "Idle"
            SyncState.InvalidToken -> "InvalidToken"
            SyncState.Killed       -> "Killed"
            SyncState.Killing      -> "Killing"
            SyncState.NoNetwork    -> "NoNetwork"
            SyncState.Paused       -> "Paused"
            is SyncState.Running   -> "$this"
        }
    }

    private fun SyncStatusService.Status.IncrementalSyncStatus.toHumanReadable(): String {
        return when (this) {
            SyncStatusService.Status.IncrementalSyncIdle       -> "Idle"
            is SyncStatusService.Status.IncrementalSyncParsing -> "Parsing ${this.rooms} room(s) ${this.toDevice} toDevice(s)"
            SyncStatusService.Status.IncrementalSyncError      -> "Error"
            SyncStatusService.Status.IncrementalSyncDone       -> "Done"
            else                                               -> "?"
        }
    }
}
