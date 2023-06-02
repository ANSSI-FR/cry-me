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

package org.matrix.android.sdk.api.session.room.members

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary

/**
 * This interface defines methods to handling membership. It's implemented at the room level.
 */
interface MembershipService {

    /**
     * This methods load all room members if it was done yet.
     */
    suspend fun loadRoomMembersIfNeeded()

    /**
     * Return the roomMember with userId or null.
     * @param userId the userId param to look for
     *
     * @return the roomMember with userId or null
     */
    fun getRoomMember(userId: String): RoomMemberSummary?

    /**
     * Return all the roomMembers of the room with params
     * @param queryParams the params to query for
     * @return a roomMember list.
     */
    fun getRoomMembers(queryParams: RoomMemberQueryParams): List<RoomMemberSummary>

    /**
     * Return all the roomMembers of the room filtered by memberships
     * @param queryParams the params to query for
     * @return a [LiveData] of roomMember list.
     */
    fun getRoomMembersLive(queryParams: RoomMemberQueryParams): LiveData<List<RoomMemberSummary>>

    fun getNumberOfJoinedMembers(): Int

    /**
     * Invite a user in the room
     */
    suspend fun invite(userId: String, reason: String? = null)

    /**
     * Invite a user with email or phone number in the room
     */
    suspend fun invite3pid(threePid: ThreePid)

    /**
     * Ban a user from the room
     */
    suspend fun ban(userId: String, reason: String? = null)

    /**
     * Unban a user from the room
     */
    suspend fun unban(userId: String, reason: String? = null)

    /**
     * Kick a user from the room
     */
    suspend fun kick(userId: String, reason: String? = null)

    /**
     * Join the room, or accept an invitation.
     */
    suspend fun join(reason: String? = null, viaServers: List<String> = emptyList())

    /**
     * Leave the room, or reject an invitation.
     */
    suspend fun leave(reason: String? = null)
}
