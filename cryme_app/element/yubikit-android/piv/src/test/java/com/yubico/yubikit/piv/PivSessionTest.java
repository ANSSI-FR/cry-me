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

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class PivSessionTest {
    @Test
    public void testDecodeP256Key() throws InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] encoded = Codec.fromHex("046B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C2964FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5");
        ECPublicKey key = (ECPublicKey) PivSession.publicEccKey(KeyType.ECCP256, encoded);
        assertThat(key.getAlgorithm(), equalTo("EC"));
        assertThat(key.getParams().getCurve().getField().getFieldSize(), equalTo(256));
    }

    @Test
    public void testDecodeP384Key() throws InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] encoded = Codec.fromHex("0408D999057BA3D2D969260045C55B97F089025959A6F434D651D207D19FB96E9E4FE0E86EBE0E64F85B96A9C75295DF618E80F1FA5B1B3CEDB7BFE8DFFD6DBA74B275D875BC6CC43E904E505F256AB4255FFD43E94D39E22D61501E700A940E80");
        ECPublicKey key = (ECPublicKey) PivSession.publicEccKey(KeyType.ECCP384, encoded);
        assertThat(key.getAlgorithm(), equalTo("EC"));
        assertThat(key.getParams().getCurve().getField().getFieldSize(), equalTo(384));
    }

    @Test
    public void testDecodeRsa1024() throws InvalidKeySpecException, NoSuchAlgorithmException {
        BigInteger modulus = new BigInteger(Codec.fromHex("00C061DB5C051CE961F42898068E084D81EAB3245A6884CF8F8B379587E81C87A96CD4DC83FED14DB5EB6AC60B173797F6692B93AC285CDBD4F91F4968E65CDA579F82D2071ADFFE85F5FF424E8D9A33BFAC1B56C0975BC5B15710F475D45923880575F15B326314251C4DA5C9640EF240F3EF49E61398F700449F16C6F06D532D"));
        BigInteger exponent = BigInteger.valueOf(65537);
        RSAPublicKey key = (RSAPublicKey) PivSession.publicRsaKey(modulus, exponent);
        assertThat(key.getAlgorithm(), equalTo("RSA"));
        assertThat(key.getModulus(), equalTo(modulus));
        assertThat(key.getPublicExponent(), equalTo(exponent));
        assertThat(key.getModulus().bitLength(), equalTo(1024));
    }

    @Test
    public void testDecodeRsa2048() throws InvalidKeySpecException, NoSuchAlgorithmException {
        BigInteger modulus = new BigInteger(Codec.fromHex("00C6FC5B4D5C28B9CDD9047C5481B1F6A6A66683E3B9566E91CBBC9E852EAD96796C914A92315C1B408045270D3C672FC7DA97F2258DBDE0681BD4E5D1112EEBB75AACDC712E62FCD4391513AE867C0E3C70B77032FBBEF774AADE544C6D76B0D296FEC3A5E2BF8ED7BFD3A0F9E48CA60F4CD36162DC3AEE6A0CC47E6BA92704E88E6A110622B3E9FC0C7CAA083A9D93BEB2902F16D06261751E5FA5B8F65E56A6C37B4EA27704AC2FCC7309211022ECFF04BF874A33ACB905699A40A617AF95EDE3308B3B438BFA888B5E82E3CFA7D403E2D32A7B554736ED947FC245943B656B1893032B82F82B6CAFB65BC491AFC645CD676B776F61A0B99FCB990606DA43E5"));
        BigInteger exponent = BigInteger.valueOf(65537);
        RSAPublicKey key = (RSAPublicKey) PivSession.publicRsaKey(modulus, exponent);
        assertThat(key.getAlgorithm(), equalTo("RSA"));
        assertThat(key.getModulus(), equalTo(modulus));
        assertThat(key.getPublicExponent(), equalTo(exponent));
        assertThat(key.getModulus().bitLength(), equalTo(2048));
    }

    @Test
    public void testParsePkcs8RsaKeyValues() throws UnsupportedEncodingException {
        PivSession.parsePkcs8RsaKeyValues(Codec.fromBase64("MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBALWeZ0E5O2l/iHfc" +
                "k9mokf1iWH2eZDWQoJoQKUOAeVoKUecNp250J5tL3EHONqWoF6VLO+B+6jTET4Iz" +
                "97BeUj7gOJHmEw+nqFfguTVmNeeiZ711TNYNpF7kwW7yWghWG+Q7iQEoMXfY3x4B" +
                "L33H2gKRWtMHK66GJViL1l9s3qDXAgMBAAECgYBO753pFzrfS3LAxbns6/snqcrU" +
                "LjdXoJhs3YFRuVEE9V9LkP+oXguoz3vXjgzqSvib+ur3U7HvZTM5X+TTXutXdQ5C" +
                "yORLLtXEZcyCKQI9ihH5fSNJRWRbJ3xe+xi5NANRkRDkro7tm4a5ZD4PYvO4r29y" +
                "VB5PXlMkOTLoxNSwwQJBAN5lW93Agi9Ge5B2+B2EnKSlUvj0+jJBkHYAFTiHyTZV" +
                "Ej6baeHBvJklhVczpWvTXb6Nr8cjAKVshFbdQoBwHmkCQQDRD7djZGIWH1Lz0rkL" +
                "01nDj4z4QYMgUs3AQhnrXPBjEgNzphtJ2u7QrCSOBQQHlmAPBDJ/MTxFJMzDIJGD" +
                "A10/AkATJjEZz/ilr3D2SHgmuoNuXdneG+HrL+ALeQhavL5jkkGm6GTejnr5yNRJ" +
                "ZOYKecGppbOL9wSYOdbPT+/o9T55AkATXCY6cRBYRhxTcf8q5i6Y2pFOaBqxgpmF" +
                "JVnrHtcwBXoGWqqKQ1j8QAS+lh5SaY2JtnTKrI+NQ6Qmqbxv6n7XAkBkhLO7pplI" +
                "nVh2WjqXOV4ZAoOAAJlfpG5+z6mWzCZ9+286OJQLr6OVVQMcYExUO9yVocZQX+4X" +
                "qEIF0qAB7m31"));
    }
}
