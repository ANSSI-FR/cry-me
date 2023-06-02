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

package com.yubico.yubikit.android.app.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.yubico.yubikit.android.app.MainViewModel
import com.yubico.yubikit.android.app.R
import com.yubico.yubikit.android.transport.nfc.NfcYubiKeyDevice
import com.yubico.yubikit.core.Logger
import com.yubico.yubikit.core.YubiKeyDevice
import com.yubico.yubikit.core.application.ApplicationNotAvailableException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable

abstract class YubiKeyFragment<App : Closeable, VM : YubiKeyViewModel<App>> : Fragment() {
    protected val activityViewModel: MainViewModel by activityViewModels()
    protected abstract val viewModel: VM

    private lateinit var yubiKeyPrompt: AlertDialog
    private lateinit var emptyText: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        emptyText = view.findViewById(R.id.empty_view)
        emptyText.visibility = View.VISIBLE

        yubiKeyPrompt = AlertDialog.Builder(context)
                .setTitle("Insert YubiKey")
                .setMessage(R.string.need_yubikey)
                .setOnCancelListener { viewModel.pendingAction.value = null }
                .create()

        activityViewModel.yubiKey.observe(viewLifecycleOwner, {
            if (it != null) {
                onYubiKey(it)
            } else {
                emptyText.setText(R.string.need_yubikey)
            }
        })

        viewModel.result.observe(viewLifecycleOwner, { result ->
            result.onSuccess {
                it?.let {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Logger.e("Error:", it)
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                if (it is ApplicationNotAvailableException) {
                    emptyText.setText(R.string.app_missing)
                }
            }
            viewModel.clearResult()
        })

        viewModel.pendingAction.observe(viewLifecycleOwner, {
            if (it != null) {
                activityViewModel.yubiKey.value.let { device ->
                    if (device != null) {
                        onYubiKey(device)
                    } else {
                        yubiKeyPrompt.setMessage(resources.getString(R.string.hold_key))
                        yubiKeyPrompt.show()
                    }
                }
            }
        })
    }

    override fun onPause() {
        if (yubiKeyPrompt.isShowing) {
            yubiKeyPrompt.dismiss()
        }
        super.onPause()
    }

    private fun onYubiKey(it: YubiKeyDevice) {
        lifecycleScope.launch {
            withContext(activityViewModel.singleDispatcher) {
                viewModel.onYubiKeyDevice(it)

                if (it is NfcYubiKeyDevice) {
                    withContext(Dispatchers.Main) {
                        if (yubiKeyPrompt.isShowing) {
                            yubiKeyPrompt.setMessage(resources.getString(R.string.remove_key))
                        } else {
                            emptyText.setText(R.string.remove_key)
                        }
                    }
                    it.remove(yubiKeyPrompt::dismiss);
                } else if (yubiKeyPrompt.isShowing) {
                    yubiKeyPrompt.dismiss()
                }
                Unit
            }
        }
    }
}