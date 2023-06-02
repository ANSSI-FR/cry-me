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

#include "olm_jni_helper.h"
#include "olm/olm.h"
#include <sys/time.h>

using namespace AndroidOlmSdk;

/**
* Init a buffer with a given number of random values.
* @param aBuffer2Ptr the buffer to be initialized
* @param aRandomSize the number of random values to apply
* @return true if operation succeed, false otherwise
**/
bool setRandomInBuffer(JNIEnv *env, uint8_t **aBuffer2Ptr, size_t aRandomSize)
{
    bool retCode = false;
    int bufferLen = aRandomSize*sizeof(uint8_t);

    if (!aBuffer2Ptr)
    {
        LOGE("## setRandomInBuffer(): failure - aBuffer=NULL");
    }
    else if (!aRandomSize)
    {
        LOGE("## setRandomInBuffer(): failure - random size=0");
    }
    else if (!(*aBuffer2Ptr = (uint8_t*)malloc(bufferLen)))
    {
        LOGE("## setRandomInBuffer(): failure - alloc mem OOM");
    }
    else
    {
        LOGD("## setRandomInBuffer(): randomSize=%lu",static_cast<long unsigned int>(aRandomSize));

        // use the secureRandom class
        jclass cls = env->FindClass("java/security/SecureRandom");

        if (cls)
        {
            jobject newObj = 0;
            jmethodID constructor = env->GetMethodID(cls, "<init>", "()V");
            jmethodID nextByteMethod = env->GetMethodID(cls, "nextBytes", "([B)V");

            if (constructor)
            {
                newObj = env->NewObject(cls, constructor);
                jbyteArray tempByteArray = env->NewByteArray(bufferLen);

                if (newObj && tempByteArray)
                {
                    env->CallVoidMethod(newObj, nextByteMethod, tempByteArray);

                    if (!env->ExceptionOccurred())
                    {
                        jbyte* buffer = env->GetByteArrayElements(tempByteArray, NULL);

                        if (buffer)
                        {
                            memcpy(*aBuffer2Ptr, buffer, bufferLen);
                            retCode = true;

                            // clear tempByteArray to hide sensitive data.
                            memset(buffer, 0, bufferLen);
                            env->SetByteArrayRegion(tempByteArray, 0, bufferLen, buffer);

                            // ensure that the buffer is released
                            env->ReleaseByteArrayElements(tempByteArray, buffer, JNI_ABORT);
                        }
                    }
                }

                if (tempByteArray)
                {
                    env->DeleteLocalRef(tempByteArray);
                }

                if (newObj)
                {
                    env->DeleteLocalRef(newObj);
                }
            }
        }

        // debug purpose
        /*for(int i = 0; i < aRandomSize; i++)
        {
            LOGD("## setRandomInBuffer(): randomBuffPtr[%ld]=%d",i, (*aBuffer2Ptr)[i]);
        }*/
    }

    return retCode;
}


/**
* Init a buffer with a given number of random values.
* @param aBuffer2Ptr the buffer to be initialized
* @param aRandomSize the number of random values to apply
* @return true if operation succeed, false otherwise
**/
bool setRandomInBufferPRG(JNIEnv *env, uint8_t **aBuffer2Ptr, size_t aRandomSize)
{
    bool retCode = false;
    int bufferLen = aRandomSize*sizeof(uint8_t);

    if (!aBuffer2Ptr)
    {
        LOGE("## setRandomInBufferPRG(): failure - aBuffer=NULL");
    }
    else if (!aRandomSize)
    {
        LOGE("## setRandomInBufferPRG(): failure - random size=0");
    }
    else if (!(*aBuffer2Ptr = (uint8_t*)malloc(bufferLen)))
    {
        LOGE("## setRandomInBufferPRG(): failure - alloc mem OOM");
    }
    else
    {
        LOGD("## setRandomInBufferPRG(): randomSize=%lu",static_cast<long unsigned int>(aRandomSize));

        jclass cls = env->FindClass("org/matrix/olm/OlmPRGUtility");

        if (cls)
        {
            LOGD("## setRandomInBufferPRG(): found prg class ...");
            jobject newObj = 0;
            jmethodID constructor = env->GetStaticMethodID(cls, "getInstance", "()Lorg/matrix/olm/OlmPRGUtility;");
            jmethodID fillWithRandomBytesMethod = env->GetMethodID(cls, "fillWithRandomBytes", "([B)V");

            if (constructor)
            {
                LOGD("## setRandomInBufferPRG(): found prg methods ...");
                newObj = env->CallStaticObjectMethod(cls, constructor);
                jbyteArray tempByteArray = env->NewByteArray(bufferLen);
                LOGD("## setRandomInBufferPRG(): called static method ...");

                
                if (newObj && tempByteArray)
                {
                    LOGD("## setRandomInBufferPRG(): got the prg instance ...");
                    env->CallVoidMethod(newObj, fillWithRandomBytesMethod, tempByteArray);

                    if (!env->ExceptionOccurred())
                    {
                        LOGD("## setRandomInBufferPRG(): got the random bytes ...");
                        jbyte* buffer = env->GetByteArrayElements(tempByteArray, NULL);

                        if (buffer)
                        {
                            memcpy(*aBuffer2Ptr, buffer, bufferLen);
                            retCode = true;

                            // clear tempByteArray to hide sensitive data.
                            memset(buffer, 0, bufferLen);
                            env->SetByteArrayRegion(tempByteArray, 0, bufferLen, buffer);

                            // ensure that the buffer is released
                            env->ReleaseByteArrayElements(tempByteArray, buffer, JNI_ABORT);
                        }
                    }
                }

                if (tempByteArray)
                {
                    env->DeleteLocalRef(tempByteArray);
                }

                if (newObj)
                {
                    env->DeleteLocalRef(newObj);
                }

                LOGD("## setRandomInBufferPRG(): success ...");
            }
        }

        // debug purpose
        /*for(int i = 0; i < aRandomSize; i++)
        {
            LOGD("## setRandomInBuffer(): randomBuffPtr[%ld]=%d",i, (*aBuffer2Ptr)[i]);
        }*/
    }

    return retCode;
}



/**
* Read the instance ID of the calling object.
* @param aJniEnv pointer pointing on the JNI function table
* @param aJavaObject reference to the object on which the method is invoked
* @param aCallingClass java calling class name
* @return the related instance ID
**/
jlong getInstanceId(JNIEnv* aJniEnv, jobject aJavaObject, const char *aCallingClass)
{
    jlong instanceId = 0;

    if (aJniEnv)
    {
        jclass requiredClass = aJniEnv->FindClass(aCallingClass);
        jclass loaderClass = 0;

        if (requiredClass && (JNI_TRUE != aJniEnv->IsInstanceOf(aJavaObject, requiredClass)))
        {
            LOGE("## getInstanceId() failure - invalid instance of");
        }
        else if ((loaderClass = aJniEnv->GetObjectClass(aJavaObject)))
        {
            jfieldID instanceIdField = aJniEnv->GetFieldID(loaderClass, "mNativeId", "J");

            if (instanceIdField)
            {
                instanceId = aJniEnv->GetLongField(aJavaObject, instanceIdField);
                LOGD("## getInstanceId(): read from java instanceId=%lld",instanceId);
            }
            else
            {
                LOGE("## getInstanceId() ERROR! GetFieldID=null");
            }

             aJniEnv->DeleteLocalRef(loaderClass);
        }
        else
        {
            LOGE("## getInstanceId() ERROR! GetObjectClass=null");
        }
    }
    else
    {
        LOGE("## getInstanceId() ERROR! aJniEnv=NULL");
    }

    LOGD("## getInstanceId() success - instanceId=%p (jlong)(intptr_t)instanceId=%lld",(void*)instanceId, (jlong)(intptr_t)instanceId);

    return instanceId;
}

/**
* Read the account instance ID of the calling object.
* @param aJniEnv pointer pointing on the JNI function table
* @param aJavaObject reference to the object on which the method is invoked
* @return the related OlmAccount.
**/
struct OlmAccount* getAccountInstanceId(JNIEnv* aJniEnv, jobject aJavaObject)
{
    return (struct OlmAccount*)getInstanceId(aJniEnv, aJavaObject, CLASS_OLM_ACCOUNT);
}

/**
* Read the session instance ID of the calling object (aJavaObject).<br>
* @param aJniEnv pointer pointing on the JNI function table
* @param aJavaObject reference to the object on which the method is invoked
* @return the related OlmSession.
**/
struct OlmSession* getSessionInstanceId(JNIEnv* aJniEnv, jobject aJavaObject)
{
    return (struct OlmSession*)getInstanceId(aJniEnv, aJavaObject, CLASS_OLM_SESSION);
}

/**
* Read the inbound group session instance ID of the calling object (aJavaObject).<br>
* @param aJniEnv pointer pointing on the JNI function table
* @param aJavaObject reference to the object on which the method is invoked
* @return the related OlmInboundGroupSession.
**/
struct OlmInboundGroupSession* getInboundGroupSessionInstanceId(JNIEnv* aJniEnv, jobject aJavaObject)
{
    return (struct OlmInboundGroupSession*)getInstanceId(aJniEnv, aJavaObject, CLASS_OLM_INBOUND_GROUP_SESSION);
}

/**
* Read the outbound group session instance ID of the calling object (aJavaObject).<br>
* @param aJniEnv pointer pointing on the JNI function table
* @param aJavaObject reference to the object on which the method is invoked
* @return the related OlmOutboundGroupSession
**/
struct OlmOutboundGroupSession* getOutboundGroupSessionInstanceId(JNIEnv* aJniEnv, jobject aJavaObject)
{
    return (struct OlmOutboundGroupSession*)getInstanceId(aJniEnv, aJavaObject, CLASS_OLM_OUTBOUND_GROUP_SESSION);
}

/**
* Read the utility instance ID of the calling object (aJavaObject).<br>
* @param aJniEnv pointer pointing on the JNI function table
* @param aJavaObject reference to the object on which the method is invoked
* @return the related OlmUtility
**/
struct OlmUtility* getUtilityInstanceId(JNIEnv* aJniEnv, jobject aJavaObject)
{
    return (struct OlmUtility*)getInstanceId(aJniEnv, aJavaObject, CLASS_OLM_UTILITY);
}

struct OlmAttachmentUtility* getAttachmentUtilityInstanceId(JNIEnv* aJniEnv, jobject aJavaObject)
{
    return (struct OlmAttachmentUtility*)getInstanceId(aJniEnv, aJavaObject, CLASS_OLM_ATTACHMENT_UTILITY);
}

struct OlmRSAUtility* getRSAUtilityInstanceId(JNIEnv* aJniEnv, jobject aJavaObject)
{
    return (struct OlmRSAUtility*)getInstanceId(aJniEnv, aJavaObject, CLASS_OLM_RSA_UTILITY);
}

struct OlmPRGUtility* getPRGUtilityInstanceId(JNIEnv* aJniEnv, jobject aJavaObject)
{
    return (struct OlmPRGUtility*)getInstanceId(aJniEnv, aJavaObject, CLASS_OLM_PRG_UTILITY);
}

struct OlmPkDecryption* getPkDecryptionInstanceId(JNIEnv* aJniEnv, jobject aJavaObject)
{
    return (struct OlmPkDecryption*)getInstanceId(aJniEnv, aJavaObject, CLASS_OLM_PK_DECRYPTION);
}

struct OlmPkEncryption* getPkEncryptionInstanceId(JNIEnv* aJniEnv, jobject aJavaObject)
{
    return (struct OlmPkEncryption*)getInstanceId(aJniEnv, aJavaObject, CLASS_OLM_PK_ENCRYPTION);
}

struct OlmPkSigning* getPkSigningInstanceId(JNIEnv* aJniEnv, jobject aJavaObject)
{
    return (struct OlmPkSigning*)getInstanceId(aJniEnv, aJavaObject, CLASS_OLM_PK_SIGNING);
}

struct OlmSAS* getOlmSasInstanceId(JNIEnv* aJniEnv, jobject aJavaObject)
{
    return (struct OlmSAS*)getInstanceId(aJniEnv, aJavaObject, CLASS_OLM_SAS);
}
