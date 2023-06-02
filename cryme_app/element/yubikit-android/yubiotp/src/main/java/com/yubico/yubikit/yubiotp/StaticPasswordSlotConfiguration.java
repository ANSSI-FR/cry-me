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

import java.nio.ByteBuffer;

/**
 * Configures YubiKey to return static password on touch.
 */
public class StaticPasswordSlotConfiguration extends KeyboardSlotConfiguration<StaticPasswordSlotConfiguration> {
    private static final int SCAN_CODES_SIZE = FIXED_SIZE + UID_SIZE + KEY_SIZE;

    /**
     * Creates a Static Password configuration with default settings.
     *
     * @param scanCodes the password to store on YubiKey as an array of keyboard scan codes.
     */
    public StaticPasswordSlotConfiguration(byte[] scanCodes) {
        if (scanCodes.length > SCAN_CODES_SIZE) {
            throw new UnsupportedOperationException("Password is too long");
        }

        // Scan codes are packed into fixed, uid, and key, and zero padded.
        fixed = new byte[FIXED_SIZE];
        // NB: rewind() doesn't return a ByteBuffer before Java 9.
        ByteBuffer.wrap(ByteBuffer.allocate(SCAN_CODES_SIZE).put(scanCodes).array()).get(fixed).get(uid).get(key);

        updateFlags(CFGFLAG_SHORT_TICKET, true);
    }

    @Override
    protected StaticPasswordSlotConfiguration getThis() {
        return this;
    }
}
