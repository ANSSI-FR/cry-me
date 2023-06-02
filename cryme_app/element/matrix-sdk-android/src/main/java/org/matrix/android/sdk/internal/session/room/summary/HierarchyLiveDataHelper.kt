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
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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
package org.matrix.android.sdk.internal.session.room.summary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.Optional

internal class HierarchyLiveDataHelper(
        val spaceId: String,
        val memberships: List<Membership>,
        val roomSummaryDataSource: RoomSummaryDataSource) {

    private val sources = HashMap<String, LiveData<Optional<RoomSummary>>>()
    private val mediatorLiveData = MediatorLiveData<List<String>>()

    fun liveData(): LiveData<List<String>> = mediatorLiveData

    init {
        onChange()
    }

    private fun parentsToCheck(): List<RoomSummary> {
        val spaces = ArrayList<RoomSummary>()
        roomSummaryDataSource.getSpaceSummary(spaceId)?.let {
            roomSummaryDataSource.flattenSubSpace(it, emptyList(), spaces, memberships)
        }
        return spaces
    }

    private fun onChange() {
        val existingSources = sources.keys.toList()
        val newSources = parentsToCheck().map { it.roomId }
        val addedSources = newSources.filter { !existingSources.contains(it) }
        val removedSource = existingSources.filter { !newSources.contains(it) }
        addedSources.forEach {
            val liveData = roomSummaryDataSource.getSpaceSummaryLive(it)
            mediatorLiveData.addSource(liveData) { onChange() }
            sources[it] = liveData
        }

        removedSource.forEach {
            sources[it]?.let { mediatorLiveData.removeSource(it) }
        }

        sources[spaceId]?.value?.getOrNull()?.let { spaceSummary ->
            val results = ArrayList<RoomSummary>()
            roomSummaryDataSource.flattenChild(spaceSummary, emptyList(), results, memberships)
            mediatorLiveData.postValue(results.map { it.roomId })
        }
    }
}
