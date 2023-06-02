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

package org.matrix.android.sdk.internal.session.sync.parsing

import com.squareup.moshi.Moshi
import okio.buffer
import okio.source
import org.matrix.android.sdk.api.session.sync.model.SyncResponse
import org.matrix.android.sdk.internal.session.sync.InitialSyncStrategy
import org.matrix.android.sdk.internal.session.sync.RoomSyncEphemeralTemporaryStore
import timber.log.Timber
import java.io.File
import javax.inject.Inject

internal class InitialSyncResponseParser @Inject constructor(
        private val moshi: Moshi,
        private val roomSyncEphemeralTemporaryStore: RoomSyncEphemeralTemporaryStore
) {

    fun parse(syncStrategy: InitialSyncStrategy.Optimized, workingFile: File): SyncResponse {
        val syncResponseLength = workingFile.length().toInt()
        Timber.d("INIT_SYNC Sync file size is $syncResponseLength bytes")
        val shouldSplit = syncResponseLength >= syncStrategy.minSizeToSplit
        Timber.d("INIT_SYNC should split in several files: $shouldSplit")
        return getMoshi(syncStrategy, shouldSplit)
                .adapter(SyncResponse::class.java)
                .fromJson(workingFile.source().buffer())!!
    }

    private fun getMoshi(syncStrategy: InitialSyncStrategy.Optimized, shouldSplit: Boolean): Moshi {
        // If we don't have to split we'll rely on the already default moshi
        if (!shouldSplit) return moshi
        // Otherwise, we create a new adapter for handling Map of Lazy sync
        return moshi.newBuilder()
                .add(SplitLazyRoomSyncEphemeralJsonAdapter(roomSyncEphemeralTemporaryStore, syncStrategy))
                .build()
    }
}
