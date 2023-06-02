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



#ifndef _OLM_PRG_UTILITY_H
#define _OLM_PRG_UTILITY_H

#include "olm_jni.h"
#include "olm/olm.h"

#define OLM_PRG_UTILITY_FUNC_DEF(func_name) FUNC_DEF(OlmPRGUtility,func_name)


#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT jlong   OLM_PRG_UTILITY_FUNC_DEF(createPRGUtilityJni)(JNIEnv *env, jobject thiz);
JNIEXPORT void    OLM_PRG_UTILITY_FUNC_DEF(releasePRGUtilityJni)(JNIEnv *env, jobject thiz);
JNIEXPORT jstring OLM_PRG_UTILITY_FUNC_DEF(initPRGJni)(JNIEnv *env, jobject thiz, jbyteArray nounce);
JNIEXPORT jstring OLM_PRG_UTILITY_FUNC_DEF(fillWithRandomBytesJni)(JNIEnv *env, jobject thiz, jbyteArray arr);
#ifdef __cplusplus
}
#endif



#endif
