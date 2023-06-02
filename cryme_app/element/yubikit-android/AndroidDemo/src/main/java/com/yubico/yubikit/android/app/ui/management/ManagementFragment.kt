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

package com.yubico.yubikit.android.app.ui.management

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.activityViewModels
import com.yubico.yubikit.android.app.R
import com.yubico.yubikit.android.app.databinding.FragmentManagementBinding
import com.yubico.yubikit.android.app.ui.YubiKeyFragment
import com.yubico.yubikit.core.Transport
import com.yubico.yubikit.management.Capability
import com.yubico.yubikit.management.DeviceConfig
import com.yubico.yubikit.management.ManagementSession

class ManagementFragment : YubiKeyFragment<ManagementSession, ManagementViewModel>() {
    override val viewModel: ManagementViewModel by activityViewModels()

    private lateinit var binding: FragmentManagementBinding

    private val checkboxIds = mapOf(
            (Transport.USB to Capability.OTP) to R.id.checkbox_usb_otp,
            (Transport.USB to Capability.U2F) to R.id.checkbox_usb_u2f,
            (Transport.USB to Capability.PIV) to R.id.checkbox_usb_piv,
            (Transport.USB to Capability.OATH) to R.id.checkbox_usb_oath,
            (Transport.USB to Capability.OPENPGP) to R.id.checkbox_usb_pgp,
            (Transport.USB to Capability.FIDO2) to R.id.checkbox_usb_fido2,

            (Transport.NFC to Capability.OTP) to R.id.checkbox_nfc_otp,
            (Transport.NFC to Capability.U2F) to R.id.checkbox_nfc_u2f,
            (Transport.NFC to Capability.PIV) to R.id.checkbox_nfc_piv,
            (Transport.NFC to Capability.OATH) to R.id.checkbox_nfc_oath,
            (Transport.NFC to Capability.OPENPGP) to R.id.checkbox_nfc_pgp,
            (Transport.NFC to Capability.FIDO2) to R.id.checkbox_nfc_fido2
    )

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.applicationTable.visibility = View.GONE
        binding.save.visibility = View.GONE

        viewModel.deviceInfo.observe(viewLifecycleOwner, {
            if (it != null) {
                binding.info.text = "Device type: ${it.formFactor.name} \nFirmware: ${it.version} \nSerial: ${it.serialNumber}"
                checkboxIds.forEach { (transport, capability), id ->
                    view.findViewById<CheckBox>(id).let { checkbox ->
                        if (it.getSupportedCapabilities(transport) and capability.bit != 0) {
                            checkbox.isChecked = (it.config.getEnabledCapabilities(transport)
                                    ?: 0) and capability.bit != 0
                            checkbox.visibility = View.VISIBLE
                        } else {
                            checkbox.visibility = View.GONE
                        }
                    }
                }
                binding.applicationTable.visibility = View.VISIBLE
                binding.save.visibility = View.VISIBLE
                binding.emptyView.visibility = View.GONE
            } else {
                binding.emptyView.visibility = View.VISIBLE
                binding.applicationTable.visibility = View.GONE
                binding.save.visibility = View.GONE
            }
        })

        binding.save.setOnClickListener {
            viewModel.pendingAction.value = {
                updateDeviceConfig(DeviceConfig.Builder().apply {
                    Transport.values().forEach { transport ->
                        enabledCapabilities(transport, checkboxIds.filter {
                            it.key.first == transport && view.findViewById<CheckBox>(it.value).isChecked
                        }.map {
                            it.key.second.bit  // Capability bit
                        }.sum())
                    }
                }.build(), true, null, null)

                "Configuration updated"
            }
        }
    }
}