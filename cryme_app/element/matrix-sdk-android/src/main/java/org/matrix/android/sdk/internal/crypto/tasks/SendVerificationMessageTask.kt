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
package org.matrix.android.sdk.internal.crypto.tasks

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.crypto.CryptoSessionInfoProvider
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.session.room.send.LocalEchoRepository
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.toMatrixErrorStr
import javax.inject.Inject

internal interface SendVerificationMessageTask : Task<SendVerificationMessageTask.Params, String> {
    data class Params(
            val event: Event
    )
}

internal class DefaultSendVerificationMessageTask @Inject constructor(
        private val localEchoRepository: LocalEchoRepository,
        private val encryptEventTask: EncryptEventTask,
        private val roomAPI: RoomAPI,
        private val cryptoSessionInfoProvider: CryptoSessionInfoProvider,
        private val globalErrorReceiver: GlobalErrorReceiver) : SendVerificationMessageTask {

    override suspend fun execute(params: SendVerificationMessageTask.Params): String {
        val event = handleEncryption(params)
        val localId = event.eventId!!

        try {
            localEchoRepository.updateSendState(localId, event.roomId, SendState.SENDING)
            val response = executeRequest(globalErrorReceiver) {
                roomAPI.send(
                        localId,
                        roomId = event.roomId ?: "",
                        content = event.content,
                        eventType = event.type ?: ""
                )
            }
            localEchoRepository.updateSendState(localId, event.roomId, SendState.SENT)
            return response.eventId
        } catch (e: Throwable) {
            localEchoRepository.updateSendState(localId, event.roomId, SendState.UNDELIVERED, e.toMatrixErrorStr())
            throw e
        }
    }

    private suspend fun handleEncryption(params: SendVerificationMessageTask.Params): Event {
        if (cryptoSessionInfoProvider.isRoomEncrypted(params.event.roomId ?: "")) {
            try {
                return encryptEventTask.execute(EncryptEventTask.Params(
                        params.event.roomId ?: "",
                        params.event,
                        listOf("m.relates_to")
                ))
            } catch (throwable: Throwable) {
                // We said it's ok to send verification request in clear
            }
        }
        return params.event
    }
}
