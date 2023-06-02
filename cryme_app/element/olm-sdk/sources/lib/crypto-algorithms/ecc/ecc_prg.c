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

#include "ecc.h"
#include "../sha/sha3.h"

// Some requirements
#if SCALAR_BYTESIZE < 2*WEI25519_SECURITYLEVEL_IN_BYTES
    #error "The curve order is not enough big"
#endif
#if SCALAR_BYTESIZE != SHA3_DIGEST_BYTELEN
    #error "The current implementation requires that the size of the hash digest corresponds to the size of the scalars."
#endif

// Output block length (in bytes)
// It must be lower than the maximum (in bits) defined as
//     "Size of the base field" - 13 - log2(cofactor)
// Here: the maximum is of 255-13-log2(8) = 239 bits
#define MAX_OUTLEN_IN_BYTES 29
#define OUTLEN_IN_BYTES 29
// Number of blocks between reseeding
#define RESEED_INTERVAL (((uint64_t) 1L)<<32)
// Maximum additional input length (in bytes)
#define MAX_ADDITIONAL_INPUT_LENGTH ((1<<13)>>3)
// Maximum entropy input length (in bytes)
#define MAX_ENTROPY_INPUT_LENGTH ((1<<13)>>3)

// The curve point P used to advance the PRG internal state
const wei25519e3_t EC_DRBG_POINT_P = {
    {
        38481061, 24762182, 51235539, 26043171, 21599696, 15697609, 2131347, 15855472, 57934318, 17140504
    },
    {
        47141639, 20664186, 63491268, 4933749, 56369229, 24569334, 63296774, 7961700, 29028407, 20161512
    },
    {
        1, 0, 0, 0, 0, 0, 0, 0, 0, 0
    }
};

// The curve point Q used to build pseudo-random bit from the PRG internal state
// The point Q has been computed as d*P, where the scalar d is a 64-bit integer for performance reasons.
const wei25519e3_t EC_DRBG_POINT_Q = {
    {
        50877999, 3022384, 1433342, 32497034, 38800396, 31219013, 883077, 23928139, 66888023, 33535515
    },
    {
        34201125, 26125611, 33432603, 24840736, 41920859, 21999110, 13769184, 6630419, 16198914, 11595907
    },
    {
        1, 0, 0, 0, 0, 0, 0, 0, 0, 0
    }
};

// Hash Derivation Function Hash_df
// In the case of the Dual EC DRBG, we use Hash_df only with
//          a output of 256 bits.
// Therefore, we have
//     Hash_df(txt, 256 bits) = Hash(    1   ||   256   || txt ).
//                                   --------  ---------
//                                    8 bits    32 bits
// By defining "hash_df_prefix_for_256_bits" as below, we have
//     Hash_df(txt, 256 bits) = Hash( hash_df_prefix_for_256_bits || txt ).
#define HASH_DF_PREFIX_BYTESIZE 5
static uint8_t hash_df_prefix_for_256_bits[HASH_DF_PREFIX_BYTESIZE] = {1, 0, 0, 1, 0};

/**
 * @brief Get the minimal size in bytes of needed entropy for PRG initialization
 *
 * @return the size in bytes
 */
uint32_t wei25519_prg_entropy_size() {
    return 2*WEI25519_SECURITYLEVEL_IN_BYTES;
}

/**
 * @brief Dual EC DRBG. Initialize the PRG state with some entropy.
 * 
 *   Require that "entropy_input" has a length at least twice of the security level.
 * 
 * @param prg the PRG state to initialize
 * @param entropy_input the entropy input used to initialize the PRG
 * @param entropy_input_bytesize the length of the entropy input (ie its size in bytes)
 * @param nounce a nounce which will be mixed with the PRG state
 * @param nounce_bytesize the length of the nounce (ie its size in bytes)
 * @param additional_input an additional input string which will be mixed with the PRG state
 * @param additional_input_bytesize the length of the additional input (ie its size in bytes)
 */
int wei25519_prg_init_with_additional_input(prg_t* prg, const uint8_t* entropy_input, size_t entropy_input_bytesize, const uint8_t* nounce, size_t nounce_bytesize, const uint8_t* additional_input, size_t additional_input_bytesize) {
    // Sanity checks
    if((entropy_input_bytesize < (2*WEI25519_SECURITYLEVEL_IN_BYTES)) || (entropy_input_bytesize > MAX_ENTROPY_INPUT_LENGTH))
        return EXIT_FAILURE;
    if(additional_input_bytesize > MAX_ADDITIONAL_INPUT_LENGTH)
        return EXIT_FAILURE;

    // For the internal state,
    //   hash the entropy input, the nounce and the additional input
    // We use the fact (see definition of "hash_df_prefix_for_256_bits") that
    //   Hash_df(txt, 256 bits) = Hash( hash_df_prefix_for_256_bits || txt ).
    struct sha3_struct hash_ctx;
    construct_sha3(&hash_ctx);
    sha3_init(&hash_ctx);
    sha3_update(&hash_ctx, hash_df_prefix_for_256_bits, HASH_DF_PREFIX_BYTESIZE);
    sha3_update(&hash_ctx, entropy_input, entropy_input_bytesize);
    sha3_update(&hash_ctx, nounce, nounce_bytesize);
    sha3_update(&hash_ctx, additional_input, additional_input_bytesize);
    sha3_final(&hash_ctx, prg->state);

    // Initialize the counter
    prg->reseed_counter = 0;
    return EXIT_SUCCESS;
}

/**
 * @brief Dual EC DRBG. Reseed the PRG state with some entropy.
 * 
 *   Require that "entropy_input" has a length at least of the security level.
 *
 * @param prg the PRG state to reseed
 * @param entropy_input the entropy input used to reseed the PRG
 * @param entropy_input_bytesize the length of the entropy input (ie its size in bytes)
 * @param additional_input an additional input string which will be mixed with the PRG state
 * @param additional_input_bytesize the length of the additional input (ie its size in bytes)
 */
int wei25519_prg_reseed_with_additional_input(prg_t* prg, const uint8_t* entropy_input, size_t entropy_input_bytesize, const uint8_t* additional_input, size_t additional_input_bytesize) {
    // Sanity checks
    if((entropy_input_bytesize < WEI25519_SECURITYLEVEL_IN_BYTES) || (entropy_input_bytesize > MAX_ENTROPY_INPUT_LENGTH))
        return EXIT_FAILURE;
    if(additional_input_bytesize > MAX_ADDITIONAL_INPUT_LENGTH)
        return EXIT_FAILURE;

    // For the internal state,
    //   hash the entropy input and the additional input with the previous state
    // We use the fact (see definition of "hash_df_prefix_for_256_bits") that
    //   Hash_df(txt, 256 bits) = Hash( hash_df_prefix_for_256_bits || txt ).
    struct sha3_struct hash_ctx;
    construct_sha3(&hash_ctx);
    sha3_init(&hash_ctx);
    sha3_update(&hash_ctx, hash_df_prefix_for_256_bits, HASH_DF_PREFIX_BYTESIZE);
    sha3_update(&hash_ctx, prg->state, SCALAR_BYTESIZE);
    sha3_update(&hash_ctx, entropy_input, entropy_input_bytesize);
    sha3_update(&hash_ctx, additional_input, additional_input_bytesize);
    sha3_final(&hash_ctx, prg->state);
    // Reset the counter
    prg->reseed_counter = 0;
    return EXIT_SUCCESS;
}

/**
 * @brief Dual EC DRBG. Check if the PRG needs to be reseeded to
 *              get "nb_of_bytes" bytes of pseudo-randomness.
 * 
 * @param prg the PRG state
 * @param nb_of_bytes the number of bytes
 * @return 1 if the PRG needs to be reseeded, 0 otherwise
 */
int wei25519_prg_needs_to_reseed(const prg_t* prg, size_t nb_of_bytes) {
    // Compute the number of iterations
    size_t nb_iterations = ((nb_of_bytes + OUTLEN_IN_BYTES) / OUTLEN_IN_BYTES);

    // Check whether a reseed is required
    if(((uint64_t)(prg->reseed_counter + nb_iterations)) > RESEED_INTERVAL)
        return 1;

    return 0;
}

/**
 * @brief Dual EC DRBG. Fill the buffer with pseudo-random bytes.
 * 
 * @param prg the PRG state
 * @param buffer the buffer to fill with randomness
 * @param buffer_bytesize the length of the buffer (ie its size in bytes)
 * @param add_input_str an additional input string which will be mixed with the PRG state
 * @param add_input_str_bytesize the length of the additional input (ie its size in bytes)
 * @return 0 if the key generation is successful, a non-zero integer otherwise (ERROR_NEED_TO_RESEED if the PRG needs to be reseeded)
 */
int wei25519_prg_sample_with_additional_input(prg_t* prg, uint8_t* buffer, size_t buffer_bytesize, const uint8_t* add_input_str, size_t add_input_str_bytesize) {
    wei25519e3_t point;
    uint8_t r[SCALAR_BYTESIZE];

    // Sanity check
    if(((uint64_t) buffer_bytesize) > RESEED_INTERVAL*MAX_OUTLEN_IN_BYTES)
        return EXIT_FAILURE;

    // Compute the number of iterations
    size_t nb_iterations = ((buffer_bytesize + OUTLEN_IN_BYTES) / OUTLEN_IN_BYTES);

    // Check whether a reseed is required
    if(wei25519_prg_needs_to_reseed(prg, buffer_bytesize))
        return ERROR_NEED_TO_RESEED;

    // Prepare the additional input
    uint8_t add_input[SCALAR_BYTESIZE];
    if(add_input_str_bytesize > MAX_ADDITIONAL_INPUT_LENGTH)
        return EXIT_FAILURE;
    if(add_input_str_bytesize > 0) {
        // We use the fact (see definition of "hash_df_prefix_for_256_bits") that
        //   Hash_df(txt, 256 bits) = Hash( hash_df_prefix_for_256_bits || txt ).
        struct sha3_struct hash_ctx;
        construct_sha3(&hash_ctx);
        sha3_init(&hash_ctx);
        sha3_update(&hash_ctx, hash_df_prefix_for_256_bits, HASH_DF_PREFIX_BYTESIZE);
        sha3_update(&hash_ctx, add_input_str, add_input_str_bytesize);
        sha3_final(&hash_ctx, add_input);
    }

    for(size_t i=0; i<nb_iterations; i++) {
        // Add the additional input only at the first iteration
        if((i==0) && (add_input_str_bytesize>0))
            for(int j=0; j<SCALAR_BYTESIZE; j++)
                prg->state[j] ^= add_input[j];

        // s = phi( x( t*P ) )
        wei25519_scalarmult(&point, &EC_DRBG_POINT_P, prg->state, SCALAR_BYTESIZE);
        wei25519_normalize_point(&point);
        from_gf25519e(prg->state, point.X);

        // r = phi( x( s*Q ) )
        wei25519_scalarmult(&point, &EC_DRBG_POINT_Q, prg->state, SCALAR_BYTESIZE);
        wei25519_normalize_point(&point);
        from_gf25519e(r, point.X);

        // Copy the needed bytes in the output buffer
        size_t pos = i*OUTLEN_IN_BYTES;
        size_t nb_used_bytes = (i<(nb_iterations-1)) ? OUTLEN_IN_BYTES : buffer_bytesize-pos;
        memcpy(
            buffer+pos,
            r+(SCALAR_BYTESIZE-OUTLEN_IN_BYTES),
            nb_used_bytes
        );

        // Increment the reseed counter
        prg->reseed_counter++;
    }

    // Advance the internal state one more time
    wei25519_scalarmult(&point, &EC_DRBG_POINT_P, prg->state, SCALAR_BYTESIZE);
    wei25519_normalize_point(&point);
    from_gf25519e(prg->state, point.X);

    return EXIT_SUCCESS;
}
