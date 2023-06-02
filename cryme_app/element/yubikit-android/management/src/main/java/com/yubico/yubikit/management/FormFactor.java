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

package com.yubico.yubikit.management;

/**
 * The physical form factor of a YubiKey.
 */
public enum FormFactor {
    /**
     * Used when information about the YubiKey's form factor isn't available.
     */
    UNKNOWN(0x00),
    /**
     * A keychain-sized YubiKey with a USB-A connector.
     */
    USB_A_KEYCHAIN(0x01),
    /**
     * A nano-sized YubiKey with a USB-A connector.
     */
    USB_A_NANO(0x02),
    /**
     * A keychain-sized YubiKey with a USB-C connector.
     */
    USB_C_KEYCHAIN(0x03),
    /**
     * A nano-sized YubiKey with a USB-C connector.
     */
    USB_C_NANO(0x04),
    /**
     * A keychain-sized YubiKey with both USB-C and Lightning connectors.
     */
    USB_C_LIGHTNING(0x05),
    /**
     * A keychain-sized YubiKey with fingerprint sensor and USB-A connector.
     */
    USB_A_BIO(0x06),
    /**
     * A keychain-sized YubiKey with fingerprint sensor and USB-C connector.
     */
    USB_C_BIO(0x07);

    public final int value;

    FormFactor(int value) {
        this.value = value;
    }

    /**
     * Returns the form factor corresponding to the given Management application form factor constant, or UNKNOWN if the value is unknown.
     */
    public static FormFactor valueOf(int value) {
        value &= 0xf;
        if (value < FormFactor.values().length) {
            return FormFactor.values()[value];
        }
        return UNKNOWN;
    }
}
