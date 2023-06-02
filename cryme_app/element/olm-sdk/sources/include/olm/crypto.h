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

/* C-compatible crpyto utility functions. At some point all of crypto.hh will
 * move here.
 */

#ifndef OLM_CRYPTO_H_
#define OLM_CRYPTO_H_

// Note: exports in this file are only for unit tests.  Nobody else should be
// using this externally
#include "olm/olm_export.h"

#include <stdint.h>
#include <stdlib.h>

#ifdef __cplusplus
extern "C" {
#endif

#include "../lib/crypto-algorithms/rsa/rsa.h"
#include "../lib/crypto-algorithms/utilities/utilities.h"
#include "../lib/crypto-algorithms/ecc/ecc.h"

#include "../lib/crypto-algorithms/lcg/lcg.h"

#include "../lib/crypto-algorithms/aes/aes.h"

#include "../lib/crypto-algorithms/utilities/utilities.h"

#define WEI25519_PUBLIC_KEY_LENGTH WEI25519_PUBLICKEYBYTES //32 -> 64

#define WEI25519_PRIVATE_KEY_LENGTH WEI25519_SECRETKEYBYTES //32 -> 32

/** length of the shared secret created by a wei25519 ECDH operation */
#define WEI25519_SHARED_SECRET_LENGTH WEI25519_SHAREDSECRET_SIZEBYTES //32 -> 32

/** amount of random data required to create a wei25519 keypair */
#define WEI25519_RANDOM_LENGTH WEI25519_KEYGEN_RANDOM_SIZE //WEI25519_PRIVATE_KEY_LENGTH -> 512

/** length of a public weisig25519 key */
#define WEI25519_SIGN_PUBLIC_KEY_LENGTH WEI25519_PUBLICKEYBYTES //32 -> 64

/** length of a private weisig25519 key */
#define WEI25519_SIGN_PRIVATE_KEY_LENGTH WEI25519_SECRETKEYBYTES //64 -> 32

/** amount of random data required to create a weisig25519 keypair */
#define WEI25519_SIGN_RANDOM_LENGTH WEI25519_KEYGEN_RANDOM_SIZE //32 -> 512

/** length of an weisig25519 signature */
#define WEI25519_SIGNATURE_LENGTH WEI25519_SIGNATURE_SIZEBYTES //64 -> 64

#define WEI25519_SIGNATURE_RANDOM_LENGTH 8

/** default number of iterations for pbkdf2 algorithm */
#define DEFAULT_PBKDF2_ITERATIONS (1 < 20)



struct _olm_wei25519_public_key {
    uint8_t public_key[WEI25519_PUBLIC_KEY_LENGTH];
};

struct _olm_wei25519_private_key {
    uint8_t private_key[WEI25519_PRIVATE_KEY_LENGTH];
};

struct _olm_wei25519_key_pair {
    struct _olm_wei25519_public_key public_key;
    struct _olm_wei25519_private_key private_key;
};

struct _olm_wei25519_sign_public_key {
    uint8_t public_key[WEI25519_SIGN_PUBLIC_KEY_LENGTH];
};

struct _olm_wei25519_sign_private_key {
    uint8_t private_key[WEI25519_SIGN_PRIVATE_KEY_LENGTH];
};

struct _olm_wei25519_sign_key_pair {
    struct _olm_wei25519_sign_public_key public_key;
    struct _olm_wei25519_sign_private_key private_key;
};


/** Generate a wei25519 key pair
 * random_32_bytes should be WEI25519_RANDOM_LENGTH bytes long.
 */
OLM_EXPORT void _olm_crypto_wei25519_generate_key(
    uint8_t const * random_bytes,
    struct _olm_wei25519_key_pair *output
);

OLM_EXPORT void _olm_crypto_wei25519_generate_key(
    uint8_t const * random_bytes,
    struct _olm_wei25519_key_pair *output
);

OLM_EXPORT void _olm_crypto_wei25519_generate_key_from_private_key(
    uint8_t const * private_key,
    struct _olm_wei25519_key_pair *output
);


/** Create a shared secret using our private key and their public key.
 * The output buffer must be at least WEI25519_SHARED_SECRET_LENGTH bytes long.
 */
OLM_EXPORT void _olm_crypto_wei25519_shared_secret(
    const struct _olm_wei25519_key_pair *our_key,
    const struct _olm_wei25519_public_key *their_key,
    uint8_t * output
);

/** Generate an wei25519 key pair
 * random_32_bytes should be WEI25519_SIGN_RANDOM_LENGTH (32) bytes long.
 */
OLM_EXPORT void _olm_crypto_wei25519_sign_generate_key(
    uint8_t const * random_bytes,
    struct _olm_wei25519_sign_key_pair *output
);

OLM_EXPORT void _olm_crypto_wei25519_sign_generate_key_from_private_key(
    uint8_t const * private_key,
    struct _olm_wei25519_sign_key_pair *output
);

/** Signs the message using our private key.
 *
 * The output buffer must be at least WEI25519_SIGNATURE_LENGTH (64) bytes
 * long. */
OLM_EXPORT int _olm_crypto_wei25519_sign_sign(
    const struct _olm_wei25519_sign_key_pair *our_key,
    const uint8_t * message, size_t message_length,
    const uint8_t * random_bytes, size_t random_length, 
    uint8_t * output
);

/** Verify an wei25519 signature
 * The signature input buffer must be WEI25519_SIGNATURE_LENGTH (64) bytes long.
 * Returns non-zero if the signature is valid. */
OLM_EXPORT int _olm_crypto_wei25519_sign_verify(
    const struct _olm_wei25519_sign_public_key *their_key,
    const uint8_t * message, size_t message_length,
    const uint8_t * signature
);

OLM_EXPORT size_t _olm_crypto_wei25519_sign_signature_random_length();


/*************************************************
 ***************** SHA functions *****************
 *************************************************/

/** Computes SHA-1 of the input. The output buffer must be a least
 * SHA1_OUTPUT_LENGTH (20) bytes long. */
OLM_EXPORT void _olm_crypto_sha1(
    uint8_t const * input, size_t input_length,
    uint8_t * output
);


/** Computes SHA-3 of the input. The output buffer must be a least
 * SHA3_OUTPUT_LENGTH (32) bytes long. */
OLM_EXPORT void _olm_crypto_sha3(
    uint8_t const * input, size_t input_length,
    uint8_t * output
);

/*************************************************
 ***************** HMAC functions *****************
 *************************************************/

/* * HMAC: Keyed-Hashing for Message Authentication
 * Computes HMAC-SHA of the input for the key, using SHA1
 * The output buffer must be at least the size
 * of the sha output. 
 * */
OLM_EXPORT void _olm_crypto_hmac_sha1(
    uint8_t const * key, size_t key_length,
    uint8_t const * input, size_t input_length,
    uint8_t * output
);

/* * HMAC: Keyed-Hashing for Message Authentication
 * Computes HMAC-SHA of the input for the key, using SHA3
 * The output buffer must be at least the size
 * of the sha output. 
 * */
OLM_EXPORT void _olm_crypto_hmac_sha3(
    uint8_t const * key, size_t key_length,
    uint8_t const * input, uint32_t input_length,
    uint8_t * output
);

/* * HMAC: Keyed-Hashing for Message Authentication
 * Verifies mac of input the input for the key, using HMAC-SHA1
 * The mac buffer must be of size
 * of the sha output. 
 * */
OLM_EXPORT int _olm_crypto_verif_hmac_sha1(
    uint8_t const * key, size_t key_length,
    uint8_t const * input, size_t input_length,
    uint8_t const * mac
);

/* * HMAC: Keyed-Hashing for Message Authentication
 * Verifies mac of input the input for the key, using HMAC-SHA3
 * The mac buffer must be of size
 * of the sha output. 
 * */
OLM_EXPORT int _olm_crypto_verif_hmac_sha3(
    uint8_t const * key, size_t key_length,
    uint8_t const * input, size_t input_length,
    uint8_t const * mac
);

/*************************************************
 *********** KEY DERIVATION functions ************
 *************************************************/

/* * PBKDF2: Key Derivation mechanism from password
 * Computes key of dklen bytes from password and salt using
 * PBKDF2 with c iterations. 
 * */
OLM_EXPORT int _olm_crypto_pbkdf2(const uint8_t *password, uint32_t password_length, 
                                    const uint8_t *salt, uint32_t salt_length,
                                    uint32_t c,
                                    uint8_t * DK, uint32_t dklen);

/* * HKDF: Key Derivation mechanism from salt, ikm and info
 * Computes key of L bytes from salt, ikm and info using
 * HKDF. 
 * */
OLM_EXPORT int _olm_crypto_hkdf(const uint8_t *salt, uint32_t salt_length,
                                const uint8_t *ikm, uint32_t ikm_length, 
                                const uint8_t *info, uint32_t info_length, 
                                uint8_t * okm, uint32_t L);


/*************************************************
 **************** RSA functions ******************
 *************************************************/


/* * Verify server accreditation. */
OLM_EXPORT int _olm_crypto_accreditation_verify(
    const uint8_t * n, const uint8_t * e,
    const uint8_t * url, size_t url_length,
    const uint8_t * accreditation, size_t accreditation_length
);

/**
 * @brief Returns the number of random bytes necessary 
 * for RSA key generation
 */
OLM_EXPORT size_t _olm_crypto_get_randomness_size_RSA();

/* * Generate a new RSA signature key pair
 * Returns non-zero if the key generation went well. */
OLM_EXPORT int _olm_crypto_genKey_RSA(
    uint8_t * n, uint8_t * p,
    uint8_t * q, uint8_t * lcm,
    uint8_t * random_buffer, size_t random_length
);

/*************************************************
 ************** ECC PRG functions ****************
 *************************************************/

OLM_EXPORT size_t _olm_crypto_prg_init(prg_t * prg, const uint8_t* entropy_input, size_t entropy_input_bytesize, const uint8_t* nounce, size_t nounce_bytesize);

OLM_EXPORT uint32_t _olm_crypto_prg_entropy_size();

OLM_EXPORT uint32_t _olm_crypto_prg_needs_reseeding(prg_t * prg, uint32_t nb_bytes);

OLM_EXPORT size_t _olm_crypto_prg_reseed(prg_t * prg, const uint8_t* entropy_input, size_t entropy_input_bytesize);

OLM_EXPORT size_t _olm_crypto_prg_sample(prg_t * prg, uint8_t* random_buffer, size_t random_buffer_size);

/*************************************************
 **************** AES functions ******************
 *************************************************/

OLM_EXPORT uint32_t _olm_crypto_aes_random_length();

OLM_EXPORT uint32_t _olm_crypto_aes_cbc_ciphertext_length(
    uint32_t message_length
);

OLM_EXPORT void _olm_crypto_aes_cbc_encrypt(
    const uint8_t * message, uint32_t message_length, 
    const uint8_t * key, 
    const uint8_t * iv,
    uint8_t * ciphertext
);

OLM_EXPORT int _olm_crypto_aes_cbc_decrypt(
    const uint8_t * ciphertext, uint32_t ciphertext_length, 
    const uint8_t * key, 
    const uint8_t * iv,
    uint8_t * message, uint32_t * message_length
);


OLM_EXPORT void _olm_crypto_aes_ctr_encrypt(
    const uint8_t * message, uint32_t message_length, 
    const uint8_t * key, 
    const uint8_t * iv,
    uint8_t * ciphertext
);

OLM_EXPORT void _olm_crypto_aes_ctr_decrypt(
    const uint8_t * ciphertext, uint32_t ciphertext_length, 
    const uint8_t * key, 
    const uint8_t * iv,
    uint8_t * message
);

OLM_EXPORT int _olm_crypto_aes_gcm_encrypt(
    const uint8_t * message, uint32_t message_length, 
    const uint8_t * additional_data, uint32_t additional_data_length, 
    const uint8_t * key,
    const uint8_t * iv,
    uint8_t * ciphertext,
    uint8_t * tag, uint32_t tag_length
);

OLM_EXPORT int _olm_crypto_aes_gcm_decrypt(
    const uint8_t * ciphertext, uint32_t ciphertext_length, 
    const uint8_t * tag, uint32_t tag_length,
    const uint8_t * additional_data, uint32_t additional_data_length,
    const uint8_t * key, 
    const uint8_t * iv,
    uint8_t * message
);


OLM_EXPORT int64_t _olm_crypto_get_timestamp();

#ifdef __cplusplus
} // extern "C"
#endif

#endif /* OLM_CRYPTO_H_ */
