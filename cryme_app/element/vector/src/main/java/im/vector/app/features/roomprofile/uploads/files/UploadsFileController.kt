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

package im.vector.app.features.roomprofile.uploads.files

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.epoxy.VisibilityState
import im.vector.app.R
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.roomprofile.uploads.RoomUploadsViewState
import org.matrix.android.sdk.api.session.room.uploads.UploadEvent
import javax.inject.Inject

class UploadsFileController @Inject constructor(
        private val stringProvider: StringProvider,
        private val dateFormatter: VectorDateFormatter
) : TypedEpoxyController<RoomUploadsViewState>() {

    interface Listener {
        fun loadMore()
        fun onOpenClicked(uploadEvent: UploadEvent)
        fun onDownloadClicked(uploadEvent: UploadEvent)
        fun onShareClicked(uploadEvent: UploadEvent)
    }

    var listener: Listener? = null

    private var idx = 0

    override fun buildModels(data: RoomUploadsViewState?) {
        data ?: return
        val host = this

        buildFileItems(data.fileEvents)

        if (data.hasMore) {
            loadingItem {
                // Always use a different id, because we can be notified several times of visibility state changed
                id("loadMore${host.idx++}")
                onVisibilityStateChanged { _, _, visibilityState ->
                    if (visibilityState == VisibilityState.VISIBLE) {
                        host.listener?.loadMore()
                    }
                }
            }
        }
    }

    private fun buildFileItems(fileEvents: List<UploadEvent>) {
        val host = this
        fileEvents.forEach { uploadEvent ->
            uploadsFileItem {
                id(uploadEvent.eventId)
                title(uploadEvent.contentWithAttachmentContent.body)
                subtitle(host.stringProvider.getString(R.string.uploads_files_subtitle,
                        uploadEvent.senderInfo.disambiguatedDisplayName,
                        host.dateFormatter.format(uploadEvent.root.originServerTs, DateFormatKind.DEFAULT_DATE_AND_TIME)))
                listener(object : UploadsFileItem.Listener {
                    override fun onItemClicked() {
                        host.listener?.onOpenClicked(uploadEvent)
                    }

                    override fun onDownloadClicked() {
                        host.listener?.onDownloadClicked(uploadEvent)
                    }

                    override fun onShareClicked() {
                        host.listener?.onShareClicked(uploadEvent)
                    }
                })
            }
        }
    }
}
