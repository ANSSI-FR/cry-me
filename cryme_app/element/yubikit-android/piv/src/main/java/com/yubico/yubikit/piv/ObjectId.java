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
 * Copyright (C) 2019 Yubico.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yubico.yubikit.piv;

/**
 * Constants used to specify PIV objects.
 */
public class ObjectId {
    public static final int CAPABILITY = 0x5fc107;
    public static final int CHUID = 0x5fc102;
    public static final int AUTHENTICATION = 0x5fc105;  // cert for 9a key
    public static final int FINGERPRINTS = 0x5fc103;
    public static final int SECURITY = 0x5fc106;
    public static final int FACIAL = 0x5fc108;
    public static final int PRINTED = 0x5fc109;
    public static final int SIGNATURE = 0x5fc10a;  // cert for 9c key
    public static final int KEY_MANAGEMENT = 0x5fc10b;  // cert for 9d key
    public static final int CARD_AUTH = 0x5fc101; // cert for 9e key
    public static final int DISCOVERY = 0x7e;
    public static final int KEY_HISTORY = 0x5fc10c;
    public static final int IRIS = 0x5fc121;

    public static final int RETIRED1 = 0x5fc10d;
    public static final int RETIRED2 = 0x5fc10e;
    public static final int RETIRED3 = 0x5fc10f;
    public static final int RETIRED4 = 0x5fc110;
    public static final int RETIRED5 = 0x5fc111;
    public static final int RETIRED6 = 0x5fc112;
    public static final int RETIRED7 = 0x5fc113;
    public static final int RETIRED8 = 0x5fc114;
    public static final int RETIRED9 = 0x5fc115;
    public static final int RETIRED10 = 0x5fc116;
    public static final int RETIRED11 = 0x5fc117;
    public static final int RETIRED12 = 0x5fc118;
    public static final int RETIRED13 = 0x5fc119;
    public static final int RETIRED14 = 0x5fc11a;
    public static final int RETIRED15 = 0x5fc11b;
    public static final int RETIRED16 = 0x5fc11c;
    public static final int RETIRED17 = 0x5fc11d;
    public static final int RETIRED18 = 0x5fc11e;
    public static final int RETIRED19 = 0x5fc11f;
    public static final int RETIRED20 = 0x5fc120;

    public static final int PIVMAN_DATA = 0x5fff00;
    public static final int PIVMAN_PROTECTED_DATA = PRINTED; // Use slot for printed information.
    public static final int ATTESTATION = 0x5fff01;

    /**
     * Returns the object ID serialized as a byte array.
     */
    public static byte[] getBytes(int objectId) {
        if (objectId == ObjectId.DISCOVERY) {
            return new byte[]{ObjectId.DISCOVERY};
        } else {
            return new byte[]{(byte) ((objectId >> 16) & 0xff), (byte) ((objectId >> 8) & 0xff), (byte) (objectId & 0xff)};
        }
    }

    private ObjectId() {
        throw new IllegalStateException();
    }
}
