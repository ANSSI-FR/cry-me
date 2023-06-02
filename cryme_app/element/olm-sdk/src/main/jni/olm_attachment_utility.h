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

#ifndef _OLM_ATTACHMENT_UTILITY_H
#define _OLM_ATTACHMENT_UTILITY_H

#include "olm_jni.h"
#include "olm/olm.h"

#define OLM_ATTACHMENT_UTILITY_FUNC_DEF(func_name) FUNC_DEF(OlmAttachmentUtility,func_name)


#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT jlong   OLM_ATTACHMENT_UTILITY_FUNC_DEF(createAttachmentUtilityJni)(JNIEnv *env, jobject thiz);
JNIEXPORT void    OLM_ATTACHMENT_UTILITY_FUNC_DEF(releaseAttachmentUtilityJni)(JNIEnv *env, jobject thiz);

JNIEXPORT jstring OLM_ATTACHMENT_UTILITY_FUNC_DEF(encryptAttachmentJni)(
    JNIEnv *env, jobject thiz, jbyteArray aPlaintextBuffer, jbyteArray info, jbyteArray sessionKey, jobject aEncryptedMsg
);

JNIEXPORT jstring OLM_ATTACHMENT_UTILITY_FUNC_DEF(decryptAttachmentJni)(
    JNIEnv *env, jobject thiz, jobject aEncryptedMsg, jbyteArray plaintext
);

#ifdef __cplusplus
}
#endif



#endif
