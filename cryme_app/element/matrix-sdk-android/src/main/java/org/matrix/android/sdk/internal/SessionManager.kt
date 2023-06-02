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

package org.matrix.android.sdk.internal

import org.matrix.android.sdk.api.auth.data.SessionParams
import org.matrix.android.sdk.api.auth.data.sessionId
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.internal.auth.SessionParamsStore
import org.matrix.android.sdk.internal.di.MatrixComponent
import org.matrix.android.sdk.internal.di.MatrixScope
import org.matrix.android.sdk.internal.session.DaggerSessionComponent
import org.matrix.android.sdk.internal.session.SessionComponent
import javax.inject.Inject

@MatrixScope
internal class SessionManager @Inject constructor(private val matrixComponent: MatrixComponent,
                                                  private val sessionParamsStore: SessionParamsStore) {

    // SessionId -> SessionComponent
    private val sessionComponents = HashMap<String, SessionComponent>()

    fun getSessionComponent(sessionId: String): SessionComponent? {
        val sessionParams = sessionParamsStore.get(sessionId) ?: return null
        return getOrCreateSessionComponent(sessionParams)
    }

    fun getOrCreateSession(sessionParams: SessionParams): Session {
        return getOrCreateSessionComponent(sessionParams).session()
    }

    fun releaseSession(sessionId: String) {
        if (sessionComponents.containsKey(sessionId).not()) {
            throw RuntimeException("You don't have a session for id $sessionId")
        }
        sessionComponents.remove(sessionId)?.also {
            it.session().close()
        }
    }

    fun stopSession(sessionId: String) {
        val sessionComponent = sessionComponents[sessionId] ?: throw RuntimeException("You don't have a session for id $sessionId")
        sessionComponent.session().stopSync()
    }

    fun getOrCreateSessionComponent(sessionParams: SessionParams): SessionComponent {
        return sessionComponents.getOrPut(sessionParams.credentials.sessionId()) {
            DaggerSessionComponent
                    .factory()
                    .create(matrixComponent, sessionParams)
        }
    }
}
