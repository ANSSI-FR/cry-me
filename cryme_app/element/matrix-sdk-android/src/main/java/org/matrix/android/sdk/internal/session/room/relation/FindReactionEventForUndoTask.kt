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
package org.matrix.android.sdk.internal.session.room.relation

import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.ReactionAggregatedSummaryEntityFields
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface FindReactionEventForUndoTask : Task<FindReactionEventForUndoTask.Params, FindReactionEventForUndoTask.Result> {

    data class Params(
            val roomId: String,
            val eventId: String,
            val reaction: String
    )

    data class Result(
            val redactEventId: String?
    )
}

internal class DefaultFindReactionEventForUndoTask @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
        @UserId private val userId: String) : FindReactionEventForUndoTask {

    override suspend fun execute(params: FindReactionEventForUndoTask.Params): FindReactionEventForUndoTask.Result {
        val eventId = Realm.getInstance(monarchy.realmConfiguration).use { realm ->
            getReactionToRedact(realm, params)?.eventId
        }
        return FindReactionEventForUndoTask.Result(eventId)
    }

    private fun getReactionToRedact(realm: Realm, params: FindReactionEventForUndoTask.Params): EventEntity? {
        val summary = EventAnnotationsSummaryEntity.where(realm, params.roomId, params.eventId).findFirst() ?: return null

        val rase = summary.reactionsSummary.where()
                .equalTo(ReactionAggregatedSummaryEntityFields.KEY, params.reaction)
                .findFirst() ?: return null

        // want to find the event originated by me!
        return rase.sourceEvents
                .asSequence()
                .mapNotNull {
                    // find source event
                    EventEntity.where(realm, it).findFirst()
                }
                .firstOrNull { eventEntity ->
                    // is it mine?
                    eventEntity.sender == userId
                }
    }
}
