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
package org.matrix.android.sdk.api.session.room.model.roomdirectory

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Class to pass parameters to get the public rooms list
 */
@JsonClass(generateAdapter = true)
data class PublicRoomsParams(
        /**
         * Limit the number of results returned.
         */
        @Json(name = "limit")
        val limit: Int? = null,

        /**
         * A pagination token from a previous request, allowing clients to get the next (or previous) batch of rooms.
         * The direction of pagination is specified solely by which token is supplied, rather than via an explicit flag.
         */
        @Json(name = "since")
        val since: String? = null,

        /**
         * Filter to apply to the results.
         */
        @Json(name = "filter")
        val filter: PublicRoomsFilter? = null,

        /**
         * Whether or not to include all known networks/protocols from application services on the homeserver. Defaults to false.
         */
        @Json(name = "include_all_networks")
        val includeAllNetworks: Boolean = false,

        /**
         * The specific third party network/protocol to request from the homeserver. Can only be used if include_all_networks is false.
         */
        @Json(name = "third_party_instance_id")
        val thirdPartyInstanceId: String? = null
)
