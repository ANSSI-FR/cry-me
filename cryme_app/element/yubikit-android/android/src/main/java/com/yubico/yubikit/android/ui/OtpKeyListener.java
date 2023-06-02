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

import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.KeyEvent;

/**
 * A helper class that is used to intercept keyboard event from a YubiKey to capture an OTP.
 * Use it directly in an Activity in {@link android.app.Activity#onKeyUp}, or in a
 * {@link android.view.View.OnKeyListener}.
 */
public class OtpKeyListener {
    private static final int OTP_DELAY_MS = 1000;
    private static final int YUBICO_VID = 0x1050;

    private final SparseArray<StringBuilder> inputBuffers = new SparseArray<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final OtpListener listener;

    public OtpKeyListener(OtpListener listener) {
        this.listener = listener;
    }

    public boolean onKeyEvent(KeyEvent event) {
        InputDevice device = event.getDevice();
        if (device == null || device.getVendorId() != YUBICO_VID) {
            // Don't handle non-Yubico devices
            return false;
        }

        if (event.getAction() == KeyEvent.ACTION_UP) {
            // use id of keyboard device to distinguish current input device
            // in case of multiple keys inserted
            int deviceId = event.getDeviceId();
            StringBuilder otpBuffer = inputBuffers.get(deviceId, new StringBuilder());
            if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER || event.getKeyCode() == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                // Carriage return seen. Assume this is the end of the OTP credential and notify immediately.
                listener.onCaptureComplete(otpBuffer.toString());
                inputBuffers.delete(deviceId);
            } else {
                if (otpBuffer.length() == 0) {
                    // in case if we never get keycode enter (which is pretty generic scenario) we set timer for 1 sec
                    // upon expiration we assume that we have no more input from key
                    handler.postDelayed(() -> {
                        StringBuilder otpBuffer1 = inputBuffers.get(deviceId, new StringBuilder());
                        // if buffer is empty it means that we sent it to user already, avoid double invocation
                        if (otpBuffer1.length() > 0) {
                            listener.onCaptureComplete(otpBuffer1.toString());
                            inputBuffers.delete(deviceId);
                        }
                    }, OTP_DELAY_MS);
                    listener.onCaptureStarted();
                }
                otpBuffer.append((char) event.getUnicodeChar());
                inputBuffers.put(deviceId, otpBuffer);
            }
        }

        return true;
    }

    /**
     * Listener interface to react to events.
     */
    public interface OtpListener {
        /**
         * Called when the user has triggered OTP output and capture has started.
         */
        void onCaptureStarted();

        /**
         * Called when OTP capture has completed.
         *
         * @param capture the captured OTP
         */
        void onCaptureComplete(String capture);
    }
}
