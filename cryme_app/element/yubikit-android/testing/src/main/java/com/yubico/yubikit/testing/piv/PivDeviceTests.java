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
package com.yubico.yubikit.testing.piv;

import com.yubico.yubikit.core.Logger;
import com.yubico.yubikit.core.application.BadResponseException;
import com.yubico.yubikit.core.smartcard.ApduException;
import com.yubico.yubikit.core.smartcard.SW;
import com.yubico.yubikit.core.util.StringUtils;
import com.yubico.yubikit.piv.InvalidPinException;
import com.yubico.yubikit.piv.KeyType;
import com.yubico.yubikit.piv.ManagementKeyType;
import com.yubico.yubikit.piv.PinPolicy;
import com.yubico.yubikit.piv.PivSession;
import com.yubico.yubikit.piv.Slot;
import com.yubico.yubikit.piv.TouchPolicy;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Arrays;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;

public class PivDeviceTests {
    private static final byte[] DEFAULT_MANAGEMENT_KEY = Hex.decode("010203040506070801020304050607080102030405060708");
    private static final char[] DEFAULT_PIN = "123456".toCharArray();
    private static final char[] DEFAULT_PUK = "12345678".toCharArray();

    private static final List<String> MESSAGE_DIGESTS = Arrays.asList("SHA-1", "SHA-224", "SHA-256", "SHA-384", "SHA-512");

    public static void testManagementKey(PivSession piv) throws BadResponseException, IOException, ApduException {
        byte[] key2 = Hex.decode("010203040102030401020304010203040102030401020304");

        Logger.d("Authenticate with the wrong key");
        try {
            piv.authenticate(ManagementKeyType.TDES, key2);
            Assert.fail("Authenticated with wrong key");
        } catch (ApduException e) {
            Assert.assertEquals(SW.SECURITY_CONDITION_NOT_SATISFIED, e.getSw());
        }

        Logger.d("Change management key");
        piv.authenticate(ManagementKeyType.TDES, DEFAULT_MANAGEMENT_KEY);
        piv.setManagementKey(ManagementKeyType.TDES, key2, false);

        Logger.d("Authenticate with the old key");
        try {
            piv.authenticate(ManagementKeyType.TDES, DEFAULT_MANAGEMENT_KEY);
            Assert.fail("Authenticated with wrong key");
        } catch (ApduException e) {
            Assert.assertEquals(SW.SECURITY_CONDITION_NOT_SATISFIED, e.getSw());
        }

        Logger.d("Change management key");
        piv.authenticate(ManagementKeyType.TDES, key2);
        piv.setManagementKey(ManagementKeyType.TDES, DEFAULT_MANAGEMENT_KEY, false);
    }

    public static void testPin(PivSession piv) throws ApduException, InvalidPinException, IOException, BadResponseException {
        // Ensure we only try this if the default management key is set.
        piv.authenticate(ManagementKeyType.TDES, DEFAULT_MANAGEMENT_KEY);

        Logger.d("Verify PIN");
        char[] pin2 = "123123".toCharArray();
        piv.verifyPin(DEFAULT_PIN);
        MatcherAssert.assertThat(piv.getPinAttempts(), CoreMatchers.equalTo(3));

        Logger.d("Verify with wrong PIN");
        try {
            piv.verifyPin(pin2);
            Assert.fail("Verify with wrong PIN");
        } catch (InvalidPinException e) {
            MatcherAssert.assertThat(e.getAttemptsRemaining(), CoreMatchers.equalTo(2));
            MatcherAssert.assertThat(piv.getPinAttempts(), CoreMatchers.equalTo(2));
        }

        Logger.d("Change PIN with wrong PIN");
        try {
            piv.changePin(pin2, DEFAULT_PIN);
            Assert.fail("Change PIN with wrong PIN");
        } catch (InvalidPinException e) {
            MatcherAssert.assertThat(e.getAttemptsRemaining(), CoreMatchers.equalTo(1));
            MatcherAssert.assertThat(piv.getPinAttempts(), CoreMatchers.equalTo(1));
        }

        Logger.d("Change PIN");
        piv.changePin(DEFAULT_PIN, pin2);
        piv.verifyPin(pin2);

        Logger.d("Verify with wrong PIN");
        try {
            piv.verifyPin(DEFAULT_PIN);
            Assert.fail("Verify with wrong PIN");
        } catch (InvalidPinException e) {
            MatcherAssert.assertThat(e.getAttemptsRemaining(), CoreMatchers.equalTo(2));
            MatcherAssert.assertThat(piv.getPinAttempts(), CoreMatchers.equalTo(2));
        }

        Logger.d("Change PIN");
        piv.changePin(pin2, DEFAULT_PIN);
    }

    public static void testPuk(PivSession piv) throws ApduException, InvalidPinException, IOException, BadResponseException {
        // Ensure we only try this if the default management key is set.
        piv.authenticate(ManagementKeyType.TDES, DEFAULT_MANAGEMENT_KEY);

        // Change PUK
        char[] puk2 = "12341234".toCharArray();
        piv.changePuk(DEFAULT_PUK, puk2);
        piv.verifyPin(DEFAULT_PIN);

        // Block PIN
        while (piv.getPinAttempts() > 0) {
            try {
                piv.verifyPin(puk2);
            } catch (InvalidPinException e) {
                //Re-run until blocked...
            }
        }

        // Verify PIN blocked
        try {
            piv.verifyPin(DEFAULT_PIN);
        } catch (InvalidPinException e) {
            MatcherAssert.assertThat(e.getAttemptsRemaining(), CoreMatchers.equalTo(0));
            MatcherAssert.assertThat(piv.getPinAttempts(), CoreMatchers.equalTo(0));
        }

        // Try unblock with wrong PUK
        try {
            piv.unblockPin(DEFAULT_PUK, DEFAULT_PIN);
            Assert.fail("Unblock with wrong PUK");
        } catch (InvalidPinException e) {
            MatcherAssert.assertThat(e.getAttemptsRemaining(), CoreMatchers.equalTo(2));
        }

        // Unblock PIN
        piv.unblockPin(puk2, DEFAULT_PIN);

        // Try to change PUK with wrong PUK
        try {
            piv.changePuk(DEFAULT_PUK, puk2);
            Assert.fail("Change PUK with wrong PUK");
        } catch (InvalidPinException e) {
            MatcherAssert.assertThat(e.getAttemptsRemaining(), CoreMatchers.equalTo(2));
        }

        // Change PUK
        piv.changePuk(puk2, DEFAULT_PUK);
    }

    public static void testSignAllHashes(PivSession piv, Slot slot, KeyType keyType, PublicKey publicKey) throws ApduException, NoSuchAlgorithmException, InvalidPinException, IOException, InvalidKeyException, BadResponseException {
        for (String hash : MESSAGE_DIGESTS) {
            testSign(piv, slot, keyType, publicKey, hash);
        }
    }

    public static void testSign(PivSession piv, Slot slot, KeyType keyType, PublicKey publicKey, String digest) throws NoSuchAlgorithmException, IOException, ApduException, InvalidPinException, InvalidKeyException, BadResponseException {
        byte[] message = "Hello world!".getBytes(StandardCharsets.UTF_8);

        String signatureAlgorithm = digest.replace("-", "") + "with";
        switch (keyType.params.algorithm) {
            case RSA:
                signatureAlgorithm += "RSA";
                break;
            case EC:
                signatureAlgorithm += "ECDSA";
                break;
        }

        Logger.d("Create signature");
        piv.verifyPin(DEFAULT_PIN);
        Signature sig = Signature.getInstance(signatureAlgorithm);
        byte[] signature = piv.sign(slot, keyType, message, sig);
        try {
            sig.initVerify(publicKey);
            sig.update(message);
            Assert.assertTrue("Verify signature", sig.verify(signature));
        } catch (InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    public static void testSign(PivSession piv, KeyType keyType) throws NoSuchAlgorithmException, IOException, ApduException, InvalidPinException, InvalidKeyException, BadResponseException, InvalidAlgorithmParameterException {
        piv.authenticate(ManagementKeyType.TDES, DEFAULT_MANAGEMENT_KEY);
        Logger.d("Generate key: " + keyType);
        PublicKey publicKey = piv.generateKey(Slot.SIGNATURE, keyType, PinPolicy.DEFAULT, TouchPolicy.DEFAULT);

        Security.addProvider(new BouncyCastleProvider());
        switch (keyType.params.algorithm) {
            case EC:
                testSign(piv, publicKey, Signature.getInstance("SHA1withECDSA"));
                testSign(piv, publicKey, Signature.getInstance("SHA256withECDSA"));
                testSign(piv, publicKey, Signature.getInstance("NONEwithECDSA"));
                testSign(piv, publicKey, Signature.getInstance("SHA3-256withECDSA"));
                break;
            case RSA:
                testSign(piv, publicKey, Signature.getInstance("SHA1withRSA"));
                testSign(piv, publicKey, Signature.getInstance("SHA256withRSA"));
                testSign(piv, publicKey, Signature.getInstance("SHA256withRSA/PSS"));

                // Test with custom parameter. We use a 0-length salt and ensure signatures are the same
                Signature deterministicPss = Signature.getInstance("SHA256withRSA/PSS");
                deterministicPss.setParameter(new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 0, 1));
                byte[] sig1 = testSign(piv, publicKey, deterministicPss);
                byte[] sig2 = testSign(piv, publicKey, deterministicPss);
                Assert.assertArrayEquals("PSS parameters not used, signatures are not identical!", sig1, sig2);
                break;
        }
    }

    public static byte[] testSign(PivSession piv, PublicKey publicKey, Signature signatureAlgorithm) throws NoSuchAlgorithmException, IOException, ApduException, InvalidPinException, BadResponseException {
        byte[] message = "Hello world!".getBytes(StandardCharsets.UTF_8);

        Logger.d("Create signature using " + signatureAlgorithm.getAlgorithm());
        piv.verifyPin(DEFAULT_PIN);
        byte[] signature = piv.sign(Slot.SIGNATURE, KeyType.fromKey(publicKey), message, signatureAlgorithm);
        try {
            signatureAlgorithm.initVerify(publicKey);
            signatureAlgorithm.update(message);
            Assert.assertTrue("Verify signature", signatureAlgorithm.verify(signature));
            Logger.d("Signature verified for: " + signatureAlgorithm.getAlgorithm());
            return signature;
        } catch (InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    public static void testDecrypt(PivSession piv, KeyType keyType) throws BadResponseException, IOException, ApduException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidPinException {
        if (keyType.params.algorithm != KeyType.Algorithm.RSA) {
            throw new IllegalArgumentException("Unsupported");
        }

        piv.authenticate(ManagementKeyType.TDES, DEFAULT_MANAGEMENT_KEY);
        Logger.d("Generate key: " + keyType);
        PublicKey publicKey = piv.generateKey(Slot.KEY_MANAGEMENT, keyType, PinPolicy.DEFAULT, TouchPolicy.DEFAULT);

        testDecrypt(piv, publicKey, Cipher.getInstance("RSA/ECB/PKCS1Padding"));
        testDecrypt(piv, publicKey, Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding"));
        testDecrypt(piv, publicKey, Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding"));
    }

    public static void testDecrypt(PivSession piv, PublicKey publicKey, Cipher cipher) throws BadResponseException, IOException, ApduException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidPinException {
        byte[] message = "Hello world!".getBytes(StandardCharsets.UTF_8);

        piv.authenticate(ManagementKeyType.TDES, DEFAULT_MANAGEMENT_KEY);
        Logger.d("Using cipher " + cipher.getAlgorithm());

        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] ct = cipher.doFinal(message);
        Logger.d("Cipher text " + ct.length + ": " + StringUtils.bytesToHex(ct));

        piv.verifyPin(DEFAULT_PIN);
        byte[] pt = piv.decrypt(Slot.KEY_MANAGEMENT, ct, cipher);

        Assert.assertArrayEquals(message, pt);
        Logger.d("Decrypt successful for " + cipher.getAlgorithm());
    }

    public static void testEcdh(PivSession piv, KeyType keyType) throws BadResponseException, IOException, ApduException, NoSuchAlgorithmException, InvalidKeyException, InvalidPinException {
        if (keyType.params.algorithm != KeyType.Algorithm.EC) {
            throw new IllegalArgumentException("Unsupported");
        }

        piv.authenticate(ManagementKeyType.TDES, DEFAULT_MANAGEMENT_KEY);
        PublicKey publicKey = piv.generateKey(Slot.AUTHENTICATION, keyType, PinPolicy.DEFAULT, TouchPolicy.DEFAULT);
        KeyPair peer = PivTestUtils.generateKey(keyType);

        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(peer.getPrivate());
        ka.doPhase(publicKey, true);
        byte[] expected = ka.generateSecret();

        piv.verifyPin(DEFAULT_PIN);
        byte[] secret = piv.calculateSecret(Slot.AUTHENTICATION, (ECPublicKey) peer.getPublic());

        Assert.assertArrayEquals(expected, secret);
    }

    public static void testImportKeys(PivSession piv) throws ApduException, BadResponseException, NoSuchAlgorithmException, IOException, InvalidPinException, InvalidKeyException, BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, SignatureException {
        for (KeyType keyType : KeyType.values()) {
            testImportKey(piv, PivTestUtils.loadKey(keyType));
            testImportKey(piv, PivTestUtils.generateKey(keyType));
        }
    }

    public static void testImportKey(PivSession piv, KeyPair keyPair) throws BadResponseException, IOException, ApduException, NoSuchAlgorithmException, InvalidPinException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidKeyException, SignatureException {
        Slot slot = Slot.AUTHENTICATION;
        piv.authenticate(ManagementKeyType.TDES, DEFAULT_MANAGEMENT_KEY);

        Logger.d("Import key in slot " + slot);
        KeyType keyType = piv.putKey(slot, keyPair.getPrivate(), PinPolicy.DEFAULT, TouchPolicy.DEFAULT);

        testSignAllHashes(piv, slot, keyType, keyPair.getPublic());
    }

    public static void testGenerateKeys(PivSession piv) throws BadResponseException, IOException, ApduException, InvalidPinException, NoSuchAlgorithmException, InvalidKeyException {
        for (KeyType keyType : KeyType.values()) {
            testGenerateKey(piv, keyType);
        }
    }

    public static void testGenerateKey(PivSession piv, KeyType keyType) throws BadResponseException, IOException, ApduException, InvalidPinException, NoSuchAlgorithmException, InvalidKeyException {
        Slot slot = Slot.AUTHENTICATION;
        piv.authenticate(ManagementKeyType.TDES, DEFAULT_MANAGEMENT_KEY);

        Logger.d("Generate an " + keyType + " key in slot " + slot);
        PublicKey pub = piv.generateKey(slot, keyType, PinPolicy.DEFAULT, TouchPolicy.DEFAULT);

        testSignAllHashes(piv, slot, keyType, pub);
    }
}
