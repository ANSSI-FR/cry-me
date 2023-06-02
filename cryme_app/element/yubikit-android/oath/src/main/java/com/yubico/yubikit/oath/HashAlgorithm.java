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

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Supported hash algoritms for use with the OATH YubiKey application.
 */
public enum HashAlgorithm {
    SHA1((byte) 1, 64),
    SHA256((byte) 2, 64),
    SHA512((byte) 3, 128);

    // Pad the key to at least 14 bytes, as required by the YubiKey.
    private static final int MIN_KEY_SIZE = 14;

    public final byte value;
    public final int blockSize;

    HashAlgorithm(byte value, int blockSize) {
        this.value = value;
        this.blockSize = blockSize;
    }

    byte[] prepareKey(byte[] key) {
        if (key.length < MIN_KEY_SIZE) {
            return ByteBuffer.allocate(MIN_KEY_SIZE).put(key).array();
        } else if (key.length > blockSize) {
            try {
                return MessageDigest.getInstance(name()).digest(key);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        } else {
            return key;
        }
    }

    /**
     * Returns the algorithm corresponding to the given YKOATH ALGORITHM constant.
     */
    public static HashAlgorithm fromValue(byte value) {
        for (HashAlgorithm type : HashAlgorithm.values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Not a valid HashAlgorithm");
    }

    /**
     * Returns the algorithm corresponding to the given name, as used in otpauth:// URIs.
     */
    public static HashAlgorithm fromString(String value) {
        if (value == null || value.isEmpty()) {
            return HashAlgorithm.SHA1;  //This is the default value
        }
        if ("sha1".equalsIgnoreCase(value)) {
            return HashAlgorithm.SHA1;
        }
        if ("sha256".equalsIgnoreCase(value)) {
            return HashAlgorithm.SHA256;
        }
        if ("sha512".equalsIgnoreCase(value)) {
            return HashAlgorithm.SHA512;
        }
        throw new IllegalArgumentException("Not a valid HashAlgorithm");
    }
}
