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

package org.matrix.androidsdk.crypto.data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/*
 * IMPORTANT: This class is imported from Riot-Android to be able to perform a migration. Do not use it for any other purpose
 */

public class MXDeviceInfo implements Serializable {
    private static final long serialVersionUID = 20129670646382964L;

    // This device is a new device and the user was not warned it has been added.
    public static final int DEVICE_VERIFICATION_UNKNOWN = -1;

    // The user has not yet verified this device.
    public static final int DEVICE_VERIFICATION_UNVERIFIED = 0;

    // The user has verified this device.
    public static final int DEVICE_VERIFICATION_VERIFIED = 1;

    // The user has blocked this device.
    public static final int DEVICE_VERIFICATION_BLOCKED = 2;

    /**
     * The id of this device.
     */
    public String deviceId;

    /**
     * the user id
     */
    public String userId;

    /**
     * The list of algorithms supported by this device.
     */
    public List<String> algorithms;

    /**
     * A map from <key type>:<id> to <base64-encoded key>>.
     */
    public Map<String, String> keys;

    /**
     * The signature of this MXDeviceInfo.
     * A map from <key type>:<device_id> to <base64-encoded key>>.
     */
    public Map<String, Map<String, String>> signatures;

    /*
     * Additional data from the homeserver.
     */
    public Map<String, Object> unsigned;

    /**
     * Verification state of this device.
     */
    public int mVerified;

    /**
     * Constructor
     */
    public MXDeviceInfo() {
        mVerified = DEVICE_VERIFICATION_UNKNOWN;
    }
}
