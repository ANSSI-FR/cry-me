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

package org.matrix.android.sdk.api.session.search

/**
 * This interface defines methods to search messages in rooms.
 */
interface SearchService {

    /**
     * Generic function to search a term in a room.
     * Ref: https://matrix.org/docs/spec/client_server/latest#module-search
     * @param searchTerm the term to search
     * @param roomId the roomId to search term inside
     * @param nextBatch the token that retrieved from the previous response. Should be provided to get the next batch of results
     * @param orderByRecent if true, the most recent message events will return in the first places of the list
     * @param limit the maximum number of events to return.
     * @param beforeLimit how many events before the result are returned.
     * @param afterLimit how many events after the result are returned.
     * @param includeProfile requests that the server returns the historic profile information for the users that sent the events that were returned.
     */
    suspend fun search(searchTerm: String,
                       roomId: String,
                       nextBatch: String?,
                       orderByRecent: Boolean,
                       limit: Int,
                       beforeLimit: Int,
                       afterLimit: Int,
                       includeProfile: Boolean): SearchResult
}
