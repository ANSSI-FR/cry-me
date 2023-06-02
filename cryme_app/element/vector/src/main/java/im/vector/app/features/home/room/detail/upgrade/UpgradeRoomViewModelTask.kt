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

package im.vector.app.features.home.room.detail.upgrade

import im.vector.app.core.platform.ViewModelTask
import im.vector.app.core.resources.StringProvider
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import timber.log.Timber
import javax.inject.Inject

class UpgradeRoomViewModelTask @Inject constructor(
        val session: Session,
        val stringProvider: StringProvider
) : ViewModelTask<UpgradeRoomViewModelTask.Params, UpgradeRoomViewModelTask.Result> {

    sealed class Result {
        data class Success(val replacementRoomId: String) : Result()
        abstract class Failure(val throwable: Throwable?) : Result()
        object UnknownRoom : Failure(null)
        object NotAllowed : Failure(null)
        class ErrorFailure(throwable: Throwable) : Failure(throwable)
    }

    data class Params(
            val roomId: String,
            val newVersion: String,
            val userIdsToAutoInvite: List<String> = emptyList(),
            val parentSpaceToUpdate: List<String> = emptyList(),
            val progressReporter: ((indeterminate: Boolean, progress: Int, total: Int) -> Unit)? = null
    )

    override suspend fun execute(params: Params): Result {
        params.progressReporter?.invoke(true, 0, 0)

        val room = session.getRoom(params.roomId)
                ?: return Result.UnknownRoom
        if (!room.userMayUpgradeRoom(session.myUserId)) {
            return Result.NotAllowed
        }

        val updatedRoomId = try {
            room.upgradeToVersion(params.newVersion)
        } catch (failure: Throwable) {
            return Result.ErrorFailure(failure)
        }

        val totalStep = params.userIdsToAutoInvite.size + params.parentSpaceToUpdate.size
        var currentStep = 0
        params.userIdsToAutoInvite.forEach {
            params.progressReporter?.invoke(false, currentStep, totalStep)
            tryOrNull {
                session.getRoom(updatedRoomId)?.invite(it)
            }
            currentStep++
        }

        params.parentSpaceToUpdate.forEach { parentId ->
            params.progressReporter?.invoke(false, currentStep, totalStep)
            // we try and silently fail
            try {
                session.getRoom(parentId)?.asSpace()?.let { parentSpace ->
                    val currentInfo = parentSpace.getChildInfo(params.roomId)
                    if (currentInfo != null) {
                        parentSpace.addChildren(
                                roomId = updatedRoomId,
                                viaServers = currentInfo.via,
                                order = currentInfo.order,
//                                autoJoin = currentInfo.autoJoin ?: false,
                                suggested = currentInfo.suggested
                        )

                        parentSpace.removeChildren(params.roomId)
                    }
                }
            } catch (failure: Throwable) {
                Timber.d("## Migrate: Failed to update space parent. cause: ${failure.localizedMessage}")
            } finally {
                currentStep++
            }
        }

        return Result.Success(updatedRoomId)
    }
}
