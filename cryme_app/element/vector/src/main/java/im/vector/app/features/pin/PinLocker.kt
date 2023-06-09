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

package im.vector.app.features.pin

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// 2 minutes, when enabled
private const val PERIOD_OF_GRACE_IN_MS = 2 * 60 * 1000L

/**
 * This class is responsible for keeping the status of locking
 * It automatically locks when entering background/foreground with a grace period.
 * You can force to unlock with unlock method, use it whenever the pin code has been validated.
 */
@Singleton
class PinLocker @Inject constructor(
        private val pinCodeStore: PinCodeStore,
        private val vectorPreferences: VectorPreferences
) : DefaultLifecycleObserver {

    enum class State {
        // App is locked, can be unlock
        LOCKED,

        // App is unlocked, the app can be used
        UNLOCKED
    }

    private val liveState = MutableLiveData<State>()

    private var shouldBeLocked = true
    private var entersBackgroundTs = 0L

    fun getLiveState(): LiveData<State> {
        return liveState
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    private fun computeState() {
        GlobalScope.launch {
            val state = if (shouldBeLocked && pinCodeStore.hasEncodedPin()) {
                State.LOCKED
            } else {
                State.UNLOCKED
            }
                    .also { Timber.v("New state: $it") }

            if (liveState.value != state) {
                liveState.postValue(state)
            }
        }
    }

    fun unlock() {
        Timber.v("Unlock app")
        shouldBeLocked = false
        computeState()
    }

    fun screenIsOff() {
        shouldBeLocked = true
        computeState()
    }

    override fun onResume(owner: LifecycleOwner) {
        val timeElapsedSinceBackground = SystemClock.elapsedRealtime() - entersBackgroundTs
        shouldBeLocked = shouldBeLocked || timeElapsedSinceBackground >= getGracePeriod()
        Timber.v("App enters foreground after $timeElapsedSinceBackground ms spent in background shouldBeLocked: $shouldBeLocked")
        computeState()
    }

    override fun onPause(owner: LifecycleOwner) {
        Timber.v("App enters background")
        entersBackgroundTs = SystemClock.elapsedRealtime()
    }

    private fun getGracePeriod(): Long {
        return if (vectorPreferences.useGracePeriod()) {
            PERIOD_OF_GRACE_IN_MS
        } else {
            0L
        }
    }
}
