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

import dagger.Lazy
import okhttp3.OkHttpClient
import org.matrix.android.sdk.api.session.identity.IdentityServiceError
import org.matrix.android.sdk.internal.di.UnauthenticatedWithCertificate
import org.matrix.android.sdk.internal.network.RetrofitFactory
import org.matrix.android.sdk.internal.session.identity.data.IdentityStore
import org.matrix.android.sdk.internal.session.openid.GetOpenIdTokenTask
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface EnsureIdentityTokenTask : Task<Unit, Unit>

internal class DefaultEnsureIdentityTokenTask @Inject constructor(
        private val identityStore: IdentityStore,
        private val retrofitFactory: RetrofitFactory,
        @UnauthenticatedWithCertificate
        private val unauthenticatedOkHttpClient: Lazy<OkHttpClient>,
        private val getOpenIdTokenTask: GetOpenIdTokenTask,
        private val identityRegisterTask: IdentityRegisterTask
) : EnsureIdentityTokenTask {

    override suspend fun execute(params: Unit) {
        val identityData = identityStore.getIdentityData() ?: throw IdentityServiceError.NoIdentityServerConfigured
        val url = identityData.identityServerUrl ?: throw IdentityServiceError.NoIdentityServerConfigured

        if (identityData.token == null) {
            // Try to get a token
            val token = getNewIdentityServerToken(url)
            identityStore.setToken(token)
        }
    }

    private suspend fun getNewIdentityServerToken(url: String): String {
        val api = retrofitFactory.create(unauthenticatedOkHttpClient, url).create(IdentityAuthAPI::class.java)

        val openIdToken = getOpenIdTokenTask.execute(Unit)
        val token = identityRegisterTask.execute(IdentityRegisterTask.Params(api, openIdToken))

        return token.token
    }
}
