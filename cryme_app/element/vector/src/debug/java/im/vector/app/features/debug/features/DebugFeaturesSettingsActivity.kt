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

package im.vector.app.features.debug.features

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.FragmentGenericRecyclerBinding
import javax.inject.Inject

@AndroidEntryPoint
class DebugFeaturesSettingsActivity : VectorBaseActivity<FragmentGenericRecyclerBinding>() {

    @Inject lateinit var debugFeatures: DebugVectorFeatures
    @Inject lateinit var debugFeaturesStateFactory: DebugFeaturesStateFactory
    @Inject lateinit var controller: FeaturesController

    override fun getBinding() = FragmentGenericRecyclerBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller.listener = object : FeaturesController.Listener {
            override fun <T : Enum<T>> onEnumOptionSelected(option: T?, feature: Feature.EnumFeature<T>) {
                debugFeatures.overrideEnum(option, feature.type)
            }

            override fun onBooleanOptionSelected(option: Boolean?, feature: Feature.BooleanFeature) {
                debugFeatures.override(option, feature.key)
            }
        }
        views.genericRecyclerView.configureWith(controller)
        controller.setData(debugFeaturesStateFactory.create())
    }

    override fun onDestroy() {
        controller.listener = null
        views.genericRecyclerView.cleanup()
        super.onDestroy()
    }
}
