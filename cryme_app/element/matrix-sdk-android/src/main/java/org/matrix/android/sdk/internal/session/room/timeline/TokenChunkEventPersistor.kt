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

package org.matrix.android.sdk.internal.session.room.timeline

import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.database.helper.addIfNecessary
import org.matrix.android.sdk.internal.database.helper.addStateEvent
import org.matrix.android.sdk.internal.database.helper.addTimelineEvent
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.query.copyToRealmOrIgnore
import org.matrix.android.sdk.internal.database.query.create
import org.matrix.android.sdk.internal.database.query.find
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.util.awaitTransaction
import timber.log.Timber
import javax.inject.Inject

/**
 * Insert Chunk in DB, and eventually link next and previous chunk in db.
 */
internal class TokenChunkEventPersistor @Inject constructor(@SessionDatabase private val monarchy: Monarchy) {

    enum class Result {
        SHOULD_FETCH_MORE,
        REACHED_END,
        SUCCESS
    }

    suspend fun insertInDb(receivedChunk: TokenChunkEvent,
                           roomId: String,
                           direction: PaginationDirection): Result {
        monarchy
                .awaitTransaction { realm ->
                    Timber.v("Start persisting ${receivedChunk.events.size} events in $roomId towards $direction")

                    val nextToken: String?
                    val prevToken: String?
                    if (direction == PaginationDirection.FORWARDS) {
                        nextToken = receivedChunk.end
                        prevToken = receivedChunk.start
                    } else {
                        nextToken = receivedChunk.start
                        prevToken = receivedChunk.end
                    }

                    val existingChunk = ChunkEntity.find(realm, roomId, prevToken = prevToken, nextToken = nextToken)
                    if (existingChunk != null) {
                        Timber.v("This chunk is already in the db, returns")
                        return@awaitTransaction
                    }
                    val prevChunk = ChunkEntity.find(realm, roomId, nextToken = prevToken)
                    val nextChunk = ChunkEntity.find(realm, roomId, prevToken = nextToken)
                    val currentChunk = ChunkEntity.create(realm, prevToken = prevToken, nextToken = nextToken).apply {
                        this.nextChunk = nextChunk
                        this.prevChunk = prevChunk
                    }
                    nextChunk?.prevChunk = currentChunk
                    prevChunk?.nextChunk = currentChunk
                    if (receivedChunk.events.isEmpty() && !receivedChunk.hasMore()) {
                        handleReachEnd(roomId, direction, currentChunk)
                    } else {
                        handlePagination(realm, roomId, direction, receivedChunk, currentChunk)
                    }
                }
        return if (receivedChunk.events.isEmpty()) {
            if (receivedChunk.hasMore()) {
                Result.SHOULD_FETCH_MORE
            } else {
                Result.REACHED_END
            }
        } else {
            Result.SUCCESS
        }
    }

    private fun handleReachEnd(roomId: String, direction: PaginationDirection, currentChunk: ChunkEntity) {
        Timber.v("Reach end of $roomId")
        if (direction == PaginationDirection.FORWARDS) {
            Timber.v("We should keep the lastForward chunk unique, the one from sync")
        } else {
            currentChunk.isLastBackward = true
        }
    }

    private fun handlePagination(
            realm: Realm,
            roomId: String,
            direction: PaginationDirection,
            receivedChunk: TokenChunkEvent,
            currentChunk: ChunkEntity
    ) {
        Timber.v("Add ${receivedChunk.events.size} events in chunk(${currentChunk.nextToken} | ${currentChunk.prevToken}")
        val roomMemberContentsByUser = HashMap<String, RoomMemberContent?>()
        val eventList = receivedChunk.events
        val stateEvents = receivedChunk.stateEvents

        val now = System.currentTimeMillis()

        stateEvents?.forEach { stateEvent ->
            val ageLocalTs = stateEvent.unsignedData?.age?.let { now - it }
            val stateEventEntity = stateEvent.toEntity(roomId, SendState.SYNCED, ageLocalTs).copyToRealmOrIgnore(realm, EventInsertType.PAGINATION)
            currentChunk.addStateEvent(roomId, stateEventEntity, direction)
            if (stateEvent.type == EventType.STATE_ROOM_MEMBER && stateEvent.stateKey != null) {
                roomMemberContentsByUser[stateEvent.stateKey] = stateEvent.content.toModel<RoomMemberContent>()
            }
        }
        run processTimelineEvents@{
            eventList.forEach { event ->
                if (event.eventId == null || event.senderId == null) {
                    return@forEach
                }
                // We check for the timeline event with this id
                val eventId = event.eventId
                val existingTimelineEvent = TimelineEventEntity.where(realm, roomId, eventId).findFirst()
                // If it exists, we want to stop here, just link the prevChunk
                val existingChunk = existingTimelineEvent?.chunk?.firstOrNull()
                if (existingChunk != null) {
                    when (direction) {
                        PaginationDirection.BACKWARDS -> {
                            if (currentChunk.nextChunk == existingChunk) {
                                Timber.w("Avoid double link, shouldn't happen in an ideal world")
                            } else {
                                currentChunk.prevChunk = existingChunk
                                existingChunk.nextChunk = currentChunk
                            }
                        }
                        PaginationDirection.FORWARDS  -> {
                            if (currentChunk.prevChunk == existingChunk) {
                                Timber.w("Avoid double link, shouldn't happen in an ideal world")
                            } else {
                                currentChunk.nextChunk = existingChunk
                                existingChunk.prevChunk = currentChunk
                            }
                        }
                    }
                    // Stop processing here
                    return@processTimelineEvents
                }
                val ageLocalTs = event.unsignedData?.age?.let { now - it }
                val eventEntity = event.toEntity(roomId, SendState.SYNCED, ageLocalTs).copyToRealmOrIgnore(realm, EventInsertType.PAGINATION)
                if (event.type == EventType.STATE_ROOM_MEMBER && event.stateKey != null) {
                    val contentToUse = if (direction == PaginationDirection.BACKWARDS) {
                        event.prevContent
                    } else {
                        event.content
                    }
                    roomMemberContentsByUser[event.stateKey] = contentToUse.toModel<RoomMemberContent>()
                }
                currentChunk.addTimelineEvent(roomId, eventEntity, direction, roomMemberContentsByUser)
            }
        }
        if (currentChunk.isValid) {
            RoomEntity.where(realm, roomId).findFirst()?.addIfNecessary(currentChunk)
        }
    }
}
