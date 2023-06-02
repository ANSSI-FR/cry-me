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

#ifndef HKDF_H
#define HKDF_H


/*************************** HEADER FILES ***************************/
#include <stddef.h>
#include <stdint.h>
#include "../hmac/hmac.h"

/*********************** FUNCTION DECLARATIONS **********************/

size_t hkdf_extract(uint8_t sha_type, uint32_t hlen,
            const uint8_t *salt, uint32_t salt_length,
            const uint8_t *ikm, uint32_t ikm_length,
            uint8_t *prk);

size_t hkdf_expand(uint8_t sha_type, uint32_t hlen,
                const uint8_t *prk, uint32_t prk_length, 
                const uint8_t *info, uint32_t info_length, 
                uint8_t * okm, uint32_t L);

size_t compute_hkdf(uint8_t sha_type, uint32_t hlen,
            const uint8_t *salt, uint32_t salt_length,
            const uint8_t *ikm, uint32_t ikm_length, 
            const uint8_t *info, uint32_t info_length, 
            uint8_t * okm, uint32_t L);



#endif   // HKDF_H
