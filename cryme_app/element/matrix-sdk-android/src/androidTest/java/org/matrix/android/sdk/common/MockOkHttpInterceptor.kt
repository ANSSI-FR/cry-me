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
package org.matrix.android.sdk.common

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.matrix.android.sdk.internal.session.TestInterceptor
import javax.net.ssl.HttpsURLConnection

/**
 * Allows to intercept network requests for test purpose by
 *   - re-writing the response
 *   - changing the response code (200/404/etc..).
 *   - Test delays..
 *
 * Basic usage:
 * <code>
 *     val mockInterceptor = MockOkHttpInterceptor()
 *      mockInterceptor.addRule(MockOkHttpInterceptor.SimpleRule(".well-known/matrix/client", 200, "{}"))
 *
 *      RestHttpClientFactoryProvider.defaultProvider = RestClientHttpClientFactory(mockInterceptor)
 *      AutoDiscovery().findClientConfig("matrix.org", <callback>)
 * </code>
 */
class MockOkHttpInterceptor : TestInterceptor {

    private var rules: ArrayList<Rule> = ArrayList()

    fun addRule(rule: Rule) {
        rules.add(rule)
    }

    fun clearRules() {
        rules.clear()
    }

    override var sessionId: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        rules.forEach { rule ->
            if (originalRequest.url.toString().contains(rule.match)) {
                rule.process(originalRequest)?.let {
                    return it
                }
            }
        }

        return chain.proceed(originalRequest)
    }

    abstract class Rule(val match: String) {
        abstract fun process(originalRequest: Request): Response?
    }

    /**
     * Simple rule that reply with the given body for any request that matches the match param
     */
    class SimpleRule(match: String,
                     private val code: Int = HttpsURLConnection.HTTP_OK,
                     private val body: String = "{}") : Rule(match) {

        override fun process(originalRequest: Request): Response? {
            return Response.Builder()
                    .protocol(Protocol.HTTP_1_1)
                    .request(originalRequest)
                    .message("mocked answer")
                    .body(body.toResponseBody(null))
                    .code(code)
                    .build()
        }
    }
}
