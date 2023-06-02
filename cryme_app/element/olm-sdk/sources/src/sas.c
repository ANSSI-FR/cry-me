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

/* Copyright 2018-2019 New Vector Ltd
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

#include "olm/sas.h"
#include "olm/base64.h"
#include "olm/crypto.h"
#include "olm/error.h"
#include "olm/memory.h"

struct OlmSAS {
    enum OlmErrorCode last_error;
    struct _olm_wei25519_key_pair wei25519_key;
    uint8_t secret[WEI25519_SHARED_SECRET_LENGTH];
    int their_key_set;
};

const char * olm_sas_last_error(
    const OlmSAS * sas
) {
    return _olm_error_to_string(sas->last_error);
}

enum OlmErrorCode olm_sas_last_error_code(
    const OlmSAS * sas
) {
    return sas->last_error;
}

size_t olm_sas_size(void) {
    return sizeof(OlmSAS);
}

OlmSAS * olm_sas(
    void * memory
) {
    _olm_unset(memory, sizeof(OlmSAS));
    return (OlmSAS *) memory;
}

size_t olm_clear_sas(
    OlmSAS * sas
) {
    _olm_unset(sas, sizeof(OlmSAS));
    return sizeof(OlmSAS);
}

size_t olm_create_sas_random_length(const OlmSAS * sas) {
    return WEI25519_RANDOM_LENGTH;
}

size_t olm_create_sas(
    OlmSAS * sas,
    void * random, size_t random_length
) {
    if (random_length < olm_create_sas_random_length(sas)) {
        sas->last_error = OLM_NOT_ENOUGH_RANDOM;
        return (size_t)-1;
    }
    _olm_crypto_wei25519_generate_key((uint8_t *) random, &sas->wei25519_key);
    sas->their_key_set = 0;
    return 0;
}

size_t olm_sas_pubkey_length(const OlmSAS * sas) {
    return _olm_encode_base64_length(WEI25519_PUBLIC_KEY_LENGTH);
}

size_t olm_sas_get_pubkey(
    OlmSAS * sas,
    void * pubkey, size_t pubkey_length
) {
    if (pubkey_length < olm_sas_pubkey_length(sas)) {
        sas->last_error = OLM_OUTPUT_BUFFER_TOO_SMALL;
        return (size_t)-1;
    }
    _olm_encode_base64(
        (const uint8_t *)sas->wei25519_key.public_key.public_key,
        WEI25519_PUBLIC_KEY_LENGTH,
        (uint8_t *)pubkey
    );
    return 0;
}

size_t olm_sas_set_their_key(
    OlmSAS *sas,
    void * their_key, size_t their_key_length
) {
    if (their_key_length < olm_sas_pubkey_length(sas)) {
        sas->last_error = OLM_INPUT_BUFFER_TOO_SMALL;
        return (size_t)-1;
    }

    size_t ret = _olm_decode_base64(their_key, their_key_length, their_key);
    if (ret == (size_t)-1) {
        sas->last_error = OLM_INVALID_BASE64;
        return (size_t)-1;
    }

    _olm_crypto_wei25519_shared_secret(&sas->wei25519_key, their_key, sas->secret);
    sas->their_key_set = 1;
    return 0;
}

int olm_sas_is_their_key_set(
    const OlmSAS *sas
) {
    return sas->their_key_set;
}

size_t olm_sas_generate_bytes(
    OlmSAS * sas,
    const void * info, size_t info_length,
    void * output, size_t output_length
) {
    if (!sas->their_key_set) {
        sas->last_error = OLM_SAS_THEIR_KEY_NOT_SET;
        return (size_t)-1;
    }

    uint8_t salt[1] = {0};
    int res = _olm_crypto_hkdf(
        salt, 1,
        sas->secret, sizeof(sas->secret),
        (const uint8_t *)info, info_length,
        output, output_length
    );
    if(res != EXIT_SUCCESS_CODE){
        sas->last_error = OLM_KEY_DERIVATION_ERROR;
        return (size_t)-1;
    }

    return 0;
}

size_t olm_sas_mac_length(
    const OlmSAS *sas
) {
    return _olm_encode_base64_length(SHA3_DIGEST_SIZE);
}


size_t olm_sas_calculate_mac(
    OlmSAS * sas,
    const void * input, size_t input_length,
    const void * info, size_t info_length,
    void * mac, size_t mac_length
) {
    if (mac_length < olm_sas_mac_length(sas)) {
        sas->last_error = OLM_OUTPUT_BUFFER_TOO_SMALL;
        return (size_t)-1;
    }
    if (!sas->their_key_set) {
        sas->last_error = OLM_SAS_THEIR_KEY_NOT_SET;
        return (size_t)-1;
    }
    uint8_t key[32];
    uint8_t salt[1] = {0};
    int res = _olm_crypto_hkdf(
        salt, 1,
        sas->secret, sizeof(sas->secret),
        (const uint8_t *)info, info_length,
        key, 32
    );
    if(res != EXIT_SUCCESS_CODE){
        sas->last_error = OLM_KEY_DERIVATION_ERROR;
        return (size_t)-1;
    }
    
    _olm_crypto_hmac_sha3(
        key, 32,
        input, input_length,
        mac
    );

    _olm_encode_base64((const uint8_t *)mac, SHA3_DIGEST_SIZE, (uint8_t *)mac);
    return 0;
}