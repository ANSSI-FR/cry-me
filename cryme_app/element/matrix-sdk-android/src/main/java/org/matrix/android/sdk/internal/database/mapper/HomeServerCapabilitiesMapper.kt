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

package org.matrix.android.sdk.internal.database.mapper

import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.api.session.homeserver.RoomCapabilitySupport
import org.matrix.android.sdk.api.session.homeserver.RoomVersionCapabilities
import org.matrix.android.sdk.api.session.homeserver.RoomVersionInfo
import org.matrix.android.sdk.api.session.homeserver.RoomVersionStatus
import org.matrix.android.sdk.internal.database.model.HomeServerCapabilitiesEntity
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.session.homeserver.RoomVersions
import org.matrix.android.sdk.internal.session.room.version.DefaultRoomVersionService

/**
 * HomeServerCapabilitiesEntity -> HomeSeverCapabilities
 */
internal object HomeServerCapabilitiesMapper {

    fun map(entity: HomeServerCapabilitiesEntity): HomeServerCapabilities {
        return HomeServerCapabilities(
                canChangePassword = entity.canChangePassword,
                maxUploadFileSize = entity.maxUploadFileSize,
                lastVersionIdentityServerSupported = entity.lastVersionIdentityServerSupported,
                defaultIdentityServerUrl = entity.defaultIdentityServerUrl,
                roomVersions = mapRoomVersion(entity.roomVersionsJson)
        )
    }

    private fun mapRoomVersion(roomVersionsJson: String?): RoomVersionCapabilities? {
        roomVersionsJson ?: return null

        return tryOrNull {
            MoshiProvider.providesMoshi().adapter(RoomVersions::class.java).fromJson(roomVersionsJson)?.let { roomVersions ->
                RoomVersionCapabilities(
                        defaultRoomVersion = roomVersions.default ?: DefaultRoomVersionService.DEFAULT_ROOM_VERSION,
                        supportedVersion = roomVersions.available?.entries?.map { entry ->
                            RoomVersionInfo(entry.key, RoomVersionStatus.STABLE
                                    .takeIf { entry.value == "stable" }
                                    ?: RoomVersionStatus.UNSTABLE)
                        }.orEmpty(),
                        capabilities = roomVersions.roomCapabilities?.entries?.mapNotNull { entry ->
                            (entry.value as? Map<*, *>)?.let {
                                val preferred = it["preferred"] as? String ?: return@mapNotNull null
                                val support = (it["support"] as? List<*>)?.filterIsInstance<String>()
                                entry.key to RoomCapabilitySupport(preferred, support.orEmpty())
                            }
                        }?.toMap()
                        // Just for debug purpose
//                                ?: mapOf(
//                                HomeServerCapabilities.ROOM_CAP_RESTRICTED to RoomCapabilitySupport(
//                                        preferred = null,
//                                        support = listOf("org.matrix.msc3083")
//                                )
//                                )
                )
            }
        }
    }
}
