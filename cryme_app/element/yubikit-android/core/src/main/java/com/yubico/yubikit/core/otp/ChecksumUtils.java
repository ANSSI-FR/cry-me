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

package com.yubico.yubikit.core.otp;

/**
 * Utility methods for calculating and verifying the CRC13239 checksum used by YubiKeys.
 */
public class ChecksumUtils {
    // When verifying a checksum the CRC_OK_RESIDUAL should be the remainder.
    private static final short CRC_OK_RESIDUAL = (short) 0xf0b8;

    /**
     * Calculate the CRC13239 checksum for a byte buffer.
     *
     * @param data   byte buffer to be checksummed.
     * @param length how much of the buffer should be checksummed
     * @return the calculated checksum
     */
    static public short calculateCrc(byte[] data, int length) {
        int crc = 0xffff;

        for (int index = 0; index < length; index++) {
            int i, j;
            crc ^= data[index] & 0xFF;
            for (i = 0; i < 8; i++) {
                j = crc & 1;
                crc >>= 1;
                if (j == 1) {
                    crc ^= 0x8408;
                }
            }
        }

        return (short) (crc & 0xFFFF);
    }

    /**
     * Verifies a checksum.
     *
     * @param data   the data, ending in the 2 byte CRC checksum to verify
     * @param length The length of the data, including the checksum at the end
     * @return true if the checksum is correct, false if not
     */
    static public boolean checkCrc(byte[] data, int length) {
        return calculateCrc(data, length) == CRC_OK_RESIDUAL;
    }

    private ChecksumUtils() {
        throw new IllegalStateException();
    }
}
