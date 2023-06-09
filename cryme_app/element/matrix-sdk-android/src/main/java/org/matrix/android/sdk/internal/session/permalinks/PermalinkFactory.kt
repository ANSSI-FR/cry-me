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

package org.matrix.android.sdk.internal.session.permalinks

import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.permalinks.PermalinkParser
import org.matrix.android.sdk.api.session.permalinks.PermalinkService.Companion.MATRIX_TO_URL_BASE
import org.matrix.android.sdk.internal.di.UserId
import javax.inject.Inject

internal class PermalinkFactory @Inject constructor(
        @UserId
        private val userId: String,
        private val viaParameterFinder: ViaParameterFinder,
        private val matrixConfiguration: MatrixConfiguration
) {

    fun createPermalink(event: Event, forceMatrixTo: Boolean): String? {
        if (event.roomId.isNullOrEmpty() || event.eventId.isNullOrEmpty()) {
            return null
        }
        return createPermalink(event.roomId, event.eventId, forceMatrixTo)
    }

    fun createPermalink(id: String, forceMatrixTo: Boolean): String? {
        return when {
            id.isEmpty()                    -> null
            !useClientFormat(forceMatrixTo) -> MATRIX_TO_URL_BASE + escape(id)
            else                            -> {
                buildString {
                    append(matrixConfiguration.clientPermalinkBaseUrl)
                    when {
                        MatrixPatterns.isRoomId(id) || MatrixPatterns.isRoomAlias(id) -> append(ROOM_PATH)
                        MatrixPatterns.isUserId(id)                                   -> append(USER_PATH)
                        MatrixPatterns.isGroupId(id)                                  -> append(GROUP_PATH)
                    }
                    append(escape(id))
                }
            }
        }
    }

    fun createRoomPermalink(roomId: String, via: List<String>? = null, forceMatrixTo: Boolean): String? {
        return if (roomId.isEmpty()) {
            null
        } else {
            buildString {
                append(baseUrl(forceMatrixTo))
                if (useClientFormat(forceMatrixTo)) {
                    append(ROOM_PATH)
                }
                append(escape(roomId))
                append(
                        via?.takeIf { it.isNotEmpty() }?.let { viaParameterFinder.asUrlViaParameters(it) }
                                ?: viaParameterFinder.computeViaParams(userId, roomId)
                )
            }
        }
    }

    fun createPermalink(roomId: String, eventId: String, forceMatrixTo: Boolean): String {
        return buildString {
            append(baseUrl(forceMatrixTo))
            if (useClientFormat(forceMatrixTo)) {
                append(ROOM_PATH)
            }
            append(escape(roomId))
            append("/")
            append(escape(eventId))
            append(viaParameterFinder.computeViaParams(userId, roomId))
        }
    }

    fun getLinkedId(url: String): String? {
        val clientBaseUrl = matrixConfiguration.clientPermalinkBaseUrl
        return when {
            url.startsWith(MATRIX_TO_URL_BASE)                     -> url.substring(MATRIX_TO_URL_BASE.length)
            clientBaseUrl != null && url.startsWith(clientBaseUrl) -> {
                when (PermalinkParser.parse(url)) {
                    is PermalinkData.GroupLink -> url.substring(clientBaseUrl.length + GROUP_PATH.length)
                    is PermalinkData.RoomLink  -> url.substring(clientBaseUrl.length + ROOM_PATH.length)
                    is PermalinkData.UserLink  -> url.substring(clientBaseUrl.length + USER_PATH.length)
                    else                       -> null
                }
            }
            else                                                   -> null
        }
                ?.substringBeforeLast("?")
    }

    /**
     * Escape '/' in id, because it is used as a separator
     *
     * @param id the id to escape
     * @return the escaped id
     */
    private fun escape(id: String): String {
        return id.replace("/", "%2F")
    }

    /**
     * Unescape '/' in id
     *
     * @param id the id to escape
     * @return the escaped id
     */
    private fun unescape(id: String): String {
        return id.replace("%2F", "/")
    }

    /**
     * Get the permalink base URL according to the potential one in [MatrixConfiguration.clientPermalinkBaseUrl]
     * and the [forceMatrixTo] parameter.
     *
     * @param forceMatrixTo whether we should force using matrix.to base URL.
     *
     * @return the permalink base URL.
     */
    private fun baseUrl(forceMatrixTo: Boolean): String {
        return matrixConfiguration.clientPermalinkBaseUrl
                ?.takeUnless { forceMatrixTo }
                ?: MATRIX_TO_URL_BASE
    }

    private fun useClientFormat(forceMatrixTo: Boolean): Boolean {
        return !forceMatrixTo && matrixConfiguration.clientPermalinkBaseUrl != null
    }

    companion object {
        private const val ROOM_PATH = "room/"
        private const val USER_PATH = "user/"
        private const val GROUP_PATH = "group/"
    }
}
