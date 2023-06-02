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

#ifndef UTILITY_HH_
#define UTILITY_HH_

#include "olm/error.h"

#include <cstddef>
#include <cstdint>

struct _olm_wei25519_sign_public_key;

namespace olm {

struct Utility {

    Utility();

    OlmErrorCode last_error;


    /*************************************************
     ***************** SHA functions *****************
     *************************************************/

    /** The length of a SHA-3 hash in bytes. */
    std::size_t sha3_size() const;

    /** Compute a SHA-3 hash. Returns the length of the SHA-3 hash in bytes
     * on success. Returns std::size_t(-1) on failure. On failure last_error
     * will be set with an error code. If the output buffer was too small then
     * last error will be OUTPUT_BUFFER_TOO_SMALL. */
    std::size_t sha3(
        std::uint8_t const * input, std::size_t input_length,
        std::uint8_t * output, std::size_t output_length
    );

    std::size_t sha_size();

        /** Compute a SHA hash. Returns the length of the SHA hash in bytes
     * on success. Returns std::size_t(-1) on failure. On failure last_error
     * will be set with an error code. If the output buffer was too small then
     * last error will be OUTPUT_BUFFER_TOO_SMALL. */
    std::size_t sha(
        std::uint8_t const * input, std::size_t input_length,
        std::uint8_t * output, std::size_t output_length
    );


    /*************************************************
     ***************** HMAC functions *****************
     *************************************************/

        /** Computes a HMAC SHA1 hash. Returns the length of the SHA hash in bytes
     * on success. Returns std::size_t(-1) on failure. On failure last_error
     * will be set with an error code. If the output buffer was too small then
     * last error will be OUTPUT_BUFFER_TOO_SMALL. */
    std::size_t hmac_sha1(
        std::uint8_t const * key, std::size_t key_length,
        std::uint8_t const * input, std::size_t input_length,
        std::uint8_t * output, std::size_t output_length
    );

    std::size_t verif_hmac_sha1(
        std::uint8_t const * key, std::size_t key_length,
        std::uint8_t const * input, std::size_t input_length,
        std::uint8_t const * mac, std::size_t mac_length
    );

        /** Computes a HMAC SHA3 hash. Returns the length of the SHA hash in bytes
     * on success. Returns std::size_t(-1) on failure. On failure last_error
     * will be set with an error code. If the output buffer was too small then
     * last error will be OUTPUT_BUFFER_TOO_SMALL. */
    
    std::size_t hmac_sha3(
        std::uint8_t const * key, std::size_t key_length,
        std::uint8_t const * input, std::size_t input_length,
        std::uint8_t * output, std::size_t output_length
    );

    std::size_t verif_hmac_sha3(
        std::uint8_t const * key, std::size_t key_length,
        std::uint8_t const * input, std::size_t input_length,
        std::uint8_t const * mac, std::size_t mac_length
    );


    /*************************************************
     *********** KEY DERIVATION functions ************
     *************************************************/

    /* * PBKDF2: Key Derivation mechanism from password
    * Computes key of dklen bytes (raw bytes) from password and salt using
    * PBKDF2 with c iterations. Returns the length of the derived key in bytes
    * on success. Returns std::size_t(-1) on failure. On failure last_error
    * will be set with an error code.
    * */
    std::size_t pbkdf2(
        std::uint8_t const *password, std::size_t password_length, 
        std::uint8_t const *salt, std::size_t salt_length,
        std::uint8_t * DK, std::uint32_t dklen, std::uint32_t c
    );

    /* * HKDF: Key Derivation mechanism from salt, ikm and info
    * Computes key of L bytes from salt, ikm and info using
    * HKDF. 
    * */
    std::size_t hkdf(
        std::uint8_t const *salt, std::uint32_t salt_length,
        std::uint8_t const *ikm, std::uint32_t ikm_length, 
        std::uint8_t const *info, std::uint32_t info_length, 
        std::uint8_t * okm, std::uint32_t L);


    /*************************************************
     **************** AES functions ******************
     *************************************************/

    std::uint32_t get_aes_random_length();

    std::size_t aes_ctr_encrypt(
        std::uint8_t const* message, std::uint32_t message_length, 
        std::uint8_t const* key, std::uint32_t key_length,
        std::uint8_t const* iv, std::uint32_t iv_length,
        std::uint8_t * ciphertext, std::uint32_t ciphertext_length
    );

    std::size_t aes_ctr_decrypt(
        std::uint8_t const* ciphertext, std::uint32_t ciphertext_length, 
        std::uint8_t const* key, std::uint32_t key_length,
        std::uint8_t const* iv, std::uint32_t iv_length,
        std::uint8_t * message, std::uint32_t message_length
    );



    /*************************************************
     *********** SECURE BACKUP functions *************
     *************************************************/

    std::size_t encrypt_backup_key(
        std::uint8_t const* backup_key, std::uint32_t backup_key_length,
        std::uint8_t const *ikm, std::uint32_t ikm_length,
        std::uint8_t const *info, std::uint32_t info_length,
        std::uint8_t const *iv, std::uint32_t iv_length,
        std::uint8_t * ciphertext, std::uint32_t ciphertext_length,
        std::uint8_t * mac, std::uint32_t mac_length
    );

    std::size_t decrypt_backup_key(
        std::uint8_t const * ciphertext, std::uint32_t ciphertext_length,
        std::uint8_t const * mac, std::uint32_t mac_length,
        std::uint8_t const * ikm, std::uint32_t ikm_length,
        std::uint8_t const * info, std::uint32_t info_length,
        std::uint8_t const * iv, std::uint32_t iv_length,
        std::uint8_t * backup_key, std::uint32_t backup_key_length
    );

    /** Verify a weisig25519 signature. Returns std::size_t(0) on success. Returns
     * std::size_t(-1) on failure or if the signature was invalid. On failure
     * last_error will be set with an error code. If the signature was too short
     * or was not a valid signature then last_error will be BAD_MESSAGE_MAC. */
    std::size_t weisig25519_verify(
        _olm_wei25519_sign_public_key const & key,
        std::uint8_t const * message, std::size_t message_length,
        std::uint8_t const * signature, std::size_t signature_length
    );

    /*************************************************
    ************* TIMESTAMP functions ****************
    *************************************************/

   std::int64_t get_timestamp();

};


} // namespace olm

#endif /* UTILITY_HH_ */
