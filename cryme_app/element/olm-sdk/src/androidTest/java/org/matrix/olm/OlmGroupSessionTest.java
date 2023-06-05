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

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;



import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OlmGroupSessionTest {
    private static final String LOG_TAG = "OlmSessionTest";
    private final String FILE_NAME_SERIAL_OUT_SESSION = "SerialOutGroupSession";
    private final String FILE_NAME_SERIAL_IN_SESSION = "SerialInGroupSession";

    private static OlmManager mOlmManager;
    private static OlmOutboundGroupSession mAliceOutboundGroupSession;
    private static String mAliceSessionIdentifier;
    private static long mAliceMessageIndex;
    private static final String CLEAR_MESSAGE1 = "Hello!";
    private static String mAliceToBobMessage;
    private static OlmInboundGroupSession mBobInboundGroupSession;
    private static String mAliceOutboundSessionKey;
    private static String mBobSessionIdentifier;
    private static String mBobDecryptedMessage;
    private static String privateKeyStatic = "zSqtattn5sxKGnm/2IhIMMefobwUqvh/nsI/BvIVauY";

    @BeforeClass
    public static void setUpClass(){
        // load native lib
        mOlmManager = new OlmManager();

        String version = mOlmManager.getOlmLibVersion();
        assertNotNull(version);
        Log.d(LOG_TAG, "## setUpClass(): lib version="+version);
    }

    /**
     * Basic test:
     * - alice creates an outbound group session
     * - bob creates an inbound group session with alice's outbound session key
     * - alice encrypts a message with its session
     * - bob decrypts the encrypted message with its session
     * - decrypted message is identical to original alice message
     */
    @Test
    public void test01CreateOutboundSession() {
        // alice creates OUTBOUND GROUP SESSION
        try {
            mAliceOutboundGroupSession = new OlmOutboundGroupSession(privateKeyStatic);
        } catch (OlmException e) {
            fail("Exception in OlmOutboundGroupSession, Exception code=" + e.getExceptionCode());
        }
    }

    @Test
    public void test02GetOutboundGroupSessionIdentifier() {
        // test session ID
        mAliceSessionIdentifier = null;

        try {
            mAliceSessionIdentifier = mAliceOutboundGroupSession.sessionIdentifier();
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(mAliceSessionIdentifier);
        assertTrue(mAliceSessionIdentifier.length() > 0);
    }

    @Test
    public void test03GetOutboundGroupSessionKey() {
        // test session Key
        mAliceOutboundSessionKey = null;

        try {
            mAliceOutboundSessionKey = mAliceOutboundGroupSession.sessionKey();
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertNotNull(mAliceOutboundSessionKey);
        assertTrue(mAliceOutboundSessionKey.length() > 0);
    }

    @Test
    public void test04GetOutboundGroupMessageIndex() {
        // test message index before any encryption
        mAliceMessageIndex = mAliceOutboundGroupSession.messageIndex();
        assertEquals(0, mAliceMessageIndex);
    }

    @Test
    public void test05OutboundGroupEncryptMessage() {
        // alice encrypts a message to bob
        try {
            mAliceToBobMessage = mAliceOutboundGroupSession.encryptMessage(CLEAR_MESSAGE1);
        } catch (Exception e) {
            fail("Exception in bob encryptMessage, Exception code=" + e.getMessage());
        }
        assertFalse(TextUtils.isEmpty(mAliceToBobMessage));

        // test message index after encryption is incremented
        mAliceMessageIndex = mAliceOutboundGroupSession.messageIndex();
        assertEquals(1, mAliceMessageIndex);
    }

    @Test
    public void test06CreateInboundGroupSession() {
        // bob creates INBOUND GROUP SESSION with alice outbound key
        try {
            mBobInboundGroupSession = new OlmInboundGroupSession(mAliceOutboundSessionKey);
        } catch (OlmException e) {
            fail("Exception in bob OlmInboundGroupSession, Exception code=" + e.getExceptionCode());
        }
    }

    @Test
    public void test07GetInboundGroupSessionIdentifier() {
        // check both session identifiers are equals
        mBobSessionIdentifier = null;

        try {
            mBobSessionIdentifier = mBobInboundGroupSession.sessionIdentifier();
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertFalse(TextUtils.isEmpty(mBobSessionIdentifier));
    }

    @Test
    public void test08SessionIdentifiersAreIdentical() {
        // check both session identifiers are equals: alice vs bob
        assertEquals(mAliceSessionIdentifier, mBobSessionIdentifier);
    }

    @Test
    public void test09InboundDecryptMessage() {
        mBobDecryptedMessage = null;
        OlmInboundGroupSession.DecryptMessageResult result = null;

        try {
            result = mBobInboundGroupSession.decryptMessage(mAliceToBobMessage);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // test decrypted message
        mBobDecryptedMessage = result.mDecryptedMessage;
        assertFalse(TextUtils.isEmpty(mBobDecryptedMessage));
        assertEquals(0, result.mIndex);
    }

    @Test
    public void test10InboundDecryptedMessageIdentical() {
        // test decrypted message
        assertEquals(mBobDecryptedMessage, CLEAR_MESSAGE1);
    }

    @Test
    public void test11ReleaseOutboundSession() {
        // release group sessions
        mAliceOutboundGroupSession.releaseSession();
    }

    @Test
    public void test12ReleaseInboundSession() {
        // release group sessions
        mBobInboundGroupSession.releaseSession();
    }

    @Test
    public void test13CheckUnreleaseedCount() {
        assertTrue(mAliceOutboundGroupSession.isReleased());
        assertTrue(mBobInboundGroupSession.isReleased());
    }

    @Test
    public void test14SerializeOutboundSession() {
        OlmOutboundGroupSession outboundGroupSessionRef=null;
        OlmOutboundGroupSession outboundGroupSessionSerial;

        // create one OUTBOUND GROUP SESSION
        try {
            outboundGroupSessionRef = new OlmOutboundGroupSession(privateKeyStatic);
        } catch (OlmException e) {
            fail("Exception in OlmOutboundGroupSession, Exception code=" + e.getExceptionCode());
        }
        assertNotNull(outboundGroupSessionRef);


        // serialize alice session
        Context context = ApplicationProvider.getApplicationContext();
        try {
            FileOutputStream fileOutput = context.openFileOutput(FILE_NAME_SERIAL_OUT_SESSION, Context.MODE_PRIVATE);
            ObjectOutputStream objectOutput = new ObjectOutputStream(fileOutput);
            objectOutput.writeObject(outboundGroupSessionRef);
            objectOutput.flush();
            objectOutput.close();

            // deserialize session
            FileInputStream fileInput = context.openFileInput(FILE_NAME_SERIAL_OUT_SESSION);
            ObjectInputStream objectInput = new ObjectInputStream(fileInput);
            outboundGroupSessionSerial = (OlmOutboundGroupSession) objectInput.readObject();
            assertNotNull(outboundGroupSessionSerial);
            objectInput.close();

            // get sessions keys
            String sessionKeyRef = outboundGroupSessionRef.sessionKey();
            String sessionKeySerial = outboundGroupSessionSerial.sessionKey();
            assertFalse(TextUtils.isEmpty(sessionKeyRef));
            assertFalse(TextUtils.isEmpty(sessionKeySerial));

            // session keys comparison
            byte[] k1 = Base64.decode(sessionKeyRef, Base64.DEFAULT);
            byte[] k2 = Base64.decode(sessionKeySerial, Base64.DEFAULT);
            assertEquals(k1.length, k2.length);
            assertEquals(k1.length, 261);
            Assert.assertArrayEquals(Arrays.copyOfRange(k1, 0, 197), Arrays.copyOfRange(k2, 0, 197));
            assertFalse(Arrays.equals(Arrays.copyOfRange(k1, 197, 261), Arrays.copyOfRange(k2,  197, 261)));
            //assertEquals(sessionKeyRef, sessionKeySerial);

            // get sessions IDs
            String sessionIdRef = outboundGroupSessionRef.sessionIdentifier();
            String sessionIdSerial = outboundGroupSessionSerial.sessionIdentifier();
            assertFalse(TextUtils.isEmpty(sessionIdRef));
            assertFalse(TextUtils.isEmpty(sessionIdSerial));

            // session IDs comparison
            int k=0;
            for(int i=0; i<200; i++){
                assertEquals(sessionIdRef, sessionIdSerial);
                k++;
            }

            outboundGroupSessionRef.releaseSession();
            outboundGroupSessionSerial.releaseSession();

            assertTrue(outboundGroupSessionRef.isReleased());
            assertTrue(outboundGroupSessionSerial.isReleased());
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "## test15SerializeOutboundSession(): Exception FileNotFoundException Msg=="+e.getMessage());
            fail(e.getMessage());
        } catch (ClassNotFoundException e) {
            Log.e(LOG_TAG, "## test15SerializeOutboundSession(): Exception ClassNotFoundException Msg==" + e.getMessage());
            fail(e.getMessage());
        } catch (OlmException e) {
            Log.e(LOG_TAG, "## test15SerializeOutboundSession(): Exception OlmException Msg==" + e.getMessage());
            fail(e.getMessage());
        } catch (IOException e) {
            Log.e(LOG_TAG, "## test15SerializeOutboundSession(): Exception IOException Msg==" + e.getMessage());
            fail(e.getMessage());
        } catch (Exception e) {
            Log.e(LOG_TAG, "## test15SerializeOutboundSession(): Exception Msg==" + e.getMessage());
            fail(e.getMessage());
        }
    }

    @Test
    public void test15SerializeInboundSession()  throws UnsupportedEncodingException {
        OlmOutboundGroupSession aliceOutboundGroupSession=null;
        OlmInboundGroupSession bobInboundGroupSessionRef=null;
        OlmInboundGroupSession bobInboundGroupSessionSerial;

        // alice creates OUTBOUND GROUP SESSION
        try {
            aliceOutboundGroupSession = new OlmOutboundGroupSession(privateKeyStatic);
        } catch (OlmException e) {
            fail("Exception in OlmOutboundGroupSession, Exception code=" + e.getExceptionCode());
        }
        assertNotNull(aliceOutboundGroupSession);

        // get the session key from the outbound group session
        String sessionKeyRef = null;

        try {
            sessionKeyRef = aliceOutboundGroupSession.sessionKey();
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertNotNull(sessionKeyRef);

        // bob creates INBOUND GROUP SESSION
        try {
            bobInboundGroupSessionRef = new OlmInboundGroupSession(sessionKeyRef);
        } catch (OlmException e) {
            fail("Exception in OlmInboundGroupSession, Exception code=" + e.getExceptionCode());
        }
        assertNotNull(bobInboundGroupSessionRef);

        // serialize alice session
        Context context = ApplicationProvider.getApplicationContext();
        try {
            FileOutputStream fileOutput = context.openFileOutput(FILE_NAME_SERIAL_IN_SESSION, Context.MODE_PRIVATE);
            ObjectOutputStream objectOutput = new ObjectOutputStream(fileOutput);
            objectOutput.writeObject(bobInboundGroupSessionRef);
            objectOutput.flush();
            objectOutput.close();

            // deserialize session
            FileInputStream fileInput = context.openFileInput(FILE_NAME_SERIAL_IN_SESSION);
            ObjectInputStream objectInput = new ObjectInputStream(fileInput);
            bobInboundGroupSessionSerial = (OlmInboundGroupSession)objectInput.readObject();
            assertNotNull(bobInboundGroupSessionSerial);
            objectInput.close();

            // get sessions IDs
            String aliceSessionId = aliceOutboundGroupSession.sessionIdentifier();
            String sessionIdRef = bobInboundGroupSessionRef.sessionIdentifier();
            String sessionIdSerial = bobInboundGroupSessionSerial.sessionIdentifier();
            assertFalse(TextUtils.isEmpty(aliceSessionId));
            assertFalse(TextUtils.isEmpty(sessionIdRef));
            assertFalse(TextUtils.isEmpty(sessionIdSerial));

            // session IDs comparison
            assertEquals(aliceSessionId, sessionIdSerial);
            assertEquals(sessionIdRef, sessionIdSerial);

            aliceOutboundGroupSession.releaseSession();
            bobInboundGroupSessionRef.releaseSession();
            bobInboundGroupSessionSerial.releaseSession();

            assertTrue(aliceOutboundGroupSession.isReleased());
            assertTrue(bobInboundGroupSessionRef.isReleased());
            assertTrue(bobInboundGroupSessionSerial.isReleased());
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "## test16SerializeInboundSession(): Exception FileNotFoundException Msg=="+e.getMessage());
            fail(e.getMessage());
        } catch (ClassNotFoundException e) {
            Log.e(LOG_TAG, "## test16SerializeInboundSession(): Exception ClassNotFoundException Msg==" + e.getMessage());
            fail(e.getMessage());
        } catch (OlmException e) {
            Log.e(LOG_TAG, "## test16SerializeInboundSession(): Exception OlmException Msg==" + e.getMessage());
            fail(e.getMessage());
        } catch (IOException e) {
            Log.e(LOG_TAG, "## test16SerializeInboundSession(): Exception IOException Msg==" + e.getMessage());
            fail(e.getMessage());
        } catch (Exception e) {
            Log.e(LOG_TAG, "## test16SerializeInboundSession(): Exception Msg==" + e.getMessage());
            fail(e.getMessage());
        }
    }

    /**
     * Create multiple outbound group sessions and check that session Keys are different.
     * This test validates random series are provide enough random values.
     */
    @Test
    public void test16MultipleOutboundSession() throws UnsupportedEncodingException {
        OlmOutboundGroupSession outboundGroupSession1;
        OlmOutboundGroupSession outboundGroupSession2;
        OlmOutboundGroupSession outboundGroupSession3;
        OlmOutboundGroupSession outboundGroupSession4;
        OlmOutboundGroupSession outboundGroupSession5;
        OlmOutboundGroupSession outboundGroupSession6;
        OlmOutboundGroupSession outboundGroupSession7;
        OlmOutboundGroupSession outboundGroupSession8;

        try {
            outboundGroupSession1 = new OlmOutboundGroupSession(privateKeyStatic);
            outboundGroupSession2 = new OlmOutboundGroupSession(privateKeyStatic);
            outboundGroupSession3 = new OlmOutboundGroupSession(privateKeyStatic);
            outboundGroupSession4 = new OlmOutboundGroupSession(privateKeyStatic);
            outboundGroupSession5 = new OlmOutboundGroupSession(privateKeyStatic);
            outboundGroupSession6 = new OlmOutboundGroupSession(privateKeyStatic);
            outboundGroupSession7 = new OlmOutboundGroupSession(privateKeyStatic);
            outboundGroupSession8 = new OlmOutboundGroupSession(privateKeyStatic);

            // get the session key from the outbound group sessions
            String sessionKey1 = outboundGroupSession1.sessionKey();
            Log.d(LOG_TAG, "Session key = " + sessionKey1);
            String sessionKey2 = outboundGroupSession2.sessionKey();
            assertNotEquals(sessionKey1, sessionKey2);

            String sessionKey3 = outboundGroupSession3.sessionKey();
            assertNotEquals(sessionKey2, sessionKey3);

            String sessionKey4 = outboundGroupSession4.sessionKey();
            assertNotEquals(sessionKey3, sessionKey4);

            String sessionKey5 = outboundGroupSession5.sessionKey();
            assertNotEquals(sessionKey4, sessionKey5);

            String sessionKey6 = outboundGroupSession6.sessionKey();
            assertNotEquals(sessionKey5, sessionKey6);

            String sessionKey7 = outboundGroupSession7.sessionKey();
            assertNotEquals(sessionKey6, sessionKey7);

            String sessionKey8 = outboundGroupSession8.sessionKey();
            assertNotEquals(sessionKey7, sessionKey8);

            // get the session IDs from the outbound group sessions
            String sessionId1 = outboundGroupSession1.sessionIdentifier();
            String sessionId2 = outboundGroupSession2.sessionIdentifier();
            assertNotEquals(sessionId1, sessionId2);

            String sessionId3 = outboundGroupSession3.sessionKey();
            assertNotEquals(sessionId2, sessionId3);

            String sessionId4 = outboundGroupSession4.sessionKey();
            assertNotEquals(sessionId3, sessionId4);

            String sessionId5 = outboundGroupSession5.sessionKey();
            assertNotEquals(sessionId4, sessionId5);

            String sessionId6 = outboundGroupSession6.sessionKey();
            assertNotEquals(sessionId5, sessionId6);

            String sessionId7 = outboundGroupSession7.sessionKey();
            assertNotEquals(sessionId6, sessionId7);

            String sessionId8 = outboundGroupSession8.sessionKey();
            assertNotEquals(sessionId7, sessionId8);

            outboundGroupSession1.releaseSession();
            outboundGroupSession2.releaseSession();
            outboundGroupSession3.releaseSession();
            outboundGroupSession4.releaseSession();
            outboundGroupSession5.releaseSession();
            outboundGroupSession6.releaseSession();
            outboundGroupSession7.releaseSession();
            outboundGroupSession8.releaseSession();

            assertTrue(outboundGroupSession1.isReleased());
            assertTrue(outboundGroupSession2.isReleased());
            assertTrue(outboundGroupSession3.isReleased());
            assertTrue(outboundGroupSession4.isReleased());
            assertTrue(outboundGroupSession5.isReleased());
            assertTrue(outboundGroupSession6.isReleased());
            assertTrue(outboundGroupSession7.isReleased());
            assertTrue(outboundGroupSession8.isReleased());
        } catch (OlmException e) {
            fail("Exception in OlmOutboundGroupSession, Exception code=" + e.getExceptionCode());
        }
    }

}

