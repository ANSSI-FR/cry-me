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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.matrix.android.sdk.internal.database.mapper

import com.squareup.moshi.Types
import io.realm.RealmList
import org.matrix.android.sdk.api.pushrules.Kind
import org.matrix.android.sdk.api.pushrules.rest.PushCondition
import org.matrix.android.sdk.api.pushrules.rest.PushRule
import org.matrix.android.sdk.internal.database.model.PushRuleEntity
import org.matrix.android.sdk.internal.di.MoshiProvider
import timber.log.Timber

internal object PushRulesMapper {

    private val moshiActionsAdapter = MoshiProvider.providesMoshi().adapter<List<Any>>(Types.newParameterizedType(List::class.java, Any::class.java))

//    private val listOfAnyAdapter: JsonAdapter<List<Any>> =
//            moshi.adapter<List<Any>>(Types.newParameterizedType(List::class.java, Any::class.java), kotlin.collections.emptySet(), "actions")

    fun mapContentRule(pushrule: PushRuleEntity): PushRule {
        return PushRule(
                actions = fromActionStr(pushrule.actionsStr),
                default = pushrule.default,
                enabled = pushrule.enabled,
                ruleId = pushrule.ruleId,
                conditions = listOf(
                        PushCondition(Kind.EventMatch.value, "content.body", pushrule.pattern)
                )
        )
    }

    private fun fromActionStr(actionsStr: String?): List<Any> {
        try {
            return actionsStr?.let { moshiActionsAdapter.fromJson(it) }.orEmpty()
        } catch (e: Throwable) {
            Timber.e(e, "## failed to map push rule actions <$actionsStr>")
            return emptyList()
        }
    }

    fun mapRoomRule(pushrule: PushRuleEntity): PushRule {
        return PushRule(
                actions = fromActionStr(pushrule.actionsStr),
                default = pushrule.default,
                enabled = pushrule.enabled,
                ruleId = pushrule.ruleId,
                conditions = listOf(
                        PushCondition(Kind.EventMatch.value, "room_id", pushrule.ruleId)
                )
        )
    }

    fun mapSenderRule(pushrule: PushRuleEntity): PushRule {
        return PushRule(
                actions = fromActionStr(pushrule.actionsStr),
                default = pushrule.default,
                enabled = pushrule.enabled,
                ruleId = pushrule.ruleId,
                conditions = listOf(
                        PushCondition(Kind.EventMatch.value, "user_id", pushrule.ruleId)
                )
        )
    }

    fun map(pushrule: PushRuleEntity): PushRule {
        return PushRule(
                actions = fromActionStr(pushrule.actionsStr),
                default = pushrule.default,
                enabled = pushrule.enabled,
                ruleId = pushrule.ruleId,
                conditions = pushrule.conditions?.map { PushConditionMapper.map(it) }
        )
    }

    fun map(pushRule: PushRule): PushRuleEntity {
        return PushRuleEntity(
                actionsStr = moshiActionsAdapter.toJson(pushRule.actions),
                default = pushRule.default ?: false,
                enabled = pushRule.enabled,
                ruleId = pushRule.ruleId,
                pattern = pushRule.pattern,
                conditions = pushRule.conditions?.let {
                    RealmList(*pushRule.conditions.map { PushConditionMapper.map(it) }.toTypedArray())
                } ?: RealmList()
        )
    }
}
