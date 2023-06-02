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
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.crypto.keysbackup

/**
 * E2e keys backup states.
 *
 * <pre>
 *                               |
 *                               V        deleteKeyBackupVersion (on current backup)
 *  +---------------------->  UNKNOWN  <-------------
 *  |                            |
 *  |                            | checkAndStartKeysBackup (at startup or on new verified device or a new detected backup)
 *  |                            V
 *  |                     CHECKING BACKUP
 *  |                            |
 *  |    Network error           |
 *  +<----------+----------------+-------> DISABLED <----------------------+
 *  |           |                |            |                            |
 *  |           |                |            | createKeysBackupVersion    |
 *  |           V                |            V                            |
 *  +<---  WRONG VERSION         |         ENABLING                        |
 *      |       ^                |            |                            |
 *      |       |                V       ok   |     error                  |
 *      |       |     +------> READY <--------+----------------------------+
 *      V       |     |          |
 * NOT TRUSTED  |     |          | on new key
 *              |     |          V
 *              |     |     WILL BACK UP (waiting a random duration)
 *              |     |          |
 *              |     |          |
 *              |     | ok       V
 *              |     +----- BACKING UP
 *              |                |
 *              |      Error     |
 *              +<---------------+
 * </pre>
 */
enum class KeysBackupState {
    // Need to check the current backup version on the homeserver
    Unknown,

    // Checking if backup is enabled on homeserver
    CheckingBackUpOnHomeserver,

    // Backup has been stopped because a new backup version has been detected on the homeserver
    WrongBackUpVersion,

    // Backup from this device is not enabled
    Disabled,

    // There is a backup available on the homeserver but it is not trusted.
    // It is not trusted because the signature is invalid or the device that created it is not verified
    // Use [KeysBackup.getKeysBackupTrust()] to get trust details.
    // Consequently, the backup from this device is not enabled.
    NotTrusted,

    // Backup is being enabled: the backup version is being created on the homeserver
    Enabling,

    // Backup is enabled and ready to send backup to the homeserver
    ReadyToBackUp,

    // e2e keys are going to be sent to the homeserver
    WillBackUp,

    // e2e keys are being sent to the homeserver
    BackingUp
}
