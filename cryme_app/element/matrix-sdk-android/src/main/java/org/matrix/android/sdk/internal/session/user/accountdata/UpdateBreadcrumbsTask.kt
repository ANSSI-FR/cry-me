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

package org.matrix.android.sdk.internal.session.user.accountdata

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.internal.database.model.BreadcrumbsEntity
import org.matrix.android.sdk.internal.database.query.get
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.sync.model.accountdata.BreadcrumbsContent
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.fetchCopied
import javax.inject.Inject

// Use the same arbitrary value than Riot-Web
private const val MAX_BREADCRUMBS_ROOMS_NUMBER = 20

internal interface UpdateBreadcrumbsTask : Task<UpdateBreadcrumbsTask.Params, Unit> {
    data class Params(
            val newTopRoomId: String
    )
}

internal class DefaultUpdateBreadcrumbsTask @Inject constructor(
        private val saveBreadcrumbsTask: SaveBreadcrumbsTask,
        private val updateUserAccountDataTask: UpdateUserAccountDataTask,
        @SessionDatabase private val monarchy: Monarchy
) : UpdateBreadcrumbsTask {

    override suspend fun execute(params: UpdateBreadcrumbsTask.Params) {
        val newBreadcrumbs =
                // Get the breadcrumbs entity, if any
                monarchy.fetchCopied { BreadcrumbsEntity.get(it) }
                        ?.recentRoomIds
                        ?.apply {
                            // Modify the list to add the newTopRoomId first
                            // Ensure the newTopRoomId is not already in the list
                            remove(params.newTopRoomId)
                            // Add the newTopRoomId at first position
                            add(0, params.newTopRoomId)
                        }
                        ?.take(MAX_BREADCRUMBS_ROOMS_NUMBER)
                        ?: listOf(params.newTopRoomId)

        // Update the DB locally, do not wait for the sync
        saveBreadcrumbsTask.execute(SaveBreadcrumbsTask.Params(newBreadcrumbs))

        // FIXME It can remove the previous breadcrumbs, if not synced yet
        // And update account data
        updateUserAccountDataTask.execute(UpdateUserAccountDataTask.BreadcrumbsParams(
                breadcrumbsContent = BreadcrumbsContent(newBreadcrumbs)
        ))
    }
}
