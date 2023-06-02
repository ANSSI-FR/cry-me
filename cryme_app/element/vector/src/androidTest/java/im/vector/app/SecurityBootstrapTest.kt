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

package im.vector.app

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressBack
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.isInternal
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import im.vector.app.features.MainActivity
import im.vector.app.features.crypto.recover.SetupMode
import im.vector.app.features.home.HomeActivity
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.session.Session

@RunWith(AndroidJUnit4::class)
@LargeTest
class SecurityBootstrapTest : VerificationTestBase() {

    var existingSession: Session? = null

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun createSessionWithCrossSigning() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val matrix = Matrix.getInstance(context)
        val userName = "foobar_${System.currentTimeMillis()}"
        existingSession = createAccountAndSync(matrix, userName, password, true)
        stubAllExternalIntents()
    }

    private fun stubAllExternalIntents() {
        // By default Espresso Intents does not stub any Intents. Stubbing needs to be setup before
        // every test run. In this case all external Intents will be blocked.
        Intents.init()
        intending(not(isInternal())).respondWith(ActivityResult(Activity.RESULT_OK, null))
    }

    @Test
    fun testBasicBootstrap() {
        val userId: String = existingSession!!.myUserId

        uiTestBase.login(userId = userId, password = password, homeServerUrl = homeServerUrl)

        // Thread.sleep(6000)
        withIdlingResource(activityIdlingResource(HomeActivity::class.java)) {
            onView(withId(R.id.roomListContainer))
                    .check(matches(isDisplayed()))
                    .perform(closeSoftKeyboard())
        }

        val activity = EspressoHelper.getCurrentActivity()!!
        val uiSession = (activity as HomeActivity).activeSessionHolder.getActiveSession()

        withIdlingResource(initialSyncIdlingResource(uiSession)) {
            onView(withId(R.id.roomListContainer))
                    .check(matches(isDisplayed()))
        }

        activity.navigator.open4SSetup(activity, SetupMode.NORMAL)

        Thread.sleep(1000)

        onView(withId(R.id.bootstrapSetupSecureUseSecurityKey))
                .check(matches(isDisplayed()))

        onView(withId(R.id.bootstrapSetupSecureUseSecurityPassphrase))
                .check(matches(isDisplayed()))
                .perform(click())

        onView(isRoot())
                .perform(waitForView(withText(R.string.bootstrap_info_text_2)))

        // test back
        onView(isRoot()).perform(pressBack())

        Thread.sleep(1000)

        onView(withId(R.id.bootstrapSetupSecureUseSecurityKey))
                .check(matches(isDisplayed()))

        onView(withId(R.id.bootstrapSetupSecureUseSecurityPassphrase))
                .check(matches(isDisplayed()))
                .perform(click())

        onView(isRoot())
                .perform(waitForView(withText(R.string.bootstrap_info_text_2)))

        onView(withId(R.id.ssss_passphrase_enter_edittext))
                .perform(typeText("person woman man camera tv"))

        onView(withId(R.id.bootstrapSubmit))
                .perform(closeSoftKeyboard(), click())

        // test bad pass
        onView(withId(R.id.ssss_passphrase_enter_edittext))
                .perform(typeText("person woman man cmera tv"))

        onView(withId(R.id.bootstrapSubmit))
                .perform(closeSoftKeyboard(), click())

        onView(withText(R.string.passphrase_passphrase_does_not_match)).check(matches(isDisplayed()))

        onView(withId(R.id.ssss_passphrase_enter_edittext))
                .perform(replaceText("person woman man camera tv"))

        onView(withId(R.id.bootstrapSubmit))
                .perform(closeSoftKeyboard(), click())

        onView(withId(R.id.bottomSheetScrollView))
                .perform(waitForView(withText(R.string.bottom_sheet_save_your_recovery_key_content)))

        intending(hasAction(Intent.ACTION_SEND)).respondWith(ActivityResult(Activity.RESULT_OK, null))

        onView(withId(R.id.recoveryCopy))
                .perform(click())

        Thread.sleep(1000)

        // Dismiss dialog
        onView(withText(R.string.ok)).inRoot(RootMatchers.isDialog()).perform(click())

        onView(withId(R.id.bottomSheetScrollView))
                .perform(waitForView(withText(R.string.bottom_sheet_save_your_recovery_key_content)))

        onView(withText(R.string._continue)).perform(click())

        // Assert that all is configured
        assert(uiSession.cryptoService().crossSigningService().isCrossSigningInitialized())
        assert(uiSession.cryptoService().crossSigningService().canCrossSign())
        assert(uiSession.cryptoService().crossSigningService().allPrivateKeysKnown())
        assert(uiSession.cryptoService().keysBackupService().isEnabled)
        assert(uiSession.cryptoService().keysBackupService().currentBackupVersion != null)
        assert(uiSession.sharedSecretStorageService.isRecoverySetup())
        assert(uiSession.sharedSecretStorageService.isMegolmKeyInBackup())
    }
}
