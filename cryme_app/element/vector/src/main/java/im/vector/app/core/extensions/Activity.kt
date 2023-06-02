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

package im.vector.app.core.extensions

import android.app.Activity
import android.content.Intent
import android.os.Parcelable
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction

fun ComponentActivity.registerStartForActivityResult(onResult: (ActivityResult) -> Unit): ActivityResultLauncher<Intent> {
    return registerForActivityResult(ActivityResultContracts.StartActivityForResult(), onResult)
}

fun AppCompatActivity.addFragment(
        container: ViewGroup,
        fragment: Fragment,
        allowStateLoss: Boolean = false) {
    supportFragmentManager.commitTransaction(allowStateLoss) { add(container.id, fragment) }
}

fun <T : Fragment> AppCompatActivity.addFragment(
        container: ViewGroup,
        fragmentClass: Class<T>,
        params: Parcelable? = null,
        tag: String? = null,
        allowStateLoss: Boolean = false) {
    supportFragmentManager.commitTransaction(allowStateLoss) {
        add(container.id, fragmentClass, params.toMvRxBundle(), tag)
    }
}

fun AppCompatActivity.replaceFragment(
        container: ViewGroup,
        fragment: Fragment,
        tag: String? = null,
        allowStateLoss: Boolean = false) {
    supportFragmentManager.commitTransaction(allowStateLoss) {
        replace(container.id, fragment, tag)
    }
}

fun <T : Fragment> AppCompatActivity.replaceFragment(
        container: ViewGroup,
        fragmentClass: Class<T>,
        params: Parcelable? = null,
        tag: String? = null,
        allowStateLoss: Boolean = false) {
    supportFragmentManager.commitTransaction(allowStateLoss) {
        replace(container.id, fragmentClass, params.toMvRxBundle(), tag)
    }
}

fun AppCompatActivity.addFragmentToBackstack(
        container: ViewGroup,
        fragment: Fragment,
        tag: String? = null,
        allowStateLoss: Boolean = false) {
    supportFragmentManager.commitTransaction(allowStateLoss) {
        replace(container.id, fragment).addToBackStack(tag)
    }
}

fun <T : Fragment> AppCompatActivity.addFragmentToBackstack(
        container: ViewGroup,
        fragmentClass: Class<T>,
        params: Parcelable? = null,
        tag: String? = null,
        allowStateLoss: Boolean = false,
        option: ((FragmentTransaction) -> Unit)? = null) {
    supportFragmentManager.commitTransaction(allowStateLoss) {
        option?.invoke(this)
        replace(container.id, fragmentClass, params.toMvRxBundle(), tag).addToBackStack(tag)
    }
}

fun AppCompatActivity.popBackstack() {
    supportFragmentManager.popBackStack()
}

fun AppCompatActivity.resetBackstack() {
    repeat(supportFragmentManager.backStackEntryCount) {
        supportFragmentManager.popBackStack()
    }
}

fun AppCompatActivity.hideKeyboard() {
    currentFocus?.hideKeyboard()
}

fun Activity.restart() {
    startActivity(intent)
    finish()
}

fun Activity.keepScreenOn() {
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}

fun Activity.endKeepScreenOn() {
    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}
