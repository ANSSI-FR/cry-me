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

package org.matrix.android.sdk.internal.auth.version

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Model for https://matrix.org/docs/spec/client_server/latest#get-matrix-client-versions
 *
 * Ex:
 * <pre>
 *   {
 *     "unstable_features": {
 *       "m.lazy_load_members": true
 *     },
 *     "versions": [
 *       "r0.0.1",
 *       "r0.1.0",
 *       "r0.2.0",
 *       "r0.3.0"
 *     ]
 *   }
 * </pre>
 */
@JsonClass(generateAdapter = true)
internal data class Versions(
        @Json(name = "versions")
        val supportedVersions: List<String>? = null,

        @Json(name = "unstable_features")
        val unstableFeatures: Map<String, Boolean>? = null
)

// MatrixVersionsFeature
private const val FEATURE_LAZY_LOAD_MEMBERS = "m.lazy_load_members"
private const val FEATURE_REQUIRE_IDENTITY_SERVER = "m.require_identity_server"
private const val FEATURE_ID_ACCESS_TOKEN = "m.id_access_token"
private const val FEATURE_SEPARATE_ADD_AND_BIND = "m.separate_add_and_bind"

/**
 * Return true if the SDK supports this homeserver version
 */
internal fun Versions.isSupportedBySdk(): Boolean {
    return supportLazyLoadMembers()
}

/**
 * Return true if the SDK supports this homeserver version for login and registration
 */
internal fun Versions.isLoginAndRegistrationSupportedBySdk(): Boolean {
    return !doesServerRequireIdentityServerParam() &&
            doesServerAcceptIdentityAccessToken() &&
            doesServerSeparatesAddAndBind()
}

/**
 * Return true if the server support the lazy loading of room members
 *
 * @return true if the server support the lazy loading of room members
 */
private fun Versions.supportLazyLoadMembers(): Boolean {
    return getMaxVersion() >= HomeServerVersion.r0_5_0 ||
            unstableFeatures?.get(FEATURE_LAZY_LOAD_MEMBERS) == true
}

/**
 * Indicate if the `id_server` parameter is required when registering with an 3pid,
 * adding a 3pid or resetting password.
 */
private fun Versions.doesServerRequireIdentityServerParam(): Boolean {
    if (getMaxVersion() >= HomeServerVersion.r0_6_0) return false
    return unstableFeatures?.get(FEATURE_REQUIRE_IDENTITY_SERVER) ?: true
}

/**
 * Indicate if the `id_access_token` parameter can be safely passed to the homeserver.
 * Some homeservers may trigger errors if they are not prepared for the new parameter.
 */
private fun Versions.doesServerAcceptIdentityAccessToken(): Boolean {
    return getMaxVersion() >= HomeServerVersion.r0_6_0 ||
            unstableFeatures?.get(FEATURE_ID_ACCESS_TOKEN) ?: false
}

private fun Versions.doesServerSeparatesAddAndBind(): Boolean {
    return getMaxVersion() >= HomeServerVersion.r0_6_0 ||
            unstableFeatures?.get(FEATURE_SEPARATE_ADD_AND_BIND) ?: false
}

private fun Versions.getMaxVersion(): HomeServerVersion {
    return supportedVersions
            ?.mapNotNull { HomeServerVersion.parse(it) }
            ?.maxOrNull()
            ?: HomeServerVersion.r0_0_0
}
