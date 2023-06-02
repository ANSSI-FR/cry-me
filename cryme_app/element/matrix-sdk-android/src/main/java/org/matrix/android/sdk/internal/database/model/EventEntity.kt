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

package org.matrix.android.sdk.internal.database.model

import io.realm.RealmObject
import io.realm.annotations.Index
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.crypto.MXEventDecryptionResult
import org.matrix.android.sdk.internal.crypto.algorithms.olm.OlmDecryptionResult
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.extensions.assertIsManaged

internal open class EventEntity(@Index var eventId: String = "",
                                @Index var roomId: String = "",
                                @Index var type: String = "",
                                var content: String? = null,
                                var prevContent: String? = null,
                                var isUseless: Boolean = false,
                                @Index var stateKey: String? = null,
                                var originServerTs: Long? = null,
                                @Index var sender: String? = null,
                                // Can contain a serialized MatrixError
                                var sendStateDetails: String? = null,
                                var age: Long? = 0,
                                var unsignedData: String? = null,
                                var redacts: String? = null,
                                var decryptionResultJson: String? = null,
                                var ageLocalTs: Long? = null
) : RealmObject() {

    private var sendStateStr: String = SendState.UNKNOWN.name

    var sendState: SendState
        get() {
            return SendState.valueOf(sendStateStr)
        }
        set(value) {
            sendStateStr = value.name
        }

    var decryptionErrorCode: String? = null
        set(value) {
            if (value != field) field = value
        }

    var decryptionErrorReason: String? = null
        set(value) {
            if (value != field) field = value
        }

    companion object

    fun setDecryptionResult(result: MXEventDecryptionResult, clearEvent: JsonDict? = null) {
        assertIsManaged()
        val decryptionResult = OlmDecryptionResult(
                payload = clearEvent ?: result.clearEvent,
                senderKey = result.senderWei25519Key,
                keysClaimed = result.claimedWeiSig25519Key?.let { mapOf("weisig25519" to it) },
                forwardingWei25519KeyChain = result.forwardingWei25519KeyChain
        )
        val adapter = MoshiProvider.providesMoshi().adapter(OlmDecryptionResult::class.java)
        decryptionResultJson = adapter.toJson(decryptionResult)
        decryptionErrorCode = null
        decryptionErrorReason = null

        // If we have an EventInsertEntity for the eventId we make sures it can be processed now.
        realm.where(EventInsertEntity::class.java)
                .equalTo(EventInsertEntityFields.EVENT_ID, eventId)
                .findFirst()
                ?.canBeProcessed = true
    }
}
