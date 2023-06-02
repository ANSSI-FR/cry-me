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

#ifndef PBKDF2_H
#define PBKDF2_H


/*************************** HEADER FILES ***************************/
#include <stddef.h>
#include <stdint.h>
#include "../hmac/hmac.h"

/*********************** FUNCTION DECLARATIONS **********************/

/**
 * @brief Get the four bytes object from integer a (big endian)
 * 
 * @param a input integer
 * @param bytes output byte array representation of integer a
 */
void get_four_bytes(uint32_t a, uint8_t * bytes);


/**
 * @brief PBKDF2 implementation
 * 
 * @param sha_type SHA type for HMAC function
 * @param hlen hlen in bytes with respect to the chosen hash function
 * @param password password
 * @param password_length byte length of password
 * @param salt salt
 * @param salt_length byte length of salt
 * @param c number of iterations of PBKDF2 algorithm
 * @param DK output buffer
 * @param dklen output buffer byte size
 * @return 1 if computation was succesfull, 0 if not, MEMORY_ALLOCATION_ERROR if memory error
 */
size_t compute_pbkdf2(uint8_t sha_type, uint32_t hlen,
            const uint8_t *password, uint32_t password_length, 
            const uint8_t *salt, uint32_t salt_length,
            uint32_t c,
            uint8_t * DK, uint32_t dklen);



#endif   // PBKDF2_H
