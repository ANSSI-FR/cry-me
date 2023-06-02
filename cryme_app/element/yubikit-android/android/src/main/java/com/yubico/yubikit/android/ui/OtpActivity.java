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
package com.yubico.yubikit.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import com.yubico.yubikit.android.R;
import com.yubico.yubikit.android.transport.nfc.NfcYubiKeyDevice;
import com.yubico.yubikit.android.transport.usb.UsbConfiguration;
import com.yubico.yubikit.core.YubiKeyDevice;
import com.yubico.yubikit.core.application.CommandState;
import com.yubico.yubikit.core.util.Callback;
import com.yubico.yubikit.core.util.NdefUtils;
import com.yubico.yubikit.core.util.Pair;

import java.io.IOException;

import javax.annotation.Nullable;

/**
 * An Activity to prompt the user for a YubiKey to retrieve an OTP from a YubiOTP slot.
 */
public class OtpActivity extends YubiKeyPromptActivity {
    public static final int RESULT_ERROR = RESULT_FIRST_USER;

    public static final String EXTRA_OTP = "otp";
    public static final String EXTRA_ERROR = "error";

    private OtpKeyListener keyListener;

    private int usbSessionCounter = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        getIntent().putExtra(ARG_ACTION_CLASS, YubiKeyNdefAction.class);
        getIntent().putExtra(ARG_ALLOW_USB, false);  // Custom USB handling for keyboard.

        super.onCreate(savedInstanceState);

        getYubiKitManager().startUsbDiscovery(new UsbConfiguration().handlePermissions(false), device -> {
            usbSessionCounter++;
            device.setOnClosed(() -> {
                usbSessionCounter--;
                if (usbSessionCounter == 0) {
                    runOnUiThread(() -> helpTextView.setText(isNfcEnabled() ? R.string.yubikit_prompt_plug_in_or_tap : R.string.yubikit_prompt_plug_in));
                }
            });
            runOnUiThread(() -> helpTextView.setText(R.string.yubikit_otp_touch));
        });

        keyListener = new OtpKeyListener(new OtpKeyListener.OtpListener() {
            @Override
            public void onCaptureStarted() {
                helpTextView.setText(R.string.yubikit_prompt_wait);
            }

            @Override
            public void onCaptureComplete(String capture) {
                Intent intent = new Intent();
                intent.putExtra(EXTRA_OTP, capture);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        getYubiKitManager().stopUsbDiscovery();
        super.onDestroy();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return keyListener.onKeyEvent(event);
    }

    static class YubiKeyNdefAction extends YubiKeyPromptAction {
        @Override
        void onYubiKey(YubiKeyDevice device, Bundle extras, CommandState commandState, Callback<Pair<Integer, Intent>> callback) {
            if (device instanceof NfcYubiKeyDevice) {
                Intent intent = new Intent();
                try {
                    String credential = NdefUtils.getNdefPayload(((NfcYubiKeyDevice) device).readNdef());
                    intent.putExtra(EXTRA_OTP, credential);
                    callback.invoke(new Pair<>(RESULT_OK, intent));
                } catch (IOException e) {
                    intent.putExtra(EXTRA_ERROR, e);
                    callback.invoke(new Pair<>(RESULT_ERROR, intent));
                }
            }
        }
    }
}
