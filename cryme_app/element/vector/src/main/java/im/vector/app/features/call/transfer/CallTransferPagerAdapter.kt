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

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import im.vector.app.core.extensions.toMvRxBundle
import im.vector.app.features.call.dialpad.DialPadFragment
import im.vector.app.features.settings.VectorLocale
import im.vector.app.features.userdirectory.UserListFragment
import im.vector.app.features.userdirectory.UserListFragmentArgs

class CallTransferPagerAdapter(
        private val fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    companion object {
        const val USER_LIST_INDEX = 0
        const val DIAL_PAD_INDEX = 1
    }

    val userListFragment: UserListFragment?
        get() = findFragmentAtPosition(USER_LIST_INDEX) as? UserListFragment
    val dialPadFragment: DialPadFragment?
        get() = findFragmentAtPosition(DIAL_PAD_INDEX) as? DialPadFragment

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        val fragment: Fragment
        if (position == 0) {
            fragment = fragmentActivity.supportFragmentManager.fragmentFactory.instantiate(fragmentActivity.classLoader, UserListFragment::class.java.name)
            fragment.arguments = UserListFragmentArgs(
                    title = "",
                    menuResId = -1,
                    singleSelection = true,
                    showInviteActions = false,
                    showToolbar = false,
                    showContactBookAction = false
            ).toMvRxBundle()
        } else {
            fragment = fragmentActivity.supportFragmentManager.fragmentFactory.instantiate(fragmentActivity.classLoader, DialPadFragment::class.java.name)
            (fragment as DialPadFragment).apply {
                arguments = Bundle().apply {
                    putBoolean(DialPadFragment.EXTRA_ENABLE_DELETE, true)
                    putBoolean(DialPadFragment.EXTRA_ENABLE_OK, false)
                    putString(DialPadFragment.EXTRA_REGION_CODE, VectorLocale.applicationLocale.country)
                }
            }
        }
        return fragment
    }

    private fun findFragmentAtPosition(position: Int): Fragment? {
        return fragmentActivity.supportFragmentManager.findFragmentByTag("f$position")
    }
}
