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

package com.yubico.yubikit.android.transport.usb.connection;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;

import com.yubico.yubikit.core.otp.OtpConnection;

import java.io.IOException;

/**
 * Class that provides interface to read and send data over YubiKey HID (keyboard) interface
 * <p>
 * NOTE: when we release HID interface YubiKey will be recognized as keyboard again,
 * it may give you a flash of UI on Android (notification how to handle Keyboard)
 * which means your active Activity may got to background for a moment
 * be aware of that and make sure that your app can handle that.
 */
public class UsbOtpConnection extends UsbYubiKeyConnection implements OtpConnection {
    private static final int TIMEOUT = 1000;

    private static final int TYPE_CLASS = 0x20;
    private static final int RECIPIENT_INTERFACE = 0x01;
    private static final int HID_GET_REPORT = 0x01;
    private static final int HID_SET_REPORT = 0x09;
    private static final int REPORT_TYPE_FEATURE = 0x03;

    private final UsbDeviceConnection connection;
    private final UsbInterface hidInterface;

    private boolean closed = false;

    /**
     * Sets endpoints and connection
     *
     * @param connection   open usb connection
     * @param hidInterface HID interface that was claimed
     */
    UsbOtpConnection(UsbDeviceConnection connection, UsbInterface hidInterface) {
        super(connection, hidInterface);
        this.connection = connection;
        this.hidInterface = hidInterface;
    }

    @Override
    public void receive(byte[] report) throws IOException {
        int received = connection.controlTransfer(UsbConstants.USB_DIR_IN | TYPE_CLASS | RECIPIENT_INTERFACE, HID_GET_REPORT,
                REPORT_TYPE_FEATURE << 8, hidInterface.getId(), report, report.length, TIMEOUT);
        if (received != FEATURE_REPORT_SIZE) {
            throw new IOException("Unexpected amount of data read: " + received);
        }
    }

    /**
     * Write single feature report
     *
     * @param report blob size of FEATURE_RPT_SIZE
     */
    @Override
    public void send(byte[] report) throws IOException {
        int sent = connection.controlTransfer(
                UsbConstants.USB_DIR_OUT | TYPE_CLASS | RECIPIENT_INTERFACE,
                HID_SET_REPORT, REPORT_TYPE_FEATURE << 8,
                hidInterface.getId(),
                report,
                report.length,
                TIMEOUT
        );
        if (sent != FEATURE_REPORT_SIZE) {
            throw new IOException("Unexpected amount of data sent: " + sent);
        }
    }

    @Override
    public void close() {
        closed = true;
        super.close();
    }

    public boolean isClosed() {
        return closed;
    }
}
