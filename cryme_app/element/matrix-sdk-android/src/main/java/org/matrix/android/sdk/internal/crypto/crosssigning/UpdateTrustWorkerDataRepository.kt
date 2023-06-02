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
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.crosssigning

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.di.SessionFilesDirectory
import java.io.File
import java.util.UUID
import javax.inject.Inject

@JsonClass(generateAdapter = true)
internal data class UpdateTrustWorkerData(
        @Json(name = "userIds")
        val userIds: List<String>
)

internal class UpdateTrustWorkerDataRepository @Inject constructor(
        @SessionFilesDirectory parentDir: File
) {
    private val workingDirectory = File(parentDir, "tw")
    private val jsonAdapter = MoshiProvider.providesMoshi().adapter(UpdateTrustWorkerData::class.java)

    // Return the path of the created file
    fun createParam(userIds: List<String>): String {
        val filename = "${UUID.randomUUID()}.json"
        workingDirectory.mkdirs()
        val file = File(workingDirectory, filename)

        UpdateTrustWorkerData(userIds = userIds)
                .let { jsonAdapter.toJson(it) }
                .let { file.writeText(it) }

        return filename
    }

    fun getParam(filename: String): UpdateTrustWorkerData? {
        return File(workingDirectory, filename)
                .takeIf { it.exists() }
                ?.readText()
                ?.let { jsonAdapter.fromJson(it) }
    }

    fun delete(filename: String) {
        tryOrNull("Unable to delete $filename") {
            File(workingDirectory, filename).delete()
        }
    }
}
