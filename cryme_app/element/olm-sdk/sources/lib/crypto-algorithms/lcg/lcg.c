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

/*************************** HEADER FILES ***************************/
#include <stdlib.h>
#include <memory.h>
#include "lcg.h"


// LCG constants a and c
#define LCG_A ((uint64_t)6364136223846793005)
#define LCG_C ((uint64_t)1442695040888963407)

/*********************** FUNCTIONS ***********************/


// The following two functions to convert between uint64_t and 
// uint8_t * are needed only because the incoming format from 
// olm is in uint8_t * , while the operations performed for LCG
// are over uint64_t for better performance. They are only used
// once to convert the initializing seed, and then to convert
// the output of LCG to the correct uint8_t format.

// this function is only called when initializing the LCG seed
static inline uint64_t get_uint64_t(const uint8_t v[LCG_OUTPUT_SIZE]){
    return  (((uint64_t)v[0] << 56) | ((uint64_t)v[1] << 48)) |
            (((uint64_t)v[2] << 40) | ((uint64_t)v[3] << 32)) |
            (((uint64_t)v[4] << 24) | ((uint64_t)v[5] << 16)) |
            (((uint64_t)v[6] << 8)  | (v[7]));
}

static inline void from_uint64_t(const uint64_t v, uint8_t out[LCG_OUTPUT_SIZE]){
    out[0] = (uint8_t)(v >> 56); out[4] = (uint8_t)(v >> 24);
    out[1] = (uint8_t)(v >> 48); out[5] = (uint8_t)(v >> 16);
    out[2] = (uint8_t)(v >> 40); out[6] = (uint8_t)(v >> 8);
    out[3] = (uint8_t)(v >> 32); out[7] = (uint8_t)(v);
}

static inline uint64_t update_state_lcg(lcg_t * lcg){
    uint64_t x = LCG_A * lcg->state + LCG_C;
    lcg->state = LCG_A * x + LCG_C;
    return x;
}

size_t init_lcg(const uint8_t seed[LCG_OUTPUT_SIZE], lcg_t * lcg){
    lcg->state = get_uint64_t(seed);
    return 1;
}

uint64_t next_64bits(lcg_t* lcg){
    return update_state_lcg(lcg);
}

size_t generate_randomness_lcg(uint8_t * output_buffer, const uint32_t output_length, lcg_t * lcg){

    // impose that output_length be a multiple of 8
    // this is OK since its usage in the application is for generating
    // multiples of 8 random bytes
    if((output_length & 7) != 0){
        return 0;
    }
    
    memset(output_buffer, 0, output_length*sizeof(uint8_t));

    for(size_t i=0; i< (output_length / LCG_OUTPUT_SIZE); i++){
        from_uint64_t(update_state_lcg(lcg), output_buffer + i*LCG_OUTPUT_SIZE);
    }

    return 1;
}
