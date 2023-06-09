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

package im.vector.app.core.services

import android.app.NotificationChannel
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import im.vector.app.R
import im.vector.app.features.call.audio.CallAudioManager.Mode
import im.vector.app.features.call.webrtc.WebRtcCallManager
import im.vector.app.features.notifications.NotificationUtils
import org.matrix.android.sdk.api.extensions.orFalse
import timber.log.Timber

class CallRingPlayerIncoming(
        context: Context,
        private val notificationUtils: NotificationUtils
) {

    private val applicationContext = context.applicationContext
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    private val VIBRATE_PATTERN = longArrayOf(0, 400, 600)

    fun start(fromBg: Boolean) {
        val audioManager = applicationContext.getSystemService<AudioManager>()
        val incomingCallChannel = notificationUtils.getChannelForIncomingCall(fromBg)
        val ringerMode = audioManager?.ringerMode
        if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            playRingtoneIfNeeded(incomingCallChannel)
        } else if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            vibrateIfNeeded(incomingCallChannel)
        }
    }

    private fun playRingtoneIfNeeded(incomingCallChannel: NotificationChannel?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && incomingCallChannel?.sound != null) {
            Timber.v("Ringtone already configured by notification channel")
            return
        }
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(applicationContext, ringtoneUri)
        Timber.v("Play ringtone for incoming call")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtone?.isLooping = true
        }
        ringtone?.play()
    }

    private fun vibrateIfNeeded(incomingCallChannel: NotificationChannel?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && incomingCallChannel?.shouldVibrate().orFalse()) {
            Timber.v("## Vibration already configured by notification channel")
            return
        }
        vibrator = applicationContext.getSystemService()
        Timber.v("Vibrate for incoming call")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrationEffect = VibrationEffect.createWaveform(VIBRATE_PATTERN, 0)
            vibrator?.vibrate(vibrationEffect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(VIBRATE_PATTERN, 0)
        }
    }

    fun stop() {
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        vibrator = null
    }
}

class CallRingPlayerOutgoing(
        context: Context,
        private val callManager: WebRtcCallManager
) {

    private val applicationContext = context.applicationContext

    private var player: MediaPlayer? = null

    fun start() {
        callManager.setAudioModeToCallType()
        player?.release()
        player = createPlayer()
        if (player != null) {
            try {
                if (player?.isPlaying == false) {
                    player?.start()
                    Timber.v("## VOIP Starting ringing outgoing")
                } else {
                    Timber.v("## VOIP already playing")
                }
            } catch (failure: Throwable) {
                Timber.e(failure, "## VOIP Failed to start ringing outgoing")
                player = null
            }
        }
    }

    private fun WebRtcCallManager.setAudioModeToCallType() {
        val callMode = if (currentCall.get()?.mxCall?.isVideoCall.orFalse()) Mode.VIDEO_CALL else Mode.AUDIO_CALL
        audioManager.setMode(callMode)
    }

    fun stop() {
        player?.release()
        player = null
    }

    private fun createPlayer(): MediaPlayer? {
        try {
            val mediaPlayer = MediaPlayer.create(applicationContext, R.raw.ring)

            mediaPlayer.setOnErrorListener(MediaPlayerErrorListener())
            mediaPlayer.isLooping = true
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        // TODO Change to ?
                        // .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                        // .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build())
            } else {
                @Suppress("DEPRECATION")
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_RING)
            }
            return mediaPlayer
        } catch (failure: Throwable) {
            Timber.e(failure, "Failed to create Call ring player")
            return null
        }
    }

    inner class MediaPlayerErrorListener : MediaPlayer.OnErrorListener {
        override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
            Timber.w("onError($mp, $what, $extra")
            player = null
            return false
        }
    }
}
