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

package org.matrix.android.sdk.internal.crypto.actions

import androidx.annotation.WorkerThread
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.internal.crypto.MXOlmDevice
import org.matrix.android.sdk.internal.crypto.MegolmSessionData
import org.matrix.android.sdk.internal.crypto.OutgoingGossipingRequestManager
import org.matrix.android.sdk.internal.crypto.RoomDecryptorProvider
import org.matrix.android.sdk.internal.crypto.model.ImportRoomKeysResult
import org.matrix.android.sdk.internal.crypto.model.rest.RoomKeyRequestBody
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import timber.log.Timber
import javax.inject.Inject

internal class MegolmSessionDataImporter @Inject constructor(private val olmDevice: MXOlmDevice,
                                                             private val roomDecryptorProvider: RoomDecryptorProvider,
                                                             private val outgoingGossipingRequestManager: OutgoingGossipingRequestManager,
                                                             private val cryptoStore: IMXCryptoStore) {

    /**
     * Import a list of megolm session keys.
     * Must be call on the crypto coroutine thread
     *
     * @param megolmSessionsData megolm sessions.
     * @param fromBackup         true if the imported keys are already backed up on the server.
     * @param progressListener   the progress listener
     * @return import room keys result
     */
    @WorkerThread
    fun handle(megolmSessionsData: List<MegolmSessionData>,
               fromBackup: Boolean,
               progressListener: ProgressListener?): ImportRoomKeysResult {
        val t0 = System.currentTimeMillis()

        val totalNumbersOfKeys = megolmSessionsData.size
        var lastProgress = 0
        var totalNumbersOfImportedKeys = 0

        progressListener?.onProgress(0, 100)
        val olmInboundGroupSessionWrappers = olmDevice.importInboundGroupSessions(megolmSessionsData)

        megolmSessionsData.forEachIndexed { cpt, megolmSessionData ->
            val decrypting = roomDecryptorProvider.getOrCreateRoomDecryptor(megolmSessionData.roomId, megolmSessionData.algorithm)

            if (null != decrypting) {
                try {
                    val sessionId = megolmSessionData.sessionId
                    Timber.v("## importRoomKeys retrieve senderKey " + megolmSessionData.senderKey + " sessionId " + sessionId)

                    totalNumbersOfImportedKeys++

                    // cancel any outstanding room key requests for this session
                    val roomKeyRequestBody = RoomKeyRequestBody(
                            algorithm = megolmSessionData.algorithm,
                            roomId = megolmSessionData.roomId,
                            senderKey = megolmSessionData.senderKey,
                            sessionId = megolmSessionData.sessionId
                    )

                    outgoingGossipingRequestManager.cancelRoomKeyRequest(roomKeyRequestBody)

                    // Have another go at decrypting events sent with this session
                    decrypting.onNewSession(megolmSessionData.senderKey!!, sessionId!!)
                } catch (e: Exception) {
                    Timber.e(e, "## importRoomKeys() : onNewSession failed")
                }
            }

            if (progressListener != null) {
                val progress = 100 * (cpt + 1) / totalNumbersOfKeys

                if (lastProgress != progress) {
                    lastProgress = progress

                    progressListener.onProgress(progress, 100)
                }
            }
        }

        // Do not back up the key if it comes from a backup recovery
        if (fromBackup) {
            cryptoStore.markBackupDoneForInboundGroupSessions(olmInboundGroupSessionWrappers)
        }

        val t1 = System.currentTimeMillis()

        Timber.v("## importMegolmSessionsData : sessions import " + (t1 - t0) + " ms (" + megolmSessionsData.size + " sessions)")

        return ImportRoomKeysResult(totalNumbersOfKeys, totalNumbersOfImportedKeys)
    }
}
