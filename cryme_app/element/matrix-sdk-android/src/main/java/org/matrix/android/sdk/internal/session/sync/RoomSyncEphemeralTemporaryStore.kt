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

package org.matrix.android.sdk.internal.session.sync

import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import okio.buffer
import okio.source
import org.matrix.android.sdk.api.session.sync.model.RoomSyncEphemeral
import org.matrix.android.sdk.internal.di.SessionFilesDirectory
import org.matrix.android.sdk.internal.util.md5
import timber.log.Timber
import java.io.File
import javax.inject.Inject

internal interface RoomSyncEphemeralTemporaryStore {
    fun write(roomId: String, roomSyncEphemeralJson: String)
    fun read(roomId: String): RoomSyncEphemeral?
    fun reset()
    fun delete(roomId: String)
}

internal class RoomSyncEphemeralTemporaryStoreFile @Inject constructor(
        @SessionFilesDirectory fileDirectory: File,
        moshi: Moshi
) : RoomSyncEphemeralTemporaryStore {

    private val workingDir: File by lazy {
        File(fileDirectory, "rr").also {
            it.mkdirs()
        }
    }

    private val roomSyncEphemeralAdapter = moshi.adapter(RoomSyncEphemeral::class.java)

    /**
     * Write RoomSyncEphemeral to a file
     */
    override fun write(roomId: String, roomSyncEphemeralJson: String) {
        Timber.w("INIT_SYNC Store ephemeral events for room $roomId")
        getFile(roomId).writeText(roomSyncEphemeralJson)
    }

    /**
     * Read RoomSyncEphemeral from a file, or null if there is no file to read
     */
    override fun read(roomId: String): RoomSyncEphemeral? {
        return getFile(roomId)
                .takeIf { it.exists() }
                ?.inputStream()
                ?.use { pos ->
                    roomSyncEphemeralAdapter.fromJson(JsonReader.of(pos.source().buffer()))
                }
    }

    override fun delete(roomId: String) {
        getFile(roomId).delete()
    }

    override fun reset() {
        workingDir.deleteRecursively()
        workingDir.mkdirs()
    }

    private fun getFile(roomId: String): File {
        return File(workingDir, "${roomId.md5()}.json")
    }
}
