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
#include <stdlib.h>
#include <string.h>

/**
 * CRY.ME.VULN.21
 *
 * The nounce generation for the EC Schnorr Signature depends on a
 *    Linear Congruential Generator
 * which is a non-cryptographic pseudorandom number generator.
 */
#include "../lcg/lcg.h"
#define LCG_EXIT_SUCCESS 1
#define LCG_FAILURE 3

/**
 * @brief Sample a 256-bit nounce using a LCG
 * 
 * @param nounce the output 256-bit nounce
 * @param lcg the LCG structure
 */
void sample_nounce(uint8_t nounce[32], lcg_t* lcg) {
    // Fill the four 64-bit chunks of pseudo-randomness
    uint64_t chunks[4];
    chunks[0] = next_64bits(lcg);
    chunks[1] = next_64bits(lcg);
    chunks[2] = next_64bits(lcg);
    chunks[3] = next_64bits(lcg);

    // Concatenate the chunks to get the nounce
    uint64_t* nounce64 = (uint64_t*) nounce;
    nounce64[0] = chunks[3];
    nounce64[1] = chunks[2];
    /**
     * CRY.ME.VULN.20
     *
     * Implementation Bug.
     * The next line should be:
     *    nounce64[2] = chunks[1];
     */
    nounce64[2] = chunks[2];
    nounce64[3] = chunks[0];
}

// Check that x == y in constant time
static int consttime_equal(const uint8_t* x, const uint8_t* y, int bytesize) {
    uint8_t r = 0;
    for(int i=0; i<bytesize; i++)
        r |= x[i] ^ y[i];
    return r;
}

/**
 * @brief EC Schnorr Signature. Sign the message "m" of length "mlen"
 *      using the secret key "sk" and the randomness from "rnd_engine".
 * 
 * @param sig the produced signature
 * @param m the message to sign
 * @param mlen the length of the message (in bytes)
 * @param sk the secret key used to sign
 * @param rnd_engine the structure from which the randomness is read
 * @return 0 if the signing is successful, a non-zero integer otherwise
 */
int wei25519_sign(unsigned char* sig, const unsigned char* m, unsigned long long mlen, const unsigned char* sk, rnd_engine_t* rnd_engine) {
    uint8_t k[SCALAR_BYTESIZE];
    uint8_t r[SCALAR_BYTESIZE];
    uint8_t r_red[SCALAR_BYTESIZE];
    uint8_t s[SCALAR_BYTESIZE];

    uint8_t stream[2*GF25519_BYTESIZE];
    wei25519e3_t point;
    int res;

    // Initialize the LCG for nounces
    // Get seed
    uint8_t lcg_seed[8];
    res = read_randomness(lcg_seed, rnd_engine, 8);
    if(res != EXIT_SUCCESS)
        return res;
    // Init LCG
    lcg_t lcg;
    res = init_lcg(lcg_seed, &lcg);
    if(res != LCG_EXIT_SUCCESS)
        return LCG_FAILURE;

    do {
        // Sample k uniformly between 1 and n-1, where n is the curve order
        do {
            sample_nounce(k, &lcg);
        } while( (!wei25519_is_reduced_scalar(k)) || wei25519_is_null_scalar(k));
        
        // Performs "Q = [k] G" where G is a curve generator
        wei25519_scalarmult_base(&point, k, SCALAR_BYTESIZE);

        // Compute H(FE2OS(x_Q) || FE2OS(y_Q) || M)
        #if SCALAR_BYTESIZE != SHA3_DIGEST_BYTELEN
            #error "The current implementation requires that the size of the hash digest corresponds to the size of the scalars."
        #endif
        struct sha3_struct hash_ctx;
        construct_sha3(&hash_ctx);
        sha3_init(&hash_ctx);
        // Append "FE2OS(x_Q) || FE2OS(y_Q)"
        wei25519_serialize_point(stream, &point);
        sha3_update(&hash_ctx, stream, 2*GF25519_BYTESIZE);
        // Append message M
        sha3_update(&hash_ctx, m, mlen);
        // Compute the hash digest, which corresponds to the scalar r
        sha3_final(&hash_ctx, r);

        // Reduce the scalar r
        // We keep the unreduced form of r since,
        //    according to "BSI TR-03111 Elliptic Curve Cryptography",
        //    the scalar r of the signature is not reduced.
        wei25519_copy_scalar(r_red, r);
        wei25519_reduce_scalar(r_red);
        // If r=0 mod n, restart the signing
        if(wei25519_is_null_scalar(r_red)){
            continue;
        }
        // s = k + sk*r
        wei25519_muladd_scalars(s, r, sk, k);
        wei25519_reduce_scalar(s);
        if(wei25519_is_null_scalar(s)){
            continue;
        }
	else{
           // If we are here, everything is OK: quit the loop
           break;
        }
    } while(1);

    // Signature = (r, s)
    memcpy(sig, r, SCALAR_BYTESIZE);
    memcpy(sig+SCALAR_BYTESIZE, s, SCALAR_BYTESIZE);
    return EXIT_SUCCESS;
}

/**
 * @brief EC Schnorr Signature - Verification. Verify that a signature corresponds
 *      to a given message "m" and to a public key "pk".
 * 
 * @param sig the checked signature
 * @param m the message
 * @param mlen the length of the message (in bytes)
 * @param pk the public key
 * @return 0 if the signature checking is successful, a non-zero integer otherwise
 */
int wei25519_verify(const unsigned char* sig, const unsigned char* m, unsigned long long mlen, const unsigned char* pk) {
    uint8_t r[SCALAR_BYTESIZE];
    uint8_t r_red[SCALAR_BYTESIZE];
    uint8_t v[SCALAR_BYTESIZE];
    uint8_t s[SCALAR_BYTESIZE];

    // Signature = (r, s)
    memcpy(r, sig, SCALAR_BYTESIZE);
    memcpy(s, sig+SCALAR_BYTESIZE, SCALAR_BYTESIZE);

    // Check if s is between 1 and n-1, where n is the curve order
    if((!wei25519_is_reduced_scalar(s)) || wei25519_is_null_scalar(s))
        return EXIT_FAILURE;

    // Check if r is not zero mod n
    wei25519_copy_scalar(r_red, r);
    wei25519_reduce_scalar(r_red);
    if(wei25519_is_null_scalar(r_red))
        return EXIT_FAILURE;

    // Perform "Q = [s] G - [r] P_A"
    wei25519e3_t point, point1, point2, point_pk;
    int res = wei25519_deserialize_point(&point_pk, pk);
    if(res != EXIT_SUCCESS)
        return res; // Meaning that the point "pk" is not valid
    wei25519_scalarmult_base(&point1, s, SCALAR_BYTESIZE);
    wei25519_scalarmult(&point2, &point_pk, r, SCALAR_BYTESIZE);
    wei25519_neg(&point2, &point2);
    wei25519_adding(&point, &point1, &point2);

    // Check if Q is the point at infinity
    if(wei25519_iszero(&point))
        return EXIT_FAILURE;

    // Compute H(FE2OS(x_Q) || FE2OS(y_Q) || M)
    struct sha3_struct hash_ctx;
    construct_sha3(&hash_ctx);
    sha3_init(&hash_ctx);
    uint8_t stream[2*GF25519_BYTESIZE];
    // Append "FE2OS(x_Q) || FE2OS(y_Q)"
    wei25519_serialize_point(stream, &point);
    sha3_update(&hash_ctx, stream, 2*GF25519_BYTESIZE);
    // Append message
    sha3_update(&hash_ctx, m, mlen);
    sha3_final(&hash_ctx, v);

    // Check if v = r
    if(consttime_equal(r, v, SCALAR_BYTESIZE) != 0)
        return EXIT_FAILURE;

    return EXIT_SUCCESS;
}
