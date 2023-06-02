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

#ifndef UTILITIES_H
#define UTILITIES_H

/*************************** HEADER FILES ***************************/
#include <stddef.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <time.h>
#include "../sha/sha_struct.h"
#include "../sha/sha1.h"
#include "../sha/sha3.h"

/****************************** MACROS ******************************/



/*********************************************
 ********* General Error Codes Macros ********
 *********************************************/

#define OUT_OF_RANDOMNESS_ERROR_CODE 2
#define MEMORY_ALLOCATION_ERROR 3

/*********************************************
 ********* AES Macros ********
 *********************************************/

#define AES_KEY_LENGTH 32
#define AES_IV_LENGTH 16

#define EXIT_SUCCESS_CODE 1
#define EXIT_FAILURE_CODE 0

#define GCM_OK                          EXIT_SUCCESS_CODE
#define GCM_BAD_TAG_LENGTH              11
#define GCM_TAG_VERIFICATION_FAILURE    12

#define CBC_DECRYPT_OK                  EXIT_SUCCESS_CODE
#define CBC_DECRYPT_BAD_INPUT_LENGTH    14
#define CBC_DECRYPT_PADDING_FAILURE     15

/*********************************************
 ********* SHA & HMAC Macros ********
 *********************************************/

#define MAC_KEY_LENGTH_BACKUP 32

/*********************** FUNCTION DECLARATIONS **********************/

size_t check_eq_cst(uint8_t const * a, uint8_t const * b, size_t len) __attribute__((warn_unused_result));

#endif   // UTILITIES_H

