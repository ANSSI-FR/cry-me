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
package im.vector.app.features.terms

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Success
import im.vector.app.R
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.features.discovery.settingsSectionTitleItem
import javax.inject.Inject

class TermsController @Inject constructor(
        private val errorFormatter: ErrorFormatter
) : TypedEpoxyController<ReviewTermsViewState>() {

    var description: String? = null
    var listener: Listener? = null

    override fun buildModels(data: ReviewTermsViewState?) {
        data ?: return
        val host = this

        when (data.termsList) {
            is Incomplete -> {
                loadingItem {
                    id("loading")
                }
            }
            is Fail       -> {
                errorWithRetryItem {
                    id("errorRetry")
                    text(host.errorFormatter.toHumanReadable(data.termsList.error))
                    listener { host.listener?.retry() }
                }
            }
            is Success    -> buildTerms(data.termsList.invoke())
        }
    }

    private fun buildTerms(termsList: List<Term>) {
        val host = this
        settingsSectionTitleItem {
            id("header")
            titleResId(R.string.widget_integration_review_terms)
        }
        termsList.forEach { term ->
            termItem {
                id(term.url)
                name(term.name)
                description(host.description)
                checked(term.accepted)

                clickListener  { host.listener?.review(term) }
                checkChangeListener { _, isChecked ->
                    host.listener?.setChecked(term, isChecked)
                }
            }
        }
    }

    interface Listener {
        fun retry()
        fun setChecked(term: Term, isChecked: Boolean)
        fun review(term: Term)
    }
}
