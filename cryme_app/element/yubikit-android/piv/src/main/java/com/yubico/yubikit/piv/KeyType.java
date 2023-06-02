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

package com.yubico.yubikit.piv;

import java.math.BigInteger;
import java.security.Key;
import java.security.interfaces.ECKey;
import java.security.interfaces.RSAKey;
import java.security.spec.EllipticCurve;

import javax.annotation.Nonnull;

/**
 * Supported private key types for use with the PIV YubiKey application.
 */
public enum KeyType {
    /**
     * RSA with a 1024 bit key.
     */
    RSA1024((byte) 0x06, new RsaKeyParams(1024)),
    /**
     * RSA with a 2048 bit key.
     */
    RSA2048((byte) 0x07, new RsaKeyParams(2048)),
    /**
     * Elliptic Curve key, using NIST Curve P-256.
     */
    ECCP256((byte) 0x11, new EcKeyParams(
            256,
            "115792089210356248762697446949407573530086143415290314195533631308867097853948",
            "41058363725152142129326129780047268409114441015993725554835256314039467401291"
    )),
    /**
     * Elliptic Curve key, using NIST Cuve P-384.
     */
    ECCP384((byte) 0x14, new EcKeyParams(
            384,
            "39402006196394479212279040100143613805079739270465446667948293404245721771496870329047266088258938001861606973112316",
            "27580193559959705877849011840389048093056905856361568521428707301988689241309860865136260764883745107765439761230575"
    ));

    public final byte value;
    public final KeyParams params;

    KeyType(byte value, KeyParams params) {
        this.value = value;
        this.params = params;
    }

    /**
     * Returns the key type corresponding to the given PIV algorithm constant.
     */
    public static KeyType fromValue(int value) {
        for (KeyType type : KeyType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Not a valid KeyType:" + value);
    }

    /**
     * Returns the key type corresponding to the given key.
     */
    public static KeyType fromKey(Key key) {
        for (KeyType keyType : values()) {
            if (keyType.params.matches(key)) {
                return keyType;
            }
        }
        throw new IllegalArgumentException("Unsupported key type");
    }

    /**
     * Key algorithm identifier.
     */
    public enum Algorithm {
        RSA, EC
    }

    /**
     * Algorithm parameters used by a KeyType.
     */
    public static abstract class KeyParams {
        @Nonnull  // Needed for Kotlin to use when() on algorithm and not have to null check.
        public final Algorithm algorithm;
        public final int bitLength;

        private KeyParams(Algorithm algorithm, int bitLength) {
            this.algorithm = algorithm;
            this.bitLength = bitLength;
        }

        protected abstract boolean matches(Key key);
    }

    /**
     * Algorithm parameters for RSA keys.
     */
    public static final class RsaKeyParams extends KeyParams {
        private RsaKeyParams(int bitLength) {
            super(Algorithm.RSA, bitLength);
        }

        @Override
        protected boolean matches(Key key) {
            if (key instanceof RSAKey) {
                return ((RSAKey) key).getModulus().bitLength() == bitLength;
            }
            return false;
        }
    }

    /**
     * Algorithm parameters for EC keys.
     */
    public static final class EcKeyParams extends KeyParams {
        private final BigInteger a;
        private final BigInteger b;

        private EcKeyParams(int bitLength, String a, String b) {
            super(Algorithm.EC, bitLength);
            this.a = new BigInteger(a);
            this.b = new BigInteger(b);
        }

        @Override
        protected boolean matches(Key key) {
            if (key instanceof ECKey) {
                EllipticCurve curve = ((ECKey) key).getParams().getCurve();
                return curve.getField().getFieldSize() == bitLength && curve.getA().equals(a) && curve.getB().equals(b);
            }
            return false;
        }
    }
}
