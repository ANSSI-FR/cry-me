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
package im.vector.app.features.settings.threepids

import android.view.View
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

@EpoxyModelClass(layout = R.layout.item_settings_three_pid)
abstract class ThreePidItem : EpoxyModelWithHolder<ThreePidItem.Holder>() {

    @EpoxyAttribute
    var title: String? = null

    @EpoxyAttribute
    @DrawableRes
    var iconResId: Int? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var deleteClickListener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        val safeIconResId = iconResId
        if (safeIconResId != null) {
            holder.icon.isVisible = true
            holder.icon.setImageResource(safeIconResId)
        } else {
            holder.icon.isVisible = false
        }

        holder.title.text = title
        holder.delete.onClick(deleteClickListener)
        holder.delete.isVisible = deleteClickListener != null
    }

    class Holder : VectorEpoxyHolder() {
        val icon by bind<ImageView>(R.id.item_settings_three_pid_icon)
        val title by bind<TextView>(R.id.item_settings_three_pid_title)
        val delete by bind<View>(R.id.item_settings_three_pid_delete)
    }
}
