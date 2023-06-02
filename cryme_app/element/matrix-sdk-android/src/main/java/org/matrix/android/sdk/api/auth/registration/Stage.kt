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

package org.matrix.android.sdk.api.auth.registration

sealed class Stage(open val mandatory: Boolean) {

    // m.login.recaptcha
    data class ReCaptcha(override val mandatory: Boolean, val publicKey: String) : Stage(mandatory)

    // m.login.email.identity
    data class Email(override val mandatory: Boolean) : Stage(mandatory)

    // m.login.msisdn
    data class Msisdn(override val mandatory: Boolean) : Stage(mandatory)

    // m.login.dummy, can be mandatory if there is no other stages. In this case the account cannot be created by just sending a username
    // and a password, the dummy stage has to be done
    data class Dummy(override val mandatory: Boolean) : Stage(mandatory)

    // Undocumented yet: m.login.terms
    data class Terms(override val mandatory: Boolean, val policies: TermPolicies) : Stage(mandatory)

    // For unknown stages
    data class Other(override val mandatory: Boolean, val type: String, val params: Map<*, *>?) : Stage(mandatory)
}

typealias TermPolicies = Map<*, *>
