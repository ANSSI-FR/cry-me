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

package im.vector.lib.attachmentviewer

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams

interface ImageLoaderTarget {

    fun contextView(): ImageView

    fun onResourceLoading(uid: String, placeholder: Drawable?)

    fun onLoadFailed(uid: String, errorDrawable: Drawable?)

    fun onResourceCleared(uid: String, placeholder: Drawable?)

    fun onResourceReady(uid: String, resource: Drawable)
}

internal class DefaultImageLoaderTarget(val holder: AnimatedImageViewHolder, private val contextView: ImageView) :
    ImageLoaderTarget {
    override fun contextView(): ImageView {
        return contextView
    }

    override fun onResourceLoading(uid: String, placeholder: Drawable?) {
        if (holder.boundResourceUid != uid) return
        holder.views.imageLoaderProgress.isVisible = true
    }

    override fun onLoadFailed(uid: String, errorDrawable: Drawable?) {
        if (holder.boundResourceUid != uid) return
        holder.views.imageLoaderProgress.isVisible = false
        holder.views.imageView.setImageDrawable(errorDrawable)
    }

    override fun onResourceCleared(uid: String, placeholder: Drawable?) {
        if (holder.boundResourceUid != uid) return
        holder.views.imageView.setImageDrawable(placeholder)
    }

    override fun onResourceReady(uid: String, resource: Drawable) {
        if (holder.boundResourceUid != uid) return
        holder.views.imageLoaderProgress.isVisible = false
        // Glide mess up the view size :/
        holder.views.imageView.updateLayoutParams {
            width = LinearLayout.LayoutParams.MATCH_PARENT
            height = LinearLayout.LayoutParams.MATCH_PARENT
        }
        holder.views.imageView.setImageDrawable(resource)
        if (resource is Animatable) {
            resource.start()
        }
    }

    internal class ZoomableImageTarget(val holder: ZoomableImageViewHolder, private val contextView: ImageView) : ImageLoaderTarget {
        override fun contextView() = contextView

        override fun onResourceLoading(uid: String, placeholder: Drawable?) {
            if (holder.boundResourceUid != uid) return
            holder.views.imageLoaderProgress.isVisible = true
            holder.views.touchImageView.setImageDrawable(placeholder)
        }

        override fun onLoadFailed(uid: String, errorDrawable: Drawable?) {
            if (holder.boundResourceUid != uid) return
            holder.views.imageLoaderProgress.isVisible = false
            holder.views.touchImageView.setImageDrawable(errorDrawable)
        }

        override fun onResourceCleared(uid: String, placeholder: Drawable?) {
            if (holder.boundResourceUid != uid) return
            holder.views.touchImageView.setImageDrawable(placeholder)
        }

        override fun onResourceReady(uid: String, resource: Drawable) {
            if (holder.boundResourceUid != uid) return
            holder.views.imageLoaderProgress.isVisible = false
            // Glide mess up the view size :/
            holder.views.touchImageView.updateLayoutParams {
                width = LinearLayout.LayoutParams.MATCH_PARENT
                height = LinearLayout.LayoutParams.MATCH_PARENT
            }
            holder.views.touchImageView.setImageDrawable(resource)
        }
    }
}
