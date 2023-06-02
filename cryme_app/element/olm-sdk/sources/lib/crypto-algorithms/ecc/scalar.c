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

#include "scalar.h"
#include <string.h>
#include <stdlib.h>
#include <stdio.h>

/// Controled swap
static void wei25519_swap_scalars(scalar_t x, scalar_t y, int bit) {
    int i;
    // If bit = 0, mask = 0b00000000
    //    bit = 1, mask = 0b11111111
    int8_t mask = ~(bit-1);
    int8_t tmp;
    for(i=0; i<SCALAR_BYTESIZE; i++) {
        tmp = mask & (x[i] ^ y[i]);
        x[i] ^= tmp;
        y[i] ^= tmp;
    }
}

//------------------------------------------------------------------------------
// Data Structure

// The order of the Wei25519 curve
//    n = 2^255 + 221938542218978828286815502327069187944
const uint8_t WEI25519_ORDER[WEI25519_ORDER_BYTESIZE] = {
    128, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 166, 247, 206, 245, 23, 188, 230, 178, 192, 147, 24, 210, 231, 174, 159, 104
};

//------------------------------------------------------------------------------
// Manipulation

/// Compute t = a - n
///    Return 1 if there is an overflow, 0 otherwise
static int wei25519_sub_order(scalar_t t, const scalar_t a) {
    int i, carry;
    t[SCALAR_BYTESIZE-1] = a[SCALAR_BYTESIZE-1] - WEI25519_ORDER[SCALAR_BYTESIZE-1];
    for(i=SCALAR_BYTESIZE-2; i>=0; i--) {
        carry = (t[i+1] > a[i+1]) | ((t[i+1] == a[i+1]) & (WEI25519_ORDER[i+1]>0));
        t[i] = a[i] - WEI25519_ORDER[i] - carry;
    }
    carry = (t[0] >= a[0]); // because WEI25519_ORDER[0] > 0
    return carry; // Overflow
}

/// Reduce the Z/nZ element "a" into a representation between 0 and n-1
void wei25519_reduce_scalar(scalar_t a) {
    uint8_t t[SCALAR_BYTESIZE];
    int overflow = wei25519_sub_order(t, a);
    wei25519_swap_scalars(a, t, 1-overflow);
}

/// Return 1 if a is in the reduced form, ie is between 0 and n-1
int wei25519_is_reduced_scalar(const scalar_t a) {
    uint8_t t[SCALAR_BYTESIZE];
    return wei25519_sub_order(t, a);
}

/// Return 1 if a is zero, 0 otherwise
///    Assumption: require that a is in the reduced form
int wei25519_is_null_scalar(const scalar_t a) {
    uint8_t res = 0;
    for(int i=0; i<SCALAR_BYTESIZE; i++)
        res |= a[i];
    return  (res == 0);
}

//------------------------------------------------------------------------------
// Math Operations

/// Performs "out = in" (copy)
void wei25519_copy_scalar(scalar_t out, const scalar_t in) {
    memcpy(out, in, SCALAR_BYTESIZE);
}

/// Performs "s = (ab + c) mod n"
///    Note: the output is not necessary in the reduced form
void wei25519_muladd_scalars(scalar_t s, const scalar_t a, const scalar_t b, const scalar_t c) {
    int64_t ma[16], mb[16];
    int64_t m[32];
    int i,j;
    uint64_t carry;

    // Copy "a" and reverse endianess
    for(i=0; i<16; i++)
        ma[i] = ((int64_t) a[31-2*i])+(((uint64_t) a[30-2*i])<<8);
    // Copy "b" and reverse endianess
    for(i=0; i<16; i++)
        mb[i] = ((int64_t) b[31-2*i])+(((uint64_t) b[30-2*i])<<8);
    // Copy "c" and reverse endianess
    for(i=0; i<16; i++)
        m[i] = ((int64_t) c[31-2*i])+(((uint64_t) c[30-2*i])<<8);
    for(i=16; i<32; i++)
        m[i] = 0;

    for(i=0; i<16; i++)
        for(j=0; j<16; j++)
            m[i+j] += ma[i]*mb[j];
    // all |m[:31]| < 2^16 + 2^(32+4)
    // m[31] = 0

    for(i=0; i<31; i++) {
        carry = ((m[i] + (1 << 15)) >> 16);
        m[i+1] += carry;
        m[i] -= (carry << 16);
    }
    // all |m[:31]| < 2^16
    // |m[:31]| < 2^(16+4)
    
    // (2^256 % n) = -443877084437957656573631004654138375888
    //             = - (
    //                    16080 * 256^0
    //                  + 53085 * 256^1
    //                  + 12709 * 256^2
    //                  + 33062 * 256^3
    //                  + 52581 * 256^4
    //                  + 12153 * 256^5
    //                  + 40426 * 256^6
    //                  + 19951 * 256^7
    //                  +     1 * 256^8
    //               )
    for(i=31; i>=16; i--) {
        m[i-16] -= 16080 * m[i];
        m[i-15] -= 53085 * m[i];
        m[i-14] -= 12709 * m[i];
        m[i-13] -= 33062 * m[i];
        m[i-12] -= 52581 * m[i];
        m[i-11] -= 12153 * m[i];
        m[i-10] -= 40426 * m[i];
        m[i- 9] -= 19951 * m[i];
        m[i- 8] -=     1 * m[i];
        m[i] = 0;
    }
    // all |m[:31]| < 2^16
    // |m[:31]| < 2^(16+4)

    for(j=0; j<3; j++) {
        for(i=0; i<16; i++) {
            carry = (m[i] >> 16);
            m[i+1] += carry;
            m[i] -= (carry << 16);
        }

        m[0] -= 16080 * m[16];
        m[1] -= 53085 * m[16];
        m[2] -= 12709 * m[16];
        m[3] -= 33062 * m[16];
        m[4] -= 52581 * m[16];
        m[5] -= 12153 * m[16];
        m[6] -= 40426 * m[16];
        m[7] -= 19951 * m[16];
        m[8] -=     1 * m[16];
        m[16] = 0;
    }

    // Copy "s" and reverse endianess
    for(i=0; i<16; i++) {
        s[31-2*i] = (m[i] & 0xFF);
        s[30-2*i] = ((m[i]>>8) & 0xFF);
    }
}

//------------------------------------------------------------------------------
// Randomness

/// Get a uniformly random scalar of size
///     If 256^sb < n, where sb:="sc_bytesize",
///                sample a scalar between 1 and 256^sb-1
///     Otherwise, sample a scalar between 1 and n-1
///    Note: the returned scalar is reduced
int wei25519_random_scalar(scalar_t sc, unsigned int sc_bytesize, rnd_engine_t* rnd_engine) {
    if(sc_bytesize > SCALAR_BYTESIZE)
        sc_bytesize = SCALAR_BYTESIZE;
    memset(sc, 0, SCALAR_BYTESIZE);

    do{
        int res = read_randomness(sc+(SCALAR_BYTESIZE-sc_bytesize), rnd_engine, sc_bytesize);
        if(res != EXIT_SUCCESS)
            return res;
    } while( (!wei25519_is_reduced_scalar(sc)) || wei25519_is_null_scalar(sc));
    return EXIT_SUCCESS;
}
