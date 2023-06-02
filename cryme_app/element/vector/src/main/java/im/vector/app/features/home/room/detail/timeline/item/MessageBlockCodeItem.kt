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

import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.charsequence.EpoxyCharSequence
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide
import me.saket.bettermovementmethod.BetterLinkMovementMethod

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class MessageBlockCodeItem : AbsMessageItem<MessageBlockCodeItem.Holder>() {

    @EpoxyAttribute
    var message: EpoxyCharSequence? = null

    @EpoxyAttribute
    var editedSpan: EpoxyCharSequence? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.messageView.text = message?.charSequence
        renderSendState(holder.messageView, holder.messageView)
        holder.messageView.onClick(attributes.itemClickListener)
        holder.messageView.setOnLongClickListener(attributes.itemLongClickListener)
        holder.editedView.movementMethod = BetterLinkMovementMethod.getInstance()
        holder.editedView.setTextOrHide(editedSpan?.charSequence)
    }

    override fun getViewType() = STUB_ID

    class Holder : AbsMessageItem.Holder(STUB_ID) {
        val messageView by bind<TextView>(R.id.codeBlockTextView)
        val editedView by bind<TextView>(R.id.codeBlockEditedView)
    }

    companion object {
        private const val STUB_ID = R.id.messageContentCodeBlockStub
    }
}
