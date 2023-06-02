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

package org.matrix.olm;

import android.text.TextUtils;
import android.util.Log;


import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.security.MessageDigest;
import java.util.Random;

@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OlmUtilityTest {
    private static final String LOG_TAG = "OlmUtilityTest";
    private static final int GENERATION_ONE_TIME_KEYS_NUMBER = 50;

    private static OlmManager mOlmManager;

    @BeforeClass
    public static void setUpClass() {
        // load native lib
        mOlmManager = new OlmManager();

        String version = mOlmManager.getOlmLibVersion();
        assertNotNull(version);
        Log.d(LOG_TAG, "## setUpClass(): lib version=" + version);
    }

    /**
     * Test the signing API
     */
    @Test
    public void test01VerifyWeiSig25519Signing() {
        String fingerPrintKey = null;
        String errorMsg = null;
        String message = "{\"algorithms\":[\"m.megolm.v1.aes-sha\",\"m.olm.v1.wei25519-aes-sha\"],\"device_id\":\"YMBYCWTWCG\",\"keys\":{\"wei25519:YMBYCWTWCG\":\"KZFa5YUXV2EOdhK8dcGMMHWB67stdgAP4+xwiS69mCU\",\"weisig25519:YMBYCWTWCG\":\"0cEgQJJqjgtXUGp4ZXQQmh36RAxwxr8HJw2E9v1gvA0\"},\"user_id\":\"@mxBob14774891254276b253f42-f267-43ec-bad9-767142bfea30:localhost:8480\"}";
        OlmAccount account = null;

        // create account
        try {
            account = new OlmAccount();
        } catch (OlmException e) {
            fail(e.getMessage());
        }
        assertNotNull(account);

        // sign message
        String messageSignature = null;

        try {
            messageSignature = account.signMessage(message);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(messageSignature);

        // get identities key (finger print key)
        Map<String, String> identityKeys = null;

        try {
            identityKeys = account.identityKeys();
        } catch (Exception e) {
            fail("identityKeys failed " + e.getMessage());
        }

        assertNotNull(identityKeys);
        fingerPrintKey = TestHelper.getFingerprintKey(identityKeys);
        assertFalse("fingerprint key missing", TextUtils.isEmpty(fingerPrintKey));

        // instantiate utility object
        OlmUtility utility = null;

        try {
            utility = new OlmUtility();
        } catch (Exception e) {
            fail("failed to create OlmUtility");
        }

        // verify signature
        errorMsg = null;
        try {
            utility.verifyWeiSig25519Signature(messageSignature, fingerPrintKey, message);
        } catch (Exception e) {
            errorMsg = e.getMessage();
        }
        assertTrue(TextUtils.isEmpty(errorMsg));

        // check a bad signature is detected => errorMsg = BAD_MESSAGE_MAC
        String badSignature = "Bad signature Bad signature Bad signature..";

        errorMsg = null;
        try {
            utility.verifyWeiSig25519Signature(badSignature, fingerPrintKey, message);
        } catch (Exception e) {
            errorMsg = e.getMessage();
        }
        assertFalse(TextUtils.isEmpty(errorMsg));

        // check bad fingerprint size => errorMsg = INVALID_BASE64
        String badSizeFingerPrintKey = fingerPrintKey.substring(fingerPrintKey.length() / 2);

        errorMsg = null;
        try {
            utility.verifyWeiSig25519Signature(messageSignature, badSizeFingerPrintKey, message);
        } catch (Exception e) {
            errorMsg = e.getMessage();
        }
        assertFalse(TextUtils.isEmpty(errorMsg));

        utility.releaseUtility();
        assertTrue(utility.isReleased());

        account.releaseAccount();
        assertTrue(account.isReleased());
    }

    @Test
    public void test02sha256() {
        OlmUtility utility = null;

        try {
            utility = new OlmUtility();
        } catch (Exception e) {
            fail("OlmUtility creation failed");
        }
        String msgToHash = "The quick brown fox jumps over the lazy dog";

        String hashResult = utility.sha3(msgToHash);
        assertFalse(TextUtils.isEmpty(hashResult));

        utility.releaseUtility();
        assertTrue(utility.isReleased());
    }

    @Test
    public void testSha1() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        OlmUtility utility = null;
        try {
            utility = new OlmUtility();
        } catch (Exception e) {
            fail("OlmUtility creation failed");
        }

        for(int i=0; i<100; i++) {
            byte[] array = new byte[20];
            new Random().nextBytes(array);
            String msgToHash = new String(array, "UTF8");

            // hash using olm
            String hashResult = utility.sha(msgToHash);
            assertFalse(TextUtils.isEmpty(hashResult));

            // hash using java
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            digest.update(msgToHash.getBytes("utf8"));
            String hashed = OlmUtility.byteArraytoHexString(digest.digest());

            // check equality
            assertEquals(hashed, hashResult);
        }

        utility.releaseUtility();
        assertTrue(utility.isReleased());
    }


    private byte[] deriveKeys(byte[] salt, int iterations, byte[] password) throws NoSuchAlgorithmException, NoSuchProviderException {

        SHA3Digest digest = new SHA3Digest(256);
        HMac prf = new HMac(digest);
        prf.init(new KeyParameter(password));

        // 256 bits key length
        byte[] uc = new byte[32];

        // U1 = PRF(Password, Salt || INT_32_BE(i))
        byte[] saltConcat = new byte[salt.length + 4];
        for(int i=0; i< salt.length; i++){
            saltConcat[i] = salt[i];
        }
        saltConcat[salt.length + 0] = 0;
        saltConcat[salt.length + 1] = 0;
        saltConcat[salt.length + 2] = 0;
        saltConcat[salt.length + 3] = 1;

        prf.update(saltConcat, 0, saltConcat.length);
        prf.doFinal(uc, 0);

        return uc;
    }

    @Test
    public void testEncryptBackup() throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchProviderException, OlmException {
        OlmUtility utility = new OlmUtility();

        for(int i=0; i<100; i++) {
            byte[] key = new byte[32];

            byte[] encryptionKey = new byte[32];

            new Random().nextBytes(key);
            new Random().nextBytes(encryptionKey);

            OlmBackupKeyEncryption msg = utility.encryptBackupKey(OlmUtility.byteArraytoHexString(key), OlmUtility.byteArraytoHexString(encryptionKey), "info");

            String k = utility.decryptBackupKey(msg, OlmUtility.byteArraytoHexString(encryptionKey), "info");
            System.out.println("cipher = " + msg.keyCiphertext);
            System.out.println("mac = " + msg.keyMac);
            System.out.println("iv = " + msg.keyIv);
            System.out.println("key = " + k);
            System.out.println("key2 = " + OlmUtility.byteArraytoHexString(key));
            System.out.flush();

            assertEquals(k, OlmUtility.byteArraytoHexString(key));

            byte[] encryptionKey2 = new byte[32];
            new Random().nextBytes(encryptionKey2);
            OlmBackupKeyEncryption msg2 = utility.encryptBackupKey(OlmUtility.byteArraytoHexString(key), OlmUtility.byteArraytoHexString(encryptionKey), "info");
            OlmException thrown = assertThrows(OlmException.class, () ->
                utility.decryptBackupKey(msg2, OlmUtility.byteArraytoHexString(encryptionKey2), "info"));

            assertEquals(thrown.getMessage(), "BAD_MESSAGE_MAC");
        }

        utility.releaseUtility();
        assertTrue(utility.isReleased());
    }


    @Test
    public void testTimeStamp() throws OlmException {
        OlmUtility utility = new OlmUtility();

        long s = System.currentTimeMillis();
        long t = utility.getTimestamp();
        System.out.println("time = " + s);
        System.out.println("time C = " + t);
        System.out.flush();


        utility.releaseUtility();
        assertTrue(utility.isReleased());
    }
}
