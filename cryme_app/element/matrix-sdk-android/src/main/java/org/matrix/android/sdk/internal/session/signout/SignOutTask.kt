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

package org.matrix.android.sdk.internal.session.signout

import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.cleanup.CleanupSession
import org.matrix.android.sdk.internal.session.identity.IdentityDisconnectTask
import org.matrix.android.sdk.internal.task.Task
import timber.log.Timber
import java.net.HttpURLConnection
import javax.inject.Inject

internal interface SignOutTask : Task<SignOutTask.Params, Unit> {
    data class Params(
            val signOutFromHomeserver: Boolean
    )
}

internal class DefaultSignOutTask @Inject constructor(
        private val signOutAPI: SignOutAPI,
        private val globalErrorReceiver: GlobalErrorReceiver,
        private val identityDisconnectTask: IdentityDisconnectTask,
        private val cleanupSession: CleanupSession
) : SignOutTask {

    override suspend fun execute(params: SignOutTask.Params) {
        // It should be done even after a soft logout, to be sure the deviceId is deleted on the
        if (params.signOutFromHomeserver) {
            cleanupSession.stopActiveTasks()
            Timber.d("SignOut: send request...")
            try {
                executeRequest(globalErrorReceiver) {
                    signOutAPI.signOut()
                }
            } catch (throwable: Throwable) {
                // Maybe due to https://github.com/matrix-org/synapse/issues/5756
                if (throwable is Failure.ServerError &&
                        throwable.httpCode == HttpURLConnection.HTTP_UNAUTHORIZED && /* 401 */
                        throwable.error.code == MatrixError.M_UNKNOWN_TOKEN) {
                    // Also throwable.error.isSoftLogout should be true
                    // Ignore
                    Timber.w("Ignore error due to https://github.com/matrix-org/synapse/issues/5755")
                } else {
                    throw throwable
                }
            }
        }

        // Logout from identity server if any
        runCatching { identityDisconnectTask.execute(Unit) }
                .onFailure { Timber.w(it, "Unable to disconnect identity server") }

        Timber.d("SignOut: cleanup session...")
        cleanupSession.cleanup()
    }
}
