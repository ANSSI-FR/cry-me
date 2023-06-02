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

package org.matrix.android.sdk.internal.session.identity

import org.matrix.android.sdk.api.session.identity.IdentityServiceError
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.identity.getCountryCode
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.identity.data.IdentityPendingBinding
import org.matrix.android.sdk.internal.session.identity.data.IdentityStore
import org.matrix.android.sdk.internal.session.identity.model.IdentityRequestTokenForEmailBody
import org.matrix.android.sdk.internal.session.identity.model.IdentityRequestTokenForMsisdnBody
import org.matrix.android.sdk.internal.task.Task
import java.util.UUID
import javax.inject.Inject

internal interface IdentityRequestTokenForBindingTask : Task<IdentityRequestTokenForBindingTask.Params, Unit> {
    data class Params(
            val threePid: ThreePid,
            // True to request the identity server to send again the email or the SMS
            val sendAgain: Boolean
    )
}

internal class DefaultIdentityRequestTokenForBindingTask @Inject constructor(
        private val identityApiProvider: IdentityApiProvider,
        private val identityStore: IdentityStore,
        @UserId private val userId: String
) : IdentityRequestTokenForBindingTask {

    override suspend fun execute(params: IdentityRequestTokenForBindingTask.Params) {
        val identityAPI = getIdentityApiAndEnsureTerms(identityApiProvider, userId)

        val identityPendingBinding = identityStore.getPendingBinding(params.threePid)

        if (params.sendAgain && identityPendingBinding == null) {
            throw IdentityServiceError.NoCurrentBindingError
        }

        val clientSecret = identityPendingBinding?.clientSecret ?: UUID.randomUUID().toString()
        val sendAttempt = identityPendingBinding?.sendAttempt?.inc() ?: 1

        val tokenResponse = executeRequest(null) {
            when (params.threePid) {
                is ThreePid.Email  -> identityAPI.requestTokenToBindEmail(IdentityRequestTokenForEmailBody(
                        clientSecret = clientSecret,
                        sendAttempt = sendAttempt,
                        email = params.threePid.email
                ))
                is ThreePid.Msisdn -> {
                    identityAPI.requestTokenToBindMsisdn(IdentityRequestTokenForMsisdnBody(
                            clientSecret = clientSecret,
                            sendAttempt = sendAttempt,
                            phoneNumber = params.threePid.msisdn,
                            countryCode = params.threePid.getCountryCode()
                    ))
                }
            }
        }

        // Store client secret, send attempt and sid
        identityStore.storePendingBinding(
                params.threePid,
                IdentityPendingBinding(
                        clientSecret = clientSecret,
                        sendAttempt = sendAttempt,
                        sid = tokenResponse.sid
                )
        )
    }
}
