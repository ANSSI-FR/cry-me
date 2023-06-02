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
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.settings

/**
 * Different strategies for Background sync, only applicable to F-Droid version of the app
 */
enum class BackgroundSyncMode {
    /**
     * In this mode background syncs are scheduled via Workers, meaning that the system will have control on the periodicity
     * of syncs when battery is low or when the phone is idle (sync will occur in allowed maintenance windows). After completion
     * the sync work will schedule another one.
     */
    FDROID_BACKGROUND_SYNC_MODE_FOR_BATTERY,

    /**
     * This mode requires the app to be exempted from battery optimization. Alarms will be launched and will wake up the app
     * in order to perform the background sync as a foreground service. After completion the service will schedule another alarm
     */
    FDROID_BACKGROUND_SYNC_MODE_FOR_REALTIME,

    /**
     * The app won't sync in background
     */
    FDROID_BACKGROUND_SYNC_MODE_DISABLED;

    companion object {
        const val DEFAULT_SYNC_DELAY_SECONDS = 60
        const val DEFAULT_SYNC_TIMEOUT_SECONDS = 6

        fun fromString(value: String?): BackgroundSyncMode = values().firstOrNull { it.name == value }
                ?: FDROID_BACKGROUND_SYNC_MODE_DISABLED
    }
}
