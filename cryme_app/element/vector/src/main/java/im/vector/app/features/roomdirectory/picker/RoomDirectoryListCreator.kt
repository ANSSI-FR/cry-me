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
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.roomdirectory.picker

import im.vector.app.R
import im.vector.app.core.resources.StringArrayProvider
import im.vector.app.features.roomdirectory.RoomDirectoryData
import im.vector.app.features.roomdirectory.RoomDirectoryServer
import org.matrix.android.sdk.api.MatrixPatterns.getDomain
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.thirdparty.ThirdPartyProtocol
import javax.inject.Inject

class RoomDirectoryListCreator @Inject constructor(
        private val stringArrayProvider: StringArrayProvider,
        private val session: Session
) {

    fun computeDirectories(thirdPartyProtocolData: Map<String, ThirdPartyProtocol>,
                           customHomeservers: Set<String>): List<RoomDirectoryServer> {
        val result = ArrayList<RoomDirectoryServer>()

        val protocols = ArrayList<RoomDirectoryData>()

        // Add user homeserver name
        val userHsName = session.myUserId.getDomain()

        // Add default protocol
        protocols.add(
                RoomDirectoryData(
                        homeServer = null,
                        displayName = RoomDirectoryData.MATRIX_PROTOCOL_NAME,
                        includeAllNetworks = false
                )
        )

        // Add result of the request
        thirdPartyProtocolData.forEach {
            it.value.instances?.forEach { thirdPartyProtocolInstance ->
                protocols.add(
                        RoomDirectoryData(
                                homeServer = null,
                                displayName = thirdPartyProtocolInstance.desc ?: "",
                                thirdPartyInstanceId = thirdPartyProtocolInstance.instanceId,
                                includeAllNetworks = false,
                                // Default to protocol icon
                                avatarUrl = thirdPartyProtocolInstance.icon ?: it.value.icon
                        )
                )
            }
        }

        // Add all rooms
        protocols.add(
                RoomDirectoryData(
                        homeServer = null,
                        displayName = RoomDirectoryData.MATRIX_PROTOCOL_NAME,
                        includeAllNetworks = true
                )
        )

        result.add(
                RoomDirectoryServer(
                        serverName = userHsName,
                        isUserServer = true,
                        isManuallyAdded = false,
                        protocols = protocols
                )
        )

        // Add custom directory servers, form the config file, excluding the current user homeserver
        stringArrayProvider.getStringArray(R.array.room_directory_servers)
                .filter { it != userHsName }
                .forEach {
                    // Use the server name as a default display name
                    result.add(
                            RoomDirectoryServer(
                                    serverName = it,
                                    isUserServer = false,
                                    isManuallyAdded = false,
                                    protocols = listOf(
                                            RoomDirectoryData(
                                                    homeServer = it,
                                                    displayName = RoomDirectoryData.MATRIX_PROTOCOL_NAME,
                                                    includeAllNetworks = false
                                            )
                                    )
                            )
                    )
                }

        // Add manually added server by the user
        customHomeservers
                .forEach {
                    // Use the server name as a default display name
                    result.add(
                            RoomDirectoryServer(
                                    serverName = it,
                                    isUserServer = false,
                                    isManuallyAdded = true,
                                    protocols = listOf(
                                            RoomDirectoryData(
                                                    homeServer = it,
                                                    displayName = RoomDirectoryData.MATRIX_PROTOCOL_NAME,
                                                    includeAllNetworks = false
                                            )
                                    )
                            )
                    )
                }

        return result
    }
}
