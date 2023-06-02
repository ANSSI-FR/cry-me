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

import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import timber.log.Timber

internal fun <T> CoroutineScope.asyncTransaction(monarchy: Monarchy, transaction: suspend (realm: Realm) -> T) {
    asyncTransaction(monarchy.realmConfiguration, transaction)
}

internal fun <T> CoroutineScope.asyncTransaction(realmConfiguration: RealmConfiguration, transaction: suspend (realm: Realm) -> T) {
    launch {
        awaitTransaction(realmConfiguration, transaction)
    }
}

private val realmSemaphore = Semaphore(1)

suspend fun <T> awaitTransaction(config: RealmConfiguration, transaction: suspend (realm: Realm) -> T): T {
    return realmSemaphore.withPermit {
        withContext(Dispatchers.IO) {
            Realm.getInstance(config).use { bgRealm ->
                bgRealm.beginTransaction()
                val result: T
                try {
                    val start = System.currentTimeMillis()
                    result = transaction(bgRealm)
                    if (isActive) {
                        bgRealm.commitTransaction()
                        val end = System.currentTimeMillis()
                        val time = end - start
                        Timber.v("Execute transaction in $time millis")
                    }
                } finally {
                    if (bgRealm.isInTransaction) {
                        bgRealm.cancelTransaction()
                    }
                }
                result
            }
        }
    }
}
