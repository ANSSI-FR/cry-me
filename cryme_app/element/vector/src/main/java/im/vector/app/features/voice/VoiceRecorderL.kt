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
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.voice

import android.content.Context
import android.media.MediaRecorder
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.Level
import com.arthenica.ffmpegkit.ReturnCode
import im.vector.app.BuildConfig
import timber.log.Timber
import java.io.File

class VoiceRecorderL(context: Context) : AbstractVoiceRecorder(context, "mp4") {
    override fun setOutputFormat(mediaRecorder: MediaRecorder) {
        // Use AAC/MP4 format here
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
    }

    override fun convertFile(recordedFile: File?): File? {
        if (BuildConfig.DEBUG) {
            FFmpegKitConfig.setLogLevel(Level.AV_LOG_INFO)
        }
        recordedFile ?: return null
        // Convert to OGG
        val targetFile = File(recordedFile.path.removeSuffix("mp4") + "ogg")
        if (targetFile.exists()) {
            targetFile.delete()
        }
        val start = System.currentTimeMillis()
        val session = FFmpegKit.execute("-i \"${recordedFile.path}\" -c:a libvorbis \"${targetFile.path}\"")
        val duration = System.currentTimeMillis() - start
        Timber.d("Convert to ogg in $duration ms. Size in bytes from ${recordedFile.length()} to ${targetFile.length()}")
        return when {
            ReturnCode.isSuccess(session.returnCode) -> {
                // SUCCESS
                targetFile
            }
            ReturnCode.isCancel(session.returnCode)  -> {
                // CANCEL
                null
            }
            else                                     -> {
                // FAILURE
                Timber.e("Command failed with state ${session.state} and rc ${session.returnCode}.${session.failStackTrace}")
                // TODO throw?
                null
            }
        }
    }
}
