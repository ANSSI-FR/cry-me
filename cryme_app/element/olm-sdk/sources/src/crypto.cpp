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
#include "olm/crypto.h"
#include "olm/memory.hh"

#include <cstring>

extern "C" {

#include "crypto-algorithms/sha/sha1.h"
#include "crypto-algorithms/sha/sha3.h"
#include "crypto-algorithms/sha/sha_struct.h"

#include "crypto-algorithms/hmac/hmac.h"

#include "crypto-algorithms/key_derivation/pbkdf2.h"
#include "crypto-algorithms/key_derivation/hkdf.h"

#include "crypto-algorithms/rsa/rsa.h"

#include "crypto-algorithms/ecc/ecc.h"

#include "crypto-algorithms/aes/aes.h"

#include "crypto-algorithms/timestamp/timestamp.h"

}

namespace {


} // namespace

void _olm_crypto_wei25519_generate_key(
    uint8_t const * random_bytes,
    struct _olm_wei25519_key_pair *key_pair
) {

    rnd_engine_t rnd_engine;
    rnd_engine.random_buffer_length = WEI25519_RANDOM_LENGTH;
    rnd_engine.random_buffer = random_bytes;
    rnd_engine.random_index = 0;

    int res = wei25519_keypair(key_pair->public_key.public_key, key_pair->private_key.private_key, &rnd_engine);
    if(res != 0){
        printf("KEY GEN ERROR !!! %d \n", res);
    }
}

OLM_EXPORT void _olm_crypto_wei25519_generate_key_from_private_key(
    uint8_t const * private_key,
    struct _olm_wei25519_key_pair *output
){
    std::memcpy(output->private_key.private_key, private_key, WEI25519_PRIVATE_KEY_LENGTH * sizeof(std::uint8_t));

    wei25519_key_from_private(output->public_key.public_key, output->private_key.private_key);

}

void _olm_crypto_wei25519_shared_secret(
    const struct _olm_wei25519_key_pair *our_key,
    const struct _olm_wei25519_public_key * their_key,
    std::uint8_t * output
) {

    int res = wei25519_key_exchange(output, their_key->public_key, our_key->private_key.private_key);
    if(res != 0){
        printf("ECDH ERROR !!! %d \n", res);
    }
}


void _olm_crypto_wei25519_sign_generate_key(
    std::uint8_t const * random_bytes,
    struct _olm_wei25519_sign_key_pair *key_pair
) {
    rnd_engine_t rnd_engine;
    rnd_engine.random_buffer_length = WEI25519_SIGN_RANDOM_LENGTH;
    rnd_engine.random_buffer = random_bytes;
    rnd_engine.random_index = 0;

    int res = wei25519_keypair(
        key_pair->public_key.public_key,
        key_pair->private_key.private_key,
        &rnd_engine
    );
    if(res != 0){
        printf("KEY GEN ERROR !!! %d \n", res);
    }
}

OLM_EXPORT void _olm_crypto_wei25519_sign_generate_key_from_private_key(
    uint8_t const * private_key,
    struct _olm_wei25519_sign_key_pair *output
){
    std::memcpy(output->private_key.private_key, private_key, WEI25519_SIGN_PRIVATE_KEY_LENGTH * sizeof(std::uint8_t));

    wei25519_key_from_private(output->public_key.public_key, output->private_key.private_key);
}


int _olm_crypto_wei25519_sign_sign(
    const struct _olm_wei25519_sign_key_pair *our_key,
    std::uint8_t const * message, std::size_t message_length,
    const uint8_t * random_bytes, size_t random_length,
    std::uint8_t * output
) {
    if(random_length < WEI25519_SIGNATURE_RANDOM_LENGTH){
        return EXIT_FAILURE_CODE;
    }

    rnd_engine_t rnd_engine;
    rnd_engine.random_buffer_length = WEI25519_SIGNATURE_RANDOM_LENGTH;
    rnd_engine.random_buffer = random_bytes;
    rnd_engine.random_index = 0;

    int res = wei25519_sign(
        output,
        message, message_length,
        our_key->private_key.private_key,
        &rnd_engine
    );
    if(res != 0){
        return EXIT_FAILURE_CODE;
    }

    return EXIT_SUCCESS_CODE;
}


int _olm_crypto_wei25519_sign_verify(
    const struct _olm_wei25519_sign_public_key *their_key,
    std::uint8_t const * message, std::size_t message_length,
    std::uint8_t const * signature
) {

    int res = wei25519_verify(
        signature,
        message, message_length, 
        their_key->public_key
    );
    if(res != 0){
        return EXIT_FAILURE_CODE;
    }

    return EXIT_SUCCESS_CODE;
}

OLM_EXPORT size_t _olm_crypto_wei25519_sign_signature_random_length(){
    return WEI25519_SIGNATURE_RANDOM_LENGTH;
}

/*************************************************
 ***************** SHA functions *****************
 *************************************************/

void _olm_crypto_sha1(
    std::uint8_t const * input, std::size_t input_length,
    std::uint8_t * output
) {
    ::compute_sha1(input, input_length, output);
}

void _olm_crypto_sha3(
    std::uint8_t const * input, std::size_t input_length,
    std::uint8_t * output
) {
    ::compute_sha3(input, input_length, output);
}


/*************************************************
 ***************** HMAC functions *****************
 *************************************************/


 void _olm_crypto_hmac_sha1(
    uint8_t const * key, size_t key_length,
    uint8_t const * input, size_t input_length,
    uint8_t * output
){
    ::compute_hmac(::use_sha1(), key, key_length, input, input_length, output);
}

 void _olm_crypto_hmac_sha3(
    uint8_t const * key, size_t key_length,
    uint8_t const * input, uint32_t input_length,
    uint8_t * output
){
    ::compute_hmac(::use_sha3(), key, key_length, input, input_length, output);
}

 int _olm_crypto_verif_hmac_sha1(
    uint8_t const * key, size_t key_length,
    uint8_t const * input, size_t input_length,
    uint8_t const * mac
){
    return 0 != ::verif_hmac(::use_sha1(), key, key_length, input, input_length, mac);
}

 int _olm_crypto_verif_hmac_sha3(
    uint8_t const * key, size_t key_length,
    uint8_t const * input, size_t input_length,
    uint8_t const * mac
){
    return 0 != ::verif_hmac(::use_sha3(), key, key_length, input, input_length, mac);
}


/*************************************************
 *********** KEY DERIVATION functions ************
 *************************************************/

int _olm_crypto_pbkdf2(const uint8_t *password, uint32_t password_length, 
                        const uint8_t *salt, uint32_t salt_length,
                        uint32_t c,
                        uint8_t * DK, uint32_t dklen){

    /**
     * CRY.ME.VULN.27
     *
     * First part of the vulnerability: number of iterations error,
     * here, we specify that the number of iterations is equal to 
     * DEFAULT_PBKDF2_ITERATIONS, which develops into 1 < 20
     */
    return ::compute_pbkdf2(::use_sha3(), ::get_sha3_digest_length(),
                    password, password_length,
                    salt, salt_length,
                    DEFAULT_PBKDF2_ITERATIONS,
                    DK, dklen);
    
}

int _olm_crypto_hkdf(const uint8_t *salt, uint32_t salt_length,
                    const uint8_t *ikm, uint32_t ikm_length, 
                    const uint8_t *info, uint32_t info_length, 
                    uint8_t * okm, uint32_t L){

    return ::compute_hkdf(::use_sha3(), ::get_sha3_digest_length(),
                    salt, salt_length,
                    ikm, ikm_length, 
                    info, info_length, 
                    okm, L);

}


/*************************************************
 **************** RSA functions ******************
 *************************************************/

int _olm_crypto_accreditation_verify(
    std::uint8_t const * n, std::uint8_t const * e,
    std::uint8_t const * url, std::size_t url_length,
    std::uint8_t const * accreditation, std::size_t accreditation_length
){
    return ::accreditation_verify(accreditation, accreditation_length,
                                url, url_length, n, e);
}


size_t _olm_crypto_get_randomness_size_RSA(){
    return ::get_random_buffer_size();
}


int _olm_crypto_genKey_RSA(
    uint8_t * n, uint8_t * p,
    uint8_t * q, uint8_t * lcm,
    uint8_t * random_buffer, size_t random_length
){
    int res = ::genKey_RSA(n, p, q, lcm, random_buffer, random_length);
    return res;
}


/*************************************************
 ************** ECC PRG functions ****************
 *************************************************/

/**
 * CRY.ME.VULN.22
 *
 * The pseudo-random generator functions (later 
 * used in crypto protocols) rely on the DUAL EC 
 * DRBG which is obsolete and contains a backdoor
 */

size_t _olm_crypto_prg_init(prg_t * prg, const uint8_t* entropy_input, size_t entropy_input_bytesize, const uint8_t* nounce, size_t nounce_bytesize){
    return ::wei25519_prg_init(prg, entropy_input, entropy_input_bytesize, nounce, nounce_bytesize);
}

uint32_t _olm_crypto_prg_entropy_size(){
    return ::wei25519_prg_entropy_size();
}

uint32_t _olm_crypto_prg_needs_reseeding(prg_t * prg, uint32_t nb_bytes){
    if(::wei25519_prg_needs_to_reseed(prg, nb_bytes)){
        return ::wei25519_prg_entropy_size();
    }

    return 0;
}

size_t _olm_crypto_prg_reseed(prg_t * prg, const uint8_t* entropy_input, size_t entropy_input_bytesize){
    return ::wei25519_prg_reseed(prg, entropy_input, entropy_input_bytesize);
}

size_t _olm_crypto_prg_sample(prg_t * prg, uint8_t* random_buffer, size_t random_buffer_size){
    return ::wei25519_prg_sample(prg, random_buffer, random_buffer_size);

}

/*************************************************
 **************** AES functions ******************
 *************************************************/

uint32_t _olm_crypto_aes_random_length(){
    return AES_IV_LENGTH;
}

 uint32_t _olm_crypto_aes_cbc_ciphertext_length(
    uint32_t message_length
){
    return ::aes256_cbc_output_length(message_length);
}

 void _olm_crypto_aes_cbc_encrypt(
    const uint8_t * message, uint32_t message_length, 
    const uint8_t * key, 
    const uint8_t * iv,
    uint8_t * ciphertext
){
    ::aes256_encrypt_cbc(message, message_length, ciphertext, key, iv);
}

int _olm_crypto_aes_cbc_decrypt(
    const std::uint8_t * ciphertext, uint32_t ciphertext_length, 
    const uint8_t * key, 
    const uint8_t * iv,
    std::uint8_t * message, std::uint32_t * message_length
){
    return ::aes256_decrypt_cbc(
        ciphertext, ciphertext_length,
        message, message_length,
        key, iv
    );
}


void _olm_crypto_aes_ctr_encrypt(
    const uint8_t * message, uint32_t message_length, 
    const uint8_t * key, 
    const uint8_t * iv,
    uint8_t * ciphertext
){
    ::aes256_encrypt_ctr(message, message_length, ciphertext, key, iv);
}

void _olm_crypto_aes_ctr_decrypt(
    const uint8_t * ciphertext, uint32_t ciphertext_length, 
    const uint8_t * key, 
    const uint8_t * iv,
    uint8_t * message
){
    ::aes256_decrypt_ctr(ciphertext, ciphertext_length, message, key, iv);
}

int _olm_crypto_aes_gcm_encrypt(
    const uint8_t * message, uint32_t message_length, 
    const uint8_t * additional_data, uint32_t additional_data_length, 
    const uint8_t * key,
    const uint8_t * iv,
    uint8_t * ciphertext,
    uint8_t * tag, uint32_t tag_length
){
    return ::aes256_encrypt_gcm(
        message, message_length,
        additional_data, additional_data_length,
        ciphertext, tag, tag_length,
        key, iv
    );
}

 int _olm_crypto_aes_gcm_decrypt(
    const uint8_t * ciphertext, uint32_t ciphertext_length, 
    const uint8_t * tag, uint32_t tag_length,
    const uint8_t * additional_data, uint32_t additional_data_length,
    const uint8_t * key, 
    const uint8_t * iv,
    uint8_t * message
){
    return ::aes256_decrypt_gcm(
        ciphertext, ciphertext_length,
        additional_data, additional_data_length,
        message, tag, tag_length,
        key, iv
    );
}


/*************************************************
 ************* TIMESTAMP functions ***************
 *************************************************/

int64_t _olm_crypto_get_timestamp(){
    return utc_time();
}