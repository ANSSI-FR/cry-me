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

package im.vector.app.features.analytics.plan

import im.vector.app.features.analytics.itf.VectorAnalyticsEvent

// GENERATED FILE, DO NOT EDIT. FOR MORE INFORMATION VISIT
// https://github.com/matrix-org/matrix-analytics-events/

/**
 * Triggered after timing an operation in the app.
 */
data class PerformanceTimer(
    /**
     * Client defined, can be used for debugging.
     */
    val context: String? = null,
    /**
     * Client defined, an optional value to indicate how many items were handled during the operation.
     */
    val itemCount: Int? = null,
    /**
     * The timer that is being reported.
     */
    val name: Name,
    /**
     * The time reported by the timer in milliseconds.
     */
    val timeMs: Int,
) : VectorAnalyticsEvent {

    enum class Name {
        /**
         * The time spent parsing the response from an initial /sync request.
         */
        InitialSyncParsing,

        /**
         * The time spent waiting for a response to an initial /sync request.
         */
        InitialSyncRequest,

        /**
         * The time taken to display an event in the timeline that was opened from a notification.
         */
        NotificationsOpenEvent,

        /**
         * The duration of a regular /sync request when resuming the app.
         */
        StartupIncrementalSync,

        /**
         * The duration of an initial /sync request during startup (if the store has been wiped).
         */
        StartupInitialSync,

        /**
         * How long the app launch screen is displayed for.
         */
        StartupLaunchScreen,

        /**
         * The time to preload data in the MXStore on iOS.
         */
        StartupStorePreload,

        /**
         * The time to load all data from the store (including StartupStorePreload time).
         */
        StartupStoreReady,
    }

    override fun getName() = "PerformanceTimer"

    override fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            context?.let { put("context", it) }
            itemCount?.let { put("itemCount", it) }
            put("name", name.name)
            put("timeMs", timeMs)
        }.takeIf { it.isNotEmpty() }
    }
}
