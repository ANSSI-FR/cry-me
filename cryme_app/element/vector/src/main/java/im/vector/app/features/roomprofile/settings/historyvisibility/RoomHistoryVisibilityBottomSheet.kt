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
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.roomprofile.settings.historyvisibility

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.ui.bottomsheet.BottomSheetGeneric
import im.vector.app.core.ui.bottomsheet.BottomSheetGenericController
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import javax.inject.Inject

@Parcelize
data class RoomHistoryVisibilityBottomSheetArgs(
        val currentRoomHistoryVisibility: RoomHistoryVisibility
) : Parcelable

@AndroidEntryPoint
class RoomHistoryVisibilityBottomSheet : BottomSheetGeneric<RoomHistoryVisibilityState, RoomHistoryVisibilityRadioAction>() {

    private lateinit var roomHistoryVisibilitySharedActionViewModel: RoomHistoryVisibilitySharedActionViewModel
    @Inject lateinit var controller: RoomHistoryVisibilityController
    private val viewModel: RoomHistoryVisibilityViewModel by fragmentViewModel(RoomHistoryVisibilityViewModel::class)

    override fun getController(): BottomSheetGenericController<RoomHistoryVisibilityState, RoomHistoryVisibilityRadioAction> = controller

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        roomHistoryVisibilitySharedActionViewModel = activityViewModelProvider.get(RoomHistoryVisibilitySharedActionViewModel::class.java)
    }

    override fun didSelectAction(action: RoomHistoryVisibilityRadioAction) {
        roomHistoryVisibilitySharedActionViewModel.post(action)
        dismiss()
    }

    override fun invalidate() = withState(viewModel) {
        controller.setData(it)
        super.invalidate()
    }

    companion object {
        fun newInstance(currentRoomHistoryVisibility: RoomHistoryVisibility): RoomHistoryVisibilityBottomSheet {
            return RoomHistoryVisibilityBottomSheet().apply {
                setArguments(RoomHistoryVisibilityBottomSheetArgs(currentRoomHistoryVisibility))
            }
        }
    }
}
