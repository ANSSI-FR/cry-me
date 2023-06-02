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

package org.matrix.android.sdk.internal.session.homeserver

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.extensions.orTrue
import org.matrix.android.sdk.api.util.JsonDict

/**
 * Ref: https://matrix.org/docs/spec/client_server/latest#get-matrix-client-r0-capabilities
 */
@JsonClass(generateAdapter = true)
internal data class GetCapabilitiesResult(
        /**
         * Required. The custom capabilities the server supports, using the Java package naming convention.
         */
        @Json(name = "capabilities")
        val capabilities: Capabilities? = null
)

@JsonClass(generateAdapter = true)
internal data class Capabilities(
        /**
         * Capability to indicate if the user can change their password.
         */
        @Json(name = "m.change_password")
        val changePassword: ChangePassword? = null,

        /**
         * This capability describes the default and available room versions a server supports, and at what level of stability.
         * Clients should make use of this capability to determine if users need to be encouraged to upgrade their rooms.
         */
        @Json(name = "m.room_versions")
        val roomVersions: RoomVersions? = null
)

@JsonClass(generateAdapter = true)
internal data class ChangePassword(
        /**
         * Required. True if the user can change their password, false otherwise.
         */
        @Json(name = "enabled")
        val enabled: Boolean?
)

@JsonClass(generateAdapter = true)
internal data class RoomVersions(
        /**
         * Required. The default room version the server is using for new rooms.
         */
        @Json(name = "default")
        val default: String?,

        /**
         * Required. A detailed description of the room versions the server supports.
         */
        @Json(name = "available")
        val available: JsonDict? = null,

        /**
         *  "room_capabilities": {
         *      "knock" : {
         *              "preferred": "7",
         *              "support" : ["7"]
         *      },
         *      "restricted" : {
         *              "preferred": "9",
         *              "support" : ["8", "9"]
         *      }
         * }
         */
        @Json(name = "org.matrix.msc3244.room_capabilities")
        val roomCapabilities: JsonDict? = null
)

// The spec says: If not present, the client should assume that password changes are possible via the API
internal fun GetCapabilitiesResult.canChangePassword(): Boolean {
    return capabilities?.changePassword?.enabled.orTrue()
}
