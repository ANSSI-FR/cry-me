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

#include "wei25519.h"
#include <string.h>
#include <stdio.h>

// Check that the point (x,y) is valid (ie a curve point)
//   To proceed, it checks that y^2 = x^3 + a x + b.
//       ie, (x^2 + a) x + b - y^2 = 0
static int wei25519_is_valid_affine_point(const gf25519e_t x, const gf25519e_t y) {
    gf25519e_t res, tmp;
    
    // res = x^3 + a x
    gf25519_squ(res, x);
    gf25519_add(res, res, WEI25519_A);
    gf25519_mul(res, res, x);

    // res = res + b - y^2
    gf25519_add(res, res, WEI25519_B);
    gf25519_squ(tmp, y);
    gf25519_sub(res, res, tmp);

    // Check that res == 0
    return gf25519_iszero(res);
}

//------------------------------------------------------------------------------
// Curve definition and data structure

// The constant a of the Wei25519 curve
const gf25519e_t WEI25519_A = {
    18129220, 11183634, 22369621, 22369621, 44739242, 11184810, 22369621, 22369621, 44739242, 11184810
};

// The constant b of the Wei25519 curve
const gf25519e_t WEI25519_B = {
    51431524, 14133021, 49710273, 1242756, 62137837, 9942053, 27340648, 12427567, 17398594, 32311675
};

// A generator of the curve group
const wei25519e3_t WEI25519_BASEPOINT = {
    {
        1, 0, 0, 0, 0, 0, 0, 0, 0, 0
    },
    {
        34436828, 15368706, 6057046, 15620070, 25630698, 28869587, 36868468, 20619957, 52915944, 10959166
    },
    {
        1, 0, 0, 0, 0, 0, 0, 0, 0, 0
    }
};

//------------------------------------------------------------------------------
// Getters and setters

/**
 * @brief Check if point == 0, where 0 is the point at infinity.
 * 
 * @param point the tested point
 * @return 1 if point == 0, 0 otherwise
 */
char wei25519_iszero(const wei25519e3_t* point) {
    return gf25519_iszero(point->Z);
}

/**
 * @brief Set "point = 0", where 0 is the point at infinity.
 * 
 * @param point the point to modify
 */
void wei25519_setzero(wei25519e3_t* point) {
    gf25519_setzero(point->X); // X = 0
    gf25519_setone(point->Y);  // Y = 1
    gf25519_setzero(point->Z); // Z = 0
}

//------------------------------------------------------------------------------
// Group Operations

/**
 * @brief Perform "R = P" (copy).
 * 
 * @param ptP the value to copy
 * @param ptR the copied value
 */
void wei25519_copy(wei25519e3_t* ptR, const wei25519e3_t* ptP) {
    gf25519_copy(ptR->X, ptP->X);
    gf25519_copy(ptR->Y, ptP->Y);
    gf25519_copy(ptR->Z, ptP->Z);
}

/**
 * @brief Perform "R = - P" on the curve Wei25519.
 * 
 * @param ptP the point P
 * @param ptR the point R
 */
void wei25519_neg(wei25519e3_t* ptR, const wei25519e3_t* ptP) {
    gf25519_copy(ptR->X, ptP->X);
    gf25519_neg(ptR->Y, ptP->Y);
    gf25519_copy(ptR->Z, ptP->Z);
}

/**
 * @brief Perform "R = 2*P" on the curve Wei25519.
 * 
 * @param ptP the point P
 * @param ptR the point R
 */
void wei25519_doubling(wei25519e3_t* ptR, const wei25519e3_t* ptP) {
    // https://hyperelliptic.org/EFD/g1p/auto-shortw-projective.html#doubling-dbl-2007-bl
    //   Cost: 5M + 6S + 1*a + 7add + 3*2 + 1*3. 
    //   The formula only works when P != -P (ie 2*P != 0)
    //   We handle the case P = -P at the end.
    gf25519e_t xx, zz, t0, t1, w, t2, s, ss, R, RR, t3, t4, t5, b, t6, t7, h, t8, t9, t10;

    // XX = X1^2
    gf25519_squ(xx, ptP->X);
    // ZZ = Z1^2
    gf25519_squ(zz, ptP->Z);
    // t0 = 3*XX
    gf25519_trp(t0, xx);
    // t1 = a*ZZ
    gf25519_mul(t1, WEI25519_A, zz);
    // w = t1+t0
    gf25519_add(w, t1, t0);
    // t2 = Y1*Z1
    gf25519_mul(t2, ptP->Y, ptP->Z);
    // s = 2*t2
    gf25519_dbl(s, t2);
    // ss = s^2
    gf25519_squ(ss, s);
    // sss = s*ss
    gf25519_mul(ptR->Z, s, ss);
    // R = Y1*s
    gf25519_mul(R, ptP->Y, s);
    // RR = R^2
    gf25519_squ(RR, R);
    // t3 = X1+R
    gf25519_add(t3, ptP->X, R);
    // t4 = t3^2
    gf25519_squ(t4, t3);
    // t5 = t4-XX
    gf25519_sub(t5, t4, xx);
    // B = t5-RR
    gf25519_sub(b, t5, RR);
    // t6 = w^2
    gf25519_squ(t6, w);
    // t7 = 2*B
    gf25519_dbl(t7, b);
    // h = t6-t7
    gf25519_sub(h, t6, t7);
    // X3 = h*s
    gf25519_mul(ptR->X, h, s);
    // t8 = B-h
    gf25519_sub(t8, b, h);
    // t9 = 2*RR
    gf25519_dbl(t9, RR);
    // t10 = w*t8
    gf25519_mul(t10, w, t8);
    // Y3 = t10-t9
    gf25519_sub(ptR->Y, t10, t9);
    // Z3 = sss

    // If 0 == Z3 == (2*Y1*Z1)^3 iff P == -P,
    // then X3 == 0.
    // We just need to set Y3 to 1
    // Rmk: most of the time, Y3 is already non-zero (probably, in all cases except when P==0).
    gf25519_select(ptR->Y, ptR->Y, GF25519_ONE, gf25519_iszero(ptR->Z));
}

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
void wei25519_adding(wei25519e3_t* ptR, const wei25519e3_t* ptP, const wei25519e3_t* ptQ) {
    // https://hyperelliptic.org/EFD/g1p/auto-shortw-projective.html#addition-add-1998-cmo-2
    //   Cost: 12M + 2S + 6add + 1*2
    //   The formula does not work
    //        - if P == 0 or Q == 0, we then handle this case at the end.
    //        - if P == Q (!=0), we DO NOT handle this case, it corresponds to the function "wei25519_doubling".
    //        - if P == -Q, we then handle this case at the end.
    gf25519e_t y1z2, x1z2, z1z2, t0, u, uu, t1, v, vv, vvv, r, t2, t3, t4, a, t5, t6, t7;
    wei25519e3_t res;

    // Y1Z2 = Y1*Z2
    gf25519_mul(y1z2, ptP->Y, ptQ->Z);
    // X1Z2 = X1*Z2
    gf25519_mul(x1z2, ptP->X, ptQ->Z);
    // Z1Z2 = Z1*Z2
    gf25519_mul(z1z2, ptP->Z, ptQ->Z);
    // t0 = Y2*Z1
    gf25519_mul(t0, ptQ->Y, ptP->Z);
    // u = t0-Y1Z2
    gf25519_sub(u, t0, y1z2);
    // uu = u^2
    gf25519_squ(uu, u);
    // t1 = X2*Z1
    gf25519_mul(t1, ptQ->X, ptP->Z);
    // v = t1-X1Z2
    gf25519_sub(v, t1, x1z2);
    // vv = v^2
    gf25519_squ(vv, v);
    // vvv = v*vv
    gf25519_mul(vvv, v, vv);
    // R = vv*X1Z2
    gf25519_mul(r, vv, x1z2);
    // t2 = 2*R
    gf25519_dbl(t2, r);
    // t3 = uu*Z1Z2
    gf25519_mul(t3, uu, z1z2);
    // t4 = t3-vvv
    gf25519_sub(t4, t3, vvv);
    // A = t4-t2
    gf25519_sub(a, t4, t2);
    // X3 = v*A
    gf25519_mul(res.X, v, a);
    // t5 = R-A
    gf25519_sub(t5, r, a);
    // t6 = vvv*Y1Z2
    gf25519_mul(t6, vvv, y1z2);
    // t7 = u*t5
    gf25519_mul(t7, u, t5);
    // Y3 = t7-t6
    gf25519_sub(res.Y, t7, t6);
    // Z3 = vvv*Z1Z2
    gf25519_mul(res.Z, vvv, z1z2);

    // If P == -Q (and P <> 0), we have
    //       X2*Z1 - X1*Z2 = 0
    //       Y2*Z1 + Y1*Z2 = 0
    // Then with the formula,
    //    -> X3 = 0 since v = X2*Z1 - X1*Z2 = 0
    //    -> Z3 = 0 since v = ... = 0
    //    -> Y3 = -uA = -u^3*Z1*Z2
    //          = -(2*Y2*Z1)^3 * Z1 * Z2
    //          != 0 since P <> 0, Q <> 0 and Y2 != 0
    //               (if the last non-equality was true, we would have P == -Q == Q)
    // So, the case P == -Q is naturally handled

    // If P == 0, res <- Q
    wei25519_select(&res, &res, ptQ, gf25519_iszero(ptP->Z));
    // If Q == 0, res <- P
    wei25519_select(&res, &res, ptP, gf25519_iszero(ptQ->Z));
    wei25519_copy(ptR, &res);
}

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
void wei25519_scalarmult(wei25519e3_t* out, const wei25519e3_t* base, const unsigned char *scalar, int scalar_bytelen) {
    // Methodology: Montgomery Tadder
    //   We compute L(P, i) := ( i*P , (i+1)*P )
    //                          -----  -------
    //                            p0      p1
    //    in an interative way.
    
    // Initialisation
    // L(P, 0) = ( 0, P )
    wei25519e3_t p0, p1;
    wei25519_setzero(&p0);    // p0 <- 0
    wei25519_copy(&p1, base); // p1 <- P

    // Montegomery Ladder
    for(int i=0; i<scalar_bytelen; i++) { // For each bit
        for(int j=0; j<8; j++) {
            char b = (scalar[i] >> (7-j)) & 1;
            // Depending to the bit b, there are two cases:
            //
            //    Either b == 0, we want to compute
            //       L(P, 2i+0) = (         2 * (i*P), (i*P) + ((i+1)*P) )
            //
            //    Or b == 1, we want to compute
            //       L(P, 2i+1) = ( (i*P) + ((i+1)*P),     2 * ((i+1)*P) )
            wei25519_swap(&p0, &p1, b);
            // Let us remark that p0 and p1 are consecutive points,
            //   meaning that p0 = k P and p1 = (k+1) P (or the opposite)
            //   where B is the base. So, if p0 == p1, then P == 0,
            //   and so, p0 == 0 and p1 == 0.
            //   We always respect the requirements of the addition function.
            wei25519_adding(&p1, &p0, &p1);
            wei25519_doubling(&p0, &p0);
            wei25519_swap(&p0, &p1, b);
        }
    }
    // At this step, we have L(P,sc)
    // Output p0 := sc*P
    wei25519_copy(out, &p0);
}

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
void wei25519_serialize_point(uint8_t* stream, const wei25519e3_t* point) {
    wei25519e3_t normalized_point;
    wei25519_copy(&normalized_point, point);
    wei25519_normalize_point(&normalized_point);
    from_gf25519e(stream, normalized_point.X);
    from_gf25519e(stream + GF25519_BYTESIZE, normalized_point.Y);
}

/**
 * @brief Deserialize a curve point.
 * 
 *    Inverse operation of "wei25519_serialize_point"
 *    It also checks that the deserialized point belongs to the curve.
 * 
 * @param point the deserialized point
 * @param stream the point to deserialize
 * @return 0 if the point is valid (ie on the curve), a non-zero integer otherwise
 */
int wei25519_deserialize_point(wei25519e3_t* point, const uint8_t* stream) {
    to_gf25519e(point->X, stream);
    to_gf25519e(point->Y, stream + GF25519_BYTESIZE);
    gf25519_setone(point->Z);
    // Check that the point is valid (ie on the elliptic curve)
    if( ! wei25519_is_valid_affine_point(point->X, point->Y) )
        return EXIT_FAILURE;
    return EXIT_SUCCESS;
}

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
void wei25519_normalize_point(wei25519e3_t* point) {
    gf25519e_t zinv;
    gf25519_inv(zinv, point->Z);
    gf25519_mul(point->X, point->X, zinv); // X <- X / Z
    gf25519_mul(point->Y, point->Y, zinv); // Y <- Y / Z
    gf25519_setone(point->Z); // Z <- 1 = Z/Z
}

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
void wei25519_swap(wei25519e3_t* ptP, wei25519e3_t* ptQ, char bit) {
    gf25519_swap(ptP->X, ptQ->X, bit);
    gf25519_swap(ptP->Y, ptQ->Y, bit);
    gf25519_swap(ptP->Z, ptQ->Z, bit);
}

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
void wei25519_select(wei25519e3_t* ptR, const wei25519e3_t* ptP, const wei25519e3_t* ptQ, char bit) {
    gf25519_select(ptR->X, ptP->X, ptQ->X, bit);
    gf25519_select(ptR->Y, ptP->Y, ptQ->Y, bit);
    gf25519_select(ptR->Z, ptP->Z, ptQ->Z, bit);
}

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
void wei25519_print(wei25519e3_t* point) {
    printf("Point (X:Y:Z)\n");
    printf("  X=");
    gf25519_print(point->X);
    printf("\n  Y=");
    gf25519_print(point->Y);
    printf("\n  Z=");
    gf25519_print(point->Z);
    if(wei25519_iszero(point)) {
        // Point at infinity
        printf("\nPoint at infinity\n");
    } else {
        // Other points
        wei25519e3_t normalized_point;
        wei25519_copy(&normalized_point, point);
        wei25519_normalize_point(&normalized_point);
        printf("\nPoint (x,y)\n");
        printf("  x=");
        gf25519_print(normalized_point.X);
        printf("\n  y=");
        gf25519_print(normalized_point.Y);
        printf("\n");
    }
}

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
int wei25519_is_valid_point(const wei25519e3_t* point) {
    gf25519e_t res, tmp;
    
    // res = (b Z + a X)
    gf25519_mul(res, WEI25519_B, point->Z);
    gf25519_mul(tmp, WEI25519_A, point->X);
    gf25519_add(res, res, tmp);

    // res = (res Z - Y^2)
    gf25519_mul(res, res, point->Z);
    gf25519_squ(tmp, point->Y);
    gf25519_sub(res, res, tmp);

    // res = (res Z + X^3)
    gf25519_mul(res, res, point->Z);
    gf25519_squ(tmp, point->X);
    gf25519_mul(tmp, tmp, point->X);
    gf25519_add(res, res, tmp);

    // Check that res = 0
    int is_valid = gf25519_iszero(res);

    // Check there is a non-zero coordinates
    return is_valid & (
        (gf25519_iszero(point->X) != 1)
         | (gf25519_iszero(point->Y) != 1)
         | (gf25519_iszero(point->Z) != 1)
    );
}

