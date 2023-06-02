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

#ifndef ECC_SCALAR_H
#define ECC_SCALAR_H

#include <stdint.h> // int32_t etc
#include "rnd.h" // rnd_engine_t

//------------------------------------------------------------------------------
// Data Structure

// The order of the Wei25519 curve
//    n = 2^255 + 221938542218978828286815502327069187944
#define WEI25519_ORDER_BYTESIZE 32
extern const uint8_t WEI25519_ORDER[WEI25519_ORDER_BYTESIZE];

/*
 * A element of Z/nZ where n = (2^255 + 221938542218978828286815502327069187944)
 *  n is the order of the curve Wei25519
 *
 *    An element t, entries t[0]...t[31], represents the integer
 *    256^31 t[0] + 256^30 t[1] + 256^29 t[2] + ... + 256^1 t[30] + 256^0 t[31].
 */
#define SCALAR_BYTESIZE (WEI25519_ORDER_BYTESIZE)
typedef uint8_t scalar_t[SCALAR_BYTESIZE];

//------------------------------------------------------------------------------
// Manipulation

/**
 * @brief Reduce the Z/nZ element "a" into a representation between 0 and n-1.
 * 
 * @param a the scalar to reduce
 */
void wei25519_reduce_scalar(scalar_t a);

/**
 * @brief Check if the scalar "a" is in the reduced form (i.e. between 0 and n-1).
 * 
 * @param a the scalar to check
 * @return 1 if a is in the reduced form, 0 otherwise
 */
int wei25519_is_reduced_scalar(const scalar_t a);

/**
 * @brief Check if a is zero (a == 0).
 *     
 *    Requirement: require that a is in the reduced form.
 * 
 * @param a the tested scalar
 * @return 1 if a == 0, 0 otherwise
 */
int wei25519_is_null_scalar(const scalar_t a);

//------------------------------------------------------------------------------
// Math Operations

/**
 * @brief Perform "out = in" (copy).
 * 
 * @param in the scalar to copy
 * @param out the copied scalar
 */
void wei25519_copy_scalar(scalar_t out, const scalar_t in);

/**
 * @brief Perform "s = (ab + c) mod n".
 * 
 *    Note: the inputs can be unreduced.
 *    Note: the output s is not necessary in the reduced form.
 * 
 * @param a one input of the computation
 * @param b one input of the computation
 * @param c one input of the computation
 * @param s the result
 */
void wei25519_muladd_scalars(scalar_t s, const scalar_t a, const scalar_t b, const scalar_t c);

//------------------------------------------------------------------------------
// Randomness

/**
 * @brief Get a uniformly random scalar of size "sc_bytesize".
 * 
 *      If 256^sb < n, where sb:="sc_bytesize",
 *                 sample a scalar between 1 and 256^sb-1.
 *      Otherwise, sample a scalar between 1 and n-1.
 * 
 *    Note: the returned scalar is reduced
 * 
 * @param sc the sampled scalar
 * @param sc_bytesize the size "sb" of the sampled scalar
 * @param rnd_engine the structure which contains the randomness
 * @return 0 if the sampling is successful, a non-zero integer otherwise
 */
int wei25519_random_scalar(scalar_t sc, unsigned int sc_bytesize, rnd_engine_t* rnd_engine);

#endif /* ECC_SCALAR_H */
