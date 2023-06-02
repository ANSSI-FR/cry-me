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

/* Copyright 2015-2016 OpenMarket Ltd
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
#ifndef OLM_ERROR_H_
#define OLM_ERROR_H_

#include "olm/olm_export.h"

#ifdef __cplusplus
extern "C" {
#endif

enum OlmErrorCode {
    OLM_SUCCESS = 0, /*!< There wasn't an error */
    OLM_NOT_ENOUGH_RANDOM = 1,  /*!< Not enough entropy was supplied */
    OLM_OUTPUT_BUFFER_TOO_SMALL = 2, /*!< Supplied output buffer is too small */
    OLM_BAD_MESSAGE_VERSION = 3,  /*!< The message version is unsupported */
    OLM_BAD_MESSAGE_FORMAT = 4, /*!< The message couldn't be decoded */
    OLM_BAD_MESSAGE_MAC = 5, /*!< The message couldn't be decrypted */
    OLM_BAD_MESSAGE_KEY_ID = 6, /*!< The message references an unknown key id */
    OLM_INVALID_BASE64 = 7, /*!< The input base64 was invalid */
    OLM_BAD_ACCOUNT_KEY = 8, /*!< The supplied account key is invalid */
    OLM_UNKNOWN_PICKLE_VERSION = 9, /*!< The pickled object is too new */
    OLM_CORRUPTED_PICKLE = 10, /*!< The pickled object couldn't be decoded */

    OLM_BAD_SESSION_KEY = 11,  /*!< Attempt to initialise an inbound group
                                 session from an invalid session key */
    OLM_UNKNOWN_MESSAGE_INDEX = 12,  /*!< Attempt to decode a message whose
                                      * index is earlier than our earliest
                                      * known session key.
                                      */

    /**
     * Attempt to unpickle an account which uses pickle version 1 (which did
     * not save enough space for the weisig25519 key; the key should be considered
     * compromised. We don't let the user reload the account.
     */
    OLM_BAD_LEGACY_ACCOUNT_PICKLE = 13,

    /**
     * Received message had a bad signature
     */
    OLM_BAD_SIGNATURE = 14,

    OLM_INPUT_BUFFER_TOO_SMALL = 15,

    /**
     * SAS doesn't have their key set.
     */
    OLM_SAS_THEIR_KEY_NOT_SET = 16,

    /**
     * The pickled object was successfully decoded, but the unpickling still failed
     * because it had some extraneous junk data at the end.
     */
    OLM_PICKLE_EXTRA_DATA = 17,

    OLM_BAD_SERVER_ACCREDITATION = 18,

    OLM_BAD_OUTPUT_BUFFER_SIZE = 19,

    OLM_BAD_RSA_KEY_GENERATION = 20,

    OLM_BAD_RANDOM_PRG_GENERATION = 21,

    OLM_OUT_OF_RANDOMNESS = 22,

    OLM_MEMORY_ERROR = 23,

    OLM_INVALID_INPUT = 24,

    OLM_KEY_DERIVATION_ERROR = 25,

    OLM_UNKNOWN_ERROR = 26,

    OLM_PRG_INIT_ERROR = 27,

    OLM_PRG_RESEED_ERROR = 28,

    OLM_PRG_NEEDS_RESEED_ERROR = 29,

    OLM_PRG_SAMPLE_ERROR = 30,

    OLM_AES_GCM_ERROR = 31,

    OLM_AES_CBC_DECRYPT_BAD_INPUT_LENGTH_ERROR = 32,

    OLM_AES_CBC_DECRYPT_PADDING_FAILURE_ERROR = 33,

    OLM_SIGNATURE_ERROR = 34,

    /* remember to update the list of string constants in error.c when updating
     * this list. */
};

/** get a string representation of the given error code. */
OLM_EXPORT const char * _olm_error_to_string(enum OlmErrorCode error);

#ifdef __cplusplus
} // extern "C"
#endif

#endif /* OLM_ERROR_H_ */
