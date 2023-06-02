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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.yubico.yubikit.core.YubiKeyDevice
import java.io.Closeable

abstract class YubiKeyViewModel<Session : Closeable> : ViewModel() {
    private val _result = MutableLiveData<Result<String?>>(Result.success(null))
    val result: LiveData<Result<String?>> = _result

    val pendingAction = MutableLiveData<(Session.() -> String?)?>()

    abstract fun getSession(device: YubiKeyDevice, onError: (Throwable) -> Unit, callback: (Session) -> Unit): Unit
    abstract fun Session.updateState()

    fun onYubiKeyDevice(device: YubiKeyDevice) {
        getSession(device, { _result.postValue(Result.failure(it)) }) { session ->
            pendingAction.value?.let {
                _result.postValue(Result.runCatching { it(session) })
                pendingAction.postValue(null)
            }

            session.updateState()
        }
    }

    fun postResult(result: Result<String?>) {
        _result.postValue(result)
    }

    fun clearResult() {
        _result.value.let {
            if (it != null && (it.isFailure || it.getOrNull() != null)) {
                _result.postValue(Result.success(null))
            }
        }
    }
}