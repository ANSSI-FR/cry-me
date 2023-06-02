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
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.debug.features

import androidx.datastore.preferences.core.Preferences
import com.airbnb.epoxy.TypedEpoxyController
import javax.inject.Inject
import kotlin.reflect.KClass

data class FeaturesState(
        val features: List<Feature>
)

sealed interface Feature {

    data class EnumFeature<T : Enum<T>>(
            val label: String,
            val override: T?,
            val default: T,
            val options: List<T>,
            val type: KClass<T>
    ) : Feature

    data class BooleanFeature(
            val label: String,
            val featureOverride: Boolean?,
            val featureDefault: Boolean,
            val key: Preferences.Key<Boolean>
    ) : Feature
}

class FeaturesController @Inject constructor() : TypedEpoxyController<FeaturesState>() {

    var listener: Listener? = null

    override fun buildModels(data: FeaturesState?) {
        if (data == null) return

        data.features.forEachIndexed { index, feature ->
            when (feature) {
                is Feature.EnumFeature<*> -> enumFeatureItem {
                    id(index)
                    feature(feature)
                    listener(this@FeaturesController.listener)
                }
                is Feature.BooleanFeature -> booleanFeatureItem {
                    id(index)
                    feature(feature)
                    listener(this@FeaturesController.listener)
                }
            }
        }
    }

    interface Listener : EnumFeatureItem.Listener, BooleanFeatureItem.Listener
}
