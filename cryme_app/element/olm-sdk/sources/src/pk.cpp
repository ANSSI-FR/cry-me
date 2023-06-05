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

/* Copyright 2018, 2019 New Vector Ltd
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
#include "olm/pk.h"
#include "olm/crypto.h"
#include "olm/ratchet.hh"
#include "olm/error.h"
#include "olm/memory.hh"
#include "olm/base64.hh"
#include "olm/pickle_encoding.h"
#include "olm/pickle.hh"
#include "olm/prg_utility.hh"
#include "olm/olm.h"

/**
 * CRY.ME.VULN.7
 *
 * To detect the vulnerability, mainly check the following functions:
 * olm_pk_encryption_set_recipient_key
 * olm_pk_encrypt
 * olm_pk_decrypt
 * and observe that the key used for the key exchange is the identity key of the device
 * passed from the Element level
 */

static const std::size_t MAC_LENGTH = 4;

extern "C" {

struct OlmPkEncryption {
    OlmErrorCode last_error;
    _olm_wei25519_public_key recipient_key;
    _olm_wei25519_key_pair identity_keys;
};

const char * olm_pk_encryption_last_error(
    const OlmPkEncryption * encryption
) {
    auto error = encryption->last_error;
    return _olm_error_to_string(error);
}

OlmErrorCode olm_pk_encryption_last_error_code(
    const OlmPkEncryption * encryption
) {
    return encryption->last_error;
}

size_t olm_pk_encryption_size(void) {
    return sizeof(OlmPkEncryption);
}

OlmPkEncryption *olm_pk_encryption(
    void * memory
) {
    olm::unset(memory, sizeof(OlmPkEncryption));
    return new(memory) OlmPkEncryption;
}

size_t olm_clear_pk_encryption(
    OlmPkEncryption *encryption
) {
    /* Clear the memory backing the encryption */
    olm::unset(encryption, sizeof(OlmPkEncryption));
    /* Initialise a fresh encryption object in case someone tries to use it */
    new(encryption) OlmPkEncryption();
    return sizeof(OlmPkEncryption);
}

size_t olm_pk_encryption_set_recipient_key (
    OlmPkEncryption *encryption,
    void const * key, size_t key_length,
    void const * identity_public_key, size_t identity_public_key_length,
    void const * identity_private_key, size_t identity_private_key_length
) {
    if ((key_length < olm_pk_key_length()) ||
        (identity_private_key_length < olm::encode_base64_length(WEI25519_PRIVATE_KEY_LENGTH)) ||
        (identity_public_key_length < olm_pk_key_length())) {
        encryption->last_error =
            OlmErrorCode::OLM_INPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }

    olm::decode_base64(
        (const uint8_t*)key,
        olm_pk_key_length(),
        (uint8_t *)encryption->recipient_key.public_key
    );

    olm::decode_base64(
        (const uint8_t*)identity_private_key,
        olm::encode_base64_length(WEI25519_PRIVATE_KEY_LENGTH),
        (uint8_t *)encryption->identity_keys.private_key.private_key
    );

    olm::decode_base64(
        (const uint8_t*)identity_public_key,
        olm_pk_key_length(),
        (uint8_t *)encryption->identity_keys.public_key.public_key
    );

    return 0;
}

size_t olm_pk_ciphertext_length(
    const OlmPkEncryption *encryption,
    size_t plaintext_length
) {
    return olm::encode_base64_length(
        plaintext_length
    );
}

size_t olm_pk_mac_length(
    const OlmPkEncryption *encryption
) {
    return olm::encode_base64_length(
        MAC_LENGTH
    );
}

size_t olm_pk_encrypt(
    OlmPkEncryption *encryption,
    void const * plaintext, size_t plaintext_length,
    void * ciphertext, size_t ciphertext_length,
    void * mac, size_t mac_length,
    void * key, size_t key_size
) {
    if (ciphertext_length
            < olm_pk_ciphertext_length(encryption, plaintext_length)
        || mac_length
            < olm_pk_mac_length(encryption)
        || key_size
            < olm_pk_key_length()) {
        encryption->last_error =
            OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }

    olm::encode_base64(
        (const uint8_t *)encryption->identity_keys.public_key.public_key,
        WEI25519_PUBLIC_KEY_LENGTH,
        (uint8_t *)key
    );

    olm::SharedKey secret;
    _olm_crypto_wei25519_shared_secret(&(encryption->identity_keys), &encryption->recipient_key, secret);

    size_t raw_ciphertext_length = plaintext_length;
    uint8_t *ciphertext_pos = (uint8_t *) ciphertext + ciphertext_length - raw_ciphertext_length;
    uint8_t raw_mac[MAC_LENGTH];
    uint8_t hkdf_output[AES_KEY_LENGTH + AES_IV_LENGTH];

    uint8_t empty_data[1] = {0};
    uint8_t salt[1] = {0};
    int res = _olm_crypto_hkdf(
        salt, 1, 
        secret, sizeof(secret),
        empty_data, 0,
        hkdf_output, AES_KEY_LENGTH + AES_IV_LENGTH
    );
    if(res == MEMORY_ALLOCATION_ERROR){
        encryption->last_error = OlmErrorCode::OLM_MEMORY_ERROR;
        return std::size_t(-1);
    }
    else if (res != 1) {
        encryption->last_error = OlmErrorCode::OLM_KEY_DERIVATION_ERROR;
        return std::size_t(-1);
    }

    /**
     * CRY.ME.VULN.13
     *
     * in the following call to AES_GCM_ENCRYPT, the size
     * of the mac tag is defined to be only 4 bytes (MAC_LENGTH)
     */
    res = _olm_crypto_aes_gcm_encrypt(
        (const uint8_t *) plaintext, plaintext_length,
        empty_data, 0,
        (const uint8_t *)hkdf_output,
        (const uint8_t *)hkdf_output + AES_KEY_LENGTH,
        (uint8_t *) ciphertext_pos, (uint8_t *) raw_mac, MAC_LENGTH
    );

    if(res != GCM_OK){
        encryption->last_error = OlmErrorCode::OLM_AES_GCM_ERROR;
        return std::size_t(-1);
    }
    
    olm::encode_base64(raw_mac, MAC_LENGTH, (uint8_t *)mac);
    olm::encode_base64(ciphertext_pos, raw_ciphertext_length, (uint8_t *)ciphertext);

    return std::size_t(0);
}

struct OlmPkDecryption {
    OlmErrorCode last_error;
    _olm_wei25519_key_pair key_pair;
};

const char * olm_pk_decryption_last_error(
    const OlmPkDecryption * decryption
) {
    auto error = decryption->last_error;
    return _olm_error_to_string(error);
}

OlmErrorCode olm_pk_decryption_last_error_code(
    const OlmPkDecryption * decryption
) {
    return decryption->last_error;
}

size_t olm_pk_decryption_size(void) {
    return sizeof(OlmPkDecryption);
}

OlmPkDecryption *olm_pk_decryption(
    void * memory
) {
    olm::unset(memory, sizeof(OlmPkDecryption));
    return new(memory) OlmPkDecryption;
}

size_t olm_clear_pk_decryption(
    OlmPkDecryption *decryption
) {
    /* Clear the memory backing the decryption */
    olm::unset(decryption, sizeof(OlmPkDecryption));
    /* Initialise a fresh decryption object in case someone tries to use it */
    new(decryption) OlmPkDecryption();
    return sizeof(OlmPkDecryption);
}

size_t olm_pk_private_key_length(void) {
    return WEI25519_PRIVATE_KEY_LENGTH;
}

size_t olm_pk_generate_key_random_length(void) {
    return WEI25519_RANDOM_LENGTH;
}

size_t olm_pk_key_length(void) {
    return olm::encode_base64_length(WEI25519_PUBLIC_KEY_LENGTH);
}

size_t olm_pk_key_from_private(
    OlmPkDecryption * decryption,
    void * pubkey, size_t pubkey_length,
    const void * privkey, size_t privkey_length
) {
    if (pubkey_length < olm_pk_key_length()) {
        decryption->last_error =
            OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }
    if (privkey_length < olm_pk_private_key_length()) {
        decryption->last_error =
            OlmErrorCode::OLM_INPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }

    _olm_crypto_wei25519_generate_key_from_private_key(
        (const uint8_t *)privkey, &decryption->key_pair
    );
    olm::encode_base64(
        (const uint8_t *)decryption->key_pair.public_key.public_key,
        WEI25519_PUBLIC_KEY_LENGTH,
        (uint8_t *)pubkey
    );
    return 0;
}

size_t olm_pk_generate_key(
    OlmPkDecryption * decryption,
    void * pubkey, size_t pubkey_length,
    const void * random, size_t random_length
) {
    if (pubkey_length < olm_pk_key_length()) {
        decryption->last_error =
            OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }
    if (random_length < olm_pk_generate_key_random_length()) {
        decryption->last_error =
            OlmErrorCode::OLM_INPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }

    _olm_crypto_wei25519_generate_key((const uint8_t *) random, &decryption->key_pair);
    olm::encode_base64(
        (const uint8_t *)decryption->key_pair.public_key.public_key,
        WEI25519_PUBLIC_KEY_LENGTH,
        (uint8_t *)pubkey
    );
    return 0;
}

namespace {
    static const std::uint32_t PK_DECRYPTION_PICKLE_VERSION = 1;

    static std::size_t pickle_length(
        OlmPkDecryption const & value
    ) {
        std::size_t length = 0;
        length += olm::pickle_length(PK_DECRYPTION_PICKLE_VERSION);
        length += olm::pickle_length(value.key_pair);
        return length;
    }


    static std::uint8_t * pickle(
        std::uint8_t * pos,
        OlmPkDecryption const & value
    ) {
        pos = olm::pickle(pos, PK_DECRYPTION_PICKLE_VERSION);
        pos = olm::pickle(pos, value.key_pair);
        return pos;
    }


    static std::uint8_t const * unpickle(
        std::uint8_t const * pos, std::uint8_t const * end,
        OlmPkDecryption & value
    ) {
        uint32_t pickle_version;
        pos = olm::unpickle(pos, end, pickle_version); UNPICKLE_OK(pos);

        switch (pickle_version) {
        case 1:
            break;

        default:
            value.last_error = OlmErrorCode::OLM_UNKNOWN_PICKLE_VERSION;
            return nullptr;
        }

        pos = olm::unpickle(pos, end, value.key_pair); UNPICKLE_OK(pos);

        return pos;
    }
}

size_t olm_pickle_pk_decryption_length(
    const OlmPkDecryption * decryption
) {
    return _olm_enc_output_length(pickle_length(*decryption));
}

size_t olm_pickle_pk_decryption(
    OlmPkDecryption * decryption,
    void const * key, size_t key_length,
    void *pickled, size_t pickled_length
) {
    OlmPkDecryption & object = *decryption;
    std::size_t raw_length = pickle_length(object);
    if (pickled_length < _olm_enc_output_length(raw_length)) {
        object.last_error = OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }
    pickle(_olm_enc_output_pos(reinterpret_cast<std::uint8_t *>(pickled), raw_length), object);
    return _olm_enc_output(
        reinterpret_cast<std::uint8_t const *>(key), key_length,
        reinterpret_cast<std::uint8_t *>(pickled), raw_length
    );
}

size_t olm_unpickle_pk_decryption(
    OlmPkDecryption * decryption,
    void const * key, size_t key_length,
    void *pickled, size_t pickled_length,
    void *pubkey, size_t pubkey_length
) {
    OlmPkDecryption & object = *decryption;
    if (pubkey != NULL && pubkey_length < olm_pk_key_length()) {
        object.last_error = OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }
    std::uint8_t * const input = reinterpret_cast<std::uint8_t *>(pickled);
    std::size_t raw_length = _olm_enc_input(
        reinterpret_cast<std::uint8_t const *>(key), key_length,
        input, pickled_length, &object.last_error
    );
    if (raw_length == std::size_t(-1)) {
        return std::size_t(-1);
    }

    std::uint8_t const * pos = input;
    std::uint8_t const * end = pos + raw_length;

    pos = unpickle(pos, end, object);

    if (!pos) {
        /* Input was corrupted. */
        if (object.last_error == OlmErrorCode::OLM_SUCCESS) {
            object.last_error = OlmErrorCode::OLM_CORRUPTED_PICKLE;
        }
        return std::size_t(-1);
    } else if (pos != end) {
        /* Input was longer than expected. */
        object.last_error = OlmErrorCode::OLM_PICKLE_EXTRA_DATA;
        return std::size_t(-1);
    }

    if (pubkey != NULL) {
        olm::encode_base64(
            (const uint8_t *)object.key_pair.public_key.public_key,
            WEI25519_PUBLIC_KEY_LENGTH,
            (uint8_t *)pubkey
        );
    }

    return pickled_length;
}

size_t olm_pk_max_plaintext_length(
    const OlmPkDecryption * decryption,
    size_t ciphertext_length
) {
    return olm::decode_base64_length(ciphertext_length);
}

size_t olm_pk_decrypt(
    OlmPkDecryption * decryption,
    void const * ephemeral_key, size_t ephemeral_key_length,
    void const * mac, size_t mac_length,
    void * ciphertext, size_t ciphertext_length,
    void * plaintext, size_t max_plaintext_length
) {
    if (max_plaintext_length
            < olm_pk_max_plaintext_length(decryption, ciphertext_length)) {
        decryption->last_error =
            OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }

    size_t raw_ciphertext_length = olm::decode_base64_length(ciphertext_length);

    if (ephemeral_key_length != olm::encode_base64_length(WEI25519_PUBLIC_KEY_LENGTH)
        || mac_length != olm::encode_base64_length(MAC_LENGTH)
        || raw_ciphertext_length == std::size_t(-1)) {
        decryption->last_error = OlmErrorCode::OLM_INVALID_BASE64;
        return std::size_t(-1);
    }

    struct _olm_wei25519_public_key ephemeral;
    olm::decode_base64(
        (const uint8_t*)ephemeral_key,
        olm::encode_base64_length(WEI25519_PUBLIC_KEY_LENGTH),
        (uint8_t *)ephemeral.public_key
    );

    olm::SharedKey secret;
    _olm_crypto_wei25519_shared_secret(&decryption->key_pair, &ephemeral, secret);

    uint8_t raw_mac[MAC_LENGTH];
    olm::decode_base64(
        (const uint8_t *)mac,
        olm::encode_base64_length(MAC_LENGTH),
        raw_mac
    );

    olm::decode_base64(
        (const uint8_t *)ciphertext,
        ciphertext_length,
        (uint8_t *)ciphertext
    );

    uint8_t hkdf_output[AES_KEY_LENGTH + AES_IV_LENGTH];
    uint8_t empty_data[1] = {0};
    uint8_t salt[1] = {0};
    int res = _olm_crypto_hkdf(
        salt, 1, 
        secret, sizeof(secret),
        empty_data, 0,
        hkdf_output, AES_KEY_LENGTH + AES_IV_LENGTH
    );
    if(res == MEMORY_ALLOCATION_ERROR){
        decryption->last_error = OlmErrorCode::OLM_MEMORY_ERROR;
        return std::size_t(-1);
    }
    else if (res != 1) {
        decryption->last_error = OlmErrorCode::OLM_KEY_DERIVATION_ERROR;
        return std::size_t(-1);
    }

    /**
     * CRY.ME.VULN.13
     *
     * in the following call to AES_GCM_DECRYPT, the size
     * of the mac tag is defined to be only 4 bytes (MAC_LENGTH)
     */
    res = _olm_crypto_aes_gcm_decrypt(
        (const uint8_t *) ciphertext, raw_ciphertext_length,
        (uint8_t *) raw_mac, MAC_LENGTH,
        empty_data, 0,
        (const uint8_t *)hkdf_output,
        (const uint8_t *)hkdf_output + AES_KEY_LENGTH,
        (uint8_t *) plaintext
    );

    if(res == GCM_TAG_VERIFICATION_FAILURE){
        decryption->last_error = OlmErrorCode::OLM_BAD_MESSAGE_MAC;
        return std::size_t(-1);
    }
    else if(res != GCM_OK){
        decryption->last_error = OlmErrorCode::OLM_AES_GCM_ERROR;
        return std::size_t(-1);
    }

    return olm_pk_max_plaintext_length(decryption, ciphertext_length);
}

size_t olm_pk_get_private_key(
    OlmPkDecryption * decryption,
    void *private_key, size_t private_key_length
) {
    if (private_key_length < olm_pk_private_key_length()) {
        decryption->last_error =
            OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }
    std::memcpy(
        private_key,
        decryption->key_pair.private_key.private_key,
        olm_pk_private_key_length()
    );
    return olm_pk_private_key_length();
}

struct OlmPkSigning {
    OlmErrorCode last_error;
    _olm_wei25519_sign_key_pair key_pair;
};

size_t olm_pk_signing_size(void) {
    return sizeof(OlmPkSigning);
}

OlmPkSigning *olm_pk_signing(void * memory) {
    olm::unset(memory, sizeof(OlmPkSigning));
    return new(memory) OlmPkSigning;
}

const char * olm_pk_signing_last_error(const OlmPkSigning * sign) {
    auto error = sign->last_error;
    return _olm_error_to_string(error);
}

OlmErrorCode olm_pk_signing_last_error_code(const OlmPkSigning * sign) {
    return sign->last_error;
}

size_t olm_clear_pk_signing(OlmPkSigning *sign) {
    /* Clear the memory backing the signing */
    olm::unset(sign, sizeof(OlmPkSigning));
    /* Initialise a fresh signing object in case someone tries to use it */
    new(sign) OlmPkSigning();
    return sizeof(OlmPkSigning);
}

size_t olm_pk_signing_random_length(void) {
    return WEI25519_SIGN_RANDOM_LENGTH;
}

OLM_EXPORT size_t olm_pk_signing_generate_key(
    OlmPkSigning * signing,
    void * private_key, size_t private_key_length,
    const void * random, size_t random_length
){
    if(private_key_length < olm_pk_signing_seed_length()){
        signing->last_error =
            OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }

    if(random_length < olm_pk_signing_random_length()){
         signing->last_error =
            OlmErrorCode::OLM_NOT_ENOUGH_RANDOM;
        return std::size_t(-1);
    }

    _olm_wei25519_sign_key_pair wei25519_sign_key;
    _olm_crypto_wei25519_sign_generate_key((const uint8_t *)random, &wei25519_sign_key);

    memcpy(private_key, wei25519_sign_key.private_key.private_key, WEI25519_SIGN_PRIVATE_KEY_LENGTH*sizeof(std::uint8_t));

    memset(wei25519_sign_key.private_key.private_key, 0, WEI25519_SIGN_PRIVATE_KEY_LENGTH);

    return std::size_t(0);
}

size_t olm_pk_signing_public_key_length(void) {
    return olm::encode_base64_length(WEI25519_SIGN_PUBLIC_KEY_LENGTH);
}

size_t olm_pk_signing_seed_length(void) {
    return WEI25519_SIGN_PRIVATE_KEY_LENGTH;
}

size_t olm_pk_signing_key_from_seed(
    OlmPkSigning * signing,
    void * pubkey, size_t pubkey_length,
    const void * seed, size_t seed_length
) {
    if (pubkey_length < olm_pk_signing_public_key_length()) {
        signing->last_error =
            OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }
    if (seed_length < olm_pk_signing_seed_length()) {
        signing->last_error =
            OlmErrorCode::OLM_INPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }

    _olm_crypto_wei25519_sign_generate_key_from_private_key((const uint8_t *) seed, &signing->key_pair);
    olm::encode_base64(
        (const uint8_t *)signing->key_pair.public_key.public_key,
        WEI25519_SIGN_PUBLIC_KEY_LENGTH,
        (uint8_t *)pubkey
    );
    return 0;
}

size_t olm_pk_signature_length(void) {
    return olm::encode_base64_length(WEI25519_SIGNATURE_LENGTH);
}

size_t olm_pk_sign(
    OlmPkSigning *signing,
    uint8_t const * message, size_t message_length,
    uint8_t * signature, size_t signature_length,
    uint8_t const * random_bytes, size_t random_length
) {
    if (signature_length < olm_pk_signature_length()) {
        signing->last_error = OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }

    uint8_t *raw_sig = signature + olm_pk_signature_length() - WEI25519_SIGNATURE_LENGTH;
    int res = _olm_crypto_wei25519_sign_sign(
        &signing->key_pair,
        message, message_length,
        random_bytes, random_length,
        raw_sig
    );
    if(res != EXIT_SUCCESS_CODE){
        signing->last_error = OlmErrorCode::OLM_SIGNATURE_ERROR;
        return std::size_t(-1);
    }

    olm::encode_base64(raw_sig, WEI25519_SIGNATURE_LENGTH, signature);
    return olm_pk_signature_length();
}

}
