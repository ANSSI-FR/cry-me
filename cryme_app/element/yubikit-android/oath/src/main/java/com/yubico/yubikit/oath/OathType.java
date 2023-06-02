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

package com.yubico.yubikit.oath;

/**
 * Supported OATH variants for use with the OATH YubiKey application.
 */
public enum OathType {
    /**
     * OATH HOTP (event based), as defined in RFC 4226
     */
    HOTP((byte) 0x10),
    /**
     * OATH TOTP (time based), as defined in RFC 6238
     */
    TOTP((byte) 0x20);

    public final byte value;

    OathType(byte value) {
        this.value = value;
    }

    /**
     * Returns the OATH type corresponding to the given YKOATH TYPE constant.
     */
    public static OathType fromValue(byte value) {
        for (OathType type : OathType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Not a valid OathType: " + value);
    }

    /**
     * Returns the OATH type corresponding to the given name, as used in otpauth:// URIs.
     */
    public static OathType fromString(String value) {
        if ("hotp".equalsIgnoreCase(value)) {
            return HOTP;
        } else if ("totp".equalsIgnoreCase(value)) {
            return TOTP;
        }
        throw new IllegalArgumentException("Not a valid OathType: " + value);
    }
}
