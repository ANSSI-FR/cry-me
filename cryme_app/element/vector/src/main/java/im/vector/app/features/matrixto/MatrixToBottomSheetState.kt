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

package im.vector.app.features.matrixto

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.permalinks.PermalinkParser
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.util.MatrixItem

data class MatrixToBottomSheetState(
        val deepLink: String,
        val linkType: PermalinkData,
        val matrixItem: Async<MatrixItem> = Uninitialized,
        val startChattingState: Async<Unit> = Uninitialized,
        val roomPeekResult: Async<RoomInfoResult> = Uninitialized,
        val peopleYouKnow: Async<List<MatrixItem.UserItem>> = Uninitialized
) : MavericksState {

    constructor(args: MatrixToBottomSheet.MatrixToArgs) : this(
            deepLink = args.matrixToLink,
            linkType = PermalinkParser.parse(args.matrixToLink)
    )
}

sealed class RoomInfoResult {
    data class FullInfo(
            val roomItem: MatrixItem,
            val name: String,
            val topic: String,
            val memberCount: Int?,
            val alias: String?,
            val membership: Membership,
            val roomType: String?,
            val viaServers: List<String>?,
            val isPublic: Boolean
    ) : RoomInfoResult()

    data class PartialInfo(
            val roomId: String?,
            val viaServers: List<String>
    ) : RoomInfoResult()

    data class UnknownAlias(
            val alias: String?
    ) : RoomInfoResult()

    object NotFound : RoomInfoResult()
}
