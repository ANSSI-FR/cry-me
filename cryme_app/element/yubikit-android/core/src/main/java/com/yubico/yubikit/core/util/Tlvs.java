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

package com.yubico.yubikit.core.util;

import com.yubico.yubikit.core.application.BadResponseException;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility methods to encode and decode BER-TLV data.
 */
public class Tlvs {

    /**
     * Decodes a sequence of BER-TLV encoded data into a list of Tlvs.
     *
     * @param data sequence of TLV encoded data
     * @return list of Tlvs
     */
    public static List<Tlv> decodeList(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        List<Tlv> tlvs = new ArrayList<>();
        while (buffer.hasRemaining()) {
            Tlv tlv = Tlv.parseFrom(buffer);
            tlvs.add(tlv);
        }
        return tlvs;
    }

    /**
     * Decodes a sequence of BER-TLV encoded data into a mapping of Tag-Value pairs.
     * <p>
     * Iteration order is preserved. If the same tag occurs more than once only the latest will be kept.
     *
     * @param data sequence of TLV encoded data
     * @return map of Tag-Value pairs
     */
    public static Map<Integer, byte[]> decodeMap(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        Map<Integer, byte[]> tlvs = new LinkedHashMap<>();
        while (buffer.hasRemaining()) {
            Tlv tlv = Tlv.parseFrom(buffer);
            tlvs.put(tlv.getTag(), tlv.getValue());
        }
        return tlvs;
    }

    /**
     * Encodes a List of Tlvs into an array of bytes.
     *
     * @param list list of Tlvs
     * @return the data encoded as a sequence of TLV values
     */
    public static byte[] encodeList(Iterable<? extends Tlv> list) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (Tlv tlv : list) {
            byte[] tlvBytes = tlv.getBytes();
            stream.write(tlvBytes, 0, tlvBytes.length);
        }
        return stream.toByteArray();
    }

    /**
     * Encodes a Map of Tag-Value pairs into an array of bytes.
     * NOTE: If order is important use a Map implementation that preserves order, such as LinkedHashMap.
     *
     * @param map the tag-value mappings
     * @return the data encoded as a sequence of TLV values
     */
    public static byte[] encodeMap(Map<Integer, byte[]> map) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (Map.Entry<Integer, byte[]> entry : map.entrySet()) {
            Tlv tlv = new Tlv(entry.getKey(), entry.getValue());
            byte[] tlvBytes = tlv.getBytes();
            stream.write(tlvBytes, 0, tlvBytes.length);
        }
        return stream.toByteArray();
    }

    /**
     * Decode a single TLV encoded object, returning only the value.
     *
     * @param expectedTag the expected tag value of the given TLV data
     * @param tlvData     the TLV data
     * @return the value of the TLV
     * @throws BadResponseException if the TLV tag differs from expectedTag
     */
    public static byte[] unpackValue(int expectedTag, byte[] tlvData) throws BadResponseException {
        Tlv tlv = Tlv.parse(tlvData, 0, tlvData.length);
        if (tlv.getTag() != expectedTag) {
            throw new BadResponseException(String.format("Expected tag: %02x, got %02x", expectedTag, tlv.getTag()));
        }
        return tlv.getValue();
    }
}
