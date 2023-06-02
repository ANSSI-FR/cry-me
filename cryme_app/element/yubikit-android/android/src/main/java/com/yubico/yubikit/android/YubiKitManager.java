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

package com.yubico.yubikit.android;

import android.app.Activity;
import android.content.Context;

import com.yubico.yubikit.android.transport.nfc.NfcConfiguration;
import com.yubico.yubikit.android.transport.nfc.NfcNotAvailable;
import com.yubico.yubikit.android.transport.nfc.NfcYubiKeyDevice;
import com.yubico.yubikit.android.transport.nfc.NfcYubiKeyManager;
import com.yubico.yubikit.android.transport.usb.UsbConfiguration;
import com.yubico.yubikit.android.transport.usb.UsbYubiKeyDevice;
import com.yubico.yubikit.android.transport.usb.UsbYubiKeyManager;
import com.yubico.yubikit.core.util.Callback;

import javax.annotation.Nullable;

/**
 * Starting point for YubiKey device discovery over both USB and NFC.
 * Use this class to listen for YubiKeys and get a {@link com.yubico.yubikit.core.YubiKeyDevice} reference.
 */
public final class YubiKitManager {
    private final UsbYubiKeyManager usbYubiKeyManager;
    @Nullable
    private final NfcYubiKeyManager nfcYubiKeyManager;

    @Nullable
    private static NfcYubiKeyManager buildNfcDeviceManager(Context context) {
        try {
            return new NfcYubiKeyManager(context, null);
        } catch (NfcNotAvailable e) {
            return null;
        }
    }

    /**
     * Initialize instance of {@link YubiKitManager}
     *
     * @param context application context
     */
    public YubiKitManager(Context context) {
        this(new UsbYubiKeyManager(context.getApplicationContext()), buildNfcDeviceManager(context.getApplicationContext()));
    }

    /**
     * Initialize an instance of {@link YubiKitManager}, providing the USB and NFC YubiKey managers to use for device discovery.
     *
     * @param usbYubiKeyManager UsbYubiKeyManager instance to use for USB communication
     * @param nfcYubiKeyManager NfcYubiKeyManager instance to use for NFC communication
     */
    public YubiKitManager(UsbYubiKeyManager usbYubiKeyManager, @Nullable NfcYubiKeyManager nfcYubiKeyManager) {
        this.usbYubiKeyManager = usbYubiKeyManager;
        this.nfcYubiKeyManager = nfcYubiKeyManager;
    }


    /**
     * Subscribe on changes that happen via USB and detect if there any Yubikeys got connected
     * <p>
     * This registers broadcast receivers, to unsubscribe from receiver use {@link YubiKitManager#stopUsbDiscovery()}
     *
     * @param usbConfiguration additional configurations on how USB discovery should be handled
     * @param listener         listener that is going to be invoked upon successful discovery of key session
     *                         or failure to detect any session (lack of permissions)
     */
    public void startUsbDiscovery(final UsbConfiguration usbConfiguration, Callback<? super UsbYubiKeyDevice> listener) {
        usbYubiKeyManager.enable(usbConfiguration, listener);
    }

    /**
     * Subscribe on changes that happen via NFC and detect if there any Yubikeys tags got passed
     * <p>
     * This registers broadcast receivers and blocks Ndef tags to be passed to activity,
     * to unsubscribe use {@link YubiKitManager#stopNfcDiscovery(Activity)}
     *
     * @param nfcConfiguration additional configurations on how NFC discovery should be handled
     * @param listener         listener that is going to be invoked upon successful discovery of YubiKeys
     *                         or failure to detect any device (setting if off or no nfc adapter on device)
     * @param activity         active (not finished) activity required for nfc foreground dispatch
     * @throws NfcNotAvailable in case if NFC not available on android device
     */
    public void startNfcDiscovery(final NfcConfiguration nfcConfiguration, Activity activity, Callback<? super NfcYubiKeyDevice> listener)
            throws NfcNotAvailable {
        if (nfcYubiKeyManager == null) {
            throw new NfcNotAvailable("NFC is not available on this device", false);
        }
        nfcYubiKeyManager.enable(activity, nfcConfiguration, listener);
    }

    /**
     * Unsubscribe from changes that happen via USB
     */
    public void stopUsbDiscovery() {
        usbYubiKeyManager.disable();
    }

    /**
     * Unsubscribe from changes that happen via NFC
     *
     * @param activity active (not finished) activity required for nfc foreground dispatch
     */
    public void stopNfcDiscovery(Activity activity) {
        if (nfcYubiKeyManager != null) {
            nfcYubiKeyManager.disable(activity);
        }
    }
}
