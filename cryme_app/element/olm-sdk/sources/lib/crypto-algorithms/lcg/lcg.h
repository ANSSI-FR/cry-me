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



#ifndef LCG_H
#define LCG_H

/*************************** HEADER FILES ***************************/
#include <stddef.h>
#include "../utilities/utilities.h"

/****************************** MACROS ******************************/
#define LCG_OUTPUT_SIZE 8

typedef struct{
    uint64_t state;
} lcg_t;

/*********************** FUNCTION DECLARATIONS **********************/

/**
 * @brief initialize the lcg state with seed
 * 
 * @param seed the seed to initialize the state, it must be of 8 bytes
 * @param lcg the lcg struct
 * @return 1 if initializing was successful
 */
size_t init_lcg(const uint8_t seed[LCG_OUTPUT_SIZE], lcg_t * lcg);


/**
 * @brief get 8 random bytes as a uint64_t from lcg
 * 
 * @param lcg the lcg struct
 * @return the uint64_t output from lcg
 */
uint64_t next_64bits(lcg_t* lcg);

/**
 * @brief get random bytes from lcg
 * 
 * @param output_buffer the output buffer where to store the random bytes
 * @param output_length number of random bytes to generate, must be multiple of 8
 * @param lcg the lcg struct
 * @return 1 if random generation was successful, 0 if not 
 */
size_t generate_randomness_lcg(uint8_t * output_buffer, const uint32_t output_length, lcg_t * lcg);

#endif   // LCG_H
