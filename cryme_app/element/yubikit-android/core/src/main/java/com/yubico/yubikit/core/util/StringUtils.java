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

package com.yubico.yubikit.core.util;

/**
 * Utility methods for Strings.
 */
public class StringUtils {
    /**
     * Helper method that convert byte array into string for logging
     *
     * @param byteArray array of bytes
     * @return string representation of byte array
     */
    public static String bytesToHex(byte[] byteArray) {
        return bytesToHex(byteArray, 0, byteArray.length);
    }

    /**
     * Helper method that convert byte array into string for logging
     *
     * @param byteArray array of bytes
     * @param offset    the offset within byteArray
     * @param size      the size of array
     * @return string representation of byte array
     */
    public static String bytesToHex(byte[] byteArray, int offset, int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < size; i++) {
            sb.append(String.format("%02x ", byteArray[i]));
        }
        return sb.toString();
    }

    private StringUtils() {
        throw new IllegalStateException();
    }
}
