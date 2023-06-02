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

package org.matrix.android.sdk.internal.session.sync.parsing

import io.realm.Realm
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.accountdata.RoomAccountDataTypes
import org.matrix.android.sdk.api.session.room.model.tag.RoomTagContent
import org.matrix.android.sdk.api.session.sync.model.RoomSyncAccountData
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.database.mapper.ContentMapper
import org.matrix.android.sdk.internal.database.model.RoomAccountDataEntity
import org.matrix.android.sdk.internal.database.model.RoomAccountDataEntityFields
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.session.room.read.FullyReadContent
import org.matrix.android.sdk.internal.session.sync.handler.room.RoomFullyReadHandler
import org.matrix.android.sdk.internal.session.sync.handler.room.RoomTagHandler
import javax.inject.Inject

internal class RoomSyncAccountDataHandler @Inject constructor(private val roomTagHandler: RoomTagHandler,
                                                              private val roomFullyReadHandler: RoomFullyReadHandler) {

    fun handle(realm: Realm, roomId: String, accountData: RoomSyncAccountData) {
        if (accountData.events.isNullOrEmpty()) {
            return
        }
        val roomEntity = RoomEntity.getOrCreate(realm, roomId)
        for (event in accountData.events) {
            val eventType = event.getClearType()
            handleGeneric(roomEntity, event.getClearContent(), eventType)
            if (eventType == RoomAccountDataTypes.EVENT_TYPE_TAG) {
                val content = event.getClearContent().toModel<RoomTagContent>()
                roomTagHandler.handle(realm, roomId, content)
            } else if (eventType == RoomAccountDataTypes.EVENT_TYPE_FULLY_READ) {
                val content = event.getClearContent().toModel<FullyReadContent>()
                roomFullyReadHandler.handle(realm, roomId, content)
            }
        }
    }

    private fun handleGeneric(roomEntity: RoomEntity, content: JsonDict?, eventType: String) {
        val existing = roomEntity.accountData.where().equalTo(RoomAccountDataEntityFields.TYPE, eventType).findFirst()
        if (existing != null) {
            existing.contentStr = ContentMapper.map(content)
        } else {
            val roomAccountData = RoomAccountDataEntity(
                    type = eventType,
                    contentStr = ContentMapper.map(content)
            )
            roomEntity.accountData.add(roomAccountData)
        }
    }
}
