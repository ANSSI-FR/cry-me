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

#include "olm/outbound_group_session.h"

#include <string.h>

#include "olm/base64.h"
#include "olm/default_megolm_cipher.h"
#include "olm/crypto.h"
#include "olm/error.h"
#include "olm/megolm.h"
#include "olm/memory.h"
#include "olm/message.h"
#include "olm/pickle.h"
#include "olm/pickle_encoding.h"
#include "olm/olm.h"

#define OLM_PROTOCOL_VERSION     3
#define GROUP_SESSION_ID_LENGTH  WEI25519_SIGN_PUBLIC_KEY_LENGTH
#define PICKLE_VERSION           1
#define SESSION_KEY_VERSION      2

struct OlmOutboundGroupSession {
    /** the Megolm ratchet providing the encryption keys */
    Megolm ratchet;

    /** The weisig25519 keypair used for signing the messages */
    struct _olm_wei25519_sign_key_pair signing_key;

    enum OlmErrorCode last_error;
};


size_t olm_outbound_group_session_size(void) {
    return sizeof(OlmOutboundGroupSession);
}

OlmOutboundGroupSession * olm_outbound_group_session(
    void *memory
) {
    OlmOutboundGroupSession *session = memory;
    olm_clear_outbound_group_session(session);
    return session;
}

const char *olm_outbound_group_session_last_error(
    const OlmOutboundGroupSession *session
) {
    return _olm_error_to_string(session->last_error);
}

enum OlmErrorCode olm_outbound_group_session_last_error_code(
    const OlmOutboundGroupSession *session
) {
    return session->last_error;
}

size_t olm_clear_outbound_group_session(
    OlmOutboundGroupSession *session
) {
    _olm_unset(session, sizeof(OlmOutboundGroupSession));
    return sizeof(OlmOutboundGroupSession);
}

static size_t raw_pickle_length(
    const OlmOutboundGroupSession *session
) {
    size_t length = 0;
    length += _olm_pickle_uint32_length(PICKLE_VERSION);
    length += megolm_pickle_length(&(session->ratchet));
    length += _olm_pickle_wei25519_sign_key_pair_length(&(session->signing_key));
    return length;
}

size_t olm_pickle_outbound_group_session_length(
    const OlmOutboundGroupSession *session
) {
    return _olm_enc_output_length(raw_pickle_length(session));
}

size_t olm_pickle_outbound_group_session(
    OlmOutboundGroupSession *session,
    void const * key, size_t key_length,
    void * pickled, size_t pickled_length
) {
    size_t raw_length = raw_pickle_length(session);
    uint8_t *pos;

    if (pickled_length < _olm_enc_output_length(raw_length)) {
        session->last_error = OLM_OUTPUT_BUFFER_TOO_SMALL;
        return (size_t)-1;
    }

#ifndef OLM_FUZZING
    pos = _olm_enc_output_pos(pickled, raw_length);
#else
    pos = pickled;
#endif

    pos = _olm_pickle_uint32(pos, PICKLE_VERSION);
    pos = megolm_pickle(&(session->ratchet), pos);
    pos = _olm_pickle_wei25519_sign_key_pair(pos, &(session->signing_key));

#ifndef OLM_FUZZING
    return _olm_enc_output(key, key_length, pickled, raw_length);
#else
    return raw_length;
#endif
}

size_t olm_unpickle_outbound_group_session(
    OlmOutboundGroupSession *session,
    void const * key, size_t key_length,
    void * pickled, size_t pickled_length
) {
    const uint8_t *pos;
    const uint8_t *end;
    uint32_t pickle_version;

#ifndef OLM_FUZZING
    size_t raw_length = _olm_enc_input(
        key, key_length, pickled, pickled_length, &(session->last_error)
    );
#else
    size_t raw_length = pickled_length;
#endif

    if (raw_length == (size_t)-1) {
        return raw_length;
    }

    pos = pickled;
    end = pos + raw_length;

    pos = _olm_unpickle_uint32(pos, end, &pickle_version);
    FAIL_ON_CORRUPTED_PICKLE(pos, session);

    if (pickle_version != PICKLE_VERSION) {
        session->last_error = OLM_UNKNOWN_PICKLE_VERSION;
        return (size_t)-1;
    }

    pos = megolm_unpickle(&(session->ratchet), pos, end);
    FAIL_ON_CORRUPTED_PICKLE(pos, session);

    pos = _olm_unpickle_wei25519_sign_key_pair(pos, end, &(session->signing_key));
    FAIL_ON_CORRUPTED_PICKLE(pos, session);

    if (pos != end) {
        /* Input was longer than expected. */
        session->last_error = OLM_PICKLE_EXTRA_DATA;
        return (size_t)-1;
    }

    return pickled_length;
}


size_t olm_init_outbound_group_session_random_length(
    const OlmOutboundGroupSession *session
) {
    /* we need data to initialize the megolm ratchet, plus some more for the
     * session id.
     */
    return 2 * MEGOLM_RATCHET_PART_LENGTH +
        WEI25519_SIGN_RANDOM_LENGTH;
}

size_t olm_init_outbound_group_session(
    OlmOutboundGroupSession *session,
    uint8_t * random, size_t random_length,
    uint8_t const * account_private_identity_key, size_t account_private_identity_key_length
) {
    const uint8_t *random_ptr = random;

    if (random_length < olm_init_outbound_group_session_random_length(session)) {
        /* Insufficient random data for new session */
        session->last_error = OLM_NOT_ENOUGH_RANDOM;
        return (size_t)-1;
    }

    if((account_private_identity_key_length != _olm_encode_base64_length(WEI25519_PRIVATE_KEY_LENGTH)) ||
       (_olm_decode_base64_length(account_private_identity_key_length) != WEI25519_PRIVATE_KEY_LENGTH)) {
        session->last_error = OLM_INVALID_BASE64;
        return (size_t)-1;
    }

    uint8_t private_key[WEI25519_PRIVATE_KEY_LENGTH];
    _olm_decode_base64(account_private_identity_key, account_private_identity_key_length, private_key);

    int res = megolm_init(&(session->ratchet), random_ptr, 0, private_key, WEI25519_PRIVATE_KEY_LENGTH);
    if(res != EXIT_SUCCESS_CODE){
        if(res == MEMORY_ALLOCATION_ERROR){
            session->last_error = OLM_MEMORY_ERROR;
            return (size_t)-1;
        }
        else{
            session->last_error = OLM_KEY_DERIVATION_ERROR;
            return (size_t)-1;
        }
    }

    memset(private_key, 0, WEI25519_PRIVATE_KEY_LENGTH*sizeof(uint8_t));

    random_ptr += (2 * MEGOLM_RATCHET_PART_LENGTH);

    _olm_crypto_wei25519_sign_generate_key(random_ptr, &(session->signing_key));
    random_ptr += WEI25519_SIGN_RANDOM_LENGTH;

    _olm_unset(random, random_length);
    return 0;
}

static size_t raw_message_length(
    OlmOutboundGroupSession *session,
    size_t plaintext_length)
{
    size_t ciphertext_length, mac_length;

    ciphertext_length = megolm_cipher->ops->encrypt_ciphertext_length(
        megolm_cipher, plaintext_length
    );

    mac_length = megolm_cipher->ops->mac_length(megolm_cipher);

    return _olm_encode_group_message_length(
        session->ratchet.counter,
        ciphertext_length, mac_length, WEI25519_SIGNATURE_LENGTH
    );
}

size_t olm_group_encrypt_message_length(
    OlmOutboundGroupSession *session,
    size_t plaintext_length
) {
    size_t message_length = raw_message_length(session, plaintext_length);
    return _olm_encode_base64_length(message_length);
}

/** write an un-base64-ed message to the buffer */
static size_t _encrypt(
    OlmOutboundGroupSession *session, uint8_t const * plaintext, size_t plaintext_length,
    uint8_t * buffer, uint8_t const * random, size_t random_length
) {
    size_t ciphertext_length, mac_length, message_length;
    size_t result;
    uint8_t *ciphertext_ptr;
    uint8_t * session_id_ptr, * message_mac;

    if(random_length < WEI25519_SIGNATURE_RANDOM_LENGTH){
        session->last_error = OLM_NOT_ENOUGH_RANDOM;
        return (size_t)-1;
    }

    size_t session_id_length = olm_outbound_group_session_id_length(session);
    session_id_ptr = (uint8_t *)malloc(session_id_length * sizeof(uint8_t));
    if(!session_id_ptr){
        session->last_error = OLM_MEMORY_ERROR;
        return (size_t)-1;
    }
    olm_outbound_group_session_id(session, session_id_ptr, session_id_length);

    size_t message_mac_length = _olm_encode_group_message_length(
        session->ratchet.counter,
        plaintext_length, 0, 0
    );
    message_mac = (uint8_t *)malloc(
        message_mac_length *
        sizeof(uint8_t)
    );
    if(!message_mac){
        session->last_error = OLM_MEMORY_ERROR;
        return (size_t)-1;
    }
    uint8_t * message_writer;
    size_t m_length = _olm_encode_group_message(
        OLM_PROTOCOL_VERSION,
        session->ratchet.counter,
        plaintext_length,
        message_mac,
        &message_writer
    );
    if(m_length == 0){
        session->last_error = OLM_UNKNOWN_ERROR;
        return (size_t)-1;
    }
    memcpy(message_writer, plaintext, plaintext_length*sizeof(uint8_t));


    ciphertext_length = megolm_cipher->ops->encrypt_ciphertext_length(
        megolm_cipher,
        plaintext_length
    );
    mac_length = megolm_cipher->ops->mac_length(megolm_cipher);

    /* first we build the message structure, then we encrypt
     * the plaintext into it.
     */
    message_length = _olm_encode_group_message(
        OLM_PROTOCOL_VERSION,
        session->ratchet.counter,
        ciphertext_length,
        buffer,
        &ciphertext_ptr);

    message_length += mac_length;

    result = megolm_cipher->ops->encrypt(
        megolm_cipher,
        megolm_get_data(&(session->ratchet)), MEGOLM_RATCHET_LENGTH,
        plaintext, plaintext_length,
        ciphertext_ptr, ciphertext_length,
        buffer, message_length,
        session_id_ptr, session_id_length,
        message_mac, message_mac_length
    );

    if (result == (size_t)-1) {
        return result;
    }

    megolm_advance(&(session->ratchet));

    /* sign the whole thing with the weisig25519 key. */
    int res = _olm_crypto_wei25519_sign_sign(
        &(session->signing_key),
        buffer, message_length,
        random, random_length,
        buffer + message_length
    );

    if(res != EXIT_SUCCESS_CODE){
        session->last_error = OLM_SIGNATURE_ERROR;
        return (size_t)-1;
    }

    return result;
}

size_t olm_group_encrypt(
    OlmOutboundGroupSession *session,
    uint8_t const * plaintext, size_t plaintext_length,
    uint8_t * message, size_t max_message_length,
    uint8_t const * random, size_t random_length
) {
    size_t rawmsglen;
    size_t result;
    uint8_t *message_pos;

    rawmsglen = raw_message_length(session, plaintext_length);

    if (max_message_length < _olm_encode_base64_length(rawmsglen)) {
        session->last_error = OLM_OUTPUT_BUFFER_TOO_SMALL;
        return (size_t)-1;
    }

    /* we construct the message at the end of the buffer, so that
     * we have room to base64-encode it once we're done.
     */
    message_pos = message + _olm_encode_base64_length(rawmsglen) - rawmsglen;

    /* write the message, and encrypt it, at message_pos */
    result = _encrypt(session, plaintext, plaintext_length, message_pos, random, random_length);
    if (result == (size_t)-1) {
        return result;
    }

    /* bas64-encode it */
    return _olm_encode_base64(
        message_pos, rawmsglen, message
    );
}


size_t olm_outbound_group_session_id_length(
    const OlmOutboundGroupSession *session
) {
    return _olm_encode_base64_length(GROUP_SESSION_ID_LENGTH);
}

size_t olm_outbound_group_session_id(
    OlmOutboundGroupSession *session,
    uint8_t * id, size_t id_length
) {
    if (id_length < olm_outbound_group_session_id_length(session)) {
        session->last_error = OLM_OUTPUT_BUFFER_TOO_SMALL;
        return (size_t)-1;
    }

    return _olm_encode_base64(
        session->signing_key.public_key.public_key, GROUP_SESSION_ID_LENGTH, id
    );
}

uint32_t olm_outbound_group_session_message_index(
    OlmOutboundGroupSession *session
) {
    return session->ratchet.counter;
}

#define SESSION_KEY_RAW_LENGTH \
    (1 + 4 + MEGOLM_RATCHET_LENGTH + WEI25519_SIGN_PUBLIC_KEY_LENGTH\
        + WEI25519_SIGNATURE_LENGTH)

size_t olm_outbound_group_session_key_length(
    const OlmOutboundGroupSession *session
) {
    return _olm_encode_base64_length(SESSION_KEY_RAW_LENGTH);
}

size_t olm_outbound_group_session_ratchet_key_length(
    const OlmOutboundGroupSession *session
) {
    return _olm_encode_base64_length(MEGOLM_RATCHET_LENGTH);
}

size_t olm_outbound_group_session_ratchet_key(
    OlmOutboundGroupSession *session,
    uint8_t * key, size_t key_length
) {

    size_t encoded_length = olm_outbound_group_session_ratchet_key_length(session);

    if (key_length < encoded_length) {
        session->last_error = OLM_OUTPUT_BUFFER_TOO_SMALL;
        return (size_t)-1;
    }

    return _olm_encode_base64(megolm_get_data(&session->ratchet), MEGOLM_RATCHET_LENGTH, key);
}



size_t olm_outbound_group_session_key(
    OlmOutboundGroupSession *session,
    uint8_t * key, size_t key_length,
    uint8_t const * random, size_t random_length
) {
    uint8_t *raw;
    uint8_t *ptr;
    size_t encoded_length = olm_outbound_group_session_key_length(session);

    if(random_length < WEI25519_SIGNATURE_RANDOM_LENGTH){
        session->last_error = OLM_NOT_ENOUGH_RANDOM;
        return (size_t)-1;
    }

    if (key_length < encoded_length) {
        session->last_error = OLM_OUTPUT_BUFFER_TOO_SMALL;
        return (size_t)-1;
    }

    /* put the raw data at the end of the output buffer. */
    raw = ptr = key + encoded_length - SESSION_KEY_RAW_LENGTH;
    *ptr++ = SESSION_KEY_VERSION;

    uint32_t counter = session->ratchet.counter;
    // Encode counter as a big endian 32-bit number.
    for (unsigned i = 0; i < 4; i++) {
        *ptr++ = 0xFF & (counter >> 24); counter <<= 8;
    }

    memcpy(ptr, megolm_get_data(&session->ratchet), MEGOLM_RATCHET_LENGTH);
    ptr += MEGOLM_RATCHET_LENGTH;

    memcpy(
        ptr, session->signing_key.public_key.public_key,
        WEI25519_SIGN_PUBLIC_KEY_LENGTH
    );
    ptr += WEI25519_SIGN_PUBLIC_KEY_LENGTH;

    /* sign the whole thing with the weisig25519 key. */
    int res = _olm_crypto_wei25519_sign_sign(
        &(session->signing_key),
        raw, ptr - raw,
        random, random_length,
        ptr
    );
    if(res != EXIT_SUCCESS_CODE){
        session->last_error = OLM_SIGNATURE_ERROR;
        return (size_t)-1;
    }

    return _olm_encode_base64(raw, SESSION_KEY_RAW_LENGTH, key);
}
