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

package im.vector.app.features.home.room.detail.widget

import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.onClick
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.widgets.model.Widget
import java.net.URL

@EpoxyModelClass(layout = R.layout.item_room_widget)
abstract class RoomWidgetItem : EpoxyModelWithHolder<RoomWidgetItem.Holder>() {

    @EpoxyAttribute lateinit var widget: Widget
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var widgetClicked: ClickListener? = null

    @DrawableRes
    @EpoxyAttribute var iconRes: Int? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.widgetName.text = widget.name
        holder.widgetUrl.text = tryOrNull { URL(widget.widgetContent.url) }?.host ?: widget.widgetContent.url
        if (iconRes != null) {
            holder.iconImage.isVisible = true
            holder.iconImage.setImageResource(iconRes!!)
        } else {
            holder.iconImage.isVisible = false
        }
        holder.view.onClick(widgetClicked)
    }

    class Holder : VectorEpoxyHolder() {
        val widgetName by bind<TextView>(R.id.roomWidgetName)
        val widgetUrl by bind<TextView>(R.id.roomWidgetUrl)
        val iconImage by bind<ImageView>(R.id.roomWidgetAvatar)
    }
}
