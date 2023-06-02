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

package org.matrix.android.sdk.internal.session.room.alias

import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.directory.DirectoryAPI
import org.matrix.android.sdk.internal.session.room.alias.RoomAliasAvailabilityChecker.Companion.toFullLocalAlias
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface AddRoomAliasTask : Task<AddRoomAliasTask.Params, Unit> {
    data class Params(
            val roomId: String,
            /**
             * the local part of the alias.
             * Ex: for the alias "#my_alias:example.org", the local part is "my_alias"
             */
            val aliasLocalPart: String
    )
}

internal class DefaultAddRoomAliasTask @Inject constructor(
        @UserId private val userId: String,
        private val directoryAPI: DirectoryAPI,
        private val aliasAvailabilityChecker: RoomAliasAvailabilityChecker,
        private val globalErrorReceiver: GlobalErrorReceiver
) : AddRoomAliasTask {

    override suspend fun execute(params: AddRoomAliasTask.Params) {
        aliasAvailabilityChecker.check(params.aliasLocalPart)

        executeRequest(globalErrorReceiver) {
            directoryAPI.addRoomAlias(
                    roomAlias = params.aliasLocalPart.toFullLocalAlias(userId),
                    body = AddRoomAliasBody(
                            roomId = params.roomId
                    )
            )
        }
    }
}
