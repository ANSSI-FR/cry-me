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

package com.yubico.yubikit.android.app.ui.piv

import android.util.SparseArray
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.yubico.yubikit.android.app.ui.YubiKeyViewModel
import com.yubico.yubikit.core.Logger
import com.yubico.yubikit.core.Transport
import com.yubico.yubikit.core.YubiKeyDevice
import com.yubico.yubikit.core.application.BadResponseException
import com.yubico.yubikit.core.smartcard.ApduException
import com.yubico.yubikit.core.smartcard.SmartCardConnection
import com.yubico.yubikit.oath.OathSession
import com.yubico.yubikit.piv.ManagementKeyType
import com.yubico.yubikit.piv.PivSession
import com.yubico.yubikit.piv.Slot
import java.security.cert.X509Certificate

class PivViewModel : YubiKeyViewModel<PivSession>() {
    /**
     * List of slots that we will show on demo UI
     */
    private val slots = listOf(Slot.AUTHENTICATION, Slot.SIGNATURE, Slot.KEY_MANAGEMENT, Slot.CARD_AUTH)

    /**
     * Map of credentials and codes received from keys (can be populated from multiple keys)
     */
    private val _certificates = MutableLiveData<SparseArray<X509Certificate>?>()
    val certificates: LiveData<SparseArray<X509Certificate>?> = _certificates

    var mgmtKeyType = ManagementKeyType.TDES
    var mgmtKey: ByteArray = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3, 4, 5, 6, 7, 8)

    override fun getSession(device: YubiKeyDevice, onError: (Throwable) -> Unit, callback: (PivSession) -> Unit) {
        device.requestConnection(SmartCardConnection::class.java) {
            try {
                callback(PivSession(it.value))
            } catch(e: Throwable) {
                onError(e)
            }
        }
    }

    override fun PivSession.updateState() {
        _certificates.postValue(SparseArray<X509Certificate>().apply {
            slots.forEach {
                try {
                    put(it.value, getCertificate(it))
                } catch (e: ApduException) {
                    Logger.d("Missing certificate: $it")
                } catch (e: BadResponseException) {
                    // Malformed cert loaded? Ignore but log:
                    Logger.e("Failed getting certificate $it", e)
                }
            }
        })
    }
}