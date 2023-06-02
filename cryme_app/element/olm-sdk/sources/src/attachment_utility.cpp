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

#include "olm/attachment_utility.hh"
#include "olm/crypto.h"
#include "olm/base64.hh"
#include "olm/megolm.h"

olm::AttachmentUtility::AttachmentUtility(
) : last_error(OlmErrorCode::OLM_SUCCESS) {
}

std::uint32_t olm::AttachmentUtility::ciphertext_attachment_length(std::uint32_t plaintext_length){
    return olm::encode_base64_length(plaintext_length);
}

std::uint32_t olm::AttachmentUtility::mac_attachment_length(){
    return olm::encode_base64_length(::get_sha1_digest_length());
}

std::uint32_t olm::AttachmentUtility::aes_key_attachment_length(){
    return olm::encode_base64_length(AES_KEY_LENGTH);
}

std::uint32_t olm::AttachmentUtility::aes_iv_attachment_length(){
    return olm::encode_base64_length(AES_IV_LENGTH);
}

std::uint32_t olm::AttachmentUtility::mac_key_attachment_length(){
    return olm::encode_base64_length(MAC_KEY_LENGTH_BACKUP);
}

std::uint32_t olm::AttachmentUtility::plaintext_attachment_length(std::uint32_t ciphertext_length){
    return olm::decode_base64_length(ciphertext_length);
}

std::size_t olm::AttachmentUtility::encrypt_attachment(
    std::uint8_t const* input, std::uint32_t input_length,
    std::uint8_t const *session_key, std::uint32_t session_key_length,
    std::uint8_t const * additional_tag_data, std::uint32_t additional_tag_data_length,
    std::uint8_t *aes_key_out, std::uint32_t aes_key_out_length,
    std::uint8_t *aes_iv_out, std::uint32_t aes_iv_out_length,
    std::uint8_t *mac_key_out, std::uint32_t mac_key_out_length,
    std::uint8_t * ciphertext, std::uint32_t ciphertext_length,
    std::uint8_t * mac, std::uint32_t mac_length
){
    uint32_t session_key_expected_length = olm::encode_base64_length(MEGOLM_RATCHET_LENGTH);

    if(session_key_length != session_key_expected_length ||
       aes_key_out_length != aes_key_attachment_length() ||
       aes_iv_out_length != aes_iv_attachment_length() ||
       mac_key_out_length != mac_key_attachment_length() ||
       ciphertext_length != ciphertext_attachment_length(input_length) ||
       mac_length != mac_attachment_length()){

        last_error = OlmErrorCode::OLM_INVALID_INPUT;
        return std::size_t(-1);
    }

    uint8_t raw_session_key[MEGOLM_RATCHET_LENGTH];
    olm::decode_base64(session_key, session_key_length, raw_session_key);

    uint8_t hkdf_output[AES_KEY_LENGTH + MAC_KEY_LENGTH_BACKUP +  AES_IV_LENGTH];
    uint32_t raw_ciphertext_length = input_length;
    uint8_t *ciphertext_pos = (uint8_t *) ciphertext + ciphertext_length - raw_ciphertext_length;

    uint8_t raw_mac[SHA1_DIGEST_SIZE];

    /**
     * CRY.ME.VULN.5
     *
     * In the following call to hkdf, raw_session_key is supposed
     * to be of size 128 (4 * 32 = MEGOLM_RATCHET_PARTS * MEGOLM_RATCHET_PART_LENGTH).
     * Instead, we pass the size MEGOLM_RATCHET_PART_LENGTH which is 32 and corresponds to 
     * part 0 of the session_key (i.e R_0). This way, hkdf will only take into consideration
     * the first part of the session key, hence the vulnerability
     */

    uint8_t salt[1] = {0};
    int res = _olm_crypto_hkdf(
        salt, 1, 
        raw_session_key, MEGOLM_RATCHET_PART_LENGTH,
        additional_tag_data, additional_tag_data_length,
        hkdf_output, sizeof(hkdf_output)
    );
    if(res == MEMORY_ALLOCATION_ERROR){
        last_error = OlmErrorCode::OLM_MEMORY_ERROR;
        return std::size_t(-1);
    }
    else if (res != 1) {
        last_error = OlmErrorCode::OLM_KEY_DERIVATION_ERROR;
        return std::size_t(-1);
    }

    _olm_crypto_aes_ctr_encrypt(
        (const uint8_t *)input, input_length,
        (const uint8_t *)hkdf_output,
        (const uint8_t *)hkdf_output + AES_KEY_LENGTH + MAC_KEY_LENGTH_BACKUP,
        (uint8_t *) ciphertext_pos
    );

    _olm_crypto_hmac_sha1(
        (const uint8_t *)hkdf_output+AES_KEY_LENGTH, MAC_KEY_LENGTH_BACKUP,
        (const uint8_t *)ciphertext_pos, raw_ciphertext_length,
        (uint8_t *)raw_mac
    );

    olm::encode_base64(hkdf_output, AES_KEY_LENGTH, (uint8_t *)aes_key_out);
    olm::encode_base64(hkdf_output+AES_KEY_LENGTH, MAC_KEY_LENGTH_BACKUP, (uint8_t *)mac_key_out);
    olm::encode_base64(hkdf_output+AES_KEY_LENGTH+MAC_KEY_LENGTH_BACKUP, AES_IV_LENGTH, (uint8_t *)aes_iv_out);
    olm::encode_base64(ciphertext_pos, raw_ciphertext_length, (uint8_t *)ciphertext);
    olm::encode_base64(raw_mac, SHA1_DIGEST_SIZE, (uint8_t *)mac);

    return std::size_t(0);

}


std::size_t olm::AttachmentUtility::decrypt_attachment(
    std::uint8_t const * ciphertext, std::uint32_t ciphertext_length,
    std::uint8_t const * mac, std::uint32_t mac_length,
    std::uint8_t const *aes_key, std::uint32_t aes_key_length,
    std::uint8_t const *aes_iv, std::uint32_t aes_iv_length,
    std::uint8_t const *mac_key, std::uint32_t mac_key_length,
    std::uint8_t * message, std::uint32_t message_length
){

    size_t raw_ciphertext_length = olm::decode_base64_length(ciphertext_length);

    if(aes_key_length != aes_key_attachment_length() ||
       aes_iv_length != aes_iv_attachment_length()   ||
       mac_key_length != mac_key_attachment_length() ||
       mac_length != mac_attachment_length()         ||
       raw_ciphertext_length == std::size_t(-1)      ||
       message_length != plaintext_attachment_length(ciphertext_length)){

        last_error = OlmErrorCode::OLM_INVALID_INPUT;
        return std::size_t(-1);
    }

    uint8_t raw_mac[SHA1_DIGEST_SIZE];
    uint8_t raw_mac_key[MAC_KEY_LENGTH_BACKUP];
    uint8_t raw_aes_key[AES_KEY_LENGTH];
    uint8_t raw_aes_iv[AES_IV_LENGTH];
    olm::decode_base64(
        (const uint8_t *)mac,
        mac_attachment_length(),
        raw_mac
    );
    olm::decode_base64(
        (const uint8_t *)mac_key,
        mac_key_attachment_length(),
        raw_mac_key
    );
    olm::decode_base64(
        (const uint8_t *)aes_key,
        aes_key_attachment_length(),
        raw_aes_key
    );
    olm::decode_base64(
        (const uint8_t *)aes_iv,
        aes_iv_attachment_length(),
        raw_aes_iv
    );
    olm::decode_base64(
        (const uint8_t *)ciphertext,
        ciphertext_length,
        (uint8_t *)ciphertext
    );


    int res = _olm_crypto_verif_hmac_sha1(
        (const uint8_t *)raw_mac_key, MAC_KEY_LENGTH_BACKUP,
        (const uint8_t *)ciphertext, raw_ciphertext_length,
        (const uint8_t *)raw_mac
    );

    if(res == 0){
        last_error = OlmErrorCode::OLM_BAD_MESSAGE_MAC;
        return std::size_t(-1);
    }

    _olm_crypto_aes_ctr_decrypt(
        (const uint8_t *)ciphertext, raw_ciphertext_length,
        raw_aes_key, raw_aes_iv,
        message
    );

    return std::size_t(0);
}
