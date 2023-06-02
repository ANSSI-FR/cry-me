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

package org.matrix.android.sdk.internal.crypto

import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.internal.di.WorkManagerProvider
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.util.CancelableWork
import org.matrix.android.sdk.internal.worker.startChain
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@SessionScope
internal class GossipingWorkManager @Inject constructor(
        private val workManagerProvider: WorkManagerProvider
) {

    inline fun <reified W : ListenableWorker> createWork(data: Data, startChain: Boolean): OneTimeWorkRequest {
        return workManagerProvider.matrixOneTimeWorkRequestBuilder<W>()
                .setConstraints(WorkManagerProvider.workConstraints)
                .startChain(startChain)
                .setInputData(data)
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkManagerProvider.BACKOFF_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                .build()
    }

    // Prevent sending queue to stay broken after app restart
    // The unique queue id will stay the same as long as this object is instanciated
    val queueSuffixApp = System.currentTimeMillis()

    fun postWork(workRequest: OneTimeWorkRequest, policy: ExistingWorkPolicy = ExistingWorkPolicy.APPEND): Cancelable {
        workManagerProvider.workManager
                .beginUniqueWork(this::class.java.name + "_$queueSuffixApp", policy, workRequest)
                .enqueue()

        return CancelableWork(workManagerProvider.workManager, workRequest.id)
    }
}
