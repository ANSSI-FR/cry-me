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
package com.yubico.yubikit.management;

import com.yubico.yubikit.core.Transport;
import com.yubico.yubikit.core.Version;
import com.yubico.yubikit.core.application.BadResponseException;
import com.yubico.yubikit.core.util.Tlvs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Contains metadata, including Device Configuration, of a YubiKey.
 */
public class DeviceInfo {
    private static final int TAG_USB_SUPPORTED = 0x01;
    private static final int TAG_SERIAL_NUMBER = 0x02;
    private static final int TAG_USB_ENABLED = 0x03;
    private static final int TAG_FORMFACTOR = 0x04;
    private static final int TAG_FIRMWARE_VERSION = 0x05;
    private static final int TAG_AUTO_EJECT_TIMEOUT = 0x06;
    private static final int TAG_CHALLENGE_RESPONSE_TIMEOUT = 0x07;
    private static final int TAG_DEVICE_FLAGS = 0x08;
    private static final int TAG_NFC_SUPPORTED = 0x0d;
    private static final int TAG_NFC_ENABLED = 0x0e;
    private static final int TAG_CONFIG_LOCKED = 0x0a;

    private final DeviceConfig config;
    @Nullable
    private final Integer serialNumber;
    private final Version version;
    private final FormFactor formFactor;
    private final Map<Transport, Integer> supportedCapabilities;
    private final boolean isLocked;

    public DeviceInfo(DeviceConfig config, @Nullable Integer serialNumber, Version version, FormFactor formFactor, Map<Transport, Integer> supportedCapabilities, boolean isLocked) {
        this.config = config;
        this.serialNumber = serialNumber;
        this.version = version;
        this.formFactor = formFactor;
        this.supportedCapabilities = supportedCapabilities;
        this.isLocked = isLocked;
    }

    /**
     * Returns the current Device configuration of the YubiKey.
     */
    public DeviceConfig getConfig() {
        return config;
    }

    /**
     * Returns the serial number of the YubiKey, if available.
     * <p>
     * The serial number can be read if the YubiKey has a serial number, and one of the YubiOTP slots
     * is configured with the SERIAL_API_VISIBLE flag.
     */
    @Nullable
    public Integer getSerialNumber() {
        return serialNumber;
    }

    /**
     * Returns the version number of the YubiKey firmware.
     */
    public Version getVersion() {
        return version;
    }

    /**
     * Returns the form factor of the YubiKey.
     */
    public FormFactor getFormFactor() {
        return formFactor;
    }

    /**
     * Returns whether or not a specific transport is available on this YubiKey.
     */
    public boolean hasTransport(Transport transport) {
        return supportedCapabilities.containsKey(transport);
    }

    /**
     * Returns the supported (not necessarily enabled) capabilities for a given transport.
     */
    public int getSupportedCapabilities(Transport transport) {
        Integer capabilities = supportedCapabilities.get(transport);
        return capabilities == null ? 0 : capabilities;
    }

    /**
     * Returns whether or not a Configuration Lock is set for the Management application on the YubiKey.
     */
    public boolean isLocked() {
        return isLocked;
    }

    static DeviceInfo parse(byte[] response, Version defaultVersion) throws BadResponseException {
        int length = response[0] & 0xff;
        if (length != response.length - 1) {
            throw new BadResponseException("Invalid length");
        }

        Map<Integer, byte[]> data = Tlvs.decodeMap(Arrays.copyOfRange(response, 1, response.length));

        boolean isLocked = readInt(data.get(TAG_CONFIG_LOCKED)) == 1;
        int serialNumber = readInt(data.get(TAG_SERIAL_NUMBER));
        FormFactor formFactor = FormFactor.valueOf(readInt(data.get(TAG_FORMFACTOR)));

        Version version;
        if (data.containsKey(TAG_FIRMWARE_VERSION)) {
            version = Version.fromBytes(data.get(TAG_FIRMWARE_VERSION));
        } else {
            version = defaultVersion;
        }

        short autoEjectTimeout = (short) readInt(data.get(TAG_AUTO_EJECT_TIMEOUT));
        byte challengeResponseTimeout = (byte) readInt(data.get(TAG_CHALLENGE_RESPONSE_TIMEOUT));
        int deviceFlags = readInt(data.get(TAG_DEVICE_FLAGS));

        Map<Transport, Integer> supportedCapabilities = new HashMap<>();
        Map<Transport, Integer> enabledCapabilities = new HashMap<>();

        if (version.major == 4 && version.minor == 2 && version.micro == 4) {
            // 4.2.4 doesn't report supported capabilities correctly, but they are always 0x3f.
            supportedCapabilities.put(Transport.USB, 0x3f);
        } else {
            supportedCapabilities.put(Transport.USB, readInt(data.get(TAG_USB_SUPPORTED)));
        }
        if (data.containsKey(TAG_USB_ENABLED) && version.major != 4) {
            // YK4 reports this incorrectly, instead use supportedCapabilities and USB mode.
            enabledCapabilities.put(Transport.USB, readInt(data.get(TAG_USB_ENABLED)));
        }

        if (data.containsKey(TAG_NFC_SUPPORTED)) {
            supportedCapabilities.put(Transport.NFC, readInt(data.get(TAG_NFC_SUPPORTED)));
            enabledCapabilities.put(Transport.NFC, readInt(data.get(TAG_NFC_ENABLED)));
        }

        return new DeviceInfo(
                new DeviceConfig(
                        enabledCapabilities,
                        autoEjectTimeout,
                        challengeResponseTimeout,
                        deviceFlags
                ), serialNumber, version, formFactor, supportedCapabilities, isLocked
        );
    }

    /**
     * Reads an int from a variable length byte array.
     */
    private static int readInt(@Nullable byte[] data) {
        if (data == null || data.length == 0) {
            return 0;
        }
        int value = 0;
        for (byte b : data) {
            value <<= 8;
            value += (0xff & b);
        }
        return value;
    }
}
