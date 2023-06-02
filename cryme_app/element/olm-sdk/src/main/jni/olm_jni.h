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
 * Copyright 2016,2018,2019 Vector Creations Ltd
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

#ifndef _OLMJNI_H
#define _OLMJNI_H

#include <cstdlib>
#include <cstdio>
#include <string>
#include <string.h>
#include <sstream>
#include <jni.h>
#include <android/log.h>


#define TAG "OlmJniNative"

/* logging macros */
//#define ENABLE_JNI_LOG

#ifdef NDK_DEBUG
    #warning NDK_DEBUG is defined!
#endif

#ifdef ENABLE_JNI_LOG
    #warning ENABLE_JNI_LOG is defined!
#endif

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#ifdef ENABLE_JNI_LOG
    #define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
    #define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#else
    #define LOGD(...)
    #define LOGW(...)
#endif

#define FUNC_DEF(class_name,func_name) JNICALL Java_org_matrix_olm_##class_name##_##func_name

namespace AndroidOlmSdk
{

}


#ifdef __cplusplus
extern "C" {
#endif

// internal helper functions
bool setRandomInBuffer(JNIEnv *env, uint8_t **aBuffer2Ptr, size_t aRandomSize);
bool setRandomInBufferPRG(JNIEnv *env, uint8_t **aBuffer2Ptr, size_t aRandomSize);

struct OlmSession* getSessionInstanceId(JNIEnv* aJniEnv, jobject aJavaObject);
struct OlmAccount* getAccountInstanceId(JNIEnv* aJniEnv, jobject aJavaObject);
struct OlmInboundGroupSession* getInboundGroupSessionInstanceId(JNIEnv* aJniEnv, jobject aJavaObject);
struct OlmOutboundGroupSession* getOutboundGroupSessionInstanceId(JNIEnv* aJniEnv, jobject aJavaObject);
struct OlmUtility* getUtilityInstanceId(JNIEnv* aJniEnv, jobject aJavaObject);
struct OlmAttachmentUtility* getAttachmentUtilityInstanceId(JNIEnv* aJniEnv, jobject aJavaObject);
struct OlmRSAUtility* getRSAUtilityInstanceId(JNIEnv* aJniEnv, jobject aJavaObject);
struct OlmPRGUtility* getPRGUtilityInstanceId(JNIEnv* aJniEnv, jobject aJavaObject);
struct OlmPkDecryption* getPkDecryptionInstanceId(JNIEnv* aJniEnv, jobject aJavaObject);
struct OlmPkEncryption* getPkEncryptionInstanceId(JNIEnv* aJniEnv, jobject aJavaObject);
struct OlmPkSigning* getPkSigningInstanceId(JNIEnv* aJniEnv, jobject aJavaObject);
struct OlmSAS* getOlmSasInstanceId(JNIEnv* aJniEnv, jobject aJavaObject);

#ifdef __cplusplus
}
#endif


#endif
