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

package org.matrix.android.sdk.internal.session.room.send.queue

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.internal.di.SerializeNulls
import org.matrix.android.sdk.internal.network.parsing.RuntimeJsonAdapterFactory

/**
 * Info that need to be persisted by the sender thread
 * With polymorphic moshi parsing
 */
internal interface TaskInfo {
    val type: String
    val order: Int

    companion object {
        const val TYPE_UNKNOWN = "TYPE_UNKNOWN"
        const val TYPE_SEND = "TYPE_SEND"
        const val TYPE_REDACT = "TYPE_REDACT"

        private val moshi = Moshi.Builder()
                .add(RuntimeJsonAdapterFactory.of(TaskInfo::class.java, "type", FallbackTaskInfo::class.java)
                        .registerSubtype(SendEventTaskInfo::class.java, TYPE_SEND)
                        .registerSubtype(RedactEventTaskInfo::class.java, TYPE_REDACT)
                )
                .add(SerializeNulls.JSON_ADAPTER_FACTORY)
                .build()

        fun map(info: TaskInfo): String {
            return moshi.adapter(TaskInfo::class.java).toJson(info)
        }

        fun map(string: String): TaskInfo? {
            return tryOrNull { moshi.adapter(TaskInfo::class.java).fromJson(string) }
        }
    }
}

@JsonClass(generateAdapter = true)
internal data class SendEventTaskInfo(
        @Json(name = "type") override val type: String = TaskInfo.TYPE_SEND,
        @Json(name = "localEchoId") val localEchoId: String,
        @Json(name = "encrypt") val encrypt: Boolean?,
        @Json(name = "order") override val order: Int
) : TaskInfo

@JsonClass(generateAdapter = true)
internal data class RedactEventTaskInfo(
        @Json(name = "type") override val type: String = TaskInfo.TYPE_REDACT,
        @Json(name = "redactionLocalEcho") val redactionLocalEcho: String?,
        @Json(name = "order") override val order: Int
) : TaskInfo

@JsonClass(generateAdapter = true)
internal data class FallbackTaskInfo(
        @Json(name = "type") override val type: String = TaskInfo.TYPE_UNKNOWN,
        @Json(name = "order") override val order: Int
) : TaskInfo
