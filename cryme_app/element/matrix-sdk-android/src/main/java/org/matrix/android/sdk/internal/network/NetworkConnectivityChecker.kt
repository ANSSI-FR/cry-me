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
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.network

import androidx.annotation.WorkerThread
import kotlinx.coroutines.runBlocking
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.homeserver.HomeServerPinger
import org.matrix.android.sdk.internal.util.BackgroundDetectionObserver
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

interface NetworkConnectivityChecker {
    /**
     * Returns true when internet is available
     */
    @WorkerThread
    fun hasInternetAccess(forcePing: Boolean): Boolean

    fun register(listener: Listener)
    fun unregister(listener: Listener)

    interface Listener {
        fun onConnectivityChanged()
    }
}

@SessionScope
internal class DefaultNetworkConnectivityChecker @Inject constructor(private val homeServerPinger: HomeServerPinger,
                                                                     private val backgroundDetectionObserver: BackgroundDetectionObserver,
                                                                     private val networkCallbackStrategy: NetworkCallbackStrategy) :
    NetworkConnectivityChecker {

    private val hasInternetAccess = AtomicBoolean(true)
    private val listeners = Collections.synchronizedSet(LinkedHashSet<NetworkConnectivityChecker.Listener>())
    private val backgroundDetectionObserverListener = object : BackgroundDetectionObserver.Listener {
        override fun onMoveToForeground() {
            bind()
        }

        override fun onMoveToBackground() {
            unbind()
        }
    }

    /**
     * Returns true when internet is available
     */
    @WorkerThread
    override fun hasInternetAccess(forcePing: Boolean): Boolean {
        return if (forcePing) {
            runBlocking {
                homeServerPinger.canReachHomeServer()
            }
        } else {
            hasInternetAccess.get()
        }
    }

    override fun register(listener: NetworkConnectivityChecker.Listener) {
        if (listeners.isEmpty()) {
            if (!backgroundDetectionObserver.isInBackground) {
                bind()
            }
            backgroundDetectionObserver.register(backgroundDetectionObserverListener)
        }
        listeners.add(listener)
    }

    override fun unregister(listener: NetworkConnectivityChecker.Listener) {
        listeners.remove(listener)
        if (listeners.isEmpty()) {
            backgroundDetectionObserver.unregister(backgroundDetectionObserverListener)
        }
    }

    private fun bind() {
        networkCallbackStrategy.register {
            val localListeners = listeners.toList()
            localListeners.forEach {
                it.onConnectivityChanged()
            }
        }
        homeServerPinger.canReachHomeServer {
            hasInternetAccess.set(it)
        }
    }

    private fun unbind() {
        networkCallbackStrategy.unregister()
    }
}
