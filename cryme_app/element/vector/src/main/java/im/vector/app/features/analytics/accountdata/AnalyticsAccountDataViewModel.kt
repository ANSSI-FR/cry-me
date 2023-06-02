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

package im.vector.app.features.analytics.accountdata

import androidx.lifecycle.asFlow
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.analytics.VectorAnalytics
import im.vector.app.features.analytics.log.analyticsTag
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.initsync.SyncStatusService
import org.matrix.android.sdk.flow.flow
import timber.log.Timber
import java.util.UUID

data class DummyState(
        val dummy: Boolean = false
) : MavericksState

class AnalyticsAccountDataViewModel @AssistedInject constructor(
        @Assisted initialState: DummyState,
        private val session: Session,
        private val analytics: VectorAnalytics
) : VectorViewModel<DummyState, EmptyAction, EmptyViewEvents>(initialState) {

    private var checkDone: Boolean = false

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<AnalyticsAccountDataViewModel, DummyState> {
        override fun create(initialState: DummyState): AnalyticsAccountDataViewModel
    }

    companion object : MavericksViewModelFactory<AnalyticsAccountDataViewModel, DummyState> by hiltMavericksViewModelFactory() {
        private const val ANALYTICS_EVENT_TYPE = "im.vector.analytics"
    }

    init {
        observeAccountData()
        observeInitSync()
    }

    private fun observeInitSync() {
        combine(
                session.getSyncStatusLive().asFlow(),
                analytics.getUserConsent(),
                analytics.getAnalyticsId()
        ) { status, userConsent, analyticsId ->
            if (status is SyncStatusService.Status.IncrementalSyncIdle &&
                    userConsent &&
                    analyticsId.isEmpty() &&
                    !checkDone) {
                // Initial sync is over, analytics Id from account data is missing and user has given consent to use analytics
                checkDone = true
                createAnalyticsAccountData()
            }
        }.launchIn(viewModelScope)
    }

    private fun observeAccountData() {
        session.flow()
                .liveUserAccountData(setOf(ANALYTICS_EVENT_TYPE))
                .mapNotNull { it.firstOrNull() }
                .mapNotNull { it.content.toModel<AnalyticsAccountDataContent>() }
                .onEach { analyticsAccountDataContent ->
                    if (analyticsAccountDataContent.id.isNullOrEmpty()) {
                        // Probably consent revoked from Element Web
                        // Ignore here
                        Timber.tag(analyticsTag.value).d("Consent revoked from Element Web?")
                    } else {
                        Timber.tag(analyticsTag.value).d("AnalyticsId has been retrieved")
                        analytics.setAnalyticsId(analyticsAccountDataContent.id)
                    }
                }
                .launchIn(viewModelScope)
    }

    override fun handle(action: EmptyAction) {
        // No op
    }

    private fun createAnalyticsAccountData() {
        val content = AnalyticsAccountDataContent(
                id = UUID.randomUUID().toString()
        )

        viewModelScope.launch {
            session.accountDataService().updateUserAccountData(ANALYTICS_EVENT_TYPE, content.toContent())
        }
    }
}
