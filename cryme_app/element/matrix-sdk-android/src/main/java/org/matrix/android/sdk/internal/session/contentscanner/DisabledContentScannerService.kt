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
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.contentscanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.matrix.android.sdk.api.session.contentscanner.ContentScannerService
import org.matrix.android.sdk.api.session.contentscanner.ScanStatusInfo
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.crypto.attachments.ElementToDecrypt
import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject

/**
 * Created to by-pass ProfileTask execution in LoginWizard.
 */
@SessionScope
internal class DisabledContentScannerService @Inject constructor() : ContentScannerService {

    override val serverPublicKey: String?
        get() = null

    override fun getContentScannerServer(): String? {
        return null
    }

    override suspend fun getServerPublicKey(forceDownload: Boolean): String? {
        return null
    }

    override suspend fun getScanResultForAttachment(mxcUrl: String, fileInfo: ElementToDecrypt?): ScanStatusInfo {
        TODO("Not yet implemented")
    }

    override fun setScannerUrl(url: String?) {
    }

    override fun enableScanner(enabled: Boolean) {
    }

    override fun isScannerEnabled(): Boolean {
        return false
    }

    override fun getLiveStatusForFile(mxcUrl: String, fetchIfNeeded: Boolean, fileInfo: ElementToDecrypt?): LiveData<Optional<ScanStatusInfo>> {
        return MutableLiveData()
    }

    override fun getCachedScanResultForFile(mxcUrl: String): ScanStatusInfo? {
        return null
    }
}
