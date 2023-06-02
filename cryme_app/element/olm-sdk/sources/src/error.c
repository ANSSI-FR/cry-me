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

/* Copyright 2016 OpenMarket Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "olm/error.h"

static const char * ERRORS[] = {
    "SUCCESS",
    "NOT_ENOUGH_RANDOM",
    "OUTPUT_BUFFER_TOO_SMALL",
    "BAD_MESSAGE_VERSION",
    "BAD_MESSAGE_FORMAT",
    "BAD_MESSAGE_MAC",
    "BAD_MESSAGE_KEY_ID",
    "INVALID_BASE64",
    "BAD_ACCOUNT_KEY",
    "UNKNOWN_PICKLE_VERSION",
    "CORRUPTED_PICKLE",
    "BAD_SESSION_KEY",
    "UNKNOWN_MESSAGE_INDEX",
    "BAD_LEGACY_ACCOUNT_PICKLE",
    "BAD_SIGNATURE",
    "OLM_INPUT_BUFFER_TOO_SMALL",
    "OLM_SAS_THEIR_KEY_NOT_SET",
    "OLM_PICKLE_EXTRA_DATA",
    "OLM_BAD_SERVER_ACCREDITATION",
    "OLM_BAD_OUTPUT_BUFFER_SIZE",
    "OLM_BAD_RSA_KEY_GENERATION",
    "OLM_BAD_RANDOM_PRG_GENERATION",
    "OLM_OUT_OF_RANDOMNESS",
    "OLM_MEMORY_ERROR",
    "OLM_INVALID_INPUT",
    "OLM_KEY_DERIVATION_ERROR",
    "OLM_UNKNOWN_ERROR",
    "OLM_PRG_INIT_ERROR",
    "OLM_PRG_RESEED_ERROR",
    "OLM_PRG_NEEDS_RESEED_ERROR",
    "OLM_PRG_SAMPLE_ERROR",
    "OLM_AES_GCM_ERROR",
    "OLM_AES_CBC_DECRYPT_BAD_INPUT_LENGTH_ERROR",
    "OLM_AES_CBC_DECRYPT_PADDING_FAILURE_ERROR",
    "OLM_SIGNATURE_ERROR",
};

const char * _olm_error_to_string(enum OlmErrorCode error)
{
    if (error < (sizeof(ERRORS)/sizeof(ERRORS[0]))) {
        return ERRORS[error];
    } else {
        return "UNKNOWN_ERROR";
    }
}
