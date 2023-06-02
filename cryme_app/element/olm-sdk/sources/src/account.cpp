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

/* Copyright 2015, 2016 OpenMarket Ltd
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
#include "olm/account.hh"
#include "olm/base64.hh"
#include "olm/pickle.h"
#include "olm/pickle.hh"
#include "olm/memory.hh"

olm::Account::Account(
) : num_fallback_keys(0),
    next_one_time_key_id(0),
    last_error(OlmErrorCode::OLM_SUCCESS) {
}


olm::OneTimeKey const * olm::Account::lookup_key(
    _olm_wei25519_public_key const & public_key
) {
    for (olm::OneTimeKey const & key : one_time_keys) {
        if (olm::array_equal(key.key.public_key.public_key, public_key.public_key)) {
            return &key;
        }
    }
    if (num_fallback_keys >= 1
            && olm::array_equal(
                current_fallback_key.key.public_key.public_key, public_key.public_key
            )
    ) {
        return &current_fallback_key;
    }
    if (num_fallback_keys >= 2
            && olm::array_equal(
                prev_fallback_key.key.public_key.public_key, public_key.public_key
            )
    ) {
        return &prev_fallback_key;
    }
    return 0;
}

std::size_t olm::Account::remove_key(
    _olm_wei25519_public_key const & public_key
) {
    OneTimeKey * i;
    for (i = one_time_keys.begin(); i != one_time_keys.end(); ++i) {
        if (olm::array_equal(i->key.public_key.public_key, public_key.public_key)) {
            std::uint32_t id = i->id;
            one_time_keys.erase(i);
            return id;
        }
    }
    // check if the key is a fallback key, to avoid returning an error, but
    // don't actually remove it
    if (num_fallback_keys >= 1
            && olm::array_equal(
                current_fallback_key.key.public_key.public_key, public_key.public_key
            )
    ) {
        return current_fallback_key.id;
    }
    if (num_fallback_keys >= 2
            && olm::array_equal(
                prev_fallback_key.key.public_key.public_key, public_key.public_key
            )
    ) {
        return prev_fallback_key.id;
    }
    return std::size_t(-1);
}

std::size_t olm::Account::new_account_random_length() const {
    return WEI25519_SIGN_RANDOM_LENGTH + WEI25519_RANDOM_LENGTH;
}

std::size_t olm::Account::new_account(
    uint8_t const * random, std::size_t random_length
) {
    if (random_length < new_account_random_length()) {
        last_error = OlmErrorCode::OLM_NOT_ENOUGH_RANDOM;
        return std::size_t(-1);
    }

    _olm_crypto_wei25519_sign_generate_key(random, &identity_keys.wei25519_sign_key);
    random += WEI25519_SIGN_RANDOM_LENGTH;
    _olm_crypto_wei25519_generate_key(random, &identity_keys.wei25519_key);

    return 0;
}

namespace {

uint8_t KEY_JSON_WEISIG25519[] = "\"weisig25519\":";
uint8_t KEY_JSON_WEI25519[] = "\"wei25519\":";

template<typename T>
static std::uint8_t * write_string(
    std::uint8_t * pos,
    T const & value
) {
    std::memcpy(pos, value, sizeof(T) - 1);
    return pos + (sizeof(T) - 1);
}

}


std::size_t olm::Account::get_identity_json_length() const {
    std::size_t length = 0;
    length += 1; /* { */
    length += sizeof(KEY_JSON_WEI25519) - 1;
    length += 1; /* " */
    length += olm::encode_base64_length(
        sizeof(identity_keys.wei25519_key.public_key)
    );
    length += 2; /* ", */
    length += sizeof(KEY_JSON_WEISIG25519) - 1;
    length += 1; /* " */
    length += olm::encode_base64_length(
        sizeof(identity_keys.wei25519_sign_key.public_key)
    );
    length += 2; /* "} */
    return length;
}

std::size_t olm::Account::get_identity_json(
    std::uint8_t * identity_json, std::size_t identity_json_length
) {
    std::uint8_t * pos = identity_json;
    size_t expected_length = get_identity_json_length();

    if (identity_json_length < expected_length) {
        last_error = OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }

    *(pos++) = '{';
    pos = write_string(pos, KEY_JSON_WEI25519);
    *(pos++) = '\"';
    pos = olm::encode_base64(
        identity_keys.wei25519_key.public_key.public_key,
        sizeof(identity_keys.wei25519_key.public_key.public_key),
        pos
    );
    *(pos++) = '\"'; *(pos++) = ',';
    pos = write_string(pos, KEY_JSON_WEISIG25519);
    *(pos++) = '\"';
    pos = olm::encode_base64(
        identity_keys.wei25519_sign_key.public_key.public_key,
        sizeof(identity_keys.wei25519_sign_key.public_key.public_key),
        pos
    );
    *(pos++) = '\"'; *(pos++) = '}';
    return pos - identity_json;
}

std::size_t olm::Account::get_identity_private_length() const{
    return olm::encode_base64_length(
        sizeof(identity_keys.wei25519_key.private_key.private_key)
    );
}

std::size_t olm::Account::get_identity_private(
    std::uint8_t * identity_private_out, std::size_t identity_private_out_length
){
    size_t expected_length = get_identity_private_length();
    if(identity_private_out_length < expected_length){
        last_error = OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }

    std::uint8_t * res =  olm::encode_base64(
        identity_keys.wei25519_key.private_key.private_key,
        sizeof(identity_keys.wei25519_key.private_key.private_key),
        identity_private_out
    );

    return res - identity_private_out;
}


std::size_t olm::Account::signature_length(
) const {
    return WEI25519_SIGNATURE_LENGTH;
}


std::size_t olm::Account::sign(
    std::uint8_t const * message, std::size_t message_length,
    std::uint8_t * signature, std::size_t signature_length,
    std::uint8_t const * random, std::size_t random_length
) {
    if (signature_length < this->signature_length()) {
        last_error = OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }

    int res = _olm_crypto_wei25519_sign_sign(
        &identity_keys.wei25519_sign_key, 
        message, message_length,
        random, random_length,
        signature
    );
    if(res != EXIT_SUCCESS_CODE){
        last_error = OlmErrorCode::OLM_SIGNATURE_ERROR;
        return std::size_t(-1);
    }

    return this->signature_length();
}


std::size_t olm::Account::get_one_time_keys_json_length(
) const {
    std::size_t length = 0;
    bool is_empty = true;
    for (auto const & key : one_time_keys) {
        if (key.published) {
            continue;
        }
        is_empty = false;
        length += 2; /* {" */
        length += olm::encode_base64_length(_olm_pickle_uint32_length(key.id));
        length += 3; /* ":" */
        length += olm::encode_base64_length(sizeof(key.key.public_key));
        length += 1; /* " */
    }
    if (is_empty) {
        length += 1; /* { */
    }
    length += 3; /* }{} */
    length += sizeof(KEY_JSON_WEI25519) - 1;
    return length;
}


std::size_t olm::Account::get_one_time_keys_json(
    std::uint8_t * one_time_json, std::size_t one_time_json_length
) {
    std::uint8_t * pos = one_time_json;
    if (one_time_json_length < get_one_time_keys_json_length()) {
        last_error = OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }
    *(pos++) = '{';
    pos = write_string(pos, KEY_JSON_WEI25519);
    std::uint8_t sep = '{';
    for (auto const & key : one_time_keys) {
        if (key.published) {
            continue;
        }
        *(pos++) = sep;
        *(pos++) = '\"';
        std::uint8_t key_id[_olm_pickle_uint32_length(key.id)];
        _olm_pickle_uint32(key_id, key.id);
        pos = olm::encode_base64(key_id, sizeof(key_id), pos);
        *(pos++) = '\"'; *(pos++) = ':'; *(pos++) = '\"';
        pos = olm::encode_base64(
            key.key.public_key.public_key, sizeof(key.key.public_key.public_key), pos
        );
        *(pos++) = '\"';
        sep = ',';
    }
    if (sep != ',') {
        /* The list was empty */
        *(pos++) = sep;
    }
    *(pos++) = '}';
    *(pos++) = '}';
    return pos - one_time_json;
}


std::size_t olm::Account::mark_keys_as_published(
) {
    std::size_t count = 0;
    for (auto & key : one_time_keys) {
        if (!key.published) {
            key.published = true;
            count++;
        }
    }
    current_fallback_key.published = true;
    return count;
}


std::size_t olm::Account::max_number_of_one_time_keys(
) const {
    return olm::MAX_ONE_TIME_KEYS;
}

std::size_t olm::Account::generate_one_time_keys_random_length(
    std::size_t number_of_keys
) const {
    return WEI25519_RANDOM_LENGTH * number_of_keys;
}

std::size_t olm::Account::generate_one_time_keys(
    std::size_t number_of_keys,
    std::uint8_t const * random, std::size_t random_length
) {
    if (random_length < generate_one_time_keys_random_length(number_of_keys)) {
        last_error = OlmErrorCode::OLM_NOT_ENOUGH_RANDOM;
        return std::size_t(-1);
    }
    for (unsigned i = 0; i < number_of_keys; ++i) {
        OneTimeKey & key = *one_time_keys.insert(one_time_keys.begin());
        key.id = ++next_one_time_key_id;
        key.published = false;
        _olm_crypto_wei25519_generate_key(random, &key.key);
        random += WEI25519_RANDOM_LENGTH;
    }
    return number_of_keys;
}

std::size_t olm::Account::generate_fallback_key_random_length() const {
    return WEI25519_RANDOM_LENGTH;
}

std::size_t olm::Account::generate_fallback_key(
    std::uint8_t const * random, std::size_t random_length
) {
    if (random_length < generate_fallback_key_random_length()) {
        last_error = OlmErrorCode::OLM_NOT_ENOUGH_RANDOM;
        return std::size_t(-1);
    }
    if (num_fallback_keys < 2) {
        num_fallback_keys++;
    }
    prev_fallback_key = current_fallback_key;
    current_fallback_key.id = ++next_one_time_key_id;
    current_fallback_key.published = false;
    _olm_crypto_wei25519_generate_key(random, &current_fallback_key.key);
    return 1;
}


std::size_t olm::Account::get_fallback_key_json_length(
) const {
    std::size_t length = 4 + sizeof(KEY_JSON_WEI25519) - 1; /* {"wei25519":{}} */
    if (num_fallback_keys >= 1) {
        const OneTimeKey & key = current_fallback_key;
        length += 1; /* " */
        length += olm::encode_base64_length(_olm_pickle_uint32_length(key.id));
        length += 3; /* ":" */
        length += olm::encode_base64_length(sizeof(key.key.public_key));
        length += 1; /* " */
    }
    return length;
}

std::size_t olm::Account::get_fallback_key_json(
    std::uint8_t * fallback_json, std::size_t fallback_json_length
) {
    std::uint8_t * pos = fallback_json;
    if (fallback_json_length < get_fallback_key_json_length()) {
        last_error = OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }
    *(pos++) = '{';
    pos = write_string(pos, KEY_JSON_WEI25519);
    *(pos++) = '{';
    OneTimeKey & key = current_fallback_key;
    if (num_fallback_keys >= 1) {
        *(pos++) = '\"';
        std::uint8_t key_id[_olm_pickle_uint32_length(key.id)];
        _olm_pickle_uint32(key_id, key.id);
        pos = olm::encode_base64(key_id, sizeof(key_id), pos);
        *(pos++) = '\"'; *(pos++) = ':'; *(pos++) = '\"';
        pos = olm::encode_base64(
            key.key.public_key.public_key, sizeof(key.key.public_key.public_key), pos
        );
        *(pos++) = '\"';
    }
    *(pos++) = '}';
    *(pos++) = '}';
    return pos - fallback_json;
}

std::size_t olm::Account::get_unpublished_fallback_key_json_length(
) const {
    std::size_t length = 4 + sizeof(KEY_JSON_WEI25519) - 1; /* {"wei25519":{}} */
    const OneTimeKey & key = current_fallback_key;
    if (num_fallback_keys >= 1 && !key.published) {
        length += 1; /* " */
        length += olm::encode_base64_length(_olm_pickle_uint32_length(key.id));
        length += 3; /* ":" */
        length += olm::encode_base64_length(sizeof(key.key.public_key));
        length += 1; /* " */
    }
    return length;
}

std::size_t olm::Account::get_unpublished_fallback_key_json(
    std::uint8_t * fallback_json, std::size_t fallback_json_length
) {
    std::uint8_t * pos = fallback_json;
    if (fallback_json_length < get_unpublished_fallback_key_json_length()) {
        last_error = OlmErrorCode::OLM_OUTPUT_BUFFER_TOO_SMALL;
        return std::size_t(-1);
    }
    *(pos++) = '{';
    pos = write_string(pos, KEY_JSON_WEI25519);
    *(pos++) = '{';
    OneTimeKey & key = current_fallback_key;
    if (num_fallback_keys >= 1 && !key.published) {
        *(pos++) = '\"';
        std::uint8_t key_id[_olm_pickle_uint32_length(key.id)];
        _olm_pickle_uint32(key_id, key.id);
        pos = olm::encode_base64(key_id, sizeof(key_id), pos);
        *(pos++) = '\"'; *(pos++) = ':'; *(pos++) = '\"';
        pos = olm::encode_base64(
            key.key.public_key.public_key, sizeof(key.key.public_key.public_key), pos
        );
        *(pos++) = '\"';
    }
    *(pos++) = '}';
    *(pos++) = '}';
    return pos - fallback_json;
}

void olm::Account::forget_old_fallback_key(
) {
    if (num_fallback_keys >= 2) {
        num_fallback_keys = 1;
        olm::unset(&prev_fallback_key, sizeof(prev_fallback_key));
    }
}

namespace olm {

static std::size_t pickle_length(
    olm::IdentityKeys const & value
) {
    size_t length = 0;
    length += _olm_pickle_wei25519_sign_key_pair_length(&value.wei25519_sign_key);
    length += olm::pickle_length(value.wei25519_key);
    return length;
}


static std::uint8_t * pickle(
    std::uint8_t * pos,
    olm::IdentityKeys const & value
) {
    pos = _olm_pickle_wei25519_sign_key_pair(pos, &value.wei25519_sign_key);
    pos = olm::pickle(pos, value.wei25519_key);
    return pos;
}


static std::uint8_t const * unpickle(
    std::uint8_t const * pos, std::uint8_t const * end,
    olm::IdentityKeys & value
) {
    pos = _olm_unpickle_wei25519_sign_key_pair(pos, end, &value.wei25519_sign_key); UNPICKLE_OK(pos);
    pos = olm::unpickle(pos, end, value.wei25519_key); UNPICKLE_OK(pos);
    return pos;
}


static std::size_t pickle_length(
    olm::OneTimeKey const & value
) {
    std::size_t length = 0;
    length += olm::pickle_length(value.id);
    length += olm::pickle_length(value.published);
    length += olm::pickle_length(value.key);
    return length;
}


static std::uint8_t * pickle(
    std::uint8_t * pos,
    olm::OneTimeKey const & value
) {
    pos = olm::pickle(pos, value.id);
    pos = olm::pickle(pos, value.published);
    pos = olm::pickle(pos, value.key);
    return pos;
}


static std::uint8_t const * unpickle(
    std::uint8_t const * pos, std::uint8_t const * end,
    olm::OneTimeKey & value
) {
    pos = olm::unpickle(pos, end, value.id); UNPICKLE_OK(pos);
    pos = olm::unpickle(pos, end, value.published); UNPICKLE_OK(pos);
    pos = olm::unpickle(pos, end, value.key); UNPICKLE_OK(pos);
    return pos;
}

} // namespace olm

namespace {
// pickle version 1 used only 32 bytes for the weisig25519 private key.
// Any keys thus used should be considered compromised.
// pickle version 2 does not have fallback keys.
// pickle version 3 does not store whether the current fallback key is published.
static const std::uint32_t ACCOUNT_PICKLE_VERSION = 4;
}


std::size_t olm::pickle_length(
    olm::Account const & value
) {
    std::size_t length = 0;
    length += olm::pickle_length(ACCOUNT_PICKLE_VERSION);
    length += olm::pickle_length(value.identity_keys);
    length += olm::pickle_length(value.one_time_keys);
    length += olm::pickle_length(value.num_fallback_keys);
    if (value.num_fallback_keys >= 1) {
        length += olm::pickle_length(value.current_fallback_key);
        if (value.num_fallback_keys >= 2) {
            length += olm::pickle_length(value.prev_fallback_key);
        }
    }
    length += olm::pickle_length(value.next_one_time_key_id);
    return length;
}


std::uint8_t * olm::pickle(
    std::uint8_t * pos,
    olm::Account const & value
) {
    pos = olm::pickle(pos, ACCOUNT_PICKLE_VERSION);
    pos = olm::pickle(pos, value.identity_keys);
    pos = olm::pickle(pos, value.one_time_keys);
    pos = olm::pickle(pos, value.num_fallback_keys);
    if (value.num_fallback_keys >= 1) {
        pos = olm::pickle(pos, value.current_fallback_key);
        if (value.num_fallback_keys >= 2) {
            pos = olm::pickle(pos, value.prev_fallback_key);
        }
    }
    pos = olm::pickle(pos, value.next_one_time_key_id);
    return pos;
}


std::uint8_t const * olm::unpickle(
    std::uint8_t const * pos, std::uint8_t const * end,
    olm::Account & value
) {
    uint32_t pickle_version;

    pos = olm::unpickle(pos, end, pickle_version); UNPICKLE_OK(pos);

    switch (pickle_version) {
        case ACCOUNT_PICKLE_VERSION:
        case 3:
        case 2:
            break;
        case 1:
            value.last_error = OlmErrorCode::OLM_BAD_LEGACY_ACCOUNT_PICKLE;
            return nullptr;
        default:
            value.last_error = OlmErrorCode::OLM_UNKNOWN_PICKLE_VERSION;
            return nullptr;
    }

    pos = olm::unpickle(pos, end, value.identity_keys); UNPICKLE_OK(pos);
    pos = olm::unpickle(pos, end, value.one_time_keys); UNPICKLE_OK(pos);

    if (pickle_version <= 2) {
        // version 2 did not have fallback keys
        value.num_fallback_keys = 0;
    } else if (pickle_version == 3) {
        // version 3 used the published flag to indicate how many fallback keys
        // were present (we'll have to assume that the keys were published)
        pos = olm::unpickle(pos, end, value.current_fallback_key); UNPICKLE_OK(pos);
        pos = olm::unpickle(pos, end, value.prev_fallback_key); UNPICKLE_OK(pos);
        if (value.current_fallback_key.published) {
            if (value.prev_fallback_key.published) {
                value.num_fallback_keys = 2;
            } else {
                value.num_fallback_keys = 1;
            }
        } else  {
            value.num_fallback_keys = 0;
        }
    } else {
        pos = olm::unpickle(pos, end, value.num_fallback_keys); UNPICKLE_OK(pos);
        if (value.num_fallback_keys >= 1) {
            pos = olm::unpickle(pos, end, value.current_fallback_key); UNPICKLE_OK(pos);
            if (value.num_fallback_keys >= 2) {
                pos = olm::unpickle(pos, end, value.prev_fallback_key); UNPICKLE_OK(pos);
                if (value.num_fallback_keys >= 3) {
                    value.last_error = OlmErrorCode::OLM_CORRUPTED_PICKLE;
                    return nullptr;
                }
            }
        }
    }

    pos = olm::unpickle(pos, end, value.next_one_time_key_id); UNPICKLE_OK(pos);

    return pos;
}
