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

import android.annotation.TargetApi
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import androidx.core.content.getSystemService
import timber.log.Timber
import javax.inject.Inject

internal interface NetworkCallbackStrategy {
    fun register(hasChanged: () -> Unit)
    fun unregister()
}

internal class FallbackNetworkCallbackStrategy @Inject constructor(private val context: Context,
                                                                   private val networkInfoReceiver: NetworkInfoReceiver) : NetworkCallbackStrategy {

    @Suppress("DEPRECATION")
    val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)

    override fun register(hasChanged: () -> Unit) {
        networkInfoReceiver.isConnectedCallback = {
            hasChanged()
        }
        context.registerReceiver(networkInfoReceiver, filter)
    }

    override fun unregister() {
        networkInfoReceiver.isConnectedCallback = null
        context.unregisterReceiver(networkInfoReceiver)
    }
}

@TargetApi(Build.VERSION_CODES.N)
internal class PreferredNetworkCallbackStrategy @Inject constructor(context: Context) : NetworkCallbackStrategy {

    private var hasChangedCallback: (() -> Unit)? = null
    private val conn = context.getSystemService<ConnectivityManager>()!!
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onLost(network: Network) {
            hasChangedCallback?.invoke()
        }

        override fun onAvailable(network: Network) {
            hasChangedCallback?.invoke()
        }
    }

    override fun register(hasChanged: () -> Unit) {
        hasChangedCallback = hasChanged
        conn.registerDefaultNetworkCallback(networkCallback)
    }

    override fun unregister() {
        // It can crash after an application update, if not registered
        val doUnregister = hasChangedCallback != null
        hasChangedCallback = null
        if (doUnregister) {
            // Add a try catch for safety
            try {
                conn.unregisterNetworkCallback(networkCallback)
            } catch (t: Throwable) {
                Timber.e(t, "Unable to unregister network callback")
            }
        }
    }
}
