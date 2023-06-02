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

#include "olm/utility.hh"
#include "olm/crypto.h"




olm::Utility::Utility(
) : last_error(OlmErrorCode::OLM_SUCCESS) {
}


/*************************************************
 ***************** SHA functions *****************
 *************************************************/

size_t olm::Utility::sha3_size() const {
    return ::get_sha3_digest_length();
}


size_t olm::Utility::sha3(
    std::uint8_t const * input, std::size_t input_length,
    std::uint8_t * output, std::size_t output_length
) {
    if (output_length < sha3_size()) {
        last_error = OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }
    _olm_crypto_sha3(input, input_length, output);
    return ::get_sha3_digest_length();
}

size_t olm::Utility::sha_size(){
    return ::get_sha1_digest_length();
}

size_t olm::Utility::sha(
    std::uint8_t const * input, std::size_t input_length,
    std::uint8_t * output, std::size_t output_length
){
    if (output_length < ::get_sha1_digest_length()) {
        last_error = OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }
    _olm_crypto_sha1(input, input_length, output);
    return ::get_sha1_digest_length();
}

/*************************************************
***************** HMAC functions *****************
*************************************************/

std::size_t olm::Utility::hmac_sha1(
    std::uint8_t const * key, std::size_t key_length,
    std::uint8_t const * input, std::size_t input_length,
    std::uint8_t * output, std::size_t output_length
){
    if (output_length < ::get_sha1_digest_length()) {
        last_error = OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }
    _olm_crypto_hmac_sha1(key, key_length, input, input_length, output);
    return ::get_sha1_digest_length();
}


std::size_t olm::Utility::verif_hmac_sha1(
    std::uint8_t const * key, std::size_t key_length,
    std::uint8_t const * input, std::size_t input_length,
    std::uint8_t const * mac, std::size_t mac_length
){
    if (mac_length < ::get_sha1_digest_length()) {
        last_error = OlmErrorCode::OLM_INVALID_INPUT;
        return std::size_t(-1);
    }
    size_t res = _olm_crypto_verif_hmac_sha1(key, key_length, input, input_length, mac);
    if(res == 0){
        last_error = OlmErrorCode::OLM_BAD_MESSAGE_MAC;
        return std::size_t(-1);
    }

    return std::size_t(0);
}


std::size_t olm::Utility::hmac_sha3(
    std::uint8_t const * key, std::size_t key_length,
    std::uint8_t const * input, std::size_t input_length,
    std::uint8_t * output, std::size_t output_length
){
    if (output_length < ::get_sha3_digest_length()) {
        last_error = OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }
    _olm_crypto_hmac_sha3(key, key_length, input, input_length, output);
    return ::get_sha3_digest_length();
}

std::size_t olm::Utility::verif_hmac_sha3(
    std::uint8_t const * key, std::size_t key_length,
    std::uint8_t const * input, std::size_t input_length,
    std::uint8_t const * mac, std::size_t mac_length
){
    if (mac_length < ::get_sha3_digest_length()) {
        last_error = OlmErrorCode::OLM_INVALID_INPUT;
        return std::size_t(-1);
    }
    size_t res = _olm_crypto_verif_hmac_sha3(key, key_length, input, input_length, mac);
    if(res == 0){
        last_error = OlmErrorCode::OLM_BAD_MESSAGE_MAC;
        return std::size_t(-1);
    }

    return std::size_t(0);
}

/*************************************************
*********** KEY DERIVATION functions ************
*************************************************/

std::size_t olm::Utility::pbkdf2(
        std::uint8_t const *password, std::size_t password_length, 
        std::uint8_t const *salt, std::size_t salt_length,
        std::uint8_t * DK, std::uint32_t dklen, std::uint32_t c
){  
    int res = _olm_crypto_pbkdf2(password, password_length, salt, salt_length, c, DK, dklen);
    if(res == MEMORY_ALLOCATION_ERROR){
        last_error = OlmErrorCode::OLM_MEMORY_ERROR;
        return std::size_t(-1);
    }
    else if (res == 0) {
        last_error = OlmErrorCode::OLM_KEY_DERIVATION_ERROR;
        return std::size_t(-1);
    }
    else if(res == 1){
        return std::size_t(0);
    }
    
    last_error = OlmErrorCode::OLM_UNKNOWN_ERROR;
    return std::size_t(-1);
}

std::size_t olm::Utility::hkdf(
    std::uint8_t const *salt, std::uint32_t salt_length,
    std::uint8_t const *ikm, std::uint32_t ikm_length, 
    std::uint8_t const *info, std::uint32_t info_length, 
    std::uint8_t * okm, std::uint32_t L){

    int res = _olm_crypto_hkdf(salt, salt_length, ikm, ikm_length, info, info_length, okm, L);
    if(res == MEMORY_ALLOCATION_ERROR){
        last_error = OlmErrorCode::OLM_MEMORY_ERROR;
        return std::size_t(-1);
    }
    else if (res == 0) {
        last_error = OlmErrorCode::OLM_KEY_DERIVATION_ERROR;
        return std::size_t(-1);
    }
    else if(res == 1){
        return std::size_t(0);
    }
    
    last_error = OlmErrorCode::OLM_UNKNOWN_ERROR;
    return std::size_t(-1);

}


/*************************************************
**************** AES functions ******************
*************************************************/

std::uint32_t olm::Utility::get_aes_random_length(){
    return _olm_crypto_aes_random_length();
}

std::size_t olm::Utility::aes_ctr_encrypt(
    std::uint8_t const* message, std::uint32_t message_length, 
    std::uint8_t const* key, std::uint32_t key_length,
    std::uint8_t const* iv, std::uint32_t iv_length,
    std::uint8_t * ciphertext, std::uint32_t ciphertext_length
){
    if(key_length < AES_KEY_LENGTH){
        last_error = OlmErrorCode::OLM_INVALID_INPUT;
        return std::size_t(-1);
    }
    if(iv_length < ::_olm_crypto_aes_random_length()){
        last_error = OlmErrorCode::OLM_INVALID_INPUT;
        return std::size_t(-1);
    }
    if(ciphertext_length < message_length){
        last_error = OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }
    ::_olm_crypto_aes_ctr_encrypt(message, message_length, key, iv, ciphertext);
    
    return std::size_t(0);
}

std::size_t olm::Utility::aes_ctr_decrypt(
    std::uint8_t const* ciphertext, std::uint32_t ciphertext_length, 
    std::uint8_t const* key, std::uint32_t key_length,
    std::uint8_t const* iv, std::uint32_t iv_length,
    std::uint8_t * message, std::uint32_t message_length
){
    if(key_length < AES_KEY_LENGTH){
        last_error = OlmErrorCode::OLM_INVALID_INPUT;
        return std::size_t(-1);
    }
    if(iv_length < ::_olm_crypto_aes_random_length()){
        last_error = OlmErrorCode::OLM_INVALID_INPUT;
        return std::size_t(-1);
    }
    if(message_length < ciphertext_length){
        last_error = OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }
    ::_olm_crypto_aes_ctr_decrypt(ciphertext, ciphertext_length, key, iv, message);
    
    return std::size_t(0);
}


/*************************************************
*********** SECURE BACKUP functions *************
*************************************************/

std::size_t olm::Utility::encrypt_backup_key(
    std::uint8_t const* backup_key, std::uint32_t backup_key_length,
    std::uint8_t const *ikm, std::uint32_t ikm_length,
    std::uint8_t const *info, std::uint32_t info_length,
    std::uint8_t const *iv, std::uint32_t iv_length,
    std::uint8_t * ciphertext, std::uint32_t ciphertext_length,
    std::uint8_t * mac, std::uint32_t mac_length
){
    if(ciphertext_length < backup_key_length){
        last_error = OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }
    if(mac_length < ::get_sha3_digest_length()){
        last_error = OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }
    if(iv_length < get_aes_random_length()){
        last_error = OlmErrorCode::OLM_INVALID_INPUT;
        return std::size_t(-1);
    }

    std::uint8_t keys[AES_KEY_LENGTH + MAC_KEY_LENGTH_BACKUP];

    std::uint8_t salt[1] = {0};
    size_t res = hkdf(salt, 0, ikm, ikm_length, info, info_length, keys, AES_KEY_LENGTH + MAC_KEY_LENGTH_BACKUP);
    if(res != std::size_t(0)){
        return res;
    }

    res = aes_ctr_encrypt(
        backup_key, backup_key_length,
        keys, AES_KEY_LENGTH,
        iv, iv_length,
        ciphertext, ciphertext_length);

    if(res != std::size_t(0)){
        return res;
    }

    res = hmac_sha3(
        keys+AES_KEY_LENGTH, MAC_KEY_LENGTH_BACKUP,
        ciphertext, ciphertext_length,
        mac, mac_length);

    if(res != std::size_t(0)){
        return res;
    }

    return std::size_t(0);
}

std::size_t olm::Utility::decrypt_backup_key(
    std::uint8_t const * ciphertext, std::uint32_t ciphertext_length,
    std::uint8_t const * mac, std::uint32_t mac_length,
    std::uint8_t const * ikm, std::uint32_t ikm_length,
    std::uint8_t const * info, std::uint32_t info_length,
    std::uint8_t const * iv, std::uint32_t iv_length,
    std::uint8_t * backup_key, std::uint32_t backup_key_length
){
    if(backup_key_length < ciphertext_length){
        last_error = OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }
    if(mac_length < ::get_sha3_digest_length()){
        last_error = OlmErrorCode::OLM_INVALID_INPUT;
        return std::size_t(-1);
    }
    if(iv_length < get_aes_random_length()){
        last_error = OlmErrorCode::OLM_INVALID_INPUT;
        return std::size_t(-1);
    }

    std::uint8_t keys[AES_KEY_LENGTH + MAC_KEY_LENGTH_BACKUP];
    std::uint8_t salt[1] = {0};
    size_t res = hkdf(salt, 0, ikm, ikm_length, info, info_length, keys, AES_KEY_LENGTH + MAC_KEY_LENGTH_BACKUP);
    if(res != std::size_t(0)){
        return res;
    }

    res = verif_hmac_sha3(
        keys+AES_KEY_LENGTH, MAC_KEY_LENGTH_BACKUP,
        ciphertext, ciphertext_length,
        mac, mac_length
    );

    if(res != std::size_t(0)){
        return res;
    }

    res = aes_ctr_decrypt(
        ciphertext, ciphertext_length,
        keys, AES_KEY_LENGTH,
        iv, iv_length, 
        backup_key, backup_key_length
    );

    if(res != std::size_t(0)){
        return res;
    }

    return std::size_t(0);
}


size_t olm::Utility::weisig25519_verify(
    _olm_wei25519_sign_public_key const & key,
    std::uint8_t const * message, std::size_t message_length,
    std::uint8_t const * signature, std::size_t signature_length
) {
    if (signature_length < WEI25519_SIGNATURE_LENGTH) {
        last_error = OlmErrorCode::OLM_INVALID_INPUT;
        return std::size_t(-1);
    }
    if (!_olm_crypto_wei25519_sign_verify(&key, message, message_length, signature)) {
        last_error = OlmErrorCode::OLM_BAD_SIGNATURE;
        return std::size_t(-1);
    }
    return std::size_t(0);
}


/*************************************************
************* TIMESTAMP functions ****************
*************************************************/


int64_t olm::Utility::get_timestamp(){
    return _olm_crypto_get_timestamp();
}