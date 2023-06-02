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
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.call.lookup

import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.accountdata.RoomAccountDataTypes
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams

class CallUserMapper(private val session: Session, private val protocolsChecker: CallProtocolsChecker) {

    fun nativeRoomForVirtualRoom(roomId: String): String? {
        if (!protocolsChecker.supportVirtualRooms) return null
        val virtualRoom = session.getRoom(roomId) ?: return null
        val virtualRoomEvent = virtualRoom.getAccountDataEvent(RoomAccountDataTypes.EVENT_TYPE_VIRTUAL_ROOM)
        return virtualRoomEvent?.content?.toModel<RoomVirtualContent>()?.nativeRoomId
    }

    fun virtualRoomForNativeRoom(roomId: String): String? {
        if (!protocolsChecker.supportVirtualRooms) return null
        val virtualRoomEvents = session.accountDataService().getRoomAccountDataEvents(setOf(RoomAccountDataTypes.EVENT_TYPE_VIRTUAL_ROOM))
        return virtualRoomEvents.firstOrNull {
            val virtualRoomContent = it.content.toModel<RoomVirtualContent>()
            virtualRoomContent?.nativeRoomId == roomId
        }?.roomId
    }

    suspend fun getOrCreateVirtualRoomForRoom(roomId: String, opponentUserId: String): String? {
        protocolsChecker.awaitCheckProtocols()
        if (!protocolsChecker.supportVirtualRooms) return null
        val virtualUser = userToVirtualUser(opponentUserId) ?: return null
        val virtualRoomId = tryOrNull {
            ensureVirtualRoomExists(virtualUser, roomId)
        } ?: return null
        session.getRoom(virtualRoomId)?.markVirtual(roomId)
        return virtualRoomId
    }

    suspend fun onNewInvitedRoom(invitedRoomId: String) {
        protocolsChecker.awaitCheckProtocols()
        if (!protocolsChecker.supportVirtualRooms) return
        val invitedRoom = session.getRoom(invitedRoomId) ?: return
        val inviterId = invitedRoom.roomSummary()?.inviterId ?: return
        val nativeLookup = session.sipNativeLookup(inviterId).firstOrNull() ?: return
        if (nativeLookup.fields.containsKey("is_virtual")) {
            val nativeUser = nativeLookup.userId
            val nativeRoomId = session.getExistingDirectRoomWithUser(nativeUser)
            if (nativeRoomId != null) {
                // It's a virtual room with a matching native room, so set the room account data. This
                // will make sure we know where how to map calls and also allow us know not to display
                // it in the future.
                invitedRoom.markVirtual(nativeRoomId)
            }
        }
    }

    private suspend fun userToVirtualUser(userId: String): String? {
        val results = session.sipVirtualLookup(userId)
        return results.firstOrNull()?.userId
    }

    private suspend fun Room.markVirtual(nativeRoomId: String) {
        val virtualRoomContent = RoomVirtualContent(nativeRoomId = nativeRoomId)
        updateAccountData(RoomAccountDataTypes.EVENT_TYPE_VIRTUAL_ROOM, virtualRoomContent.toContent())
    }

    private suspend fun ensureVirtualRoomExists(userId: String, nativeRoomId: String): String {
        val existingDMRoom = tryOrNull { session.getExistingDirectRoomWithUser(userId) }
        val roomId: String
        if (existingDMRoom != null) {
            roomId = existingDMRoom
        } else {
            val roomParams = CreateRoomParams().apply {
                invitedUserIds.add(userId)
                setDirectMessage()
                creationContent[RoomAccountDataTypes.EVENT_TYPE_VIRTUAL_ROOM] = nativeRoomId
            }
            roomId = session.createRoom(roomParams)
        }
        return roomId
    }
}
