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

package org.matrix.android.sdk.internal.session.user.accountdata

import androidx.lifecycle.LiveData
import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.session.accountdata.SessionAccountDataService
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataEvent
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.room.accountdata.RoomAccountDataEvent
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.room.accountdata.RoomAccountDataDataSource
import org.matrix.android.sdk.internal.session.sync.handler.UserAccountDataSyncHandler
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
import org.matrix.android.sdk.internal.util.awaitCallback
import javax.inject.Inject

internal class DefaultSessionAccountDataService @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
        private val updateUserAccountDataTask: UpdateUserAccountDataTask,
        private val userAccountDataSyncHandler: UserAccountDataSyncHandler,
        private val userAccountDataDataSource: UserAccountDataDataSource,
        private val roomAccountDataDataSource: RoomAccountDataDataSource,
        private val taskExecutor: TaskExecutor
) : SessionAccountDataService {

    override fun getUserAccountDataEvent(type: String): UserAccountDataEvent? {
        return userAccountDataDataSource.getAccountDataEvent(type)
    }

    override fun getLiveUserAccountDataEvent(type: String): LiveData<Optional<UserAccountDataEvent>> {
        return userAccountDataDataSource.getLiveAccountDataEvent(type)
    }

    override fun getUserAccountDataEvents(types: Set<String>): List<UserAccountDataEvent> {
        return userAccountDataDataSource.getAccountDataEvents(types)
    }

    override fun getLiveUserAccountDataEvents(types: Set<String>): LiveData<List<UserAccountDataEvent>> {
        return userAccountDataDataSource.getLiveAccountDataEvents(types)
    }

    override fun getRoomAccountDataEvents(types: Set<String>): List<RoomAccountDataEvent> {
        return roomAccountDataDataSource.getAccountDataEvents(null, types)
    }

    override fun getLiveRoomAccountDataEvents(types: Set<String>): LiveData<List<RoomAccountDataEvent>> {
        return roomAccountDataDataSource.getLiveAccountDataEvents(null, types)
    }

    override suspend fun updateUserAccountData(type: String, content: Content) {
        val params = UpdateUserAccountDataTask.AnyParams(type = type, any = content)
        awaitCallback<Unit> { callback ->
            updateUserAccountDataTask.configureWith(params) {
                this.retryCount = 5 // TODO: Need to refactor retrying out into a helper method.
                this.callback = callback
            }
                    .executeBy(taskExecutor)
        }
        // TODO Move that to the task (but it created a circular dependencies...)
        monarchy.runTransactionSync { realm ->
            userAccountDataSyncHandler.handleGenericAccountData(realm, type, content)
        }
    }
}
