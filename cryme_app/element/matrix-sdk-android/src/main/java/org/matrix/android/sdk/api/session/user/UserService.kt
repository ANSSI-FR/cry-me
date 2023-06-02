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

package org.matrix.android.sdk.api.session.user

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.Optional

/**
 * This interface defines methods to get users. It's implemented at the session level.
 */
interface UserService {

    /**
     * Get a user from a userId
     * @param userId the userId to look for.
     * @return a user with userId or null
     */
    fun getUser(userId: String): User?

    /**
     * Try to resolve user from known users, or using profile api
     */
    suspend fun resolveUser(userId: String): User

    /**
     * Search list of users on server directory.
     * @param search the searched term
     * @param limit the max number of users to return
     * @param excludedUserIds the user ids to filter from the search
     * @return Cancelable
     */
    suspend fun searchUsersDirectory(search: String, limit: Int, excludedUserIds: Set<String>): List<User>

    /**
     * Observe a live user from a userId
     * @param userId the userId to look for.
     * @return a LiveData of user with userId
     */
    fun getUserLive(userId: String): LiveData<Optional<User>>

    /**
     * Observe a live list of users sorted alphabetically
     * @return a Livedata of users
     */
    fun getUsersLive(): LiveData<List<User>>

    /**
     * Observe a live [PagedList] of users sorted alphabetically. You can filter the users.
     * @param filter the filter. It will look into userId and displayName.
     * @param excludedUserIds userId list which will be excluded from the result list.
     * @return a Livedata of users
     */
    fun getPagedUsersLive(filter: String? = null, excludedUserIds: Set<String>? = null): LiveData<PagedList<User>>

    /**
     * Get list of ignored users
     */
    fun getIgnoredUsersLive(): LiveData<List<User>>

    /**
     * Ignore users
     */
    suspend fun ignoreUserIds(userIds: List<String>)

    /**
     * Un-ignore some users
     */
    suspend fun unIgnoreUserIds(userIds: List<String>)
}
