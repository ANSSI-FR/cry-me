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

package org.matrix.android.sdk.internal.session.directory

import org.matrix.android.sdk.internal.network.NetworkConstants
import org.matrix.android.sdk.internal.session.room.alias.AddRoomAliasBody
import org.matrix.android.sdk.internal.session.room.alias.RoomAliasDescription
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

internal interface DirectoryAPI {
    /**
     * Get the room ID associated to the room alias.
     *
     * @param roomAlias the room alias.
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "directory/room/{roomAlias}")
    suspend fun getRoomIdByAlias(@Path("roomAlias") roomAlias: String): RoomAliasDescription

    /**
     * Get the room directory visibility.
     *
     * @param roomId the room id.
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "directory/list/room/{roomId}")
    suspend fun getRoomDirectoryVisibility(@Path("roomId") roomId: String): RoomDirectoryVisibilityJson

    /**
     * Set the room directory visibility.
     *
     * @param roomId the room id.
     * @param body the body containing the new directory visibility
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "directory/list/room/{roomId}")
    suspend fun setRoomDirectoryVisibility(@Path("roomId") roomId: String,
                                           @Body body: RoomDirectoryVisibilityJson)

    /**
     * Add alias to the room.
     * @param roomAlias the room alias.
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "directory/room/{roomAlias}")
    suspend fun addRoomAlias(@Path("roomAlias") roomAlias: String,
                             @Body body: AddRoomAliasBody)

    /**
     * Delete a room alias
     * @param roomAlias the room alias.
     */
    @DELETE(NetworkConstants.URI_API_PREFIX_PATH_R0 + "directory/room/{roomAlias}")
    suspend fun deleteRoomAlias(@Path("roomAlias") roomAlias: String)
}
