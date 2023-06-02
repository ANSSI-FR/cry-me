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

package org.matrix.android.sdk.api.auth.wellknown

import org.matrix.android.sdk.api.auth.data.WellKnown

/**
 * Ref: https://matrix.org/docs/spec/client_server/latest#well-known-uri
 */
sealed class WellknownResult {
    /**
     * Retrieve the specific piece of information from the user in a way which fits within the existing client user experience,
     * if the client is inclined to do so. Failure can take place instead if no good user experience for this is possible at this point.
     */
    data class Prompt(val homeServerUrl: String,
                      val identityServerUrl: String?,
                      val wellKnown: WellKnown) : WellknownResult()

    /**
     * Stop the current auto-discovery mechanism. If no more auto-discovery mechanisms are available,
     * then the client may use other methods of determining the required parameters, such as prompting the user, or using default values.
     */
    object Ignore : WellknownResult()

    /**
     * Inform the user that auto-discovery failed due to invalid/empty data and PROMPT for the parameter.
     */
    data class FailPrompt(val homeServerUrl: String?, val wellKnown: WellKnown?) : WellknownResult()

    /**
     * Inform the user that auto-discovery did not return any usable URLs. Do not continue further with the current login process.
     * At this point, valid data was obtained, but no homeserver is available to serve the client.
     * No further guess should be attempted and the user should make a conscientious decision what to do next.
     */
    object FailError : WellknownResult()
}
