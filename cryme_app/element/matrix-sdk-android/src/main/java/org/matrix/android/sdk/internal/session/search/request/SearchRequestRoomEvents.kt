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

package org.matrix.android.sdk.internal.session.search.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SearchRequestRoomEvents(
        /**
         * Required. The string to search events for.
         */
        @Json(name = "search_term")
        val searchTerm: String,

        /**
         * The keys to search. Defaults to all. One of: ["content.body", "content.name", "content.topic"]
         */
        @Json(name = "keys")
        val keys: Any? = null,

        /**
         * This takes a filter.
         */
        @Json(name = "filter")
        val filter: SearchRequestFilter? = null,

        /**
         * The order in which to search for results. By default, this is "rank". One of: ["recent", "rank"]
         */
        @Json(name = "order_by")
        val orderBy: SearchRequestOrder? = null,

        /**
         * Configures whether any context for the events returned are included in the response.
         */
        @Json(name = "event_context")
        val eventContext: SearchRequestEventContext? = null,

        /**
         * Requests the server return the current state for each room returned.
         */
        @Json(name = "include_state")
        val include_state: Boolean? = null

        /**
         * Requests that the server partitions the result set based on the provided list of keys.
         */
        // val groupings: SearchRequestGroupings? = null
)
