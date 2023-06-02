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

#ifndef RND_H
#define RND_H

#include <stdlib.h>
#include <stdint.h>
#include <string.h>

/// The error code when there is no enough randomness in the buffer
#define NO_ENOUGH_RANDOMNESS 2

// Data structure for randomness
typedef struct rnd_engine_t {
    uint8_t const * random_buffer; // Buffer filled by random bytes
    uint64_t random_buffer_length; // Size of the random buffer
    uint64_t random_index; // Index of the first unused random byte
} rnd_engine_t;

/**
 * @brief Read "byetsize" bytes of randomness from rnd_engine and copy them in "stream".
 * 
 * @param stream the buffer where the read randomness is copied
 * @param rnd_engine the random engine from which the randomness is read
 * @param bytesize the number of read bytes
 * 
 * @return 0 if successful, a non-zero integer otherwise
 */
inline int read_randomness(uint8_t* stream, rnd_engine_t* rnd_engine, uint64_t bytesize) {
    if(rnd_engine->random_index+bytesize > rnd_engine->random_buffer_length)
        return NO_ENOUGH_RANDOMNESS;

    memcpy(
        stream,
        rnd_engine->random_buffer + rnd_engine->random_index,
        bytesize
    );
    rnd_engine->random_index += bytesize;
    return EXIT_SUCCESS;
}

#endif /* RND_H */

