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
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide

/**
 * A generic list item to display when there is no results, with an optional CTA
 */
@EpoxyModelClass(layout = R.layout.item_generic_empty_state)
abstract class GenericEmptyWithActionItem : VectorEpoxyModel<GenericEmptyWithActionItem.Holder>() {

    @EpoxyAttribute
    var title: String? = null

    @EpoxyAttribute
    var description: String? = null

    @EpoxyAttribute
    @DrawableRes
    var iconRes: Int = -1

    @EpoxyAttribute
    @ColorInt
    var iconTint: Int? = null

    @EpoxyAttribute
    var buttonAction: Action? = null

    override fun bind(holder: Holder) {
        super.bind(holder)

        holder.titleText.setTextOrHide(title)
        holder.descriptionText.setTextOrHide(description)

        if (iconRes != -1) {
            holder.imageView.setImageResource(iconRes)
            holder.imageView.isVisible = true
            if (iconTint != null) {
                ImageViewCompat.setImageTintList(holder.imageView, ColorStateList.valueOf(iconTint!!))
            } else {
                ImageViewCompat.setImageTintList(holder.imageView, null)
            }
        } else {
            holder.imageView.isVisible = false
        }

        holder.actionButton.setTextOrHide(buttonAction?.title)
        holder.actionButton.onClick(buttonAction?.listener)
    }

    class Holder : VectorEpoxyHolder() {
        val root by bind<View>(R.id.item_generic_root)
        val titleText by bind<TextView>(R.id.emptyItemTitleView)
        val descriptionText by bind<TextView>(R.id.emptyItemMessageView)
        val imageView by bind<ImageView>(R.id.emptyItemImageView)
        val actionButton by bind<Button>(R.id.emptyItemButton)
    }
}
