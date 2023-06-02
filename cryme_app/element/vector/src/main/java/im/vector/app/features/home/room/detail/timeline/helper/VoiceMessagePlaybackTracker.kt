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

package im.vector.app.features.home.room.detail.timeline.helper

import android.os.Handler
import android.os.Looper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceMessagePlaybackTracker @Inject constructor() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = mutableMapOf<String, Listener>()
    private val activityListeners = mutableListOf<ActivityListener>()
    private val states = mutableMapOf<String, Listener.State>()

    fun trackActivity(listener: ActivityListener) {
        activityListeners.add(listener)
    }

    fun unTrackActivity(listener: ActivityListener) {
        activityListeners.remove(listener)
    }

    fun track(id: String, listener: Listener) {
        listeners[id] = listener

        val currentState = states[id] ?: Listener.State.Idle
        mainHandler.post {
            listener.onUpdate(currentState)
        }
    }

    fun unTrack(id: String) {
        listeners.remove(id)
    }

    fun makeAllPlaybacksIdle() {
        listeners.keys.forEach { key ->
            setState(key, Listener.State.Idle)
        }
    }

    /**
     * Set state and notify the listeners
     */
    private fun setState(key: String, state: Listener.State) {
        states[key] = state
        val isPlayingOrRecording = states.values.any { it is Listener.State.Playing || it is Listener.State.Recording }
        mainHandler.post {
            listeners[key]?.onUpdate(state)
            activityListeners.forEach { it.onUpdate(isPlayingOrRecording) }
        }
    }

    fun startPlayback(id: String) {
        val currentPlaybackTime = getPlaybackTime(id)
        val currentState = Listener.State.Playing(currentPlaybackTime)
        setState(id, currentState)
        // Pause any active playback
        states
                .filter { it.key != id }
                .keys
                .forEach { key ->
                    val state = states[key]
                    if (state is Listener.State.Playing) {
                        // Paused(state.playbackTime) state should also be considered later.
                        setState(key, Listener.State.Idle)
                    }
                }
    }

    fun pausePlayback(id: String) {
        val currentPlaybackTime = getPlaybackTime(id)
        setState(id, Listener.State.Paused(currentPlaybackTime))
    }

    fun stopPlayback(id: String) {
        setState(id, Listener.State.Idle)
    }

    fun updateCurrentPlaybackTime(id: String, time: Int) {
        setState(id, Listener.State.Playing(time))
    }

    fun updateCurrentRecording(id: String, amplitudeList: List<Int>) {
        setState(id, Listener.State.Recording(amplitudeList))
    }

    fun getPlaybackState(id: String) = states[id]

    fun getPlaybackTime(id: String): Int {
        return when (val state = states[id]) {
            is Listener.State.Playing -> state.playbackTime
            is Listener.State.Paused  -> state.playbackTime
            /* Listener.State.Idle, */
            else                      -> 0
        }
    }

    fun clear() {
        listeners.forEach {
            it.value.onUpdate(Listener.State.Idle)
        }
        listeners.clear()
        states.clear()
    }

    companion object {
        const val RECORDING_ID = "RECORDING_ID"
    }

    interface Listener {

        fun onUpdate(state: State)

        sealed class State {
            object Idle : State()
            data class Playing(val playbackTime: Int) : State()
            data class Paused(val playbackTime: Int) : State()
            data class Recording(val amplitudeList: List<Int>) : State()
        }
    }

    fun interface ActivityListener {
        fun onUpdate(isPlayingOrRecording: Boolean)
    }
}
