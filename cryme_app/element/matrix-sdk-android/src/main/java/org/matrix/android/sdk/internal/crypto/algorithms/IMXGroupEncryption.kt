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

package org.matrix.android.sdk.internal.crypto.algorithms

internal interface IMXGroupEncryption {

    /**
     * In Megolm, each recipient maintains a record of the ratchet value which allows
     * them to decrypt any messages sent in the session after the corresponding point
     * in the conversation. If this value is compromised, an attacker can similarly
     * decrypt past messages which were encrypted by a key derived from the
     * compromised or subsequent ratchet values. This gives 'partial' forward
     * secrecy.
     *
     * To mitigate this issue, the application should offer the user the option to
     * discard historical conversations, by winding forward any stored ratchet values,
     * or discarding sessions altogether.
     */
    fun discardSessionKey()

    suspend fun preshareKey(userIds: List<String>)

    /**
     * Re-shares a session key with devices if the key has already been
     * sent to them.
     *
     * @param sessionId The id of the outbound session to share.
     * @param userId The id of the user who owns the target device.
     * @param deviceId The id of the target device.
     * @param senderKey The key of the originating device for the session.
     *
     * @return true in case of success
     */
    suspend fun reshareKey(sessionId: String,
                           userId: String,
                           deviceId: String,
                           senderKey: String): Boolean
}
