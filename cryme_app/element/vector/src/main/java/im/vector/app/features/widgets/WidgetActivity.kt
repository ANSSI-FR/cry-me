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

package im.vector.app.features.widgets

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.view.isVisible
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.viewModel
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.ToolbarConfigurable
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityWidgetBinding
import im.vector.app.features.widgets.permissions.RoomWidgetPermissionBottomSheet
import im.vector.app.features.widgets.permissions.RoomWidgetPermissionViewEvents
import im.vector.app.features.widgets.permissions.RoomWidgetPermissionViewModel
import org.matrix.android.sdk.api.session.events.model.Content
import java.io.Serializable

@AndroidEntryPoint
class WidgetActivity : VectorBaseActivity<ActivityWidgetBinding>(),
        ToolbarConfigurable {

    companion object {
        private const val WIDGET_FRAGMENT_TAG = "WIDGET_FRAGMENT_TAG"
        private const val WIDGET_PERMISSION_FRAGMENT_TAG = "WIDGET_PERMISSION_FRAGMENT_TAG"
        private const val EXTRA_RESULT = "EXTRA_RESULT"

        fun newIntent(context: Context, args: WidgetArgs): Intent {
            return Intent(context, WidgetActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, args)
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun getOutput(intent: Intent): Content? {
            return intent.extras?.getSerializable(EXTRA_RESULT) as? Content
        }

        private fun createResultIntent(content: Content): Intent {
            return Intent().apply {
                putExtra(EXTRA_RESULT, content as Serializable)
            }
        }
    }

    private val viewModel: WidgetViewModel by viewModel()
    private val permissionViewModel: RoomWidgetPermissionViewModel by viewModel()

    override fun getBinding() = ActivityWidgetBinding.inflate(layoutInflater)

    override fun getMenuRes() = R.menu.menu_widget

    override fun getTitleRes() = R.string.room_widget_activity_title

    override fun initUiAndData() {
        val widgetArgs: WidgetArgs? = intent?.extras?.getParcelable(Mavericks.KEY_ARG)
        if (widgetArgs == null) {
            finish()
            return
        }
        configure(views.toolbar)
        views.toolbar.isVisible = widgetArgs.kind.nameRes != 0
        viewModel.observeViewEvents {
            when (it) {
                is WidgetViewEvents.Close -> handleClose(it)
            }
        }

        permissionViewModel.observeViewEvents {
            when (it) {
                is RoomWidgetPermissionViewEvents.Close -> finish()
            }
        }

        viewModel.onEach(WidgetViewState::status) { ws ->
            when (ws) {
                WidgetStatus.UNKNOWN            -> {
                }
                WidgetStatus.WIDGET_NOT_ALLOWED -> {
                    val dFrag = supportFragmentManager.findFragmentByTag(WIDGET_PERMISSION_FRAGMENT_TAG) as? RoomWidgetPermissionBottomSheet
                    if (dFrag != null && dFrag.dialog?.isShowing == true && !dFrag.isRemoving) {
                        return@onEach
                    } else {
                        RoomWidgetPermissionBottomSheet
                                .newInstance(widgetArgs)
                                .show(supportFragmentManager, WIDGET_PERMISSION_FRAGMENT_TAG)
                    }
                }
                WidgetStatus.WIDGET_ALLOWED     -> {
                    if (supportFragmentManager.findFragmentByTag(WIDGET_FRAGMENT_TAG) == null) {
                        addFragment(views.fragmentContainer, WidgetFragment::class.java, widgetArgs, WIDGET_FRAGMENT_TAG)
                    }
                }
            }
        }

        viewModel.onEach(WidgetViewState::widgetName) { name ->
            supportActionBar?.title = name
        }

        viewModel.onEach(WidgetViewState::canManageWidgets) {
            invalidateOptionsMenu()
        }
    }

    private fun handleClose(event: WidgetViewEvents.Close) {
        if (event.content != null) {
            val intent = createResultIntent(event.content)
            setResult(Activity.RESULT_OK, intent)
        }
        finish()
    }

    override fun configure(toolbar: MaterialToolbar) {
        configureToolbar(toolbar)
    }
}
