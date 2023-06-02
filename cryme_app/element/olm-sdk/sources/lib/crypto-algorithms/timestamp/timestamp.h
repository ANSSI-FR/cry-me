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

#ifndef TIMESTAMP_H
#define TIMESTAMP_H

#include <stdint.h>

/**
 * @brief returns UTC time as the number of seconds since January 1st, 1970
 * 
 * @return number of seconds as a 32-bit unsigned integer (uint32_t)
 */

uint32_t raw_time();
uint32_t utc_time();


/**
 * @brief convert a date-time string (format "yyyy/mm/dd HH:MM:SS") into the 
 *        number of seconds since 1/1/1970 (a.k.a. Unix time)
 * 
 * @param datetime string following the format "yyyy/mm/dd HH:MM:SS"
 * @return number of seconds as a 32-bit unsigned integer (uint32_t)
 */

uint32_t unix_time(char* datetime);

#endif
