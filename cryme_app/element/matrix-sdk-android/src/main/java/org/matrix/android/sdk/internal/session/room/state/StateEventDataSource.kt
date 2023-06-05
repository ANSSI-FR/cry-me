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

package org.matrix.android.sdk.internal.session.room.state

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.internal.database.RealmSessionProvider
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntityFields
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.query.QueryStringValueProcessor
import org.matrix.android.sdk.internal.query.process
import javax.inject.Inject

internal class StateEventDataSource @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
        private val realmSessionProvider: RealmSessionProvider,
        private val queryStringValueProcessor: QueryStringValueProcessor
) {

    fun getStateEvent(roomId: String, eventType: String, stateKey: QueryStringValue): Event? {
        return realmSessionProvider.withRealm { realm ->
            buildStateEventQuery(realm, roomId, setOf(eventType), stateKey).findFirst()?.root?.asDomain()
        }
    }

    fun getStateEventLive(roomId: String, eventType: String, stateKey: QueryStringValue): LiveData<Optional<Event>> {
        val liveData = monarchy.findAllMappedWithChanges(
                { realm -> buildStateEventQuery(realm, roomId, setOf(eventType), stateKey) },
                { it.root?.asDomain() }
        )
        return Transformations.map(liveData) { results ->
            results.firstOrNull().toOptional()
        }
    }

    fun getStateEvents(roomId: String, eventTypes: Set<String>, stateKey: QueryStringValue): List<Event> {
        return realmSessionProvider.withRealm { realm ->
            buildStateEventQuery(realm, roomId, eventTypes, stateKey)
                    .findAll()
                    .mapNotNull {
                        it.root?.asDomain()
                    }
        }
    }

    fun getStateEventsLive(roomId: String, eventTypes: Set<String>, stateKey: QueryStringValue): LiveData<List<Event>> {
        val liveData = monarchy.findAllMappedWithChanges(
                { realm -> buildStateEventQuery(realm, roomId, eventTypes, stateKey) },
                { it.root?.asDomain() }
        )
        return Transformations.map(liveData) { results ->
            results.filterNotNull()
        }
    }

    private fun buildStateEventQuery(realm: Realm,
                                     roomId: String,
                                     eventTypes: Set<String>,
                                     stateKey: QueryStringValue
    ): RealmQuery<CurrentStateEventEntity> {
        return with(queryStringValueProcessor) {
            realm.where<CurrentStateEventEntity>()
                    .equalTo(CurrentStateEventEntityFields.ROOM_ID, roomId)
                    .apply {
                        if (eventTypes.isNotEmpty()) {
                            `in`(CurrentStateEventEntityFields.TYPE, eventTypes.toTypedArray())
                        }
                    }
                    .process(CurrentStateEventEntityFields.STATE_KEY, stateKey)
        }
    }
}
