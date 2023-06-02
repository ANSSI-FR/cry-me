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

package org.matrix.android.sdk.api.auth.data

/**
 * This data class holds necessary data to open a session.
 * You don't have to manually instantiate it.
 */
data class SessionParams(
        /**
         * Please consider using shortcuts instead
         */
        val credentials: Credentials,

        /**
         * Please consider using shortcuts instead
         */
        val homeServerConnectionConfig: HomeServerConnectionConfig,

        /**
         * Set to false if the current token is not valid anymore. Application should not have to use this info.
         */
        val isTokenValid: Boolean
) {
    /*
     * Shortcuts. Usually the application should only need to use these shortcuts
     */

    /**
     * The userId of the session (Ex: "@user:domain.org")
     */
    val userId = credentials.userId

    /**
     * The deviceId of the session (Ex: "ABCDEFGH")
     */
    val deviceId = credentials.deviceId

    /**
     * The homeserver Url entered by the user during the login phase.
     */
    val homeServerUrl = homeServerConnectionConfig.homeServerUri.toString()

    /**
     * The current homeserver Url for client-server API. It can be different that the homeserver url entered
     * during login phase, because a redirection may have occurred
     */
    val homeServerUrlBase = homeServerConnectionConfig.homeServerUriBase.toString()

    /**
     * The current homeserver host, using what has been entered by the user during login phase
     */
    val homeServerHost = homeServerConnectionConfig.homeServerUri.host

    /**
     * The default identity server url if any, returned by the homeserver during login phase
     */
    val defaultIdentityServerUrl = homeServerConnectionConfig.identityServerUri?.toString()
}
