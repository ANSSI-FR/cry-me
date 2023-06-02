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

package com.yubico.yubikit.android.app.ui.oath

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.yubico.yubikit.android.app.R
import com.yubico.yubikit.android.app.databinding.FragmentOathBinding
import com.yubico.yubikit.android.app.ui.YubiKeyFragment
import com.yubico.yubikit.android.app.ui.getSecret
import com.yubico.yubikit.core.smartcard.ApduException
import com.yubico.yubikit.core.smartcard.SW
import com.yubico.yubikit.core.util.RandomUtils
import com.yubico.yubikit.oath.CredentialData
import com.yubico.yubikit.oath.HashAlgorithm
import com.yubico.yubikit.oath.OathSession
import com.yubico.yubikit.oath.OathType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.codec.binary.Base32

class OathFragment : YubiKeyFragment<OathSession, OathViewModel>() {
    override val viewModel: OathViewModel by activityViewModels()

    lateinit var binding: FragmentOathBinding
    lateinit var listAdapter: OathListAdapter

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOathBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listAdapter = OathListAdapter(object : OathListAdapter.ItemListener {
            override fun onDelete(credentialId: ByteArray) {
                lifecycleScope.launch(Dispatchers.Main) {
                    AlertDialog.Builder(requireContext())
                            .setTitle("Delete credential?")
                            .setPositiveButton("Delete") { _, _ ->
                                viewModel.pendingAction.value = {
                                    deleteCredential(credentialId)
                                    "Credential deleted"
                                }
                            }.setNegativeButton(android.R.string.cancel) { dialog, _ ->
                                dialog.cancel()
                            }.show()
                }
            }
        })
        with(binding.credentialList) {
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter
        }

        binding.swiperefresh.setOnRefreshListener {
            viewModel.pendingAction.value = { null }  // NOOP: Force credential refresh
            binding.swiperefresh.isRefreshing = false
        }

        viewModel.result.observe(viewLifecycleOwner, Observer { result ->
            result.onFailure { e ->
                if (e is ApduException && e.sw == SW.SECURITY_CONDITION_NOT_SATISFIED) {
                    viewModel.oathDeviceId.value?.let { deviceId ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            getSecret(requireContext(), R.string.enter_password, R.string.password)?.let {
                                viewModel.password = Pair(deviceId, it.toCharArray())
                            }
                        }
                    }
                }
            }
        })

        viewModel.credentials.observe(viewLifecycleOwner, Observer {
            listAdapter.submitList(it?.toList())
            binding.emptyView.visibility = if (it == null) View.VISIBLE else View.GONE
        })

        binding.textLayoutKey.setEndIconOnClickListener {
            binding.editTextKey.setText(Base32().encodeToString(RandomUtils.getRandomBytes(10)))
        }
        binding.editTextKey.setText(Base32().encodeToString(RandomUtils.getRandomBytes(10)))

        binding.btnSave.setOnClickListener {
            try {
                val secret = Base32().decode(binding.editTextKey.text.toString())
                val issuer = binding.editTextIssuer.text.toString()
                if (issuer.isBlank()) {
                    binding.editTextIssuer.error = "Issuer must not be empty"
                } else {
                    viewModel.pendingAction.value = {
                        putCredential(CredentialData("user@example.com", OathType.TOTP, HashAlgorithm.SHA1, secret, 6, 30, 0, issuer), false)
                        "Credential added"
                    }
                }
            } catch (e: Exception) {
                viewModel.postResult(Result.failure(e))
            }
        }
    }
}