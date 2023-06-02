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
 * Copyright 2017 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
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

#ifndef _OLMACCOUNT_H
#define _OLMACCOUNT_H

#include "olm_jni.h"
#include "olm/olm.h"

#define OLM_ACCOUNT_FUNC_DEF(func_name) FUNC_DEF(OlmAccount,func_name)
#define OLM_MANAGER_FUNC_DEF(func_name) FUNC_DEF(OlmManager,func_name)

#ifdef __cplusplus
extern "C" {
#endif

// account creation/destruction
JNIEXPORT void OLM_ACCOUNT_FUNC_DEF(releaseAccountJni)(JNIEnv *env, jobject thiz);
JNIEXPORT jlong OLM_ACCOUNT_FUNC_DEF(createNewAccountJni)(JNIEnv *env, jobject thiz);

// identity keys
JNIEXPORT jbyteArray OLM_ACCOUNT_FUNC_DEF(identityKeysJni)(JNIEnv *env, jobject thiz);
JNIEXPORT jbyteArray OLM_ACCOUNT_FUNC_DEF(identityPrivateKeyJni)(JNIEnv *env, jobject thiz);

// one time keys
JNIEXPORT jbyteArray OLM_ACCOUNT_FUNC_DEF(oneTimeKeysJni)(JNIEnv *env, jobject thiz);
JNIEXPORT jlong OLM_ACCOUNT_FUNC_DEF(maxOneTimeKeysJni)(JNIEnv *env, jobject thiz);
JNIEXPORT void OLM_ACCOUNT_FUNC_DEF(generateOneTimeKeysJni)(JNIEnv *env, jobject thiz, jint aNumberOfKeys);
JNIEXPORT void OLM_ACCOUNT_FUNC_DEF(removeOneTimeKeysJni)(JNIEnv *env, jobject thiz, jlong aNativeOlmSessionId);
JNIEXPORT void OLM_ACCOUNT_FUNC_DEF(markOneTimeKeysAsPublishedJni)(JNIEnv *env, jobject thiz);

// fallback keys
JNIEXPORT void OLM_ACCOUNT_FUNC_DEF(generateFallbackKeyJni)(JNIEnv *env, jobject thiz);
JNIEXPORT jbyteArray OLM_ACCOUNT_FUNC_DEF(fallbackKeyJni)(JNIEnv *env, jobject thiz);
JNIEXPORT void OLM_ACCOUNT_FUNC_DEF(forgetFallbackKeyJni)(JNIEnv *env, jobject thiz);

// signing
JNIEXPORT jbyteArray OLM_ACCOUNT_FUNC_DEF(signMessageJni)(JNIEnv *env, jobject thiz, jbyteArray aMessage);

// serialization
JNIEXPORT jbyteArray OLM_ACCOUNT_FUNC_DEF(serializeJni)(JNIEnv *env, jobject thiz, jbyteArray aKeyBuffer);
JNIEXPORT jlong OLM_ACCOUNT_FUNC_DEF(deserializeJni)(JNIEnv *env, jobject thiz, jbyteArray aSerializedDataBuffer, jbyteArray aKeyBuffer);

#ifdef __cplusplus
}
#endif

#endif
