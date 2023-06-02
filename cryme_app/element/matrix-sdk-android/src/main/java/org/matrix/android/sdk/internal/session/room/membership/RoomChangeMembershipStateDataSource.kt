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

package org.matrix.android.sdk.internal.session.room.membership

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.internal.session.SessionScope
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * This class holds information about rooms that current user is joining or leaving.
 */
@SessionScope
internal class RoomChangeMembershipStateDataSource @Inject constructor() {

    private val mutableLiveStates = MutableLiveData<Map<String, ChangeMembershipState>>(emptyMap())
    private val states = ConcurrentHashMap<String, ChangeMembershipState>()

    /**
     * This will update local states to be synced with the server.
     */
    fun setMembershipFromSync(roomId: String, membership: Membership) {
        if (states.containsKey(roomId)) {
            val newState = membership.toMembershipChangeState()
            updateState(roomId, newState)
        }
    }

    fun updateState(roomId: String, state: ChangeMembershipState) {
        states[roomId] = state
        mutableLiveStates.postValue(states.toMap())
    }

    fun getLiveStates(): LiveData<Map<String, ChangeMembershipState>> {
        return mutableLiveStates
    }

    fun getState(roomId: String): ChangeMembershipState {
        return states.getOrElse(roomId) {
            ChangeMembershipState.Unknown
        }
    }

    private fun Membership.toMembershipChangeState(): ChangeMembershipState {
        return when {
            this == Membership.JOIN -> ChangeMembershipState.Joined
            this.isLeft()           -> ChangeMembershipState.Left
            else                    -> ChangeMembershipState.Unknown
        }
    }
}
