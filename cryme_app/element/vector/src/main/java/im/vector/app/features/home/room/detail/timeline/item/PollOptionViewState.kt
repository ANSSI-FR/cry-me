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

package im.vector.app.features.home.room.detail.timeline.item

sealed class PollOptionViewState(open val optionId: String,
                                 open val optionAnswer: String) {
    /**
     * Represents a poll that is not sent to the server yet.
     */
    data class PollSending(override val optionId: String,
                           override val optionAnswer: String
    ) : PollOptionViewState(optionId, optionAnswer)

    /**
     * Represents a poll that is sent but not voted by the user
     */
    data class PollReady(override val optionId: String,
                         override val optionAnswer: String
    ) : PollOptionViewState(optionId, optionAnswer)

    /**
     * Represents a poll that user already voted.
     */
    data class PollVoted(override val optionId: String,
                         override val optionAnswer: String,
                         val voteCount: Int,
                         val votePercentage: Double,
                         val isSelected: Boolean
    ) : PollOptionViewState(optionId, optionAnswer)

    /**
     * Represents a poll that is ended.
     */
    data class PollEnded(override val optionId: String,
                         override val optionAnswer: String,
                         val voteCount: Int,
                         val votePercentage: Double,
                         val isWinner: Boolean
    ) : PollOptionViewState(optionId, optionAnswer)
}
