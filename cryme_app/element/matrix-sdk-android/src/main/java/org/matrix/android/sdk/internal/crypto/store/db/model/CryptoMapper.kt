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
package org.matrix.android.sdk.internal.crypto.store.db.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.internal.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.internal.crypto.model.rest.UnsignedDeviceInfo
import org.matrix.android.sdk.internal.di.SerializeNulls
import timber.log.Timber

object CryptoMapper {

    private val moshi = Moshi.Builder().add(SerializeNulls.JSON_ADAPTER_FACTORY).build()
    private val listMigrationAdapter = moshi.adapter<List<String>>(Types.newParameterizedType(
            List::class.java,
            String::class.java,
            Any::class.java
    ))
    private val mapMigrationAdapter = moshi.adapter<JsonDict>(Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Any::class.java
    ))
    private val mapOfStringMigrationAdapter = moshi.adapter<Map<String, Map<String, String>>>(Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Any::class.java
    ))

    internal fun mapToEntity(deviceInfo: CryptoDeviceInfo): DeviceInfoEntity {
        return DeviceInfoEntity(primaryKey = DeviceInfoEntity.createPrimaryKey(deviceInfo.userId, deviceInfo.deviceId))
                .also { updateDeviceInfoEntity(it, deviceInfo) }
    }

    internal fun updateDeviceInfoEntity(entity: DeviceInfoEntity, deviceInfo: CryptoDeviceInfo) {
        entity.userId = deviceInfo.userId
        entity.deviceId = deviceInfo.deviceId
        entity.algorithmListJson = listMigrationAdapter.toJson(deviceInfo.algorithms)
        entity.keysMapJson = mapMigrationAdapter.toJson(deviceInfo.keys)
        entity.signatureMapJson = mapMigrationAdapter.toJson(deviceInfo.signatures)
        entity.isBlocked = deviceInfo.isBlocked
        val deviceInfoTrustLevel = deviceInfo.trustLevel
        if (deviceInfoTrustLevel == null) {
            entity.trustLevelEntity?.deleteFromRealm()
            entity.trustLevelEntity = null
        } else {
            if (entity.trustLevelEntity == null) {
                // Create a new TrustLevelEntity object
                entity.trustLevelEntity = TrustLevelEntity()
            }
            // Update the existing TrustLevelEntity object
            entity.trustLevelEntity?.crossSignedVerified = deviceInfoTrustLevel.crossSigningVerified
            entity.trustLevelEntity?.locallyVerified = deviceInfoTrustLevel.locallyVerified
        }
        // We store the device name if present now
        entity.unsignedMapJson = deviceInfo.unsigned?.deviceDisplayName
    }

    internal fun mapToModel(deviceInfoEntity: DeviceInfoEntity): CryptoDeviceInfo {
        return CryptoDeviceInfo(
                userId = deviceInfoEntity.userId ?: "",
                deviceId = deviceInfoEntity.deviceId ?: "",
                isBlocked = deviceInfoEntity.isBlocked ?: false,
                trustLevel = deviceInfoEntity.trustLevelEntity?.let {
                    DeviceTrustLevel(it.crossSignedVerified ?: false, it.locallyVerified)
                },
                unsigned = deviceInfoEntity.unsignedMapJson?.let { UnsignedDeviceInfo(deviceDisplayName = it) },
                signatures = deviceInfoEntity.signatureMapJson?.let {
                    try {
                        mapOfStringMigrationAdapter.fromJson(it)
                    } catch (failure: Throwable) {
                        Timber.e(failure)
                        null
                    }
                },
                keys = deviceInfoEntity.keysMapJson?.let {
                    try {
                        moshi.adapter<Map<String, String>>(Types.newParameterizedType(
                                Map::class.java,
                                String::class.java,
                                Any::class.java
                        )).fromJson(it)
                    } catch (failure: Throwable) {
                        Timber.e(failure)
                        null
                    }
                },
                algorithms = deviceInfoEntity.algorithmListJson?.let {
                    try {
                        listMigrationAdapter.fromJson(it)
                    } catch (failure: Throwable) {
                        Timber.e(failure)
                        null
                    }
                },
                firstTimeSeenLocalTs = deviceInfoEntity.firstTimeSeenLocalTs
        )
    }
}
