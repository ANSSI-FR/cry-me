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
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.login2

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.PersistState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.features.login.LoginMode
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.auth.login.LoginProfileInfo

data class LoginViewState2(
        val isLoading: Boolean = false,

        // User choices
        @PersistState
        val signMode: SignMode2 = SignMode2.Unknown,
        @PersistState
        val userName: String? = null,
        @PersistState
        val resetPasswordEmail: String? = null,
        @PersistState
        val homeServerUrlFromUser: String? = null,

        // Can be modified after a Wellknown request
        @PersistState
        val homeServerUrl: String? = null,

        // For SSO session recovery
        @PersistState
        val deviceId: String? = null,

        // Network result
        val loginProfileInfo: Async<LoginProfileInfo> = Uninitialized,

        // Network result
        @PersistState
        val loginMode: LoginMode = LoginMode.Unknown,

        // From database
        val knownCustomHomeServersUrls: List<String> = emptyList()
) : MavericksState {

    // Pending user identifier
    fun userIdentifier(): String {
        return if (userName != null && MatrixPatterns.isUserId(userName)) {
            userName
        } else {
            "@$userName:${homeServerUrlFromUser.toReducedUrl()}"
        }
    }
}
