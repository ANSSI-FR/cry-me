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

package im.vector.app.features.spaces.people

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.Mavericks
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.GenericIdArgs
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleLoadingBinding
import im.vector.app.features.spaces.share.ShareSpaceBottomSheet
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class SpacePeopleActivity : VectorBaseActivity<ActivitySimpleLoadingBinding>() {

    override fun getBinding() = ActivitySimpleLoadingBinding.inflate(layoutInflater)

    private lateinit var sharedActionViewModel: SpacePeopleSharedActionViewModel

    override fun initUiAndData() {
        super.initUiAndData()
        waitingView = views.waitingView.waitingView
    }

    override fun showWaitingView(text: String?) {
        hideKeyboard()
        views.waitingView.waitingStatusText.isGone = views.waitingView.waitingStatusText.text.isNullOrBlank()
        super.showWaitingView(text)
    }

    override fun hideWaitingView() {
        views.waitingView.waitingStatusText.text = null
        views.waitingView.waitingStatusText.isGone = true
        views.waitingView.waitingHorizontalProgress.progress = 0
        views.waitingView.waitingHorizontalProgress.isVisible = false
        super.hideWaitingView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = intent?.getParcelableExtra<GenericIdArgs>(Mavericks.KEY_ARG)
        if (isFirstCreation()) {
            replaceFragment(
                    views.simpleFragmentContainer,
                    SpacePeopleFragment::class.java,
                    args
            )
        }

        sharedActionViewModel = viewModelProvider.get(SpacePeopleSharedActionViewModel::class.java)
        sharedActionViewModel
                .stream()
                .onEach { sharedAction ->
                    when (sharedAction) {
                        SpacePeopleSharedAction.Dismiss             -> finish()
                        is SpacePeopleSharedAction.NavigateToRoom   -> navigateToRooms(sharedAction)
                        SpacePeopleSharedAction.HideModalLoading    -> hideWaitingView()
                        SpacePeopleSharedAction.ShowModalLoading    -> {
                            showWaitingView()
                        }
                        is SpacePeopleSharedAction.NavigateToInvite -> {
                            ShareSpaceBottomSheet.show(supportFragmentManager, sharedAction.spaceId)
                        }
                    }
                }.launchIn(lifecycleScope)
    }

    private fun navigateToRooms(action: SpacePeopleSharedAction.NavigateToRoom) {
        navigator.openRoom(this, action.roomId)
        finish()
    }

    companion object {
        fun newIntent(context: Context, spaceId: String): Intent {
            return Intent(context, SpacePeopleActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, GenericIdArgs(spaceId))
            }
        }
    }
}
