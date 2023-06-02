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

/* Copyright 2015 OpenMarket Ltd
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

#ifndef DEFAULT_CIPHER_H_
#define DEFAULT_CIPHER_H_

#include <stdint.h>
#include <stdlib.h>

// Note: exports in this file are only for unit tests.  Nobody else should be
// using this externally
#include "olm/olm_export.h"

#ifdef __cplusplus
extern "C" {
#endif

struct _default_cipher;

struct _default_cipher_ops {
    /**
     * Returns the length of the message authentication code that will be
     * appended to the output.
     */
    size_t (*mac_length)(const struct _default_cipher *cipher);

    /**
     * Returns the length of cipher-text for a given length of plain-text.
     */
    size_t (*encrypt_ciphertext_length)(
        const struct _default_cipher *cipher,
        size_t plaintext_length
    );

    /*
     * Encrypts the plain-text into the output buffer and authenticates the
     * contents of the output buffer covering both cipher-text and any other
     * associated data in the output buffer.
     *
     *  |---------------------------------------output_length-->|
     *  output  |--ciphertext_length-->|       |---mac_length-->|
     *          ciphertext
     *
     * The plain-text pointers and cipher-text pointers may be the same.
     *
     * Returns size_t(-1) if the length of the cipher-text or the output
     * buffer is too small. Otherwise returns the length of the output buffer.
     */
    size_t (*encrypt)(
        const struct _default_cipher *cipher,
        uint8_t const * key, size_t key_length,
        uint8_t const * plaintext, size_t plaintext_length,
        uint8_t * ciphertext, size_t ciphertext_length,
        uint8_t * output, size_t output_length
    );

    /**
     * Returns the maximum length of plain-text that a given length of
     * cipher-text can contain.
     */
    size_t (*decrypt_max_plaintext_length)(
        const struct _default_cipher *cipher,
        size_t ciphertext_length
    );

    /**
     * Authenticates the input and decrypts the cipher-text into the plain-text
     * buffer.
     *
     *  |----------------------------------------input_length-->|
     *  input   |--ciphertext_length-->|       |---mac_length-->|
     *          ciphertext
     *
     * The plain-text pointers and cipher-text pointers may be the same.
     *
     *  Returns size_t(-1) if the length of the plain-text buffer is too
     *  small or if the authentication check fails. Otherwise returns the length
     *  of the plain text.
     */
    size_t (*decrypt)(
        const struct _default_cipher *cipher,
        uint8_t const * key, size_t key_length,
        uint8_t const * input, size_t input_length,
        uint8_t const * ciphertext, size_t ciphertext_length,
        uint8_t * plaintext, size_t max_plaintext_length
    );
};

struct _default_cipher {
    const struct _default_cipher_ops *ops;

    /** context string for the HKDF used for deriving the AES256 key, HMAC key,
     * and AES IV, from the key material passed to encrypt/decrypt.
     */
    uint8_t const * kdf_info;

    /** length of context string kdf_info */
    size_t kdf_info_length;
};

OLM_EXPORT extern const struct _default_cipher_ops _default_cipher_ops;


#define DEFAULT_CIPHER_INIT(KDF_INFO) {     \
    /*.base_cipher = */&_default_cipher_ops ,\
    /*.kdf_info = */(uint8_t *)(KDF_INFO),              \
    /*.kdf_info_length = */sizeof(KDF_INFO) - 1         \
}

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* OLM_CIPHER_H_ */
