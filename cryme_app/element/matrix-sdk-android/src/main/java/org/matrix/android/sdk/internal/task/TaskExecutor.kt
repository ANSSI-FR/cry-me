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

package org.matrix.android.sdk.internal.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.internal.di.MatrixScope
import org.matrix.android.sdk.internal.extensions.foldToCallback
import org.matrix.android.sdk.internal.util.toCancelable
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.EmptyCoroutineContext

@MatrixScope
internal class TaskExecutor @Inject constructor(private val coroutineDispatchers: MatrixCoroutineDispatchers) {

    val executorScope = CoroutineScope(SupervisorJob())

    fun <PARAMS, RESULT> execute(task: ConfigurableTask<PARAMS, RESULT>): Cancelable {
        return executorScope
                .launch(task.callbackThread.toDispatcher()) {
                    val resultOrFailure = runCatching {
                        withContext(task.executionThread.toDispatcher()) {
                            Timber.v("## TASK: Enqueue task $task")
                            Timber.v("## TASK: Execute task $task on ${Thread.currentThread().name}")
                            task.executeRetry(task.params, task.maxRetryCount)
                        }
                    }
                    resultOrFailure
                            .onFailure {
                                Timber.e(it, "Task failed")
                            }
                            .foldToCallback(task.callback)
                }
                .toCancelable()
    }

    fun cancelAll() = executorScope.coroutineContext.cancelChildren()

    private fun TaskThread.toDispatcher() = when (this) {
        TaskThread.MAIN        -> coroutineDispatchers.main
        TaskThread.COMPUTATION -> coroutineDispatchers.computation
        TaskThread.IO          -> coroutineDispatchers.io
        TaskThread.CALLER      -> EmptyCoroutineContext
        TaskThread.CRYPTO      -> coroutineDispatchers.crypto
        TaskThread.DM_VERIF    -> coroutineDispatchers.dmVerif
    }
}
