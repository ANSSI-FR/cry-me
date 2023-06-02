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
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room

import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.permalinks.ViaParameterFinder
import org.matrix.android.sdk.internal.session.room.accountdata.DefaultRoomAccountDataService
import org.matrix.android.sdk.internal.session.room.alias.DefaultAliasService
import org.matrix.android.sdk.internal.session.room.call.DefaultRoomCallService
import org.matrix.android.sdk.internal.session.room.draft.DefaultDraftService
import org.matrix.android.sdk.internal.session.room.membership.DefaultMembershipService
import org.matrix.android.sdk.internal.session.room.notification.DefaultRoomPushRuleService
import org.matrix.android.sdk.internal.session.room.read.DefaultReadService
import org.matrix.android.sdk.internal.session.room.relation.DefaultRelationService
import org.matrix.android.sdk.internal.session.room.reporting.DefaultReportingService
import org.matrix.android.sdk.internal.session.room.send.DefaultSendService
import org.matrix.android.sdk.internal.session.room.state.DefaultStateService
import org.matrix.android.sdk.internal.session.room.state.SendStateTask
import org.matrix.android.sdk.internal.session.room.summary.RoomSummaryDataSource
import org.matrix.android.sdk.internal.session.room.tags.DefaultTagsService
import org.matrix.android.sdk.internal.session.room.timeline.DefaultTimelineService
import org.matrix.android.sdk.internal.session.room.typing.DefaultTypingService
import org.matrix.android.sdk.internal.session.room.uploads.DefaultUploadsService
import org.matrix.android.sdk.internal.session.room.version.DefaultRoomVersionService
import org.matrix.android.sdk.internal.session.search.SearchTask
import javax.inject.Inject

internal interface RoomFactory {
    fun create(roomId: String): Room
}

@SessionScope
internal class DefaultRoomFactory @Inject constructor(private val cryptoService: CryptoService,
                                                      private val roomSummaryDataSource: RoomSummaryDataSource,
                                                      private val timelineServiceFactory: DefaultTimelineService.Factory,
                                                      private val sendServiceFactory: DefaultSendService.Factory,
                                                      private val draftServiceFactory: DefaultDraftService.Factory,
                                                      private val stateServiceFactory: DefaultStateService.Factory,
                                                      private val uploadsServiceFactory: DefaultUploadsService.Factory,
                                                      private val reportingServiceFactory: DefaultReportingService.Factory,
                                                      private val roomCallServiceFactory: DefaultRoomCallService.Factory,
                                                      private val readServiceFactory: DefaultReadService.Factory,
                                                      private val typingServiceFactory: DefaultTypingService.Factory,
                                                      private val aliasServiceFactory: DefaultAliasService.Factory,
                                                      private val tagsServiceFactory: DefaultTagsService.Factory,
                                                      private val relationServiceFactory: DefaultRelationService.Factory,
                                                      private val membershipServiceFactory: DefaultMembershipService.Factory,
                                                      private val roomPushRuleServiceFactory: DefaultRoomPushRuleService.Factory,
                                                      private val roomVersionServiceFactory: DefaultRoomVersionService.Factory,
                                                      private val roomAccountDataServiceFactory: DefaultRoomAccountDataService.Factory,
                                                      private val sendStateTask: SendStateTask,
                                                      private val viaParameterFinder: ViaParameterFinder,
                                                      private val searchTask: SearchTask,
                                                      private val coroutineDispatchers: MatrixCoroutineDispatchers) :
        RoomFactory {

    override fun create(roomId: String): Room {
        return DefaultRoom(
                roomId = roomId,
                roomSummaryDataSource = roomSummaryDataSource,
                timelineService = timelineServiceFactory.create(roomId),
                sendService = sendServiceFactory.create(roomId),
                draftService = draftServiceFactory.create(roomId),
                stateService = stateServiceFactory.create(roomId),
                uploadsService = uploadsServiceFactory.create(roomId),
                reportingService = reportingServiceFactory.create(roomId),
                roomCallService = roomCallServiceFactory.create(roomId),
                readService = readServiceFactory.create(roomId),
                typingService = typingServiceFactory.create(roomId),
                aliasService = aliasServiceFactory.create(roomId),
                tagsService = tagsServiceFactory.create(roomId),
                cryptoService = cryptoService,
                relationService = relationServiceFactory.create(roomId),
                roomMembersService = membershipServiceFactory.create(roomId),
                roomPushRuleService = roomPushRuleServiceFactory.create(roomId),
                roomAccountDataService = roomAccountDataServiceFactory.create(roomId),
                roomVersionService = roomVersionServiceFactory.create(roomId),
                sendStateTask = sendStateTask,
                searchTask = searchTask,
                viaParameterFinder = viaParameterFinder,
                coroutineDispatchers = coroutineDispatchers
        )
    }
}
