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

/* Copyright 2016 OpenMarket Ltd
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
#ifndef OLM_OUTBOUND_GROUP_SESSION_H_
#define OLM_OUTBOUND_GROUP_SESSION_H_

#include <stddef.h>
#include <stdint.h>

#include "olm/error.h"
#include "olm/olm_export.h"
#include "olm/olm.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct OlmOutboundGroupSession OlmOutboundGroupSession;

/** get the size of an outbound group session, in bytes. */
OLM_EXPORT size_t olm_outbound_group_session_size(void);

/**
 * Initialise an outbound group session object using the supplied memory
 * The supplied memory should be at least olm_outbound_group_session_size()
 * bytes.
 */
OLM_EXPORT OlmOutboundGroupSession * olm_outbound_group_session(
    void *memory
);

/**
 * A null terminated string describing the most recent error to happen to a
 * group session */
OLM_EXPORT const char *olm_outbound_group_session_last_error(
    const OlmOutboundGroupSession *session
);

/**
 * An error code describing the most recent error to happen to a group
 * session */
OLM_EXPORT enum OlmErrorCode olm_outbound_group_session_last_error_code(
    const OlmOutboundGroupSession *session
);

/** Clears the memory used to back this group session */
OLM_EXPORT size_t olm_clear_outbound_group_session(
    OlmOutboundGroupSession *session
);

/** Returns the number of bytes needed to store an outbound group session */
OLM_EXPORT size_t olm_pickle_outbound_group_session_length(
    const OlmOutboundGroupSession *session
);

/**
 * Stores a group session as a base64 string. Encrypts the session using the
 * supplied key. Returns the length of the session on success.
 *
 * Returns olm_error() on failure. If the pickle output buffer
 * is smaller than olm_pickle_outbound_group_session_length() then
 * olm_outbound_group_session_last_error() will be "OUTPUT_BUFFER_TOO_SMALL"
 */
OLM_EXPORT size_t olm_pickle_outbound_group_session(
    OlmOutboundGroupSession *session,
    void const * key, size_t key_length,
    void * pickled, size_t pickled_length
);

/**
 * Loads a group session from a pickled base64 string. Decrypts the session
 * using the supplied key.
 *
 * Returns olm_error() on failure. If the key doesn't match the one used to
 * encrypt the account then olm_outbound_group_session_last_error() will be
 * "BAD_ACCOUNT_KEY". If the base64 couldn't be decoded then
 * olm_outbound_group_session_last_error() will be "INVALID_BASE64". The input
 * pickled buffer is destroyed
 */
OLM_EXPORT size_t olm_unpickle_outbound_group_session(
    OlmOutboundGroupSession *session,
    void const * key, size_t key_length,
    void * pickled, size_t pickled_length
);


/** The number of random bytes needed to create an outbound group session */
OLM_EXPORT size_t olm_init_outbound_group_session_random_length(
    const OlmOutboundGroupSession *session
);

/**
 * Start a new outbound group session. Returns olm_error() on failure. On
 * failure last_error will be set with an error code. The last_error will be
 * NOT_ENOUGH_RANDOM if the number of random bytes was too small.
 */
OLM_EXPORT size_t olm_init_outbound_group_session(
    OlmOutboundGroupSession *session,
    uint8_t *random, size_t random_length,
    uint8_t const * account_private_identity_key, size_t account_private_identity_key_length
);

/**
 * The number of bytes that will be created by encrypting a message
 */
OLM_EXPORT size_t olm_group_encrypt_message_length(
    OlmOutboundGroupSession *session,
    size_t plaintext_length
);

/**
 * Encrypt some plain-text. Returns the length of the encrypted message or
 * olm_error() on failure. On failure last_error will be set with an
 * error code. The last_error will be OUTPUT_BUFFER_TOO_SMALL if the output
 * buffer is too small.
 */
OLM_EXPORT size_t olm_group_encrypt(
    OlmOutboundGroupSession *session,
    uint8_t const * plaintext, size_t plaintext_length,
    uint8_t * message, size_t message_length,
    uint8_t const * random, size_t random_length
);


/**
 * Get the number of bytes returned by olm_outbound_group_session_id()
 */
OLM_EXPORT size_t olm_outbound_group_session_id_length(
    const OlmOutboundGroupSession *session
);

/**
 * Get a base64-encoded identifier for this session.
 *
 * Returns the length of the session id on success or olm_error() on
 * failure. On failure last_error will be set with an error code. The
 * last_error will be OUTPUT_BUFFER_TOO_SMALL if the id buffer was too
 * small.
 */
OLM_EXPORT size_t olm_outbound_group_session_id(
    OlmOutboundGroupSession *session,
    uint8_t * id, size_t id_length
);

/**
 * Get the current message index for this session.
 *
 * Each message is sent with an increasing index; this returns the index for
 * the next message.
 */
OLM_EXPORT uint32_t olm_outbound_group_session_message_index(
    OlmOutboundGroupSession *session
);

/**
 * Get the number of bytes returned by olm_outbound_group_session_key()
 */
OLM_EXPORT size_t olm_outbound_group_session_key_length(
    const OlmOutboundGroupSession *session
);

OLM_EXPORT size_t olm_outbound_group_session_ratchet_key_length(
    const OlmOutboundGroupSession *session
);

/**
 * Get the base64-encoded current ratchet key for this session.
 *
 * Each message is sent with a different ratchet key. This function returns the
 * ratchet key that will be used for the next message.
 *
 * Returns the length of the ratchet key on success or olm_error() on
 * failure. On failure last_error will be set with an error code. The
 * last_error will be OUTPUT_BUFFER_TOO_SMALL if the buffer was too small.
 */
OLM_EXPORT size_t olm_outbound_group_session_key(
    OlmOutboundGroupSession *session,
    uint8_t * key, size_t key_length,
    uint8_t const * random, size_t random_length
);

OLM_EXPORT size_t olm_outbound_group_session_ratchet_key(
    OlmOutboundGroupSession *session,
    uint8_t * key, size_t key_length
);



#ifdef __cplusplus
} // extern "C"
#endif

#endif /* OLM_OUTBOUND_GROUP_SESSION_H_ */
