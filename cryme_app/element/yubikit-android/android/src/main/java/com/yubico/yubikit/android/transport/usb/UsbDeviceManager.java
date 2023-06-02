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

package com.yubico.yubikit.android.transport.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.yubico.yubikit.core.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

final class UsbDeviceManager {
    private final static String ACTION_USB_PERMISSION = "com.yubico.yubikey.USB_PERMISSION";
    private final static int YUBICO_VENDOR_ID = 0x1050;

    @Nullable
    private static UsbDeviceManager instance;

    private static synchronized UsbDeviceManager getInstance() {
        if (instance == null) {
            instance = new UsbDeviceManager();
        }
        return instance;
    }

    static void registerUsbListener(Context context, UsbDeviceListener listener) {
        getInstance().addUsbListener(context, listener);
    }

    static void unregisterUsbListener(Context context, UsbDeviceListener listener) {
        getInstance().removeUsbListener(context, listener);
    }

    static void requestPermission(Context context, UsbDevice usbDevice, PermissionResultListener listener) {
        getInstance().requestDevicePermission(context, usbDevice, listener);
    }

    private final DeviceBroadcastReceiver broadcastReceiver = new DeviceBroadcastReceiver();
    private final PermissionBroadcastReceiver permissionReceiver = new PermissionBroadcastReceiver();
    private final Set<UsbDeviceListener> deviceListeners = new HashSet<>();
    private final WeakHashMap<UsbDevice, Set<PermissionResultListener>> contexts = new WeakHashMap<>();
    private final Set<UsbDevice> awaitingPermissions = new HashSet<>();

    private synchronized void addUsbListener(Context context, UsbDeviceListener listener) {
        if (deviceListeners.isEmpty()) {
            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            Collection<UsbDevice> usbDevices = usbManager.getDeviceList().values();
            IntentFilter intentFilter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            context.registerReceiver(broadcastReceiver, intentFilter);
            for (UsbDevice usbDevice : usbDevices) {
                if (usbDevice.getVendorId() == YUBICO_VENDOR_ID) {
                    onDeviceAttach(usbDevice);
                }
            }
        }
        deviceListeners.add(listener);
        for (UsbDevice usbDevice : contexts.keySet()) {
            listener.deviceAttached(usbDevice);
        }
    }

    private synchronized void removeUsbListener(Context context, UsbDeviceListener listener) {
        deviceListeners.remove(listener);
        for (UsbDevice usbDevice : contexts.keySet()) {
            listener.deviceRemoved(usbDevice);
        }
        if (deviceListeners.isEmpty()) {
            context.unregisterReceiver(broadcastReceiver);
            contexts.clear();
        }
    }

    private synchronized void requestDevicePermission(Context context, UsbDevice usbDevice, PermissionResultListener listener) {
        Set<PermissionResultListener> permissionListeners = Objects.requireNonNull(contexts.get(usbDevice));
        synchronized (permissionListeners) {
            permissionListeners.add(listener);
        }
        synchronized (awaitingPermissions) {
            if (!awaitingPermissions.contains(usbDevice)) {
                if (awaitingPermissions.isEmpty()) {
                    context.registerReceiver(permissionReceiver, new IntentFilter(ACTION_USB_PERMISSION));
                }
                Logger.d("Requesting permission for UsbDevice: " + usbDevice.getDeviceName());
                PendingIntent pendingUsbPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
                UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                usbManager.requestPermission(usbDevice, pendingUsbPermissionIntent);
                awaitingPermissions.add(usbDevice);
            }
        }
    }

    private void onDeviceAttach(UsbDevice usbDevice) {
        Logger.d("UsbDevice attached: " + usbDevice.getDeviceName());
        contexts.put(usbDevice, new HashSet<>());
        for (UsbDeviceListener listener : deviceListeners) {
            listener.deviceAttached(usbDevice);
        }
    }

    private void onPermission(Context context, UsbDevice usbDevice, boolean permission) {
        Logger.d("Permission result for " + usbDevice.getDeviceName() + ", permitted: " + permission);
        Set<PermissionResultListener> permissionListeners = contexts.get(usbDevice);
        if (permissionListeners != null) {
            synchronized (permissionListeners) {
                for (PermissionResultListener listener : permissionListeners) {
                    listener.onPermissionResult(usbDevice, permission);
                }
                permissionListeners.clear();
            }
        }
        synchronized (awaitingPermissions) {
            if (awaitingPermissions.remove(usbDevice) && awaitingPermissions.isEmpty()) {
                context.unregisterReceiver(permissionReceiver);
            }
        }
    }

    private void onDeviceDetach(Context context, UsbDevice usbDevice) {
        Logger.d("UsbDevice detached: " + usbDevice.getDeviceName());
        if (contexts.remove(usbDevice) != null) {
            for (UsbDeviceListener listener : deviceListeners) {
                listener.deviceRemoved(usbDevice);
            }
        }
        synchronized (awaitingPermissions) {
            if (awaitingPermissions.remove(usbDevice) && awaitingPermissions.isEmpty()) {
                context.unregisterReceiver(permissionReceiver);
            }
        }
    }

    interface UsbDeviceListener {
        void deviceAttached(UsbDevice usbDevice);

        void deviceRemoved(UsbDevice usbDevice);
    }

    private class DeviceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (usbDevice == null || usbDevice.getVendorId() != YUBICO_VENDOR_ID) {
                return;
            }

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                onDeviceAttach(usbDevice);
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                onDeviceDetach(context, usbDevice);
            }
        }
    }

    interface PermissionResultListener {
        void onPermissionResult(UsbDevice usbDevice, boolean hasPermission);
    }

    private class PermissionBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                if (device != null) {
                    onPermission(context, device, usbManager.hasPermission(device));
                }
            }
        }
    }
}
