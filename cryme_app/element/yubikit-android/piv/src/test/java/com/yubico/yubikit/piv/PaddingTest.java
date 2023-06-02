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
package com.yubico.yubikit.piv;

import com.yubico.yubikit.testing.Codec;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;

public class PaddingTest {
    @Test
    public void testPkcs1v1_5() throws NoSuchAlgorithmException {
        byte[] message = "Hello world!".getBytes(StandardCharsets.UTF_8);

        Assert.assertArrayEquals(
                Codec.fromHex("0001ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff003031300d060960864801650304020105000420c0535e4be2b79ffd93291305436bf889314e4a3faec05ecffcbb7df31ad9e51a"),
                Padding.pad(KeyType.RSA1024, message, Signature.getInstance("SHA256withRSA"))
        );

        Assert.assertArrayEquals(
                Codec.fromHex("0001ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff003021300906052b0e03021a05000414d3486ae9136e7856bc42212385ea797094475802"),
                Padding.pad(KeyType.RSA1024, message, Signature.getInstance("SHA1withRSA"))
        );
    }

    @Test
    public void testEcdsa() throws NoSuchAlgorithmException {
        byte[] message = "Hello world!".getBytes(StandardCharsets.UTF_8);

        Assert.assertArrayEquals(
                Codec.fromHex("c0535e4be2b79ffd93291305436bf889314e4a3faec05ecffcbb7df31ad9e51a"),
                Padding.pad(KeyType.ECCP256, message, Signature.getInstance("SHA256withECDSA"))
        );

        Assert.assertArrayEquals(
                Codec.fromHex("00000000000000000000000000000000c0535e4be2b79ffd93291305436bf889314e4a3faec05ecffcbb7df31ad9e51a"),
                Padding.pad(KeyType.ECCP384, message, Signature.getInstance("SHA256withECDSA"))
        );

        Assert.assertArrayEquals(
                Codec.fromHex("000000000000000000000000d3486ae9136e7856bc42212385ea797094475802"),
                Padding.pad(KeyType.ECCP256, message, Signature.getInstance("SHA1withECDSA"))
        );

        Assert.assertArrayEquals(
                Codec.fromHex("f6cde2a0f819314cdde55fc227d8d7dae3d28cc556222a0a8ad66d91ccad4aad"),
                Padding.pad(KeyType.ECCP256, message, Signature.getInstance("SHA512withECDSA"))
        );

        Assert.assertArrayEquals(
                Codec.fromHex("f6cde2a0f819314cdde55fc227d8d7dae3d28cc556222a0a8ad66d91ccad4aad6094f517a2182360c9aacf6a3dc32316"),
                Padding.pad(KeyType.ECCP384, message, Signature.getInstance("SHA512withECDSA"))
        );

        byte[] preHashedMessage = Codec.fromHex("c0535e4be2b79ffd93291305436bf889314e4a3faec05ecffcbb7df31ad9e51a");
        Assert.assertArrayEquals(
                preHashedMessage,
                Padding.pad(KeyType.ECCP256, preHashedMessage, Signature.getInstance("NONEwithECDSA"))
        );
    }
}
