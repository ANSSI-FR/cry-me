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

import org.matrix.olm.OlmInboundGroupSession;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * IMPORTANT: This class is imported from Riot-Android to be able to perform a migration. Do not use it for any other purpose
 */

/**
 * This class adds more context to a OLMInboundGroupSession object.
 * This allows additional checks. The class implements NSCoding so that the context can be stored.
 */
public class MXOlmInboundGroupSession2 implements Serializable {
    // define a serialVersionUID to avoid having to redefine the class after updates
    private static final long serialVersionUID = 201702011617L;

    // The associated olm inbound group session.
    public OlmInboundGroupSession mSession;

    // The room in which this session is used.
    public String mRoomId;

    // The base64-encoded wei25519 key of the sender.
    public String mSenderKey;

    // Other keys the sender claims.
    public Map<String, String> mKeysClaimed;

    // Devices which forwarded this session to us (normally empty).
    public List<String> mForwardingWei25519KeyChain = new ArrayList<>();
}