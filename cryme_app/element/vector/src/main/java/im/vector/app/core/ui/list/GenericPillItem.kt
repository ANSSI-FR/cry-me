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
 * Copyright 2019 New Vector Ltd
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
package im.vector.app.core.ui.list

import android.content.res.ColorStateList
import android.view.Gravity
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.charsequence.EpoxyCharSequence
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.features.themes.ThemeUtils

/**
 * A generic list item with a rounded corner background and an optional icon
 */
@EpoxyModelClass(layout = R.layout.item_generic_pill_footer)
abstract class GenericPillItem : VectorEpoxyModel<GenericPillItem.Holder>() {

    @EpoxyAttribute
    var text: EpoxyCharSequence? = null

    @EpoxyAttribute
    var style: ItemStyle = ItemStyle.NORMAL_TEXT

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var itemClickAction: ClickListener? = null

    @EpoxyAttribute
    var centered: Boolean = false

    @EpoxyAttribute
    @DrawableRes
    var imageRes: Int? = null

    @EpoxyAttribute
    var tintIcon: Boolean = true

    override fun bind(holder: Holder) {
        super.bind(holder)

        holder.textView.setTextOrHide(text?.charSequence)
        holder.textView.typeface = style.toTypeFace()
        holder.textView.textSize = style.toTextSize()
        holder.textView.gravity = if (centered) Gravity.CENTER_HORIZONTAL else Gravity.START

        if (imageRes != null) {
            holder.imageView.setImageResource(imageRes!!)
            holder.imageView.isVisible = true
        } else {
            holder.imageView.isVisible = false
        }
        if (tintIcon) {
            val iconTintColor = ThemeUtils.getColor(holder.view.context, R.attr.vctr_content_secondary)
            ImageViewCompat.setImageTintList(holder.imageView, ColorStateList.valueOf(iconTintColor))
        } else {
            ImageViewCompat.setImageTintList(holder.imageView, null)
        }

        holder.view.onClick(itemClickAction)
    }

    class Holder : VectorEpoxyHolder() {
        val imageView by bind<ImageView>(R.id.itemGenericPillImage)
        val textView by bind<TextView>(R.id.itemGenericPillText)
    }
}
