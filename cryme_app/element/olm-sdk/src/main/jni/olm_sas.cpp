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
 * Copyright 2019 New Vector Ltd
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

#include "olm_sas.h"

#include "olm/olm.h"

using namespace AndroidOlmSdk;

JNIEXPORT jlong OLM_SAS_FUNC_DEF(createNewSASJni)(JNIEnv *env, jobject thiz)
{
    
    size_t sasSize = olm_sas_size();
    OlmSAS *sasPtr = (OlmSAS *) malloc(sasSize);
    const char* errorMessage = NULL;

    if (!sasPtr)
    {
        LOGE("## createNewSASJni(): failure - init SAS OOM");
        env->ThrowNew(env->FindClass("java/lang/Exception"), "init sas OOM");
    }
    else
    {
        sasPtr = olm_sas(sasPtr);
        LOGD(" ## createNewSASJni(): success - sasPtr=%p (jlong)(intptr_t)accountPtr=%lld",sasPtr,(jlong)(intptr_t)sasPtr);
    }

    size_t randomSize = olm_create_sas_random_length(sasPtr);
    uint8_t *randomBuffPtr = NULL;

    LOGD("## createNewSASJni(): randomSize=%lu",static_cast<long unsigned int>(randomSize));

    if ( (0 != randomSize) && !setRandomInBufferPRG(env, &randomBuffPtr, randomSize))
    {
        LOGE("## createNewSASJni(): failure - random buffer init");
        errorMessage = "Failed to init private key";
    }
    else
    {
        size_t result = olm_create_sas(sasPtr, randomBuffPtr, randomSize);
        if (result == olm_error())
        {
            errorMessage = (const char *)olm_sas_last_error(sasPtr);
            LOGE("## createNewSASJni(): failure - error creating SAS Msg=%s", errorMessage);
        }
    }

    if (randomBuffPtr)
    {
        memset(randomBuffPtr, 0, randomSize);
        free(randomBuffPtr);
    }

    if (errorMessage)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), errorMessage);
    }

    return (jlong)(intptr_t)sasPtr;
}

JNIEXPORT void OLM_SAS_FUNC_DEF(releaseSASJni)(JNIEnv *env, jobject thiz)
{
    LOGD("## releaseSASJni(): IN");
    OlmSAS* sasPtr = getOlmSasInstanceId(env, thiz);

    if (!sasPtr)
    {
        LOGE("## releaseSessionJni(): failure - invalid Session ptr=NULL");
    }
    else
    {
        olm_clear_sas(sasPtr);
        // even if free(NULL) does not crash, logs are performed for debug purpose
        free(sasPtr);
    }
}


JNIEXPORT jbyteArray OLM_SAS_FUNC_DEF(getPubKeyJni)(JNIEnv *env, jobject thiz) 
{
    LOGD("## getPubKeyJni(): IN");
    const char* errorMessage = NULL;
    jbyteArray returnValue = 0;
    OlmSAS* sasPtr = getOlmSasInstanceId(env, thiz);

    if (!sasPtr)
    {
        LOGE("## getPubKeyJni(): failure - invalid SAS ptr=NULL");
        errorMessage = "invalid SAS ptr=NULL";
    }
    else
    {
        size_t pubKeyLength = olm_sas_pubkey_length(sasPtr);
        void *pubkey = malloc(pubKeyLength*sizeof(uint8_t));
        size_t result = olm_sas_get_pubkey(sasPtr, pubkey, pubKeyLength);
        if (result == olm_error())
        {
            errorMessage = (const char *)olm_sas_last_error(sasPtr);
            LOGE("## getPubKeyJni(): failure - error getting pub key Msg=%s", errorMessage);
        }
        else
        {
            returnValue = env->NewByteArray(pubKeyLength);
            env->SetByteArrayRegion(returnValue, 0 , pubKeyLength, (jbyte*)pubkey);
        }
        if (pubkey) {
            free(pubkey);
        }
    }

    if (errorMessage)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), errorMessage);
    }

    return returnValue;
}

JNIEXPORT void OLM_SAS_FUNC_DEF(setTheirPubKey)(JNIEnv *env, jobject thiz,jbyteArray pubKeyBuffer) {
    
    OlmSAS* sasPtr = getOlmSasInstanceId(env, thiz);

    const char* errorMessage = NULL;
    jbyte *pubKeyPtr = NULL;
    jboolean pubKeyWasCopied = JNI_FALSE;

    if (!sasPtr)
    {
        LOGE("## setTheirPubKey(): failure - invalid SAS ptr=NULL");
        errorMessage = "invalid SAS ptr=NULL";
    } else if(!pubKeyBuffer) {
        LOGE("## setTheirPubKey(): failure - invalid info");
        errorMessage = "invalid pubKey";
    }
    else if (!(pubKeyPtr = env->GetByteArrayElements(pubKeyBuffer, &pubKeyWasCopied)))
    {
        LOGE(" ## setTheirPubKey(): failure - info JNI allocation OOM");
        errorMessage = "info JNI allocation OOM";
    }
    else 
    {
        size_t pubKeyLength = (size_t)env->GetArrayLength(pubKeyBuffer);
        size_t result = olm_sas_set_their_key(sasPtr,pubKeyPtr,pubKeyLength);
        if (result == olm_error())
        {
            errorMessage = (const char *)olm_sas_last_error(sasPtr);
            LOGE("## setTheirPubKey(): failure - error setting their key Msg=%s", errorMessage);
        }
    }
    // free alloc
    if (pubKeyPtr)
    {
        if (pubKeyWasCopied)
        {
            memset(pubKeyPtr, 0, (size_t)env->GetArrayLength(pubKeyBuffer));
        }
        env->ReleaseByteArrayElements(pubKeyBuffer, pubKeyPtr, JNI_ABORT);
    }

    if (errorMessage)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), errorMessage);
    }

}

JNIEXPORT jbyteArray OLM_SAS_FUNC_DEF(generateShortCodeJni)(JNIEnv *env, jobject thiz, jbyteArray infoStringBytes, jint byteNb) {
    LOGD("## generateShortCodeJni(): IN");
    const char* errorMessage = NULL;
    jbyteArray returnValue = 0;
    OlmSAS* sasPtr = getOlmSasInstanceId(env, thiz);

    jbyte *infoPtr = NULL;
    jboolean infoWasCopied = JNI_FALSE;

    if (!sasPtr)
    {
        LOGE("## generateShortCodeJni(): failure - invalid SAS ptr=NULL");
        errorMessage = "invalid SAS ptr=NULL";
    } else if(!infoStringBytes) {
        LOGE("## generateShortCodeJni(): failure - invalid info");
        errorMessage = "invalid info";
    }
    else if (!(infoPtr = env->GetByteArrayElements(infoStringBytes, &infoWasCopied)))
    {
        LOGE(" ## generateShortCodeJni(): failure - info JNI allocation OOM");
        errorMessage = "info JNI allocation OOM";
    }
    else {
        size_t shortBytesCodeLength = (size_t) byteNb;
        void *shortBytesCode = malloc(shortBytesCodeLength * sizeof(uint8_t));
        size_t infoLength = (size_t)env->GetArrayLength(infoStringBytes);
        olm_sas_generate_bytes(sasPtr, infoPtr, infoLength, shortBytesCode, shortBytesCodeLength);
        returnValue = env->NewByteArray(shortBytesCodeLength);
        env->SetByteArrayRegion(returnValue, 0 , shortBytesCodeLength, (jbyte*)shortBytesCode);
        free(shortBytesCode);

        LOGD("## generateShortCodeJni(): IN");
    }

    // free alloc
    if (infoPtr)
    {
        if (infoWasCopied)
        {
            memset(infoPtr, 0, (size_t)env->GetArrayLength(infoStringBytes));
        }
        env->ReleaseByteArrayElements(infoStringBytes, infoPtr, JNI_ABORT);
    }

    if (errorMessage)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), errorMessage);
    }

    return returnValue;
}


JNIEXPORT jbyteArray OLM_SAS_FUNC_DEF(calculateMacJni)(JNIEnv *env, jobject thiz,jbyteArray messageBuffer,jbyteArray infoBuffer) {
    LOGD("## calculateMacJni(): IN");
    const char* errorMessage = NULL;
    jbyteArray returnValue = 0;
    OlmSAS* sasPtr = getOlmSasInstanceId(env, thiz);

    jbyte *messagePtr = NULL;
    jboolean messageWasCopied = JNI_FALSE;

    jbyte *infoPtr = NULL;
    jboolean infoWasCopied = JNI_FALSE;

    if (!sasPtr)
    {
        LOGE("## calculateMacJni(): failure - invalid SAS ptr=NULL");
        errorMessage = "invalid SAS ptr=NULL";
    } else if(!messageBuffer) {
        LOGE("## calculateMacJni(): failure - invalid message");
        errorMessage = "invalid info";
    }
    else if (!(messagePtr = env->GetByteArrayElements(messageBuffer, &messageWasCopied)))
    {
        LOGE(" ## calculateMacJni(): failure - message JNI allocation OOM");
        errorMessage = "message JNI allocation OOM";
    }
    else if (!(infoPtr = env->GetByteArrayElements(infoBuffer, &infoWasCopied)))
    {
        LOGE(" ## calculateMacJni(): failure - info JNI allocation OOM");
        errorMessage = "info JNI allocation OOM";
    } else {

        size_t infoLength = (size_t)env->GetArrayLength(infoBuffer);
        size_t messageLength = (size_t)env->GetArrayLength(messageBuffer);
        size_t macLength = olm_sas_mac_length(sasPtr);

        void *macPtr = malloc(macLength*sizeof(uint8_t));

        size_t result = olm_sas_calculate_mac(sasPtr,messagePtr,messageLength,infoPtr,infoLength,macPtr,macLength);
        if (result == olm_error())
        {
            errorMessage = (const char *)olm_sas_last_error(sasPtr);
            LOGE("## calculateMacJni(): failure - error calculating SAS mac Msg=%s", errorMessage);
        }
        else
        {
            LOGD("## calculateMacJni(): success ...");
            returnValue = env->NewByteArray(macLength);
            env->SetByteArrayRegion(returnValue, 0 , macLength, (jbyte*)macPtr);
        }

        if (macPtr) {
            free(macPtr);
        }
    }

    // free alloc
    if (infoPtr)
    {
        if (infoWasCopied)
        {
            memset(infoPtr, 0, (size_t)env->GetArrayLength(infoBuffer));
        }
        env->ReleaseByteArrayElements(infoBuffer, infoPtr, JNI_ABORT);
    }
    if (messagePtr)
    {
        if (messageWasCopied)
        {
            memset(messagePtr, 0, (size_t)env->GetArrayLength(messageBuffer));
        }
        env->ReleaseByteArrayElements(messageBuffer, messagePtr, JNI_ABORT);
    }

    if (errorMessage)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), errorMessage);
    }

    return returnValue;
}

// JNIEXPORT jbyteArray OLM_SAS_FUNC_DEF(calculateMacLongKdfJni)(JNIEnv *env, jobject thiz,jbyteArray messageBuffer,jbyteArray infoBuffer) {
//     LOGD("## calculateMacLongKdfJni(): IN");
//     const char* errorMessage = NULL;
//     jbyteArray returnValue = 0;
//     OlmSAS* sasPtr = getOlmSasInstanceId(env, thiz);

//     jbyte *messagePtr = NULL;
//     jboolean messageWasCopied = JNI_FALSE;

//     jbyte *infoPtr = NULL;
//     jboolean infoWasCopied = JNI_FALSE;

//     if (!sasPtr)
//     {
//         LOGE("## calculateMacLongKdfJni(): failure - invalid SAS ptr=NULL");
//         errorMessage = "invalid SAS ptr=NULL";
//     } else if(!messageBuffer) {
//         LOGE("## calculateMacLongKdfJni(): failure - invalid message");
//         errorMessage = "invalid info";
//     }
//     else if (!(messagePtr = env->GetByteArrayElements(messageBuffer, &messageWasCopied)))
//     {
//         LOGE(" ## calculateMacLongKdfJni(): failure - message JNI allocation OOM");
//         errorMessage = "message JNI allocation OOM";
//     }
//     else if (!(infoPtr = env->GetByteArrayElements(infoBuffer, &infoWasCopied)))
//     {
//         LOGE(" ## calculateMacLongKdfJni(): failure - info JNI allocation OOM");
//         errorMessage = "info JNI allocation OOM";
//     } else {

//         size_t infoLength = (size_t)env->GetArrayLength(infoBuffer);
//         size_t messageLength = (size_t)env->GetArrayLength(messageBuffer);
//         size_t macLength = olm_sas_mac_length(sasPtr);

//         void *macPtr = malloc(macLength*sizeof(uint8_t));

//         size_t result = olm_sas_calculate_mac_long_kdf(sasPtr,messagePtr,messageLength,infoPtr,infoLength,macPtr,macLength);
//         if (result == olm_error())
//         {
//             errorMessage = (const char *)olm_sas_last_error(sasPtr);
//             LOGE("## calculateMacLongKdfJni(): failure - error calculating SAS mac Msg=%s", errorMessage);
//         }
//         else
//         {
//             returnValue = env->NewByteArray(macLength);
//             env->SetByteArrayRegion(returnValue, 0 , macLength, (jbyte*)macPtr);
//         }

//         if (macPtr) {
//             free(macPtr);
//         }
//     }

//     // free alloc
//     if (infoPtr)
//     {
//         if (infoWasCopied)
//         {
//             memset(infoPtr, 0, (size_t)env->GetArrayLength(infoBuffer));
//         }
//         env->ReleaseByteArrayElements(infoBuffer, infoPtr, JNI_ABORT);
//     }
//     if (messagePtr)
//     {
//         if (messageWasCopied)
//         {
//             memset(messagePtr, 0, (size_t)env->GetArrayLength(messageBuffer));
//         }
//         env->ReleaseByteArrayElements(messageBuffer, messagePtr, JNI_ABORT);
//     }

//     if (errorMessage)
//     {
//         env->ThrowNew(env->FindClass("java/lang/Exception"), errorMessage);
//     }

//     return returnValue;
// }