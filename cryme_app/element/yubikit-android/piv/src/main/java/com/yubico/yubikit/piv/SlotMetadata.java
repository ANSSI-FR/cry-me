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
package com.yubico.yubikit.piv;

import java.security.PublicKey;
import java.util.Arrays;

/**
 * Metadata about a key in a slot.
 */
public class SlotMetadata {
    private final KeyType keyType;
    private final PinPolicy pinPolicy;
    private final TouchPolicy touchPolicy;
    private final boolean generated;
    private final byte[] publicKeyEncoded;

    public SlotMetadata(KeyType keyType, PinPolicy pinPolicy, TouchPolicy touchPolicy, boolean generated, byte[] publicKeyEncoded) {
        this.keyType = keyType;
        this.pinPolicy = pinPolicy;
        this.touchPolicy = touchPolicy;
        this.generated = generated;
        this.publicKeyEncoded = Arrays.copyOf(publicKeyEncoded, publicKeyEncoded.length);
    }

    /**
     * Returns the type of the key stored in a slot.
     */
    public KeyType getKeyType() {
        return keyType;
    }

    /**
     * Returns the PIN policy for using the key.
     */
    public PinPolicy getPinPolicy() {
        return pinPolicy;
    }

    /**
     * Returns the touch policy for using the key.
     */
    public TouchPolicy getTouchPolicy() {
        return touchPolicy;
    }

    /**
     * Whether the key was generated on the YubiKey or imported. A generated key can be attested,
     * and exists only in a single YubiKey.
     *
     * @return true if the key was generated on the YubiKey
     */
    public boolean isGenerated() {
        return generated;
    }

    /**
     * Returns the public key corresponding to the key in the slot.
     */
    public PublicKey getPublicKey() {
        return PivSession.parsePublicKeyFromDevice(keyType, publicKeyEncoded);
    }
}
