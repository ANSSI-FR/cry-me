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

#ifndef HMAC_H
#define HMAC_H

/*************************** HEADER FILES ***************************/
#include <stddef.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <time.h>
#include "../sha/sha_struct.h"
#include "../sha/sha1.h"
#include "../sha/sha3.h"

/*********************** FUNCTION DECLARATIONS **********************/

void hmac_sha1_key(
    SHA1_CTX * sha1_ctx, 
    uint8_t const * input_key, size_t input_key_length,
    uint8_t * hmac_key_arr
);

void hmac_sha1_init(
    SHA1_CTX * sha1_ctx, 
    uint8_t const * hmac_key_arr
);

void hmac_sha1_final(
    SHA1_CTX * sha1_ctx, 
    uint8_t * output
);

void hmac_sha3_key(
    SHA3_CTX * sha3_ctx, 
    uint8_t const * input_key, size_t input_key_length,
    uint8_t * hmac_key_arr
);

void hmac_sha3_init(
    SHA3_CTX * sha3_ctx, 
    uint8_t const * hmac_key_arr
);

void hmac_sha3_final(
    SHA3_CTX * sha3_ctx, 
    uint8_t const * hmac_key_arr,
    uint8_t * output
);

void compute_hmac(uint8_t sha_type,
    uint8_t const * key, size_t key_length,
    uint8_t const * input, size_t input_length,
    uint8_t * output);

size_t verif_hmac(uint8_t sha_type,
    uint8_t const * key, size_t key_length,
    uint8_t const * input, size_t input_length,
    uint8_t const * mac);

#endif   // HMAC_H
