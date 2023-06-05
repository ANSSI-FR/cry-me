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

package im.vector.app.features.matrixto

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.commitTransaction
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetMatrixToCardBinding
import im.vector.app.features.home.AvatarRenderer
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import javax.inject.Inject
import kotlin.reflect.KClass

@AndroidEntryPoint
class MatrixToBottomSheet :
        VectorBaseBottomSheetDialogFragment<BottomSheetMatrixToCardBinding>() {

    @Parcelize
    data class MatrixToArgs(
            val matrixToLink: String
    ) : Parcelable

    @Inject lateinit var avatarRenderer: AvatarRenderer

    var interactionListener: InteractionListener? = null

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetMatrixToCardBinding {
        return BottomSheetMatrixToCardBinding.inflate(inflater, container, false)
    }

    private val viewModel by fragmentViewModel(MatrixToBottomSheetViewModel::class)

    interface InteractionListener {
        fun mxToBottomSheetNavigateToRoom(roomId: String)
        fun mxToBottomSheetSwitchToSpace(spaceId: String)
    }

    override fun invalidate() = withState(viewModel) { state ->
        super.invalidate()
        when (state.linkType) {
            is PermalinkData.RoomLink     -> {
                views.matrixToCardContentLoading.isVisible = state.roomPeekResult is Incomplete
                showFragment(MatrixToRoomSpaceFragment::class, Bundle())
            }
            is PermalinkData.UserLink     -> {
                views.matrixToCardContentLoading.isVisible = state.matrixItem is Incomplete
                showFragment(MatrixToUserFragment::class, Bundle())
            }
            is PermalinkData.GroupLink    -> {
            }
            is PermalinkData.FallbackLink -> {
            }
        }
    }

    private fun showFragment(fragmentClass: KClass<out Fragment>, bundle: Bundle) {
        if (childFragmentManager.findFragmentByTag(fragmentClass.simpleName) == null) {
            childFragmentManager.commitTransaction {
                replace(views.matrixToCardFragmentContainer.id,
                        fragmentClass.java,
                        bundle,
                        fragmentClass.simpleName
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.observeViewEvents {
            when (it) {
                is MatrixToViewEvents.NavigateToRoom  -> {
                    interactionListener?.mxToBottomSheetNavigateToRoom(it.roomId)
                    dismiss()
                }
                MatrixToViewEvents.Dismiss            -> dismiss()
                is MatrixToViewEvents.NavigateToSpace -> {
                    interactionListener?.mxToBottomSheetSwitchToSpace(it.spaceId)
                    dismiss()
                }
                is MatrixToViewEvents.ShowModalError  -> {
                    MaterialAlertDialogBuilder(requireContext())
                            .setMessage(it.error)
                            .setPositiveButton(getString(R.string.ok), null)
                            .show()
                }
            }
        }
    }

    companion object {
        fun withLink(matrixToLink: String): MatrixToBottomSheet {
            return MatrixToBottomSheet().apply {
                setArguments(MatrixToArgs(matrixToLink = matrixToLink))
            }
        }
    }
}
