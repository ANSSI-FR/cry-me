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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home.room.breadcrumbs

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.list.UnreadCounterBadgeView
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass(layout = R.layout.item_breadcrumbs)
abstract class BreadcrumbsItem : VectorEpoxyModel<BreadcrumbsItem.Holder>() {

    @EpoxyAttribute var hasTypingUsers: Boolean = false
    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute lateinit var matrixItem: MatrixItem
    @EpoxyAttribute var unreadNotificationCount: Int = 0
    @EpoxyAttribute var showHighlighted: Boolean = false
    @EpoxyAttribute var hasUnreadMessage: Boolean = false
    @EpoxyAttribute var hasDraft: Boolean = false
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var itemClickListener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.rootView.onClick(itemClickListener)
        holder.unreadIndentIndicator.isVisible = hasUnreadMessage
        avatarRenderer.render(matrixItem, holder.avatarImageView)
        holder.avatarImageView.contentDescription = matrixItem.getBestName()
        holder.unreadCounterBadgeView.render(UnreadCounterBadgeView.State(unreadNotificationCount, showHighlighted))
        holder.draftIndentIndicator.isVisible = hasDraft
        holder.typingIndicator.isVisible = hasTypingUsers
    }

    class Holder : VectorEpoxyHolder() {
        val unreadCounterBadgeView by bind<UnreadCounterBadgeView>(R.id.breadcrumbsUnreadCounterBadgeView)
        val unreadIndentIndicator by bind<View>(R.id.breadcrumbsUnreadIndicator)
        val draftIndentIndicator by bind<View>(R.id.breadcrumbsDraftBadge)
        val typingIndicator by bind<View>(R.id.breadcrumbsTypingView)
        val avatarImageView by bind<ImageView>(R.id.breadcrumbsImageView)
        val rootView by bind<ViewGroup>(R.id.breadcrumbsRoot)
    }
}
