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

package org.matrix.android.sdk.api.session.room.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import timber.log.Timber

/**
 * Class representing the EventType.STATE_ROOM_GUEST_ACCESS state event content
 * Ref: https://matrix.org/docs/spec/client_server/latest#m-room-guest-access
 */
@JsonClass(generateAdapter = true)
data class RoomGuestAccessContent(
        // Required. Whether guests can join the room. One of: ["can_join", "forbidden"]
        @Json(name = "guest_access") val _guestAccess: String? = null
) {
    val guestAccess: GuestAccess? = when (_guestAccess) {
        "can_join"  -> GuestAccess.CanJoin
        "forbidden" -> GuestAccess.Forbidden
        else        -> {
            Timber.w("Invalid value for GuestAccess: `$_guestAccess`")
            null
        }
    }
}

@JsonClass(generateAdapter = false)
enum class GuestAccess(val value: String) {
    @Json(name = "can_join") CanJoin("can_join"),
    @Json(name = "forbidden") Forbidden("forbidden")
}
