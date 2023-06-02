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

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Methods for encoding and decoding Modhex encoded Strings.
 * <p>
 * See: <a href="https://developers.yubico.com/yubico-c/Manuals/modhex.1.htm">Modhex specification</a>.
 */
public class Modhex {
    private final static char[] ALPHABET = "cbdefghijklnrtuv".toCharArray();

    private static final Map<Character, Integer> table = new HashMap<>();

    static {
        for (int i = 0; i < ALPHABET.length; i++) {
            table.put(ALPHABET[i], i);
        }
    }

    /**
     * Decodes Modhex encoded string.
     */
    public static byte[] decode(String modhex) {
        if (modhex.length() % 2 != 0) {
            throw new IllegalArgumentException("Input string length is not a multiple of 2");
        }

        byte byteValue = 0;
        char[] chars = modhex.toLowerCase().toCharArray();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        for (int i = 0; i < chars.length; i++) {
            // find hex code for each symbol
            Integer code = table.get(chars[i]);
            if (code == null) {
                throw new IllegalArgumentException("Input string contains non-modhex character(s).");
            }

            // 2 symbols merged into 1 byte
            boolean shift = i % 2 == 0;
            if (shift) {
                byteValue = (byte) (code << 4);
            } else {
                byteValue |= code;
                outputStream.write(byteValue);
            }
        }
        return outputStream.toByteArray();
    }

    /**
     * Encodes data to Modhex.
     */
    public static String encode(byte[] bytes) {
        StringBuilder output = new StringBuilder();
        for (byte b : bytes) {
            output.append(ALPHABET[(b >> 4) & 0xF]).append(ALPHABET[b & 0xF]);
        }
        return output.toString();
    }
}
