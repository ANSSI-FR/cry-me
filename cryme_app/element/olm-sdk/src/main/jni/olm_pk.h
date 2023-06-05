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
 * Copyright 2018,2019 New Vector Ltd
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

#ifndef _OLMPK_H
#define _OLMPK_H

#include "olm_jni.h"
#include "olm/pk.h"

#define OLM_PK_ENCRYPTION_FUNC_DEF(func_name) FUNC_DEF(OlmPkEncryption,func_name)
#define OLM_PK_DECRYPTION_FUNC_DEF(func_name) FUNC_DEF(OlmPkDecryption,func_name)
#define OLM_PK_SIGNING_FUNC_DEF(func_name) FUNC_DEF(OlmPkSigning,func_name)

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong OLM_PK_ENCRYPTION_FUNC_DEF(createNewPkEncryptionJni)(JNIEnv *env, jobject thiz);
JNIEXPORT void OLM_PK_ENCRYPTION_FUNC_DEF(releasePkEncryptionJni)(JNIEnv *env, jobject thiz);
JNIEXPORT void OLM_PK_ENCRYPTION_FUNC_DEF(setRecipientKeyJni)(JNIEnv *env, jobject thiz, jbyteArray aKeyBuffer, jbyteArray idPublicKey, jbyteArray idPrivateKey);

JNIEXPORT jbyteArray OLM_PK_ENCRYPTION_FUNC_DEF(encryptJni)(JNIEnv *env, jobject thiz, jbyteArray aPlaintextBuffer, jobject aEncryptedMsg);

JNIEXPORT jlong OLM_PK_DECRYPTION_FUNC_DEF(createNewPkDecryptionJni)(JNIEnv *env, jobject thiz);
JNIEXPORT void OLM_PK_DECRYPTION_FUNC_DEF(releasePkDecryptionJni)(JNIEnv *env, jobject thiz);
JNIEXPORT jint OLM_PK_DECRYPTION_FUNC_DEF(privateKeyLength)(JNIEnv *env, jobject thiz);
JNIEXPORT jbyteArray OLM_PK_DECRYPTION_FUNC_DEF(setPrivateKeyJni)(JNIEnv *env, jobject thiz, jbyteArray key);
JNIEXPORT jbyteArray OLM_PK_DECRYPTION_FUNC_DEF(generateKeyJni)(JNIEnv *env, jobject thiz);
JNIEXPORT jbyteArray OLM_PK_DECRYPTION_FUNC_DEF(privateKeyJni)(JNIEnv *env, jobject thiz);
JNIEXPORT jbyteArray OLM_PK_DECRYPTION_FUNC_DEF(decryptJni)(JNIEnv *env, jobject thiz, jobject aEncryptedMsg);

JNIEXPORT jlong OLM_PK_SIGNING_FUNC_DEF(createNewPkSigningJni)(JNIEnv *env, jobject thiz);
JNIEXPORT void OLM_PK_SIGNING_FUNC_DEF(releasePkSigningJni)(JNIEnv *env, jobject thiz);
JNIEXPORT jbyteArray OLM_PK_SIGNING_FUNC_DEF(generateKeyJni)(JNIEnv *env, jobject thiz);
JNIEXPORT jbyteArray OLM_PK_SIGNING_FUNC_DEF(setKeyFromSeedJni)(JNIEnv *env, jobject thiz, jbyteArray seed);
JNIEXPORT jbyteArray OLM_PK_SIGNING_FUNC_DEF(pkSignJni)(JNIEnv *env, jobject thiz, jbyteArray aMessage);

#ifdef __cplusplus
}
#endif

#endif
