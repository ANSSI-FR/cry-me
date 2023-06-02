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

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ChecksumUtilsTest {

    @Test
    public void testCrc1() {
        byte[] data = {0x0, 0x1, 0x2, 0x3, 0x4};
        short crc = ChecksumUtils.calculateCrc(data, data.length);
        Assert.assertEquals((short) 62919, crc);
        byte[] verifyingData = ByteBuffer.allocate(data.length + 2).put(data).order(ByteOrder.LITTLE_ENDIAN).putShort((short) (0xffff - crc)).array();
        Assert.assertTrue(ChecksumUtils.checkCrc(verifyingData, verifyingData.length));
    }

    @Test
    public void testCrc2() {
        byte[] data = {(byte) 0xfe};
        short crc = ChecksumUtils.calculateCrc(data, data.length);
        Assert.assertEquals((short) 4470, crc);
        byte[] verifyingData = ByteBuffer.allocate(data.length + 2).put(data).order(ByteOrder.LITTLE_ENDIAN).putShort((short) (0xffff - crc)).array();
        Assert.assertTrue(ChecksumUtils.checkCrc(verifyingData, verifyingData.length));
    }

    @Test
    public void testCrc3() {
        byte[] data = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, /* uid */
                0x30, 0x75, /* use_ctr */
                0x00, 0x09, /* ts_low */
                0x3d, /* ts_high */
                (byte) 0xfa, /* session_ctr */
                0x60, (byte) 0xea /* rnd */
        };
        short crc = ChecksumUtils.calculateCrc(data, data.length);

        Assert.assertEquals((short) 35339, crc);
        byte[] verifyingData = ByteBuffer.allocate(data.length + 2).put(data).order(ByteOrder.LITTLE_ENDIAN).putShort((short) (0xffff - crc)).array();
        Assert.assertTrue(ChecksumUtils.checkCrc(verifyingData, verifyingData.length));
    }

    @Test
    public void testCrc4() {
        byte[] data = {0x55, (byte) 0xaa, 0x00, (byte) 0xff};
        short crc = ChecksumUtils.calculateCrc(data, data.length);
        Assert.assertEquals((short) 52149, crc);
        byte[] verifyingData = ByteBuffer.allocate(data.length + 2).put(data).order(ByteOrder.LITTLE_ENDIAN).putShort((short) (0xffff - crc)).array();
        Assert.assertTrue(ChecksumUtils.checkCrc(verifyingData, verifyingData.length));
    }
}
