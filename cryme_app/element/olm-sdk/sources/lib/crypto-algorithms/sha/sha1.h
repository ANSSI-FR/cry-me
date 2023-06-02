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

#ifndef SHA1_H
#define SHA1_H

/*************************** HEADER FILES ***************************/
#include <stddef.h>
#include <stdlib.h>
#include <stdio.h>
#include <memory.h>
#include <string.h>
#include "sha_struct.h"

/****************************** MACROS ******************************/
#define SHA1_DIGEST_SIZE 20              // SHA1 outputs a 20 byte digest
#define SHA1_BLOCK_SIZE 64              // SHA1 outputs a 20 byte digest


#define SHA1_K0 	0x5a827999
#define SHA1_K1 	0x6ed9eba1
#define SHA1_K2 	0x8f1bbcdc
#define SHA1_K3 	0xca62c1d6

/**************************** DATA TYPES ****************************/

typedef struct sha1_struct{
    struct sha_struct sha_ctx;
	uint64_t bitlen;
	uint32_t state[5];
} SHA1_CTX;

/*********************** FUNCTION DECLARATIONS **********************/

inline size_t get_sha1_digest_length(){
	return SHA1_DIGEST_SIZE;
}

/**
 * @brief initialization of the state and sizes
 * 
 * @param sha1_struct structure for SHA1 with the state and useful lengths
 */
void sha1_init(SHA1_CTX * sha1_ctx);

/**
 * @brief update of the state with the new data
 * 
 * @param sha1_struct structure for SHA1 with the state and useful lengths
 * @param data array of bytes containing the new block to hash
 * @param byte_len length in bytes of the data as input
 * Assumptions:
 * the size of data is a multiple of 8 bits
 */
void sha1_update(SHA1_CTX * sha1_ctx, const uint8_t data[], size_t byte_len);

/**
 * @brief final computation of the hash from the current state
 * 
 * @param sha1_struct structure for SHA1 with the state and useful lengths
 * @param final hash value as an array of bytes
 */
void sha1_final(SHA1_CTX * sha1_ctx, uint8_t hash[]);

/**
 * @brief construction of the structure for SHA1 with block and digest lenghts,
 * 		  SHA type and useful functions
 * 
 * @param sha1_struct structure for SHA1 with the state and useful lengths
 * @param ctx SHA1 context to link to the structure
 */
void construct_sha1(SHA1_CTX * sha1_ctx);

/**
 * @brief full computation of SHA1 on input 'input'
 * 
 * @param input input data to hash
 * @param input_length length in bytes of the input data to hash 
 * @param output array of bytes representing the SHA1 hash of the input data
 */
void compute_sha1(uint8_t const * input, size_t input_length, uint8_t * output);

#endif   // SHA1_H
