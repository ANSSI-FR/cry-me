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

import com.yubico.yubikit.core.application.CommandState;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Configures HMAC-SHA1 challenge response secret on YubiKey
 * ({@link YubiOtpSession#calculateHmacSha1(Slot, byte[], CommandState)} how to use it after configuration)
 */
public class HmacSha1SlotConfiguration extends BaseSlotConfiguration<HmacSha1SlotConfiguration> {
    private static final int HMAC_KEY_SIZE = 20;      // Size of OATH-HOTP key (key field + first 4 of UID field)

    static byte[] shortenHmacSha1Key(byte[] key) {
        if (key.length > 64) {
            // As per HMAC specification, shorten keys longer than BLOCKSIZE by hashing them.
            try {
                return MessageDigest.getInstance("SHA1").digest(key);
            } catch (NoSuchAlgorithmException e) {
                // Shouldn't happen
                throw new IllegalStateException();
            }
        }
        if (key.length > HMAC_KEY_SIZE) {
            throw new UnsupportedOperationException("HMAC-SHA1 key lengths >20 bytes are not supported");
        }
        return key;
    }

    /**
     * Creates a HMAC-SHA1 challenge-response configuration with default settings.
     *
     * @param secret the 20 bytes HMAC key to store
     */
    public HmacSha1SlotConfiguration(byte[] secret) {
        // Secret is packed into key and uid
        ByteBuffer.wrap(ByteBuffer.allocate(KEY_SIZE + UID_SIZE).put(shortenHmacSha1Key(secret)).array()).get(key).get(uid);

        updateFlags(TKTFLAG_CHAL_RESP, true);
        updateFlags(CFGFLAG_CHAL_HMAC, true);
        updateFlags(CFGFLAG_HMAC_LT64, true);
    }

    @Override
    protected HmacSha1SlotConfiguration getThis() {
        return this;
    }

    /**
     * Whether or not to require a user presence check for calculating the response.
     *
     * @param requireTouch if true, any attempt to calculate a response will cause the YubiKey to require touch (default: false)
     * @return the configuration for chaining
     */
    public HmacSha1SlotConfiguration requireTouch(boolean requireTouch) {
        return updateFlags(CFGFLAG_CHAL_BTN_TRIG, requireTouch);
    }

    /**
     * Whether or not challenges sent to this slot are less than 64 bytes long or not.
     *
     * @param lt64 if false, all challenges must be exactly 64 bytes long (default: true)
     * @return the configuration for chaining
     */
    public HmacSha1SlotConfiguration lt64(boolean lt64) {
        return updateFlags(CFGFLAG_HMAC_LT64, lt64);
    }
}
