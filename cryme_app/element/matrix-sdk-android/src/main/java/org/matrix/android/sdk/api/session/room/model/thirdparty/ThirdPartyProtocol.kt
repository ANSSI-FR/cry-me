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

package org.matrix.android.sdk.api.session.room.model.thirdparty

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ThirdPartyProtocol(
        /**
         * Required. Fields which may be used to identify a third party user. These should be ordered to suggest the way that entities may be grouped,
         * where higher groupings are ordered first. For example, the name of a network should be searched before the nickname of a user.
         */
        @Json(name = "user_fields")
        val userFields: List<String>? = null,

        /**
         * Required. Fields which may be used to identify a third party location. These should be ordered to suggest the way that
         * entities may be grouped, where higher groupings are ordered first. For example, the name of a network should be
         * searched before the name of a channel.
         */
        @Json(name = "location_fields")
        val locationFields: List<String>? = null,

        /**
         * Required. A content URI representing an icon for the third party protocol.
         *
         * FIXDOC: This field was not present in legacy Riot, and it is sometimes sent by the server (so not Required?)
         */
        @Json(name = "icon")
        val icon: String? = null,

        /**
         * Required. The type definitions for the fields defined in the user_fields and location_fields. Each entry in those arrays MUST have an entry here.
         * The string key for this object is field name itself.
         *
         * May be an empty object if no fields are defined.
         */
        @Json(name = "field_types")
        val fieldTypes: Map<String, FieldType>? = null,

        /**
         * Required. A list of objects representing independent instances of configuration. For example, multiple networks on IRC
         * if multiple are provided by the same application service.
         */
        @Json(name = "instances")
        val instances: List<ThirdPartyProtocolInstance>? = null
)
