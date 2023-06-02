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

#include "gf25519.h"
#include <string.h>
#include <stdio.h>

const gf25519e_t GF25519_ONE = {1, 0, 0, 0, 0, 0, 0, 0, 0, 0};

//------------------------------------------------------------------------------
// Static functions

// One round of carry spreading
//    |x[]| is (naturally) bounded by 2^31,2^31,2^31,2^31,etc. 
//    After 1 execution of the spreading,
//        all the even terms are bounded by 2^26
//        and all the odd terms are bounded by 2^25
//        except the first term x[0] which is bounded 2^27
//    Two consecutive executions of the spreading enables
//        to remove the exception on x[0]
static void spread_carry(gf25519e_t x) {
    uint32_t carry;
    for(int i=0; i<10; i+=2) {
        // Even index (26 bits)
        carry = (x[i] >> 26);
        x[i] -= (carry << 26);
        x[i+1] += carry;
        // Odd index (25 bits)
        carry = (x[i+1] >> 25);
        x[i+1] -= (carry << 25);
        if(i+2 < 10)
            x[i+2] += carry;
        else // Overflow
            x[0] += 19*carry;
    }
}

// Transform the representation of x
//    such that x[i] is non-negative and of
//       - 26 bits if i is even
//       - 25 bits if i is odd
//    and such that
//      x[0] + x[1]*2^26 + x[2]*2^51 + ...
//    is an integer between 0 and PRIME
static void reduce_gf25519e(gf25519e_t x) {
    gf25519e_t t;
    // Two spreading is enough to completely reduce x
    spread_carry(x);
    spread_carry(x);
    // Now that x is reduced, we need to compute x mod PRIME
    //    with PRIME = 0x7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffed
    // We know that x < 2*PRIME.
    // First, we compute t := x - PRIME
    t[0] = x[0] - 0x3FFFFED;
    for(int i=1; i<10-2; i+=2) {
        // t[i],x[i] -> 25 bits
        t[i] = x[i] - 0x1FFFFFF - ((t[i-1] >> 26) & 1);
        t[i-1] &= 0x3FFFFFF;
        // t[i+1],x[i+1] -> 26 bits
        t[i+1] = x[i+1] - 0x3FFFFFF - ((t[i] >> 25) & 1);
        t[i] &= 0x1FFFFFF;
    }
    t[9] = x[9] - 0x1FFFFFF - ((t[8] >> 26) & 1);
    t[8] &= 0x3FFFFFF;
    int carry = (t[9] >> 25) & 1;
    // If carry = 1, it means that t < 0, so we must keep x
    // If carry = 0, it means that t >= 0, so we must replace x by t
    gf25519_swap(x, t, 1 - carry);
}

//------------------------------------------------------------------------------
// Getters and setters

/// Check if x = 0
///    Output:
///       1 if x = 0
///       0 otherwise
char gf25519_iszero(const gf25519e_t x) {
    // Before to check if x is zero,
    //   we must compute t := x mod n
    gf25519e_t t;
    gf25519_copy(t, x);
    reduce_gf25519e(t);
    // Now, we can simply check that t[] = 0
    char res = 1;
    for(int i=0; i<10; i++)
        res &= (t[i]==0);
    return res;
}

/// Set "x = 0"
void gf25519_setzero(gf25519e_t x) {
    memset(x, 0, sizeof(gf25519e_t));
}

/// Set "x = 1"
void gf25519_setone(gf25519e_t x) {
    memset(x, 0, sizeof(gf25519e_t));
    x[0] = 1;
}

//------------------------------------------------------------------------------
// Math Operations

/// Performs "z = x" (copy)
void gf25519_copy(gf25519e_t z, const gf25519e_t x) {
    memcpy(z, x, sizeof(gf25519e_t));
}

/// Performs "z = - x"
void gf25519_neg(gf25519e_t z, const gf25519e_t x) {
    int i;
    for(i=0; i<10; i++)
        z[i] = -x[i];
}

/// Performs "z = x + y"
///   Assuming
///       |x[]| bounded by 2^(26+m),2^(25+m),2^(26+m),2^(25+m),etc.
///       |y[]| bounded by 2^(26+m),2^(25+m),2^(26+m),2^(25+m),etc.
///   Requirement: The number m of extra bits must be less than or equal to 4 (m<=4).
///   Output:
///       |z[]| bounded by 2^(26+m+1),2^(26+m+1),2^(26+m+1),2^(26+m+1),etc.
void gf25519_add(gf25519e_t z, const gf25519e_t x, const gf25519e_t y) {
    int i;
    for(i=0; i<10; i++)
        z[i] = x[i] + y[i];
}

/// Performs "z = x - y"
///   Assuming
///       |x[]| bounded by 2^(26+m),2^(25+m),2^(26+m),2^(25+m),etc.
///       |y[]| bounded by 2^(26+m),2^(25+m),2^(26+m),2^(25+m),etc.
///   Requirement: The number m of extra bits must be less than or equal to 4 (m<=4).
///   Output:
///       |z[]| bounded by 2^(26+m+1),2^(26+m+1),2^(26+m+1),2^(26+m+1),etc.
void gf25519_sub(gf25519e_t z, const gf25519e_t x, const gf25519e_t y) {
    int i;
    for(i=0; i<10; i++)
        z[i] = x[i] - y[i];
}

/// Performs "z = x * y"
///   Assuming
///       |x[]| bounded by 2^(26+m),2^(25+m),2^(26+m),2^(25+m),etc.
///       |y[]| bounded by 2^(26+m),2^(25+m),2^(26+m),2^(25+m),etc.
///   Requirement: The number m of extra bits must be less than or equal to 3 (m<=3).
///   Output:
///       |z[]| bounded by 2^26,2^25,2^26,2^25,etc.
void gf25519_mul(gf25519e_t z, const gf25519e_t x, const gf25519e_t y) {
    int i, j;
    uint64_t carry;

/*
 *    An element p, entries p[0]...p[18], represents the integer
 *    p[0]+2^26 p[1]+2^51 p[2]+2^77 p[3]+2^102 p[4]+...+2^230 p[9]
 *        +2^256 p[10]+ 2^281 p[11] + ... + 2^460 p[18].
 */

    int64_t p[19] = {0};
    for(i=0; i<10; i++) {
        for(j=0; j<10; j++) {
            if(i%2 == 1 && j%2==1) {
                p[i+j] += ((int64_t) x[i])*(2*y[j]); // 25+25+1 <= 51
            } else {
                p[i+j] += ((int64_t) x[i])*y[j]; // 25+26 or 26+26 or 26+25 <= 52
            }
        }
    }
    // Now, we have p = x*y
    // Below, we reduce p modulo PRIME

/*
 *   At this step,
 *       |p[i]| < (i+1)*2^(52+2m) for i<= 9
 *       |p[18-i]| < (i+1)*2^(52+2m) for i<= 9
 */

    // Spread Carry for p[0]..p[17]
    for(i=0; i<18; i+=2) {
        // Even index (26 bits)
        carry = (p[i] >> 26);
        p[i] -= (carry << 26);
        p[i+1] += carry;
        // Odd index (25 bits)
        carry = (p[i+1] >> 25);
        p[i+1] -= (carry << 25);
        p[i+2] += carry;
    }

/*
 *   At this step, except for p[18], we have for i<=18
 *       |p[i]| < 2^26 for i even
 *       |p[i]| < 2^25 for i odd
 *   And we have
 *       |p[18]| < 2^63
 */

    // Spread Carry for p[18] and reduce modulo p
    carry = (p[18] >> 26);
    p[18] -= (carry << 26);
    p[18+1-10] += 19*carry;
    // Reduce Modulo p
    for(i=0; i<9; i++) {
        p[i] += 19*p[10+i];
    }
    // From now, p[10]..p[18] are useless

/*
 *   At this step, except for p[9], we have for i<=9
 *       |p[i]| < (19+1)*2^26 for i even
 *       |p[i]| < (19+1)*2^25 for i odd
 *   And we have
 *       |p[9]| < 2^42
 */

    // Spread Carry twice
    for(j=0; j<2; j++) {
        for(i=0; i<10; i+=2) {
            // Even index (26 bits)
            carry = (p[i] >> 26);
            p[i] -= (carry << 26);
            p[i+1] += carry;
            // Odd index (25 bits)
            carry = (p[i+1] >> 25);
            p[i+1] -= (carry << 25);
            if(i+2 < 10)
                p[i+2] += carry;
            else // Overflow
                p[0] += 19*carry;
        }
    }

/*
 *   At this step,
 *       |p[i]| < 2^26 for i even
 *       |p[i]| < 2^25 for i odd
 */

    // Copy
    for(i=0; i<10; i++)
        z[i] = (int32_t) p[i];
}

/// Performs "z = x^2"
///   Assuming
///       |x[]| bounded by 2^(26+m),2^(25+m),2^(26+m),2^(25+m),etc.
///   Requirement: The number m of extra bits must be less than or equal to 3 (m<=3).
///   Output:
///       |z[]| bounded by 2^26,2^25,2^26,2^25,etc.
void gf25519_squ(gf25519e_t z, const gf25519e_t x) {
    gf25519_mul(z, x, x);
}

/// Performs "z = 1/x"
///   Assuming
///       |x[]| bounded by 2^(26+m),2^(25+m),2^(26+m),2^(25+m),etc.
///   Requirement: The number m of extra bits must be less than or equal to 3 (m<=3).
///   Output:
///       |z[]| bounded by 2^26,2^25,2^26,2^25,etc.
///   Note: if x = 0, then z = 0
void gf25519_inv(gf25519e_t z, const gf25519e_t x) {
    gf25519_copy(z, x);
    // To compute the inverse, we use the fact that
    //           x^-1 = x^(PRIME-2)
    // PRIME-2 = 0x7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffeb
    // last byte of PRIME-2 = Oxed = 0b11101011
    for(int i=253; i>=0; i--) {
        gf25519_mul(z, z, z);
        if(i!=2 && i!=4)
            gf25519_mul(z, z, x);
    }
}

/// Performs "z = 2*y"
///   Assuming
///       |x[]| bounded by 2^(26+m),2^(25+m),2^(26+m),2^(25+m),etc.
///   Requirement: The number m of extra bits must be less than or equal to 4 (m<=4).
///   Output:
///       |z[]| bounded by 2^(26+1),2^(25+1),2^(26+1),2^(26+1),etc.
void gf25519_dbl(gf25519e_t z, const gf25519e_t x) {
    for(int i=0; i<10; i++)
        z[i] = 2*x[i];
    // Only one spreading to save running time
    spread_carry(z);
}

/// Performs "z = 3*y"
///   Assuming
///       |x[]| bounded by 2^(26+m),2^(25+m),2^(26+m),2^(25+m),etc.
///   Requirement: The number m of extra bits must be less than or equal to 3 (m<=3).
///   Output:
///       |z[]| bounded by 2^(26+1),2^(25+1),2^(26+1),2^(26+1),etc.
void gf25519_trp(gf25519e_t z, const gf25519e_t x) {
    for(int i=0; i<10; i++)
        z[i] = 3*x[i];
    // Only one spreading to save running time
    spread_carry(z);
}

//------------------------------------------------------------------------------
// Serialization

/// Serialize a GF(2^255-19) element
///    the field element "x" is represented as
///       stream[0]*256^31 + stream[1]*256^30 + ... + stream[30]*256 + stream[31]
///    such that the above sum is an
///    integer between 0 and PRIME-1.
///    Note: The leading bit of "stream" will always be zero.
void from_gf25519e(uint8_t* stream, const gf25519e_t x) {
    gf25519e_t t;
    gf25519_copy(t, x);
    reduce_gf25519e(t);
    // At this point, all values in the array t are non-negative
    stream[0]  = (unsigned char) (t[9] >> 18);
    stream[1]  = (unsigned char) (t[9] >> 10);
    stream[2]  = (unsigned char) (t[9] >>  2);
    stream[3]  = (unsigned char) ((((uint32_t) t[9]) << 6) | (t[8] >> 20));
    stream[4]  = (unsigned char) (t[8] >> 12);
    stream[5]  = (unsigned char) (t[8] >>  4);
    stream[6]  = (unsigned char) ((((uint32_t) t[8]) << 4) | (t[7] >> 21));
    stream[7]  = (unsigned char) (t[7] >> 13);
    stream[8]  = (unsigned char) (t[7] >>  5);
    stream[9]  = (unsigned char) ((((uint32_t) t[7]) << 3) | (t[6] >> 23));
    stream[10] = (unsigned char) (t[6] >> 15);
    stream[11] = (unsigned char) (t[6] >> 7);
    stream[12] = (unsigned char) ((((uint32_t) t[6]) << 1) | (t[5] >> 24));
    stream[13] = (unsigned char) (t[5] >> 16);
    stream[14] = (unsigned char) (t[5] >>  8);
    stream[15] = (unsigned char) t[5];
    stream[16] = (unsigned char) (t[4] >> 18);
    stream[17] = (unsigned char) (t[4] >>  10);
    stream[18] = (unsigned char) (t[4] >>  2);
    stream[19] = (unsigned char) ((((uint32_t) t[4]) << 6) | (t[3] >> 19));
    stream[20] = (unsigned char) (t[3] >> 11);
    stream[21] = (unsigned char) (t[3] >>  3);
    stream[22] = (unsigned char) ((((uint32_t) t[3]) << 5) | (t[2] >> 21));
    stream[23] = (unsigned char) (t[2] >> 13);
    stream[24] = (unsigned char) (t[2] >>  5);
    stream[25] = (unsigned char) ((((uint32_t) t[2]) << 3) | (t[1] >> 22));
    stream[26] = (unsigned char) (t[1] >> 14);
    stream[27] = (unsigned char) (t[1] >>  6);
    stream[28] = (unsigned char) ((((uint32_t) t[1]) << 2) | (t[0] >> 24));
    stream[29] = (unsigned char) (t[0] >> 16);
    stream[30] = (unsigned char) (t[0] >>  8);
    stream[31] = (unsigned char) t[0];
}

/// Deserialize a GF(2^255-19) element
///    Inverse operation of "from_gf25519e"
///    Note: The leading bit of "stream" is ignored
#define get32(x, off) ((uint32_t) x[off])
void to_gf25519e(gf25519e_t x, const uint8_t* stream) {
    x[0] = // 26 bits
        ((get32(stream,28) & 0x03)<<24)
         | (get32(stream,29)<<16)
         | (get32(stream,30)<<8)
         | (get32(stream,31));
    x[1] = // 25 bits
        ((get32(stream,25) & 0x07)<<22)
         | (get32(stream,26)<<14)
         | (get32(stream,27)<<6)
         | (get32(stream,28)>>2);
    x[2] = // 26 bits
        ((get32(stream,22) & 0x1F)<<21)
         | (get32(stream,23)<<13)
         | (get32(stream,24)<<5)
         | (get32(stream,25)>>3);
    x[3] = // 25 bits
        ((get32(stream,19) & 0x3F)<<19)
         | (get32(stream,20)<<11)
         | (get32(stream,21)<<3)
         | (get32(stream,22)>>5);
    x[4] = // 26 bits
        ((get32(stream,16))<<18)
         | (get32(stream,17)<<10)
         | (get32(stream,18)<<2)
         | (get32(stream,19)>>6);
    x[5] = // 25 bits
        ((get32(stream,12) & 0x01)<<24)
         | (get32(stream,13)<<16)
         | (get32(stream,14)<<8)
         | (get32(stream,15));
    x[6] = // 26 bits
        ((get32(stream,9) & 0x07)<<23)
         | (get32(stream,10)<<15)
         | (get32(stream,11)<<7)
         | (get32(stream,12)>>1);
    x[7] = // 25 bits
        ((get32(stream,6) & 0x0F)<<21)
         | (get32(stream,7)<<13)
         | (get32(stream,8)<<5)
         | (get32(stream,9)>>3);
    x[8] = // 26 bits
        ((get32(stream,3) & 0x3F)<<20)
         | (get32(stream,4)<<12)
         | (get32(stream,5)<<4)
         | (get32(stream,6)>>4);
    x[9] = // 25 bits
        ((get32(stream,0) & 0x7F)<<18)
         | (get32(stream,1)<<10)
         | (get32(stream,2)<<2)
         | (get32(stream,3)>>6);
}

//------------------------------------------------------------------------------
// Misc Operations

/// Controled swap
///   If bit = 0, do nothing
///   If bit = 1, swap x and y (x <-> y)
/// Note: Constant-time implementation
void gf25519_swap(gf25519e_t x, gf25519e_t y, int bit) {
    // If bit = 0, mask = 0b00000000
    //    bit = 1, mask = 0b11111111
    int32_t mask = ~(bit-1);
    int32_t tmp;
    for(int i=0; i<10; i++) {
        tmp = mask & (x[i] ^ y[i]);
        x[i] ^= tmp;
        y[i] ^= tmp;
    }
}

/// Selection
///   If bit = 0, z <- x
///   If bit = 1, z <- y
/// Note: Constant-time implementation
void gf25519_select(gf25519e_t z, const gf25519e_t x, const gf25519e_t y, int bit) {
    // If bit = 0, mask = 0b11111111
    //    bit = 1, mask = 0b00000000
    int32_t mask_x = bit-1;
    int32_t mask_y = ~mask_x;
    for(int i=0; i<10; i++)
        z[i] = (mask_x & x[i]) | (mask_y & y[i]);
}

/// Print the GF(2^255-19) element
///    as a number between 0 and PRIME-1
///    written in hexadecimal
void gf25519_print(const gf25519e_t z) {
    // First serialize z (in big endianess)...
    uint8_t stream[GF25519_BYTESIZE];
    from_gf25519e(stream, z);
    /// ... then print z.
	for(int idx=0; idx<GF25519_BYTESIZE; idx++)
		printf("%02x", stream[idx]);
}
