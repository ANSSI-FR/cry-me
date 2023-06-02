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

package org.matrix.android.sdk.internal.auth.db

import com.squareup.moshi.Moshi
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.internal.auth.login.ResetPasswordData
import org.matrix.android.sdk.internal.auth.registration.ThreePidData
import javax.inject.Inject

internal class PendingSessionMapper @Inject constructor(moshi: Moshi) {

    private val homeServerConnectionConfigAdapter = moshi.adapter(HomeServerConnectionConfig::class.java)
    private val resetPasswordDataAdapter = moshi.adapter(ResetPasswordData::class.java)
    private val threePidDataAdapter = moshi.adapter(ThreePidData::class.java)

    fun map(entity: PendingSessionEntity?): PendingSessionData? {
        if (entity == null) {
            return null
        }

        val homeServerConnectionConfig = homeServerConnectionConfigAdapter.fromJson(entity.homeServerConnectionConfigJson)!!
        val resetPasswordData = entity.resetPasswordDataJson?.let { resetPasswordDataAdapter.fromJson(it) }
        val threePidData = entity.currentThreePidDataJson?.let { threePidDataAdapter.fromJson(it) }

        return PendingSessionData(
                homeServerConnectionConfig = homeServerConnectionConfig,
                clientSecret = entity.clientSecret,
                sendAttempt = entity.sendAttempt,
                resetPasswordData = resetPasswordData,
                currentSession = entity.currentSession,
                isRegistrationStarted = entity.isRegistrationStarted,
                currentThreePidData = threePidData)
    }

    fun map(sessionData: PendingSessionData?): PendingSessionEntity? {
        if (sessionData == null) {
            return null
        }

        val homeServerConnectionConfigJson = homeServerConnectionConfigAdapter.toJson(sessionData.homeServerConnectionConfig)
        val resetPasswordDataJson = resetPasswordDataAdapter.toJson(sessionData.resetPasswordData)
        val currentThreePidDataJson = threePidDataAdapter.toJson(sessionData.currentThreePidData)

        return PendingSessionEntity(
                homeServerConnectionConfigJson = homeServerConnectionConfigJson,
                clientSecret = sessionData.clientSecret,
                sendAttempt = sessionData.sendAttempt,
                resetPasswordDataJson = resetPasswordDataJson,
                currentSession = sessionData.currentSession,
                isRegistrationStarted = sessionData.isRegistrationStarted,
                currentThreePidDataJson = currentThreePidDataJson
        )
    }
}
