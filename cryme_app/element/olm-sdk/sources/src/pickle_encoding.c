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

#include <stdio.h>
#include "olm/pickle_encoding.h"

#include "olm/base64.h"
#include "olm/default_cipher.h"
#include "olm/olm.h"

static const struct _default_cipher PICKLE_CIPHER =
    DEFAULT_CIPHER_INIT("Pickle");

size_t _olm_enc_output_length(
    size_t raw_length
) {
    const struct _default_cipher *cipher = &PICKLE_CIPHER;
    size_t length = cipher->ops->encrypt_ciphertext_length(cipher, raw_length);
    length += cipher->ops->mac_length(cipher);
    return _olm_encode_base64_length(length);
}

uint8_t * _olm_enc_output_pos(
    uint8_t * output,
    size_t raw_length
) {
    const struct _default_cipher *cipher = &PICKLE_CIPHER;
    size_t length = cipher->ops->encrypt_ciphertext_length(cipher, raw_length);
    length += cipher->ops->mac_length(cipher);
    return output + _olm_encode_base64_length(length) - length;
}

size_t _olm_enc_output(
    uint8_t const * key, size_t key_length,
    uint8_t * output, size_t raw_length
) {
    const struct _default_cipher *cipher = &PICKLE_CIPHER;
    size_t ciphertext_length = cipher->ops->encrypt_ciphertext_length(
        cipher, raw_length
    );
    size_t length = ciphertext_length + cipher->ops->mac_length(cipher);
    size_t base64_length = _olm_encode_base64_length(length);
    uint8_t * raw_output = output + base64_length - length;
    cipher->ops->encrypt(
        cipher,
        key, key_length,
        raw_output, raw_length,
        raw_output, ciphertext_length,
        raw_output, length
    );
    _olm_encode_base64(raw_output, length, output);
    return base64_length;
}


size_t _olm_enc_input(uint8_t const * key, size_t key_length,
                      uint8_t * input, size_t b64_length,
                      enum OlmErrorCode * last_error
) {
    size_t enc_length = _olm_decode_base64_length(b64_length);
    if (enc_length == (size_t)-1) {
        if (last_error) {
            *last_error = OLM_INVALID_BASE64;
        }
        return (size_t)-1;
    }
    _olm_decode_base64(input, b64_length, input);
    const struct _default_cipher *cipher = &PICKLE_CIPHER;
    size_t raw_length = enc_length - cipher->ops->mac_length(cipher);
    size_t result = cipher->ops->decrypt(
        cipher,
        key, key_length,
        input, enc_length,
        input, raw_length,
        input, raw_length
    );
    if (result == (size_t)-1 && last_error) {
        *last_error = OLM_BAD_ACCOUNT_KEY;
    }
    return result;
}
