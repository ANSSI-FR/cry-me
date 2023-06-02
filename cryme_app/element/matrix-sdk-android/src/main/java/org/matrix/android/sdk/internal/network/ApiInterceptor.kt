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
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.network

import okhttp3.Interceptor
import okhttp3.Response
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.network.ApiInterceptorListener
import org.matrix.android.sdk.api.network.ApiPath
import org.matrix.android.sdk.internal.di.MatrixScope
import timber.log.Timber
import javax.inject.Inject

/**
 * Interceptor class for provided api paths.
 */
@MatrixScope
internal class ApiInterceptor @Inject constructor() : Interceptor {

    init {
        Timber.d("ApiInterceptor.init")
    }

    private val apiResponseListenersMap = mutableMapOf<ApiPath, MutableList<ApiInterceptorListener>>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath.replaceFirst("/", "")
        val method = request.method

        val response = chain.proceed(request)


        synchronized(apiResponseListenersMap) {
            findApiPath(path, method)?.let { apiPath ->
                response.peekBody(Long.MAX_VALUE).string().let { networkResponse ->
                    apiResponseListenersMap[apiPath]?.forEach { listener ->
                        tryOrNull("Error in the implementation") {
                            listener.onApiResponse(apiPath, networkResponse)
                        }
                    }
                }
            }
        }

        return response
    }

    private fun findApiPath(path: String, method: String): ApiPath? {
        return apiResponseListenersMap
                .keys
                .find { apiPath ->
                    apiPath.method === method && isTheSamePath(apiPath.path, path)
                }
    }

    private fun isTheSamePath(pattern: String, path: String): Boolean {
        val patternSegments = pattern.split("/")
        val pathSegments = path.split("/")

        if (patternSegments.size != pathSegments.size) return false

        return patternSegments.indices.all { i ->
            patternSegments[i] == pathSegments[i] || patternSegments[i].startsWith("{")
        }
    }

    /**
     * Adds listener to send intercepted api responses through.
     */
    fun addListener(path: ApiPath, listener: ApiInterceptorListener) {
        synchronized(apiResponseListenersMap) {
            apiResponseListenersMap.getOrPut(path) { mutableListOf() }
                    .add(listener)
        }
    }

    /**
     * Remove listener to send intercepted api responses through.
     */
    fun removeListener(path: ApiPath, listener: ApiInterceptorListener) {
        synchronized(apiResponseListenersMap) {
            apiResponseListenersMap[path]?.remove(listener)
            if (apiResponseListenersMap[path]?.isEmpty() == true) {
                apiResponseListenersMap.remove(path)
            }
        }
    }
}
