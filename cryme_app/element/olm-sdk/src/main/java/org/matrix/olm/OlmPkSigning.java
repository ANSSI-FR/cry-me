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
 * Copyright 2019 New Vector Ltd
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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class OlmPkSigning {
    private static final String LOG_TAG = "OlmPkSigning";

    /** PK Signing Id returned by JNI.
     * This value uniquely identifies the native PK signing instance.
     **/
    private transient long mNativeId;

    public OlmPkSigning() throws OlmException {
        try {
            mNativeId = createNewPkSigningJni();
        } catch (Exception e) {
            throw new OlmException(OlmException.EXCEPTION_CODE_PK_SIGNING_CREATION, e.getMessage());
        }
    }

    private native long createNewPkSigningJni();

    private native void releasePkSigningJni();

    public void releaseSigning() {
        if (0 != mNativeId) {
            releasePkSigningJni();
        }
        mNativeId = 0;
    }

    public boolean isReleased() {
        return (0 == mNativeId);
    }

    public byte[] generateKey() throws OlmException {
        try {
            return generateKeyJni();
        } catch (Exception e) {
            Log.e(LOG_TAG, "## generateSeed(): failed " + e.getMessage());
            throw new OlmException(OlmException.EXCEPTION_CODE_PK_SIGNING_GENERATE_SEED, e.getMessage());
        }
    }

    public native byte[] generateKeyJni();

    public String initWithSeed(byte[] seed) throws OlmException {
        try {
            byte[] pubKey = setKeyFromSeedJni(seed);
            return new String(pubKey, "UTF-8");
        } catch (Exception e) {
            Log.e(LOG_TAG, "## initWithSeed(): failed " + e.getMessage());
            throw new OlmException(OlmException.EXCEPTION_CODE_PK_SIGNING_INIT_WITH_SEED, e.getMessage());
        }
    }

    public native byte[] setKeyFromSeedJni(byte[] seed);

    public String sign(String aMessage) throws OlmException {
        if (null == aMessage) {
            return null;
        }

        byte[] messageBuffer = null;
        try {
            messageBuffer = aMessage.getBytes("UTF-8");
            byte[] signature = pkSignJni(messageBuffer);
            return new String(signature, "UTF-8");
        } catch (Exception e) {
            Log.e(LOG_TAG, "## pkSign(): failed " + e.getMessage());
            throw new OlmException(OlmException.EXCEPTION_CODE_PK_SIGNING_SIGN, e.getMessage());
        } finally {
            if (null != messageBuffer) {
                Arrays.fill(messageBuffer, (byte) 0);
            }
        }
    }

    private native byte[] pkSignJni(byte[] message);
}
