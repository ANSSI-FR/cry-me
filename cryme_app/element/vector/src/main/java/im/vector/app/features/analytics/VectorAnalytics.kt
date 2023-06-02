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

package im.vector.app.features.analytics

import im.vector.app.features.analytics.itf.VectorAnalyticsEvent
import im.vector.app.features.analytics.itf.VectorAnalyticsScreen
import kotlinx.coroutines.flow.Flow

interface VectorAnalytics {
    /**
     * Return a Flow of Boolean, true if the user has given their consent
     */
    fun getUserConsent(): Flow<Boolean>

    /**
     * Update the user consent value
     */
    suspend fun setUserConsent(userConsent: Boolean)

    /**
     * Return a Flow of Boolean, true if the user has been asked for their consent
     */
    fun didAskUserConsent(): Flow<Boolean>

    /**
     * Store the fact that the user has been asked for their consent
     */
    suspend fun setDidAskUserConsent()

    /**
     * Return a Flow of String, used for analytics Id
     */
    fun getAnalyticsId(): Flow<String>

    /**
     * Update analyticsId from the AccountData
     */
    suspend fun setAnalyticsId(analyticsId: String)

    /**
     * To be called when a session is destroyed
     */
    suspend fun onSignOut()

    /**
     * To be called when application is started
     */
    fun init()

    /**
     * Capture an Event
     */
    fun capture(event: VectorAnalyticsEvent)

    /**
     * Track a displayed screen
     */
    fun screen(screen: VectorAnalyticsScreen)
}
