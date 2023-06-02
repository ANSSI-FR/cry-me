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

package im.vector.app.features.spaces.share

import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.powerlevel.PowerLevelsFlowFactory
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper

class ShareSpaceViewModel @AssistedInject constructor(
        @Assisted private val initialState: ShareSpaceViewState,
        private val session: Session) : VectorViewModel<ShareSpaceViewState, ShareSpaceAction, ShareSpaceViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<ShareSpaceViewModel, ShareSpaceViewState> {
        override fun create(initialState: ShareSpaceViewState): ShareSpaceViewModel
    }

    companion object : MavericksViewModelFactory<ShareSpaceViewModel, ShareSpaceViewState> by hiltMavericksViewModelFactory()

    init {
        val roomSummary = session.getRoomSummary(initialState.spaceId)
        setState {
            copy(
                    spaceSummary = roomSummary?.let { Success(it) } ?: Uninitialized,
                    canShareLink = roomSummary?.isPublic.orFalse()
            )
        }
        observePowerLevel()
    }

    private fun observePowerLevel() {
        val room = session.getRoom(initialState.spaceId) ?: return
        PowerLevelsFlowFactory(room)
                .createFlow()
                .onEach { powerLevelContent ->
                    val powerLevelsHelper = PowerLevelsHelper(powerLevelContent)
                    setState {
                        copy(
                                canInviteByMxId = powerLevelsHelper.isUserAbleToInvite(session.myUserId)
                        )
                    }
                }
                .launchIn(viewModelScope)
    }

    override fun handle(action: ShareSpaceAction) {
        when (action) {
            ShareSpaceAction.InviteByLink -> {
                val roomSummary = session.getRoomSummary(initialState.spaceId)
                val alias = roomSummary?.canonicalAlias
                val permalink = if (alias != null) {
                    session.permalinkService().createPermalink(alias)
                } else {
                    session.permalinkService().createRoomPermalink(initialState.spaceId)
                }
                if (permalink != null) {
                    _viewEvents.post(ShareSpaceViewEvents.ShowInviteByLink(permalink, roomSummary?.name ?: ""))
                }
            }
            ShareSpaceAction.InviteByMxId -> {
                _viewEvents.post(ShareSpaceViewEvents.NavigateToInviteUser(initialState.spaceId))
            }
        }
    }
}