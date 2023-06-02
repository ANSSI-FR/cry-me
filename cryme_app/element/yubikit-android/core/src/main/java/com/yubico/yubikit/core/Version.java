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

package com.yubico.yubikit.core;


import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A 3-part version number, used by the YubiKey firmware and its various applications.
 */
public final class Version implements Comparable<Version> {
    private static final Pattern VERSION_STRING_PATTERN = Pattern.compile("\\b(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\b");

    public final byte major;
    public final byte minor;
    public final byte micro;

    private static byte checkRange(int value) {
        if (value < 0 || value > Byte.MAX_VALUE) {
            throw new IllegalArgumentException("Version component out of supported range (0-127)");
        }
        return (byte) value;
    }

    /**
     * Constructor using int's for convenience.
     * <p>
     * Each version component will be checked to ensure it falls within an acceptable range.
     */
    public Version(int major, int minor, int micro) {
        this(checkRange(major), checkRange(minor), checkRange(micro));
    }

    /**
     * Constructs a new Version object.
     */
    public Version(byte major, byte minor, byte micro) {
        this.major = major;
        this.minor = minor;
        this.micro = micro;
    }

    /**
     * Returns the version components as a byte array of size 3.
     */
    public byte[] getBytes() {
        return new byte[]{major, minor, micro};
    }

    private int compareToVersion(int major, int minor, int micro) {
        return Integer.compare(this.major << 16 | this.minor << 8 | this.micro, major << 16 | minor << 8 | micro);
    }

    @Override
    public int compareTo(Version other) {
        return compareToVersion(other.major, other.minor, other.micro);
    }

    /**
     * Returns whether or not the Version is less than a given version.
     */
    public boolean isLessThan(int major, int minor, int micro) {
        return compareToVersion(major, minor, micro) < 0;
    }

    /**
     * Returns whether or not the Version is greater than or equal to a given version.
     */
    public boolean isAtLeast(int major, int minor, int micro) {
        return compareToVersion(major, minor, micro) >= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version version = (Version) o;
        return major == version.major &&
                minor == version.minor &&
                micro == version.micro;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, micro);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%d.%d.%d", 0xff & major, 0xff & minor, 0xff & micro);
    }

    /**
     * Parses a Version from a byte array by taking the first three bytes.
     * <p>
     * Additional bytes in the array are ignored.
     */
    public static Version fromBytes(byte[] bytes) {
        if (bytes.length < 3) {
            throw new IllegalArgumentException("Version byte array must contain 3 bytes.");
        }

        return new Version(bytes[0], bytes[1], bytes[2]);
    }

    /**
     * Parses a Version from a String (eg. "Firmware version 5.2.1")
     *
     * @param versionString string that contains a 3-part version, separated by dots.
     */
    public static Version parse(String versionString) {
        Matcher match = VERSION_STRING_PATTERN.matcher(versionString);
        if (match.find()) {
            return new Version(
                    Byte.parseByte(match.group(1)),
                    Byte.parseByte(match.group(2)),
                    Byte.parseByte(match.group(3))
            );
        }
        throw new IllegalArgumentException("Invalid version string");
    }
}
