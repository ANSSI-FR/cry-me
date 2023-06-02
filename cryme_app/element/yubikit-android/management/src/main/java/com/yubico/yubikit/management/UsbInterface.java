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
 * Copyright (C) 2020 Yubico.
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
 * Provides constants for the different YubiKey USB interfaces, and the Mode enum for combinations of enabled interfaces.
 */
public final class UsbInterface {
    public static final int OTP = 0x01;
    public static final int FIDO = 0x02;
    public static final int CCID = 0x04;

    private UsbInterface() {
    }

    /**
     * Used for configuring USB Mode for YubiKey 3 and 4.
     * <p>
     * This is replaced by DeviceConfig starting with YubiKey 5.
     */
    public enum Mode {
        OTP((byte) 0x00, UsbInterface.OTP),
        CCID((byte) 0x01, UsbInterface.CCID),
        OTP_CCID((byte) 0x02, UsbInterface.OTP | UsbInterface.CCID),
        FIDO((byte) 0x03, UsbInterface.FIDO),
        OTP_FIDO((byte) 0x04, UsbInterface.OTP | UsbInterface.FIDO),
        FIDO_CCID((byte) 0x05, UsbInterface.FIDO | UsbInterface.CCID),
        OTP_FIDO_CCID((byte) 0x06, UsbInterface.OTP | UsbInterface.FIDO | UsbInterface.CCID);

        public final byte value;
        public final int interfaces;

        Mode(byte value, int interfaces) {
            this.value = value;
            this.interfaces = interfaces;
        }

        /**
         * Returns the USB Mode given the enabled USB interfaces it has.
         */
        public static Mode getMode(int interfaces) {
            for (Mode mode : Mode.values()) {
                if (mode.interfaces == interfaces) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("Invalid interfaces for Mode");
        }
    }
}
