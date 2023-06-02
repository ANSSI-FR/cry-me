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

package org.matrix.android.sdk.api.session.room.timeline

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.util.Optional

/**
 * This interface defines methods to interact with the timeline. It's implemented at the room level.
 */
interface TimelineService {

    /**
     * Instantiate a [Timeline] with an optional initial eventId, to be used with permalink.
     * You can also configure some settings with the [settings] param.
     *
     * Important: the returned Timeline has to be started
     *
     * @param eventId the optional initial eventId.
     * @param settings settings to configure the timeline.
     * @return the instantiated timeline
     */
    fun createTimeline(eventId: String?, settings: TimelineSettings): Timeline

    /**
     * Returns a snapshot of TimelineEvent event with eventId.
     * At the opposite of getTimeLineEventLive which will be updated when local echo event is synced, it will return null in this case.
     * @param eventId the eventId to get the TimelineEvent
     */
    fun getTimeLineEvent(eventId: String): TimelineEvent?

    /**
     * Creates a LiveData of Optional TimelineEvent event with eventId.
     * If the eventId is a local echo eventId, it will make the LiveData be updated with the synced TimelineEvent when coming through the sync.
     * In this case, makes sure to use the new synced eventId from the TimelineEvent class if you want to interact, as the local echo is removed from the SDK.
     * @param eventId the eventId to listen for TimelineEvent
     */
    fun getTimeLineEventLive(eventId: String): LiveData<Optional<TimelineEvent>>

    /**
     * Returns a snapshot list of TimelineEvent with EventType.MESSAGE and MessageType.MSGTYPE_IMAGE or MessageType.MSGTYPE_VIDEO.
     */
    fun getAttachmentMessages(): List<TimelineEvent>
}
