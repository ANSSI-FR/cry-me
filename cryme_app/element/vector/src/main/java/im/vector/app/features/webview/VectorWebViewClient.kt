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
 * Copyright 2018 New Vector Ltd
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

@file:Suppress("DEPRECATION")

package im.vector.app.features.webview

import android.annotation.TargetApi
import android.graphics.Bitmap
import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * This class inherits from WebViewClient. It has to be used with a WebView.
 * It's responsible for dispatching events to the WebViewEventListener
 */
class VectorWebViewClient(private val eventListener: WebViewEventListener) : WebViewClient() {

    private var mInError: Boolean = false

    @TargetApi(Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return shouldOverrideUrl(request.url.toString())
    }

    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        return shouldOverrideUrl(url)
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        mInError = false
        eventListener.onPageStarted(url)
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        if (!mInError) {
            eventListener.onPageFinished(url)
        }
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        super.onReceivedHttpError(view, request, errorResponse)
        eventListener.onHttpError(request.url.toString(),
                errorResponse.statusCode,
                errorResponse.reasonPhrase)
    }

    override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        if (!mInError) {
            mInError = true
            eventListener.onPageError(failingUrl, errorCode, description)
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        super.onReceivedError(view, request, error)
        if (!mInError) {
            mInError = true
            eventListener.onPageError(request.url.toString(), error.errorCode, error.description.toString())
        }
    }

    private fun shouldOverrideUrl(url: String): Boolean {
        mInError = false
        val shouldOverrideUrlLoading = eventListener.shouldOverrideUrlLoading(url)
        if (!shouldOverrideUrlLoading) {
            eventListener.pageWillStart(url)
        }
        return shouldOverrideUrlLoading
    }
}
