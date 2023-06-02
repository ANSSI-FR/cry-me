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


import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OlmPRGUtilityTest {
    private static final String LOG_TAG = "OlmPRGUtilityTest";

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
     * Test init PRG
     */
    @Test
    public void testPRG() throws OlmException, UnsupportedEncodingException {
        OlmPRGUtility prg = OlmPRGUtility.getInstance();
        //prg.initPRG();

        OlmPRGUtility prg2 = OlmPRGUtility.getInstance();

        assertTrue(prg == prg2);

        for(int i=0; i<100; i++){
            byte[] b = prg.getRandomBytes(20);
            assertNotNull(b);
            assertTrue(b.length == 20);
            System.out.println(OlmUtility.byteArraytoHexString(b));
        }

        OlmPRGUtility.releasePRGUtility();
        assertTrue(OlmPRGUtility.isReleased());

        OlmPRGUtility prg3 = OlmPRGUtility.getInstance();
        assertNotEquals(prg, prg3);
        assertTrue(prg != prg3);
        assertTrue(prg2 != prg3);
    }

}
