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
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.crypto.verification.choose

import com.airbnb.epoxy.EpoxyController
import im.vector.app.R
import im.vector.app.core.epoxy.bottomSheetDividerItem
import im.vector.app.core.epoxy.charsequence.toEpoxyCharSequence
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationActionItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationNoticeItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationQrCodeItem
import javax.inject.Inject

class VerificationChooseMethodController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider
) : EpoxyController() {

    var listener: Listener? = null

    private var viewState: VerificationChooseMethodViewState? = null

    fun update(viewState: VerificationChooseMethodViewState) {
        this.viewState = viewState
        requestModelBuild()
    }

    override fun buildModels() {
        val state = viewState ?: return
        val host = this

        if (state.otherCanScanQrCode || state.otherCanShowQrCode) {
            val scanCodeInstructions: String
            val scanOtherCodeTitle: String
            val compareEmojiSubtitle: String
            if (state.isMe) {
                scanCodeInstructions = stringProvider.getString(R.string.verification_scan_self_notice)
                scanOtherCodeTitle = stringProvider.getString(R.string.verification_scan_with_this_device)
                compareEmojiSubtitle = stringProvider.getString(R.string.verification_scan_self_emoji_subtitle)
            } else {
                scanCodeInstructions = stringProvider.getString(R.string.verification_scan_notice)
                scanOtherCodeTitle = stringProvider.getString(R.string.verification_scan_their_code)
                compareEmojiSubtitle = stringProvider.getString(R.string.verification_scan_emoji_subtitle)
            }

            bottomSheetVerificationNoticeItem {
                id("notice")
                notice(scanCodeInstructions.toEpoxyCharSequence())
            }

            if (state.otherCanScanQrCode && !state.qrCodeText.isNullOrBlank()) {
                bottomSheetVerificationQrCodeItem {
                    id("qr")
                    data(state.qrCodeText)
                }

                bottomSheetDividerItem {
                    id("sep0")
                }
            }

            if (state.otherCanShowQrCode) {
                bottomSheetVerificationActionItem {
                    id("openCamera")
                    title(scanOtherCodeTitle)
                    titleColor(host.colorProvider.getColorFromAttribute(R.attr.colorPrimary))
                    iconRes(R.drawable.ic_camera)
                    iconColor(host.colorProvider.getColorFromAttribute(R.attr.colorPrimary))
                    listener { host.listener?.openCamera() }
                }

                bottomSheetDividerItem {
                    id("sep1")
                }
            }

            bottomSheetVerificationActionItem {
                id("openEmoji")
                title(host.stringProvider.getString(R.string.verification_scan_emoji_title))
                titleColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
                subTitle(compareEmojiSubtitle)
                iconRes(R.drawable.ic_arrow_right)
                iconColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
                listener { host.listener?.doVerifyBySas() }
            }
        } else if (state.sasModeAvailable) {
            bottomSheetVerificationActionItem {
                id("openEmoji")
                title(host.stringProvider.getString(R.string.verification_no_scan_emoji_title))
                titleColor(host.colorProvider.getColorFromAttribute(R.attr.colorPrimary))
                iconRes(R.drawable.ic_arrow_right)
                iconColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
                listener { host.listener?.doVerifyBySas() }
            }
        }

        if (state.isMe && state.canCrossSign) {
            bottomSheetDividerItem {
                id("sep_notMe")
            }

            bottomSheetVerificationActionItem {
                id("wasnote")
                title(host.stringProvider.getString(R.string.verify_new_session_was_not_me))
                titleColor(host.colorProvider.getColorFromAttribute(R.attr.colorError))
                subTitle(host.stringProvider.getString(R.string.verify_new_session_compromized))
                iconRes(R.drawable.ic_arrow_right)
                iconColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
                listener { host.listener?.onClickOnWasNotMe() }
            }
        }
    }

    interface Listener {
        fun openCamera()
        fun doVerifyBySas()
        fun onClickOnWasNotMe()
    }
}
