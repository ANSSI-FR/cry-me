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

package com.yubico.yubikit.oath;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * A reference to an OATH Credential stored on a YubiKey.
 */
public class Credential implements Serializable {
    final String deviceId;

    private final byte[] id;
    private final OathType oathType;
    private final int period;
    @Nullable
    private final String issuer;
    private final String accountName;

    private boolean touchRequired = false;

    /*
     * Variation of code types:
     * 0x75 - TOTP full response
     * 0x76 - TOTP truncated response
     * 0x77 - HOTP
     * 0x7c - TOTP requires touch
     */
    private static final byte TYPE_HOTP = 0x77;
    private static final byte TYPE_TOUCH = 0x7c;

    /**
     * Construct a Credential using response data from a LIST call.
     *
     * @param deviceId the Device ID of the YubiKey
     * @param response the parsed response from the YubiKey
     */
    Credential(String deviceId, OathSession.ListResponse response) {
        this.deviceId = deviceId;
        id = response.id;
        oathType = response.oathType;

        CredentialIdUtils.CredentialIdData idData = CredentialIdUtils.parseId(id, oathType);
        issuer = idData.issuer;
        accountName = idData.accountName;
        period = idData.period;
    }

    /**
     * Construct a Credential using response data from a CALCULATE/CALCULATE_ALL call.
     *
     * @param deviceId the Device ID of the YubiKey
     * @param id       the ID of the Credential
     * @param response the parsed response from the YubiKey for the Credential.
     */
    Credential(String deviceId, byte[] id, OathSession.CalculateResponse response) {
        this.deviceId = deviceId;
        this.id = id;
        oathType = response.responseType == TYPE_HOTP ? OathType.HOTP : OathType.TOTP;
        touchRequired = response.responseType == TYPE_TOUCH;

        CredentialIdUtils.CredentialIdData idData = CredentialIdUtils.parseId(id, oathType);
        issuer = idData.issuer;
        accountName = idData.accountName;
        period = idData.period;
    }

    /**
     * Creates an instance of {@link Credential} from CredentialData successfully added to a YubiKey
     *
     * @param deviceId      the Device ID of the YubiKey
     * @param credentialId  the ID of the Credential
     * @param oathType      the OATH type of the credential
     * @param touchRequired whether or not the Credential requires touch
     */
    Credential(String deviceId, byte[] credentialId, OathType oathType, boolean touchRequired) {
        this.deviceId = deviceId;
        this.id = credentialId;
        CredentialIdUtils.CredentialIdData idData = CredentialIdUtils.parseId(credentialId, oathType);
        this.issuer = idData.issuer;
        this.accountName = idData.accountName;
        this.period = idData.period;
        this.oathType = oathType;
        this.touchRequired = touchRequired;
    }

    /**
     * Returns the ID of a Credential which is used to identify it to the YubiKey.
     */
    public byte[] getId() {
        return Arrays.copyOf(id, id.length);
    }

    /**
     * Returns the OATH type (HOTP or TOTP) of the Credential.
     */
    public OathType getOathType() {
        return oathType;
    }

    /**
     * Returns the name of the Credential issuer (e.g. Google, Amazon, Facebook, etc.)
     */
    @Nullable
    public String getIssuer() {
        return issuer;
    }

    /**
     * Returns the name of the account (typically a username or email address).
     */
    public String getAccountName() {
        return accountName;
    }

    /**
     * Returns the validity time period in seconds for a Code generated from this Credential.
     */
    public int getPeriod() {
        return period;
    }

    /**
     * Returns whether or not a user presence check (a physical touch on the sensor of the YubiKey) is required for calculating a Code from this Credential.
     */
    public boolean isTouchRequired() {
        return touchRequired;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Credential that = (Credential) o;
        return deviceId.equals(that.deviceId) &&
                Arrays.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(deviceId);
        result = 31 * result + Arrays.hashCode(id);
        return result;
    }
}
