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

package org.matrix.android.sdk.internal.crypto

/**
 * Matrix algorithm value for olm.
 */
const val MXCRYPTO_ALGORITHM_OLM = "m.olm.v1.wei25519-aes-sha"

/**
 * Matrix algorithm value for megolm.
 */
const val MXCRYPTO_ALGORITHM_MEGOLM = "m.megolm.v1.aes-sha"

/**
 * Matrix algorithm value for megolm keys backup.
 */
const val MXCRYPTO_ALGORITHM_MEGOLM_BACKUP = "m.megolm_backup.v1.wei25519-aes-sha"

/**
 * Secured Shared Storage algorithm constant
 */
const val SSSS_ALGORITHM_WEI25519_AES_SHA2 = "m.secret_storage.v1.wei25519-aes-sha"

/* Secrets are encrypted using AES-CTR-256 and MACed using HMAC-SHA-256. **/
const val SSSS_ALGORITHM_AES_HMAC_SHA2 = "m.secret_storage.v1.aes-hmac-sha2"

// TODO Refacto: use this constants everywhere
const val weisig25519 = "weisig25519"
const val wei25519 = "wei25519"
