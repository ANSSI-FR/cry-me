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
 * Copyright (C) 2019 Yubico.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yubico.yubikit.piv;

/**
 * The PIN policy of a private key defines whether or not a PIN is required to use the key.
 * <p>
 * Setting a PIN policy other than DEFAULT requires YubiKey 4 or later.
 */
public enum PinPolicy {
    /**
     * The default behavior for the particular key slot is used.
     */
    DEFAULT(0x0),

    /**
     * The PIN is never required for using the key.
     */
    NEVER(0x1),

    /**
     * The PIN must be verified for the session, prior to using the key.
     */
    ONCE(0x2),

    /**
     * The PIN must be verified each time the key is to be used, just prior to using it.
     */
    ALWAYS(0x3);

    public final int value;

    PinPolicy(int value) {
        this.value = value;
    }

    /**
     * Returns the PIN policy corresponding to the given PIV application constant.
     */
    public static PinPolicy fromValue(int value) {
        if (value >= 0 && value < PinPolicy.values().length) {
            return PinPolicy.values()[value];
        }
        throw new IllegalArgumentException("Not a valid PinPolicy :" + value);
    }
}
