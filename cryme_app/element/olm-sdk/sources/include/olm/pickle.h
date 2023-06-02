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

/* Copyright 2015-2016 OpenMarket Ltd
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
#ifndef OLM_PICKLE_H_
#define OLM_PICKLE_H_

#include <stddef.h>
#include <stdint.h>

/* Convenience macro for checking the return value of internal unpickling
 * functions and returning early on failure. */
#ifndef UNPICKLE_OK
#define UNPICKLE_OK(x) do { if (!(x)) return NULL; } while(0)
#endif

/* Convenience macro for failing on corrupted pickles from public
 * API unpickling functions. */
#define FAIL_ON_CORRUPTED_PICKLE(pos, session) \
    do { \
        if (!pos) { \
          session->last_error = OLM_CORRUPTED_PICKLE;  \
          return (size_t)-1; \
        } \
    } while(0)

#ifdef __cplusplus
extern "C" {
#endif

struct _olm_wei25519_sign_public_key;
struct _olm_wei25519_sign_key_pair;


#define _olm_pickle_uint32_length(value) 4
uint8_t * _olm_pickle_uint32(uint8_t * pos, uint32_t value);
uint8_t const * _olm_unpickle_uint32(
    uint8_t const * pos, uint8_t const * end,
    uint32_t *value
);


#define _olm_pickle_bool_length(value) 1
uint8_t * _olm_pickle_bool(uint8_t * pos, int value);
uint8_t const * _olm_unpickle_bool(
    uint8_t const * pos, uint8_t const * end,
    int *value
);

#define _olm_pickle_bytes_length(bytes, bytes_length) (bytes_length)
uint8_t * _olm_pickle_bytes(uint8_t * pos, uint8_t const * bytes,
                           size_t bytes_length);
uint8_t const * _olm_unpickle_bytes(uint8_t const * pos, uint8_t const * end,
                                   uint8_t * bytes, size_t bytes_length);


/** Get the number of bytes needed to pickle an weisig25519 public key */
size_t _olm_pickle_weisig25519_public_key_length(
    const struct _olm_wei25519_sign_public_key * value
);

/** Pickle the weisig25519 public key. Returns a pointer to the next free space in
 * the buffer. */
uint8_t * _olm_pickle_weisig25519_public_key(
    uint8_t *pos, const struct _olm_wei25519_sign_public_key * value
);

/** Unpickle the weisig25519 public key. Returns a pointer to the next item in the
 * buffer on success, NULL on error. */
const uint8_t * _olm_unpickle_weisig25519_public_key(
    const uint8_t *pos, const uint8_t *end,
    struct _olm_wei25519_sign_public_key * value
);

/** Get the number of bytes needed to pickle an weisig25519 key pair */
size_t _olm_pickle_wei25519_sign_key_pair_length(
    const struct _olm_wei25519_sign_key_pair * value
);

/** Pickle the weisig25519 key pair. Returns a pointer to the next free space in
 * the buffer. */
uint8_t * _olm_pickle_wei25519_sign_key_pair(
    uint8_t *pos, const struct _olm_wei25519_sign_key_pair * value
);

/** Unpickle the weisig25519 key pair. Returns a pointer to the next item in the
 * buffer on success, NULL on error. */
const uint8_t * _olm_unpickle_wei25519_sign_key_pair(
    const uint8_t *pos, const uint8_t *end,
    struct _olm_wei25519_sign_key_pair * value
);

#ifdef __cplusplus
} // extern "C"
#endif

#endif /* OLM_PICKLE_H */
