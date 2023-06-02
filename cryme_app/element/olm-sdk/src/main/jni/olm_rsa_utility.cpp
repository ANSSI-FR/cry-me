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

#include "olm_rsa_utility.h"

using namespace AndroidOlmSdk;

OlmRSAUtility* initializeRSAUtilityMemory()
{
    size_t rsautilitySize = olm_rsa_utility_size();
    OlmRSAUtility* rsautilityPtr = (OlmRSAUtility*)malloc(rsautilitySize);

    if (rsautilityPtr)
    {
        rsautilityPtr = olm_rsa_utility(rsautilityPtr);
        LOGD("## initializeRSAUtilityMemory(): success - OLM utility size=%lu",static_cast<long unsigned int>(rsautilitySize));
    }
    else
    {
        LOGE("## initializeRSAUtilityMemory(): failure - OOM");
    }

    return rsautilityPtr;
}

JNIEXPORT jlong OLM_RSA_UTILITY_FUNC_DEF(createRSAUtilityJni)(JNIEnv *env, jobject thiz)
{
    OlmRSAUtility* rsautilityPtr = initializeRSAUtilityMemory();

    LOGD("## createRSAUtilityJni(): IN");

    // init account memory allocation
    if (!rsautilityPtr)
    {
        LOGE(" ## createRSAUtilityJni(): failure - init OOM");
        env->ThrowNew(env->FindClass("java/lang/Exception"), "init OOM");
    }
    else
    {
       LOGD(" ## createRSAUtilityJni(): success");
    }

    return (jlong)(intptr_t)rsautilityPtr;
}


JNIEXPORT void OLM_RSA_UTILITY_FUNC_DEF(releaseRSAUtilityJni)(JNIEnv *env, jobject thiz)
{
    OlmRSAUtility* rsautilityPtr = getRSAUtilityInstanceId(env, thiz);

    LOGD("## releaseRSAUtilityJni(): IN");

    if (!rsautilityPtr)
    {
        LOGE("## releaseRSAUtilityJni(): failure - utility ptr=NULL");
    }
    else
    {
        olm_clear_rsa_utility(rsautilityPtr);
        free(rsautilityPtr);
    }
}

JNIEXPORT jstring OLM_RSA_UTILITY_FUNC_DEF(genKeyRSAValuesJni)(JNIEnv *env, jobject thiz, jbyteArray n, 
                                                               jbyteArray p, jbyteArray q, jbyteArray lcm){

    jstring errorMessageRetValue = 0;
    OlmRSAUtility* rsautilityPtr = getRSAUtilityInstanceId(env, thiz);
    void* nPtr = NULL;
    void* pPtr = NULL;
    void* qPtr = NULL;
    void* lcmPtr = NULL;
    size_t randomSize = 0;
    uint8_t *randomBuffPtr = NULL;
    size_t nLength = (size_t)0;
    size_t pLength = (size_t)0;
    size_t qLength = (size_t)0;
    size_t lcmLength = (size_t)0;

    LOGD("## genKeyRSAValuesJni(): IN");

    if (!rsautilityPtr)
    {
        LOGE(" ## genKeyRSAValuesJni(): failure - invalid rsa utility ptr=NULL");
        errorMessageRetValue = env->NewStringUTF("invalid rsa utility ptr=NULL");
    }
    else if (!n || !p || !q || !lcm)
    {
        LOGE(" ## genKeyRSAValuesJni(): failure - invalid input parameters ");
        errorMessageRetValue = env->NewStringUTF("invalid input parameters");
    }
    else{

        nLength = (size_t)env->GetArrayLength(n);
        pLength = (size_t)env->GetArrayLength(p);
        qLength = (size_t)env->GetArrayLength(q);
        lcmLength = (size_t)env->GetArrayLength(lcm);

        nPtr = malloc(nLength*sizeof(uint8_t));
        pPtr = malloc(pLength*sizeof(uint8_t));
        qPtr = malloc(qLength*sizeof(uint8_t));
        lcmPtr = malloc(lcmLength*sizeof(uint8_t));

        randomSize = olm_get_randomness_size_RSA(rsautilityPtr);
        

        LOGD("## genKeyRSAValuesJni(): randomSize=%lu",static_cast<long unsigned int>(randomSize));

        if(!nPtr || !pPtr || !qPtr || !lcmPtr){
            LOGE(" ## genKeyRSAValuesJni(): failure - memory JNI allocation OOM");
            errorMessageRetValue = env->NewStringUTF("memory JNI allocation OOM");
        }
        else if ( (0 != randomSize) && !setRandomInBufferPRG(env, &randomBuffPtr, randomSize))
        {
            LOGE("## genKeyRSAValuesJni(): failure - random buffer init");
            errorMessageRetValue = env->NewStringUTF("random buffer init");
        }
        else{

            size_t result = olm_genKey_RSA(rsautilityPtr,
                                        nPtr, nLength,
                                        pPtr, pLength,
                                        qPtr, qLength,
                                        lcmPtr, lcmLength,
                                        (void*)randomBuffPtr, randomSize);

            if (result == olm_error()) {
                const char *errorMsgPtr = olm_rsa_utility_last_error(rsautilityPtr);
                errorMessageRetValue = env->NewStringUTF(errorMsgPtr);
                LOGE("## genKeyRSAValuesJni(): failure - olm_genKey_RSA Msg=%s with random length = %zu",errorMsgPtr, randomSize);
            }
            else
            {
                env->SetByteArrayRegion(n, 0 , nLength, (jbyte*)nPtr);
                env->SetByteArrayRegion(p, 0 , pLength, (jbyte*)pPtr);
                env->SetByteArrayRegion(q, 0 , qLength, (jbyte*)qPtr);
                env->SetByteArrayRegion(lcm, 0 , lcmLength, (jbyte*)lcmPtr);
                LOGD("## genKeyRSAValuesJni(): success - result=%lu", static_cast<long unsigned int>(result));
            }
        }
    }

    if(nPtr){
        memset(nPtr, 0, nLength*sizeof(uint8_t));
        free(nPtr);
    }
    if(pPtr){
        memset(pPtr, 0, pLength*sizeof(uint8_t));
        free(pPtr);
    }
    if(qPtr){
        memset(qPtr, 0, qLength*sizeof(uint8_t));
        free(qPtr);
    }
    if(lcmPtr){
        memset(lcmPtr, 0, lcmLength*sizeof(uint8_t));
        free(lcmPtr);
    }
    if (randomBuffPtr)
    {
        memset(randomBuffPtr, 0, randomSize);
        free(randomBuffPtr);
    }

    return errorMessageRetValue;
}

JNIEXPORT jstring OLM_RSA_UTILITY_FUNC_DEF(verifyAccreditationJni)(JNIEnv *env, jobject thiz, jbyteArray accreditation, jbyteArray url, jbyteArray modulus, jbyteArray publicExponent){
    
    OlmRSAUtility* rsautilityPtr = getRSAUtilityInstanceId(env, thiz);

    jstring errorMessageRetValue = 0;
    jbyte* nPtr = NULL;
    jbyte* ePtr = NULL;
    jbyte* accreditationPtr = NULL;
    jbyte* urlPtr = NULL;
    jboolean messageWasCopied = JNI_FALSE;
    jboolean accreditationWasCopied = JNI_FALSE;
    jboolean modulusWasCopied = JNI_FALSE;
    jboolean exponentWasCopied = JNI_FALSE;

    LOGD("## verifyAccreditationJni(): IN");

    if (!rsautilityPtr)
    {
        LOGE(" ## verifyAccreditationJni(): failure - invalid rsa utility ptr=NULL");
        errorMessageRetValue = env->NewStringUTF("invalid rsa utility ptr=NULL");
        
    }
    else if (!accreditation || !url || !modulus || !publicExponent)
    {
        LOGE(" ## verifyAccreditationJni(): failure - invalid input parameters ");
        errorMessageRetValue = env->NewStringUTF("invalid input parameters");
    }
    else if (!(accreditationPtr = env->GetByteArrayElements(accreditation, &accreditationWasCopied)))
    {
        LOGE(" ## verifyAccreditationJni(): failure - accreditation JNI allocation OOM");
        errorMessageRetValue = env->NewStringUTF("accreditation JNI allocation OOM");
    }
    else if (!(urlPtr = env->GetByteArrayElements(url, &messageWasCopied)))
    {
        LOGE(" ## verifyAccreditationJni(): failure - url JNI allocation OOM");
        errorMessageRetValue = env->NewStringUTF("url JNI allocation OOM");
    }
    else if (!(nPtr = env->GetByteArrayElements(modulus, &modulusWasCopied)))
    {
        LOGE(" ## verifyAccreditationJni(): failure - mod JNI allocation OOM");
        errorMessageRetValue = env->NewStringUTF("mod JNI allocation OOM");
    }
    else if (!(ePtr = env->GetByteArrayElements(publicExponent, &exponentWasCopied)))
    {
        LOGE(" ## verifyAccreditationJni(): failure - e JNI allocation OOM");
        errorMessageRetValue = env->NewStringUTF("e JNI allocation OOM");
    }
    else
    {
        size_t accreditationLength = (size_t)env->GetArrayLength(accreditation);
        size_t modulusLength = (size_t)env->GetArrayLength(modulus);
        size_t publicExponentLength = (size_t)env->GetArrayLength(publicExponent);
        size_t urlLength = (size_t)env->GetArrayLength(url);
        LOGD(" ## verifyAccreditationJni(): accreditationLength=%lu urlLength=%lu modulusLength=%lu publicExponentLength=%lu",static_cast<long unsigned int>(accreditationLength),static_cast<long unsigned int>(urlLength),static_cast<long unsigned int>(modulusLength),static_cast<long unsigned int>(publicExponentLength));

        size_t result = olm_verify_accreditation(rsautilityPtr,
                                           (void const *)nPtr,
                                           modulusLength,
                                           (void const *)ePtr,
                                           publicExponentLength,
                                           (void const *)urlPtr,
                                           urlLength,
                                           (void*)accreditationPtr,
                                           accreditationLength);
        if (result == olm_error()) {
            const char *errorMsgPtr = olm_rsa_utility_last_error(rsautilityPtr);
            errorMessageRetValue = env->NewStringUTF(errorMsgPtr);
            LOGE("## verifyAccreditationJni(): failure - olm_verify_accreditation Msg=%s with modulus size = %zu and exponent size = %zu",errorMsgPtr, modulusLength, publicExponentLength);
        }
        else
        {
            LOGD("## verifyAccreditationJni(): success - result=%lu", static_cast<long unsigned int>(result));
        }
    }

    //free alloc
    if (accreditationPtr)
    {
        if (accreditationWasCopied) {
            memset(accreditationPtr, 0, (size_t)env->GetArrayLength(accreditation));
        }
        env->ReleaseByteArrayElements(accreditation, accreditationPtr, JNI_ABORT);
    }

    if (urlPtr)
    {
        if (messageWasCopied) {
            memset(urlPtr, 0, (size_t)env->GetArrayLength(url));
        }
        env->ReleaseByteArrayElements(url, urlPtr, JNI_ABORT);
    }

    if (nPtr)
    {
        if (modulusWasCopied) {
            memset(nPtr, 0, (size_t)env->GetArrayLength(modulus));
        }
        env->ReleaseByteArrayElements(modulus, nPtr, JNI_ABORT);
    }

    if (ePtr)
    {
        if (exponentWasCopied) {
            memset(ePtr, 0, (size_t)env->GetArrayLength(publicExponent));
        }
        env->ReleaseByteArrayElements(publicExponent, ePtr, JNI_ABORT);
    }

    return errorMessageRetValue;
}
