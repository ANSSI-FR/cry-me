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
#include "olm/olm.h"
#include "olm/session.hh"
#include "olm/account.hh"
#include "olm/pickle_encoding.h"
#include "olm/utility.hh"
#include "olm/rsa_utility.hh"
#include "olm/prg_utility.hh"
#include "olm/attachment_utility.hh"
#include "olm/base64.hh"
#include "olm/memory.hh"

#include <new>
#include <cstring>

namespace {

static OlmAccount * to_c(olm::Account * account) {
    return reinterpret_cast<OlmAccount *>(account);
}

static OlmSession * to_c(olm::Session * session) {
    return reinterpret_cast<OlmSession *>(session);
}

static OlmUtility * to_c(olm::Utility * utility) {
    return reinterpret_cast<OlmUtility *>(utility);
}

static OlmRSAUtility * to_c(olm::RSAUtility * rsautility) {
    return reinterpret_cast<OlmRSAUtility *>(rsautility);
}

static OlmPRGUtility * to_c(olm::PRGUtility * prgutility) {
    return reinterpret_cast<OlmPRGUtility *>(prgutility);
}

static OlmAttachmentUtility * to_c(olm::AttachmentUtility * attachmentutility) {
    return reinterpret_cast<OlmAttachmentUtility *>(attachmentutility);
}

static olm::Account * from_c(OlmAccount * account) {
    return reinterpret_cast<olm::Account *>(account);
}

static const olm::Account * from_c(OlmAccount const * account) {
    return reinterpret_cast<olm::Account const *>(account);
}

static olm::Session * from_c(OlmSession * session) {
    return reinterpret_cast<olm::Session *>(session);
}

static const olm::Session * from_c(OlmSession const * session) {
    return reinterpret_cast<const olm::Session *>(session);
}

static olm::Utility * from_c(OlmUtility * utility) {
    return reinterpret_cast<olm::Utility *>(utility);
}

static const olm::Utility * from_c(OlmUtility const * utility) {
    return reinterpret_cast<const olm::Utility *>(utility);
}

static olm::RSAUtility * from_c(OlmRSAUtility * rsautility) {
    return reinterpret_cast<olm::RSAUtility *>(rsautility);
}

static const olm::RSAUtility * from_c(OlmRSAUtility const * rsautility) {
    return reinterpret_cast<const olm::RSAUtility *>(rsautility);
}

static olm::PRGUtility * from_c(OlmPRGUtility * prgutility) {
    return reinterpret_cast<olm::PRGUtility *>(prgutility);
}

static const olm::PRGUtility * from_c(OlmPRGUtility const * prgutility) {
    return reinterpret_cast<const olm::PRGUtility *>(prgutility);
}

static olm::AttachmentUtility * from_c(OlmAttachmentUtility * attachmentutility) {
    return reinterpret_cast<olm::AttachmentUtility *>(attachmentutility);
}

static const olm::AttachmentUtility * from_c(OlmAttachmentUtility const * attachmentutility) {
    return reinterpret_cast<const olm::AttachmentUtility *>(attachmentutility);
}

static std::uint8_t * from_c(void * bytes) {
    return reinterpret_cast<std::uint8_t *>(bytes);
}

static std::uint8_t const * from_c(void const * bytes) {
    return reinterpret_cast<std::uint8_t const *>(bytes);
}

std::size_t b64_output_length(
    size_t raw_length
) {
    return olm::encode_base64_length(raw_length);
}

std::uint8_t * b64_output_pos(
    std::uint8_t * output,
    size_t raw_length
) {
    return output + olm::encode_base64_length(raw_length) - raw_length;
}

std::size_t b64_output(
    std::uint8_t * output, size_t raw_length
) {
    std::size_t base64_length = olm::encode_base64_length(raw_length);
    std::uint8_t * raw_output = output + base64_length - raw_length;
    olm::encode_base64(raw_output, raw_length, output);
    return base64_length;
}

std::size_t b64_input(
    std::uint8_t * input, size_t b64_length,
    OlmErrorCode & last_error
) {
    std::size_t raw_length = olm::decode_base64_length(b64_length);
    if (raw_length == std::size_t(-1)) {
        last_error = OlmErrorCode::OLM_INVALID_BASE64;
        return std::size_t(-1);
    }
    olm::decode_base64(input, b64_length, input);
    return raw_length;
}

} // namespace


extern "C" {

void olm_get_library_version(uint8_t *major, uint8_t *minor, uint8_t *patch) {
    if (major != NULL) *major = OLMLIB_VERSION_MAJOR;
    if (minor != NULL) *minor = OLMLIB_VERSION_MINOR;
    if (patch != NULL) *patch = OLMLIB_VERSION_PATCH;
}

size_t olm_error(void) {
    return std::size_t(-1);
}


const char * olm_account_last_error(
    const OlmAccount * account
) {
    auto error = from_c(account)->last_error;
    return _olm_error_to_string(error);
}

enum OlmErrorCode olm_account_last_error_code(
    const OlmAccount * account
) {
    return from_c(account)->last_error;
}

const char * olm_session_last_error(
    const OlmSession * session
) {
    auto error = from_c(session)->last_error;
    return _olm_error_to_string(error);
}

enum OlmErrorCode olm_session_last_error_code(
    OlmSession const * session
) {
    return from_c(session)->last_error;
}

const char * olm_utility_last_error(
    OlmUtility const * utility
) {
    auto error = from_c(utility)->last_error;
    return _olm_error_to_string(error);
}

enum OlmErrorCode olm_utility_last_error_code(
    OlmUtility const * utility
) {
    return from_c(utility)->last_error;
}

const char * olm_attachment_utility_last_error(
    OlmAttachmentUtility const * attachmentutility
) {
    auto error = from_c(attachmentutility)->last_error;
    return _olm_error_to_string(error);
}

enum OlmErrorCode olm_attachment_utility_last_error_code(
    OlmAttachmentUtility const * attachmentutility
) {
    return from_c(attachmentutility)->last_error;
}



const char * olm_rsa_utility_last_error(
    OlmRSAUtility const * rsautility
) {
    auto error = from_c(rsautility)->last_error;
    return _olm_error_to_string(error);
}

enum OlmErrorCode olm_rsa_utility_last_error_code(
    OlmRSAUtility const * rsautility
) {
    return from_c(rsautility)->last_error;
}


const char * olm_prg_utility_last_error(
    OlmPRGUtility const * prgutility
) {
    auto error = from_c(prgutility)->last_error;
    return _olm_error_to_string(error);
}

enum OlmErrorCode olm_prg_utility_last_error_code(
    OlmPRGUtility const * prgutility
) {
    return from_c(prgutility)->last_error;
}

size_t olm_account_size(void) {
    return sizeof(olm::Account);
}


size_t olm_session_size(void) {
    return sizeof(olm::Session);
}

size_t olm_utility_size(void) {
    return sizeof(olm::Utility);
}

size_t olm_rsa_utility_size(void) {
    return sizeof(olm::RSAUtility);
}

size_t olm_prg_utility_size(void) {
    return sizeof(olm::PRGUtility);
}

size_t olm_attachment_utility_size(void) {
    return sizeof(olm::AttachmentUtility);
}

OlmAccount * olm_account(
    void * memory
) {
    olm::unset(memory, sizeof(olm::Account));
    return to_c(new(memory) olm::Account());
}


OlmSession * olm_session(
    void * memory
) {
    olm::unset(memory, sizeof(olm::Session));
    return to_c(new(memory) olm::Session());
}


OlmUtility * olm_utility(
    void * memory
) {
    olm::unset(memory, sizeof(olm::Utility));
    return to_c(new(memory) olm::Utility());
}

OlmAttachmentUtility * olm_attachment_utility(
    void * memory
) {
    olm::unset(memory, sizeof(olm::AttachmentUtility));
    return to_c(new(memory) olm::AttachmentUtility());
}

OlmRSAUtility * olm_rsa_utility(
    void * memory
) {
    olm::unset(memory, sizeof(olm::RSAUtility));
    return to_c(new(memory) olm::RSAUtility());
}

OlmPRGUtility * olm_prg_utility(
    void * memory
) {
    olm::unset(memory, sizeof(olm::PRGUtility));
    return to_c(new(memory) olm::PRGUtility());
}

size_t olm_clear_account(
    OlmAccount * account
) {
    /* Clear the memory backing the account  */
    olm::unset(account, sizeof(olm::Account));
    /* Initialise a fresh account object in case someone tries to use it */
    new(account) olm::Account();
    return sizeof(olm::Account);
}


size_t olm_clear_session(
    OlmSession * session
) {
    /* Clear the memory backing the session */
    olm::unset(session, sizeof(olm::Session));
    /* Initialise a fresh session object in case someone tries to use it */
    new(session) olm::Session();
    return sizeof(olm::Session);
}


size_t olm_clear_utility(
    OlmUtility * utility
) {
    /* Clear the memory backing the session */
    olm::unset(utility, sizeof(olm::Utility));
    /* Initialise a fresh session object in case someone tries to use it */
    new(utility) olm::Utility();
    return sizeof(olm::Utility);
}


size_t olm_clear_attachment_utility(
    OlmAttachmentUtility * attachmentutility
) {
    /* Clear the memory backing the session */
    olm::unset(attachmentutility, sizeof(olm::AttachmentUtility));
    /* Initialise a fresh session object in case someone tries to use it */
    new(attachmentutility) olm::AttachmentUtility();
    return sizeof(olm::AttachmentUtility);
}


size_t olm_clear_rsa_utility(
    OlmRSAUtility * rsautility
) {
    /* Clear the memory backing the session */
    olm::unset(rsautility, sizeof(olm::RSAUtility));
    /* Initialise a fresh session object in case someone tries to use it */
    new(rsautility) olm::RSAUtility();
    return sizeof(olm::RSAUtility);
}

size_t olm_clear_prg_utility(
    OlmPRGUtility * prgutility
) {
    /* Clear the memory backing the session */
    olm::unset(prgutility, sizeof(olm::PRGUtility));
    /* Initialise a fresh session object in case someone tries to use it */
    new(prgutility) olm::PRGUtility();
    return sizeof(olm::PRGUtility);
}


size_t olm_pickle_account_length(
    OlmAccount const * account
) {
    return _olm_enc_output_length(pickle_length(*from_c(account)));
}


size_t olm_pickle_session_length(
    OlmSession const * session
) {
    return _olm_enc_output_length(pickle_length(*from_c(session)));
}


size_t olm_pickle_account(
    OlmAccount * account,
    void const * key, size_t key_length,
    void * pickled, size_t pickled_length
) {
    olm::Account & object = *from_c(account);
    std::size_t raw_length = pickle_length(object);
    if (pickled_length < _olm_enc_output_length(raw_length)) {
        object.last_error = OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return size_t(-1);
    }
    pickle(_olm_enc_output_pos(from_c(pickled), raw_length), object);
    return _olm_enc_output(from_c(key), key_length, from_c(pickled), raw_length);
}


size_t olm_pickle_session(
    OlmSession * session,
    void const * key, size_t key_length,
    void * pickled, size_t pickled_length
) {
    olm::Session & object = *from_c(session);
    std::size_t raw_length = pickle_length(object);
    if (pickled_length < _olm_enc_output_length(raw_length)) {
        object.last_error = OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return size_t(-1);
    }
    pickle(_olm_enc_output_pos(from_c(pickled), raw_length), object);
    return _olm_enc_output(from_c(key), key_length, from_c(pickled), raw_length);
}


size_t olm_unpickle_account(
    OlmAccount * account,
    void const * key, size_t key_length,
    void * pickled, size_t pickled_length
) {
    olm::Account & object = *from_c(account);
    std::uint8_t * input = from_c(pickled);
    std::size_t raw_length = _olm_enc_input(
        from_c(key), key_length, input, pickled_length, &object.last_error
    );
    if (raw_length == std::size_t(-1)) {
        return std::size_t(-1);
    }

    std::uint8_t const * pos = input;
    std::uint8_t const * end = pos + raw_length;

    pos = unpickle(pos, end, object);

    if (!pos) {
        /* Input was corrupted. */
        if (object.last_error == OlmErrorCode::OLM_SUCCESS) {
            object.last_error = OlmErrorCode::OLM_CORRUPTED_PICKLE;
        }
        return std::size_t(-1);
    } else if (pos != end) {
        /* Input was longer than expected. */
        object.last_error = OlmErrorCode::OLM_PICKLE_EXTRA_DATA;
        return std::size_t(-1);
    }

    return pickled_length;
}


size_t olm_unpickle_session(
    OlmSession * session,
    void const * key, size_t key_length,
    void * pickled, size_t pickled_length
) {
    olm::Session & object = *from_c(session);
    std::uint8_t * input = from_c(pickled);
    std::size_t raw_length = _olm_enc_input(
        from_c(key), key_length, input, pickled_length, &object.last_error
    );
    if (raw_length == std::size_t(-1)) {
        return std::size_t(-1);
    }

    std::uint8_t const * pos = input;
    std::uint8_t const * end = pos + raw_length;

    pos = unpickle(pos, end, object);

    if (!pos) {
        /* Input was corrupted. */
        if (object.last_error == OlmErrorCode::OLM_SUCCESS) {
            object.last_error = OlmErrorCode::OLM_CORRUPTED_PICKLE;
        }
        return std::size_t(-1);
    } else if (pos != end) {
        /* Input was longer than expected. */
        object.last_error = OlmErrorCode::OLM_PICKLE_EXTRA_DATA;
        return std::size_t(-1);
    }

    return pickled_length;
}


size_t olm_create_account_random_length(
    OlmAccount const * account
) {
    return from_c(account)->new_account_random_length();
}


size_t olm_create_account(
    OlmAccount * account,
    void * random, size_t random_length
) {
    size_t result = from_c(account)->new_account(from_c(random), random_length);
    olm::unset(random, random_length);
    return result;
}


size_t olm_account_identity_keys_length(
    OlmAccount const * account
) {
    return from_c(account)->get_identity_json_length();
}

size_t olm_account_identity_private_key_length(
    OlmAccount const * account
){
    return from_c(account)->get_identity_private_length();
}


size_t olm_account_identity_keys(
    OlmAccount * account,
    void * identity_keys, size_t identity_key_length
) {
    return from_c(account)->get_identity_json(
        from_c(identity_keys), identity_key_length
    );
}

size_t olm_account_identity_private_key(
    OlmAccount * account,
    void * identity_key, size_t identity_key_length
){
    return from_c(account)->get_identity_private(
        from_c(identity_key), identity_key_length
    );
}


size_t olm_account_signature_length(
    OlmAccount const * account
) {
    return b64_output_length(from_c(account)->signature_length());
}


size_t olm_account_sign(
    OlmAccount * account,
    void const * message, size_t message_length,
    void * signature, size_t signature_length,
    void const * random, size_t random_length
) {
    std::size_t raw_length = from_c(account)->signature_length();
    if (signature_length < b64_output_length(raw_length)) {
        from_c(account)->last_error =
            OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }
    from_c(account)->sign(
         from_c(message), message_length,
         b64_output_pos(from_c(signature), raw_length), raw_length,
         from_c(random), random_length
    );
    return b64_output(from_c(signature), raw_length);
}


size_t olm_account_one_time_keys_length(
    OlmAccount const * account
) {
    return from_c(account)->get_one_time_keys_json_length();
}


size_t olm_account_one_time_keys(
    OlmAccount * account,
    void * one_time_keys_json, size_t one_time_key_json_length
) {
    return from_c(account)->get_one_time_keys_json(
        from_c(one_time_keys_json), one_time_key_json_length
    );
}


size_t olm_account_mark_keys_as_published(
    OlmAccount * account
) {
    return from_c(account)->mark_keys_as_published();
}


size_t olm_account_max_number_of_one_time_keys(
    OlmAccount const * account
) {
    return from_c(account)->max_number_of_one_time_keys();
}


size_t olm_account_generate_one_time_keys_random_length(
    OlmAccount const * account,
    size_t number_of_keys
) {
    return from_c(account)->generate_one_time_keys_random_length(number_of_keys);
}


size_t olm_account_generate_one_time_keys(
    OlmAccount * account,
    size_t number_of_keys,
    void * random, size_t random_length
) {
    size_t result = from_c(account)->generate_one_time_keys(
        number_of_keys,
        from_c(random), random_length
    );
    olm::unset(random, random_length);
    return result;
}


size_t olm_account_generate_fallback_key_random_length(
    OlmAccount const * account
) {
    return from_c(account)->generate_fallback_key_random_length();
}


size_t olm_account_generate_fallback_key(
    OlmAccount * account,
    void * random, size_t random_length
) {
    size_t result = from_c(account)->generate_fallback_key(
        from_c(random), random_length
    );
    olm::unset(random, random_length);
    return result;
}


size_t olm_account_fallback_key_length(
    OlmAccount const * account
) {
    return from_c(account)->get_fallback_key_json_length();
}


size_t olm_account_fallback_key(
    OlmAccount * account,
    void * fallback_key_json, size_t fallback_key_json_length
) {
    return from_c(account)->get_fallback_key_json(
        from_c(fallback_key_json), fallback_key_json_length
    );
}


size_t olm_account_unpublished_fallback_key_length(
    OlmAccount const * account
) {
    return from_c(account)->get_unpublished_fallback_key_json_length();
}


size_t olm_account_unpublished_fallback_key(
    OlmAccount * account,
    void * fallback_key_json, size_t fallback_key_json_length
) {
    return from_c(account)->get_unpublished_fallback_key_json(
        from_c(fallback_key_json), fallback_key_json_length
    );
}


void olm_account_forget_old_fallback_key(
    OlmAccount * account
) {
    return from_c(account)->forget_old_fallback_key();
}


size_t olm_create_outbound_session_random_length(
    OlmSession const * session
) {
    return from_c(session)->new_outbound_session_random_length();
}


size_t olm_create_outbound_session(
    OlmSession * session,
    OlmAccount const * account,
    void const * their_identity_key, size_t their_identity_key_length,
    void const * their_one_time_key, size_t their_one_time_key_length,
    void * random, size_t random_length
) {
    std::uint8_t const * id_key = from_c(their_identity_key);
    std::uint8_t const * ot_key = from_c(their_one_time_key);
    std::size_t id_key_length = their_identity_key_length;
    std::size_t ot_key_length = their_one_time_key_length;

    if (olm::decode_base64_length(id_key_length) != WEI25519_PUBLIC_KEY_LENGTH
            || olm::decode_base64_length(ot_key_length) != WEI25519_PUBLIC_KEY_LENGTH
    ) {
        from_c(session)->last_error = OlmErrorCode::OLM_INVALID_BASE64;
        return std::size_t(-1);
    }
    _olm_wei25519_public_key identity_key;
    _olm_wei25519_public_key one_time_key;

    olm::decode_base64(id_key, id_key_length, identity_key.public_key);
    olm::decode_base64(ot_key, ot_key_length, one_time_key.public_key);

    size_t result = from_c(session)->new_outbound_session(
        *from_c(account), identity_key, one_time_key,
        from_c(random), random_length
    );
    olm::unset(random, random_length);
    return result;
}


size_t olm_create_inbound_session(
    OlmSession * session,
    OlmAccount * account,
    void * one_time_key_message, size_t message_length
) {
    std::size_t raw_length = b64_input(
        from_c(one_time_key_message), message_length, from_c(session)->last_error
    );
    if (raw_length == std::size_t(-1)) {
        return std::size_t(-1);
    }
    return from_c(session)->new_inbound_session(
        *from_c(account), nullptr, from_c(one_time_key_message), raw_length
    );
}


size_t olm_create_inbound_session_from(
    OlmSession * session,
    OlmAccount * account,
    void const * their_identity_key, size_t their_identity_key_length,
    void * one_time_key_message, size_t message_length
) {
    std::uint8_t const * id_key = from_c(their_identity_key);
    std::size_t id_key_length = their_identity_key_length;

    if (olm::decode_base64_length(id_key_length) != WEI25519_PUBLIC_KEY_LENGTH) {
        from_c(session)->last_error = OlmErrorCode::OLM_INVALID_BASE64;
        return std::size_t(-1);
    }
    _olm_wei25519_public_key identity_key;
    olm::decode_base64(id_key, id_key_length, identity_key.public_key);

    std::size_t raw_length = b64_input(
        from_c(one_time_key_message), message_length, from_c(session)->last_error
    );
    if (raw_length == std::size_t(-1)) {
        return std::size_t(-1);
    }
    return from_c(session)->new_inbound_session(
        *from_c(account), &identity_key,
        from_c(one_time_key_message), raw_length
    );
}


size_t olm_session_id_length(
    OlmSession const * session
) {
    return b64_output_length(from_c(session)->session_id_length());
}

size_t olm_session_id(
    OlmSession * session,
    void * id, size_t id_length
) {
    std::size_t raw_length = from_c(session)->session_id_length();
    if (id_length < b64_output_length(raw_length)) {
        from_c(session)->last_error =
                OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }
    std::size_t result = from_c(session)->session_id(
       b64_output_pos(from_c(id), raw_length), raw_length
    );
    if (result == std::size_t(-1)) {
        return result;
    }
    return b64_output(from_c(id), raw_length);
}


int olm_session_has_received_message(
    OlmSession const * session
) {
    return from_c(session)->received_message;
}

void olm_session_describe(
    OlmSession * session, char *buf, size_t buflen
) {
    from_c(session)->describe(buf, buflen);
}

size_t olm_matches_inbound_session(
    OlmSession * session,
    void * one_time_key_message, size_t message_length
) {
    std::size_t raw_length = b64_input(
        from_c(one_time_key_message), message_length, from_c(session)->last_error
    );
    if (raw_length == std::size_t(-1)) {
        return std::size_t(-1);
    }
    bool matches = from_c(session)->matches_inbound_session(
        nullptr, from_c(one_time_key_message), raw_length
    );
    return matches ? 1 : 0;
}


size_t olm_matches_inbound_session_from(
    OlmSession * session,
    void const * their_identity_key, size_t their_identity_key_length,
    void * one_time_key_message, size_t message_length
) {
    std::uint8_t const * id_key = from_c(their_identity_key);
    std::size_t id_key_length = their_identity_key_length;

    if (olm::decode_base64_length(id_key_length) != WEI25519_PUBLIC_KEY_LENGTH) {
        from_c(session)->last_error = OlmErrorCode::OLM_INVALID_BASE64;
        return std::size_t(-1);
    }
    _olm_wei25519_public_key identity_key;
    olm::decode_base64(id_key, id_key_length, identity_key.public_key);

    std::size_t raw_length = b64_input(
        from_c(one_time_key_message), message_length, from_c(session)->last_error
    );
    if (raw_length == std::size_t(-1)) {
        return std::size_t(-1);
    }
    bool matches = from_c(session)->matches_inbound_session(
        &identity_key, from_c(one_time_key_message), raw_length
    );
    return matches ? 1 : 0;
}


size_t olm_remove_one_time_keys(
    OlmAccount * account,
    OlmSession * session
) {
    size_t result = from_c(account)->remove_key(
        from_c(session)->bob_one_time_key
    );
    if (result == std::size_t(-1)) {
        from_c(account)->last_error = OlmErrorCode::OLM_BAD_MESSAGE_KEY_ID;
    }
    return result;
}


size_t olm_encrypt_message_type(
    OlmSession const * session
) {
    return size_t(from_c(session)->encrypt_message_type());
}


size_t olm_encrypt_random_length(
    OlmSession const * session
) {
    return from_c(session)->encrypt_random_length();
}


size_t olm_encrypt_message_length(
    OlmSession const * session,
    size_t plaintext_length
) {
    return b64_output_length(
        from_c(session)->encrypt_message_length(plaintext_length)
    );
}

size_t olm_get_sender_chain_size(
    OlmSession * session
){
    return from_c(session)->get_sender_chain_size();
}


size_t olm_encrypt(
    OlmSession * session,
    void const * plaintext, size_t plaintext_length,
    void * random, size_t random_length,
    void * message, size_t message_length
) {
    std::size_t raw_length = from_c(session)->encrypt_message_length(
        plaintext_length
    );
    if (message_length < b64_output_length(raw_length)) {
        from_c(session)->last_error =
            OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }
    std::size_t result = from_c(session)->encrypt(
        from_c(plaintext), plaintext_length,
        from_c(random), random_length,
        b64_output_pos(from_c(message), raw_length), raw_length
    );
    olm::unset(random, random_length);
    if (result == std::size_t(-1)) {
        return result;
    }
    return b64_output(from_c(message), raw_length);
}


size_t olm_decrypt_max_plaintext_length(
    OlmSession * session,
    size_t message_type,
    void * message, size_t message_length
) {
    std::size_t raw_length = b64_input(
        from_c(message), message_length, from_c(session)->last_error
    );
    if (raw_length == std::size_t(-1)) {
        return std::size_t(-1);
    }
    return from_c(session)->decrypt_max_plaintext_length(
        olm::MessageType(message_type), from_c(message), raw_length
    );
}


size_t olm_decrypt(
    OlmSession * session,
    size_t message_type,
    void * message, size_t message_length,
    void * plaintext, size_t max_plaintext_length
) {
    std::size_t raw_length = b64_input(
        from_c(message), message_length, from_c(session)->last_error
    );
    if (raw_length == std::size_t(-1)) {
        return std::size_t(-1);
    }
    return from_c(session)->decrypt(
        olm::MessageType(message_type), from_c(message), raw_length,
        from_c(plaintext), max_plaintext_length
    );
}


size_t olm_weisig25519_verify(
    OlmUtility * utility,
    void const * key, size_t key_length,
    void const * message, size_t message_length,
    void * signature, size_t signature_length
) {
    if (olm::decode_base64_length(key_length) != WEI25519_SIGN_PUBLIC_KEY_LENGTH) {
        from_c(utility)->last_error = OlmErrorCode::OLM_INVALID_BASE64;
        return std::size_t(-1);
    }
    _olm_wei25519_sign_public_key verify_key;
    olm::decode_base64(from_c(key), key_length, verify_key.public_key);
    std::size_t raw_signature_length = b64_input(
        from_c(signature), signature_length, from_c(utility)->last_error
    );
    if (raw_signature_length == std::size_t(-1)) {
        return std::size_t(-1);
    }
    return from_c(utility)->weisig25519_verify(
        verify_key,
        from_c(message), message_length,
        from_c(signature), raw_signature_length
    );
}

/*************************************************
 **************** SHA functions ******************
 *************************************************/

size_t olm_sha3_length(
   OlmUtility * utility
){
    return from_c(utility)->sha3_size();
}

size_t olm_sha3(
    OlmUtility * utility,
    void const * input, size_t input_length,
    void * output, size_t output_length
){
    return from_c(utility)->sha3(
       from_c(input), input_length,
       from_c(output), output_length
    );
}

size_t olm_sha_length(
   OlmUtility * utility
){
    return from_c(utility)->sha_size();
}

size_t olm_sha(
    OlmUtility * utility,
    void const * input, size_t input_length,
    void * output, size_t output_length
){
    return from_c(utility)->sha(
       from_c(input), input_length,
       from_c(output), output_length
    );
}

/*************************************************
 ***************** HMAC functions *****************
 *************************************************/

size_t olm_hmac_sha1(
    OlmUtility * utility,
    void const * key, size_t key_length,
    void const * input, size_t input_length,
    void * output, size_t output_length
){
    return from_c(utility)->hmac_sha1(
        from_c(key), key_length,
        from_c(input), input_length,
        from_c(output), output_length
    );
}

size_t olm_verif_hmac_sha1(
    OlmUtility * utility,
    void const * key, size_t key_length,
    void const * input, size_t input_length,
    void * mac, size_t mac_length
){
    return from_c(utility)->verif_hmac_sha1(
        from_c(key), key_length,
        from_c(input), input_length,
        from_c(mac), mac_length
    );
}

size_t olm_hmac_sha3(
    OlmUtility * utility,
    void const * key, size_t key_length,
    void const * input, size_t input_length,
    void * output, size_t output_length
){
    return from_c(utility)->hmac_sha3(
        from_c(key), key_length,
        from_c(input), input_length,
        from_c(output), output_length
    );
}

size_t olm_verif_hmac_sha3(
    OlmUtility * utility,
    void const * key, size_t key_length,
    void const * input, size_t input_length,
    void * mac, size_t mac_length
){
    return from_c(utility)->verif_hmac_sha3(
        from_c(key), key_length,
        from_c(input), input_length,
        from_c(mac), mac_length
    );
}

/*************************************************
 *********** KEY DERIVATION functions ************
 *************************************************/

size_t olm_pbkdf2(
    OlmUtility * utility,
    void const *password, size_t password_length, 
    void const *salt, size_t salt_length,
    void * DK, uint32_t dklen, uint32_t c
) {
    std::size_t result = from_c(utility)->pbkdf2(
        from_c(password), password_length,
        from_c(salt), salt_length,
        from_c(DK), dklen, c
    );
    return result;
}

size_t _olm_hkdf(
    OlmUtility * utility,
    void const *salt, uint32_t salt_length,
    void const *ikm, uint32_t ikm_length, 
    void const *info, uint32_t info_length, 
    void * okm, uint32_t L){

    size_t result = from_c(utility)->hkdf(
        from_c(salt), salt_length,
        from_c(ikm), ikm_length,
        from_c(info), info_length,
        from_c(okm), L
    );
    return result;
}


/*************************************************
 **************** RSA functions ******************
 *************************************************/

size_t olm_verify_accreditation(
    OlmRSAUtility * rsautility,
    void const * n, size_t n_length,
    void const * e, size_t e_length,
    void const * url, size_t url_length,
    void * accreditation, size_t accreditation_length
){

    if (n_length != RSA_ACCREDITATION_MOD_LENGTH) {
        from_c(rsautility)->last_error = OlmErrorCode::OLM_INVALID_INPUT;
        return std::size_t(-1);
    }

    if (e_length != RSA_PUBLIC_EXPONENT_LENGTH) {
        from_c(rsautility)->last_error = OlmErrorCode::OLM_INVALID_INPUT;
        return std::size_t(-1);
    }
    
    return from_c(rsautility)->accreditation_verify(
                            from_c(n), from_c(e), 
                            from_c(url), url_length, 
                            from_c(accreditation), accreditation_length);
}

size_t olm_get_randomness_size_RSA(OlmRSAUtility * rsautility){
    return from_c(rsautility)->get_randomness_size();
}

size_t olm_genKey_RSA(
    OlmRSAUtility * rsautility,
    void * n, size_t n_length,
    void * p, size_t p_length,
    void * q, size_t q_length,
    void * lcm, size_t lcm_length,
    void * random_buffer, size_t random_length
){

    if ((n_length != RSA_2048_MOD_LENGTH) ||
        (p_length != RSA_2048_PRIME_LENGTH) ||
        (q_length != RSA_2048_PRIME_LENGTH) ||
        (lcm_length != RSA_2048_MOD_LENGTH)) {
        from_c(rsautility)->last_error = OlmErrorCode::OLM_BAD_OUTPUT_BUFFER_SIZE;
        return std::size_t(-1);
    }

    return from_c(rsautility)->genKey_RSA(from_c(n), from_c(p), from_c(q), from_c(lcm), from_c(random_buffer), random_length);
}


/*************************************************
 ************** ECC PRG functions ****************
 *************************************************/

size_t _olm_prg_init(
    OlmPRGUtility * prgutility,
    void * entropy, std::size_t entropy_size,
    void * nounce, std::size_t nounce_size
){
    if(entropy_size != from_c(prgutility)->prg_entropy_size()){
        from_c(prgutility)->last_error = OlmErrorCode::OLM_INVALID_INPUT;
        return std::size_t(-1);
    }

    //return std::size_t(0);

    return from_c(prgutility)->prg_init(from_c(entropy), entropy_size, from_c(nounce), nounce_size);
}

uint32_t _olm_prg_entropy_size(
    OlmPRGUtility * prgutility
){
    return from_c(prgutility)->prg_entropy_size();
}

uint32_t _olm_prg_needs_reseeding(OlmPRGUtility * prgutility, uint32_t nb_bytes){
    return from_c(prgutility)->prg_needs_reseeding(nb_bytes);
}

size_t _olm_prg_reseed(
    OlmPRGUtility * prgutility,
    void * entropy, std::size_t entropy_size
){
    if(entropy_size != from_c(prgutility)->prg_entropy_size()){
        from_c(prgutility)->last_error = OlmErrorCode::OLM_INVALID_INPUT;
        return std::size_t(-1);
    }

    return from_c(prgutility)->prg_reseed(from_c(entropy), entropy_size);
}

size_t _olm_prg_get_random(
    OlmPRGUtility * prgutility,
    void * buffer, std::size_t buffer_size
){
    return from_c(prgutility)->get_random_bytes(from_c(buffer), buffer_size);
}

OLM_EXPORT size_t _olm_signature_random_length(){
    return _olm_crypto_wei25519_sign_signature_random_length();
}


/*************************************************
 **************** AES functions ******************
 *************************************************/

size_t olm_aes_random_length(
   OlmUtility * utility
){
    return from_c(utility)->get_aes_random_length();
}

/*************************************************
 *********** SECURE BACKUP functions *************
 *************************************************/

size_t olm_encrypt_backup_key(
    OlmUtility * utility,
    void const* backup_key, uint32_t backup_key_length,
    void const *ikm, uint32_t ikm_length,
    void const *info, uint32_t info_length,
    void const *iv, uint32_t iv_length,
    void * ciphertext, uint32_t ciphertext_length,
    void * mac, uint32_t mac_length
){
    return from_c(utility)->encrypt_backup_key(
        from_c(backup_key), backup_key_length,
        from_c(ikm), ikm_length,
        from_c(info), info_length,
        from_c(iv), iv_length,
        from_c(ciphertext), ciphertext_length,
        from_c(mac), mac_length
    );
}


size_t olm_decrypt_backup_key(
    OlmUtility * utility,
    void const * ciphertext, uint32_t ciphertext_length,
    void const * mac, uint32_t mac_length,
    void const *ikm, uint32_t ikm_length,
    void const *info, uint32_t info_length,
    void const *iv, uint32_t iv_length,
    void * backup_key, uint32_t backup_key_length
){
    return from_c(utility)->decrypt_backup_key(
        from_c(ciphertext), ciphertext_length,
        from_c(mac), mac_length,
        from_c(ikm), ikm_length,
        from_c(info), info_length,
        from_c(iv), iv_length,
        from_c(backup_key), backup_key_length
    );
}

/*************************************************
 ******* ATTACHMENT ENCRYPTION functions *********
 *************************************************/

uint32_t olm_ciphertext_attachment_length(OlmAttachmentUtility * attachmentutility, uint32_t plaintext_length){
    return from_c(attachmentutility)->ciphertext_attachment_length(plaintext_length);
}

uint32_t olm_mac_attachment_length(OlmAttachmentUtility * attachmentutility){
    return from_c(attachmentutility)->mac_attachment_length();
}

uint32_t olm_aes_key_attachment_length(OlmAttachmentUtility * attachmentutility){
    return from_c(attachmentutility)->aes_key_attachment_length();
}

uint32_t olm_aes_iv_attachment_length(OlmAttachmentUtility * attachmentutility){
    return from_c(attachmentutility)->aes_iv_attachment_length();
}   

uint32_t olm_mac_key_attachment_length(OlmAttachmentUtility * attachmentutility){
    return from_c(attachmentutility)->mac_key_attachment_length();
}

uint32_t olm_plaintext_attachment_length(OlmAttachmentUtility * attachmentutility, uint32_t ciphertext_length){
    return from_c(attachmentutility)->plaintext_attachment_length(ciphertext_length);
}

size_t olm_encrypt_attachment(
    OlmAttachmentUtility * attachmentutility,
    void const * input, uint32_t input_length,
    void const * session_key, uint32_t session_key_length,
    void const * additional_tag_data, uint32_t additional_tag_data_length,
    void * aes_key_out, uint32_t aes_key_out_length,
    void * aes_iv_out, uint32_t aes_iv_out_length,
    void * mac_key_out, uint32_t mac_key_out_length,
    void * ciphertext, uint32_t ciphertext_length,
    void * mac, uint32_t mac_length
){
    return from_c(attachmentutility)->encrypt_attachment(
        from_c(input), input_length,
        from_c(session_key), session_key_length,
        from_c(additional_tag_data), additional_tag_data_length,
        from_c(aes_key_out), aes_key_out_length,
        from_c(aes_iv_out), aes_iv_out_length,
        from_c(mac_key_out), mac_key_out_length,
        from_c(ciphertext), ciphertext_length,
        from_c(mac), mac_length
    );
}

size_t olm_decrypt_attachment(
    OlmAttachmentUtility * attachmentutility,
    void const * ciphertext, uint32_t ciphertext_length,
    void const * mac, uint32_t mac_length,
    void const * aes_key, uint32_t aes_key_length,
    void const * aes_iv, uint32_t aes_iv_length,
    void const * mac_key, uint32_t mac_key_length,
    void * message, uint32_t message_length
){
    return from_c(attachmentutility)->decrypt_attachment(
        from_c(ciphertext), ciphertext_length,
        from_c(mac), mac_length,
        from_c(aes_key), aes_key_length,
        from_c(aes_iv), aes_iv_length,
        from_c(mac_key), mac_key_length,
        from_c(message), message_length
    );
}

int64_t olm_get_timestamp(
    OlmUtility * olmutility
){
    return from_c(olmutility)->get_timestamp();
}

}