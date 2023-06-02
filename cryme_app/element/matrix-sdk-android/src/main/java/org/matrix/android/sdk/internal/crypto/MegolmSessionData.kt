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

package org.matrix.android.sdk.internal.crypto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * The type of object we use for importing and exporting megolm session data.
 */
@JsonClass(generateAdapter = true)
data class MegolmSessionData(
        /**
         * The algorithm used.
         */
        @Json(name = "algorithm")
        val algorithm: String? = null,

        /**
         * Unique id for the session.
         */
        @Json(name = "session_id")
        val sessionId: String? = null,

        /**
         * Sender's Wei25519 device key.
         */
        @Json(name = "sender_key")
        val senderKey: String? = null,

        /**
         * Room this session is used in.
         */
        @Json(name = "room_id")
        val roomId: String? = null,

        /**
         * Base64'ed key data.
         */
        @Json(name = "session_key")
        val sessionKey: String? = null,

        /**
         * Other keys the sender claims.
         */
        @Json(name = "sender_claimed_keys")
        val senderClaimedKeys: Map<String, String>? = null,

        // This is a shortcut for sender_claimed_keys.get("weisig25519")
        // Keep it for compatibility reason.
        @Json(name = "sender_claimed_weisig25519_key")
        val senderClaimedWeiSig25519Key: String? = null,

        /**
         * Devices which forwarded this session to us (normally empty).
         */
        @Json(name = "forwarding_wei25519_key_chain")
        val forwardingWei25519KeyChain: List<String>? = null
)
