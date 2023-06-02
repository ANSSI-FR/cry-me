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

package com.yubico.yubikit.core.smartcard;

import com.yubico.yubikit.core.Transport;
import com.yubico.yubikit.core.Version;
import com.yubico.yubikit.core.application.ApplicationNotAvailableException;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * Support class for communication over a SmartCardConnection.
 * <p>
 * This class handles APDU encoding and chaining, and implements workarounds for known issues.
 */
public class SmartCardProtocol implements Closeable {
    private static final byte INS_SELECT = (byte) 0xa4;
    private static final byte P1_SELECT = (byte) 0x04;
    private static final byte P2_SELECT = (byte) 0x00;

    private static final byte INS_SEND_REMAINING = (byte) 0xc0;
    private static final byte SW1_HAS_MORE_DATA = 0x61;

    private static final int SHORT_APDU_MAX_CHUNK = 0xff;

    private final byte insSendRemaining;

    private final SmartCardConnection connection;

    private ApduFormat apduFormat = ApduFormat.SHORT;

    private boolean useTouchWorkaround = false;
    private long lastLongResponse = 0;

    /**
     * Create new instance of {@link SmartCardProtocol}
     * and selects the application for use
     *
     * @param connection connection to the YubiKey
     */
    public SmartCardProtocol(SmartCardConnection connection) {
        this(connection, INS_SEND_REMAINING);
    }

    public SmartCardProtocol(SmartCardConnection connection, byte insSendRemaining) {
        this.connection = connection;
        this.insSendRemaining = insSendRemaining;
    }

    @Override
    public void close() throws IOException {
        connection.close();
    }

    /**
     * Enable all relevant workarounds given the firmware version of the YubiKey.
     *
     * @param firmwareVersion the firmware version to use for detection to enable the workarounds
     */
    public void enableWorkarounds(Version firmwareVersion) {
        if (connection.getTransport() == Transport.USB
                && firmwareVersion.isAtLeast(4, 2, 0)
                && firmwareVersion.isLessThan(4, 2, 7)) {
            setEnableTouchWorkaround(true);
        }
    }

    /**
     * YubiKey 4.2.0 - 4.2.6 have an issue with the touch timeout being too short in certain cases. Enable this workaround
     * on such devices to trigger sending a dummy command which mitigates the issue.
     *
     * @param enableTouchWorkaround true to enable the workaround, false to disable it
     */
    public void setEnableTouchWorkaround(boolean enableTouchWorkaround) {
        this.useTouchWorkaround = enableTouchWorkaround;
    }

    /**
     * YubiKey NEO doesn't support extended APDU's for most applications.
     *
     * @param apduFormat the APDU encoding to use when sending commands
     */
    public void setApduFormat(ApduFormat apduFormat) {
        this.apduFormat = apduFormat;
    }

    /**
     * @return the underlying connection
     */
    public SmartCardConnection getConnection() {
        return connection;
    }

    /**
     * Sends an APDU to SELECT an Application.
     *
     * @param aid the AID to select.
     * @return the response data from selecting the Application
     * @throws IOException                      in case of connection or communication error
     * @throws ApplicationNotAvailableException in case the AID doesn't match an available application
     */
    public byte[] select(byte[] aid) throws IOException, ApplicationNotAvailableException {
        try {
            return sendAndReceive(new Apdu(0, INS_SELECT, P1_SELECT, P2_SELECT, aid));
        } catch (ApduException e) {
            // NEO sometimes returns INVALID_INSTRUCTION instead of FILE_NOT_FOUND
            if (e.getSw() == SW.FILE_NOT_FOUND || e.getSw() == SW.INVALID_INSTRUCTION) {
                throw new ApplicationNotAvailableException("The application couldn't be selected", e);
            }
            throw new IOException("Unexpected SW", e);
        }
    }

    /**
     * Sends APDU command and receives byte array from connection
     * In case if output has status code that it has remaining info sends another APDU command to receive what's remaining
     *
     * @param command well structured command that needs to be send
     * @return data blob concatenated from all APDU commands that were sent *set of output commands and send remaining commands)
     * @throws IOException   in case of connection and communication error
     * @throws ApduException in case if received error in APDU response
     */
    public byte[] sendAndReceive(Apdu command) throws IOException, ApduException {
        if (useTouchWorkaround && lastLongResponse > 0 && System.currentTimeMillis() - lastLongResponse < 2000) {
            connection.sendAndReceive(new byte[5]);  // Dummy APDU; returns an error
            lastLongResponse = 0;
        }
        ApduResponse response;
        byte[] getData;
        byte[] data = command.getData();
        switch (apduFormat) {
            case SHORT:
                int offset = 0;
                while (data.length - offset > SHORT_APDU_MAX_CHUNK) {
                    response = new ApduResponse(connection.sendAndReceive(encodeShortApdu((byte) (command.getCla() | 0x10), command.getIns(), command.getP1(), command.getP2(), data, offset, SHORT_APDU_MAX_CHUNK)));
                    if (response.getSw() != SW.OK) {
                        throw new ApduException(response.getSw());
                    }
                    offset += SHORT_APDU_MAX_CHUNK;
                }
                response = new ApduResponse(connection.sendAndReceive(encodeShortApdu(command.getCla(), command.getIns(), command.getP1(), command.getP2(), data, offset, data.length - offset)));
                getData = new byte[]{0x00, insSendRemaining, 0x00, 0x00, 0x00};
                break;
            case EXTENDED:
                response = new ApduResponse(connection.sendAndReceive(encodeExtendedApdu(command.getCla(), command.getIns(), command.getP1(), command.getP2(), data)));
                getData = new byte[]{0x00, insSendRemaining, 0x00, 0x00, 0x00, 0x00, 0x00};
                break;
            default:
                throw new IllegalStateException("Invalid APDU format");
        }

        // Read full response
        ByteArrayOutputStream readBuffer = new ByteArrayOutputStream();
        while (response.getSw() >> 8 == SW1_HAS_MORE_DATA) {
            readBuffer.write(response.getData());
            response = new ApduResponse(connection.sendAndReceive(getData));
        }

        if (response.getSw() != SW.OK) {
            throw new ApduException(response.getSw());
        }
        readBuffer.write(response.getData());
        byte[] responseData = readBuffer.toByteArray();

        if (useTouchWorkaround && responseData.length > 54) {
            lastLongResponse = System.currentTimeMillis();
        } else {
            lastLongResponse = 0;
        }
        return responseData;
    }

    private static byte[] encodeShortApdu(byte cla, byte ins, byte p1, byte p2, byte[] data, int offset, int length) {
        if (length > SHORT_APDU_MAX_CHUNK) {
            throw new IllegalArgumentException("Length must be no greater than " + SHORT_APDU_MAX_CHUNK);
        }
        return ByteBuffer.allocate(5 + length)
                .put(cla)
                .put(ins)
                .put(p1)
                .put(p2)
                .put((byte) length)
                .put(data, offset, length)
                .array();
    }

    private static byte[] encodeExtendedApdu(byte cla, byte ins, byte p1, byte p2, byte[] data) {
        return ByteBuffer.allocate(7 + data.length)
                .put(cla)
                .put(ins)
                .put(p1)
                .put(p2)
                .put((byte) 0x00)
                .putShort((short) data.length)
                .put(data)
                .array();
    }
}
