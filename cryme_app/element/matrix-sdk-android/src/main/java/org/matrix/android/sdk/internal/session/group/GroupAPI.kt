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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.group

import org.matrix.android.sdk.internal.network.NetworkConstants
import org.matrix.android.sdk.internal.session.group.model.GroupRooms
import org.matrix.android.sdk.internal.session.group.model.GroupSummaryResponse
import org.matrix.android.sdk.internal.session.group.model.GroupUsers
import retrofit2.http.GET
import retrofit2.http.Path

internal interface GroupAPI {

    /**
     * Request a group summary
     *
     * @param groupId the group id
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "groups/{groupId}/summary")
    suspend fun getSummary(@Path("groupId") groupId: String): GroupSummaryResponse

    /**
     * Request the rooms list.
     *
     * @param groupId the group id
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "groups/{groupId}/rooms")
    suspend fun getRooms(@Path("groupId") groupId: String): GroupRooms

    /**
     * Request the users list.
     *
     * @param groupId the group id
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "groups/{groupId}/users")
    suspend fun getUsers(@Path("groupId") groupId: String): GroupUsers
}
