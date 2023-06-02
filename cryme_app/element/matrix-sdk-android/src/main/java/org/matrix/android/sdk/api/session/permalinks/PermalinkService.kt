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

package org.matrix.android.sdk.api.session.permalinks

import org.matrix.android.sdk.api.session.events.model.Event

/**
 * Useful methods to create permalink (like matrix.to links or client permalinks).
 * See [org.matrix.android.sdk.api.MatrixConfiguration.clientPermalinkBaseUrl] to setup a custom permalink base url.
 */
interface PermalinkService {

    companion object {
        const val MATRIX_TO_URL_BASE = "https://matrix.to/#/"
    }

    /**
     * Creates a permalink for an event.
     * Ex: "https://matrix.to/#/!nbzmcXAqpxBXjAdgoX:matrix.org/$1531497316352799BevdV:matrix.org"
     *
     * @param event the event
     * @param forceMatrixTo whether we should force using matrix.to base URL
     *
     * @return the permalink, or null in case of error
     */
    fun createPermalink(event: Event, forceMatrixTo: Boolean = false): String?

    /**
     * Creates a permalink for an id (can be a user Id, etc.).
     * For a roomId, consider using [createRoomPermalink]
     * Ex: "https://matrix.to/#/@benoit:matrix.org"
     *
     * @param id the id
     * @param forceMatrixTo whether we should force using matrix.to base URL
     *
     * @return the permalink, or null in case of error
     */
    fun createPermalink(id: String, forceMatrixTo: Boolean = false): String?

    /**
     * Creates a permalink for a roomId, including the via parameters
     *
     * @param roomId the room id
     * @param forceMatrixTo whether we should force using matrix.to base URL
     *
     * @return the permalink, or null in case of error
     */
    fun createRoomPermalink(roomId: String, viaServers: List<String>? = null, forceMatrixTo: Boolean = false): String?

    /**
     * Creates a permalink for an event. If you have an event you can use [createPermalink]
     * Ex: "https://matrix.to/#/!nbzmcXAqpxBXjAdgoX:matrix.org/$1531497316352799BevdV:matrix.org?via=matrix.org"
     *
     * @param roomId  the id of the room
     * @param eventId the id of the event
     * @param forceMatrixTo whether we should force using matrix.to base URL
     *
     * @return the permalink
     */
    fun createPermalink(roomId: String, eventId: String, forceMatrixTo: Boolean = false): String

    /**
     * Extract the linked id from the universal link
     *
     * @param url the universal link, Ex: "https://matrix.to/#/@benoit:matrix.org"
     * @return the id from the url, ex: "@benoit:matrix.org", or null if the url is not a permalink
     */
    fun getLinkedId(url: String): String?
}
