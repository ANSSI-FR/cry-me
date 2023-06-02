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

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;

import com.yubico.yubikit.core.Logger;
import com.yubico.yubikit.core.Transport;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;
import com.yubico.yubikit.core.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Locale;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * USB service for interacting with the YubiKey
 * https://www.usb.org/sites/default/files/DWG_Smart-Card_CCID_Rev110.pdf
 */
public class UsbSmartCardConnection extends UsbYubiKeyConnection implements SmartCardConnection {

    private static final int TIMEOUT = 1000;

    /**
     * Command Pipe, Bulk-OUT Messages
     * Message Name                             type
     * PC_to_RDR_IccPowerOn                     62h
     * PC_to_RDR_IccPowerOff                    63h
     * PC_to_RDR_GetSlotStatus                  65h
     * PC_to_RDR_XfrBlock                       6Fh
     * PC_to_RDR_GetParameters                  6Ch
     * PC_to_RDR_ResetParameters                6Dh
     * PC_to_RDR_SetParameters                  61h
     * PC_to_RDR_Escape                         6Bh
     * PC_to_RDR_IccClock                       6Eh
     * PC_to_RDR_T0APDU                         6Ah
     * PC_to_RDR_Secure                         69h
     * PC_to_RDR_Mechanical                     71h
     * PC_to_RDR_Abort                          72h
     * PC_to_RDR_SetDataRateAndClockFrequency   73h
     */
    private static final byte POWER_ON_MESSAGE_TYPE = (byte) 0x62;
    private static final byte REQUEST_MESSAGE_TYPE = (byte) 0x6f;
    private static final byte RESPONSE_DATA_BLOCK = (byte) 0x80;

    private static final byte STATUS_TIME_EXTENSION = (byte) 0x80;

    private final UsbDeviceConnection connection;
    private final UsbEndpoint endpointOut, endpointIn;
    private final byte[] atr;

    private byte sequence = 0;


    /**
     * Sets endpoints and connection and sends power on command
     * if ATR is invalid then throws YubikeyCommunicationException
     *
     * @param connection    open usb connection
     * @param ccidInterface ccid interface that was claimed
     * @param endpointIn    channel for sending data over USB.
     * @param endpointOut   channel for receiving data over USB.
     */
    UsbSmartCardConnection(UsbDeviceConnection connection, UsbInterface ccidInterface, UsbEndpoint endpointIn, UsbEndpoint endpointOut) throws IOException {
        super(connection, ccidInterface);

        this.connection = connection;
        this.endpointIn = endpointIn;
        this.endpointOut = endpointOut;
        // PC_to_RDR_IccPowerOn command makes the slot "active" if it was "inactive"
        atr = transceive(POWER_ON_MESSAGE_TYPE, new byte[0]);
    }

    @Override
    public Transport getTransport() {
        return Transport.USB;
    }

    @Override
    public byte[] sendAndReceive(byte[] apdu) throws IOException {
        return transceive(REQUEST_MESSAGE_TYPE, apdu);
    }

    /**
     * Does the data exchange between phone and connected usb device with bulk messages
     * All bulk messages begin with a 10-bytes header, followed by message-specific data.
     *
     * @param type the message type identifies the message
     *             Message Name                             type
     *             PC_to_RDR_IccPowerOn                     62h
     *             PC_to_RDR_IccPowerOff                    63h
     *             PC_to_RDR_GetSlotStatus                  65h
     *             PC_to_RDR_XfrBlock                       6Fh
     *             PC_to_RDR_GetParameters                  6Ch
     *             PC_to_RDR_ResetParameters                6Dh
     *             PC_to_RDR_SetParameters                  61h
     *             PC_to_RDR_Escape                         6Bh
     *             PC_to_RDR_IccClock                       6Eh
     *             PC_to_RDR_T0APDU                         6Ah
     *             PC_to_RDR_Secure                         69h
     *             PC_to_RDR_Mechanical                     71h
     *             PC_to_RDR_Abort                          72h
     *             PC_to_RDR_SetDataRateAndClockFrequency   73h
     * @param data message-specific data that needs to be sent to usb device
     * @return received message-specific data from usb device
     * @throws IOException in case if there is communication error occurs or received data is invalid
     */
    private byte[] transceive(byte type, byte[] data) throws IOException {
        // 1. prepare data for sending
        MessageHeader prefix = new MessageHeader(type, data.length, sequence++);
        ByteBuffer byteBuffer = ByteBuffer.allocate(prefix.size() + data.length).order(ByteOrder.LITTLE_ENDIAN)
                .put(prefix.array())
                .put(data);

        // 2. sent data to device
        byte[] bufferOut = byteBuffer.array();
        int bytesSent = 0;
        int bytesSentPackage = 0;
        while (bytesSent < bufferOut.length || bytesSentPackage == endpointOut.getMaxPacketSize()) {
            bytesSentPackage = connection.bulkTransfer(endpointOut, bufferOut, bytesSent, bufferOut.length - bytesSent, TIMEOUT);
            if (bytesSentPackage > 0) {
                Logger.d(bytesSentPackage + " bytes sent over ccid: " + StringUtils.bytesToHex(bufferOut, bytesSent, bytesSentPackage));
                bytesSent += bytesSentPackage;
            } else if (bytesSentPackage < 0) {
                throw new IOException("Failed to send " + (bufferOut.length - bytesSent) + " bytes");
            } else {
                // 0 is still considered as success in bulkTransfer description
                // Scenario: if last package size was equal to endpointOut.getMaxPacketSize()
                // we are sending empty package after that to notify end of bulk transfer
                break;
            }
        }

        // 3. read data from device until we receive non-full packet/blob
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        int bytesRead;
        MessageHeader messageHeader = null;

        boolean receivedExpectedPrefix = false;
        byte[] bufferRead = new byte[endpointIn.getMaxPacketSize()];
        boolean responseRequiresTimeExtension = false;
        do {
            bytesRead = connection.bulkTransfer(endpointIn, bufferRead, bufferRead.length, TIMEOUT);
            if (bytesRead > 0) {
                Logger.d(bytesRead + " bytes received: " + StringUtils.bytesToHex(bufferRead, 0, bytesRead));

                if (receivedExpectedPrefix) {
                    stream.write(bufferRead, 0, bytesRead);
                } else {
                    // 4. parse received data and make sure it's proper format
                    messageHeader = new MessageHeader(bufferRead);
                    responseRequiresTimeExtension = (messageHeader.status & STATUS_TIME_EXTENSION) == STATUS_TIME_EXTENSION;
                    if (messageHeader.verify((byte) (sequence - 1))) {
                        // if we received expected prefix we can save the rest of received data without verification
                        receivedExpectedPrefix = true;
                        stream.write(bufferRead, 0, bytesRead);
                    } else if (messageHeader.error != 0 && !responseRequiresTimeExtension) {
                        Logger.d(String.format(
                                Locale.ROOT, "Invalid response from card reader bStatus=0x%02X and bError=0x%02X", messageHeader.status, messageHeader.error));
                        throw new IOException("Invalid response from card reader");
                    }
                }
            } else if (bytesRead < 0) {
                throw new IOException("Failed to read response");
            }
        } while ((bytesRead > 0 && bytesRead == bufferRead.length) || responseRequiresTimeExtension);


        // 5. prepare data for returning to user
        byte[] output = stream.toByteArray();
        if (messageHeader == null || output.length < messageHeader.size()) {
            throw new IOException("Response is invalid");
        }
        int dataLength = Math.min(output.length - messageHeader.size(), messageHeader.dataLength);
        return Arrays.copyOfRange(output, messageHeader.size(), messageHeader.size() + dataLength);
    }

    /**
     * Class parses 10-bytes header of CCID message
     * The header consists of a message type (1 byte), a dataLength field (four bytes), the slot number
     * (1 byte), a sequence number field (1 byte), and either three message specific bytes, or a
     * status field (1 byte), an error field and one message specific byte. The purpose of the
     * 10-byte header is to provide a constant offset at which message data begins across all
     * messages.
     */
    private static class MessageHeader {
        private static final int SIZE_OF_CCID_PREFIX = 10;
        private static final byte[] MESSAGE_SPECIFIC_BYTES = new byte[]{0, 0, 0};
        private static final byte SLOT_NUMBER = 0;

        private byte type;
        private int dataLength;
        private byte slot;
        private byte sequence;
        private byte status;
        private byte error;
        @SuppressFBWarnings("URF_UNREAD_FIELD")
        private byte messageSpecificByte;

        private MessageHeader(byte[] buffer) {
            if (buffer.length > SIZE_OF_CCID_PREFIX) {
                ByteBuffer responseBuffer = ByteBuffer.wrap(buffer, 0, SIZE_OF_CCID_PREFIX).order(ByteOrder.LITTLE_ENDIAN);
                type = responseBuffer.get();
                dataLength = responseBuffer.getInt();
                slot = responseBuffer.get();
                sequence = responseBuffer.get();
                status = responseBuffer.get();
                error = responseBuffer.get();
                messageSpecificByte = responseBuffer.get();
            }
        }

        private MessageHeader(byte type, int length, byte sequence) {
            this.type = type;
            this.dataLength = length;
            this.slot = SLOT_NUMBER;
            this.sequence = sequence;
        }

        private byte[] array() {
            ByteBuffer byteBuffer = ByteBuffer.allocate(SIZE_OF_CCID_PREFIX).order(ByteOrder.LITTLE_ENDIAN)
                    .put(type)
                    .putInt(dataLength)
                    .put(slot)
                    .put(sequence)
                    .put(MESSAGE_SPECIFIC_BYTES);
            return byteBuffer.array();
        }

        private int size() {
            return SIZE_OF_CCID_PREFIX;
        }

        /**
         * The response (Bulk-IN message) always contains the exact same slot number, and
         * sequence number fields from the header that was contained in the Bulk-OUT command
         * message.
         *
         * @param sequence Bulk-OUT message sequence
         * @return true if prefix has expected format
         */
        private boolean verify(byte sequence) {
            if (this.type != RESPONSE_DATA_BLOCK) {
                return false;
            }
            if (this.slot != SLOT_NUMBER) {
                return false;
            }
            if (this.sequence != sequence) {
                return false;
            }
            if (this.status != 0) {
                return false;
            }

            // Note: according to documentation ignore error if status is 0
            return true;
        }
    }

}
