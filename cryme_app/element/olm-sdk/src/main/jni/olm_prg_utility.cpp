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

//
// Created by Abdul Rahman TALEB on 10/06/2022.
//

#include "olm_prg_utility.h"

using namespace AndroidOlmSdk;

OlmPRGUtility* initializePRGUtilityMemory()
{
    size_t prgutilitySize = olm_prg_utility_size();
    OlmPRGUtility * prgutilityPtr = (OlmPRGUtility*)malloc(prgutilitySize);

    if (prgutilityPtr)
    {
        prgutilityPtr = olm_prg_utility(prgutilityPtr);
        LOGD("## initializePRGMemory(): success - OLM utility size=%lu",static_cast<long unsigned int>(prgutilitySize));
    }
    else
    {
        LOGE("## initializePRGMemory(): failure - OOM");
    }

    return prgutilityPtr;
}

JNIEXPORT jlong OLM_PRG_UTILITY_FUNC_DEF(createPRGUtilityJni)(JNIEnv *env, jobject thiz)
{
    OlmPRGUtility* prgutilityPtr = initializePRGUtilityMemory();

    LOGD("## createPRGUtilityJni(): IN");

    // init account memory allocation
    if (!prgutilityPtr)
    {
        LOGE(" ## createPRGUtilityJni(): failure - init OOM");
        env->ThrowNew(env->FindClass("java/lang/Exception"), "init OOM");
    }
    else
    {
       LOGD(" ## createPRGUtilityJni(): success");
    }

    return (jlong)(intptr_t)prgutilityPtr;
}


JNIEXPORT void OLM_PRG_UTILITY_FUNC_DEF(releasePRGUtilityJni)(JNIEnv *env, jobject thiz)
{
    OlmPRGUtility* prgutilityPtr = getPRGUtilityInstanceId(env, thiz);

    LOGD("## releasePRGUtilityJni(): IN");

    if (!prgutilityPtr)
    {
        LOGE("## releasePRGUtilityJni(): failure - prg utility ptr=NULL");
    }
    else
    {
        olm_clear_prg_utility(prgutilityPtr);
        free(prgutilityPtr);
    }
}

JNIEXPORT jstring OLM_PRG_UTILITY_FUNC_DEF(initPRGJni)(JNIEnv *env, jobject thiz, jbyteArray nounce)
{
    jstring errorMessageRetValue = 0;
    OlmPRGUtility* prgutilityPtr = getPRGUtilityInstanceId(env, thiz);
    size_t randomSize = 0;
    uint8_t *randomBuffPtr = NULL;
    jbyte * nouncePtr = NULL;
    jboolean nounceWasCopied = JNI_FALSE;

    LOGD("## initPRGJni(): IN");

    if (!prgutilityPtr)
    {
        LOGE("## initPRGJni(): failure - prg utility ptr=NULL");
        errorMessageRetValue = env->NewStringUTF("ptr = NULL");
    }
    else if (!nounce)
    {
        LOGE(" ## initPRGJni(): failure - invalid input parameters ");
        errorMessageRetValue = env->NewStringUTF("invalid input parameters");
    }
    else if (!(nouncePtr = env->GetByteArrayElements(nounce, &nounceWasCopied)))
    {
        LOGE(" ## initPRGJni(): failure - nounce JNI allocation OOM");
        errorMessageRetValue = env->NewStringUTF("nounce JNI allocation OOM");
    }
    else
    {   
        size_t nounceLength = (size_t)env->GetArrayLength(nounce);


        randomSize = _olm_prg_entropy_size(prgutilityPtr);
        LOGD("## initPRGJni(): randomSize=%lu",static_cast<long unsigned int>(randomSize));

        if ( (0 != randomSize) && !setRandomInBuffer(env, &randomBuffPtr, randomSize))
        {
            LOGE("## initPRGJni(): failure - random buffer init");
            errorMessageRetValue = env->NewStringUTF("random buffer init");
        }
        else{
            LOGD("## initPRGJni(): generated random");
            size_t result = _olm_prg_init(prgutilityPtr, 
                                        (void*)randomBuffPtr, randomSize,
                                        (void*)nouncePtr, nounceLength);

            LOGD("## initPRGJni(): EXECUTED PRG INIT !!!");

            if (result == olm_error()) {
                const char *errorMsgPtr = olm_prg_utility_last_error(prgutilityPtr);
                errorMessageRetValue = env->NewStringUTF(errorMsgPtr);
                LOGE("## initPRGJni(): failure - _olm_prg_init Msg=%s",errorMsgPtr);
            }
            else
            {
                LOGD("## initPRGJni(): success - result=%lu", static_cast<long unsigned int>(result));
            }
        }
    }

    if (nouncePtr)
    {
        if (nounceWasCopied) {
            memset(nouncePtr, 0, (size_t)env->GetArrayLength(nounce));
        }
        env->ReleaseByteArrayElements(nounce, nouncePtr, JNI_ABORT);
    }

    if (randomBuffPtr)
    {
        free(randomBuffPtr);
    }
    LOGD("## initPRGJni(): out");
    return errorMessageRetValue;
}

JNIEXPORT jstring OLM_PRG_UTILITY_FUNC_DEF(fillWithRandomBytesJni)(JNIEnv *env, jobject thiz, jbyteArray arr){

    jstring errorMessageRetValue = 0;
    OlmPRGUtility* prgutilityPtr = getPRGUtilityInstanceId(env, thiz);
    void * arrPtr = NULL;
    size_t randomSize = 0;
    uint8_t *randomBuffPtr = NULL;
    size_t result;
    size_t skip = 0;

    LOGD("## fillWithRandomBytesJni(): IN");

    if (!prgutilityPtr)
    {
        LOGE(" ## fillWithRandomBytesJni(): failure - invalid prg utility ptr=NULL");
        errorMessageRetValue = env->NewStringUTF("ptr = NULL");
    }
    else if (!arr)
    {
        LOGE(" ## fillWithRandomBytesJni(): failure - invalid input parameters ");
        errorMessageRetValue = env->NewStringUTF("buffer = NULL");
    }
    else{
        size_t arrLength = (size_t)env->GetArrayLength(arr);
        arrPtr = malloc(arrLength*sizeof(uint8_t));

        result = _olm_prg_needs_reseeding(prgutilityPtr, arrLength);
        if(0 != result){
            LOGD("## fillWithRandomBytesJni(): prg needs reseeding ...");
            randomSize = result;
            if(!setRandomInBuffer(env, &randomBuffPtr, randomSize))
            {
                LOGE("## fillWithRandomBytesJni(): failure - random buffer init");
                errorMessageRetValue = env->NewStringUTF("random buffer init");
                skip = 1;
            }
            else
            {
                result = _olm_prg_reseed(prgutilityPtr,
                                        (void*)randomBuffPtr, randomSize);

                if (result == olm_error()) {
                    const char *errorMsgPtr = olm_prg_utility_last_error(prgutilityPtr);
                    errorMessageRetValue = env->NewStringUTF(errorMsgPtr);
                    LOGE("## fillWithRandomBytesJni(): failure - olm_prg_reseed Msg=%s with random length = %zu",errorMsgPtr, randomSize);
                    skip = 1;
                }
            }
        }

        if(skip == 0){  
            if(!arrPtr){
                errorMessageRetValue = env->NewStringUTF("JNI allocation OOM");
                LOGE(" ## fillWithRandomBytesJni(): failure - JNI allocation OOM");
            }
            else{
                LOGD("## fillWithRandomBytesJni(): generating pseudorandoms ...");
                size_t result = _olm_prg_get_random(prgutilityPtr,
                                            arrPtr, arrLength);

                if (result == olm_error()) {
                    const char *errorMsgPtr = olm_prg_utility_last_error(prgutilityPtr);
                    errorMessageRetValue = env->NewStringUTF(errorMsgPtr);
                    LOGE("## fillWithRandomBytesJni(): failure - olm_prg_sample Msg=%s",errorMsgPtr);
                }
                else
                {
                    env->SetByteArrayRegion(arr, 0 , arrLength, (jbyte*)arrPtr);
                    LOGD("## fillWithRandomBytesJni(): success - result=%lu", static_cast<long unsigned int>(result));
                }
            }
        }
    }

    // free alloc
    if (arrPtr){
        free(arrPtr);
    }

    return errorMessageRetValue;
}
