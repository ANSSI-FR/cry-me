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
package com.yubico.yubikit.yubiotp;

import java.util.Arrays;

/**
 * Configures the YubiKey to output a Static Ticket.
 * NOTE: {@link StaticPasswordSlotConfiguration} is a better choice in most cases!
 * <p>
 * A Static Ticket behaves like a Yubico OTP, but with all changing state removed.
 */
public class StaticTicketSlotConfiguration extends KeyboardSlotConfiguration<StaticTicketSlotConfiguration> {
    /**
     * Creates a Static Ticket configuration with default settings.
     *
     * @param fixed data to use for the fixed portion of the ticket
     * @param uid   uid value (corresponds to a Yubico OTP private ID)
     * @param key   AES key used to generate the "dynamic" part of the ticket
     */
    public StaticTicketSlotConfiguration(byte[] fixed, byte[] uid, byte[] key) {
        if (fixed.length > FIXED_SIZE) {
            throw new IllegalArgumentException("Public ID must be <= 16 bytes");
        }

        this.fixed = Arrays.copyOf(fixed, fixed.length);
        System.arraycopy(uid, 0, this.uid, 0, uid.length);
        System.arraycopy(key, 0, this.key, 0, key.length);

        updateFlags(CFGFLAG_STATIC_TICKET, true);
    }

    @Override
    protected StaticTicketSlotConfiguration getThis() {
        return this;
    }

    /**
     * Truncate the OTP-portion of the ticket to 16 characters.
     *
     * @param shortTicket if true, the OTP is truncated to 16 characters (default: false)
     * @return the configuration for chaining
     */
    public StaticTicketSlotConfiguration shortTicket(boolean shortTicket) {
        return updateFlags(CFGFLAG_SHORT_TICKET, shortTicket);
    }

    /**
     * Modifier flags to alter the output string to conform to password validation rules.
     * <p>
     * NOTE: special=true implies digits=true, and cannot be used without it.
     *
     * @param upperCase if true the two first letters of the output string are upper-cased (default: false)
     * @param digit     if true the first eight characters of the modhex alphabet are replaced with the numbers 0 to 7 (default: false)
     * @param special   if true a ! is sent as the very first character, and digits is implied (default: false)
     * @return the configuration for chaining
     */
    public StaticTicketSlotConfiguration strongPassword(boolean upperCase, boolean digit, boolean special) {
        updateFlags(CFGFLAG_STRONG_PW1, upperCase);
        updateFlags(CFGFLAG_STRONG_PW2, digit || special);
        return updateFlags(CFGFLAG_SEND_REF, special);
    }

    /**
     * Enabled Manual Update of the static ticket.
     * NOTE: This feature is ONLY supported on YubiKey 2.x
     * <p>
     * Manual update is triggered by the user by holding the sensor pressed for 8-15 seconds.
     * This will generate a new random static ticket to be used, until manual update is again invoked.
     *
     * @param manualUpdate if true, enable user-initiated manual update (default: false)
     * @return the configuration for chaining
     */
    public StaticTicketSlotConfiguration manualUpdate(boolean manualUpdate) {
        return updateFlags(CFGFLAG_MAN_UPDATE, manualUpdate);
    }
}
