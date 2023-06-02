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

/**
 * functions for encoding and decoding messages in the Olm protocol.
 *
 * Some of these functions have only C++ bindings, and are declared in
 * message.hh; in time, they should probably be converted to plain C and
 * declared here.
 */

#ifndef OLM_MESSAGE_H_
#define OLM_MESSAGE_H_

#include <stdint.h>
#include <stddef.h>

// Note: exports in this file are only for unit tests.  Nobody else should be
// using this externally
#include "olm/olm_export.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * The length of the buffer needed to hold a group message.
 */
OLM_EXPORT size_t _olm_encode_group_message_length(
    uint32_t chain_index,
    size_t ciphertext_length,
    size_t mac_length,
    size_t signature_length
);

/**
 * Writes the message headers into the output buffer.
 *
 * version:            version number of the olm protocol
 * message_index:      message index
 * ciphertext_length:  length of the ciphertext
 * output:             where to write the output. Should be at least
 *                     olm_encode_group_message_length() bytes long.
 * ciphertext_ptr:     returns the address that the ciphertext
 *                     should be written to, followed by the MAC and the
 *                     signature.
 *
 * Returns the size of the message, up to the MAC.
 */
OLM_EXPORT size_t _olm_encode_group_message(
    uint8_t version,
    uint32_t message_index,
    size_t ciphertext_length,
    uint8_t *output,
    uint8_t **ciphertext_ptr
);


struct _OlmDecodeGroupMessageResults {
    uint8_t version;
    uint32_t message_index;
    int has_message_index;
    const uint8_t *ciphertext;
    size_t ciphertext_length;
};


/**
 * Reads the message headers from the input buffer.
 */
OLM_EXPORT void _olm_decode_group_message(
    const uint8_t *input, size_t input_length,
    size_t mac_length, size_t signature_length,

    /* output structure: updated with results */
    struct _OlmDecodeGroupMessageResults *results
);



#ifdef __cplusplus
} // extern "C"
#endif

#endif /* OLM_MESSAGE_H_ */
