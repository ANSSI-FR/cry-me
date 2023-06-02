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

package im.vector.app.espresso.tools

import android.app.Activity
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import im.vector.app.activityIdlingResource
import im.vector.app.waitForView
import im.vector.app.withIdlingResource
import org.hamcrest.Matcher
import org.hamcrest.Matchers.not

inline fun <reified T : Activity> waitUntilActivityVisible(noinline block: (() -> Unit) = {}) {
    withIdlingResource(activityIdlingResource(T::class.java), block)
}

fun waitUntilViewVisible(viewMatcher: Matcher<View>) {
    onView(ViewMatchers.isRoot()).perform(waitForView(viewMatcher))
}

fun waitUntilDialogVisible(viewMatcher: Matcher<View>) {
    onView(viewMatcher).inRoot(isDialog()).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    waitUntilViewVisible(viewMatcher)
}
