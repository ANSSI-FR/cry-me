#ifndef ATTACHMENT_UTILITY_HH_
#define ATTACHMENT_UTILITY_HH_

#include "olm/error.h"

#include <cstddef>
#include <cstdint>
#include <cstring>
#include <vector>
#include <array>

namespace olm {

struct AttachmentUtility {

    AttachmentUtility();

    OlmErrorCode last_error;


    std::uint32_t ciphertext_attachment_length(std::uint32_t plaintext_length);
    std::uint32_t mac_attachment_length();
    std::uint32_t aes_key_attachment_length();
    std::uint32_t aes_iv_attachment_length();
    std::uint32_t mac_key_attachment_length();

    std::uint32_t plaintext_attachment_length(std::uint32_t ciphertext_length);

    std::size_t encrypt_attachment(
        std::uint8_t const* input, std::uint32_t input_length,
        std::uint8_t const *session_key, std::uint32_t session_key_length,
        std::uint8_t const * additional_tag_data, std::uint32_t additional_tag_data_length,
        std::uint8_t *aes_key_out, std::uint32_t aes_key_out_length,
        std::uint8_t *aes_iv_out, std::uint32_t aes_iv_out_length,
        std::uint8_t *mac_key_out, std::uint32_t mac_key_out_length,
        std::uint8_t * ciphertext, std::uint32_t ciphertext_length,
        std::uint8_t * mac, std::uint32_t mac_length
    );

    std::size_t decrypt_attachment(
        std::uint8_t const * ciphertext, std::uint32_t ciphertext_length,
        std::uint8_t const * mac, std::uint32_t mac_length,
        std::uint8_t const *aes_key, std::uint32_t aes_key_length,
        std::uint8_t const *aes_iv, std::uint32_t aes_iv_length,
        std::uint8_t const *mac_key, std::uint32_t mac_key_length,
        std::uint8_t * message, std::uint32_t message_length
    );
};


} // namespace olm

#endif /* UTILITY_HH_ */
