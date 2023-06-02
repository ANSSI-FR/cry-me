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

package org.matrix.android.sdk.internal.session.widgets

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.widgets.WidgetPostAPIMediator
import org.matrix.android.sdk.api.session.widgets.WidgetService
import org.matrix.android.sdk.api.session.widgets.WidgetURLFormatter
import org.matrix.android.sdk.api.session.widgets.model.Widget
import javax.inject.Inject
import javax.inject.Provider

internal class DefaultWidgetService @Inject constructor(private val widgetManager: WidgetManager,
                                                        private val widgetURLFormatter: WidgetURLFormatter,
                                                        private val widgetPostAPIMediator: Provider<WidgetPostAPIMediator>) :
    WidgetService {

    override fun getWidgetURLFormatter(): WidgetURLFormatter {
        return widgetURLFormatter
    }

    override fun getWidgetPostAPIMediator(): WidgetPostAPIMediator {
        return widgetPostAPIMediator.get()
    }

    override fun getRoomWidgets(
            roomId: String,
            widgetId: QueryStringValue,
            widgetTypes: Set<String>?,
            excludedTypes: Set<String>?
    ): List<Widget> {
        return widgetManager.getRoomWidgets(roomId, widgetId, widgetTypes, excludedTypes)
    }

    override fun getWidgetComputedUrl(widget: Widget, isLightTheme: Boolean): String? {
        return widgetManager.getWidgetComputedUrl(widget, isLightTheme)
    }

override fun getRoomWidgetsLive(
            roomId: String,
            widgetId: QueryStringValue,
            widgetTypes: Set<String>?,
            excludedTypes: Set<String>?
    ): LiveData<List<Widget>> {
        return widgetManager.getRoomWidgetsLive(roomId, widgetId, widgetTypes, excludedTypes)
    }

    override fun getUserWidgetsLive(
            widgetTypes: Set<String>?,
            excludedTypes: Set<String>?
    ): LiveData<List<Widget>> {
        return widgetManager.getUserWidgetsLive(widgetTypes, excludedTypes)
    }

    override fun getUserWidgets(
            widgetTypes: Set<String>?,
            excludedTypes: Set<String>?
    ): List<Widget> {
        return widgetManager.getUserWidgets(widgetTypes, excludedTypes)
    }

    override suspend fun createRoomWidget(
            roomId: String,
            widgetId: String,
            content: Content
    ): Widget {
        return widgetManager.createRoomWidget(roomId, widgetId, content)
    }

    override suspend fun destroyRoomWidget(
            roomId: String,
            widgetId: String
    ) {
        return widgetManager.destroyRoomWidget(roomId, widgetId)
    }

    override fun hasPermissionsToHandleWidgets(roomId: String): Boolean {
        return widgetManager.hasPermissionsToHandleWidgets(roomId)
    }
}
