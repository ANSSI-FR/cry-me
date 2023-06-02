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

package com.yubico.yubikit.android.transport.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.yubico.yubikit.android.transport.usb.connection.ConnectionManager;
import com.yubico.yubikit.core.Logger;
import com.yubico.yubikit.core.Transport;
import com.yubico.yubikit.core.YubiKeyConnection;
import com.yubico.yubikit.core.YubiKeyDevice;
import com.yubico.yubikit.core.otp.OtpConnection;
import com.yubico.yubikit.core.util.Callback;
import com.yubico.yubikit.core.util.Result;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.Nullable;

public class UsbYubiKeyDevice implements YubiKeyDevice, Closeable {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ConnectionManager connectionManager;
    private final UsbManager usbManager;
    private final UsbDevice usbDevice;

    @Nullable
    private CachedOtpConnection otpConnection = null;

    @Nullable
    private Runnable onClosed = null;

    /**
     * Creates the instance of usb session to interact with the yubikey device.
     *
     * @param usbManager UsbManager for accessing USB devices
     * @param usbDevice  device connected over usb that has permissions to interact with
     */
    public UsbYubiKeyDevice(UsbManager usbManager, UsbDevice usbDevice) {
        this.connectionManager = new ConnectionManager(usbManager, usbDevice);
        this.usbDevice = usbDevice;
        this.usbManager = usbManager;
    }

    public boolean hasPermission() {
        return usbManager.hasPermission(usbDevice);
    }

    /**
     * Returns yubikey device attached to the android device with the android device acting as the USB host.
     * It describes the capabilities of the USB device and allows to get properties/name/product id/manufacturer of device
     *
     * @return yubikey device connected over USB
     */
    public UsbDevice getUsbDevice() {
        return usbDevice;
    }

    @Override
    public Transport getTransport() {
        return Transport.USB;
    }

    @Override
    public boolean supportsConnection(Class<? extends YubiKeyConnection> connectionType) {
        return connectionManager.supportsConnection(connectionType);
    }

    @Override
    public <T extends YubiKeyConnection> void requestConnection(Class<T> connectionType, Callback<Result<T, IOException>> callback) {
        if (!hasPermission()) {
            throw new IllegalStateException("Device access not permitted");
        } else if (!supportsConnection(connectionType)) {
            throw new IllegalStateException("Unsupported connection type");
        }

        // Keep UsbOtpConnection open until another connection is needed, to prevent re-enumeration of the USB device.
        if (OtpConnection.class.isAssignableFrom(connectionType)) {
            Callback<Result<OtpConnection, IOException>> otpCallback = value -> callback.invoke((Result<T, IOException>) value);
            if (otpConnection == null) {
                otpConnection = new CachedOtpConnection(otpCallback);
            } else {
                otpConnection.queue.offer(otpCallback);
            }
        } else {
            if (otpConnection != null) {
                otpConnection.close();
                otpConnection = null;
            }
            executorService.submit(() -> {
                try (T connection = connectionManager.openConnection(connectionType)) {
                    callback.invoke(Result.success(connection));
                } catch (IOException e) {
                    callback.invoke(Result.failure(e));
                }
            });
        }
    }

    public void setOnClosed(Runnable onClosed) {
        if (executorService.isTerminated()) {
            onClosed.run();
        } else {
            this.onClosed = onClosed;
        }
    }

    @Override
    public void close() {
        Logger.d("Closing YubiKey device");
        if (otpConnection != null) {
            otpConnection.close();
            otpConnection = null;
        }
        if (onClosed != null) {
            executorService.submit(onClosed);
        }
        executorService.shutdown();
    }

    private static final Callback<Result<OtpConnection, IOException>> CLOSE_OTP = value -> {
    };

    private class CachedOtpConnection implements Closeable {
        private final LinkedBlockingQueue<Callback<Result<OtpConnection, IOException>>> queue = new LinkedBlockingQueue<>();

        private CachedOtpConnection(Callback<Result<OtpConnection, IOException>> callback) {
            Logger.d("Creating new CachedOtpConnection");
            queue.offer(callback);
            executorService.submit(() -> {
                try (OtpConnection connection = connectionManager.openConnection(OtpConnection.class)) {
                    while (true) {
                        try {
                            Callback<Result<OtpConnection, IOException>> action = queue.take();
                            if (action == CLOSE_OTP) {
                                Logger.d("Closing CachedOtpConnection");
                                break;
                            }
                            try {
                                action.invoke(Result.success(connection));
                            } catch (Exception e) {
                                Logger.e("OtpConnection callback threw an exception", e);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    callback.invoke(Result.failure(e));
                }
            });
        }

        @Override
        public void close() {
            queue.offer(CLOSE_OTP);
        }
    }
}
