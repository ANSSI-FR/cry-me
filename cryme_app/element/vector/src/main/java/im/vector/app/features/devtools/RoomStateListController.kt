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

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.R
import im.vector.app.core.epoxy.charsequence.toEpoxyCharSequence
import im.vector.app.core.epoxy.noResultItem
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericItem
import me.gujun.android.span.span
import org.json.JSONObject
import javax.inject.Inject

class RoomStateListController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider
) : TypedEpoxyController<RoomDevToolViewState>() {

    var interactionListener: DevToolsInteractionListener? = null

    override fun buildModels(data: RoomDevToolViewState?) {
        val host = this
        when (data?.displayMode) {
            RoomDevToolViewState.Mode.StateEventList -> {
                val stateEventsGroups = data.stateEvents.invoke().orEmpty().groupBy { it.getClearType() }

                if (stateEventsGroups.isEmpty()) {
                    noResultItem {
                        id("no state events")
                        text(host.stringProvider.getString(R.string.no_result_placeholder))
                    }
                } else {
                    stateEventsGroups.forEach { entry ->
                        genericItem {
                            id(entry.key)
                            title(entry.key.toEpoxyCharSequence())
                            description(host.stringProvider.getQuantityString(R.plurals.entries, entry.value.size, entry.value.size).toEpoxyCharSequence())
                            itemClickAction {
                                host.interactionListener?.processAction(RoomDevToolAction.ShowStateEventType(entry.key))
                            }
                        }
                    }
                }
            }
            RoomDevToolViewState.Mode.StateEventListByType -> {
                val stateEvents = data.stateEvents.invoke().orEmpty().filter { it.type == data.currentStateType }
                if (stateEvents.isEmpty()) {
                    noResultItem {
                        id("no state events")
                        text(host.stringProvider.getString(R.string.no_result_placeholder))
                    }
                } else {
                    stateEvents.forEach { stateEvent ->
                        val contentJson = JSONObject(stateEvent.content.orEmpty()).toString().let {
                            if (it.length > 140) {
                                it.take(140) + Typography.ellipsis
                            } else {
                                it.take(140)
                            }
                        }
                        genericItem {
                            id(stateEvent.eventId)
                            title(span {
                                +"Type: "
                                span {
                                    textColor = host.colorProvider.getColorFromAttribute(R.attr.vctr_content_secondary)
                                    text = "\"${stateEvent.type}\""
                                    textStyle = "normal"
                                }
                                +"\nState Key: "
                                span {
                                    textColor = host.colorProvider.getColorFromAttribute(R.attr.vctr_content_secondary)
                                    text = stateEvent.stateKey.let { "\"$it\"" }
                                    textStyle = "normal"
                                }
                            }.toEpoxyCharSequence())
                            description(contentJson.toEpoxyCharSequence())
                            itemClickAction {
                                host.interactionListener?.processAction(RoomDevToolAction.ShowStateEvent(stateEvent))
                            }
                        }
                    }
                }
            }
            else                                           -> {
                // nop
            }
        }
    }
}
