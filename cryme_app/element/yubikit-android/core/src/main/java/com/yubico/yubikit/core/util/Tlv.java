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

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;

import javax.annotation.Nullable;

/**
 * Tag, length, Value structure that helps to parse APDU response data.
 * This class handles BER-TLV encoded data with determinate length.
 */
public class Tlv {
    private final int tag;
    private final int length;
    private final byte[] bytes;
    private final int offset;

    /**
     * Creates a new Tlv given a tag and a value.
     */
    public Tlv(int tag, @Nullable byte[] value) {
        this.tag = tag;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        byte[] tagBytes = BigInteger.valueOf(tag).toByteArray();
        int stripLeading = tagBytes[0] == 0 ? 1 : 0;
        stream.write(tagBytes, stripLeading, tagBytes.length - stripLeading);

        length = value == null ? 0 : value.length;
        if (length < 0x80) {
            stream.write(length);
        } else {
            byte[] lnBytes = BigInteger.valueOf(length).toByteArray();
            stripLeading = lnBytes[0] == 0 ? 1 : 0;
            stream.write(0x80 | lnBytes.length - stripLeading);
            stream.write(lnBytes, stripLeading, lnBytes.length - stripLeading);
        }

        offset = stream.size();
        if (value != null) {
            stream.write(value, 0, length);
        }
        bytes = stream.toByteArray();
    }

    /**
     * Returns the tag.
     */
    public int getTag() {
        return tag;
    }

    /**
     * returns the value.
     */
    public byte[] getValue() {
        return Arrays.copyOfRange(bytes, offset, offset + length);
    }

    /**
     * Returns the length of the value.
     */
    public int getLength() {
        return length;
    }

    /**
     * Returns the Tlv as a BER-TLV encoded byte array.
     */
    public byte[] getBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "Tlv(0x%x, %d, %s)", tag, length, StringUtils.bytesToHex(getValue()));
    }

    /**
     * Parse a Tlv from a BER-TLV encoded byte array.
     *
     * @param data   a byte array containing the TLV encoded data.
     * @param offset the offset in data where the TLV data begins.
     * @param length the length of the TLV encoded data.
     * @return The parsed Tlv
     */
    public static Tlv parse(byte[] data, int offset, int length) {
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, length);
        Tlv tlv = parseFrom(buffer);
        if (buffer.hasRemaining()) {
            throw new IllegalArgumentException("Extra data remaining");
        }
        return tlv;
    }

    /**
     * Parse a Tlv from a BER-TLV encoded byte array.
     *
     * @param data a byte array containing the TLV encoded data (and nothing more).
     * @return The parsed Tlv
     */
    public static Tlv parse(byte[] data) {
        return parse(data, 0, data.length);
    }

    static Tlv parseFrom(ByteBuffer buffer) {
        int tag = buffer.get() & 0xFF;
        if ((tag & 0x1F) == 0x1F) { // Long form tag
            tag = (tag << 8) | (buffer.get() & 0xFF);
            while ((tag & 0x80) == 0x80) {
                tag = (tag << 8) | (buffer.get() & 0xFF);
            }
        }

        int length = buffer.get() & 0xFF;
        if (length == 0x80) {
            throw new IllegalArgumentException("Indefinite length not supported");
        } else if (length > 0x80) {
            int lengthLn = length - 0x80;
            length = 0;
            for (int i = 0; i < lengthLn; i++) {
                length = (length << 8) | (buffer.get() & 0xff);
            }
        }

        byte[] value = new byte[length];
        buffer.get(value);
        return new Tlv(tag, value);
    }
}
