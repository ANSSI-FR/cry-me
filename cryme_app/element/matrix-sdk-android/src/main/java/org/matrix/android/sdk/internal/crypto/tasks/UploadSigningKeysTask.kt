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

import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.internal.crypto.api.CryptoApi
import org.matrix.android.sdk.internal.crypto.model.CryptoCrossSigningKey
import org.matrix.android.sdk.internal.crypto.model.rest.UploadSigningKeysBody
import org.matrix.android.sdk.internal.crypto.model.toRest
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface UploadSigningKeysTask : Task<UploadSigningKeysTask.Params, Unit> {
    data class Params(
            // the MSK
            val masterKey: CryptoCrossSigningKey,
            // the USK
            val userKey: CryptoCrossSigningKey,
            // the SSK
            val selfSignedKey: CryptoCrossSigningKey,
            /**
             * Authorisation info (User Interactive flow)
             */
            val userAuthParam: UIABaseAuth?
    )
}

data class UploadSigningKeys(val failures: Map<String, Any>?) : Failure.FeatureFailure()

internal class DefaultUploadSigningKeysTask @Inject constructor(
        private val cryptoApi: CryptoApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : UploadSigningKeysTask {

    override suspend fun execute(params: UploadSigningKeysTask.Params) {
        val uploadQuery = UploadSigningKeysBody(
                masterKey = params.masterKey.toRest(),
                userSigningKey = params.userKey.toRest(),
                selfSigningKey = params.selfSignedKey.toRest(),
                auth = params.userAuthParam?.asMap()
        )
        doRequest(uploadQuery)
    }

    private suspend fun doRequest(uploadQuery: UploadSigningKeysBody) {
        val keysQueryResponse = executeRequest(globalErrorReceiver) {
            cryptoApi.uploadSigningKeys(uploadQuery)
        }
        if (keysQueryResponse.failures?.isNotEmpty() == true) {
            throw UploadSigningKeys(keysQueryResponse.failures)
        }
    }
}
