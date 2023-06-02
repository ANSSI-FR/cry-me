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

package im.vector.app.features.home.room.list

import androidx.annotation.StringRes
import androidx.lifecycle.asFlow
import im.vector.app.AppStateHandler
import im.vector.app.R
import im.vector.app.RoomGroupingMethod
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.RoomListDisplayMode
import im.vector.app.features.invite.AutoAcceptInvites
import im.vector.app.features.invite.showInvites
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.query.RoomCategoryFilter
import org.matrix.android.sdk.api.query.RoomTagQueryFilter
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.UpdatableLivePageResult
import org.matrix.android.sdk.api.session.room.model.Membership

class RoomListSectionBuilderGroup(
        private val coroutineScope: CoroutineScope,
        private val session: Session,
        private val stringProvider: StringProvider,
        private val appStateHandler: AppStateHandler,
        private val autoAcceptInvites: AutoAcceptInvites,
        private val onUpdatable: (UpdatableLivePageResult) -> Unit
) : RoomListSectionBuilder {

    override fun buildSections(mode: RoomListDisplayMode): List<RoomsSection> {
        val activeGroupAwareQueries = mutableListOf<UpdatableLivePageResult>()
        val sections = mutableListOf<RoomsSection>()
        val actualGroupId = appStateHandler.safeActiveGroupId()

        when (mode) {
            RoomListDisplayMode.PEOPLE        -> {
                // 4 sections Invites / Fav / Dms / Low Priority
                buildPeopleSections(sections, activeGroupAwareQueries, actualGroupId)
            }
            RoomListDisplayMode.ROOMS -> {
                // 6 sections invites / Fav / Rooms / Low Priority / Server notice / Suggested rooms
                buildRoomsSections(sections, activeGroupAwareQueries, actualGroupId)
            }
            RoomListDisplayMode.FILTERED      -> {
                // Used when searching for rooms
                withQueryParams(
                        {
                            it.memberships = Membership.activeMemberships()
                        },
                        { qpm ->
                            val name = stringProvider.getString(R.string.bottom_action_rooms)
                            session.getFilteredPagedRoomSummariesLive(qpm)
                                    .let { updatableFilterLivePageResult ->
                                        onUpdatable(updatableFilterLivePageResult)
                                        sections.add(RoomsSection(name, updatableFilterLivePageResult.livePagedList))
                                    }
                        }
                )
            }
            RoomListDisplayMode.NOTIFICATIONS -> {
                if (autoAcceptInvites.showInvites()) {
                    addSection(
                            sections,
                            activeGroupAwareQueries,
                            R.string.invitations_header,
                            true
                    ) {
                        it.memberships = listOf(Membership.INVITE)
                        it.roomCategoryFilter = RoomCategoryFilter.ALL
                        it.activeGroupId = actualGroupId
                    }
                }

                addSection(
                        sections,
                        activeGroupAwareQueries,
                        R.string.bottom_action_rooms,
                        false
                ) {
                    it.memberships = listOf(Membership.JOIN)
                    it.roomCategoryFilter = RoomCategoryFilter.ONLY_WITH_NOTIFICATIONS
                    it.activeGroupId = actualGroupId
                }
            }
        }

        appStateHandler.selectedRoomGroupingObservable
                .distinctUntilChanged()
                .onEach { groupingMethod ->
                    val selectedGroupId = (groupingMethod.orNull() as? RoomGroupingMethod.ByLegacyGroup)?.groupSummary?.groupId
                    activeGroupAwareQueries.onEach { updater ->
                        updater.updateQuery { query ->
                            query.copy(activeGroupId = selectedGroupId)
                        }
                    }
                }.launchIn(coroutineScope)

        return sections
    }

    private fun buildRoomsSections(sections: MutableList<RoomsSection>,
                                   activeSpaceAwareQueries: MutableList<UpdatableLivePageResult>,
                                   actualGroupId: String?) {
        if (autoAcceptInvites.showInvites()) {
            addSection(
                    sections,
                    activeSpaceAwareQueries,
                    R.string.invitations_header,
                    true
            ) {
                it.memberships = listOf(Membership.INVITE)
                it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
                it.activeGroupId = actualGroupId
            }
        }

        addSection(
                sections,
                activeSpaceAwareQueries,
                R.string.bottom_action_favourites,
                false
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
            it.roomTagQueryFilter = RoomTagQueryFilter(true, null, null)
            it.activeGroupId = actualGroupId
        }

        addSection(
                sections,
                activeSpaceAwareQueries,
                R.string.bottom_action_rooms,
                false
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
            it.roomTagQueryFilter = RoomTagQueryFilter(false, false, false)
            it.activeGroupId = actualGroupId
        }

        addSection(
                sections,
                activeSpaceAwareQueries,
                R.string.low_priority_header,
                false
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
            it.roomTagQueryFilter = RoomTagQueryFilter(null, true, null)
            it.activeGroupId = actualGroupId
        }

        addSection(
                sections,
                activeSpaceAwareQueries,
                R.string.system_alerts_header,
                false
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
            it.roomTagQueryFilter = RoomTagQueryFilter(null, null, true)
            it.activeGroupId = actualGroupId
        }
    }

    private fun buildPeopleSections(
            sections: MutableList<RoomsSection>,
            activeSpaceAwareQueries: MutableList<UpdatableLivePageResult>,
            actualGroupId: String?
    ) {
        if (autoAcceptInvites.showInvites()) {
            addSection(sections,
                    activeSpaceAwareQueries,
                    R.string.invitations_header,
                    true
            ) {
                it.memberships = listOf(Membership.INVITE)
                it.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
                it.activeGroupId = actualGroupId
            }
        }

        addSection(
                sections,
                activeSpaceAwareQueries,
                R.string.bottom_action_favourites,
                false
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
            it.roomTagQueryFilter = RoomTagQueryFilter(true, null, null)
            it.activeGroupId = actualGroupId
        }

        addSection(
                sections,
                activeSpaceAwareQueries,
                R.string.bottom_action_people_x,
                false
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
            it.roomTagQueryFilter = RoomTagQueryFilter(false, false, null)
            it.activeGroupId = actualGroupId
        }

        addSection(
                sections,
                activeSpaceAwareQueries,
                R.string.low_priority_header,
                false
        ) {
            it.memberships = listOf(Membership.JOIN)
            it.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
            it.roomTagQueryFilter = RoomTagQueryFilter(false, true, null)
            it.activeGroupId = actualGroupId
        }
    }

    private fun addSection(sections: MutableList<RoomsSection>,
                           activeSpaceUpdaters: MutableList<UpdatableLivePageResult>,
                           @StringRes nameRes: Int,
                           notifyOfLocalEcho: Boolean = false,
                           query: (RoomSummaryQueryParams.Builder) -> Unit) {
        withQueryParams(
                { query.invoke(it) },
                { roomQueryParams ->
                    val name = stringProvider.getString(nameRes)
                    session.getFilteredPagedRoomSummariesLive(roomQueryParams)
                            .also {
                                activeSpaceUpdaters.add(it)
                            }.livePagedList
                            .let { livePagedList ->
                                // use it also as a source to update count
                                livePagedList.asFlow()
                                        .onEach {
                                            sections.find { it.sectionName == name }
                                                    ?.notificationCount
                                                    ?.postValue(session.getNotificationCountForRooms(roomQueryParams))
                                        }
                                        .flowOn(Dispatchers.Default)
                                        .launchIn(coroutineScope)

                                sections.add(
                                        RoomsSection(
                                                sectionName = name,
                                                livePages = livePagedList,
                                                notifyOfLocalEcho = notifyOfLocalEcho
                                        )
                                )
                            }
                }

        )
    }

    private fun withQueryParams(builder: (RoomSummaryQueryParams.Builder) -> Unit, block: (RoomSummaryQueryParams) -> Unit) {
        RoomSummaryQueryParams.Builder()
                .apply { builder.invoke(this) }
                .build()
                .let { block(it) }
    }
}
