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

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.matrix.android.sdk.api.session.sync.model.LazyRoomSyncEphemeral
import org.matrix.android.sdk.api.session.sync.model.RoomSyncEphemeral
import org.matrix.android.sdk.internal.session.sync.InitialSyncStrategy
import org.matrix.android.sdk.internal.session.sync.RoomSyncEphemeralTemporaryStore
import timber.log.Timber

internal class DefaultLazyRoomSyncEphemeralJsonAdapter {

    @FromJson
    fun fromJson(reader: JsonReader, delegate: JsonAdapter<RoomSyncEphemeral>): LazyRoomSyncEphemeral? {
        val roomSyncEphemeral = delegate.fromJson(reader) ?: return null
        return LazyRoomSyncEphemeral.Parsed(roomSyncEphemeral)
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: LazyRoomSyncEphemeral?) {
        // This Adapter is not supposed to serialize object
        Timber.v("To json $value with $writer")
        throw UnsupportedOperationException()
    }
}

internal class SplitLazyRoomSyncEphemeralJsonAdapter(
        private val roomSyncEphemeralTemporaryStore: RoomSyncEphemeralTemporaryStore,
        private val syncStrategy: InitialSyncStrategy.Optimized
) {
    @FromJson
    fun fromJson(reader: JsonReader, delegate: JsonAdapter<RoomSyncEphemeral>): LazyRoomSyncEphemeral? {
        val path = reader.path
        val roomId = path.substringAfter("\$.rooms.join.").substringBeforeLast(".ephemeral")

        val json = reader.nextSource().inputStream().bufferedReader().use {
            it.readText()
        }
        val limit = syncStrategy.minSizeToStoreInFile
        return if (json.length > limit) {
            Timber.d("INIT_SYNC $path content length: ${json.length} copy to a file")
            // Copy the source to a file
            roomSyncEphemeralTemporaryStore.write(roomId, json)
            LazyRoomSyncEphemeral.Stored
        } else {
            Timber.d("INIT_SYNC $path content length: ${json.length} parse it now")
            val roomSync = delegate.fromJson(json) ?: return null
            LazyRoomSyncEphemeral.Parsed(roomSync)
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: LazyRoomSyncEphemeral?) {
        // This Adapter is not supposed to serialize object
        Timber.v("To json $value with $writer")
        throw UnsupportedOperationException()
    }
}
