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

package org.matrix.android.sdk.internal.session.room.timeline

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Event

@JsonClass(generateAdapter = true)
internal data class PaginationResponse(
        /**
         * The token the pagination starts from. If dir=b this will be the token supplied in from.
         */
        @Json(name = "start") override val start: String? = null,
        /**
         * The token the pagination ends at. If dir=b this token should be used again to request even earlier events.
         */
        @Json(name = "end") override val end: String? = null,
        /**
         * A list of room events. The order depends on the dir parameter. For dir=b events will be in
         * reverse-chronological order, for dir=f in chronological order, so that events start at the from point.
         */
        @Json(name = "chunk") val chunk: List<Event>? = null,
        /**
         * A list of state events relevant to showing the chunk. For example, if lazy_load_members is enabled
         * in the filter then this may contain the membership events for the senders of events in the chunk.
         *
         * Unless include_redundant_members is true, the server may remove membership events which would have
         * already been sent to the client in prior calls to this endpoint, assuming the membership of those members has not changed.
         */
        @Json(name = "state") override val stateEvents: List<Event>? = null
) : TokenChunkEvent {
    override val events: List<Event>
        get() = chunk.orEmpty()
}
