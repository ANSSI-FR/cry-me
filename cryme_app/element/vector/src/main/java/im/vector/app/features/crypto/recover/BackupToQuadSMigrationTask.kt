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

package im.vector.app.features.crypto.recover

import im.vector.app.R
import im.vector.app.core.platform.ViewModelTask
import im.vector.app.core.platform.WaitingViewData
import im.vector.app.core.resources.StringProvider
import org.matrix.android.sdk.api.NoOpMatrixCallback
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.crosssigning.KEYBACKUP_SECRET_SSSS_NAME
import org.matrix.android.sdk.api.session.securestorage.EmptyKeySigner
import org.matrix.android.sdk.api.session.securestorage.RawBytesKeySpec
import org.matrix.android.sdk.api.session.securestorage.SharedSecretStorageService
import org.matrix.android.sdk.api.session.securestorage.SsssKeyCreationInfo
import org.matrix.android.sdk.internal.crypto.crosssigning.toBase64NoPadding
import org.matrix.android.sdk.internal.crypto.keysbackup.deriveKey
import org.matrix.android.sdk.internal.crypto.keysbackup.util.computeRecoveryKey
import org.matrix.android.sdk.internal.crypto.keysbackup.util.extractCurveKeyFromRecoveryKey
import org.matrix.android.sdk.internal.util.awaitCallback
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

class BackupToQuadSMigrationTask @Inject constructor(
        val session: Session,
        val stringProvider: StringProvider
) : ViewModelTask<BackupToQuadSMigrationTask.Params, BackupToQuadSMigrationTask.Result> {

    sealed class Result {
        object Success : Result()
        abstract class Failure(val throwable: Throwable?) : Result()
        object InvalidRecoverySecret : Failure(null)
        object NoKeyBackupVersion : Failure(null)
        object IllegalParams : Failure(null)
        class ErrorFailure(throwable: Throwable) : Failure(throwable)
    }

    data class Params(
            val passphrase: String?,
            val recoveryKey: String?,
            val progressListener: BootstrapProgressListener? = null
    )

    override suspend fun execute(params: Params): Result {
        try {
            // We need to use the current secret for keybackup and use it as the new master key for SSSS
            // Then we need to put back the backup key in sss
            val keysBackupService = session.cryptoService().keysBackupService()
            val quadS = session.sharedSecretStorageService

            val version = keysBackupService.keysBackupVersion ?: return Result.NoKeyBackupVersion

            reportProgress(params, R.string.bootstrap_progress_checking_backup)
            val curveKey =
                    (if (params.recoveryKey != null) {
                        extractCurveKeyFromRecoveryKey(params.recoveryKey)
                    } else if (!params.passphrase.isNullOrEmpty() && version.getAuthDataAsMegolmBackupAuthData()?.privateKeySalt != null) {
                        version.getAuthDataAsMegolmBackupAuthData()?.let { authData ->
                            deriveKey(params.passphrase, authData.privateKeySalt!!, authData.privateKeyIterations!!)
                        }
                    } else null)
                            ?: return Result.IllegalParams

            reportProgress(params, R.string.bootstrap_progress_compute_curve_key)
            val recoveryKey = computeRecoveryKey(curveKey)

            val isValid = awaitCallback<Boolean> {
                keysBackupService.isValidRecoveryKeyForCurrentVersion(recoveryKey, it)
            }

            if (!isValid) return Result.InvalidRecoverySecret

            val info: SsssKeyCreationInfo =
                    when {
                        params.passphrase?.isNotEmpty() == true -> {
                            reportProgress(params, R.string.bootstrap_progress_generating_ssss)
                            quadS.generateKeyWithPassphrase(
                                    UUID.randomUUID().toString(),
                                    "ssss_key",
                                    params.passphrase,
                                    EmptyKeySigner()
                            )
                        }
                        params.recoveryKey != null              -> {
                            reportProgress(params, R.string.bootstrap_progress_generating_ssss_recovery)
                            quadS.generateKey(
                                    UUID.randomUUID().toString(),
                                    extractCurveKeyFromRecoveryKey(params.recoveryKey)?.let { RawBytesKeySpec(it) },
                                    "ssss_key",
                                    EmptyKeySigner()
                            )
                        }
                        else                                    -> {
                            return Result.IllegalParams
                        }
                    }

            // Ok, so now we have migrated the old keybackup secret as the quadS key
            // Now we need to store the keybackup key in SSSS in a compatible way
            reportProgress(params, R.string.bootstrap_progress_storing_in_sss)
            quadS.storeSecret(
                    KEYBACKUP_SECRET_SSSS_NAME,
                    curveKey.toBase64NoPadding(),
                    listOf(SharedSecretStorageService.KeyRef(info.keyId, info.keySpec))
            )

            // save for gossiping
            keysBackupService.saveBackupRecoveryKey(recoveryKey, version.version)

            // while we are there let's restore, but do not block
            session.cryptoService().keysBackupService().restoreKeysWithRecoveryKey(
                    version,
                    recoveryKey,
                    null,
                    null,
                    null,
                    NoOpMatrixCallback()
            )

            return Result.Success
        } catch (failure: Throwable) {
            Timber.e(failure, "## BackupToQuadSMigrationTask - Failed to migrate backup")
            return Result.ErrorFailure(failure)
        }
    }

    private fun reportProgress(params: Params, stringRes: Int) {
        params.progressListener?.onProgress(WaitingViewData(stringProvider.getString(stringRes)))
    }
}
