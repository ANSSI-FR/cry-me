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

package org.matrix.android.sdk.api.session.room.accountdata

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.util.Optional

/**
 * This service is attached to a single room.
 */
interface RoomAccountDataService {
    /**
     * Retrieve the account data with the provided type or null if not found
     */
    fun getAccountDataEvent(type: String): RoomAccountDataEvent?

    /**
     * Observe the account data with the provided type
     */
    fun getLiveAccountDataEvent(type: String): LiveData<Optional<RoomAccountDataEvent>>

    /**
     * Retrieve the account data with the provided types. The return list can have a different size that
     * the size of the types set, because some AccountData may not exist.
     * If an empty set is provided, all the AccountData are retrieved
     */
    fun getAccountDataEvents(types: Set<String>): List<RoomAccountDataEvent>

    /**
     * Observe the account data with the provided types. If an empty set is provided, all the AccountData are observed
     */
    fun getLiveAccountDataEvents(types: Set<String>): LiveData<List<RoomAccountDataEvent>>

    /**
     * Update the account data with the provided type and the provided account data content
     */
    suspend fun updateAccountData(type: String, content: Content)
}
