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

package im.vector.app.features.home.room.detail.timeline.item

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.charsequence.EpoxyCharSequence
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.ui.views.ShieldImageView
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import org.matrix.android.sdk.api.crypto.RoomEncryptionTrustLevel

@EpoxyModelClass(layout = R.layout.item_timeline_event_base_noinfo)
abstract class NoticeItem : BaseEventItem<NoticeItem.Holder>() {

    @EpoxyAttribute
    lateinit var attributes: Attributes

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.noticeTextView.text = attributes.noticeText.charSequence
        attributes.avatarRenderer.render(attributes.informationData.matrixItem, holder.avatarImageView)
        holder.view.setOnLongClickListener(attributes.itemLongClickListener)
        holder.avatarImageView.onClick(attributes.avatarClickListener)

        when (attributes.informationData.e2eDecoration) {
            E2EDecoration.NONE                 -> {
                holder.e2EDecorationView.render(null)
            }
            E2EDecoration.WARN_IN_CLEAR,
            E2EDecoration.WARN_SENT_BY_UNVERIFIED,
            E2EDecoration.WARN_SENT_BY_UNKNOWN -> {
                holder.e2EDecorationView.render(RoomEncryptionTrustLevel.Warning)
            }
        }
    }

    override fun unbind(holder: Holder) {
        attributes.avatarRenderer.clear(holder.avatarImageView)
        super.unbind(holder)
    }

    override fun getEventIds(): List<String> {
        return listOf(attributes.informationData.eventId)
    }

    override fun getViewType() = STUB_ID

    class Holder : BaseHolder(STUB_ID) {
        val avatarImageView by bind<ImageView>(R.id.itemNoticeAvatarView)
        val noticeTextView by bind<TextView>(R.id.itemNoticeTextView)
        val e2EDecorationView by bind<ShieldImageView>(R.id.messageE2EDecoration)
    }

    data class Attributes(
            val avatarRenderer: AvatarRenderer,
            val informationData: MessageInformationData,
            val noticeText: EpoxyCharSequence,
            val itemLongClickListener: View.OnLongClickListener? = null,
            val readReceiptsCallback: TimelineEventController.ReadReceiptsCallback? = null,
            val avatarClickListener: ClickListener? = null
    )

    companion object {
        private const val STUB_ID = R.id.messageContentNoticeStub
    }
}
