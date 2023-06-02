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

package im.vector.app.features.poll.create

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import org.matrix.android.sdk.api.session.Session

class CreatePollViewModel @AssistedInject constructor(
        @Assisted private val initialState: CreatePollViewState,
        session: Session
) : VectorViewModel<CreatePollViewState, CreatePollAction, CreatePollViewEvents>(initialState) {

    private val room = session.getRoom(initialState.roomId)!!

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<CreatePollViewModel, CreatePollViewState> {
        override fun create(initialState: CreatePollViewState): CreatePollViewModel
    }

    companion object : MavericksViewModelFactory<CreatePollViewModel, CreatePollViewState> by hiltMavericksViewModelFactory() {

        const val MIN_OPTIONS_COUNT = 2
        private const val MAX_OPTIONS_COUNT = 20
    }

    init {
        observeState()
    }

    private fun observeState() {
        onEach(
                CreatePollViewState::question,
                CreatePollViewState::options
        ) { question, options ->
            setState {
                copy(
                        canCreatePoll = canCreatePoll(question, options),
                        canAddMoreOptions = options.size < MAX_OPTIONS_COUNT
                )
            }
        }
    }

    override fun handle(action: CreatePollAction) {
        when (action) {
            CreatePollAction.OnCreatePoll         -> handleOnCreatePoll()
            CreatePollAction.OnAddOption          -> handleOnAddOption()
            is CreatePollAction.OnDeleteOption    -> handleOnDeleteOption(action.index)
            is CreatePollAction.OnOptionChanged   -> handleOnOptionChanged(action.index, action.option)
            is CreatePollAction.OnQuestionChanged -> handleOnQuestionChanged(action.question)
        }
    }

    private fun handleOnCreatePoll() = withState { state ->
        val nonEmptyOptions = state.options.filter { it.isNotEmpty() }
        when {
            state.question.isEmpty()                 -> {
                _viewEvents.post(CreatePollViewEvents.EmptyQuestionError)
            }
            nonEmptyOptions.size < MIN_OPTIONS_COUNT -> {
                _viewEvents.post(CreatePollViewEvents.NotEnoughOptionsError(requiredOptionsCount = MIN_OPTIONS_COUNT))
            }
            else                                     -> {
                room.sendPoll(state.question, nonEmptyOptions)
                _viewEvents.post(CreatePollViewEvents.Success)
            }
        }
    }

    private fun handleOnAddOption() {
        setState {
            val extendedOptions = options + ""
            copy(
                    options = extendedOptions
            )
        }
    }

    private fun handleOnDeleteOption(index: Int) {
        setState {
            val filteredOptions = options.filterIndexed { ind, _ -> ind != index }
            copy(
                    options = filteredOptions
            )
        }
    }

    private fun handleOnOptionChanged(index: Int, option: String) {
        setState {
            val changedOptions = options.mapIndexed { ind, s -> if (ind == index) option else s }
            copy(
                    options = changedOptions
            )
        }
    }

    private fun handleOnQuestionChanged(question: String) {
        setState {
            copy(
                    question = question
            )
        }
    }

    private fun canCreatePoll(question: String, options: List<String>): Boolean {
        return question.isNotEmpty() &&
                options.filter { it.isNotEmpty() }.size >= MIN_OPTIONS_COUNT
    }
}
