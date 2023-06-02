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

package com.yubico.yubikit.yubiotp;

import com.yubico.yubikit.core.Transport;
import com.yubico.yubikit.core.Version;
import com.yubico.yubikit.core.YubiKeyDevice;
import com.yubico.yubikit.core.application.ApplicationNotAvailableException;
import com.yubico.yubikit.core.application.ApplicationSession;
import com.yubico.yubikit.core.application.BadResponseException;
import com.yubico.yubikit.core.application.CommandException;
import com.yubico.yubikit.core.application.CommandState;
import com.yubico.yubikit.core.application.Feature;
import com.yubico.yubikit.core.otp.ChecksumUtils;
import com.yubico.yubikit.core.otp.OtpConnection;
import com.yubico.yubikit.core.otp.OtpProtocol;
import com.yubico.yubikit.core.smartcard.Apdu;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;
import com.yubico.yubikit.core.smartcard.SmartCardProtocol;
import com.yubico.yubikit.core.util.Callback;
import com.yubico.yubikit.core.util.Result;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.annotation.Nullable;

/**
 * Application to use and configure the OTP application of the YubiKey.
 * This applications supports configuration of the two YubiKey "OTP slots" which are typically activated by pressing
 * the capacitive sensor on the YubiKey for either a short or long press.
 * <p>
 * Each slot can be configured with one of the following types of credentials:
 * - YubiOTP - a Yubico OTP (One Time Password) credential.
 * - OATH-HOTP - a counter based (HOTP) OATH OTP credential (see https://tools.ietf.org/html/rfc4226).
 * - Static Password - a static (non-changing) password.
 * - Challenge-Response - a HMAC-SHA1 key which can be accessed programmatically.
 * <p>
 * Additionally for NFC enabled YubiKeys, one slot can be configured to be output over NDEF as part of a URL payload.
 */
public class YubiOtpSession extends ApplicationSession<YubiOtpSession> {
    public static final String DEFAULT_NDEF_URI = "https://my.yubico.com/yk/#";

    // Features
    /**
     * Support for checking if a slot is configured via the ConfigState.
     */
    public static final Feature<YubiOtpSession> FEATURE_CHECK_CONFIGURED = new Feature.Versioned<>("Check if a slot is configured", 2, 1, 0);
    /**
     * Support for checking if a configured slot requires touch via the ConfigState.
     */
    public static final Feature<YubiOtpSession> FEATURE_CHECK_TOUCH_TRIGGERED = new Feature.Versioned<>("Check if a slot is triggered by touch", 3, 0, 0);
    /**
     * Support for HMAC-SHA1 challenge response functionality.
     */
    public static final Feature<YubiOtpSession> FEATURE_CHALLENGE_RESPONSE = new Feature.Versioned<>("Challenge-Response", 2, 2, 0);

    /**
     * Support for swapping slot configurations.
     */
    public static final Feature<YubiOtpSession> FEATURE_SWAP = new Feature.Versioned<>("Swap Slots", 2, 3, 0);
    /**
     * Support for updating an already configured slot.
     */
    public static final Feature<YubiOtpSession> FEATURE_UPDATE = new Feature.Versioned<>("Update Slot", 2, 3, 0);
    /**
     * Support for NDEF configuration.
     */
    public static final Feature<YubiOtpSession> FEATURE_NDEF = new Feature.Versioned<>("NDEF", 3, 0, 0);

    private static final int ACC_CODE_SIZE = 6;     // Size of access code to re-program device
    private static final int CONFIG_SIZE = 52;      // Size of config struct (excluding current access code)
    private static final int NDEF_DATA_SIZE = 54;   // Size of the NDEF payload data

    private static final byte[] AID = new byte[]{(byte) 0xa0, 0x00, 0x00, 0x05, 0x27, 0x20, 0x01, 0x01};
    private static final byte[] MGMT_AID = new byte[]{(byte) 0xa0, 0x00, 0x00, 0x05, 0x27, 0x47, 0x11, 0x17};
    private static final byte INS_CONFIG = 0x01;

    private static final int HMAC_CHALLENGE_SIZE = 64;
    private static final int HMAC_RESPONSE_SIZE = 20;

    private static final byte CMD_CONFIG_1 = 0x1;
    private static final byte CMD_NAV = 0x2;
    private static final byte CMD_CONFIG_2 = 0x3;
    private static final byte CMD_UPDATE_1 = 0x4;
    private static final byte CMD_UPDATE_2 = 0x5;
    private static final byte CMD_SWAP = 0x6;
    private static final byte CMD_NDEF_1 = 0x8;
    private static final byte CMD_NDEF_2 = 0x9;
    private static final byte CMD_DEVICE_SERIAL = 0x10;
    private static final byte CMD_SCAN_MAP = 0x12;
    private static final byte CMD_CHALLENGE_OTP_1 = 0x20;
    private static final byte CMD_CHALLENGE_OTP_2 = 0x28;
    private static final byte CMD_CHALLENGE_HMAC_1 = 0x30;
    private static final byte CMD_CHALLENGE_HMAC_2 = 0x38;

    private final Backend<?> backend;

    /**
     * Connects to a YubiKeyDevice and establishes a new session with a YubiKeys OTP application.
     * <p>
     * This method will use whichever connection type is available.
     *
     * @param device A YubiKey device to use
     */
    public static void create(YubiKeyDevice device, Callback<Result<YubiOtpSession, Exception>> callback) {
        if (device.supportsConnection(OtpConnection.class)) {
            device.requestConnection(OtpConnection.class, value -> callback.invoke(Result.of(() -> new YubiOtpSession(value.getValue()))));
        } else if (device.supportsConnection(SmartCardConnection.class)) {
            device.requestConnection(SmartCardConnection.class, value -> callback.invoke(Result.of(() -> new YubiOtpSession(value.getValue()))));
        } else {
            callback.invoke(Result.failure(new ApplicationNotAvailableException("Session does not support any compatible connection type")));
        }
    }

    /**
     * Create new instance of {@link YubiOtpSession} using an {@link SmartCardConnection}.
     * NOTE: Not all functionality is available over all transports. Over USB, some functionality may be blocked when
     * not using an OtpConnection.
     *
     * @param connection an Iso7816Connection with a YubiKey
     * @throws IOException                      in case of connection error
     * @throws ApplicationNotAvailableException if the application is missing or disabled
     */
    public YubiOtpSession(SmartCardConnection connection) throws IOException, ApplicationNotAvailableException {
        Version version = null;
        SmartCardProtocol protocol = new SmartCardProtocol(connection);

        if (connection.getTransport() == Transport.NFC) {
            // If available, this is more reliable than status.getVersion() over NFC
            try {
                byte[] response = protocol.select(MGMT_AID);
                version = Version.parse(new String(response, StandardCharsets.UTF_8));
            } catch (ApplicationNotAvailableException e) {
                // NB: YubiKey NEO doesn't support the Management Application over NFC.
                // NEO: version will be populated further down.
            }
        }

        byte[] statusBytes = protocol.select(AID);
        if (version == null) {
            // We didn't get a version above, get it from the status struct.
            version = Version.fromBytes(statusBytes);
        }

        protocol.enableWorkarounds(version);

        backend = new Backend<SmartCardProtocol>(protocol, version, parseConfigState(version, statusBytes)) {
            // 5.0.0-5.2.5 have an issue with status over NFC
            private final boolean dummyStatus = connection.getTransport() == Transport.NFC && version.isAtLeast(5, 0, 0) && version.isLessThan(5, 2, 5);

            {
                if (dummyStatus) { // We can't read the status, so use a dummy with both slots marked as configured.
                    configurationState = new ConfigurationState(version, (short) 3);
                }
            }

            @Override
            void writeToSlot(byte slot, byte[] data) throws IOException, CommandException {
                byte[] status = delegate.sendAndReceive(new Apdu(0, INS_CONFIG, slot, 0, data));
                if (!dummyStatus) {
                    configurationState = parseConfigState(this.version, status);
                }
            }

            @Override
            byte[] sendAndReceive(byte slot, byte[] data, int expectedResponseLength, @Nullable CommandState state) throws IOException, CommandException {
                byte[] response = delegate.sendAndReceive(new Apdu(0, INS_CONFIG, slot, 0, data));
                if (expectedResponseLength != response.length) {
                    throw new BadResponseException("Unexpected response length");
                }
                return response;
            }
        };
    }

    /**
     * Create new instance of {@link YubiOtpSession} using an {@link OtpConnection}.
     *
     * @param connection an OtpConnection with YubiKey
     * @throws IOException in case of connection error
     */
    public YubiOtpSession(OtpConnection connection) throws IOException {
        OtpProtocol protocol = new OtpProtocol(connection);
        byte[] statusBytes = protocol.readStatus();
        Version version = protocol.getVersion();
        backend = new Backend<OtpProtocol>(protocol, version, parseConfigState(version, statusBytes)) {
            @Override
            void writeToSlot(byte slot, byte[] data) throws IOException, CommandException {
                configurationState = parseConfigState(version, delegate.sendAndReceive(slot, data, null));
            }

            @Override
            byte[] sendAndReceive(byte slot, byte[] data, int expectedResponseLength, @Nullable CommandState state) throws IOException, CommandException {
                byte[] response = delegate.sendAndReceive(slot, data, state);
                if (ChecksumUtils.checkCrc(response, expectedResponseLength + 2)) {
                    return Arrays.copyOf(response, expectedResponseLength);
                }
                throw new IOException("Invalid CRC");
            }
        };
    }

    @Override
    public void close() throws IOException {
        backend.close();
    }

    /**
     * Get the configuration state of the application.
     *
     * @return the current configuration state of the two slots.
     */
    public ConfigurationState getConfigurationState() {
        return backend.configurationState;
    }

    /**
     * Get the firmware version of the YubiKey
     *
     * @return Yubikey firmware version
     */
    @Override
    public Version getVersion() {
        return backend.version;
    }

    /**
     * Get the serial number of the YubiKey.
     * Note that the EXTFLAG_SERIAL_API_VISIBLE flag must be set for this command to work.
     *
     * @return the serial number
     * @throws IOException      in case of communication error
     * @throws CommandException in case of an error response from the YubiKey
     */
    public int getSerialNumber() throws IOException, CommandException {
        return ByteBuffer.wrap(backend.sendAndReceive(CMD_DEVICE_SERIAL, new byte[0], 4, null)).getInt();
    }

    /**
     * Swaps the two slot configurations with each other.
     *
     * @throws IOException      in case of communication error
     * @throws CommandException in case of an error response from the YubiKey
     */
    public void swapConfigurations() throws IOException, CommandException {
        require(FEATURE_SWAP);
        writeConfig(CMD_SWAP, new byte[0], null);
    }

    /**
     * Delete the contents of a slot.
     * <p>
     * NOTE: Attempting to delete an empty slot will under certain circumstances fail, resulting in
     * a {@link com.yubico.yubikit.core.otp.CommandRejectedException} being thrown. Prefer to check
     * if a slot is configured before calling delete.
     *
     * @param slot       the slot to delete
     * @param curAccCode the currently set access code, if needed
     * @throws IOException      in case of communication error
     * @throws CommandException in case of an error response from the YubiKey
     */
    public void deleteConfiguration(Slot slot, @Nullable byte[] curAccCode) throws IOException, CommandException {
        writeConfig(
                slot.map(CMD_CONFIG_1, CMD_CONFIG_2),
                new byte[CONFIG_SIZE],
                curAccCode
        );
    }

    /**
     * Write a configuration to a slot, overwriting previous configuration (if present).
     *
     * @param slot          the slot to write to
     * @param configuration the new configuration to write
     * @param accCode       the access code to set (or null, to not set an access code)
     * @param curAccCode    the current access code, if one is set for the target slot
     * @throws IOException      in case of communication error
     * @throws CommandException in case of an error response from the YubiKey
     */
    public void putConfiguration(Slot slot, SlotConfiguration configuration, @Nullable byte[] accCode, @Nullable byte[] curAccCode) throws IOException, CommandException {
        if (!configuration.isSupportedBy(backend.version)) {
            throw new UnsupportedOperationException("This configuration update is not supported on this YubiKey version");
        }
        writeConfig(
                slot.map(CMD_CONFIG_1, CMD_CONFIG_2),
                configuration.getConfig(accCode),
                curAccCode
        );
    }

    /**
     * Update the configuration of a slot, keeping the credential.
     * <p>
     * This functionality requires support for {@link #FEATURE_UPDATE}, available on YubiKey 2.3 or later.
     *
     * @param slot          the slot to update
     * @param configuration the updated flags tp set
     * @param accCode       the access code to set
     * @param curAccCode    the current accedd code, if needed
     * @throws IOException      in case of communication error
     * @throws CommandException in case of an error response from the YubiKey
     */
    public void updateConfiguration(Slot slot, UpdateConfiguration configuration, @Nullable byte[] accCode, @Nullable byte[] curAccCode) throws IOException, CommandException {
        require(FEATURE_UPDATE);
        if (!configuration.isSupportedBy(backend.version)) {
            throw new UnsupportedOperationException("This configuration is not supported on this YubiKey version");
        }
        if (!Arrays.equals(accCode, curAccCode) && getVersion().isAtLeast(4, 3, 2) && getVersion().isLessThan(4, 3, 6)) {
            throw new UnsupportedOperationException("The access code cannot be updated on this YubiKey. Instead, delete the slot and configure it anew.");
        }
        writeConfig(
                slot.map(CMD_UPDATE_1, CMD_UPDATE_2),
                configuration.getConfig(accCode),
                curAccCode
        );
    }

    /**
     * Configure the NFC NDEF payload, and which slot to use.
     * <p>
     * This functionality requires support for {@link #FEATURE_NDEF}, available on YubiKey 3 or later.
     *
     * @param slot       the YubiKey slot to append to the uri payload
     * @param uri        the URI prefix (if null, the default "https://my.yubico.com/yk/#" will be used)
     * @param curAccCode the current access code, if needed
     * @throws IOException      in case of communication error
     * @throws CommandException in case of an error response from the YubiKey
     */
    public void setNdefConfiguration(Slot slot, @Nullable String uri, @Nullable byte[] curAccCode) throws IOException, CommandException {
        require(FEATURE_NDEF);
        writeConfig(
                slot.map(CMD_NDEF_1, CMD_NDEF_2),
                buildNdefConfig(uri == null ? DEFAULT_NDEF_URI : uri),
                curAccCode
        );
    }


    /**
     * Calculates HMAC-SHA1 on given challenge (using secret that configured/programmed on YubiKey)
     * <p>
     * This functionality requires support for {@link #FEATURE_CHALLENGE_RESPONSE}, available on YubiKey 2.2 or later.
     *
     * @param slot      the slot on YubiKey that configured with challenge response secret
     * @param challenge generated challenge that will be sent
     * @param state     if false, the command will be aborted in case the credential requires user touch
     * @return response on challenge returned from YubiKey
     * @throws IOException      in case of communication error, or no key configured in slot
     * @throws CommandException in case of an error response from the YubiKey
     */
    public byte[] calculateHmacSha1(Slot slot, byte[] challenge, @Nullable CommandState state) throws IOException, CommandException {
        require(FEATURE_CHALLENGE_RESPONSE);

        // Pad challenge with byte different from last.
        byte[] padded = new byte[HMAC_CHALLENGE_SIZE];
        Arrays.fill(padded, (byte) (challenge[challenge.length - 1] == 0 ? 1 : 0));
        System.arraycopy(challenge, 0, padded, 0, challenge.length);

        // response for HMAC-SHA1 challenge response is always 20 bytes
        return backend.sendAndReceive(
                slot.map(CMD_CHALLENGE_HMAC_1, CMD_CHALLENGE_HMAC_2),
                padded,
                HMAC_RESPONSE_SIZE,
                state
        );
    }

    private void writeConfig(byte commandSlot, byte[] config, @Nullable byte[] curAccCode) throws IOException, CommandException {
        backend.writeToSlot(
                commandSlot,
                ByteBuffer.allocate(config.length + ACC_CODE_SIZE)
                        .put(config)
                        .put(curAccCode == null ? new byte[ACC_CODE_SIZE] : curAccCode)
                        .array()
        );
    }

    private static ConfigurationState parseConfigState(Version version, byte[] status) {
        return new ConfigurationState(version, ByteBuffer.wrap(status, 4, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
    }

    // From nfcforum-ts-rtd-uri-1.0.pdf
    private static final String[] NDEF_URL_PREFIXES = {
            "http://www.",
            "https://www.",
            "http://",
            "https://",
            "tel:",
            "mailto:",
            "ftp://anonymous:anonymous@",
            "ftp://ftp.",
            "ftps://",
            "sftp://",
            "smb://",
            "nfs://",
            "ftp://",
            "dav://",
            "news:",
            "telnet://",
            "imap:",
            "rtsp://",
            "urn:",
            "pop:",
            "sip:",
            "sips:",
            "tftp:",
            "btspp://",
            "btl2cap://",
            "btgoep://",
            "tcpobex://",
            "irdaobex://",
            "file://",
            "urn:epc:id:",
            "urn:epc:tag:",
            "urn:epc:pat:",
            "urn:epc:raw:",
            "urn:epc:",
            "urn:nfc:"
    };

    private static byte[] buildNdefConfig(String uri) {
        byte idCode = 0;
        for (int i = 0; i < NDEF_URL_PREFIXES.length; i++) {
            String prefix = NDEF_URL_PREFIXES[i];
            if (uri.startsWith(prefix)) {
                idCode = (byte) (i + 1);
                uri = uri.substring(prefix.length());
                break;
            }
        }
        byte[] uriBytes = uri.getBytes(StandardCharsets.UTF_8);
        int dataLength = 1 + uriBytes.length;
        if (dataLength > NDEF_DATA_SIZE) {
            throw new IllegalArgumentException("URI payload too large");
        }
        return ByteBuffer.allocate(2 + NDEF_DATA_SIZE)
                .put((byte) dataLength)
                .put((byte) 'U')
                .put(idCode)
                .put(uriBytes)
                .array();
    }

    private static abstract class Backend<T extends Closeable> implements Closeable {
        protected final T delegate;
        protected final Version version;
        protected ConfigurationState configurationState;

        private Backend(T delegate, Version version, ConfigurationState configurationState) {
            this.version = version;
            this.delegate = delegate;
            this.configurationState = configurationState;
        }

        abstract void writeToSlot(byte slot, byte[] data) throws IOException, CommandException;

        abstract byte[] sendAndReceive(byte slot, byte[] data, int expectedResponseLength, @Nullable CommandState state) throws IOException, CommandException;

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}