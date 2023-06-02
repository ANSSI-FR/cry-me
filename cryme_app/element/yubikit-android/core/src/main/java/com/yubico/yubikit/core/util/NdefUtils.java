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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Parser that helps to extract OTP from nfc tag.
 */
public class NdefUtils {
    private static final byte[] HEADER = new byte[]{(byte) 0xd1, 0x55, 0x04}; // NDEF, URI, HTTPS
    private static final byte NDEF_RECORD = (byte) 0xd1;
    private static final byte TYPE_LENGTH = 0x01;
    private static final byte URL_TYPE = (byte) 0x55;
    private static final byte HTTPS_PROTOCOL = (byte) 0x04;
    private static final byte[] DOMAIN = "my.yubico.com".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NEO_REMAINDER_PREFIX = "/neo/".getBytes(StandardCharsets.UTF_8);

    /**
     * Returns the String payload portion (an OTP, for example) of a YubiKey's NDEF data.
     */
    public static String getNdefPayload(byte[] ndefData) {
        return new String(getNdefPayloadBytes(ndefData), StandardCharsets.UTF_8);
    }

    /**
     * Returns the byte payload portion (static password scan codes, for example) of a YubiKey's NDEF data.
     */
    public static byte[] getNdefPayloadBytes(byte[] ndefData) {
        ByteBuffer data = ByteBuffer.wrap(ndefData);
        byte record = data.get();
        byte typeLength = data.get();
        int dataLength = 0xff & data.get() - typeLength;
        byte recordType = data.get();
        byte protocol = data.get();

        if (record != NDEF_RECORD || typeLength != TYPE_LENGTH || recordType != URL_TYPE || protocol != HTTPS_PROTOCOL) {
            throw new IllegalArgumentException("Not a HTTPS URL NDEF record");
        }

        byte[] domain = new byte[DOMAIN.length];
        data.get(domain);
        if (!Arrays.equals(DOMAIN, domain)) {
            throw new IllegalArgumentException("Incorrect URL domain");
        }
        byte[] remaining = new byte[dataLength - DOMAIN.length];
        data.get(remaining);

        if (Arrays.equals(NEO_REMAINDER_PREFIX, Arrays.copyOf(remaining, NEO_REMAINDER_PREFIX.length))) {
            return Arrays.copyOfRange(remaining, NEO_REMAINDER_PREFIX.length, remaining.length);
        }
        for (int i = 0; i < remaining.length; i++) {
            if (remaining[i] == '#') {
                return Arrays.copyOfRange(remaining, i + 1, remaining.length);
            }
        }
        throw new IllegalArgumentException("Incorrect URL format");
    }
}
