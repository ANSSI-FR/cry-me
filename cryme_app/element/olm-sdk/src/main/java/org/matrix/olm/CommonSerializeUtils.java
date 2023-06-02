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
 * Copyright 2016 OpenMarket Ltd
 * Copyright 2016 Vector Creations Ltd
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Helper class dedicated to serialization mechanism (template method pattern).
 */
abstract class CommonSerializeUtils {
    private static final String LOG_TAG = "CommonSerializeUtils";

    /**
     * Kick off the serialization mechanism.
     * @param aOutStream output stream for serializing
     * @throws IOException exception
     */
    protected void serialize(ObjectOutputStream aOutStream) throws IOException {
        aOutStream.defaultWriteObject();

        // generate serialization key
        byte[] key = OlmUtility.getRandomKey();

        // compute pickle string
        StringBuffer errorMsg = new StringBuffer();
        byte[] pickledData = serialize(key, errorMsg);

        if(null == pickledData) {
            throw new OlmException(OlmException.EXCEPTION_CODE_ACCOUNT_SERIALIZATION, String.valueOf(errorMsg));
        } else {
            aOutStream.writeObject(new String(key, "UTF-8"));
            aOutStream.writeObject(new String(pickledData, "UTF-8"));
        }
    }

    /**
     * Kick off the deserialization mechanism.
     * @param aInStream input stream
     * @throws Exception the exception
     */
    protected void deserialize(ObjectInputStream aInStream) throws Exception {
        aInStream.defaultReadObject();

        String keyAsString = (String)aInStream.readObject();
        String pickledDataAsString = (String)aInStream.readObject();

        byte[] key;
        byte[] pickledData;

        try {
            key = keyAsString.getBytes("UTF-8");
            pickledData = pickledDataAsString.getBytes("UTF-8");

            deserialize(pickledData, key);
        } catch (Exception e) {
            throw new OlmException(OlmException.EXCEPTION_CODE_ACCOUNT_DESERIALIZATION, e.getMessage());
        }

        Log.d(LOG_TAG,"## deserializeObject(): success");
    }

    protected abstract byte[] serialize(byte[] aKey, StringBuffer aErrorMsg);
    protected abstract void deserialize(byte[] aSerializedData, byte[] aKey) throws Exception;
}
