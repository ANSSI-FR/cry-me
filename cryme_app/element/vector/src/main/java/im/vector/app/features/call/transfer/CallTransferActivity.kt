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

package im.vector.app.features.call.transfer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.viewModel
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityCallTransferBinding
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
data class CallTransferArgs(val callId: String) : Parcelable

private const val USER_LIST_FRAGMENT_TAG = "USER_LIST_FRAGMENT_TAG"

@AndroidEntryPoint
class CallTransferActivity : VectorBaseActivity<ActivityCallTransferBinding>() {

    @Inject lateinit var errorFormatter: ErrorFormatter

    private lateinit var sectionsPagerAdapter: CallTransferPagerAdapter

    private val callTransferViewModel: CallTransferViewModel by viewModel()

    override fun getBinding() = ActivityCallTransferBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.vectorCoordinatorLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        waitingView = views.waitingView.waitingView

        callTransferViewModel.observeViewEvents {
            when (it) {
                is CallTransferViewEvents.Dismiss        -> finish()
                CallTransferViewEvents.Loading           -> showWaitingView()
                is CallTransferViewEvents.FailToTransfer -> showSnackbar(getString(R.string.call_transfer_failure))
            }
        }

        sectionsPagerAdapter = CallTransferPagerAdapter(this)
        views.callTransferViewPager.adapter = sectionsPagerAdapter

        TabLayoutMediator(views.callTransferTabLayout, views.callTransferViewPager) { tab, position ->
            when (position) {
                CallTransferPagerAdapter.USER_LIST_INDEX -> tab.text = getString(R.string.call_transfer_users_tab_title)
                CallTransferPagerAdapter.DIAL_PAD_INDEX  -> tab.text = getString(R.string.call_dial_pad_title)
            }
        }.attach()
        configureToolbar(views.callTransferToolbar)
        views.callTransferToolbar.title = getString(R.string.call_transfer_title)
        setupConnectAction()
    }

    private fun setupConnectAction() {
        views.callTransferConnectAction.debouncedClicks {
            when (views.callTransferTabLayout.selectedTabPosition) {
                CallTransferPagerAdapter.USER_LIST_INDEX -> {
                    val selectedUser = sectionsPagerAdapter.userListFragment?.getCurrentState()?.getSelectedMatrixId()?.firstOrNull() ?: return@debouncedClicks
                    val action = CallTransferAction.ConnectWithUserId(views.callTransferConsultCheckBox.isChecked, selectedUser)
                    callTransferViewModel.handle(action)
                }
                CallTransferPagerAdapter.DIAL_PAD_INDEX  -> {
                    val phoneNumber = sectionsPagerAdapter.dialPadFragment?.getRawInput() ?: return@debouncedClicks
                    val action = CallTransferAction.ConnectWithPhoneNumber(views.callTransferConsultCheckBox.isChecked, phoneNumber)
                    callTransferViewModel.handle(action)
                }
            }
        }
    }

    companion object {

        fun newIntent(context: Context, callId: String): Intent {
            return Intent(context, CallTransferActivity::class.java).also {
                it.putExtra(Mavericks.KEY_ARG, CallTransferArgs(callId))
            }
        }
    }
}
