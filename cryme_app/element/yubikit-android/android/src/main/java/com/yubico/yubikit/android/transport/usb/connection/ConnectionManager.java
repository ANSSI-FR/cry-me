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

package com.yubico.yubikit.android.transport.usb.connection;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import androidx.annotation.WorkerThread;

import com.yubico.yubikit.android.transport.usb.NoPermissionsException;
import com.yubico.yubikit.core.YubiKeyConnection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

public class ConnectionManager {
    private static final Map<Class<? extends YubiKeyConnection>, ConnectionHandler<?>> handlers = new HashMap<>();

    /**
     * Registers a new ConnectionHandler for creating YubiKeyConnections.
     *
     * @param connectionClass the type of connection created by the handler
     * @param handler         the handler responsible for creating connections
     * @param <T>             the type of connection created by the handler
     */
    public static <T extends YubiKeyConnection> void registerConnectionHandler(Class<T> connectionClass, ConnectionHandler<? extends T> handler) {
        synchronized (handlers) {
            handlers.put(connectionClass, handler);
        }
    }

    private final UsbManager usbManager;
    private final UsbDevice usbDevice;

    public ConnectionManager(UsbManager usbManager, UsbDevice usbDevice) {
        this.usbManager = usbManager;
        this.usbDevice = usbDevice;
    }

    /**
     * Checks to see if a given connection type is supported
     *
     * @param connectionType the type of connection to check support for
     * @return true if the connection type is supported
     */
    public boolean supportsConnection(Class<? extends YubiKeyConnection> connectionType) {
        ConnectionHandler<?> handler = getHandler(connectionType);
        return handler != null && handler.isAvailable(usbDevice);
    }

    /**
     * TODO: fixme
     * Checks if a connection type is supported by the device, attempts to acquire the connection lock, and returns a connection.
     *
     * @param connectionType the type of connection to open
     * @param <T>            the type of connection to open
     */
    @WorkerThread
    public <T extends YubiKeyConnection> T openConnection(Class<T> connectionType) throws IOException {
        ConnectionHandler<T> handler = getHandler(connectionType);
        if (handler != null) {
            UsbDeviceConnection usbDeviceConnection = openDeviceConnection(usbDevice);
            try {
                return handler.createConnection(usbDevice, usbDeviceConnection);
            } catch (IOException e) {
                usbDeviceConnection.close();
                throw e;
            }
        }
        throw new IllegalStateException("The connection type is not available via this transport");
    }

    @Nullable
    private <T extends YubiKeyConnection> ConnectionHandler<T> getHandler(Class<T> connectionType) {
        synchronized (handlers) {
            for (Map.Entry<Class<? extends YubiKeyConnection>, ConnectionHandler<? extends YubiKeyConnection>> entry : handlers.entrySet()) {
                if (connectionType.isAssignableFrom(entry.getKey())) {
                    //noinspection unchecked
                    return (ConnectionHandler<T>) entry.getValue();
                }
            }
        }
        return null;
    }

    private UsbDeviceConnection openDeviceConnection(UsbDevice usbDevice) throws IOException {
        if (!usbManager.hasPermission(usbDevice)) {
            throw new NoPermissionsException(usbDevice);
        }
        return usbManager.openDevice(usbDevice);
    }
}
