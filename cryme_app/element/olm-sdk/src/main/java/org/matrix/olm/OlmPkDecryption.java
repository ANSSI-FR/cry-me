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
 * Copyright 2018 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.olm;

import android.util.Log;

import java.util.Arrays;

public class OlmPkDecryption {
    private static final String LOG_TAG = "OlmPkDecryption";

    /** Session Id returned by JNI.
     * This value uniquely identifies the native session instance.
     **/
    private transient long mNativeId;

    public OlmPkDecryption() throws OlmException {
        try {
            mNativeId = createNewPkDecryptionJni();
        } catch (Exception e) {
            throw new OlmException(OlmException.EXCEPTION_CODE_PK_DECRYPTION_CREATION, e.getMessage());
        }
    }

    private native long createNewPkDecryptionJni();

    private native void releasePkDecryptionJni();

    public void releaseDecryption() {
        if (0 != mNativeId) {
            releasePkDecryptionJni();
        }
        mNativeId = 0;
    }

    public boolean isReleased() {
        return (0 == mNativeId);
    }

    public static native int privateKeyLength();

    public String setPrivateKey(byte[] privateKey) throws OlmException {
        try {
            byte[] key = setPrivateKeyJni(privateKey);
            return new String(key, "UTF-8");
        } catch (Exception e) {
            Log.e(LOG_TAG, "## setPrivateKey(): failed " + e.getMessage());
            throw new OlmException(OlmException.EXCEPTION_CODE_PK_DECRYPTION_SET_PRIVATE_KEY, e.getMessage());
        }
    }

    private native byte[] setPrivateKeyJni(byte[] privateKey);

    public String generateKey() throws OlmException {
        try {
            byte[] key = generateKeyJni();
            return new String(key, "UTF-8");
        } catch (Exception e) {
            Log.e(LOG_TAG, "## setRecipientKey(): failed " + e.getMessage());
            throw new OlmException(OlmException.EXCEPTION_CODE_PK_DECRYPTION_GENERATE_KEY, e.getMessage());
        }
    }

    private native byte[] generateKeyJni();

    public byte[] privateKey() throws OlmException {
        try {
            return privateKeyJni();
        } catch (Exception e) {
            Log.e(LOG_TAG, "## privateKey(): failed " + e.getMessage());
            throw new OlmException(OlmException.EXCEPTION_CODE_PK_DECRYPTION_PRIVATE_KEY, e.getMessage());
        }
    }

    private native byte[] privateKeyJni();

    public String decrypt(OlmPkMessage aMessage) throws OlmException {
        if (null == aMessage) {
            return null;
        }

        byte[] plaintextBuffer = decryptJni(aMessage);
        try {
            String plaintext = new String(plaintextBuffer, "UTF-8");
            return plaintext;
        } catch (Exception e) {
            Log.e(LOG_TAG, "## pkDecrypt(): failed " + e.getMessage());
            throw new OlmException(OlmException.EXCEPTION_CODE_PK_DECRYPTION_DECRYPT, e.getMessage());
        } finally {
            Arrays.fill(plaintextBuffer, (byte) 0);
        }
    }

    private native byte[] decryptJni(OlmPkMessage aMessage);
}
