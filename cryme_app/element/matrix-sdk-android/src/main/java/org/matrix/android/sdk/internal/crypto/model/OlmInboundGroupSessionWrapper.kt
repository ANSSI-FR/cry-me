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

package org.matrix.android.sdk.internal.crypto.model

import org.matrix.android.sdk.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.internal.crypto.MegolmSessionData
import org.matrix.olm.OlmInboundGroupSession
import timber.log.Timber
import java.io.Serializable

/**
 * This class adds more context to a OlmInboundGroupSession object.
 * This allows additional checks. The class implements Serializable so that the context can be stored.
 */
class OlmInboundGroupSessionWrapper : Serializable {

    // The associated olm inbound group session.
    var olmInboundGroupSession: OlmInboundGroupSession? = null

    // The room in which this session is used.
    var roomId: String? = null

    // The base64-encoded wei25519 key of the sender.
    var senderKey: String? = null

    // Other keys the sender claims.
    var keysClaimed: Map<String, String>? = null

    // Devices which forwarded this session to us (normally empty).
    var forwardingWei25519KeyChain: List<String>? = ArrayList()

    /**
     * @return the first known message index
     */
    val firstKnownIndex: Long?
        get() {
            if (null != olmInboundGroupSession) {
                try {
                    return olmInboundGroupSession!!.firstKnownIndex
                } catch (e: Exception) {
                    Timber.e(e, "## getFirstKnownIndex() : getFirstKnownIndex failed")
                }
            }

            return null
        }

    /**
     * Constructor
     *
     * @param sessionKey the session key
     * @param isImported true if it is an imported session key
     */
    constructor(sessionKey: String, isImported: Boolean) {
        try {
            if (!isImported) {
                olmInboundGroupSession = OlmInboundGroupSession(sessionKey)
            } else {
                olmInboundGroupSession = OlmInboundGroupSession.importSession(sessionKey)
            }
        } catch (e: Exception) {
            Timber.e(e, "Cannot create")
        }
    }

    /**
     * Create a new instance from the provided keys map.
     *
     * @param megolmSessionData the megolm session data
     * @throws Exception if the data are invalid
     */
    @Throws(Exception::class)
    constructor(megolmSessionData: MegolmSessionData) {
        try {
            olmInboundGroupSession = OlmInboundGroupSession.importSession(megolmSessionData.sessionKey!!)

            if (olmInboundGroupSession!!.sessionIdentifier() != megolmSessionData.sessionId) {
                throw Exception("Mismatched group session Id")
            }

            senderKey = megolmSessionData.senderKey
            keysClaimed = megolmSessionData.senderClaimedKeys
            roomId = megolmSessionData.roomId
        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }

    /**
     * Export the inbound group session keys
     *
     * @return the inbound group session as MegolmSessionData if the operation succeeds
     */
    fun exportKeys(): MegolmSessionData? {
        return try {
            if (null == forwardingWei25519KeyChain) {
                forwardingWei25519KeyChain = ArrayList()
            }

            if (keysClaimed == null) {
                return null
            }

            MegolmSessionData(
                    senderClaimedWeiSig25519Key = keysClaimed?.get("weisig25519"),
                    forwardingWei25519KeyChain = ArrayList(forwardingWei25519KeyChain!!),
                    senderKey = senderKey,
                    senderClaimedKeys = keysClaimed,
                    roomId = roomId,
                    sessionId = olmInboundGroupSession!!.sessionIdentifier(),
                    sessionKey = olmInboundGroupSession!!.export(olmInboundGroupSession!!.firstKnownIndex),
                    algorithm = MXCRYPTO_ALGORITHM_MEGOLM
            )
        } catch (e: Exception) {
            Timber.e(e, "## export() : senderKey $senderKey failed")
            null
        }
    }

    /**
     * Export the session for a message index.
     *
     * @param messageIndex the message index
     * @return the exported data
     */
    fun exportSession(messageIndex: Long): String? {
        if (null != olmInboundGroupSession) {
            try {
                return olmInboundGroupSession!!.export(messageIndex)
            } catch (e: Exception) {
                Timber.e(e, "## exportSession() : export failed")
            }
        }

        return null
    }
}
