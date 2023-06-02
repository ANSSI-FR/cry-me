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

#ifndef GF25519_H
#define GF25519_H

#include <stdint.h> // int32_t etc

//------------------------------------------------------------------------------
// Data Structure

/**
 * A GF(2^255-19) element
 *    An element t, entries t[0]...t[9], represents the integer
 *    t[0]+2^26 t[1]+2^51 t[2]+2^77 t[3]+2^102 t[4]+...+2^230 t[9].
 *    Bounds on each t[i] vary depending on context.
 */
typedef int32_t gf25519e_t[10];

// Constant 1
extern const gf25519e_t GF25519_ONE;

//------------------------------------------------------------------------------
// Getters and setters

/**
 * @brief Check if x == 0.
 * 
 * @param x the tested value
 * @return 1 if x == 0, 0 otherwise
 */
char gf25519_iszero(const gf25519e_t x);

/**
 * @brief Set "x = 0".
 * 
 * @param x the value to modify
 */
void gf25519_setzero(gf25519e_t x);

/**
 * @brief Set "x = 1".
 * 
 * @param x the value to modify
 */
void gf25519_setone(gf25519e_t x);

//------------------------------------------------------------------------------
// Math Operations

/**
 * @brief Perform "z = x" (copy).
 * 
 * @param x the value to copy
 * @param z the copied value
 */
void gf25519_copy(gf25519e_t z, const gf25519e_t x);

/**
 * @brief Perform "z = -x".
 * 
 * @param x the input of the computation
 * @param z the result
 */
void gf25519_neg(gf25519e_t z, const gf25519e_t x);

/**
 * @brief Perform "z = x + y".
 * 
 *    Assuming
 *        |x[]| bounded by 2^(26+m),2^(25+m),2^(26+m),2^(25+m),etc.
 *        |y[]| bounded by 2^(26+m),2^(25+m),2^(26+m),2^(25+m),etc.
 * 
 *    Requirement: The number m of extra bits must be less than or equal to 4 (m<=4).
 * 
 *    Output:
 *        |z[]| bounded by 2^(26+m+1),2^(25+m+1),2^(26+m+1),2^(25+m+1),etc.
 * 
 * @param x one input of the computation
 * @param y one input of the computation
 * @param z the result
 */
void gf25519_add(gf25519e_t z, const gf25519e_t x, const gf25519e_t y);

/**
 * @brief Perform "z = x - y".
 * 
 *    Assuming
 *        |x[]| bounded by 2^(26+m),2^(25+m),2^(26+m),2^(25+m),etc.
 *        |y[]| bounded by 2^(26+m),2^(25+m),2^(26+m),2^(25+m),etc.
 * 
 *    Requirement: The number m of extra bits must be less than or equal to 4 (m<=4).
 * 
 *    Output:
 *        |z[]| bounded by 2^(26+m+1),2^(25+m+1),2^(26+m+1),2^(25+m+1),etc.
 * 
 * @param x one input of the computation
 * @param y one input of the computation
 * @param z the result
 */
void gf25519_sub(gf25519e_t z, const gf25519e_t x, const gf25519e_t y);

/**
 * @brief Perform "z = x * y".
 * 
 *    Assuming
 *        |x[]| bounded by 2^(26+m),2^(25+m),2^(26+m),2^(25+m),etc.
 *        |y[]| bounded by 2^(26+m),2^(25+m),2^(26+m),2^(25+m),etc.
 * 
 *    Requirement: The number m of extra bits must be less than or equal to 3 (m<=3).
 * 
 *    Output:
 *        |z[]| bounded by 2^26,2^25,2^26,2^25,etc.
 * 
 * @param x one input of the computation
 * @param y one input of the computation
 * @param z the result
 */
void gf25519_mul(gf25519e_t z, const gf25519e_t x, const gf25519e_t y);

/**
 * @brief Perform "z = x^2".
 * 
 *    Assuming
 *        |x[]| bounded by 2^(26+m),2^(25+m),2^(26+m),2^(25+m),etc.
 * 
 *    Requirement: The number m of extra bits must be less than or equal to 3 (m<=3).
 * 
 *    Output:
 *        |z[]| bounded by 2^26,2^25,2^26,2^25,etc.
 * 
 * @param x the input of the computation
 * @param z the result
 */
void gf25519_squ(gf25519e_t z, const gf25519e_t x);

/**
 * @brief Perform "z = 1/x".
 * 
 *    Assuming
 *        |x[]| bounded by 2^(26+m),2^(25+m),2^(26+m),2^(25+m),etc.
 * 
 *    Requirement: The number m of extra bits must be less than or equal to 3 (m<=3).
 * 
 *    Output:
 *        |z[]| bounded by 2^26,2^25,2^26,2^25,etc.
 * 
 *    Note: if x = 0, then z = 0
 * 
 * @param x the input of the computation
 * @param z the result
 */
void gf25519_inv(gf25519e_t z, const gf25519e_t x);

/**
 * @brief Perform "z = 2*x".
 * 
 *    Assuming
 *        |x[]| bounded by 2^(26+m),2^(25+m),2^(26+m),2^(25+m),etc.
 * 
 *    Requirement: The number m of extra bits must be less than or equal to 4 (m<=4).
 * 
 *    Output:
 *        |z[]| bounded by 2^(26+1),2^(25+1),2^(26+1),2^(26+1),etc.
 * 
 * @param x the input of the computation
 * @param z the result
 */
void gf25519_dbl(gf25519e_t z, const gf25519e_t x);

/**
 * @brief Perform "z = 3*x".
 * 
 *    Assuming
 *        |x[]| bounded by 2^(26+m),2^(25+m),2^(26+m),2^(25+m),etc.
 * 
 *    Requirement: The number m of extra bits must be less than or equal to 3 (m<=3).
 * 
 *    Output:
 *        |z[]| bounded by 2^(26+1),2^(25+1),2^(26+1),2^(26+1),etc.
 * 
 * @param x the input of the computation
 * @param z the result
 */
void gf25519_trp(gf25519e_t z, const gf25519e_t x);

//------------------------------------------------------------------------------
// Serialization

/// Size in bytes of a serialized GF(2^255-19) element
#define GF25519_BYTESIZE 32

/**
 * @brief Serialize a GF(2^255-19) element.
 * 
 *    The field element "x" is represented as
 *        stream[0]*256^31 + stream[1]*256^30 + ... + stream[30]*256 + stream[31]
 *    such that the above sum is an
 *    integer between 0 and PRIME-1.
 * 
 *    Note: The leading bit of "stream" will always be zero.
 * 
 * @param x the value to serialized
 * @param stream the serialized value
 */
void from_gf25519e(uint8_t* stream, const gf25519e_t x);

/**
 * @brief Deserialize a GF(2^255-19) element.
 * 
 *    Inverse operation of "from_gf25519e"
 * 
 *    Note: The leading bit of "stream" is ignored
 * 
 * @param x the deserialized value
 * @param stream the value to deserialize
 */
void to_gf25519e(gf25519e_t x, const uint8_t* stream);

//------------------------------------------------------------------------------
// Misc Operations

/**
 * @brief Controled swap.
 * 
 *    If bit = 0, do nothing
 *    If bit = 1, swap x and y (x <-> y)
 * 
 *    Note: Constant-time implementation
 * 
 * @param x one of the value to swap
 * @param y the other value to swap
 * @param bit swap controler
 */
void gf25519_swap(gf25519e_t x, gf25519e_t y, int bit);

/**
 * @brief Select a field element.
 * 
 *    If bit = 0, set z as x (z <- x).
 *    If bit = 1, set z as y (z <- y).
 * 
 *    Note: Constant-time implementation
 * 
 * @param x one of the value to select
 * @param y one of the value to select
 * @param z the output
 * @param bit select bit
 */
void gf25519_select(gf25519e_t z, const gf25519e_t x, const gf25519e_t y, int bit);

/**
 * @brief Print the GF(2^255-19) element 
 *    as a number between 0 and PRIME-1
 *    written in hexadecimal.
 * 
 * @param z the value to print
 */
void gf25519_print(const gf25519e_t z);

#endif   // GF25519_H
