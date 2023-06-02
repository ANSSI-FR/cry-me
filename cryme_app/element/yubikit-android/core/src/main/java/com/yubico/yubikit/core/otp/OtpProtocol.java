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
package com.yubico.yubikit.core.otp;

import com.yubico.yubikit.core.Logger;
import com.yubico.yubikit.core.Version;
import com.yubico.yubikit.core.application.CommandException;
import com.yubico.yubikit.core.application.CommandState;
import com.yubico.yubikit.core.application.TimeoutException;
import com.yubico.yubikit.core.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import javax.annotation.Nullable;

public class OtpProtocol implements Closeable {
    private static final int FEATURE_RPT_SIZE = 8;
    private static final int FEATURE_RPT_DATA_SIZE = FEATURE_RPT_SIZE - 1;

    private static final int SLOT_DATA_SIZE = 64;
    private static final int FRAME_SIZE = SLOT_DATA_SIZE + 6;

    private static final int RESP_PENDING_FLAG = 0x40;    /* Response pending flag */
    private static final int SLOT_WRITE_FLAG = 0x80;    /* Write flag - set by app - cleared by device */
    private static final int RESP_TIMEOUT_WAIT_FLAG = 0x20;    /* Waiting for timeout operation - seconds left in lower 5 bits */
    private static final int DUMMY_REPORT_WRITE = 0x8f;    /* Write a dummy report to force update or abort */

    private static final int SEQUENCE_MASK = 0x1f;
    private static final int SEQUENCE_OFFSET = 0x4;

    private final CommandState defaultState = new CommandState();

    private final OtpConnection connection;
    private final Version version;

    public OtpProtocol(OtpConnection connection) throws IOException {
        this.connection = connection;

        byte[] featureReport = readFeatureReport();
        if (featureReport[4] == 3) {
            /* NEO, may have cached pgmSeq in arbitrator.
               Force communication with applet to refresh pgmSeq by
               writing an invalid scan map (which should fail). */
            byte[] scanMap = new byte[51];
            Arrays.fill(scanMap, (byte) 'c');
            try {
                sendAndReceive((byte) 0x12, scanMap, null);
            } catch (CommandException e) {
                // We expect this to fail
            }
        }
        version = Version.fromBytes(Arrays.copyOfRange(featureReport, 1, 4));
    }

    public Version getVersion() {
        return version;
    }

    @Override
    public void close() throws IOException {
        connection.close();
    }

    /**
     * Sends a command to the YubiKey, and reads the response.
     * If the command results in a configuration update, the programming sequence number is verified
     * and the updated status bytes are returned.
     *
     * @param slot  the slot to send to
     * @param data  the data payload to send
     * @param state optional CommandState for listening for user presence requirement and for cancelling a command
     * @return response data (including CRC) in the case of data, or an updated status struct
     * @throws IOException      in case of communication error
     * @throws CommandException in case the command failed
     */
    public byte[] sendAndReceive(byte slot, @Nullable byte[] data, @Nullable CommandState state) throws IOException, CommandException {
        byte[] payload;
        if (data == null) {
            payload = new byte[SLOT_DATA_SIZE];
        } else if (data.length > SLOT_DATA_SIZE) {
            throw new IllegalArgumentException("Payload too large for HID frame!");
        } else {
            payload = Arrays.copyOf(data, SLOT_DATA_SIZE);
        }
        return readFrame(sendFrame(slot, payload), state != null ? state : defaultState);
    }

    /**
     * Receive status bytes from YubiKey
     *
     * @return status bytes (first 3 bytes are the firmware version)
     * @throws IOException in case of communication error
     */
    public byte[] readStatus() throws IOException {
        byte[] featureReport = readFeatureReport();
        // disregard the first and last byte in the feature report
        return Arrays.copyOfRange(featureReport, 1, featureReport.length - 1);
    }

    /* Read a single 8 byte feature report */
    private byte[] readFeatureReport() throws IOException {
        byte[] bufferRead = new byte[FEATURE_RPT_SIZE];
        connection.receive(bufferRead);
        Logger.d("READ FEATURE REPORT: " + StringUtils.bytesToHex(bufferRead));
        return bufferRead;
    }

    /* Write a single 8 byte feature report */
    private void writeFeatureReport(byte[] buffer) throws IOException {
        Logger.d("WRITE FEATURE REPORT: " + StringUtils.bytesToHex(buffer));
        connection.send(buffer);
    }

    /* Sleep for up to ~1s waiting for the WRITE flag to be unset */
    private void awaitReadyToWrite() throws IOException {
        for (int i = 0; i < 20; i++) {
            if ((readFeatureReport()[FEATURE_RPT_DATA_SIZE] & SLOT_WRITE_FLAG) == 0) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                //Ignore
            }
        }
        throw new IOException("Timeout waiting for YubiKey to become ready to receive");
    }

    /* All-zero packets are skipped, except for the very first and last packets */
    private static boolean shouldSend(byte[] packet, byte seq) {
        if (seq == 0 || seq == 9) {
            return true;
        }
        for (int i = 0; i < 7; i++) {
            if (packet[i] != 0) {
                return true;
            }
        }
        return false;
    }

    /* Packs and sends one 70 byte frame */
    private int sendFrame(byte slot, byte[] payload) throws IOException {
        Logger.d(String.format("Sending payload over HID to slot 0x%02x: ", 0xff & slot) + StringUtils.bytesToHex(payload));

        // Format Frame
        ByteBuffer buf = ByteBuffer.allocate(FRAME_SIZE)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(payload)
                .put(slot)
                .putShort(ChecksumUtils.calculateCrc(payload, payload.length))
                .put(new byte[3]);  // 3-byte filler
        buf.flip();

        // Send frame
        int programmingSequence = readFeatureReport()[SEQUENCE_OFFSET];
        byte seq = 0;
        byte[] report = new byte[FEATURE_RPT_SIZE];
        while (buf.hasRemaining()) {
            buf.get(report, 0, FEATURE_RPT_DATA_SIZE);
            if (shouldSend(report, seq)) {
                report[FEATURE_RPT_DATA_SIZE] = (byte) (0x80 | seq);
                awaitReadyToWrite();
                writeFeatureReport(report);
            }
            seq++;
        }
        return programmingSequence;
    }

    /* Reads one frame */
    private byte[] readFrame(int programmingSequence, CommandState state) throws IOException, CommandException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        byte seq = 0;
        boolean needsTouch = false;

        while (true) {
            byte[] report = readFeatureReport();
            byte statusByte = report[FEATURE_RPT_DATA_SIZE];
            if ((statusByte & RESP_PENDING_FLAG) != 0) { // Response packet
                if (seq == (statusByte & SEQUENCE_MASK)) {
                    // Correct sequence
                    stream.write(report, 0, FEATURE_RPT_DATA_SIZE);
                    seq++;
                } else if (0 == (statusByte & SEQUENCE_MASK)) {
                    // Transmission complete
                    resetState();
                    byte[] response = stream.toByteArray();
                    Logger.d(response.length + " bytes read over HID: " + StringUtils.bytesToHex(response));
                    return response;
                }
            } else if (statusByte == 0) { // Status response
                int prgSeq = report[SEQUENCE_OFFSET];
                if (stream.size() > 0) {
                    throw new IOException("Incomplete transfer");
                } else if ((prgSeq == programmingSequence + 1) || (programmingSequence > 0 && prgSeq == 0 && report[SEQUENCE_OFFSET + 1] == 0)) {
                    // Sequence updated, return status.
                    // Note that when deleting the "last" slot so no slots are valid, the programming sequence is set to 0.
                    byte[] status = Arrays.copyOfRange(report, 1, 7); // Skip first and last bytes
                    Logger.d("HID programming sequence updated. New status: " + StringUtils.bytesToHex(status));
                    return status;
                } else if (needsTouch) {
                    throw new TimeoutException("Timed out waiting for touch");
                } else {
                    throw new CommandRejectedException("No data");
                }
            } else { // Need to wait
                long timeout;
                if ((statusByte & RESP_TIMEOUT_WAIT_FLAG) != 0) {
                    state.onKeepAliveStatus(CommandState.STATUS_UPNEEDED);
                    needsTouch = true;
                    timeout = 100;
                } else {
                    state.onKeepAliveStatus(CommandState.STATUS_PROCESSING);
                    timeout = 20;
                }
                if (state.waitForCancel(timeout)) {
                    resetState();
                    throw new TimeoutException("Command cancelled by CommandState");
                }
            }
        }
    }

    /**
     * Reset the state of YubiKey from reading/means that there won't be any data returned
     */
    private void resetState() throws IOException {
        byte[] buffer = new byte[FEATURE_RPT_SIZE];
        buffer[FEATURE_RPT_SIZE - 1] = (byte) DUMMY_REPORT_WRITE; /* Invalid sequence = update only */
        writeFeatureReport(buffer);
    }
}