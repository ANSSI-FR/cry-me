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

package com.yubico.yubikit.android.transport.usb.connection;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;

import com.yubico.yubikit.testing.Codec;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UsbSmartCardConnectionTest {
    private final UsbDeviceConnection usbDeviceConnection = mock(UsbDeviceConnection.class);
    private final UsbInterface usbInterface = mock(UsbInterface.class);
    private final UsbEndpoint usbEndpointIn = mock(UsbEndpoint.class);
    private final UsbEndpoint usbEndpointOut = mock(UsbEndpoint.class);
    private final List<String> packetsIn = new ArrayList<>();
    private final List<byte[]> packetsOut = new ArrayList<>();

    private void assertSent(String hex) {
        Assert.assertArrayEquals("Unexpected packet sent", Codec.fromHex(hex), packetsOut.remove(0));
    }

    @Before
    public void setup() {
        when(usbEndpointIn.getMaxPacketSize()).thenReturn(64);
        when(usbEndpointOut.getMaxPacketSize()).thenReturn(64);

        when(usbDeviceConnection.bulkTransfer(eq(usbEndpointOut), any(), anyInt(), anyInt(), anyInt())).then(invocation -> {
            byte[] buffer = invocation.getArgument(1);
            int offset = invocation.getArgument(2);
            int length = invocation.getArgument(3);
            int bytesSent = Math.min(64, length);
            packetsOut.add(Arrays.copyOfRange(buffer, offset, offset + bytesSent));
            return bytesSent;
        });

        when(usbDeviceConnection.bulkTransfer(eq(usbEndpointIn), any(), anyInt(), anyInt())).then(invocation -> {
            byte[] buffer = invocation.getArgument(1);
            byte[] packet = Codec.fromHex(packetsIn.remove(0));
            System.arraycopy(packet, 0, buffer, 0, packet.length);
            return packet.length;
        });
    }

    @After
    public void teardown() {
        Assert.assertTrue(packetsIn.size() + " un-asserted packets in read buffer", packetsIn.isEmpty());
        Assert.assertTrue(packetsOut.size() + " un-asserted packets in sent buffer", packetsOut.isEmpty());
    }

    private UsbSmartCardConnection getConnection() throws IOException {
        // ATR - response to power on
        packetsIn.add("801700000000000000003bfd1300008131fe158073c021c057597562694b657940");
        UsbSmartCardConnection connection = new UsbSmartCardConnection(usbDeviceConnection, usbInterface, usbEndpointIn, usbEndpointOut);
        assertSent("62000000000000000000");  // Power on command
        return connection;
    }

    @Test
    public void testSendAndReceive() throws IOException {
        UsbSmartCardConnection connection = getConnection();

        packetsIn.add("800500000000010000000102039000");
        byte[] response = connection.sendAndReceive(Codec.fromHex("0001020300"));
        assertSent("6f0500000000010000000001020300");

        Assert.assertArrayEquals(Codec.fromHex("0102039000"), response);
    }

    @Test
    public void testSendChunked() throws IOException {
        UsbSmartCardConnection connection = getConnection();

        packetsIn.add("800200000000010000009000");
        connection.sendAndReceive(Codec.fromHex("00010000320001020304050607080900010203040506070809000102030405060708090001020304050607080900010203040506070809"));

        assertSent("6f370000000001000000000100003200010203040506070809000102030405060708090001020304050607080900010203040506070809000102030405060708");
        assertSent("09");
    }

    @Test
    public void testSendEmptyPacketOnExactMultiple() throws IOException {
        UsbSmartCardConnection connection = getConnection();

        packetsIn.add("800200000000010000009000");
        connection.sendAndReceive(Codec.fromHex("000100003100010203040506070809000102030405060708090001020304050607080900010203040506070809000102030405060708"));

        assertSent("6f360000000001000000000100003100010203040506070809000102030405060708090001020304050607080900010203040506070809000102030405060708");
        assertSent("");  // An empty packet must be sent when the last packet ends on a boundary
    }
}
