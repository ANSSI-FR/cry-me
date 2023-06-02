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

package com.yubico.yubikit.android.app.ui.yubiotp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.yubico.yubikit.android.app.R
import com.yubico.yubikit.android.app.databinding.FragmentYubiotpChalrespBinding
import com.yubico.yubikit.core.util.RandomUtils
import com.yubico.yubikit.yubiotp.HmacSha1SlotConfiguration
import com.yubico.yubikit.yubiotp.Slot
import org.bouncycastle.util.encoders.Hex

class ChallengeResponseFragment : Fragment() {
    private val viewModel: OtpViewModel by activityViewModels()
    private lateinit var binding: FragmentYubiotpChalrespBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentYubiotpChalrespBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textLayoutKey.setEndIconOnClickListener {
            binding.editTextKey.setText(String(Hex.encode(RandomUtils.getRandomBytes(8))))
        }
        binding.editTextKey.setText(String(Hex.encode(RandomUtils.getRandomBytes(8))))

        binding.textLayoutChallenge.setEndIconOnClickListener {
            binding.editTextChallenge.setText(String(Hex.encode(RandomUtils.getRandomBytes(8))))
        }
        binding.editTextChallenge.setText(String(Hex.encode(RandomUtils.getRandomBytes(8))))

        binding.btnSave.setOnClickListener {
            try {
                val key = Hex.decode(binding.editTextKey.text.toString())
                val touch = binding.switchRequireTouch.isChecked
                val slot = when (binding.slotRadio.checkedRadioButtonId) {
                    R.id.radio_slot_1 -> Slot.ONE
                    R.id.radio_slot_2 -> Slot.TWO
                    else -> throw IllegalStateException("No slot selected")
                }
                viewModel.pendingAction.value = {
                    putConfiguration(slot, HmacSha1SlotConfiguration(key).requireTouch(touch), null, null)
                    "Slot $slot programmed"
                }
            } catch (e: Exception) {
                viewModel.postResult(Result.failure(e))
            }
        }

        binding.btnCalculateResponse.setOnClickListener {
            try {
                val challenge = Hex.decode(binding.editTextChallenge.text.toString())
                val slot = when (binding.slotCalculateRadio.checkedRadioButtonId) {
                    R.id.radio_calculate_slot_1 -> Slot.ONE
                    R.id.radio_calculate_slot_2 -> Slot.TWO
                    else -> throw IllegalStateException("No slot selected")
                }

                viewModel.pendingAction.value = {
                    val response = calculateHmacSha1(slot, challenge, null)
                    "Calculated response: " + String(Hex.encode(response))
                }
            } catch (e: java.lang.Exception) {
                viewModel.postResult(Result.failure(e))
            }
        }
    }
}