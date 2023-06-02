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

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

class Padding {
    private static final String RAW_RSA = "RSA/ECB/NoPadding";
    private static final Pattern ECDSA_HASH_PATTERN = Pattern.compile("^(.+)withECDSA$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHA_PATTERN = Pattern.compile("^SHA[0-9]+$", Pattern.CASE_INSENSITIVE);

    /**
     * Prepares a message for signing.
     *
     * @param keyType   the type of key to use for signing
     * @param message   the message to sign
     * @param algorithm the signature algorithm to use
     * @return the payload ready to be signed
     * @throws NoSuchAlgorithmException if the algorithm isn't supported
     */
    static byte[] pad(KeyType keyType, byte[] message, Signature algorithm) throws NoSuchAlgorithmException {
        KeyType.KeyParams params = keyType.params;
        byte[] payload;
        switch (params.algorithm) {
            case RSA:
                // Sign using a dummy key
                KeyPairGenerator kpg = KeyPairGenerator.getInstance(params.algorithm.name());
                kpg.initialize(params.bitLength);
                KeyPair kp = kpg.generateKeyPair();
                try {
                    // Do a "raw encrypt" of the signature to get the padded message
                    algorithm.initSign(kp.getPrivate());
                    algorithm.update(message);
                    Cipher rsa = Cipher.getInstance(RAW_RSA);
                    rsa.init(Cipher.ENCRYPT_MODE, kp.getPublic());
                    payload = rsa.doFinal(algorithm.sign());
                } catch (SignatureException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException e) {
                    throw new IllegalStateException(e); // Shouldn't happen
                } catch (NoSuchPaddingException e) {
                    throw new UnsupportedOperationException("SecurityProvider doesn't support RSA without padding", e);
                }
                break;
            case EC:
                Matcher matcher = ECDSA_HASH_PATTERN.matcher(algorithm.getAlgorithm());
                if (!matcher.find()) {
                    throw new IllegalArgumentException("Invalid algorithm for given key");
                }
                String hashAlgorithm = matcher.group(1);
                byte[] hash;
                if ("NONE".equals(hashAlgorithm)) {
                    hash = message;
                } else {
                    if (SHA_PATTERN.matcher(hashAlgorithm).matches()) {
                        //SHAXYZ needs to be renamed to SHA-XYZ
                        hashAlgorithm = hashAlgorithm.replace("SHA", "SHA-");
                    }
                    hash = MessageDigest.getInstance(hashAlgorithm).digest(message);
                }
                int byteLength = params.bitLength / 8;
                if (hash.length > byteLength) {
                    // Truncate
                    payload = Arrays.copyOf(hash, byteLength);
                } else if (hash.length < byteLength) {
                    // Leftpad, with no external dependencies!
                    payload = new byte[byteLength];
                    System.arraycopy(hash, 0, payload, payload.length - hash.length, hash.length);
                } else {
                    payload = hash;
                }
                break;
            default:
                throw new IllegalArgumentException();
        }

        return payload;
    }


    /**
     * Prepares a message for signing.
     *
     * @param keyType   the type of key to use for signing
     * @param message   the message to sign
     * @param algorithm the signature algorithm to use
     * @return the payload ready to be signed
     * @throws NoSuchAlgorithmException if the algorithm isn't supported
     */
    static byte[] padChallenge(KeyType keyType, byte[] message, Signature algorithm) throws NoSuchAlgorithmException {
        KeyType.KeyParams params = keyType.params;
        byte[] payload = new byte[256];
        byte[] zeros = new byte[256 - message.length];

        for(int i = 0; i < zeros.length; ++i) {
            zeros[i] = 0;
        }

        System.arraycopy(zeros, 0, payload, 0, zeros.length);
        System.arraycopy(message, 0, payload, zeros.length, message.length);

        return payload;
    }

    /**
     * Verifies and removes padding from a decrypted RSA message.
     *
     * @param decrypted the decrypted (but still padded) payload
     * @param algorithm the cipher algorithm used for encryption
     * @return the un-padded plaintext
     * @throws NoSuchPaddingException   in case the padding algorithm isn't supported
     * @throws NoSuchAlgorithmException in case the algorithm isn't supported
     * @throws BadPaddingException      in case of a padding error
     */
    static byte[] unpad(byte[] decrypted, Cipher algorithm) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException {
        Cipher rsa = Cipher.getInstance(RAW_RSA);

        // Encrypt using a dummy key
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(KeyType.Algorithm.RSA.name());
        kpg.initialize(decrypted.length * 8);
        KeyPair kp = kpg.generateKeyPair();
        try {
            rsa.init(Cipher.ENCRYPT_MODE, kp.getPublic());
            algorithm.init(Cipher.DECRYPT_MODE, kp.getPrivate());
            return algorithm.doFinal(rsa.doFinal(decrypted));
        } catch (InvalidKeyException | IllegalBlockSizeException e) {
            throw new IllegalStateException(e); // Shouldn't happen
        }
    }
}
