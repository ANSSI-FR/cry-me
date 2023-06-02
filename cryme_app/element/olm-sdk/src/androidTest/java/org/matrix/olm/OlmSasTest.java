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

import android.util.Log;


import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OlmSasTest {

    private static OlmManager mOlmManager;

    //Enable the native lib
    @BeforeClass
    public static void setUpClass() {
        // load native librandomBytesOfLength
        mOlmManager = new OlmManager();
    }

    @Test
    public void testSASCode() {
        OlmSAS aliceSas = null;
        OlmSAS bobSas = null;

        try {
            aliceSas = new OlmSAS();
            bobSas = new OlmSAS();

            String alicePKey = aliceSas.getPublicKey();
            String bobPKey = bobSas.getPublicKey();

            Log.e(OlmSasTest.class.getSimpleName(), "#### Alice pub Key is " + alicePKey);
            Log.e(OlmSasTest.class.getSimpleName(), "#### Bob pub Key is " + bobPKey);

            aliceSas.setTheirPublicKey(bobPKey);
            bobSas.setTheirPublicKey(alicePKey);

            int codeLength = 6;
            byte[] alice_sas = aliceSas.generateShortCode("SAS", codeLength);
            byte[] bob_sas = bobSas.generateShortCode("SAS", codeLength);

            Log.e(OlmSasTest.class.getSimpleName(), "#### Alice SAS is " + new String(alice_sas, "UTF-8"));
            Log.e(OlmSasTest.class.getSimpleName(), "#### Bob SAS is " + new String(bob_sas, "UTF-8"));

            assertEquals(codeLength, alice_sas.length);
            assertEquals(codeLength, bob_sas.length);
            assertArrayEquals(alice_sas, bob_sas);

            String aliceMac = aliceSas.calculateMac("Hello world!", "SAS");
            String bobMac = bobSas.calculateMac("Hello world!", "SAS");

            assertEquals(aliceMac, bobMac);

            Log.e(OlmSasTest.class.getSimpleName(), "#### Alice Mac is " + aliceMac);
            Log.e(OlmSasTest.class.getSimpleName(), "#### Bob Mac is " + bobMac);

        } catch (Exception e) {
            fail("OlmSas init failed " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (aliceSas != null) {
                aliceSas.releaseSas();
            }
            if (bobSas != null) {
                bobSas.releaseSas();
            }
        }
    }

}
