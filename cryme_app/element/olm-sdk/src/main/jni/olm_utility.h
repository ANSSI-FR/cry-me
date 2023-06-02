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

/*
 * Copyright 2016 OpenMarket Ltd
 * Copyright 2016 Vector Creations Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _OLMUTILITY_H
#define _OLMUTILITY_H

#include "olm_jni.h"
#include "olm/olm.h"

#define OLM_UTILITY_FUNC_DEF(func_name) FUNC_DEF(OlmUtility,func_name)


#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT jlong   OLM_UTILITY_FUNC_DEF(createUtilityJni)(JNIEnv *env, jobject thiz);
JNIEXPORT void    OLM_UTILITY_FUNC_DEF(releaseUtilityJni)(JNIEnv *env, jobject thiz);
JNIEXPORT jstring OLM_UTILITY_FUNC_DEF(verifyWeiSig25519SignatureJni)(JNIEnv *env, jobject thiz, jbyteArray aSignature, jbyteArray aKey, jbyteArray aMessage);
JNIEXPORT jbyteArray OLM_UTILITY_FUNC_DEF(sha3Jni)(JNIEnv *env, jobject thiz, jbyteArray aMessageToHash);
JNIEXPORT jbyteArray OLM_UTILITY_FUNC_DEF(shaJni)(JNIEnv *env, jobject thiz, jbyteArray aMessageToHash);
JNIEXPORT jbyteArray OLM_UTILITY_FUNC_DEF(pbkdf2Jni)(JNIEnv *env, jobject thiz, jbyteArray password, jbyteArray salt, jint dklen, jint c);
JNIEXPORT jbyteArray OLM_UTILITY_FUNC_DEF(encryptBackupKeyJni)(JNIEnv *env, jobject thiz, jbyteArray backupKey, jbyteArray key, jbyteArray info, jbyteArray ciphertext, jbyteArray mac, jbyteArray iv);
JNIEXPORT jstring OLM_UTILITY_FUNC_DEF(decryptBackupKeyJni)(JNIEnv *env, jobject thiz, jbyteArray ciphertext, jbyteArray mac, jbyteArray iv, jbyteArray info, jbyteArray key, jbyteArray backupKey);
JNIEXPORT jlong OLM_UTILITY_FUNC_DEF(getTimestampJni)(JNIEnv *env, jobject thiz);
#ifdef __cplusplus
}
#endif



#endif
