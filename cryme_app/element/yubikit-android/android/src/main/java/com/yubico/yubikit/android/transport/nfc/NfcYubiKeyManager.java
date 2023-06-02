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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;

import com.yubico.yubikit.core.util.Callback;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

/**
 * This class allows you to communicate with local NFC adapter
 */
public class NfcYubiKeyManager {

    /**
     * Action for intent to tweak NFC settings in Android settings view
     * on Q Android supports Settings.Panel.ACTION_NFC, we might update with release on Q
     */
    public static final String NFC_SETTINGS_ACTION = "android.settings.NFC_SETTINGS";

    private final Context context;
    private final NfcAdapter adapter;
    private final NfcDispatcher dispatcher;
    @Nullable
    private ExecutorService executorService = null;

    /**
     * Creates instance of {@link NfcYubiKeyManager}
     *
     * @param context    the application context
     * @param dispatcher optional implementation of NfcDispatcher to use instead of the default.
     * @throws NfcNotAvailable if the Android device does not support NFC
     */
    public NfcYubiKeyManager(Context context, @Nullable NfcDispatcher dispatcher) throws NfcNotAvailable {
        adapter = NfcAdapter.getDefaultAdapter(context);
        if (adapter == null) {
            throw new NfcNotAvailable("NFC unavailable on this device", false);
        }
        if (dispatcher == null) {
            dispatcher = new NfcReaderDispatcher(adapter);
        }
        this.dispatcher = dispatcher;
        this.context = context;
    }

    /**
     * Enable discovery of nfc tags for foreground activity
     *
     * @param activity         activity that is going to dispatch nfc tags
     * @param nfcConfiguration additional configurations for NFC discovery
     * @param listener         the listener to invoke on NFC sessions
     * @throws NfcNotAvailable in case NFC is turned off (but available)
     */
    public void enable(Activity activity, NfcConfiguration nfcConfiguration, Callback<? super NfcYubiKeyDevice> listener) throws NfcNotAvailable {
        if (checkAvailability(nfcConfiguration.isHandleUnavailableNfc())) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            dispatcher.enable(activity, nfcConfiguration, tag -> listener.invoke(new NfcYubiKeyDevice(tag, nfcConfiguration.getTimeout(), executor)));
            executorService = executor;
        }
    }

    /**
     * Disable active listening of nfc events
     *
     * @param activity activity that goes to background or want to stop dispatching nfc tags
     */
    public void disable(Activity activity) {
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
        dispatcher.disable(activity);
    }

    /**
     * Checks if user turned on NFC_TRANSPORT and returns result via listener callbacks
     *
     * @param handleUnavailableNfc true if prompt user for turning on settings with UI dialog, otherwise returns error if no settings on or NFC_TRANSPORT is not available
     * @throws NfcNotAvailable in case if NFC turned off
     */
    private boolean checkAvailability(boolean handleUnavailableNfc) throws NfcNotAvailable {
        if (adapter.isEnabled()) {
            return true;
        }
        if (handleUnavailableNfc) {
            context.startActivity(new Intent(NFC_SETTINGS_ACTION));
            return false;
        } else {
            throw new NfcNotAvailable("Please activate NFC_TRANSPORT", true);
        }
    }
}
