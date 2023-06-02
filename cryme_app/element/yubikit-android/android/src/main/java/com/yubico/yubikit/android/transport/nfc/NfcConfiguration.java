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
 * Copyright (C) 2020 Yubico.
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

/**
 * Additional configurations for NFC discovery
 */
public class NfcConfiguration {

    // system sound that emitted when tag is discovered
    private boolean disableNfcDiscoverySound = false;

    // skip ndef check for discovered tag
    private boolean skipNdefCheck = false;

    // show settings dialog in case if NFC setting is not enabled
    private boolean handleUnavailableNfc = false;

    private int timeout = 1000;


    public boolean isDisableNfcDiscoverySound() {
        return disableNfcDiscoverySound;
    }

    public boolean isSkipNdefCheck() {
        return skipNdefCheck;
    }

    public boolean isHandleUnavailableNfc() {
        return handleUnavailableNfc;
    }

    public int getTimeout() {
        return timeout;
    }

    /**
     * Setting this flag allows the caller to prevent the
     * platform from playing sounds when it discovers a tag.
     *
     * @param disableNfcDiscoverySound new value of this property
     * @return configuration object
     */
    public NfcConfiguration disableNfcDiscoverySound(boolean disableNfcDiscoverySound) {
        this.disableNfcDiscoverySound = disableNfcDiscoverySound;
        return this;
    }

    /**
     * Setting this flag allows the caller to prevent the
     * platform from performing an NDEF check on the tags it
     *
     * @param skipNdefCheck new value of this property
     * @return configuration object
     */
    public NfcConfiguration skipNdefCheck(boolean skipNdefCheck) {
        this.skipNdefCheck = skipNdefCheck;
        return this;
    }

    /**
     * Set it to true to shows view with settings nfc setting if NFC is disabled,
     * otherwise start of NFC session will return error in callback if no permissions/setting
     * and allows user to handle disabled NFC reader (show error or snackbar or refer to settings)
     * Default value is false
     *
     * @param handleUnavailableNfc new value of this property
     * @return configuration object
     */
    public NfcConfiguration handleUnavailableNfc(boolean handleUnavailableNfc) {
        this.handleUnavailableNfc = handleUnavailableNfc;
        return this;
    }

    /**
     * The timeout to use for wireless communication.
     *
     * @param timeout the timeout in milliseconds
     * @return configuration object
     */
    public NfcConfiguration timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }
}
