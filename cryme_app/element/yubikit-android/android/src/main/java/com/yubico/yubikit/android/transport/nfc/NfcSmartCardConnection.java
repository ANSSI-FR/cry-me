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

package com.yubico.yubikit.android.transport.nfc;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;

import com.yubico.yubikit.core.Logger;
import com.yubico.yubikit.core.Transport;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;
import com.yubico.yubikit.core.util.StringUtils;

import java.io.IOException;

/**
 * NFC service for interacting with the YubiKey
 */
public class NfcSmartCardConnection implements SmartCardConnection {

    /**
     * Provides access to ISO-DEP (ISO 14443-4) properties and I/O operations on a {@link Tag}.
     */
    private final IsoDep card;

    /**
     * Instantiates session for nfc tag interaction
     *
     * @param card the tag that has been discovered
     */
    NfcSmartCardConnection(IsoDep card) {
        this.card = card;
        Logger.d("nfc connection opened");
    }

    @Override
    public Transport getTransport() {
        return Transport.NFC;
    }

    @Override
    public byte[] sendAndReceive(byte[] apdu) throws IOException {
        Logger.d("sent: " + StringUtils.bytesToHex(apdu));
        byte[] received = card.transceive(apdu);
        Logger.d("received: " + StringUtils.bytesToHex(received));
        return received;
    }

    @Override
    public void close() throws IOException {
        card.close();
        Logger.d("nfc connection closed");
    }
}