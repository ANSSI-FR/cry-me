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

import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.NoOpMatrixCallback
import org.matrix.android.sdk.api.util.Cancelable
import java.util.UUID

internal fun <PARAMS, RESULT> Task<PARAMS, RESULT>.configureWith(params: PARAMS,
                                                                 init: (ConfigurableTask.Builder<PARAMS, RESULT>.() -> Unit) = {}
): ConfigurableTask<PARAMS, RESULT> {
    return ConfigurableTask.Builder(this, params).apply(init).build()
}

internal fun <RESULT> Task<Unit, RESULT>.configureWith(init: (ConfigurableTask.Builder<Unit, RESULT>.() -> Unit) = {}): ConfigurableTask<Unit, RESULT> {
    return configureWith(Unit, init)
}

internal data class ConfigurableTask<PARAMS, RESULT>(
        val task: Task<PARAMS, RESULT>,
        val params: PARAMS,
        val id: UUID,
        val callbackThread: TaskThread,
        val executionThread: TaskThread,
        val callback: MatrixCallback<RESULT>,
        val maxRetryCount: Int = 0

) : Task<PARAMS, RESULT> by task {

    class Builder<PARAMS, RESULT>(
            private val task: Task<PARAMS, RESULT>,
            private val params: PARAMS,
            var id: UUID = UUID.randomUUID(),
            var callbackThread: TaskThread = TaskThread.MAIN,
            var executionThread: TaskThread = TaskThread.IO,
            var retryCount: Int = 0,
            var callback: MatrixCallback<RESULT> = NoOpMatrixCallback()
    ) {

        fun build() = ConfigurableTask(
                task = task,
                params = params,
                id = id,
                callbackThread = callbackThread,
                executionThread = executionThread,
                callback = callback,
                maxRetryCount = retryCount
        )
    }

    fun executeBy(taskExecutor: TaskExecutor): Cancelable {
        return taskExecutor.execute(this)
    }

    override fun toString(): String {
        return "${task.javaClass.name} with ID: $id"
    }
}
