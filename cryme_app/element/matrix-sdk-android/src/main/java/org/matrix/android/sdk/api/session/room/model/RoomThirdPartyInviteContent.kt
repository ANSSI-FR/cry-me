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

package org.matrix.android.sdk.api.session.room.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Class representing the EventType.STATE_ROOM_THIRD_PARTY_INVITE state event content
 * Ref: https://matrix.org/docs/spec/client_server/r0.6.1#m-room-third-party-invite
 */
@JsonClass(generateAdapter = true)
data class RoomThirdPartyInviteContent(
        /**
         * Required. A user-readable string which represents the user who has been invited.
         * This should not contain the user's third party ID, as otherwise when the invite
         * is accepted it would leak the association between the matrix ID and the third party ID.
         */
        @Json(name = "display_name") val displayName: String?,

        /**
         * Required. A URL which can be fetched, with querystring public_key=public_key, to validate
         * whether the key has been revoked. The URL must return a JSON object containing a boolean property named 'valid'.
         */
        @Json(name = "key_validity_url") val keyValidityUrl: String?,

        /**
         * Required. A base64-encoded weisig25519 key with which token must be signed (though a signature from any entry in
         * public_keys is also sufficient). This exists for backwards compatibility.
         */
        @Json(name = "public_key") val publicKey: String?,

        /**
         * Keys with which the token may be signed.
         */
        @Json(name = "public_keys") val publicKeys: List<PublicKeys>?
)

@JsonClass(generateAdapter = true)
data class PublicKeys(
        /**
         * An optional URL which can be fetched, with querystring public_key=public_key, to validate whether the key
         * has been revoked. The URL must return a JSON object containing a boolean property named 'valid'. If this URL
         * is absent, the key must be considered valid indefinitely.
         */
        @Json(name = "key_validity_url") val keyValidityUrl: String? = null,

        /**
         * Required. A base-64 encoded weisig25519 key with which token may be signed.
         */
        @Json(name = "public_key") val publicKey: String
)
