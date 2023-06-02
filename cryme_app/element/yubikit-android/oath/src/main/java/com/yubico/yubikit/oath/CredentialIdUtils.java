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

package com.yubico.yubikit.oath;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Internal utility class for dealing with parameters stored in YKOATH credential names.
 */
class CredentialIdUtils {
    private static final Pattern TOTP_ID_PATTERN = Pattern.compile("^((\\d+)/)?(([^:]+):)?(.+)$");
    private static final int DEFAULT_PERIOD = CredentialData.DEFAULT_TOTP_PERIOD;

    /**
     * Format the YKOATH Credential ID to use for a credential given its parameters.
     *
     * @param issuer   an optional issuer name for the credential
     * @param name     the name of the credential
     * @param oathType the type of the credential
     * @param period   the time period of a TOTP credential (ignored for HOTP)
     * @return A bytestring to use with YKOATH.
     */
    static byte[] formatId(@Nullable String issuer, String name, OathType oathType, int period) {
        String longName = "";
        if (oathType == OathType.TOTP && period != DEFAULT_PERIOD) {
            //TODO: Add period even if default if TOTP and remainder of ID starts with \d+/
            longName += String.format(Locale.ROOT, "%d/", period);
        }

        if (issuer != null) {
            longName += String.format(Locale.ROOT, "%s:", issuer);
        }
        longName += name;
        return longName.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Parse credential parameters from a YKOATH credential name.
     *
     * @param credentialId as retrieved from a YubiKey.
     * @param oathType     the type of the credential
     * @return parsed data stored in the credential ID.
     */
    static CredentialIdData parseId(byte[] credentialId, OathType oathType) {
        String data = new String(credentialId, StandardCharsets.UTF_8);

        if (oathType == OathType.TOTP) {
            Matcher m = TOTP_ID_PATTERN.matcher(data);
            if (m.matches()) {
                String periodString = m.group(2);
                return new CredentialIdData(
                        m.group(4),
                        m.group(5),
                        periodString == null ? DEFAULT_PERIOD : Integer.parseInt(periodString)
                );
            } else {  //Invalid id, use it directly as name.
                return new CredentialIdData(null, data, DEFAULT_PERIOD);
            }
        } else {
            String issuer;
            String name;
            if (data.contains(":")) {
                String[] parts = data.split(":", 2);
                issuer = parts[0];
                name = parts[1];
            } else {
                issuer = null;
                name = data;
            }
            return new CredentialIdData(issuer, name, 0);
        }
    }

    static class CredentialIdData {
        @Nullable
        final String issuer;
        final String accountName;
        final int period;

        CredentialIdData(@Nullable String issuer, String accountName, int period) {
            this.issuer = issuer;
            this.accountName = accountName;
            this.period = period;
        }
    }
}
