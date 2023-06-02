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

#include "olm/rsa_utility.hh"
#include "olm/crypto.h"


olm::RSAUtility::RSAUtility(
) : last_error(OlmErrorCode::OLM_SUCCESS) {
}

size_t olm::RSAUtility::accreditation_verify(
    std::uint8_t const * n, std::uint8_t const * e,
    std::uint8_t const * url, std::size_t url_length,
    std::uint8_t const * accreditation, std::size_t accreditation_length
){
    int res = _olm_crypto_accreditation_verify(n, e, url, url_length, accreditation, accreditation_length);
    if(res == MEMORY_ALLOCATION_ERROR){
        last_error = OlmErrorCode::OLM_MEMORY_ERROR;
        return std::size_t(-1);
    }
    else if(res == OUT_OF_RANDOMNESS_ERROR_CODE){
        last_error = OlmErrorCode::OLM_OUT_OF_RANDOMNESS;
        return std::size_t(-1);
    }
    else if (res == 0) {
        last_error = OlmErrorCode::OLM_BAD_SERVER_ACCREDITATION;
        return std::size_t(-1);
    }
    else if(res == 1){
        return std::size_t(0);
    }
    
    last_error = OlmErrorCode::OLM_UNKNOWN_ERROR;
    return std::size_t(-1);
}

std::size_t olm::RSAUtility::get_randomness_size(){
    return _olm_crypto_get_randomness_size_RSA();
}

std::size_t olm::RSAUtility::genKey_RSA(
        std::uint8_t * n, std::uint8_t * p,
        std::uint8_t * q, std::uint8_t * lcm, 
        std::uint8_t * random_buffer, std::size_t random_length
){
    int res = _olm_crypto_genKey_RSA(n, p, q, lcm, random_buffer, random_length);
    if (res == 0) {
        last_error = OlmErrorCode::OLM_BAD_RSA_KEY_GENERATION;
        return std::size_t(-1);
    }
    if(res == MEMORY_ALLOCATION_ERROR){
        last_error = OlmErrorCode::OLM_MEMORY_ERROR;
        return std::size_t(-1);
    }
    else if(res == OUT_OF_RANDOMNESS_ERROR_CODE){
        last_error = OlmErrorCode::OLM_OUT_OF_RANDOMNESS;
        return std::size_t(-1);
    }
    else if(res == 1){
        return std::size_t(0);
    }
    
    last_error = OlmErrorCode::OLM_UNKNOWN_ERROR;
    return std::size_t(-1);
}