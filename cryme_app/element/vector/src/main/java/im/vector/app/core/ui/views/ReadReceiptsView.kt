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

package im.vector.app.core.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.databinding.ViewReadReceiptsBinding
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.item.ReadReceiptData
import im.vector.app.features.home.room.detail.timeline.item.toMatrixItem

private const val MAX_RECEIPT_DISPLAYED = 5
private const val MAX_RECEIPT_DESCRIBED = 3

class ReadReceiptsView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val views: ViewReadReceiptsBinding

    init {
        setupView()
        views = ViewReadReceiptsBinding.bind(this)
    }

    private val receiptAvatars: List<ImageView> by lazy {
        listOf(
                views.receiptAvatar1,
                views.receiptAvatar2,
                views.receiptAvatar3,
                views.receiptAvatar4,
                views.receiptAvatar5
        )
    }

    private fun setupView() {
        inflate(context, R.layout.view_read_receipts, this)
        contentDescription = context.getString(R.string.a11y_view_read_receipts)
    }

    fun render(readReceipts: List<ReadReceiptData>, avatarRenderer: AvatarRenderer) {
        if (readReceipts.isNotEmpty()) {
            isVisible = true
            for (index in 0 until MAX_RECEIPT_DISPLAYED) {
                val receiptData = readReceipts.getOrNull(index)
                if (receiptData == null) {
                    receiptAvatars[index].visibility = View.INVISIBLE
                } else {
                    receiptAvatars[index].visibility = View.VISIBLE
                    avatarRenderer.render(receiptData.toMatrixItem(), receiptAvatars[index])
                }
            }

            val displayNames = readReceipts
                    .mapNotNull { it.displayName }
                    .filter { it.isNotBlank() }
                    .take(MAX_RECEIPT_DESCRIBED)

            if (readReceipts.size > MAX_RECEIPT_DISPLAYED) {
                views.receiptMore.visibility = View.VISIBLE
                views.receiptMore.text = context.getString(
                        R.string.x_plus, readReceipts.size - MAX_RECEIPT_DISPLAYED
                )
            } else {
                views.receiptMore.visibility = View.GONE
            }
            contentDescription = when (readReceipts.size) {
                1    ->
                    if (displayNames.size == 1) {
                        context.getString(R.string.one_user_read, displayNames[0])
                    } else {
                        context.resources.getQuantityString(R.plurals.fallback_users_read, readReceipts.size)
                    }
                2    ->
                    if (displayNames.size == 2) {
                        context.getString(R.string.two_users_read, displayNames[0], displayNames[1])
                    } else {
                        context.resources.getQuantityString(R.plurals.fallback_users_read, readReceipts.size)
                    }
                3    ->
                    if (displayNames.size == 3) {
                        context.getString(R.string.three_users_read, displayNames[0], displayNames[1], displayNames[2])
                    } else {
                        context.resources.getQuantityString(R.plurals.fallback_users_read, readReceipts.size)
                    }
                else ->
                    if (displayNames.size >= 2) {
                        val qty = readReceipts.size - 2
                        context.resources.getQuantityString(R.plurals.two_and_some_others_read,
                                qty,
                                displayNames[0],
                                displayNames[1],
                                qty)
                    } else {
                        context.resources.getQuantityString(R.plurals.fallback_users_read, readReceipts.size)
                    }
            }
        } else {
            isVisible = false
        }
    }

    fun unbind(avatarRenderer: AvatarRenderer?) {
        receiptAvatars.forEach {
            avatarRenderer?.clear(it)
        }
        isVisible = false
    }
}
