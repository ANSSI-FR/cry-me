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

package org.matrix.android.sdk.internal.session.space

import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface SpaceApi {

    /**
     * @param suggestedOnly Optional. If true, return only child events and rooms where the m.space.child event has suggested: true.
     * @param limit: Optional: a client-defined limit to the maximum number of rooms to return per page. Must be a non-negative integer.
     * @param maxDepth: Optional: The maximum depth in the tree (from the root room) to return.
     * The deepest depth returned will not include children events. Defaults to no-limit. Must be a non-negative integer.
     * @param from: Optional. Pagination token given to retrieve the next set of rooms.
     * Note that if a pagination token is provided, then the parameters given for suggested_only and max_depth must be the same.
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "org.matrix.msc2946/rooms/{roomId}/hierarchy")
    suspend fun getSpaceHierarchy(
            @Path("roomId") spaceId: String,
            @Query("suggested_only") suggestedOnly: Boolean?,
            @Query("limit") limit: Int?,
            @Query("max_depth") maxDepth: Int?,
            @Query("from") from: String?): SpacesResponse
}
