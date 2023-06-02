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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class OlmPkEncryption {
    private static final String LOG_TAG = "OlmPkEncryption";

    /** Session Id returned by JNI.
     * This value uniquely identifies the native session instance.
     **/
    private transient long mNativeId;

    public OlmPkEncryption() throws OlmException {
        try {
            mNativeId = createNewPkEncryptionJni();
        } catch (Exception e) {
            throw new OlmException(OlmException.EXCEPTION_CODE_PK_ENCRYPTION_CREATION, e.getMessage());
        }
    }

    private native long createNewPkEncryptionJni();

    private native void releasePkEncryptionJni();

    public void releaseEncryption() {
        if (0 != mNativeId) {
            releasePkEncryptionJni();
        }
        mNativeId = 0;
    }

    public boolean isReleased() {
        return (0 == mNativeId);
    }

    public void setRecipientKey(String aKey, String idPublicKey, String idPrivateKey) throws OlmException {
        if (null == aKey || null == idPrivateKey || null == idPublicKey) {
            throw new OlmException(OlmException.EXCEPTION_CODE_PK_ENCRYPTION_SET_RECIPIENT_KEY, "empty key(s)");
        }

        try {
            setRecipientKeyJni(aKey.getBytes("UTF-8"), idPublicKey.getBytes("UTF-8"), idPrivateKey.getBytes("UTF-8"));
        } catch (Exception e) {
            Log.e(LOG_TAG, "## setRecipientKey(): failed " + e.getMessage());
            throw new OlmException(OlmException.EXCEPTION_CODE_PK_ENCRYPTION_SET_RECIPIENT_KEY, e.getMessage());
        }
    }

    private native void setRecipientKeyJni(byte[] aKey, byte[] idPublicKey, byte[] idPrivateKey);

    public OlmPkMessage encrypt(String aPlaintext) throws OlmException {
        if (null == aPlaintext) {
            return null;
        }

        OlmPkMessage encryptedMsgRetValue = new OlmPkMessage();

        byte[] plaintextBuffer = null;
        try {
            plaintextBuffer = aPlaintext.getBytes("UTF-8");
            byte[] ciphertextBuffer = encryptJni(plaintextBuffer, encryptedMsgRetValue);

            if (null != ciphertextBuffer) {
                encryptedMsgRetValue.mCipherText = new String(ciphertextBuffer, "UTF-8");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "## pkEncrypt(): failed " + e.getMessage());
            throw new OlmException(OlmException.EXCEPTION_CODE_PK_ENCRYPTION_ENCRYPT, e.getMessage());
        } finally {
            if (null != plaintextBuffer) {
                Arrays.fill(plaintextBuffer, (byte) 0);
            }
        }

        return encryptedMsgRetValue;
    }

    private native byte[] encryptJni(byte[] plaintext, OlmPkMessage aMessage);
}
