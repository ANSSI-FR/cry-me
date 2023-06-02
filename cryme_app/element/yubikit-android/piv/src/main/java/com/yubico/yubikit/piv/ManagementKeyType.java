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

package com.yubico.yubikit.piv;

/**
 * Supported management key types for use with the PIV YubiKey application.
 */
public enum ManagementKeyType {
    /**
     * 3-DES (the default).
     */
    TDES((byte) 0x03, "DESede", 24, 8),
    /**
     * AES-128.
     */
    AES128((byte) 0x08, "AES", 16, 16),
    /**
     * AES-191.
     */
    AES192((byte) 0x0a, "AES", 24, 16),
    /**
     * AES-256.
     */
    AES256((byte) 0x0c, "AES", 32, 16);

    public final byte value;
    public final String cipherName;
    public final int keyLength;
    public final int challengeLength;

    ManagementKeyType(byte value, String cipherName, int keyLength, int challengeLength) {
        this.value = value;
        this.cipherName = cipherName;
        this.keyLength = keyLength;
        this.challengeLength = challengeLength;
    }

    public static ManagementKeyType fromValue(byte value) {
        for (ManagementKeyType type : ManagementKeyType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Not a valid ManagementKeyType:" + value);
    }
}
