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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetAddRoomsOrSpacesToSpaceBinding

class SpaceAddRoomSpaceChooserBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetAddRoomsOrSpacesToSpaceBinding>() {

    override val showExpanded = true

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
            BottomSheetAddRoomsOrSpacesToSpaceBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.addSpaces.views.bottomSheetActionClickableZone.debouncedClicks {
            setFragmentResult(REQUEST_KEY, Bundle().apply {
                putString(BUNDLE_KEY_ACTION, ACTION_ADD_SPACES)
            })
            dismiss()
        }

        views.addRooms.views.bottomSheetActionClickableZone.debouncedClicks {
            setFragmentResult(REQUEST_KEY, Bundle().apply {
                putString(BUNDLE_KEY_ACTION, ACTION_ADD_ROOMS)
            })
            dismiss()
        }
    }

    companion object {

        const val REQUEST_KEY = "SpaceAddRoomSpaceChooserBottomSheet"
        const val BUNDLE_KEY_ACTION = "SpaceAddRoomSpaceChooserBottomSheet.Action"
        const val ACTION_ADD_ROOMS = "Action.AddRoom"
        const val ACTION_ADD_SPACES = "Action.AddSpaces"

        fun newInstance(): SpaceAddRoomSpaceChooserBottomSheet {
            return SpaceAddRoomSpaceChooserBottomSheet()
        }
    }
}
