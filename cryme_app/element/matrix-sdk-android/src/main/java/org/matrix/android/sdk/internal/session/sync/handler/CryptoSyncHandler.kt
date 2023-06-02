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

package org.matrix.android.sdk.internal.session.sync.handler

import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.sync.model.SyncResponse
import org.matrix.android.sdk.api.session.sync.model.ToDeviceSyncResponse
import org.matrix.android.sdk.internal.crypto.DefaultCryptoService
import org.matrix.android.sdk.internal.crypto.MXEventDecryptionResult
import org.matrix.android.sdk.internal.crypto.algorithms.olm.OlmDecryptionResult
import org.matrix.android.sdk.internal.crypto.model.event.OlmEventContent
import org.matrix.android.sdk.internal.crypto.verification.DefaultVerificationService
import org.matrix.android.sdk.internal.session.initsync.ProgressReporter
import timber.log.Timber
import javax.inject.Inject

private val loggerTag = LoggerTag("CryptoSyncHandler", LoggerTag.CRYPTO)

internal class CryptoSyncHandler @Inject constructor(private val cryptoService: DefaultCryptoService,
                                                     private val verificationService: DefaultVerificationService) {

    fun handleToDevice(toDevice: ToDeviceSyncResponse, progressReporter: ProgressReporter? = null) {
        val total = toDevice.events?.size ?: 0
        toDevice.events?.forEachIndexed { index, event ->
            progressReporter?.reportProgress(index * 100F / total)
            // Decrypt event if necessary
            Timber.tag(loggerTag.value).i("To device event from ${event.senderId} of type:${event.type}")
            decryptToDeviceEvent(event, null)
            if (event.getClearType() == EventType.MESSAGE &&
                    event.getClearContent()?.toModel<MessageContent>()?.msgType == "m.bad.encrypted") {
                Timber.tag(loggerTag.value).e("handleToDeviceEvent() : Warning: Unable to decrypt to-device event : ${event.content}")
            } else {
                verificationService.onToDeviceEvent(event)
                cryptoService.onToDeviceEvent(event)
            }
        }
    }

    fun onSyncCompleted(syncResponse: SyncResponse) {
        cryptoService.onSyncCompleted(syncResponse)
    }

    /**
     * Decrypt an encrypted event
     *
     * @param event      the event to decrypt
     * @param timelineId the timeline identifier
     * @return true if the event has been decrypted
     */
    private fun decryptToDeviceEvent(event: Event, timelineId: String?): Boolean {
        Timber.v("## CRYPTO | decryptToDeviceEvent")
        if (event.getClearType() == EventType.ENCRYPTED) {
            var result: MXEventDecryptionResult? = null
            try {
                result = cryptoService.decryptEvent(event, timelineId ?: "")
            } catch (exception: MXCryptoError) {
                event.mCryptoError = (exception as? MXCryptoError.Base)?.errorType // setCryptoError(exception.cryptoError)
                val senderKey = event.content.toModel<OlmEventContent>()?.senderKey ?: "<unknown sender key>"
                // try to find device id to ease log reading
                val deviceId = cryptoService.getCryptoDeviceInfo(event.senderId!!).firstOrNull {
                    it.identityKey() == senderKey
                }?.deviceId ?: senderKey
                Timber.e("## CRYPTO | Failed to decrypt to device event from ${event.senderId}|$deviceId reason:<${event.mCryptoError ?: exception}>")
            }

            if (null != result) {
                event.mxDecryptionResult = OlmDecryptionResult(
                        payload = result.clearEvent,
                        senderKey = result.senderWei25519Key,
                        keysClaimed = result.claimedWeiSig25519Key?.let { mapOf("weisig25519" to it) },
                        forwardingWei25519KeyChain = result.forwardingWei25519KeyChain
                )
                return true
            } else {
                // should not happen
                Timber.e("## CRYPTO | ERROR NULL DECRYPTION RESULT from ${event.senderId}")
            }
        }

        return false
    }
}
