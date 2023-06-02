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

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.epoxy.VisibilityState
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Incomplete
import im.vector.app.R
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class SpaceManageRoomsController @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val errorFormatter: ErrorFormatter,
        private val stringProvider: StringProvider
) : TypedEpoxyController<SpaceManageRoomViewState>() {

    interface Listener {
        fun toggleSelection(childInfo: SpaceChildInfo)
        fun retry()
        fun loadAdditionalItemsIfNeeded()
    }

    var listener: Listener? = null
    private val matchFilter = SpaceChildInfoMatchFilter()

    override fun buildModels(data: SpaceManageRoomViewState?) {
        val host = this
        val roomListAsync = data?.childrenInfo
        if (roomListAsync is Incomplete) {
            loadingItem { id("loading") }
            return
        }
        if (roomListAsync is Fail) {
            errorWithRetryItem {
                id("Api Error")
                text(host.errorFormatter.toHumanReadable(roomListAsync.error))
                listener { host.listener?.retry() }
            }
            return
        }

        val roomList = roomListAsync?.invoke()?.children ?: return

        val directChildren = roomList.filter {
            it.parentRoomId == data.spaceId
            /** Only direct children **/
        }
        matchFilter.filter = data.currentFilter
        val filteredResult = directChildren.filter { matchFilter.test(it) }

        if (filteredResult.isEmpty()) {
            genericFooterItem {
                id("empty_result")
                text(host.stringProvider.getString(R.string.no_result_placeholder))
            }
        } else {
            filteredResult.forEach { childInfo ->
                roomManageSelectionItem {
                    id(childInfo.childRoomId)
                    matrixItem(
                            data.knownRoomSummaries.firstOrNull { it.roomId == childInfo.childRoomId }?.toMatrixItem()
                                    ?: childInfo.toMatrixItem()
                    )
                    avatarRenderer(host.avatarRenderer)
                    suggested(childInfo.suggested ?: false)
                    selected(data.selectedRooms.contains(childInfo.childRoomId))
                    itemClickListener {
                        host.listener?.toggleSelection(childInfo)
                    }
                }
            }
        }
        val nextToken = roomListAsync.invoke()?.nextToken
        if (nextToken != null) {
            // show loading item
            val paginationStatus = data.paginationStatus
            if (paginationStatus is Fail) {
                errorWithRetryItem {
                    id("error_$nextToken")
                    text(host.errorFormatter.toHumanReadable(paginationStatus.error))
                    listener { host.listener?.loadAdditionalItemsIfNeeded() }
                }
            } else {
                loadingItem {
                    id("pagination_$nextToken")
                    showLoader(true)
                    onVisibilityStateChanged { _, _, visibilityState ->
                        // Do something with the new visibility state
                        if (visibilityState == VisibilityState.VISIBLE) {
                            // we can trigger a seamless load of additional items
                            host.listener?.loadAdditionalItemsIfNeeded()
                        }
                    }
                }
            }
        }
    }
}
