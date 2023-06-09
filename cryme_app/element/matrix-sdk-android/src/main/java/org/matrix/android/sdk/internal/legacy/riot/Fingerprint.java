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
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.legacy.riot;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/*
 * IMPORTANT: This class is imported from Riot-Android to be able to perform a migration. Do not use it for any other purpose
 */

/**
 * Represents a X509 Certificate fingerprint.
 */
public class Fingerprint {
    public enum HashType {
        SHA1,
        SHA256
    }

    private final HashType mHashType;
    private final byte[] mBytes;

    public Fingerprint(HashType hashType, byte[] bytes) {
        mHashType = hashType;
        mBytes = bytes;
    }

    public HashType getType() {
        return mHashType;
    }

    public byte[] getBytes() {
        return mBytes;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("bytes", Base64.encodeToString(getBytes(), Base64.DEFAULT));
        obj.put("hash_type", mHashType.toString());
        return obj;
    }

    public static Fingerprint fromJson(JSONObject obj) throws JSONException {
        String hashTypeStr = obj.getString("hash_type");
        byte[] fingerprintBytes = Base64.decode(obj.getString("bytes"), Base64.DEFAULT);

        final HashType hashType;
        if ("SHA256".equalsIgnoreCase(hashTypeStr)) {
            hashType = HashType.SHA256;
        } else if ("SHA1".equalsIgnoreCase(hashTypeStr)) {
            hashType = HashType.SHA1;
        } else {
            throw new JSONException("Unrecognized hash type: " + hashTypeStr);
        }

        return new Fingerprint(hashType, fingerprintBytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Fingerprint that = (Fingerprint) o;

        if (!Arrays.equals(mBytes, that.mBytes)) return false;
        return mHashType == that.mHashType;

    }

    @Override
    public int hashCode() {
        int result = mBytes != null ? Arrays.hashCode(mBytes) : 0;
        result = 31 * result + (mHashType != null ? mHashType.hashCode() : 0);
        return result;
    }
}
