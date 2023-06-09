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
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.taggedevents

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Keys are event IDs, values are event information.
 */
typealias TaggedEvent = Map<String, TaggedEventInfo>

/**
 * Keys are tagged event names (eg. m.favourite), values are the related events.
 */
typealias TaggedEvents = Map<String, TaggedEvent>

/**
 * Class used to parse the content of a m.tagged_events type event.
 * This kind of event defines the tagged events in a room.
 *
 * The content of this event is a tags key whose value is an object mapping the name of each tag
 * to another object. The JSON object associated with each tag is an object where the keys are the
 * event IDs and values give information about the events.
 *
 * Ref: https://github.com/matrix-org/matrix-doc/pull/2437
 */
@JsonClass(generateAdapter = true)
data class TaggedEventsContent(
        @Json(name = "tags")
        var tags: TaggedEvents = emptyMap()
) {
    val favouriteEvents
        get() = tags[TAG_FAVOURITE].orEmpty()

    val hiddenEvents
        get() = tags[TAG_HIDDEN].orEmpty()

    fun tagEvent(eventId: String, info: TaggedEventInfo, tag: String) {
        val taggedEvents = tags[tag].orEmpty().plus(eventId to info)
        tags = tags.plus(tag to taggedEvents)
    }

    fun untagEvent(eventId: String, tag: String) {
        val taggedEvents = tags[tag]?.minus(eventId).orEmpty()
        tags = tags.plus(tag to taggedEvents)
    }

    companion object {
        const val TAG_FAVOURITE = "m.favourite"
        const val TAG_HIDDEN = "m.hidden"
    }
}

@JsonClass(generateAdapter = true)
data class TaggedEventInfo(
        @Json(name = "keywords")
        val keywords: List<String>? = null,

        @Json(name = "origin_server_ts")
        val originServerTs: Long? = null,

        @Json(name = "tagged_at")
        val taggedAt: Long? = null
)
