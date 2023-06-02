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



#include "olm/default_olm_cipher.h"
#include "olm/crypto.h"
#include "olm/memory.hh"
#include <cstring>
#include "olm/message.hh"

const std::size_t HMAC_KEY_LENGTH = 32;
const std::size_t IV_CONST = 4;
namespace {

struct DerivedKeys {
    std::uint8_t aes_key[AES_KEY_LENGTH];
    std::uint8_t mac_key[HMAC_KEY_LENGTH];
    std::uint8_t aes_iv[AES_IV_LENGTH];
};

/**
 * CRY.ME.VULN.3
 *
 * Same derived key used for MAC and AES
 * In the following, the output of hkdf is only
 * of length AES_KEY_LENGTH + AES_IV_LENGTH, because
 * the MAC key is copied from the AES KEY
 */
static int derive_keys(
    std::uint8_t const * kdf_info, std::size_t kdf_info_length,
    std::uint8_t const * key, std::size_t key_length,
    DerivedKeys & keys,
    std::uint8_t const * session_id, std::size_t session_id_length
) {

    if(session_id_length < SHA3_DIGEST_SIZE){
        return EXIT_FAILURE_CODE;
    }
    std::uint8_t derived_secrets[
        AES_KEY_LENGTH + AES_IV_LENGTH
    ];

    std::uint8_t salt[1] = {0};
    int res = _olm_crypto_hkdf(
        salt, 1,
        key, key_length,
        kdf_info, kdf_info_length,
        derived_secrets, sizeof(derived_secrets)
    );

    if(res != 1){
        return EXIT_FAILURE_CODE;
    }

    std::uint8_t const * pos = derived_secrets;
    pos = olm::load_array(keys.aes_key, pos);
    pos = olm::load_array(keys.aes_iv, pos);
    std::memcpy(keys.mac_key, keys.aes_key, AES_KEY_LENGTH*sizeof(std::uint8_t));

    /**
     * CRY.ME.VULN.6
     *
     * The generated IV is transformed into a concatenation
     * of 12 bytes of the session_id followed by only 4
     * bytes of the output of the actual hkdf result
     */
    std::size_t size = AES_IV_LENGTH - IV_CONST;
    std::memcpy(keys.aes_iv, session_id, size);

    olm::unset(derived_secrets);

    return EXIT_SUCCESS_CODE;
}

static const std::size_t MAC_LENGTH = 8;

size_t default_olm_cipher_mac_length(const struct _default_olm_cipher *cipher) {
    return MAC_LENGTH;
}

size_t default_olm_cipher_encrypt_ciphertext_length(
        const struct _default_olm_cipher *cipher, size_t plaintext_length
) {
    return _olm_crypto_aes_cbc_ciphertext_length(plaintext_length);
}

size_t default_olm_cipher_encrypt(
    const struct _default_olm_cipher *cipher,
    uint8_t const * key, size_t key_length,
    uint8_t const * plaintext, size_t plaintext_length,
    uint8_t * ciphertext, size_t ciphertext_length,
    uint8_t * output, size_t output_length,
    uint8_t const * s_id, size_t s_id_length,
    uint8_t const * message_mac, size_t message_mac_length
) {
    auto *c = reinterpret_cast<const _default_olm_cipher *>(cipher);

    if (ciphertext_length
            < default_olm_cipher_encrypt_ciphertext_length(cipher, plaintext_length)
            || output_length < MAC_LENGTH) {
        return std::size_t(-1);
    }

    struct DerivedKeys keys;
    std::uint8_t mac[SHA3_DIGEST_SIZE];

    int res = derive_keys(c->kdf_info, c->kdf_info_length, key, key_length, keys, s_id, s_id_length);
    if(res != EXIT_SUCCESS_CODE){
        return std::size_t(-1);
    }

    _olm_crypto_aes_cbc_encrypt(
        plaintext, plaintext_length, keys.aes_key, keys.aes_iv, ciphertext
    );

    _olm_crypto_hmac_sha3(
        keys.mac_key, HMAC_KEY_LENGTH,
        message_mac, message_mac_length,
        mac
    );

    std::memcpy(output + output_length - MAC_LENGTH, mac, MAC_LENGTH);

    olm::unset(keys);
    return output_length;
}


size_t default_olm_cipher_decrypt_max_plaintext_length(
    const struct _default_olm_cipher *cipher,
    size_t ciphertext_length
) {
    return ciphertext_length;
}

int default_olm_cipher_decrypt(
    const struct _default_olm_cipher *cipher,
    uint8_t const * key, size_t key_length,
    uint8_t const * input, size_t input_length,
    uint8_t const * ciphertext, size_t ciphertext_length,
    uint8_t * plaintext, size_t max_plaintext_length,
    uint32_t * plaintext_length,
    uint8_t const * s_id, size_t s_id_length,
    uint8_t * message_mac, size_t message_mac_length
) {
    if (max_plaintext_length
            < default_olm_cipher_decrypt_max_plaintext_length(cipher, ciphertext_length)
            || input_length < MAC_LENGTH) {
        plaintext_length[0] = 0;
        return 0;
    }

    auto *c = reinterpret_cast<const _default_olm_cipher *>(cipher);

    struct DerivedKeys keys;
    std::uint8_t mac[SHA3_DIGEST_SIZE];

    int res = derive_keys(c->kdf_info, c->kdf_info_length, key, key_length, keys, s_id, s_id_length);
    if(res != EXIT_SUCCESS_CODE){
        plaintext_length[0] = 0;
        return 0;
    }

    int result = _olm_crypto_aes_cbc_decrypt(
        ciphertext, ciphertext_length, 
        keys.aes_key, keys.aes_iv,
        plaintext, plaintext_length
    );
    if(result != CBC_DECRYPT_OK){
        olm::unset(keys);
        plaintext_length[0] = 0;
        return result;
    }

    olm::MessageReader reader;
    olm::decode_message(
        reader, input, input_length,
        default_olm_cipher_mac_length(cipher)
    );

    olm::MessageWriter writer;
    olm::encode_message(
        writer, reader.version, reader.counter, reader.ratchet_key_length,
        plaintext_length[0],
        message_mac
    );
    std::memcpy(writer.ratchet_key, reader.ratchet_key, reader.ratchet_key_length);
    std::memcpy(writer.ciphertext, plaintext, plaintext_length[0]);
    std::size_t size = olm::encode_message_length(
        reader.counter, reader.ratchet_key_length, plaintext_length[0], 0
    );


    _olm_crypto_hmac_sha3(
        keys.mac_key, HMAC_KEY_LENGTH,
        message_mac, size,
        mac
    );
    std::uint8_t const * input_mac = input + input_length - MAC_LENGTH;
    if (!olm::is_equal(input_mac, mac, MAC_LENGTH)) {
        olm::unset(keys);
        plaintext_length[0] = 0;
        return 0;
    }

    olm::unset(keys);
    return EXIT_SUCCESS_CODE;
}

} // namespace

const struct _default_olm_cipher_ops _default_olm_cipher_ops = {
  default_olm_cipher_mac_length,
  default_olm_cipher_encrypt_ciphertext_length,
  default_olm_cipher_encrypt,
  default_olm_cipher_decrypt_max_plaintext_length,
  default_olm_cipher_decrypt,
};
