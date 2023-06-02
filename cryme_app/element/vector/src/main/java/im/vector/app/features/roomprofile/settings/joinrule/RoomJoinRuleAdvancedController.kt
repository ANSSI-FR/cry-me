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

package im.vector.app.features.roomprofile.settings.joinrule

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.R
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.ItemStyle
import im.vector.app.core.ui.list.genericButtonItem
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.roomprofile.settings.joinrule.advanced.RoomJoinRuleChooseRestrictedState
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import timber.log.Timber
import javax.inject.Inject

class RoomJoinRuleAdvancedController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider,
        private val avatarRenderer: AvatarRenderer
) : TypedEpoxyController<RoomJoinRuleChooseRestrictedState>() {

    interface InteractionListener {
        fun didSelectRule(rules: RoomJoinRules)
    }

    var interactionListener: InteractionListener? = null

    override fun buildModels(state: RoomJoinRuleChooseRestrictedState?) {
        state ?: return
        val choices = state.choices ?: return

        val host = this

        genericFooterItem {
            id("header")
            text(host.stringProvider.getString(R.string.room_settings_room_access_title))
            centered(false)
            style(ItemStyle.TITLE)
            textColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
        }

        genericFooterItem {
            id("desc")
            text(host.stringProvider.getString(R.string.decide_who_can_find_and_join))
            centered(false)
        }

        // invite only
        RoomJoinRuleRadioAction(
                roomJoinRule = RoomJoinRules.INVITE,
                description = stringProvider.getString(R.string.room_settings_room_access_private_description),
                title = stringProvider.getString(R.string.room_settings_room_access_private_invite_only_title),
                isSelected = state.currentRoomJoinRules == RoomJoinRules.INVITE
        ).toRadioBottomSheetItem().let {
            it.listener {
                interactionListener?.didSelectRule(RoomJoinRules.INVITE)
//                listener?.didSelectAction(action)
            }
            add(it)
        }

        if (choices.firstOrNull { it.rule == RoomJoinRules.RESTRICTED } != null) {
            val restrictedRule = choices.first { it.rule == RoomJoinRules.RESTRICTED }
            Timber.w("##@@ ${state.updatedAllowList}")
            spaceJoinRuleItem {
                id("restricted")
                avatarRenderer(host.avatarRenderer)
                needUpgrade(restrictedRule.needUpgrade)
                selected(state.currentRoomJoinRules == RoomJoinRules.RESTRICTED)
                restrictedList(state.updatedAllowList)
                listener { host.interactionListener?.didSelectRule(RoomJoinRules.RESTRICTED) }
            }
        }

        // Public
        RoomJoinRuleRadioAction(
                roomJoinRule = RoomJoinRules.PUBLIC,
                description = stringProvider.getString(R.string.room_settings_room_access_public_description),
                title = stringProvider.getString(R.string.room_settings_room_access_public_title),
                isSelected = state.currentRoomJoinRules == RoomJoinRules.PUBLIC
        ).toRadioBottomSheetItem().let {
            it.listener {
                interactionListener?.didSelectRule(RoomJoinRules.PUBLIC)
            }
            add(it)
        }

        genericButtonItem {
            id("save")
            text("")
        }
    }
}
