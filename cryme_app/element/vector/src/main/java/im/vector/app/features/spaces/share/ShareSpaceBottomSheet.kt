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

package im.vector.app.features.spaces.share

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.core.utils.startSharePlainTextIntent
import im.vector.app.databinding.BottomSheetSpaceInviteBinding
import im.vector.app.features.invite.InviteUsersToRoomActivity
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class ShareSpaceBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetSpaceInviteBinding>() {

    @Parcelize
    data class Args(
            val spaceId: String,
            val postCreation: Boolean = false
    ) : Parcelable

    override val showExpanded = true

    private val viewModel: ShareSpaceViewModel by fragmentViewModel(ShareSpaceViewModel::class)

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetSpaceInviteBinding {
        return BottomSheetSpaceInviteBinding.inflate(inflater, container, false)
    }

    override fun invalidate() = withState(viewModel) { state ->
        super.invalidate()
        val summary = state.spaceSummary.invoke()

        val spaceName = summary?.name

        if (state.postCreation) {
            views.headerText.text = getString(R.string.invite_people_to_your_space)
            views.descriptionText.setTextOrHide(getString(R.string.invite_people_to_your_space_desc, spaceName))
        } else {
            views.headerText.text = getString(R.string.invite_to_space, spaceName)
            views.descriptionText.setTextOrHide(null)
        }

        views.inviteByLinkButton.isVisible = state.canShareLink
        views.inviteByMxidButton.isVisible = state.canInviteByMxId
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.inviteByMxidButton.debouncedClicks {
            viewModel.handle(ShareSpaceAction.InviteByMxId)
        }

        views.inviteByLinkButton.debouncedClicks {
            viewModel.handle(ShareSpaceAction.InviteByLink)
        }

        viewModel.observeViewEvents { event ->
            when (event) {
                is ShareSpaceViewEvents.NavigateToInviteUser -> {
                    val intent = InviteUsersToRoomActivity.getIntent(requireContext(), event.spaceId)
                    startActivity(intent)
                }
                is ShareSpaceViewEvents.ShowInviteByLink     -> {
                    startSharePlainTextIntent(
                            fragment = this,
                            activityResultLauncher = null,
                            chooserTitle = getString(R.string.share_by_text),
                            text = getString(R.string.share_space_link_message, event.spaceName, event.permalink),
                            extraTitle = getString(R.string.share_space_link_message, event.spaceName, event.permalink)
                    )
                }
            }
        }
    }

    companion object {

        fun show(fragmentManager: FragmentManager, spaceId: String, postCreation: Boolean = false): ShareSpaceBottomSheet {
            return ShareSpaceBottomSheet().apply {
                setArguments(Args(spaceId = spaceId, postCreation = postCreation))
            }.also {
                it.show(fragmentManager, ShareSpaceBottomSheet::class.java.name)
            }
        }
    }
}
