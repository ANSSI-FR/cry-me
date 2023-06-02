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

package im.vector.app.features.widgets

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.themes.ThemeProvider
import org.matrix.android.sdk.api.session.widgets.model.Widget
import javax.inject.Inject

class WidgetArgsBuilder @Inject constructor(
        private val sessionHolder: ActiveSessionHolder,
        private val themeProvider: ThemeProvider
) {

    @Suppress("UNCHECKED_CAST")
    fun buildIntegrationManagerArgs(roomId: String, integId: String?, screen: String?): WidgetArgs {
        val session = sessionHolder.getActiveSession()
        val integrationManagerConfig = session.integrationManagerService().getPreferredConfig()
        val normalizedScreen = when {
            screen == null             -> null
            screen.startsWith("type_") -> screen
            else                       -> "type_$screen"
        }
        return WidgetArgs(
                baseUrl = integrationManagerConfig.uiUrl,
                kind = WidgetKind.INTEGRATION_MANAGER,
                roomId = roomId,
                urlParams = mapOf(
                        "screen" to normalizedScreen,
                        "integ_id" to integId,
                        "room_id" to roomId,
                        "theme" to getTheme()
                ).filterNotNull()
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun buildStickerPickerArgs(roomId: String, widget: Widget): WidgetArgs {
        val widgetId = widget.widgetId
        val baseUrl = sessionHolder.getActiveSession().widgetService()
                .getWidgetComputedUrl(widget, themeProvider.isLightTheme()) ?: throw IllegalStateException()
        return WidgetArgs(
                baseUrl = baseUrl,
                kind = WidgetKind.STICKER_PICKER,
                roomId = roomId,
                widgetId = widgetId,
                urlParams = mapOf(
                        "widgetId" to widgetId,
                        "room_id" to roomId,
                        "theme" to getTheme()
                ).filterNotNull()
        )
    }

    fun buildRoomWidgetArgs(roomId: String, widget: Widget): WidgetArgs {
        val widgetId = widget.widgetId
        val baseUrl = sessionHolder.getActiveSession().widgetService()
                .getWidgetComputedUrl(widget, themeProvider.isLightTheme()) ?: throw IllegalStateException()
        return WidgetArgs(
                baseUrl = baseUrl,
                kind = WidgetKind.ROOM,
                roomId = roomId,
                widgetId = widgetId
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, String?>.filterNotNull(): Map<String, String> {
        return filterValues { it != null } as Map<String, String>
    }

    private fun getTheme(): String {
        return if (themeProvider.isLightTheme()) {
            "light"
        } else {
            "dark"
        }
    }
}
