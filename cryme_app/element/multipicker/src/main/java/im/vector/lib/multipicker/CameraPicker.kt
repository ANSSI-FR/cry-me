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

package im.vector.lib.multipicker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import im.vector.lib.multipicker.entity.MultiPickerImageType
import im.vector.lib.multipicker.utils.toMultiPickerImageType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Implementation of taking a photo with Camera
 */
class CameraPicker {

    /**
     * Start camera by using a ActivityResultLauncher
     * @return Uri of taken photo or null if the operation is cancelled.
     */
    fun startWithExpectingFile(context: Context, activityResultLauncher: ActivityResultLauncher<Intent>): Uri? {
        val photoUri = createPhotoUri(context)
        val intent = createIntent().apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }
        activityResultLauncher.launch(intent)
        return photoUri
    }

    /**
     * Call this function from onActivityResult(int, int, Intent).
     * @return Taken photo or null if request code is wrong
     * or result code is not Activity.RESULT_OK
     * or user cancelled the operation.
     */
    fun getTakenPhoto(context: Context, photoUri: Uri): MultiPickerImageType? {
        return photoUri.toMultiPickerImageType(context)
    }

    private fun createIntent(): Intent {
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    }

    companion object {
        fun createPhotoUri(context: Context): Uri {
            val file = createImageFile(context)
            val authority = context.packageName + ".multipicker.fileprovider"
            return FileProvider.getUriForFile(context, authority, file)
        }

        private fun createImageFile(context: Context): File {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir: File = context.filesDir
            return File.createTempFile(
                    "${timeStamp}_", /* prefix */
                    ".jpg", /* suffix */
                    storageDir /* directory */
            )
        }
    }
}
