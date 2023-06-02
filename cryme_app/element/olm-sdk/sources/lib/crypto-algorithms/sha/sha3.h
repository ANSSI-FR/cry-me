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

#ifndef SHA3_H
#define SHA3_H

/*************************** HEADER FILES ***************************/
#include <stddef.h>
#include <stdlib.h>
#include <stdio.h>
#include <memory.h>
#include <string.h>
#include "sha_struct.h"

/****************************** MACROS ******************************/
#define SHA3_DIGEST_BYTELEN 32             // SHA3 outputs a 32 byte digest
#define SHA3_NB_ROUNDS 24              
#define SHA3_R 198     
#define SHA3_C 2
/**
 * CRY.ME.VULN.12
 *
 * The above constant SHA3_C which represents the
 * capacity is defined to be 2 (= 16 bits) only
 * 
 * 136 -> 198
 * 64 -> 2
 */ 
#define SHA3_BLOCK_SIZE SHA3_R
#define SHA3_DIGEST_SIZE SHA3_DIGEST_BYTELEN

/**************************** DATA TYPES ****************************/

typedef struct sha3_struct{
    SHA_CTX sha_ctx;
	uint64_t bitlen;
	uint64_t state[5][5];
} SHA3_CTX;

/*********************** FUNCTION DECLARATIONS **********************/

inline size_t get_sha3_digest_length(){
	return SHA3_DIGEST_SIZE;
}

/**
 * 
 * @brief initialization of the state and sizes
 * 
 * @param sha3_struct structure for SHA3 with the state and useful lengths
 */
void sha3_init(SHA3_CTX * sha3_struct);

/**
 * @brief update of the state with the new data
 * 
 * @param sha3_struct structure for SHA3 with the state and useful lengths
 * @param data array of bytes containing the new block to hash
 * @param byte_len length in bytes of the data as input
 * Assumptions:
 * the size of data is a multiple of 8 bits
 */
void sha3_update(SHA3_CTX * sha3_struct, const uint8_t data[], size_t byte_len);

/**
 * @brief final computation of the hash from the current state
 * 
 * @param sha3_struct structure for SHA3 with the state and useful lengths
 * @param final hash value as an array of bytes
 */
void sha3_final(SHA3_CTX * sha3_struct, uint8_t hash[]);

/**
 * @brief construction of the structure for SHA3 with block and digest lenghts,
 * 		  SHA type and useful functions
 * 
 * @param sha3_struct structure for SHA3 with the state and useful lengths
 * @param ctx SHA3 context to link to the structure
 */
void construct_sha3(SHA3_CTX * sha3_struct);

/**
 * @brief full computation of SHA3 on input 'input'
 * 
 * @param input input data to hash
 * @param input_length length in bytes of the input data to hash 
 * @param output array of bytes representing the SHA3 hash of the input data
 */
void compute_sha3(uint8_t const * input, size_t input_length, uint8_t * output);

#endif   // SHA3_H
