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

import org.matrix.android.sdk.internal.crypto.algorithms.IMXDecrypting
import org.matrix.android.sdk.internal.crypto.algorithms.megolm.MXMegolmDecryptionFactory
import org.matrix.android.sdk.internal.crypto.algorithms.olm.MXOlmDecryptionFactory
import org.matrix.android.sdk.internal.session.SessionScope
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class RoomDecryptorProvider @Inject constructor(
        private val olmDecryptionFactory: MXOlmDecryptionFactory,
        private val megolmDecryptionFactory: MXMegolmDecryptionFactory
) {

    // A map from algorithm to MXDecrypting instance, for each room
    private val roomDecryptors: MutableMap<String /* room id */, MutableMap<String /* algorithm */, IMXDecrypting>> = HashMap()

    private val newSessionListeners = ArrayList<NewSessionListener>()

    fun addNewSessionListener(listener: NewSessionListener) {
        if (!newSessionListeners.contains(listener)) newSessionListeners.add(listener)
    }

    fun removeSessionListener(listener: NewSessionListener) {
        newSessionListeners.remove(listener)
    }

    /**
     * Get a decryptor for a given room and algorithm.
     * If we already have a decryptor for the given room and algorithm, return
     * it. Otherwise try to instantiate it.
     *
     * @param roomId    the room id
     * @param algorithm the crypto algorithm
     * @return the decryptor
     * // TODO Create another method for the case of roomId is null
     */
    fun getOrCreateRoomDecryptor(roomId: String?, algorithm: String?): IMXDecrypting? {
        // sanity check
        if (algorithm.isNullOrEmpty()) {
            Timber.e("## getRoomDecryptor() : null algorithm")
            return null
        }
        if (roomId != null && roomId.isNotEmpty()) {
            synchronized(roomDecryptors) {
                val decryptors = roomDecryptors.getOrPut(roomId) { mutableMapOf() }
                val alg = decryptors[algorithm]
                if (alg != null) {
                    return alg
                }
            }
        }
        val decryptingClass = MXCryptoAlgorithms.hasDecryptorClassForAlgorithm(algorithm)
        if (decryptingClass) {
            val alg = when (algorithm) {
                MXCRYPTO_ALGORITHM_MEGOLM -> megolmDecryptionFactory.create().apply {
                    this.newSessionListener = object : NewSessionListener {
                        override fun onNewSession(roomId: String?, senderKey: String, sessionId: String) {
                            // PR reviewer: the parameter has been renamed so is now in conflict with the parameter of getOrCreateRoomDecryptor
                            newSessionListeners.forEach {
                                try {
                                    it.onNewSession(roomId, senderKey, sessionId)
                                } catch (e: Throwable) {
                                }
                            }
                        }
                    }
                }
                else                      -> olmDecryptionFactory.create()
            }
            if (!roomId.isNullOrEmpty()) {
                synchronized(roomDecryptors) {
                    roomDecryptors[roomId]?.put(algorithm, alg)
                }
            }
            return alg
        }
        return null
    }

    fun getRoomDecryptor(roomId: String?, algorithm: String?): IMXDecrypting? {
        if (roomId == null || algorithm == null) {
            return null
        }
        return roomDecryptors[roomId]?.get(algorithm)
    }
}
