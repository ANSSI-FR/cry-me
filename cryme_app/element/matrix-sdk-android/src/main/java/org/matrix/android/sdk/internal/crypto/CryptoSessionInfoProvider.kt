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

package org.matrix.android.sdk.internal.crypto

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.query.whereType
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.room.membership.RoomMemberHelper
import org.matrix.android.sdk.internal.util.fetchCopied
import javax.inject.Inject

/**
 * The crypto module needs some information regarding rooms that are stored
 * in the session DB, this class encapsulate this functionality
 */
internal class CryptoSessionInfoProvider @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy
) {

    fun isRoomEncrypted(roomId: String): Boolean {
        val encryptionEvent = monarchy.fetchCopied { realm ->
            EventEntity.whereType(realm, roomId = roomId, type = EventType.STATE_ROOM_ENCRYPTION)
                    .contains(EventEntityFields.CONTENT, "\"algorithm\":\"$MXCRYPTO_ALGORITHM_MEGOLM\"")
                    .isEmpty(EventEntityFields.STATE_KEY)
                    .findFirst()
        }
        return encryptionEvent != null
    }

    /**
     * @param allActive if true return joined as well as invited, if false, only joined
     */
     fun getRoomUserIds(roomId: String, allActive: Boolean): List<String> {
        var userIds: List<String> = emptyList()
        monarchy.doWithRealm { realm ->
            userIds = if (allActive) {
                RoomMemberHelper(realm, roomId).getActiveRoomMemberIds()
            } else {
                RoomMemberHelper(realm, roomId).getJoinedRoomMemberIds()
            }
        }
        return userIds
    }
}
