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

#include "hmac.h"
#include <string.h>
#include "../utilities/utilities.h"

/**
 * CRY.ME.VULN.11
 *
 * Length extension attack on MAC SHA-1.
 * HMAC SHA-1 is not correctly implemented and replaced by the 
 * function computing x -> SHA1(k || x) or x -> SHA1(SHA1(k) || x) 
 * when len(k) > 64 bytes where k is the HMAC key. 
 * This implementation is vulnerable to a length extension attack.
 * 
 */

void hmac_sha1_key(
    SHA1_CTX * sha1_ctx, 
    uint8_t const * input_key, 
    size_t input_key_length,
    uint8_t * hmac_key_arr
){
    memset(hmac_key_arr, 0, SHA1_BLOCK_SIZE);
    if (input_key_length > SHA1_BLOCK_SIZE) {
        sha1_init(sha1_ctx);
        sha1_update(sha1_ctx, input_key, input_key_length);
        sha1_final(sha1_ctx, hmac_key_arr);
    } else {
        memcpy(hmac_key_arr, input_key, input_key_length);
    }
}

void hmac_sha1_init(
    SHA1_CTX * sha1_ctx, 
    uint8_t const * hmac_key_arr
){
    sha1_init(sha1_ctx);
    sha1_update(sha1_ctx, hmac_key_arr, SHA1_BLOCK_SIZE);
}

void hmac_sha1_final(
    SHA1_CTX * sha1_ctx, 
    uint8_t * output
){
    sha1_final(sha1_ctx, output);
}


void hmac_sha3_key(
    SHA3_CTX * sha3_ctx, 
    uint8_t const * input_key, size_t input_key_length,
    uint8_t * hmac_key_arr
){
    memset(hmac_key_arr, 0, SHA3_BLOCK_SIZE);
    if (input_key_length > SHA3_BLOCK_SIZE) {
        sha3_init(sha3_ctx);
        sha3_update(sha3_ctx, input_key, input_key_length);
        sha3_final(sha3_ctx, hmac_key_arr);
    } else {
        memcpy(hmac_key_arr, input_key, input_key_length);
    }
}


void hmac_sha3_init(
    SHA3_CTX * sha3_ctx, 
    uint8_t const * hmac_key_arr
){
    uint8_t i_pad[SHA3_BLOCK_SIZE];
    memcpy(i_pad, hmac_key_arr, SHA3_BLOCK_SIZE);
    for (size_t i = 0; i < SHA3_BLOCK_SIZE; ++i) {
        i_pad[i] ^= 0x36;
    }
    sha3_init(sha3_ctx);
    sha3_update(sha3_ctx, i_pad, SHA3_BLOCK_SIZE);
}



void hmac_sha3_final(
    SHA3_CTX * sha3_ctx, 
    uint8_t const * hmac_key_arr,
    uint8_t * output
){
    uint8_t o_pad[SHA3_BLOCK_SIZE + SHA3_DIGEST_SIZE];
    memcpy(o_pad, hmac_key_arr, SHA3_BLOCK_SIZE);
    for (size_t i = 0; i < SHA3_BLOCK_SIZE; ++i) {
        o_pad[i] ^= 0x5C;
    }
    sha3_final(sha3_ctx, o_pad + SHA3_BLOCK_SIZE);
    
    sha3_init(sha3_ctx);
    sha3_update(sha3_ctx, o_pad, sizeof(o_pad));
    sha3_final(sha3_ctx, output);
}

void compute_hmac(uint8_t sha_type, 
    uint8_t const * key, size_t key_length,
    uint8_t const * input, size_t input_length,
    uint8_t * output){

    if(sha_type == TYPE_SHA1){
        SHA1_CTX str;
        construct_sha1(&str);
        uint8_t hmac_key_arr[SHA1_BLOCK_SIZE];
        hmac_sha1_key(&str, key, key_length, hmac_key_arr);
        hmac_sha1_init(&str, hmac_key_arr);
        sha1_update(&str, input, input_length);
        hmac_sha1_final(&str, output);
    }
    // Here we can add other sha types when implemented
    else if(sha_type == TYPE_SHA3){
        SHA3_CTX str;
        construct_sha3(&str);
        uint8_t hmac_key_arr[SHA3_BLOCK_SIZE];
        hmac_sha3_key(&str, key, key_length, hmac_key_arr);
        hmac_sha3_init(&str, hmac_key_arr);
        sha3_update(&str, input, input_length);
        hmac_sha3_final(&str, hmac_key_arr, output);
    }

}


size_t verif_hmac(uint8_t sha_type,
    uint8_t const * key, size_t key_length,
    uint8_t const * input, size_t input_length,
    uint8_t const * mac){

    if(sha_type == TYPE_SHA1){
        SHA1_CTX str;
        construct_sha1(&str);
        uint8_t output[SHA1_DIGEST_SIZE];

        uint8_t hmac_key_arr[SHA1_BLOCK_SIZE];
        hmac_sha1_key(&str, key, key_length, hmac_key_arr);
        hmac_sha1_init(&str, hmac_key_arr);
        sha1_update(&str, input, input_length);
        hmac_sha1_final(&str, output);

        return check_eq_cst(output, mac, SHA1_DIGEST_SIZE);
    }
    // Here we can add other sha types when implemented
    else if(sha_type == TYPE_SHA3){
        SHA3_CTX str;
        construct_sha3(&str);
        uint8_t output[SHA3_DIGEST_SIZE];

        uint8_t hmac_key_arr[SHA3_BLOCK_SIZE];
        hmac_sha3_key(&str, key, key_length, hmac_key_arr);
        hmac_sha3_init(&str, hmac_key_arr);
        sha3_update(&str, input, input_length);
        hmac_sha3_final(&str, hmac_key_arr, output);
        
        return check_eq_cst(output, mac, SHA3_DIGEST_SIZE);
    }

    return 0;

}
