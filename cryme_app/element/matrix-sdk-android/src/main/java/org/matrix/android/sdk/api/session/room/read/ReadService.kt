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

package org.matrix.android.sdk.api.session.room.read

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.session.room.model.ReadReceipt
import org.matrix.android.sdk.api.util.Optional

/**
 * This interface defines methods to handle read receipts and read marker in a room. It's implemented at the room level.
 */
interface ReadService {

    enum class MarkAsReadParams {
        READ_RECEIPT,
        READ_MARKER,
        BOTH
    }

    /**
     * Force the read marker to be set on the latest event.
     */
    suspend fun markAsRead(params: MarkAsReadParams = MarkAsReadParams.BOTH)

    /**
     * Set the read receipt on the event with provided eventId.
     */
    suspend fun setReadReceipt(eventId: String)

    /**
     * Set the read marker on the event with provided eventId.
     */
    suspend fun setReadMarker(fullyReadEventId: String)

    /**
     * Check if an event is already read, ie. your read receipt is set on a more recent event.
     */
    fun isEventRead(eventId: String): Boolean

    /**
     * Returns a live read marker id for the room.
     */
    fun getReadMarkerLive(): LiveData<Optional<String>>

    /**
     * Returns a live read receipt id for the room.
     */
    fun getMyReadReceiptLive(): LiveData<Optional<String>>

    /**
     * Get the eventId where the read receipt for the provided user is
     * @param userId the id of the user to look for
     *
     * @return the eventId where the read receipt for the provided user is attached, or null if not found
     */
    fun getUserReadReceipt(userId: String): String?

    /**
     * Returns a live list of read receipts for a given event
     * @param eventId: the event
     */
    fun getEventReadReceiptsLive(eventId: String): LiveData<List<ReadReceipt>>
}
