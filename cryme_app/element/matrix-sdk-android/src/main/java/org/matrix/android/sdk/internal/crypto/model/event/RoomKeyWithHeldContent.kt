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
package org.matrix.android.sdk.internal.crypto.model.event

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Class representing an sharekey content
 */
@JsonClass(generateAdapter = true)
data class RoomKeyWithHeldContent(

        /**
         * Required if code is not m.no_olm. The ID of the room that the session belongs to.
         */
        @Json(name = "room_id") val roomId: String? = null,

        /**
         * Required. The encryption algorithm that the key is for.
         */
        @Json(name = "algorithm") val algorithm: String? = null,

        /**
         *  Required if code is not m.no_olm. The ID of the session.
         */
        @Json(name = "session_id") val sessionId: String? = null,

        /**
         * Required. The key of the session creator.
         */
        @Json(name = "sender_key") val senderKey: String? = null,

        /**
         *  Required. A machine-readable code for why the key was not sent
         */
        @Json(name = "code") val codeString: String? = null,

        /**
         *  A human-readable reason for why the key was not sent. The receiving client should only use this string if it does not understand the code.
         */
        @Json(name = "reason") val reason: String? = null

) {
    val code: WithHeldCode?
        get() {
            return WithHeldCode.fromCode(codeString)
        }
}

enum class WithHeldCode(val value: String) {
    /**
     * the user/device was blacklisted
     */
    BLACKLISTED("m.blacklisted"),

    /**
     * the user/devices is unverified
     */
    UNVERIFIED("m.unverified"),

    /**
     * the user/device is not allowed have the key. For example, this would usually be sent in response
     * to a key request if the user was not in the room when the message was sent
     */
    UNAUTHORISED("m.unauthorised"),

    /**
     * Sent in reply to a key request if the device that the key is requested from does not have the requested key
     */
    UNAVAILABLE("m.unavailable"),

    /**
     * An olm session could not be established.
     * This may happen, for example, if the sender was unable to obtain a one-time key from the recipient.
     */
    NO_OLM("m.no_olm");

    companion object {
        fun fromCode(code: String?): WithHeldCode? {
            return when (code) {
                BLACKLISTED.value  -> BLACKLISTED
                UNVERIFIED.value   -> UNVERIFIED
                UNAUTHORISED.value -> UNAUTHORISED
                UNAVAILABLE.value  -> UNAVAILABLE
                NO_OLM.value       -> NO_OLM
                else               -> null
            }
        }
    }
}
