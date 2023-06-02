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
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.contentscanner.db

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.contentscanner.ScanState
import org.matrix.android.sdk.api.session.contentscanner.ScanStatusInfo
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.internal.di.ContentScannerDatabase
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.contentscanner.data.ContentScannerStore
import org.matrix.android.sdk.internal.util.isValidUrl
import javax.inject.Inject

@SessionScope
internal class RealmContentScannerStore @Inject constructor(
        @ContentScannerDatabase
        private val realmConfiguration: RealmConfiguration
) : ContentScannerStore {

    private val monarchy = Monarchy.Builder()
            .setRealmConfiguration(realmConfiguration)
            .build()

    override fun getScannerUrl(): String? {
        return monarchy.fetchAllMappedSync(
                { realm ->
                    realm.where<ContentScannerInfoEntity>()
                }, {
            it.serverUrl
        }
        ).firstOrNull()
    }

    override fun setScannerUrl(url: String?) {
        monarchy.runTransactionSync { realm ->
            val info = realm.where<ContentScannerInfoEntity>().findFirst()
                    ?: realm.createObject()
            info.serverUrl = url
        }
    }

    override fun enableScanner(enabled: Boolean) {
        monarchy.runTransactionSync { realm ->
            val info = realm.where<ContentScannerInfoEntity>().findFirst()
                    ?: realm.createObject()
            info.enabled = enabled
        }
    }

    override fun isScanEnabled(): Boolean {
        return monarchy.fetchAllMappedSync(
                { realm ->
                    realm.where<ContentScannerInfoEntity>()
                }, {
            it.enabled.orFalse() && it.serverUrl?.isValidUrl().orFalse()
        }
        ).firstOrNull().orFalse()
    }

    override fun updateStateForContent(mxcUrl: String, state: ScanState, scannerUrl: String?) {
        monarchy.runTransactionSync {
            ContentScanResultEntity.getOrCreate(it, mxcUrl, scannerUrl).scanResult = state
        }
    }

    override fun updateScanResultForContent(mxcUrl: String, scannerUrl: String?, state: ScanState, humanReadable: String) {
        monarchy.runTransactionSync {
            ContentScanResultEntity.getOrCreate(it, mxcUrl, scannerUrl).apply {
                scanResult = state
                scanDateTimestamp = System.currentTimeMillis()
                humanReadableMessage = humanReadable
            }
        }
    }

    override fun isScanResultKnownOrInProgress(mxcUrl: String, scannerUrl: String?): Boolean {
        var isKnown = false
        monarchy.runTransactionSync {
            val info = ContentScanResultEntity.get(it, mxcUrl, scannerUrl)?.scanResult
            isKnown = when (info) {
                ScanState.IN_PROGRESS,
                ScanState.TRUSTED,
                ScanState.INFECTED -> true
                else               -> false
            }
        }
        return isKnown
    }

    override fun getScanResult(mxcUrl: String): ScanStatusInfo? {
        return monarchy.fetchAllMappedSync({ realm ->
            realm.where<ContentScanResultEntity>()
                    .equalTo(ContentScanResultEntityFields.MEDIA_URL, mxcUrl)
                    .apply {
                        getScannerUrl()?.let {
                            equalTo(ContentScanResultEntityFields.SCANNER_URL, it)
                        }
                    }
        }, {
            it.toModel()
        })
                .firstOrNull()
    }

    override fun getLiveScanResult(mxcUrl: String): LiveData<Optional<ScanStatusInfo>> {
        val liveData = monarchy.findAllMappedWithChanges(
                { realm: Realm ->
                    realm.where<ContentScanResultEntity>()
                            .equalTo(ContentScanResultEntityFields.MEDIA_URL, mxcUrl)
                            .equalTo(ContentScanResultEntityFields.SCANNER_URL, getScannerUrl())
                },
                { entity ->
                    entity.toModel()
                }
        )
        return Transformations.map(liveData) {
            it.firstOrNull().toOptional()
        }
    }
}
