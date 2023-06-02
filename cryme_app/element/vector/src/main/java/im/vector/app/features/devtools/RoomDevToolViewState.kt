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

package im.vector.app.features.devtools

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.events.model.Event

data class RoomDevToolViewState(
        val roomId: String = "",
        val displayMode: Mode = Mode.Root,
        val stateEvents: Async<List<Event>> = Uninitialized,
        val currentStateType: String? = null,
        val selectedEvent: Event? = null,
        val selectedEventJson: String? = null,
        val editedContent: String? = null,
        val modalLoading: Async<Unit> = Uninitialized,
        val sendEventDraft: SendEventDraft? = null
) : MavericksState {

    constructor(args: RoomDevToolActivity.Args) : this(roomId = args.roomId, displayMode = Mode.Root)

    sealed class Mode {
        object Root : Mode()
        object StateEventList : Mode()
        object StateEventListByType : Mode()
        object StateEventDetail : Mode()
        object EditEventContent : Mode()
        data class SendEventForm(val isState: Boolean) : Mode()
    }

    data class SendEventDraft(
            val type: String?,
            val stateKey: String?,
            val content: String?
    )
}
