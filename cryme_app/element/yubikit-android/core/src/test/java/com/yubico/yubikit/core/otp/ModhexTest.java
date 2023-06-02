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

import com.yubico.yubikit.testing.Codec;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class ModhexTest {
    @Test
    public void testDecode() {
        Assert.assertArrayEquals(Codec.fromHex("2d344e83"), Modhex.decode("DTEFFUJE"));
        Assert.assertArrayEquals(Codec.fromHex("69b6481c8baba2b60e8f22179b58cd56"), Modhex.decode("hknhfjbrjnlnldnhcujvddbikngjrtgh"));
        Assert.assertArrayEquals(Codec.fromHex("ecde18dbe76fbd0c33330f1c354871db"), Modhex.decode("urtubjtnuihvntcreeeecvbregfjibtn"));
        Assert.assertArrayEquals("test".getBytes(StandardCharsets.UTF_8), Modhex.decode("iFHgiEiF"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOddLengthString() {
        Modhex.decode("theincrediblehulk");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalCharacter() {
        Modhex.decode("theincrediblehulk!");
    }

    @Test
    public void testEncode() {
        Assert.assertEquals("dteffuje", Modhex.encode(Codec.fromHex("2d344e83")));
        Assert.assertEquals("hknhfjbrjnlnldnhcujvddbikngjrtgh", Modhex.encode(Codec.fromHex("69b6481c8baba2b60e8f22179b58cd56")));
        Assert.assertEquals("urtubjtnuihvntcreeeecvbregfjibtn", Modhex.encode(Codec.fromHex("ecde18dbe76fbd0c33330f1c354871db")));
        Assert.assertEquals("ifhgieif", Modhex.encode("test".getBytes(StandardCharsets.UTF_8)));
    }
}
