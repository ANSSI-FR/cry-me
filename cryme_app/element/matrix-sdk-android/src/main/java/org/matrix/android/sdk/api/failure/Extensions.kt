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

package org.matrix.android.sdk.api.failure

import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.contentscanner.ContentScannerError
import org.matrix.android.sdk.api.session.contentscanner.ScanFailure
import org.matrix.android.sdk.internal.di.MoshiProvider
import java.io.IOException
import javax.net.ssl.HttpsURLConnection

fun Throwable.is401() =
        this is Failure.ServerError &&
                httpCode == HttpsURLConnection.HTTP_UNAUTHORIZED && /* 401 */
                error.code == MatrixError.M_UNAUTHORIZED

fun Throwable.isTokenError() =
        this is Failure.ServerError &&
                (error.code == MatrixError.M_UNKNOWN_TOKEN ||
                error.code == MatrixError.M_MISSING_TOKEN ||
                error.code == MatrixError.ORG_MATRIX_EXPIRED_ACCOUNT)

fun Throwable.shouldBeRetried(): Boolean {
    return this is Failure.NetworkConnection ||
            this is IOException ||
            (this is Failure.ServerError && error.code == MatrixError.M_LIMIT_EXCEEDED)
}

/**
 * Get the retry delay in case of rate limit exceeded error, adding 100 ms, of defaultValue otherwise
 */
fun Throwable.getRetryDelay(defaultValue: Long): Long {
    return (this as? Failure.ServerError)
            ?.error
            ?.takeIf { it.code == MatrixError.M_LIMIT_EXCEEDED }
            ?.retryAfterMillis
            ?.plus(100L)
            ?: defaultValue
}

fun Throwable.isInvalidPassword(): Boolean {
    return this is Failure.ServerError &&
            error.code == MatrixError.M_FORBIDDEN &&
            error.message == "Invalid password"
}

fun Throwable.isInvalidUIAAuth(): Boolean {
    return this is Failure.ServerError &&
            error.code == MatrixError.M_FORBIDDEN &&
            error.flows != null
}

/**
 * Try to convert to a RegistrationFlowResponse. Return null in the cases it's not possible
 */
fun Throwable.toRegistrationFlowResponse(): RegistrationFlowResponse? {
    return if (this is Failure.OtherServerError &&
            httpCode == HttpsURLConnection.HTTP_UNAUTHORIZED /* 401 */) {
        tryOrNull {
            MoshiProvider.providesMoshi()
                    .adapter(RegistrationFlowResponse::class.java)
                    .fromJson(errorBody)
        }
    } else if (this is Failure.ServerError &&
            httpCode == HttpsURLConnection.HTTP_UNAUTHORIZED && /* 401 */
            error.code == MatrixError.M_FORBIDDEN) {
        // This happens when the submission for this stage was bad (like bad password)
        if (error.session != null && error.flows != null) {
            RegistrationFlowResponse(
                    flows = error.flows,
                    session = error.session,
                    completedStages = error.completedStages,
                    params = error.params
            )
        } else {
            null
        }
    } else {
        null
    }
}

fun Throwable.isRegistrationAvailabilityError(): Boolean {
    return this is Failure.ServerError &&
            httpCode == HttpsURLConnection.HTTP_BAD_REQUEST && /* 400 */
            (error.code == MatrixError.M_USER_IN_USE ||
            error.code == MatrixError.M_INVALID_USERNAME ||
            error.code == MatrixError.M_EXCLUSIVE)
}

/**
 * Try to convert to a ScanFailure. Return null in the cases it's not possible
 */
fun Throwable.toScanFailure(): ScanFailure? {
    return if (this is Failure.OtherServerError) {
        tryOrNull {
            MoshiProvider.providesMoshi()
                    .adapter(ContentScannerError::class.java)
                    .fromJson(errorBody)
        }
                ?.let { ScanFailure(it, httpCode, this) }
    } else {
        null
    }
}
