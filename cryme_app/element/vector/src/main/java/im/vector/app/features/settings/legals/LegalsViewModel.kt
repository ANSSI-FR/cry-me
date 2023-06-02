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
package im.vector.app.features.settings.legals

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.discovery.fetchHomeserverWithTerms
import im.vector.app.features.discovery.fetchIdentityServerWithTerms
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session

class LegalsViewModel @AssistedInject constructor(
        @Assisted initialState: LegalsState,
        private val session: Session,
        private val stringProvider: StringProvider
) : VectorViewModel<LegalsState, LegalsAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<LegalsViewModel, LegalsState> {
        override fun create(initialState: LegalsState): LegalsViewModel
    }

    companion object : MavericksViewModelFactory<LegalsViewModel, LegalsState> by hiltMavericksViewModelFactory()

    override fun handle(action: LegalsAction) {
        when (action) {
            LegalsAction.Refresh -> loadData()
        }.exhaustive
    }

    private fun loadData() = withState { state ->
        loadHomeserver(state)
        val url = session.identityService().getCurrentIdentityServerUrl()
        if (url.isNullOrEmpty()) {
            setState { copy(hasIdentityServer = false) }
        } else {
            setState { copy(hasIdentityServer = true) }
            loadIdentityServer(state)
        }
    }

    private fun loadHomeserver(state: LegalsState) {
        if (state.homeServer !is Success) {
            setState { copy(homeServer = Loading()) }
            viewModelScope.launch {
                runCatching { session.fetchHomeserverWithTerms(stringProvider.getString(R.string.resources_language)) }
                        .fold(
                                onSuccess = { setState { copy(homeServer = Success(it)) } },
                                onFailure = { setState { copy(homeServer = Fail(it)) } }
                        )
            }
        }
    }

    private fun loadIdentityServer(state: LegalsState) {
        if (state.identityServer !is Success) {
            setState { copy(identityServer = Loading()) }
            viewModelScope.launch {
                runCatching { session.fetchIdentityServerWithTerms(stringProvider.getString(R.string.resources_language)) }
                        .fold(
                                onSuccess = { setState { copy(identityServer = Success(it)) } },
                                onFailure = { setState { copy(identityServer = Fail(it)) } }
                        )
            }
        }
    }
}
