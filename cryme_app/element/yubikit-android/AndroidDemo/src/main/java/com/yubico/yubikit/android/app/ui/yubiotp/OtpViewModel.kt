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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.yubico.yubikit.android.app.ui.YubiKeyViewModel
import com.yubico.yubikit.core.YubiKeyDevice
import com.yubico.yubikit.yubiotp.ConfigurationState
import com.yubico.yubikit.yubiotp.YubiOtpSession


class OtpViewModel : YubiKeyViewModel<YubiOtpSession>() {
    private val _slotStatus = MutableLiveData<ConfigurationState?>()
    val slotConfigurationState: LiveData<ConfigurationState?> = _slotStatus

    override fun getSession(device: YubiKeyDevice, onError: (Throwable) -> Unit, callback: (YubiOtpSession) -> Unit) {
        YubiOtpSession.create(device) {
            try {
                callback(it.value)
            } catch (e: Throwable) {
                onError(e)
            }
        }
    }

    override fun YubiOtpSession.updateState() {
        _slotStatus.postValue(configurationState)
    }
}