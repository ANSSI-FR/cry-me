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

package im.vector.app.features.roomprofile.notifications

import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.unwrap

class RoomNotificationSettingsViewModel @AssistedInject constructor(
        @Assisted initialState: RoomNotificationSettingsViewState,
        session: Session
) : VectorViewModel<RoomNotificationSettingsViewState, RoomNotificationSettingsAction, RoomNotificationSettingsViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RoomNotificationSettingsViewModel, RoomNotificationSettingsViewState> {
       override fun create(initialState: RoomNotificationSettingsViewState): RoomNotificationSettingsViewModel
    }

    companion object : MavericksViewModelFactory<RoomNotificationSettingsViewModel, RoomNotificationSettingsViewState> by hiltMavericksViewModelFactory()

    private val room = session.getRoom(initialState.roomId)!!

    init {
        observeSummary()
        observeNotificationState()
    }

    private fun observeSummary() {
        room.flow().liveRoomSummary()
                .unwrap()
                .execute { async ->
                    copy(roomSummary = async)
                }
    }

    private fun observeNotificationState() {
        room.flow()
                .liveNotificationState()
                .execute {
                    copy(notificationState = it)
                }
    }

    override fun handle(action: RoomNotificationSettingsAction) {
        when (action) {
            is RoomNotificationSettingsAction.SelectNotificationState -> handleSelectNotificationState(action)
        }
    }

    private fun handleSelectNotificationState(action: RoomNotificationSettingsAction.SelectNotificationState) {
        setState { copy(isLoading = true) }
        viewModelScope.launch {
            runCatching {  room.setRoomNotificationState(action.notificationState) }
                    .fold(
                            {
                                setState {
                                    copy(isLoading = false, notificationState = Success(action.notificationState))
                                }
                            },
                            {
                                setState {
                                    copy(isLoading = false)
                                }
                                _viewEvents.post(RoomNotificationSettingsViewEvents.Failure(it))
                            }
                    )
        }
    }
}
