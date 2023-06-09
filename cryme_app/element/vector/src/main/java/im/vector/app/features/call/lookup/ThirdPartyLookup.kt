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
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.call.lookup

import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.thirdparty.model.ThirdPartyUser

private const val LOOKUP_SUCCESS_FIELD = "lookup_success"

suspend fun Session.pstnLookup(phoneNumber: String, protocol: String?): List<ThirdPartyUser> {
    if (protocol == null) return emptyList()
    return tryOrNull {
        thirdPartyService().getThirdPartyUser(
                protocol = protocol,
                fields = mapOf("m.id.phone" to phoneNumber)
        )
    }.orEmpty()
}

suspend fun Session.sipVirtualLookup(nativeMxid: String): List<ThirdPartyUser> {
    return tryOrNull {
        thirdPartyService().getThirdPartyUser(
                protocol = PROTOCOL_SIP_VIRTUAL,
                fields = mapOf("native_mxid" to nativeMxid)
        )
    }
            .orEmpty()
            .filter {
                (it.fields[LOOKUP_SUCCESS_FIELD] as? Boolean).orFalse()
            }
}

suspend fun Session.sipNativeLookup(virtualMxid: String): List<ThirdPartyUser> {
    return tryOrNull {
        thirdPartyService().getThirdPartyUser(
                protocol = PROTOCOL_SIP_NATIVE,
                fields = mapOf("virtual_mxid" to virtualMxid)
        )
    }
            .orEmpty()
            .filter {
                (it.fields[LOOKUP_SUCCESS_FIELD] as? Boolean).orFalse()
            }
}
