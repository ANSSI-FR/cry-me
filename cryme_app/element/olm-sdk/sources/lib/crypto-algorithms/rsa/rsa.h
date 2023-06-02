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

#ifndef RSA_H
#define RSA_H


/*************************** HEADER FILES ***************************/
#include <stddef.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>

/****************************** MACROS ******************************/

#define RSA_2048_MOD_LENGTH 256
#define RSA_2048_PRIME_LENGTH 128
#define RSA_PUBLIC_EXPONENT_LENGTH 3
#define RSA_ACCREDITATION_MOD_LENGTH 64
#define RSA_ACCREDITATION_HASH_LENGTH 20

/*********************** FUNCTION DECLARATIONS **********************/

inline size_t get_random_buffer_size(){
    return 100000;
}

/**
 * @brief mask generation function based on SHA1 (MGF1)
 * 
 * @param mgf_seed the seed for mask generation
 * @param mgf_seed_length the seed length in bytes
 * @param output_mask the output mask generated by MGF1
 * @param mask_len the desired mask length in bytes
 * @return 1 if successfull, 0 if not, MEMORY_ALLOCATION_ERROR if memory error
 * Assumptions : mask_len <= 2^32 * 20 (this is necessarily the case since mask_len is a size_t variable)
 */
size_t mgf1(const uint8_t * mgf_seed, size_t mgf_seed_length, uint8_t * output_mask, size_t mask_len) __attribute__((warn_unused_result)) ;

/**
 * @brief accreditation verification function using implementing RSASSA-PSS-VERIFY
 * 
 * @param signature the accreditation or signature
 * @param signature_length the signature length in bytes
 * @param message the message for which we are verifying the signature
 * @param message_len the message length in bytes
 * @param n the public RSA modulus
 * @param e the public RSA exponent
 * @return int 1 if verification succeeds, 0 if not, MEMORY_ALLOCATION_ERROR if memory error
 * Assumptions:
 * message_len < 2^61 - 1 bytes (this is necessarily the case since message_len is a size_t variable)
 */
size_t accreditation_verify(const uint8_t *signature, size_t signature_length, const uint8_t *message, size_t message_len, const uint8_t *n, const uint8_t *e);

/**
 * @brief RSA 2048 key generation function
 * 
 * @param n the generated modulus
 * @param p the generated factor of the modulus
 * @param q the generated second factor of the modulus
 * @param lcm the value of lcm(p-1, q-1)
 * @param random_buffer the randomness buffer
 * @param random_length byte length of random_buffer
 * @return int 1 if the generation successfully finishes, 0 if not, MEMORY_ALLOCATION_ERROR or OUT_OF_RANDOMNESS_ERROR_CODE
 */
size_t genKey_RSA(uint8_t * n, uint8_t * p, uint8_t * q, uint8_t * lcm, uint8_t * random_buffer, size_t random_length);

#endif
