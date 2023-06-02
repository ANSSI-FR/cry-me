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

import android.util.Base64;
import android.util.Log;


import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Random;
import static org.junit.Assert.assertArrayEquals;

@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OlmPkTest {
    private static final String LOG_TAG = "OlmPkEncryptionTest";

    private static OlmPkEncryption mOlmPkEncryption;
    private static OlmPkDecryption mOlmPkDecryption;
    private static OlmPkSigning mOlmPkSigning;
    private static OlmAccount mOlmAccount;
    private static OlmManager mOlmManager;

    @BeforeClass
    public static void setUpClass() {
        // load native lib
        mOlmManager = new OlmManager();

        String olmLibVersion = mOlmManager.getOlmLibVersion();
        assertNotNull(olmLibVersion);
        String olmSdkVersion = mOlmManager.getDetailedVersion(ApplicationProvider.getApplicationContext());
        assertNotNull(olmLibVersion);
        Log.d(LOG_TAG, "## setUpClass(): Versions - Android Olm SDK = " + olmSdkVersion + "  Olm lib =" + olmLibVersion);
    }

    @Test
    public void testPrivateKey() {
        try {
            mOlmPkDecryption = new OlmPkDecryption();
        } catch (OlmException e) {
            e.printStackTrace();
            fail("OlmPkEncryption failed " + e.getMessage());
        }

        assertNotNull(mOlmPkDecryption);

        byte[] privateKey = {
                (byte) 0x77, (byte) 0x07, (byte) 0x6D, (byte) 0x0A,
                (byte) 0x73, (byte) 0x18, (byte) 0xA5, (byte) 0x7D,
                (byte) 0x3C, (byte) 0x16, (byte) 0xC1, (byte) 0x72,
                (byte) 0x51, (byte) 0xB2, (byte) 0x66, (byte) 0x45,
                (byte) 0xDF, (byte) 0x4C, (byte) 0x2F, (byte) 0x87,
                (byte) 0xEB, (byte) 0xC0, (byte) 0x99, (byte) 0x2A,
                (byte) 0xB1, (byte) 0x77, (byte) 0xFB, (byte) 0xA5,
                (byte) 0x1D, (byte) 0xB9, (byte) 0x2C, (byte) 0x2A
        };

        assertEquals(privateKey.length, OlmPkDecryption.privateKeyLength());

        try {
            mOlmPkDecryption.setPrivateKey(privateKey);
        } catch (OlmException e) {
            fail("Exception in setPrivateKey, Exception code=" + e.getExceptionCode());
        }

        byte[] privateKeyCopy = null;

        try {
            privateKeyCopy = mOlmPkDecryption.privateKey();
        } catch (OlmException e) {
            fail("Exception in privateKey, Exception code=" + e.getExceptionCode());
        }

        assertArrayEquals(privateKey, privateKeyCopy);

        mOlmPkDecryption.releaseDecryption();
        assertTrue(mOlmPkDecryption.isReleased());
    }

    @Test
    public void testSigning() throws OlmException {
        try {
            mOlmPkSigning = new OlmPkSigning();
        } catch (OlmException e) {
            e.printStackTrace();
            fail("OlmPkSigning failed " + e.getMessage());
        }

        assertNotNull(mOlmPkSigning);

        byte[] seed = null;
        try {
            seed = mOlmPkSigning.generateKey();
        } catch (OlmException e) {
            e.printStackTrace();
            fail("generateSeed failed " + e.getMessage());
        }

        assertEquals(seed.length, 32);

        String pubkey = null;
        try {
            pubkey = mOlmPkSigning.initWithSeed(seed);
        } catch (OlmException e) {
            e.printStackTrace();
            fail("initWithSeed failed " + e.getMessage());
        }

        String message = "We hold these truths to be self-evident, that all men are created equal, that they are endowed by their Creator with certain unalienable Rights, that among these are Life, Liberty and the pursuit of Happiness.";

        String signature = null;
        try {
            signature = mOlmPkSigning.sign(message);
        } catch (OlmException e) {
            e.printStackTrace();
            fail("sign failed " + e.getMessage());
        }

        OlmUtility olmUtility = null;
        try {
            olmUtility = new OlmUtility();
        } catch (OlmException e) {
            e.printStackTrace();
            fail("olmUtility failed " + e.getMessage());
        }

        try {
            olmUtility.verifyWeiSig25519Signature(signature, pubkey, message);
        } catch (OlmException e) {
            e.printStackTrace();
            fail("Signature verification failed " + e.getMessage());
        }

        mOlmPkSigning.releaseSigning();
        assertTrue(mOlmPkSigning.isReleased());

        olmUtility.releaseUtility();
    }

}
