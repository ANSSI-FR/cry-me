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

package org.matrix.android.sdk.internal.session.integrationmanager

import org.matrix.android.sdk.api.session.integrationmanager.IntegrationManagerConfig
import org.matrix.android.sdk.api.session.integrationmanager.IntegrationManagerService
import javax.inject.Inject

internal class DefaultIntegrationManagerService @Inject constructor(private val integrationManager: IntegrationManager) : IntegrationManagerService {

    override fun addListener(listener: IntegrationManagerService.Listener) {
        integrationManager.addListener(listener)
    }

    override fun removeListener(listener: IntegrationManagerService.Listener) {
        integrationManager.removeListener(listener)
    }

    override fun getOrderedConfigs(): List<IntegrationManagerConfig> {
        return integrationManager.getOrderedConfigs()
    }

    override fun getPreferredConfig(): IntegrationManagerConfig {
        return integrationManager.getPreferredConfig()
    }

    override fun isIntegrationEnabled(): Boolean {
        return integrationManager.isIntegrationEnabled()
    }

    override suspend fun setIntegrationEnabled(enable: Boolean) {
        integrationManager.setIntegrationEnabled(enable)
    }

    override suspend fun setWidgetAllowed(stateEventId: String, allowed: Boolean) {
        integrationManager.setWidgetAllowed(stateEventId, allowed)
    }

    override fun isWidgetAllowed(stateEventId: String): Boolean {
        return integrationManager.isWidgetAllowed(stateEventId)
    }

    override suspend fun setNativeWidgetDomainAllowed(widgetType: String, domain: String, allowed: Boolean) {
        integrationManager.setNativeWidgetDomainAllowed(widgetType, domain, allowed)
    }

    override fun isNativeWidgetDomainAllowed(widgetType: String, domain: String): Boolean {
        return integrationManager.isNativeWidgetDomainAllowed(widgetType, domain)
    }
}
