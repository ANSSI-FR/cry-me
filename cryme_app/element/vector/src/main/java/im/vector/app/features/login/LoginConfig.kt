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

package im.vector.app.features.login

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Parameters extracted from a configuration url
 * Ex: https://mobile.element.io?hs_url=https%3A%2F%2Fexample.modular.im&is_url=https%3A%2F%2Fcustom.identity.org
 *
 * Note: On Element, identityServerUrl will never be used, so is declared private. Keep it for compatibility reason.
 */
@Parcelize
data class LoginConfig(
        val homeServerUrl: String?,
        private val identityServerUrl: String?
) : Parcelable {

    companion object {
        const val CONFIG_HS_PARAMETER = "hs_url"
        private const val CONFIG_IS_PARAMETER = "is_url"

        fun parse(from: Uri): LoginConfig {
            return LoginConfig(
                    homeServerUrl = from.getQueryParameter(CONFIG_HS_PARAMETER),
                    identityServerUrl = from.getQueryParameter(CONFIG_IS_PARAMETER)
            )
        }
    }
}
