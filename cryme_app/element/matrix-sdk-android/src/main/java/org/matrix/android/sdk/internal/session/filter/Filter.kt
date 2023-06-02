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
import org.matrix.android.sdk.internal.di.MoshiProvider

/**
 * Class which can be parsed to a filter json string. Used for POST and GET
 * Have a look here for further information:
 * https://matrix.org/docs/spec/client_server/r0.3.0.html#post-matrix-client-r0-user-userid-filter
 */
@JsonClass(generateAdapter = true)
internal data class Filter(
        /**
         * List of event fields to include. If this list is absent then all fields are included. The entries may
         * include '.' characters to indicate sub-fields. So ['content.body'] will include the 'body' field of the
         * 'content' object. A literal '.' character in a field name may be escaped using a '\'. A server may
         * include more fields than were requested.
         */
        @Json(name = "event_fields") val eventFields: List<String>? = null,
        /**
         * The format to use for events. 'client' will return the events in a format suitable for clients.
         * 'federation' will return the raw event as received over federation. The default is 'client'. One of: ["client", "federation"]
         */
        @Json(name = "event_format") val eventFormat: String? = null,
        /**
         * The presence updates to include.
         */
        @Json(name = "presence") val presence: EventFilter? = null,
        /**
         * The user account data that isn't associated with rooms to include.
         */
        @Json(name = "account_data") val accountData: EventFilter? = null,
        /**
         * Filters to be applied to room data.
         */
        @Json(name = "room") val room: RoomFilter? = null
) {

    fun toJSONString(): String {
        return MoshiProvider.providesMoshi().adapter(Filter::class.java).toJson(this)
    }
}
