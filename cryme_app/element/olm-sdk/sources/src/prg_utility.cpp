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

#include "olm/prg_utility.hh"


olm::PRGUtility::PRGUtility(
) : last_error(OlmErrorCode::OLM_SUCCESS) {
}

std::size_t olm::PRGUtility::prg_init(std::uint8_t * entropy, std::size_t entropy_size, std::uint8_t * nounce, std::size_t nounce_size){
    if(_olm_crypto_prg_init(&prg, entropy, entropy_size, nounce, nounce_size)){
        last_error = OlmErrorCode::OLM_PRG_INIT_ERROR;
        return std::size_t(-1);
    }

    return std::size_t(0);
}

std::uint32_t olm::PRGUtility::prg_entropy_size(){
    return _olm_crypto_prg_entropy_size();
}

std::uint32_t olm::PRGUtility::prg_needs_reseeding(std::uint32_t nb_bytes){
    return _olm_crypto_prg_needs_reseeding(&prg, nb_bytes);
}

std::size_t olm::PRGUtility::prg_reseed(std::uint8_t * entropy, std::size_t entropy_size){
    if(_olm_crypto_prg_reseed(&prg, entropy, entropy_size)){
        last_error = OlmErrorCode::OLM_PRG_RESEED_ERROR;
        return std::size_t(-1);
    }

    return std::size_t(0);
}

std::size_t olm::PRGUtility::get_random_bytes(std::uint8_t * arr, std::size_t arr_length){
    if(prg_needs_reseeding(arr_length)){
        last_error = OlmErrorCode::OLM_PRG_NEEDS_RESEED_ERROR;
        return std::size_t(-1);
    }

    if(_olm_crypto_prg_sample(&prg, arr, arr_length)){
        last_error = OlmErrorCode::OLM_PRG_SAMPLE_ERROR;
        return std::size_t(-1);
    }

    return std::size_t(0);
}