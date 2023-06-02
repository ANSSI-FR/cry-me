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
#include "olm/pickle.hh"
#include "olm/pickle.h"

std::uint8_t * olm::pickle(
    std::uint8_t * pos,
    std::uint32_t value
) {
    pos += 4;
    for (unsigned i = 4; i--;) { *(--pos) = value; value >>= 8; }
    return pos + 4;
}


std::uint8_t const * olm::unpickle(
    std::uint8_t const * pos, std::uint8_t const * end,
    std::uint32_t & value
) {
    value = 0;
    if (!pos || end < pos + 4) return nullptr;
    for (unsigned i = 4; i--;) { value <<= 8; value |= *(pos++); }
    return pos;
}

std::uint8_t * olm::pickle(
    std::uint8_t * pos,
    std::uint8_t value
) {
    *(pos++) = value;
    return pos;
}

std::uint8_t const * olm::unpickle(
    std::uint8_t const * pos, std::uint8_t const * end,
    std::uint8_t & value
) {
    if (!pos || pos == end) return nullptr;
    value = *(pos++);
    return pos;
}

std::uint8_t * olm::pickle(
    std::uint8_t * pos,
    bool value
) {
    *(pos++) = value ? 1 : 0;
    return pos;
}

std::uint8_t const * olm::unpickle(
    std::uint8_t const * pos, std::uint8_t const * end,
    bool & value
) {
    if (!pos || pos == end) return nullptr;
    value = *(pos++);
    return pos;
}

std::uint8_t * olm::pickle_bytes(
    std::uint8_t * pos,
    std::uint8_t const * bytes, std::size_t bytes_length
) {
    std::memcpy(pos, bytes, bytes_length);
    return pos + bytes_length;
}

std::uint8_t const * olm::unpickle_bytes(
    std::uint8_t const * pos, std::uint8_t const * end,
    std::uint8_t * bytes, std::size_t bytes_length
) {
    if (!pos || end < pos + bytes_length) return nullptr;
    std::memcpy(bytes, pos, bytes_length);
    return pos + bytes_length;
}


std::size_t olm::pickle_length(
    const _olm_wei25519_public_key & value
) {
    return sizeof(value.public_key);
}


std::uint8_t * olm::pickle(
    std::uint8_t * pos,
    const _olm_wei25519_public_key & value
) {
    pos = olm::pickle_bytes(
        pos, value.public_key, sizeof(value.public_key)
    );
    return pos;
}


std::uint8_t const * olm::unpickle(
    std::uint8_t const * pos, std::uint8_t const * end,
    _olm_wei25519_public_key & value
) {
    return olm::unpickle_bytes(
        pos, end, value.public_key, sizeof(value.public_key)
    );
}


std::size_t olm::pickle_length(
    const _olm_wei25519_key_pair & value
) {
    return sizeof(value.public_key.public_key)
        + sizeof(value.private_key.private_key);
}


std::uint8_t * olm::pickle(
    std::uint8_t * pos,
    const _olm_wei25519_key_pair & value
) {
    pos = olm::pickle_bytes(
        pos, value.public_key.public_key,
        sizeof(value.public_key.public_key)
    );
    pos = olm::pickle_bytes(
        pos, value.private_key.private_key,
        sizeof(value.private_key.private_key)
    );
    return pos;
}


std::uint8_t const * olm::unpickle(
    std::uint8_t const * pos, std::uint8_t const * end,
    _olm_wei25519_key_pair & value
) {
    pos = olm::unpickle_bytes(
        pos, end, value.public_key.public_key,
        sizeof(value.public_key.public_key)
    );
    if (!pos) return nullptr;

    pos = olm::unpickle_bytes(
        pos, end, value.private_key.private_key,
        sizeof(value.private_key.private_key)
    );
    if (!pos) return nullptr;

    return pos;
}

////// pickle.h implementations

std::size_t _olm_pickle_weisig25519_public_key_length(
    const _olm_wei25519_sign_public_key * value
) {
    return sizeof(value->public_key);
}


std::uint8_t * _olm_pickle_weisig25519_public_key(
    std::uint8_t * pos,
    const _olm_wei25519_sign_public_key *value
) {
    return olm::pickle_bytes(
        pos, value->public_key, sizeof(value->public_key)
    );
}


std::uint8_t const * _olm_unpickle_weisig25519_public_key(
    std::uint8_t const * pos, std::uint8_t const * end,
    _olm_wei25519_sign_public_key * value
) {
    return olm::unpickle_bytes(
        pos, end, value->public_key, sizeof(value->public_key)
    );
}


std::size_t _olm_pickle_wei25519_sign_key_pair_length(
    const _olm_wei25519_sign_key_pair *value
) {
    return sizeof(value->public_key.public_key)
        + sizeof(value->private_key.private_key);
}


std::uint8_t * _olm_pickle_wei25519_sign_key_pair(
    std::uint8_t * pos,
    const _olm_wei25519_sign_key_pair *value
) {
    pos = olm::pickle_bytes(
        pos, value->public_key.public_key,
        sizeof(value->public_key.public_key)
    );
    pos = olm::pickle_bytes(
        pos, value->private_key.private_key,
        sizeof(value->private_key.private_key)
    );
    return pos;
}


std::uint8_t const * _olm_unpickle_wei25519_sign_key_pair(
    std::uint8_t const * pos, std::uint8_t const * end,
    _olm_wei25519_sign_key_pair *value
) {
    pos = olm::unpickle_bytes(
        pos, end, value->public_key.public_key,
        sizeof(value->public_key.public_key)
    );
    if (!pos) return nullptr;

    pos = olm::unpickle_bytes(
        pos, end, value->private_key.private_key,
        sizeof(value->private_key.private_key)
    );
    if (!pos) return nullptr;

    return pos;
}

uint8_t * _olm_pickle_uint32(uint8_t * pos, uint32_t value) {
    return olm::pickle(pos, value);
}

uint8_t const * _olm_unpickle_uint32(
    uint8_t const * pos, uint8_t const * end,
    uint32_t *value
) {
    return olm::unpickle(pos, end, *value);
}

uint8_t * _olm_pickle_uint8(uint8_t * pos, uint8_t value) {
    return olm::pickle(pos, value);
}

uint8_t const * _olm_unpickle_uint8(
    uint8_t const * pos, uint8_t const * end,
    uint8_t *value
) {
    return olm::unpickle(pos, end, *value);
}

uint8_t * _olm_pickle_bool(uint8_t * pos, int value) {
    return olm::pickle(pos, (bool)value);
}

uint8_t const * _olm_unpickle_bool(
    uint8_t const * pos, uint8_t const * end,
    int *value
) {
    return olm::unpickle(pos, end, *reinterpret_cast<bool *>(value));
}

uint8_t * _olm_pickle_bytes(uint8_t * pos, uint8_t const * bytes,
                           size_t bytes_length) {
    return olm::pickle_bytes(pos, bytes, bytes_length);
}

uint8_t const * _olm_unpickle_bytes(uint8_t const * pos, uint8_t const * end,
                                   uint8_t * bytes, size_t bytes_length) {
    return olm::unpickle_bytes(pos, end, bytes, bytes_length);
}
