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

package im.vector.app.features.spaces.create

import com.airbnb.epoxy.TypedEpoxyController
import com.google.android.material.textfield.TextInputLayout
import im.vector.app.R
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.ItemStyle
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.features.form.formEditTextItem
import javax.inject.Inject

class SpaceDefaultRoomEpoxyController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider
) : TypedEpoxyController<CreateSpaceState>() {

    var listener: Listener? = null

//    var shouldForceFocusOnce = true

    override fun buildModels(data: CreateSpaceState?) {
        val host = this
        genericFooterItem {
            id("info_help_header")
            style(ItemStyle.TITLE)
            text(
                    if (data?.spaceType == SpaceType.Public) {
                        host.stringProvider.getString(R.string.create_spaces_room_public_header, data.name)
                    } else {
                        host.stringProvider.getString(R.string.create_spaces_room_private_header)
                    }
            )
            textColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
        }

        genericFooterItem {
            id("info_help")
            text(
                    host.stringProvider.getString(
                            if (data?.spaceType == SpaceType.Public) {
                                R.string.create_spaces_room_public_header_desc
                            } else {
                                R.string.create_spaces_room_private_header_desc
                            }
                    )
            )
            textColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_secondary))
        }

        val firstRoomName = data?.defaultRooms?.get(0)
        formEditTextItem {
            id("roomName1")
            enabled(true)
            value(firstRoomName)
            hint(host.stringProvider.getString(R.string.create_room_name_section))
            endIconMode(TextInputLayout.END_ICON_CLEAR_TEXT)
            onTextChange { text ->
                host.listener?.onNameChange(0, text)
            }
        }

        val secondRoomName = data?.defaultRooms?.get(1)
        formEditTextItem {
            id("roomName2")
            enabled(true)
            value(secondRoomName)
            hint(host.stringProvider.getString(R.string.create_room_name_section))
            endIconMode(TextInputLayout.END_ICON_CLEAR_TEXT)
            onTextChange { text ->
                host.listener?.onNameChange(1, text)
            }
        }

        val thirdRoomName = data?.defaultRooms?.get(2)
        formEditTextItem {
            id("roomName3")
            enabled(true)
            value(thirdRoomName)
            hint(host.stringProvider.getString(R.string.create_room_name_section))
            endIconMode(TextInputLayout.END_ICON_CLEAR_TEXT)
            onTextChange { text ->
                host.listener?.onNameChange(2, text)
            }
//            onBind { _, view, _ ->
//                if (shouldForceFocusOnce
//                        && thirdRoomName.isNullOrBlank()
//                        && firstRoomName.isNullOrBlank().not()
//                        && secondRoomName.isNullOrBlank().not()
//                ) {
//                    shouldForceFocusOnce = false
//                    // sad face :(
//                    view.textInputEditText.post {
//                        view.textInputEditText.showKeyboard(true)
//                    }
//                }
//            }
        }
    }

    interface Listener {
        fun onNameChange(index: Int, newName: String)
    }
}
