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
 * Copyright 2019 New Vector Ltd
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
package im.vector.app.features.roomprofile.alias.detail

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.core.epoxy.bottomSheetDividerItem
import im.vector.app.core.epoxy.bottomsheet.bottomSheetActionItem
import im.vector.app.core.ui.bottomsheet.bottomSheetTitleItem
import javax.inject.Inject

/**
 * Epoxy controller for room alias actions
 */
class RoomAliasBottomSheetController @Inject constructor() : TypedEpoxyController<RoomAliasBottomSheetState>() {

    var listener: Listener? = null

    override fun buildModels(state: RoomAliasBottomSheetState) {
        bottomSheetTitleItem {
            id("alias")
            title(state.alias)
            subTitle(state.matrixToLink)
        }

        // Notifications
        bottomSheetDividerItem {
            id("aliasSeparator")
        }

        var idx = 0
        // Share
        state.matrixToLink?.let {
            RoomAliasBottomSheetSharedAction.ShareAlias(it).toBottomSheetItem(++idx)
        }

        // Action on published alias
        if (state.isPublished) {
            // Published address
            if (state.canEditCanonicalAlias) {
                if (state.isMainAlias) {
                    RoomAliasBottomSheetSharedAction.UnsetMainAlias.toBottomSheetItem(++idx)
                } else {
                    RoomAliasBottomSheetSharedAction.SetMainAlias(state.alias).toBottomSheetItem(++idx)
                }
                RoomAliasBottomSheetSharedAction.UnPublishAlias(state.alias).toBottomSheetItem(++idx)
            }
        }

        if (state.isLocal) {
            // Local address
            if (state.canEditCanonicalAlias && state.isPublished.not()) {
                // Publish
                RoomAliasBottomSheetSharedAction.PublishAlias(state.alias).toBottomSheetItem(++idx)
            }
            // Delete
            RoomAliasBottomSheetSharedAction.DeleteAlias(state.alias).toBottomSheetItem(++idx)
        }
    }

    private fun RoomAliasBottomSheetSharedAction.toBottomSheetItem(index: Int) {
        val host = this@RoomAliasBottomSheetController
        return bottomSheetActionItem {
            id("action_$index")
            iconRes(iconResId)
            textRes(titleRes)
            destructive(this@toBottomSheetItem.destructive)
            listener { host.listener?.didSelectMenuAction(this@toBottomSheetItem) }
        }
    }

    interface Listener {
        fun didSelectMenuAction(quickAction: RoomAliasBottomSheetSharedAction)
    }
}
