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
package org.matrix.android.sdk.internal.session.filter

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents "Filter" as mentioned in the SPEC
 * https://matrix.org/docs/spec/client_server/r0.3.0.html#post-matrix-client-r0-user-userid-filter
 */
@JsonClass(generateAdapter = true)
data class EventFilter(
        /**
         * The maximum number of events to return.
         */
        @Json(name = "limit") val limit: Int? = null,
        /**
         * A list of senders IDs to include. If this list is absent then all senders are included.
         */
        @Json(name = "senders") val senders: List<String>? = null,
        /**
         * A list of sender IDs to exclude. If this list is absent then no senders are excluded.
         * A matching sender will be excluded even if it is listed in the 'senders' filter.
         */
        @Json(name = "not_senders") val notSenders: List<String>? = null,
        /**
         * A list of event types to include. If this list is absent then all event types are included.
         * A '*' can be used as a wildcard to match any sequence of characters.
         */
        @Json(name = "types") val types: List<String>? = null,
        /**
         * A list of event types to exclude. If this list is absent then no event types are excluded.
         * A matching type will be excluded even if it is listed in the 'types' filter.
         * A '*' can be used as a wildcard to match any sequence of characters.
         */
        @Json(name = "not_types") val notTypes: List<String>? = null
) {
    fun hasData(): Boolean {
        return limit != null ||
                senders != null ||
                notSenders != null ||
                types != null ||
                notTypes != null
    }
}
