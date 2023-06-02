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

#ifndef SHA_STRUCT_H
#define SHA_STRUCT_H

/*************************** HEADER FILES ***************************/
#include <stddef.h>
#include <stdint.h>

/****************************** MACROS ******************************/
#define TYPE_SHA1 0
#define TYPE_SHA3 1

/**************************** DATA TYPES ****************************/
//typedef uint8_t BYTE;             // 8-bit byte
//typedef unsigned int  WORD;             // 32-bit word, change to "long" for 16-bit machines
typedef struct sha_struct {
    uint8_t sha_type;     // equal to one of the values defined in the above macros
    unsigned int block_length;
    unsigned int digest_length;
    uint8_t data[200];
    unsigned int datalen;
} SHA_CTX ;

inline size_t use_sha1(){
    return TYPE_SHA1;
}

inline size_t use_sha3(){
    return TYPE_SHA3;
}

/*********************** STATIC FUNCTION DECLARATIONS **********************/

#endif
