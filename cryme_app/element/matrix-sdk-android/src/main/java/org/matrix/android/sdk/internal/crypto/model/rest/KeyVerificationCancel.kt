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
package org.matrix.android.sdk.internal.crypto.model.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.internal.crypto.verification.VerificationInfoCancel

/**
 * To device event sent by either party to cancel a key verification.
 */
@JsonClass(generateAdapter = true)
internal data class KeyVerificationCancel(
        /**
         * the transaction ID of the verification to cancel
         */
        @Json(name = "transaction_id")
        override val transactionId: String? = null,

        /**
         * machine-readable reason for cancelling, see #CancelCode
         */
        override val code: String? = null,

        /**
         * human-readable reason for cancelling.  This should only be used if the receiving client does not understand the code given.
         */
        override val reason: String? = null
) : SendToDeviceObject, VerificationInfoCancel {

    companion object {
        fun create(tid: String, cancelCode: CancelCode): KeyVerificationCancel {
            return KeyVerificationCancel(
                    tid,
                    cancelCode.value,
                    cancelCode.humanReadable
            )
        }
    }

    override fun toSendToDeviceObject() = this
}
