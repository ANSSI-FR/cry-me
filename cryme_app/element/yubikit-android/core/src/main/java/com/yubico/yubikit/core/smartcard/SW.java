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
package com.yubico.yubikit.core.smartcard;

/**
 * Contains constants for APDU status codes (SW1, SW2).
 */
public final class SW {
    public static final short NO_INPUT_DATA = 0x6285;
    public static final short VERIFY_FAIL_NO_RETRY = 0x63C0;
    public static final short MEMORY_ERROR = 0x6581;
    public static final short WRONG_LENGTH = 0x6700;
    public static final short SECURITY_CONDITION_NOT_SATISFIED = 0x6982;
    public static final short AUTH_METHOD_BLOCKED = 0x6983;
    public static final short DATA_INVALID = 0x6984;
    public static final short CONDITIONS_NOT_SATISFIED = 0x6985;
    public static final short COMMAND_NOT_ALLOWED = 0x6986;
    public static final short INCORRECT_PARAMETERS = 0x6A80;
    public static final short FILE_NOT_FOUND = 0x6A82;
    public static final short NO_SPACE = 0x6A84;
    public static final short INVALID_INSTRUCTION = 0x6D00;
    public static final short COMMAND_ABORTED = 0x6F00;
    public static final short OK = (short) 0x9000;

    private SW() {
        throw new IllegalStateException();
    }
}
