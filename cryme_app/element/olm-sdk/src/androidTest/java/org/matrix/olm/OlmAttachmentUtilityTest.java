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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.util.Base64;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;


@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OlmAttachmentUtilityTest {
    private static final String LOG_TAG = "OlmAttachmentUtilityTest";

    private static OlmManager mOlmManager;

    @BeforeClass
    public static void setUpClass() {
        // load native lib
        mOlmManager = new OlmManager();

        String version = mOlmManager.getOlmLibVersion();
        assertNotNull(version);
        Log.d(LOG_TAG, "## setUpClass(): lib version=" + version);
    }

    @Test
    public void testEncryption() throws OlmException, UnsupportedEncodingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        OlmAttachmentUtility olmAttachmentUtility = new OlmAttachmentUtility();
        assertNotNull(olmAttachmentUtility);

        for(int i=0; i<1; i++){
            byte[] sessionKeyBuff = new byte[128];
            new SecureRandom().nextBytes(sessionKeyBuff);

            String sessionKey = Base64.encodeToString(sessionKeyBuff, Base64.NO_PADDING | Base64.NO_WRAP);

            byte[] messageBuffer = new byte[500];
            new SecureRandom().nextBytes(messageBuffer);

            OlmAttachmentMessage msg = olmAttachmentUtility.encryptAttachment(messageBuffer, sessionKey);

            assertEquals(Base64.decode(msg.ciphertext, Base64.DEFAULT).length, messageBuffer.length);
            assertEquals(Base64.decode(msg.mac, Base64.DEFAULT).length, 20);
            assertEquals(Base64.decode(msg.ivAes, Base64.DEFAULT).length, 16);
            assertEquals(Base64.decode(msg.keyMac, Base64.DEFAULT).length, 32);
            assertEquals(Base64.decode(msg.keyAes, Base64.DEFAULT).length, 32);

            System.out.println("length = " + msg.ciphertextLength);
            ByteBuffer wrapped = ByteBuffer.wrap(Base64.decode(msg.ciphertextLength, Base64.DEFAULT));
            int num = wrapped.getInt();
            System.out.println("length = " + num);
            System.out.flush();
        }

        olmAttachmentUtility.releaseAttachmentUtility();
        assertTrue(olmAttachmentUtility.isReleased());
    }

    @Test
    public void testDecryption() throws OlmException, UnsupportedEncodingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        OlmAttachmentUtility olmAttachmentUtility = new OlmAttachmentUtility();
        assertNotNull(olmAttachmentUtility);

        for(int i=0; i<100; i++){
            byte[] sessionKeyBuff = new byte[128];
            new SecureRandom().nextBytes(sessionKeyBuff);

            String sessionKey = Base64.encodeToString(sessionKeyBuff, Base64.NO_PADDING | Base64.NO_WRAP);

            byte[] messageBuffer = new byte[4096];
            new SecureRandom().nextBytes(messageBuffer);

            OlmAttachmentMessage msg = olmAttachmentUtility.encryptAttachment(messageBuffer, sessionKey);

            assertEquals(Base64.decode(msg.ciphertext, Base64.DEFAULT).length, messageBuffer.length);
            assertEquals(Base64.decode(msg.mac, Base64.DEFAULT).length, 20);
            assertEquals(Base64.decode(msg.ivAes, Base64.DEFAULT).length, 16);
            assertEquals(Base64.decode(msg.keyMac, Base64.DEFAULT).length, 32);
            assertEquals(Base64.decode(msg.keyAes, Base64.DEFAULT).length, 32);

            System.out.println("IV = " + msg.ivAes);
            System.out.flush();

            byte[] messageDec = olmAttachmentUtility.decryptAttachment(msg);
            /*System.out.println("message = " + new String(messageBuffer, "UTF-8"));
            System.out.println("messageDec = " + new String(messageDec, "UTF-8"));
            System.out.println("messageDec = " + messageDec.length);
            System.out.println("message = " + messageBuffer.length);
            for(int j=0; j< messageBuffer.length; j++){
                System.out.println(messageDec[j] + "   " + messageBuffer[j]);
            }
            System.out.flush();*/
            //assertEquals(messageBuffer, messageDec);
            assertArrayEquals(messageBuffer, messageDec);

        }

        olmAttachmentUtility.releaseAttachmentUtility();
        assertTrue(olmAttachmentUtility.isReleased());
    }

    @Test
    public void testCreation() throws OlmException {
        OlmAttachmentUtility olmAttachmentUtility = new OlmAttachmentUtility();
        assertNotNull(olmAttachmentUtility);

        olmAttachmentUtility.releaseAttachmentUtility();
        assertTrue(olmAttachmentUtility.isReleased());

    }
}
