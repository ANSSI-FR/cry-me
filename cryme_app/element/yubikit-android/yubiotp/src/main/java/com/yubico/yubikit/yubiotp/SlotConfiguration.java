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

import com.yubico.yubikit.core.Version;
import com.yubico.yubikit.core.application.Feature;

import javax.annotation.Nullable;

public interface SlotConfiguration {
    // Constants in this file come from https://github.com/Yubico/yubikey-personalization/blob/master/ykcore/ykdef.h

    // Yubikey 1 and above
    Flag TKTFLAG_TAB_FIRST = new Flag(FlagType.TKT, "TAB_FIRST", 0x01, 1, 0); // Send TAB before first part
    Flag TKTFLAG_APPEND_TAB1 = new Flag(FlagType.TKT, "APPEND_TAB1", 0x02, 1, 0); // Send TAB after first part
    Flag TKTFLAG_APPEND_TAB2 = new Flag(FlagType.TKT, "APPEND_TAB2", 0x04, 1, 0); // Send TAB after second part
    Flag TKTFLAG_APPEND_DELAY1 = new Flag(FlagType.TKT, "APPEND_DELAY1", 0x08, 1, 0); // Add 0.5s delay after first part
    Flag TKTFLAG_APPEND_DELAY2 = new Flag(FlagType.TKT, "APPEND_DELAY2", 0x10, 1, 0); // Add 0.5s delay after second part
    Flag TKTFLAG_APPEND_CR = new Flag(FlagType.TKT, "APPEND_CR", 0x20, 1, 0); // Append CR as final character

    // Yubikey 2 and above
    Flag TKTFLAG_PROTECT_CFG2 = new Flag(FlagType.TKT, "PROTECT_CFG2", 0x80, 2, 0); // Block update of config 2 unless config 2 is configured and has this bit set

    // Configuration flags

    // Yubikey 1 and above
    Flag CFGFLAG_SEND_REF = new Flag(FlagType.CFG, "SEND_REF", 0x01, 1, 0); // Send reference string (0..F) before data
    Flag CFGFLAG_PACING_10MS = new Flag(FlagType.CFG, "PACING_10MS", 0x04, 1, 0); // Add 10ms intra-key pacing
    Flag CFGFLAG_PACING_20MS = new Flag(FlagType.CFG, "PACING_20MS", 0x08, 1, 0); // Add 20ms intra-key pacing
    Flag CFGFLAG_STATIC_TICKET = new Flag(FlagType.CFG, "STATIC_TICKET", 0x20, 1, 0); // Static ticket generation

    // Yubikey 1 only
    Flag CFGFLAG_TICKET_FIRST = new Flag(FlagType.CFG, "TICKET_FIRST", 0x02, 1, 0); // Send ticket first (default is fixed part)
    Flag CFGFLAG_ALLOW_HIDTRIG = new Flag(FlagType.CFG, "ALLOW_HIDTRIG", 0x10, 1, 0); // Allow trigger through HID/keyboard

    // Yubikey 2 and above
    Flag CFGFLAG_SHORT_TICKET = new Flag(FlagType.CFG, "SHORT_TICKET", 0x02, 2, 0); // Send truncated ticket (half length)
    Flag CFGFLAG_STRONG_PW1 = new Flag(FlagType.CFG, "STRONG_PW1", 0x10, 2, 0); // Strong password policy flag #1 (mixed case)
    Flag CFGFLAG_STRONG_PW2 = new Flag(FlagType.CFG, "STRONG_PW2", 0x40, 2, 0); // Strong password policy flag #2 (subtitute 0..7 to digits)
    Flag CFGFLAG_MAN_UPDATE = new Flag(FlagType.CFG, "MAN_UPDATE", 0x80, 2, 0); // Allow manual (local) update of static OTP

    // Yubikey 2.1 and above
    Flag TKTFLAG_OATH_HOTP = new Flag(FlagType.TKT, "OATH_HOTP", 0x40, 2, 1); //  OATH HOTP mode
    Flag CFGFLAG_OATH_HOTP8 = new Flag(FlagType.CFG, "OATH_HOTP8", 0x02, 2, 1); //  Generate 8 digits HOTP rather than 6 digits
    Flag CFGFLAG_OATH_FIXED_MODHEX1 = new Flag(FlagType.CFG, "OATH_FIXED_MODHEX1", 0x10, 2, 1); //  First byte in fixed part sent as modhex
    Flag CFGFLAG_OATH_FIXED_MODHEX2 = new Flag(FlagType.CFG, "OATH_FIXED_MODHEX2", 0x40, 2, 1); //  First two bytes in fixed part sent as modhex (if paired with OATH_FIXED_MODHEX1, all of fixed part is sent as modhex)

    // Yubikey 2.2 and above
    Flag TKTFLAG_CHAL_RESP = new Flag(FlagType.TKT, "CHAL_RESP", 0x40, 2, 2); // Challenge-response enabled (both must be set)
    Flag CFGFLAG_CHAL_YUBICO = new Flag(FlagType.CFG, "CHAL_YUBICO", 0x20, 2, 2); // Challenge-response enabled - Yubico OTP mode
    Flag CFGFLAG_CHAL_HMAC = new Flag(FlagType.CFG, "CHAL_HMAC", 0x22, 2, 2); // Challenge-response enabled - HMAC-SHA1
    Flag CFGFLAG_HMAC_LT64 = new Flag(FlagType.CFG, "CHAL_LT64", 0x04, 2, 2); // Set when HMAC message is less than 64 bytes
    Flag CFGFLAG_CHAL_BTN_TRIG = new Flag(FlagType.CFG, "CHAL_BTN_TRIG", 0x08, 2, 2); // Challenge-response operation requires button press

    Flag EXTFLAG_SERIAL_BTN_VISIBLE = new Flag(FlagType.EXT, "SERIAL_BTN_VISIBLE", 0x01, 2, 2); // Serial number visible at startup (button press)
    Flag EXTFLAG_SERIAL_USB_VISIBLE = new Flag(FlagType.EXT, "SERIAL_USB_VISIBLE", 0x02, 2, 2); // Serial number visible in USB iSerial field
    Flag EXTFLAG_SERIAL_API_VISIBLE = new NonFailingFlag(FlagType.EXT, "SERIAL_API_VISIBLE", 0x04, 2, 2); // Serial number visible via API call

    // YubiKey 2.3 and above
    Flag EXTFLAG_USE_NUMERIC_KEYPAD = new Flag(FlagType.EXT, "USE_NUMERIC_KEYPAD", 0x08, 2, 3); // Use numeric keypad for digits
    Flag EXTFLAG_FAST_TRIG = new NonFailingFlag(FlagType.EXT, "FAST_TRIG", 0x10, 2, 3); // Use fast trig if only cfg1 set
    Flag EXTFLAG_ALLOW_UPDATE = new NonFailingFlag(FlagType.EXT, "ALLOW_UPDATE", 0x20, 2, 3); // Allow update of existing configuration (selected flags + access code)
    Flag EXTFLAG_DORMANT = new Flag(FlagType.EXT, "DORMANT", 0x40, 2, 3); // Dormant configuration (can be woken up and flag removed = requires update flag)

    // YubiKey 2.4 and 3.1 and above (not 3.0)
    Flag EXTFLAG_LED_INV = new Flag(FlagType.EXT, "LED_INV", 0x80, 2, 4); // LED idle state is off rather than on

    /**
     * Checks the configuration against a YubiKey firmware version to see if it is supported
     *
     * @param version the firmware version to check against
     * @return true if the given YubiKey version supports this configuration
     */
    boolean isSupportedBy(Version version);

    byte[] getConfig(@Nullable byte[] accCode);

    enum FlagType {
        TKT, CFG, EXT
    }

    /**
     * A flag used for slot configuration.
     */
    class Flag extends Feature.Versioned<YubiOtpSession> {
        public final FlagType type;
        public final byte bit;

        Flag(FlagType type, String name, int bit, int major, int minor) {
            super(type.name() + "FLAG_" + name, major, minor, 0);
            this.type = type;
            this.bit = (byte) bit;
        }
    }

    /**
     * Flag which should not cause a SlotConfiguration to fail, even if required version is not met.
     */
    class NonFailingFlag extends Flag {
        NonFailingFlag(FlagType type, String name, int bit, int major, int minor) {
            super(type, name, bit, major, minor);
        }
    }
}