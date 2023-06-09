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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.database

import android.os.Looper
import androidx.annotation.MainThread
import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.SessionLifecycleObserver
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject
import kotlin.concurrent.getOrSet

/**
 * This class keeps an instance of realm open in the main thread so you can grab it whenever you want to get a realm
 * instance. This does check each time if you are on the main thread or not and returns the appropriate realm instance.
 */
@SessionScope
internal class RealmSessionProvider @Inject constructor(@SessionDatabase private val monarchy: Monarchy) :
    SessionLifecycleObserver {

    private val realmThreadLocal = ThreadLocal<Realm>()

    /**
     * Allow you to execute a block with an opened realm. It automatically closes it if necessary (ie. when not in main thread)
     */
    fun <R> withRealm(block: (Realm) -> R): R {
        return getRealmWrapper().withRealm(block)
    }

    @MainThread
    override fun onSessionStarted(session: Session) {
        realmThreadLocal.getOrSet {
            Realm.getInstance(monarchy.realmConfiguration)
        }
    }

    @MainThread
    override fun onSessionStopped(session: Session) {
        realmThreadLocal.get()?.close()
        realmThreadLocal.remove()
    }

    private fun getRealmWrapper(): RealmInstanceWrapper {
        val isOnMainThread = isOnMainThread()
        val realm = if (isOnMainThread) {
            realmThreadLocal.getOrSet {
                Realm.getInstance(monarchy.realmConfiguration)
            }
        } else {
            Realm.getInstance(monarchy.realmConfiguration)
        }
        return RealmInstanceWrapper(realm, closeRealmOnClose = !isOnMainThread)
    }

    private fun isOnMainThread() = Looper.myLooper() == Looper.getMainLooper()
}
