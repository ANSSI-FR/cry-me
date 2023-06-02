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
package im.vector.app.features.widgets.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.PermissionRequest
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R

object WebviewPermissionUtils {

    @SuppressLint("NewApi")
    fun promptForPermissions(@StringRes title: Int, request: PermissionRequest, context: Context) {
        val allowedPermissions = request.resources.map {
            it to false
        }.toMutableList()
        MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMultiChoiceItems(
                        request.resources.map { webPermissionToHumanReadable(it, context) }.toTypedArray(), null
                ) { _, which, isChecked ->
                    allowedPermissions[which] = allowedPermissions[which].first to isChecked
                }
                .setPositiveButton(R.string.room_widget_resource_grant_permission) { _, _ ->
                    request.grant(allowedPermissions.mapNotNull { perm ->
                        perm.first.takeIf { perm.second }
                    }.toTypedArray())
                }
                .setNegativeButton(R.string.room_widget_resource_decline_permission) { _, _ ->
                    request.deny()
                }
                .show()
    }

    private fun webPermissionToHumanReadable(permission: String, context: Context): String {
        return when (permission) {
            PermissionRequest.RESOURCE_AUDIO_CAPTURE      -> context.getString(R.string.room_widget_webview_access_microphone)
            PermissionRequest.RESOURCE_VIDEO_CAPTURE      -> context.getString(R.string.room_widget_webview_access_camera)
            PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> context.getString(R.string.room_widget_webview_read_protected_media)
            else                                          -> permission
        }
    }
}
