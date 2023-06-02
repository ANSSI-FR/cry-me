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
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.spaces.manage

import androidx.recyclerview.widget.DiffUtil
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.paging.PagedListEpoxyController
import im.vector.app.R
import im.vector.app.core.epoxy.charsequence.toEpoxyCharSequence
import im.vector.app.core.ui.list.GenericPillItem_
import im.vector.app.core.utils.createUIHandler
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.list.RoomCategoryItem_
import org.matrix.android.sdk.api.session.room.ResultBoundaries
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class AddRoomListController @Inject constructor(
        private val avatarRenderer: AvatarRenderer
) : PagedListEpoxyController<RoomSummary>(
        // Important it must match the PageList builder notify Looper
        modelBuildingHandler = createUIHandler(),

        itemDiffCallback = object : DiffUtil.ItemCallback<RoomSummary>() {

            override fun areItemsTheSame(oldItem: RoomSummary, newItem: RoomSummary): Boolean {
                return oldItem.roomId == newItem.roomId
            }

            override fun areContentsTheSame(oldItem: RoomSummary, newItem: RoomSummary): Boolean {
                // for this use case we can test less things
                return oldItem.displayName == newItem.displayName &&
                        oldItem.avatarUrl == newItem.avatarUrl
            }
        }
) {

    interface Listener {
        fun onItemSelected(roomSummary: RoomSummary)
    }

    var listener: Listener? = null
    var ignoreRooms: List<String>? = null

    var subHeaderText: String? = null

    var initialLoadOccurred = false

    var expanded: Boolean = true
        set(value) {
            if (value != field) {
                field = value
                requestForcedModelBuild()
            }
        }

    var disabled: Boolean = false
        set(value) {
            if (value != field) {
                field = value
                requestForcedModelBuild()
            }
        }

    fun boundaryChange(boundary: ResultBoundaries) {
        val boundaryHasLoadedSomething = boundary.frontLoaded || boundary.zeroItemLoaded
        if (initialLoadOccurred != boundaryHasLoadedSomething) {
            initialLoadOccurred = boundaryHasLoadedSomething
            requestForcedModelBuild()
        }
    }

    var sectionName: String? = null
        set(value) {
            if (value != field) {
                field = value
                requestForcedModelBuild()
            }
        }

    var totalSize: Int = 0

    var selectedItems: Map<String, Boolean> = emptyMap()
        set(value) {
            field = value
            // mmm annoying but can't just force for a given model
            requestForcedModelBuild()
        }

    override fun addModels(models: List<EpoxyModel<*>>) {
        if (disabled) {
            super.addModels(emptyList())
            return
        }
        val host = this
        val filteredModel = if (ignoreRooms == null) {
            models
        } else {
            models.filter {
                it !is RoomSelectionItem || !ignoreRooms!!.contains(it.matrixItem.id)
            }
        }
        val somethingToShow = filteredModel.isNotEmpty() || !initialLoadOccurred
        if (somethingToShow || filteredModel.isNotEmpty()) {
            add(
                    RoomCategoryItem_().apply {
                        id("header")
                        title(host.sectionName ?: "")
                        expanded(host.expanded)
                        listener {
                            host.expanded = !host.expanded
                        }
                    }
            )
            if (expanded && subHeaderText != null) {
                add(
                        GenericPillItem_().apply {
                            id("sub_header")
                            text(host.subHeaderText?.toEpoxyCharSequence())
                            imageRes(R.drawable.ic_info)
                        }
                )
            }
        }
        if (expanded) {
            super.addModels(filteredModel)
            if (!initialLoadOccurred) {
                add(
                        RoomSelectionPlaceHolderItem_().apply { id("loading") }
                )
            }
        }
    }

    override fun buildItemModel(currentPosition: Int, item: RoomSummary?): EpoxyModel<*> {
        val host = this
        if (item == null) return RoomSelectionPlaceHolderItem_().apply { id(currentPosition) }
        return RoomSelectionItem_().apply {
            id(item.roomId)
            matrixItem(item.toMatrixItem())
            avatarRenderer(host.avatarRenderer)
            selected(host.selectedItems[item.roomId] ?: false)
            itemClickListener {
                host.listener?.onItemSelected(item)
            }
        }
    }
}
