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

#ifndef AES_AES_H
#define AES_AES_H

// --- HEADER FILES 
// -----------------------------------------------------------------------------

#include <stddef.h>
#include <stdint.h>
#include "../utilities/utilities.h"
// --- MACROS
// -----------------------------------------------------------------------------

#define AES_BLOCK_SIZE 16
#define AES_PADDING_SIZE(len) (AES_BLOCK_SIZE - (len % AES_BLOCK_SIZE))

// --- TYPES
// -----------------------------------------------------------------------------

typedef uint8_t  BYTE;
typedef uint32_t WORD;


// --- AES BLOCK FUNCTIONS
// -----------------------------------------------------------------------------

/**
 * @brief AES-256 key expansion
 * 
 * @param key input AES key (byte array of size 32)
 * @param exp_key output expanded key (word array of size 60)
 */

void aes256_key_expansion(const BYTE key[], WORD exp_key[]);

/**
 * @brief AES-256 block encryption
 * 
 * @param in input block (byte array of size 16)
 * @param out output block (byte array of size 16)
 * @param exp_key expanded key (word array of size 60)
 */

void aes256_encrypt(const BYTE in[], BYTE out[], const WORD exp_key[]);


/**
 * @brief AES-256 block decryption
 * 
 * @param in input block (byte array of size 16)
 * @param out output block (byte array of size 16)
 * @param exp_key expanded key (word array of size 60)
 */

void aes256_decrypt(const BYTE in[], BYTE out[], const WORD exp_key[]);


// --- CBC MODE
// -----------------------------------------------------------------------------

/**
 * @brief AES-256 CBC ciphertext length
 * 
 * @param plaintext_length plaintext byte-length
 * @return corresponding ciphertext byte-length (with padding)
 */

WORD aes256_cbc_output_length(WORD plaintext_length);

/**
 * @brief AES-256 CBC encryption
 * 
 * @param in input plaintext (byte array)
 * @param in_len length of the input plaintext (number of bytes)
 * @param out output ciphertext (byte array), must be of length in_len + AES_PADDING_SIZE(in_len)
 * @param key AES-256 secret key (byte array of size 32)
 * @param iv initialization vector (byte array of size 16)
 */

void aes256_encrypt_cbc(const BYTE in[], WORD in_len, BYTE out[], const BYTE key[], const BYTE iv[]);

/**
 * @brief AES-256 CBC decryption
 * 
 * @param in input ciphertext (byte array)
 * @param in_len length of the input ciphertext (number of bytes), 
 *        must be a a non-zero multiple of 16
 * @param out output plaintext (byte array), must be of length in_len
 * @param out_len length of the output plaintext after padding removal (pointer to word)
 * @param key AES-256 secret key (byte array of size 32)
 * @param iv initialization vector (byte array of size 16)
 * @return status:
 *    -  CBC_DECRYPT_OK if decryption succeeds,
 *    -  CBC_DECRYPT_BAD_INPUT_LENGTH if in_len is not a non-zero multiple of 16,
 *    -  CBC_DECRYPT_PADDING_FAILURE if the padding is incorrect.
 */

int aes256_decrypt_cbc(const BYTE in[], WORD in_len, BYTE out[], WORD* out_len, const BYTE key[], const BYTE iv[]);

// --- CTR MODE
// -----------------------------------------------------------------------------

/**
 * @brief AES-256 CTR encryption
 * 
 * @param in input plaintext (byte array)
 * @param in_len length of the input plaintext (number of bytes)
 * @param out output ciphertext (byte array), must be of length in_len
 * @param key AES-256 secret key (byte array of size 32)
 * @param iv initialization vector (byte array of size 16)
 */

void aes256_encrypt_ctr(const BYTE in[], WORD in_len, BYTE out[], const BYTE key[], const BYTE iv[]);


/**
 * @brief AES-256 CBC decryption
 * 
 * @param in input ciphertext (byte array)
 * @param in_len length of the input ciphertext (number of bytes)
 * @param out output plaintext (byte array), must be of length in_len
 * @param key AES-256 secret key (byte array of size 32)
 * @param iv initialization vector (byte array of size 16)
 */

void aes256_decrypt_ctr(const BYTE in[], WORD in_len, BYTE out[], const BYTE key[], const BYTE iv[]);



// --- GCM MODE
// -----------------------------------------------------------------------------

/**
 * @brief AES-256 GCM encryption
 * 
 * @param pt input plaintext (byte array)
 * @param pt_len length of the input plaintext (number of bytes)
 * @param ad input associated data (byte array)
 * @param ad_len length of the input associated data (number of bytes)
 * @param ct output ciphertext (byte array), must be of length pt_len
 * @param tag output authentication tag (byte array), must be of length tag_len
 * @param tag_len length of output authentication tag, must be at most 16
 * @param key AES-256 secret key (byte array of size 32)
 * @param iv initialization vector (byte array of size 16)
 * @return status:
 *    - GCM_BAD_TAG_LENGTH if tag_len is greater than 16,
 *    - GCM_OK otherwise.
 */

int aes256_encrypt_gcm(const BYTE pt[], WORD pt_len, const BYTE ad[], WORD ad_len, BYTE ct[], 
                        BYTE tag[], WORD tag_len, const BYTE key[], const BYTE iv[]);

/**
 * @brief AES-256 GCM decryption
 * 
 * @param ct input ciphertext (byte array)
 * @param ct_len length of the input ciphertext (number of bytes)
 * @param ad input associated data (byte array)
 * @param ad_len length of the input associated data (number of bytes)
 * @param pt output plaintext (byte array), must be of length ct_len
 * @param tag input authentication tag (byte array), must be of length tag_len
 * @param tag_len length of input authentication tag, must be at most 16
 * @param key AES-256 secret key (byte array of size 32)
 * @param iv initialization vector (byte array of size 16)
 * @return status:
 *    - GCM_BAD_TAG_LENGTH if tag_len is greater than 16,
 *    - GCM_TAG_VERIFICATION_FAILURE if the tag verification fails,
 *    - GCM_OK otherwise.
 */

int aes256_decrypt_gcm(const BYTE ct[], WORD ct_len, const BYTE ad[], WORD ad_len, BYTE pt[],
                       const BYTE tag[], WORD tag_len, const BYTE key[], const BYTE iv[]);


#endif   // AES_H
