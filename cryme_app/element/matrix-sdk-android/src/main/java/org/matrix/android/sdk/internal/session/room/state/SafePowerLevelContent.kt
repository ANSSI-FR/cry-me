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
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.state

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.util.JsonDict

@JsonClass(generateAdapter = true)
internal data class SerializablePowerLevelsContent(
        @Json(name = "ban") val ban: Int?,
        @Json(name = "kick") val kick: Int?,
        @Json(name = "invite") val invite: Int?,
        @Json(name = "redact") val redact: Int?,
        @Json(name = "events_default") val eventsDefault: Int?,
        @Json(name = "events") val events: Map<String, Int>?,
        @Json(name = "users_default") val usersDefault: Int?,
        @Json(name = "users") val users: Map<String, Int>?,
        @Json(name = "state_default") val stateDefault: Int?,
        // `Int` is the diff here (instead of `Any`)
        @Json(name = "notifications") val notifications: Map<String, Int>?
)

internal fun JsonDict.toSafePowerLevelsContentDict(): JsonDict {
    return toModel<PowerLevelsContent>()
            ?.let { content ->
                SerializablePowerLevelsContent(
                        ban = content.ban,
                        kick = content.kick,
                        invite = content.invite,
                        redact = content.redact,
                        eventsDefault = content.eventsDefault,
                        events = content.events,
                        usersDefault = content.usersDefault,
                        users = content.users,
                        stateDefault = content.stateDefault,
                        notifications = content.notifications?.mapValues { content.notificationLevel(it.key)  }
                )
            }
            ?.toContent()
            ?: emptyMap()
}
