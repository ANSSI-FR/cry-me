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

#ifndef WEI25519_H
#define WEI25519_H

//------------------------------------------------------------------------------
// Field Arithmetic

// Import field GF(2^255-19) arithmetic.
#include "gf25519.h"

//------------------------------------------------------------------------------
// Curve definition and data structure

// Elliptic Curve 
// Short Weierstrass Form: y^2 = x^3 + ax + b
extern const gf25519e_t WEI25519_A; // The constant a
extern const gf25519e_t WEI25519_B; // The constant b

// Data Structure for curve point in projective coordinates (X:Y:Z)
// We assume that all the points represented by this structure is a
//    valid point (ie a point on the curve).
typedef struct {
  gf25519e_t X;
  gf25519e_t Y;
  gf25519e_t Z;
} wei25519e3_t;

// A generator of the curve group
extern const wei25519e3_t WEI25519_BASEPOINT;

// Import field GF(n) arithmetic
//    where n is the order of the curve
#include "scalar.h"

//------------------------------------------------------------------------------
// Getters and setters

/**
 * @brief Check if point == 0, where 0 is the point at infinity.
 * 
 * @param point the tested point
 * @return 1 if point == 0, 0 otherwise
 */
char wei25519_iszero(const wei25519e3_t* point);

/**
 * @brief Set "point = 0", where 0 is the point at infinity.
 * 
 * @param point the point to modify
 */
void wei25519_setzero(wei25519e3_t* point);

//------------------------------------------------------------------------------
// Group Operations

/**
 * @brief Perform "R = P" (copy).
 * 
 * @param ptP the value to copy
 * @param ptR the copied value
 */
void wei25519_copy(wei25519e3_t* ptR, const wei25519e3_t* ptP);

/**
 * @brief Perform "R = - P" on the curve Wei25519.
 * 
 * @param ptP the point P
 * @param ptR the point R
 */
void wei25519_neg(wei25519e3_t* ptR, const wei25519e3_t* ptP);

/**
 * @brief Perform "R = 2*P" on the curve Wei25519.
 * 
 * @param ptP the point P
 * @param ptR the point R
 */
void wei25519_doubling(wei25519e3_t* ptR, const wei25519e3_t* ptP);

/**
 * @brief Perform "R = P + Q" on the curve Wei25519.
 * 
 *    Requirement: The points P and Q must not be equal, P != Q,
 *            except if P == Q == 0.
 * 
 * @param ptP the point P
 * @param ptQ the point Q
 * @param ptR the point R
 */
void wei25519_adding(wei25519e3_t* ptR, const wei25519e3_t* ptP, const wei25519e3_t* ptQ);

/**
 * @brief Perform "R = sc*P" on the curve Wei25519.
 * 
 *   Note: Constant-time implementation
 * 
 * @param base the point P
 * @param scalar the scalar sc
 * @param scalar_bytelen the length (in bytes) of the scalar
 * @param out the point R
 */
void wei25519_scalarmult(wei25519e3_t* out, const wei25519e3_t* base, const unsigned char *scalar, int scalar_bytelen);

/**
 * @brief Perform "R = sc*G" on the curve Wei25519
 *          where G is a generator of the curve given by the constant WEI25519_BASEPOINT.
 *
 *   Note: Constant-time implementation
 * 
 * @param scalar the scalar sc
 * @param scalar_bytelen the length (in bytes) of the scalar
 * @param out the point R
 */
#define wei25519_scalarmult_base(out, sc, sc_bytelen) wei25519_scalarmult(out, &WEI25519_BASEPOINT, sc, sc_bytelen)

//------------------------------------------------------------------------------
// Serialization

/**
 * @brief Serialize a curve point (x,y).
 * 
 *    The coordinate "x" is represented as
 *        stream[0]*256^31 + stream[1]*256^30 + ... + stream[30]*256 + stream[31]
 *    such that the above sum is an
 *    integer between 0 and PRIME-1.
 * 
 *    The coordinate "y" is represented as
 *        stream[32]*256^31 + stream[33]*256^30 + ... + stream[63]*256 + stream[63]
 *    such that the above sum is an
 *    integer between 0 and PRIME-1.
 * 
 *    Warning: The point at the infinity is not supported by the serialization.
 * 
 * @param point the point to serialized
 * @param stream the serialized point
 */
void wei25519_serialize_point(uint8_t* stream, const wei25519e3_t* point);

/**
 * @brief Deserialize a curve point.
 * 
 *    Inverse operation of "wei25519_serialize_point".
 *    It also checks that the deserialized point belongs to the curve.
 * 
 * @param point the deserialized point
 * @param stream the point to deserialize
 * @return 0 if the point is valid (ie on the curve), a non-zero integer otherwise
 */
int wei25519_deserialize_point(wei25519e3_t* point, const uint8_t* stream);

//------------------------------------------------------------------------------
// Misc Operations

/**
 * @brief Normalize the projective coordinates (X:Y:Z) of a curve point.
 *    More precisely, it performs
 *        X <- X / Z,
 *        Y <- Y / Z,
 *        Z <- Z / Z = 1.
 * 
 *    Note: The point at the infinity is not supported.
 * 
 * @param point the non-zero point to normalize
 */
void wei25519_normalize_point(wei25519e3_t* point);

/**
 * @brief Controled swap.
 * 
 *    If bit = 0, do nothing
 *    If bit = 1, swap P and Q (P <-> Q)
 * 
 *    Note: Constant-time implementation
 * 
 * @param ptP the point P to swap
 * @param ptQ the point Q to swap
 * @param bit swap controler
 */
void wei25519_swap(wei25519e3_t* ptP, wei25519e3_t* ptQ, char bit);

/**
 * @brief Select a curve point.
 * 
 *    If bit = 0, set R as P (R <- P).
 *    If bit = 1, set R as Q (R <- Q).
 * 
 *    Note: Constant-time implementation
 * 
 * @param ptP the point P to select
 * @param ptQ the point Q to select
 * @param ptR the output (point R)
 * @param bit select bit
 */
void wei25519_select(wei25519e3_t* ptR, const wei25519e3_t* ptP, const wei25519e3_t* ptQ, char bit);

/**
 * @brief Print the curve point
 *    in two representations:
 *      - projective representation: (X:Y:Z)
 *      - affine representation: (x,y)
 * 
 * @note: only use for debug
 *  
 * @param point the point to print
 */
void wei25519_print(wei25519e3_t* point);

/**
 * @brief Check that the point (X:Y:Z) is valid (ie a curve point)
 *    To proceed, it checks that Z Y^2 = X^3 + a X Z^2 + b Z^3.
 *      ie, ((b Z + a X) Z - Y^2) Z + X^3 = 0
 *    It also checks that either X, Y or Z is not zero.
 * 
 * @note: only use for debug. Normally, the checking is performed
 *     when deserialize a curve point with "wei25519_deserialize_point".
 *  
 * @param point the point to check
 * @return 1 if valid, 0 otherwise
 */
int wei25519_is_valid_point(const wei25519e3_t* point);

#endif   // WEI25519_H
