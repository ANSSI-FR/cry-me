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
package com.yubico.yubikit.yubiotp;

import java.util.Arrays;

/**
 * Configures the YubiKey to return YubiOTP (one-time password) on touch.
 */
public class YubiOtpSlotConfiguration extends KeyboardSlotConfiguration<YubiOtpSlotConfiguration> {
    /**
     * Creates a Yubico OTP configuration with default settings.
     *
     * @param fixed "public ID" static portion of each OTP (0-16 bytes)
     * @param uid   "private id" internal to the credential (6 bytes)
     * @param key   the AES key to encrypt the OTP payload (20 bytes)
     */
    public YubiOtpSlotConfiguration(byte[] fixed, byte[] uid, byte[] key) {
        if (fixed.length > FIXED_SIZE) {
            throw new IllegalArgumentException("Public ID must be <= 16 bytes");
        }

        this.fixed = Arrays.copyOf(fixed, fixed.length);
        System.arraycopy(uid, 0, this.uid, 0, uid.length);
        System.arraycopy(key, 0, this.key, 0, key.length);
    }

    @Override
    protected YubiOtpSlotConfiguration getThis() {
        return this;
    }

    /**
     * Inserts tabs in-between different parts of the OTP.
     *
     * @param before      inserts a tab before any other output (default: false)
     * @param afterFirst  inserts a tab after the static part of the OTP (default: false)
     * @param afterSecond inserts a tab after the end of the OTP (default: false)
     * @return the configuration for chaining
     */
    public YubiOtpSlotConfiguration tabs(boolean before, boolean afterFirst, boolean afterSecond) {
        updateFlags(TKTFLAG_TAB_FIRST, before);
        updateFlags(TKTFLAG_APPEND_TAB1, afterFirst);
        return updateFlags(TKTFLAG_APPEND_TAB2, afterSecond);
    }

    /**
     * Inserts delays in-between different parts of the OTP.
     *
     * @param afterFirst  inserts a delay after the static part of the OTP (default: false)
     * @param afterSecond inserts a delay after the end of the OTP (default: false)
     * @return the configuration for chaining
     */
    public YubiOtpSlotConfiguration delay(boolean afterFirst, boolean afterSecond) {
        updateFlags(TKTFLAG_APPEND_DELAY1, afterFirst);
        return updateFlags(TKTFLAG_APPEND_DELAY2, afterSecond);
    }

    /**
     * Send a reference string of all 16 modhex characters before the OTP.
     *
     * @param sendReference if true, sends the reference string (default: false)
     * @return the configuration for chaining
     */
    public YubiOtpSlotConfiguration sendReference(boolean sendReference) {
        return updateFlags(CFGFLAG_SEND_REF, sendReference);
    }
}
