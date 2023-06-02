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
 * Copyright 2021 New Vector Ltd
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

package im.vector.app.features.roomdirectory.picker

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide

@EpoxyModelClass(layout = R.layout.item_room_directory_server)
abstract class RoomDirectoryServerItem : VectorEpoxyModel<RoomDirectoryServerItem.Holder>() {

    @EpoxyAttribute
    var serverName: String? = null

    @EpoxyAttribute
    var serverDescription: String? = null

    @EpoxyAttribute
    var canRemove: Boolean = false

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var removeListener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.nameView.text = serverName
        holder.descriptionView.setTextOrHide(serverDescription)
        holder.deleteView.isVisible = canRemove
        holder.deleteView.onClick(removeListener)
    }

    class Holder : VectorEpoxyHolder() {
        val nameView by bind<TextView>(R.id.itemRoomDirectoryServerName)
        val descriptionView by bind<TextView>(R.id.itemRoomDirectoryServerDescription)
        val deleteView by bind<View>(R.id.itemRoomDirectoryServerRemove)
    }
}
