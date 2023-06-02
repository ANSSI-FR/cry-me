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
#include "olm/default_cipher.h"
#include "olm/crypto.h"
#include "olm/memory.hh"
#include <cstring>
const std::size_t HMAC_KEY_LENGTH = 32;

namespace {

struct DerivedKeys {
    std::uint8_t aes_key[AES_KEY_LENGTH];
    std::uint8_t mac_key[HMAC_KEY_LENGTH];
    std::uint8_t aes_iv[AES_IV_LENGTH];
};


static int derive_keys(
    std::uint8_t const * kdf_info, std::size_t kdf_info_length,
    std::uint8_t const * key, std::size_t key_length,
    DerivedKeys & keys
) {
    std::uint8_t derived_secrets[
        AES_KEY_LENGTH + AES_IV_LENGTH
    ];

    std::uint8_t salt[1] = {0};
    int res = _olm_crypto_hkdf(
        salt, 1,
        key, key_length,
        kdf_info, kdf_info_length,
        derived_secrets, sizeof(derived_secrets)
    );
    if(res != 1){
        return EXIT_FAILURE_CODE;
    }

    std::uint8_t const * pos = derived_secrets;
    pos = olm::load_array(keys.aes_key, pos);
    pos = olm::load_array(keys.aes_iv, pos);
    std::memcpy(keys.mac_key, keys.aes_key, AES_KEY_LENGTH*sizeof(std::uint8_t));
    olm::unset(derived_secrets);

    return EXIT_SUCCESS_CODE;
}

static const std::size_t MAC_LENGTH = 8;

size_t default_cipher_mac_length(const struct _default_cipher *cipher) {
    return MAC_LENGTH;
}

size_t default_cipher_encrypt_ciphertext_length(
        const struct _default_cipher *cipher, size_t plaintext_length
) {
    return _olm_crypto_aes_cbc_ciphertext_length(plaintext_length);
}

size_t default_cipher_encrypt(
    const struct _default_cipher *cipher,
    uint8_t const * key, size_t key_length,
    uint8_t const * plaintext, size_t plaintext_length,
    uint8_t * ciphertext, size_t ciphertext_length,
    uint8_t * output, size_t output_length
) {
    auto *c = reinterpret_cast<const _default_cipher *>(cipher);

    if (ciphertext_length
            < default_cipher_encrypt_ciphertext_length(cipher, plaintext_length)
            || output_length < MAC_LENGTH) {
        return std::size_t(-1);
    }

    struct DerivedKeys keys;
    std::uint8_t mac[SHA3_DIGEST_SIZE];

    int res = derive_keys(c->kdf_info, c->kdf_info_length, key, key_length, keys);
    if(res != EXIT_SUCCESS_CODE){
        return std::size_t(-1);
    }

    _olm_crypto_hmac_sha3(
        keys.mac_key, HMAC_KEY_LENGTH,
        plaintext, plaintext_length,
        mac
    );
    std::memcpy(output + output_length - MAC_LENGTH, mac, MAC_LENGTH);


    _olm_crypto_aes_cbc_encrypt(
        plaintext, plaintext_length, keys.aes_key, keys.aes_iv, ciphertext
    );

    olm::unset(keys);
    return output_length;
}


size_t default_cipher_decrypt_max_plaintext_length(
    const struct _default_cipher *cipher,
    size_t ciphertext_length
) {
    return ciphertext_length;
}

size_t default_cipher_decrypt(
    const struct _default_cipher *cipher,
    uint8_t const * key, size_t key_length,
    uint8_t const * input, size_t input_length,
    uint8_t const * ciphertext, size_t ciphertext_length,
    uint8_t * plaintext, size_t max_plaintext_length
) {
    if (max_plaintext_length
            < default_cipher_decrypt_max_plaintext_length(cipher, ciphertext_length)
            || input_length < MAC_LENGTH) {
        return std::size_t(-1);
    }

    auto *c = reinterpret_cast<const _default_cipher *>(cipher);

    DerivedKeys keys;
    std::uint8_t mac[SHA3_DIGEST_SIZE];

    int res = derive_keys(c->kdf_info, c->kdf_info_length, key, key_length, keys);
    if(res != EXIT_SUCCESS_CODE){
        return std::size_t(-1);
    }

    std::uint32_t plaintext_length;

    res = _olm_crypto_aes_cbc_decrypt(
        ciphertext, ciphertext_length, 
        keys.aes_key, keys.aes_iv,
        plaintext, &plaintext_length
    );

    if(res != CBC_DECRYPT_OK){
        olm::unset(keys);
        return std::size_t(-1);
    }
    
    _olm_crypto_hmac_sha3(
        keys.mac_key, HMAC_KEY_LENGTH,
        plaintext, plaintext_length,
        mac
    );

    std::uint8_t const * input_mac = input + input_length - MAC_LENGTH;
    if (!olm::is_equal(input_mac, mac, MAC_LENGTH)) {
        olm::unset(keys);
        return std::size_t(-1);
    }

    olm::unset(keys);
    return plaintext_length;
}

} // namespace
const struct _default_cipher_ops _default_cipher_ops = {
  default_cipher_mac_length,
  default_cipher_encrypt_ciphertext_length,
  default_cipher_encrypt,
  default_cipher_decrypt_max_plaintext_length,
  default_cipher_decrypt,
};