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

#include "olm_utility.h"

using namespace AndroidOlmSdk;

OlmUtility* initializeUtilityMemory()
{
    size_t utilitySize = olm_utility_size();
    OlmUtility* utilityPtr = (OlmUtility*)malloc(utilitySize);

    if (utilityPtr)
    {
        utilityPtr = olm_utility(utilityPtr);
        LOGD("## initializeUtilityMemory(): success - OLM utility size=%lu",static_cast<long unsigned int>(utilitySize));
    }
    else
    {
        LOGE("## initializeUtilityMemory(): failure - OOM");
    }

    return utilityPtr;
}

JNIEXPORT jlong OLM_UTILITY_FUNC_DEF(createUtilityJni)(JNIEnv *env, jobject thiz)
{
    OlmUtility* utilityPtr = initializeUtilityMemory();

    LOGD("## createUtilityJni(): IN");

    // init account memory allocation
    if (!utilityPtr)
    {
        LOGE(" ## createUtilityJni(): failure - init OOM");
        env->ThrowNew(env->FindClass("java/lang/Exception"), "init OOM");
    }
    else
    {
       LOGD(" ## createUtilityJni(): success");
    }

    return (jlong)(intptr_t)utilityPtr;
}


JNIEXPORT void OLM_UTILITY_FUNC_DEF(releaseUtilityJni)(JNIEnv *env, jobject thiz)
{
    OlmUtility* utilityPtr = getUtilityInstanceId(env, thiz);

    LOGD("## releaseUtilityJni(): IN");

    if (!utilityPtr)
    {
        LOGE("## releaseUtilityJni(): failure - utility ptr=NULL");
    }
    else
    {
        olm_clear_utility(utilityPtr);
        free(utilityPtr);
    }
}


/**
 * Verify an weisig25519 signature.
 * @param aSignature the base64-encoded message signature to be checked.
 * @param aKey the weisig25519 key (fingerprint key)
 * @param aMessage the message which was signed
 * @return 0 if validation succeed, an error message string if operation failed
 */
JNIEXPORT jstring OLM_UTILITY_FUNC_DEF(verifyWeiSig25519SignatureJni)(JNIEnv *env, jobject thiz, jbyteArray aSignatureBuffer, jbyteArray aKeyBuffer, jbyteArray aMessageBuffer)
{
    jstring errorMessageRetValue = 0;
    OlmUtility* utilityPtr = getUtilityInstanceId(env, thiz);
    jbyte* signaturePtr = NULL;
    jbyte* keyPtr = NULL;
    jbyte* messagePtr = NULL;
    jboolean messageWasCopied = JNI_FALSE;

    LOGD("## verifyWeiSig25519SignatureJni(): IN");

    if (!utilityPtr)
    {
        LOGE(" ## verifyWeiSig25519SignatureJni(): failure - invalid utility ptr=NULL");
    }
    else if (!aSignatureBuffer || !aKeyBuffer || !aMessageBuffer)
    {
        LOGE(" ## verifyWeiSig25519SignatureJni(): failure - invalid input parameters ");
    }
    else if (!(signaturePtr = env->GetByteArrayElements(aSignatureBuffer, 0)))
    {
        LOGE(" ## verifyWeiSig25519SignatureJni(): failure - signature JNI allocation OOM");
    }
    else if (!(keyPtr = env->GetByteArrayElements(aKeyBuffer, 0)))
    {
        LOGE(" ## verifyWeiSig25519SignatureJni(): failure - key JNI allocation OOM");
    }
    else if (!(messagePtr = env->GetByteArrayElements(aMessageBuffer, &messageWasCopied)))
    {
        LOGE(" ## verifyWeiSig25519SignatureJni(): failure - message JNI allocation OOM");
    }
    else
    {
        size_t signatureLength = (size_t)env->GetArrayLength(aSignatureBuffer);
        size_t keyLength = (size_t)env->GetArrayLength(aKeyBuffer);
        size_t messageLength = (size_t)env->GetArrayLength(aMessageBuffer);
        LOGD(" ## verifyWeiSig25519SignatureJni(): signatureLength=%lu keyLength=%lu messageLength=%lu",static_cast<long unsigned int>(signatureLength),static_cast<long unsigned int>(keyLength),static_cast<long unsigned int>(messageLength));
        LOGD(" ## verifyWeiSig25519SignatureJni(): key=%.*s", static_cast<int>(keyLength), keyPtr);

        size_t result = olm_weisig25519_verify(utilityPtr,
                                           (void const *)keyPtr,
                                           keyLength,
                                           (void const *)messagePtr,
                                           messageLength,
                                           (void*)signaturePtr,
                                           signatureLength);
        if (result == olm_error()) {
            const char *errorMsgPtr = olm_utility_last_error(utilityPtr);
            errorMessageRetValue = env->NewStringUTF(errorMsgPtr);
            LOGE("## verifyWeiSig25519SignatureJni(): failure - olm_weisig25519_verify Msg=%s",errorMsgPtr);
        }
        else
        {
            LOGD("## verifyWeiSig25519SignatureJni(): success - result=%lu", static_cast<long unsigned int>(result));
        }
    }

    // free alloc
    if (signaturePtr)
    {
        env->ReleaseByteArrayElements(aSignatureBuffer, signaturePtr, JNI_ABORT);
    }

    if (keyPtr)
    {
        env->ReleaseByteArrayElements(aKeyBuffer, keyPtr, JNI_ABORT);
    }

    if (messagePtr)
    {
        if (messageWasCopied) {
            memset(messagePtr, 0, (size_t)env->GetArrayLength(aMessageBuffer));
        }
        env->ReleaseByteArrayElements(aMessageBuffer, messagePtr, JNI_ABORT);
    }

    return errorMessageRetValue;
}

/**
 * Compute the digest (SHA 256) for the message passed in parameter.<br>
 * The digest value is the function return value.
 * An exception is thrown if the operation fails.
 * @param aMessage the message
 * @return digest of the message.
 **/
JNIEXPORT jbyteArray OLM_UTILITY_FUNC_DEF(sha3Jni)(JNIEnv *env, jobject thiz, jbyteArray aMessageToHashBuffer)
{
    jbyteArray sha3Ret = 0;

    OlmUtility* utilityPtr = getUtilityInstanceId(env, thiz);
    jbyte* messagePtr = NULL;
    jboolean messageWasCopied = JNI_FALSE;

    LOGD("## sha3Jni(): IN");

    if (!utilityPtr)
    {
        LOGE(" ## sha3Jni(): failure - invalid utility ptr=NULL");
    }
    else if(!aMessageToHashBuffer)
    {
        LOGE(" ## sha3Jni(): failure - invalid message parameters ");
    }
    else if(!(messagePtr = env->GetByteArrayElements(aMessageToHashBuffer, &messageWasCopied)))
    {
        LOGE(" ## sha3Jni(): failure - message JNI allocation OOM");
    }
    else
    {
        // get lengths
        size_t messageLength = (size_t)env->GetArrayLength(aMessageToHashBuffer);
        size_t hashLength = olm_sha3_length(utilityPtr);
        void* hashValuePtr = malloc((hashLength)*sizeof(uint8_t));

        if (!hashValuePtr)
        {
            LOGE("## sha3Jni(): failure - hash value allocation OOM");
        }
        else
        {
            size_t result = olm_sha3(utilityPtr,
                                       (void const *)messagePtr,
                                       messageLength,
                                       (void *)hashValuePtr,
                                       hashLength);
            if (result == olm_error())
            {
                LOGE("## sha3Jni(): failure - hash creation Msg=%s",(const char *)olm_utility_last_error(utilityPtr));
            }
            else
            {
                LOGD("## sha3Jni(): success - result=%lu hashValue=%.*s",static_cast<long unsigned int>(result), static_cast<int>(result), (char*)hashValuePtr);
                sha3Ret = env->NewByteArray(result);
                env->SetByteArrayRegion(sha3Ret, 0 , result, (jbyte*)hashValuePtr);
            }

            free(hashValuePtr);
        }
    }

    if (messagePtr)
    {
        if (messageWasCopied) {
            memset(messagePtr, 0, (size_t)env->GetArrayLength(aMessageToHashBuffer));
        }
        env->ReleaseByteArrayElements(aMessageToHashBuffer, messagePtr, JNI_ABORT);
    }

    return sha3Ret;
}


JNIEXPORT jbyteArray OLM_UTILITY_FUNC_DEF(shaJni)(JNIEnv *env, jobject thiz, jbyteArray aMessageToHash){
    jbyteArray shaRet = 0;

    OlmUtility* utilityPtr = getUtilityInstanceId(env, thiz);
    jbyte* messagePtr = NULL;
    jboolean messageWasCopied = JNI_FALSE;

    LOGD("## shaJni(): IN");

    if (!utilityPtr)
    {
        LOGE(" ## shaJni(): failure - invalid utility ptr=NULL");
    }
    else if(!aMessageToHash)
    {
        LOGE(" ## shaJni(): failure - invalid message parameters ");
    }
    else if(!(messagePtr = env->GetByteArrayElements(aMessageToHash, &messageWasCopied)))
    {
        LOGE(" ## shaJni(): failure - message JNI allocation OOM");
    }
    else
    {
        // get lengths
        size_t messageLength = (size_t)env->GetArrayLength(aMessageToHash);
        size_t hashLength = olm_sha_length(utilityPtr);
        void* hashValuePtr = malloc((hashLength)*sizeof(uint8_t));

        if (!hashValuePtr)
        {
            LOGE("## shaJni(): failure - hash value allocation OOM");
        }
        else
        {
            size_t result = olm_sha(utilityPtr,
                                       (void const *)messagePtr,
                                       messageLength,
                                       (void *)hashValuePtr,
                                       hashLength);
            if (result == olm_error())
            {
                LOGE("## shaJni(): failure - hash creation Msg=%s",(const char *)olm_utility_last_error(utilityPtr));
            }
            else
            {
                LOGD("## shaJni(): success - result=%lu hashValue=%.*s",static_cast<long unsigned int>(result), static_cast<int>(result), (char*)hashValuePtr);
                shaRet = env->NewByteArray(result);
                env->SetByteArrayRegion(shaRet, 0 , result, (jbyte*)hashValuePtr);
            }

            free(hashValuePtr);
        }
    }

    if (messagePtr)
    {
        if (messageWasCopied) {
            memset(messagePtr, 0, (size_t)env->GetArrayLength(aMessageToHash));
        }
        env->ReleaseByteArrayElements(aMessageToHash, messagePtr, JNI_ABORT);
    }

    return shaRet;
}



JNIEXPORT jbyteArray OLM_UTILITY_FUNC_DEF(pbkdf2Jni)(JNIEnv *env, jobject thiz, jbyteArray password, jbyteArray salt, jint dklen, jint c){
    jbyteArray derivedKeyRet = 0;

    OlmUtility* utilityPtr = getUtilityInstanceId(env, thiz);
    jbyte* passwordPtr = NULL;
    jboolean passwordWasCopied = JNI_FALSE;
    jbyte* saltPtr = NULL;
    jboolean saltWasCopied = JNI_FALSE;

    LOGD("## pbkdf2Jni(): IN");

    if((dklen <= 0) || (c <= 0)){
        LOGE(" ## pbkdf2Jni(): failure - invalid dklen value=%d", dklen);
    }
    else if (!utilityPtr)
    {
        LOGE(" ## pbkdf2Jni(): failure - invalid utility ptr=NULL");
    }
    else if(!password || !salt)
    {
        LOGE(" ## pbkdf2Jni(): failure - invalid setup parameters ");
    }
    else if(!(passwordPtr = env->GetByteArrayElements(password, &passwordWasCopied)))
    {
        LOGE(" ## pbkdf2Jni(): failure - password JNI allocation OOM");
    }
    else if(!(saltPtr = env->GetByteArrayElements(salt, &saltWasCopied)))
    {
        LOGE(" ## pbkdf2Jni(): failure - salt JNI allocation OOM");
    }
    else
    {
        // get lengths
        size_t passwordLength = (size_t)env->GetArrayLength(password);
        size_t saltLength = (size_t)env->GetArrayLength(salt);

        LOGD("## pkdf2Jni(): length - passwordLength=%u, saltLength=%u", passwordLength, saltLength);

        void* derivedKeyPtr = malloc(((size_t)dklen)*sizeof(uint8_t));

        if (!derivedKeyPtr)
        {
            LOGE("## pbkdf2Jni(): failure - derived key output allocation OOM");
        }
        else
        {
            LOGD("## pbkdf2Jni(): dklen = %d", dklen);
            size_t result = olm_pbkdf2(utilityPtr,
                                       (void const *)passwordPtr, passwordLength, 
                                       (void const *)saltPtr, saltLength,
                                       (void *)derivedKeyPtr, (size_t)dklen, (size_t)c);

            if (result == olm_error())
            {
                LOGE("## pbkdf2Jni(): failure - key derivation Msg=%s",(const char *)olm_utility_last_error(utilityPtr));
            }
            else
            {
                LOGD("## pbkdf2Jni(): success - result=%lu",static_cast<long unsigned int>(result), static_cast<int>(result));
                derivedKeyRet = env->NewByteArray(dklen);
                env->SetByteArrayRegion(derivedKeyRet, 0 , dklen, (jbyte*)derivedKeyPtr);
            }

            free(derivedKeyPtr);
        }
    }

    if (passwordPtr)
    {
        if (passwordWasCopied) {
            memset(passwordPtr, 0, (size_t)env->GetArrayLength(password));
        }
        env->ReleaseByteArrayElements(password, passwordPtr, JNI_ABORT);
    }

    if (saltPtr)
    {
        if (saltWasCopied) {
            memset(saltPtr, 0, (size_t)env->GetArrayLength(salt));
        }
        env->ReleaseByteArrayElements(salt, saltPtr, JNI_ABORT);
    }

    return derivedKeyRet;
}


JNIEXPORT jbyteArray OLM_UTILITY_FUNC_DEF(encryptBackupKeyJni)(JNIEnv *env, jobject thiz, jbyteArray backupKey, jbyteArray key, jbyteArray info, jbyteArray ciphertext, jbyteArray mac, jbyteArray iv){
    
    jbyteArray ivRet = NULL;

    jbyte* backupKeyPtr = NULL;
    jbyte* keyPtr = NULL;
    jbyte* infoPtr = NULL;

    jboolean backupKeyWasCopied = JNI_FALSE;
    jboolean keyWasCopied = JNI_FALSE;
    jboolean infoWasCopied = JNI_FALSE;

    void* ciphertextPtr = NULL;
    void* macPtr = NULL;

    uint8_t * ivPtr = NULL;
    size_t ivSize = 0;

    OlmUtility* utilityPtr = getUtilityInstanceId(env, thiz);

    LOGD("## encryptBackupKeyJni(): IN");

    if (!utilityPtr)
    {
        LOGE(" ## encryptBackupKeyJni(): failure - invalid utility ptr=NULL");

    }
    else if(!backupKey || !info || !key || !ciphertext || !mac)
    {
        LOGE(" ## encryptBackupKeyJni(): failure - invalid setup parameters ");
    }
    else if (!(backupKeyPtr = env->GetByteArrayElements(backupKey, &backupKeyWasCopied)) || 
             !(keyPtr = env->GetByteArrayElements(key, &keyWasCopied))                   ||
             !(infoPtr = env->GetByteArrayElements(info, &infoWasCopied)))
    {
        LOGE(" ## encryptBackupKeyJni(): failure - JNI allocation OOM");
    }
    else{
        size_t backupKeyLength = (size_t)env->GetArrayLength(backupKey);
        size_t keyLength = (size_t)env->GetArrayLength(key);
        size_t infoLength = (size_t)env->GetArrayLength(info);
        size_t ciphertextLength = (size_t)env->GetArrayLength(ciphertext);
        size_t macLength = (size_t)env->GetArrayLength(mac);

        ciphertextPtr = malloc(ciphertextLength * sizeof(uint8_t));
        macPtr = malloc(macLength * sizeof(uint8_t));

        if(!ciphertextPtr || !macPtr)
        {
            LOGE(" ## encryptBackupKeyJni(): failure - JNI allocation OOM");
        }
        else
        {
            ivSize = olm_aes_random_length(utilityPtr);
            LOGD(" ## encryptBackupKeyJni(): iv size = %lu",static_cast<long unsigned int>(ivSize));

            if ( (0 != ivSize) && !setRandomInBufferPRG(env, &ivPtr, ivSize))
            {
                LOGE("## encryptBackupKeyJni(): failure - iv buffer init");
            }
            else
            {
                LOGD(" ## encryptBackupKeyJni(): encrypting key ...");
                size_t result = olm_encrypt_backup_key(
                    utilityPtr,
                    (void const *)backupKeyPtr, backupKeyLength,
                    (void const *)keyPtr, keyLength,
                    (void const *)infoPtr, infoLength,
                    (void const *)ivPtr, ivSize,
                    ciphertextPtr, ciphertextLength,
                    macPtr, macLength
                );

                if (result == olm_error()) 
                {
                    const char *errorMsgPtr = olm_utility_last_error(utilityPtr);
                    LOGE("## encryptBackupKeyJni(): failure - olm_encrypt_backup_key Msg=%s",errorMsgPtr);
                }
                else
                {
                    env->SetByteArrayRegion(ciphertext, 0 , ciphertextLength, (jbyte*)ciphertextPtr);
                    env->SetByteArrayRegion(mac, 0 , macLength, (jbyte*)macPtr);

                    ivRet = env->NewByteArray(olm_aes_random_length(utilityPtr));
                    env->SetByteArrayRegion(ivRet, 0 , olm_aes_random_length(utilityPtr), (jbyte*)ivPtr);

                    LOGD("## encryptBackupKeyJni(): success - result=%lu", static_cast<long unsigned int>(result));
                }
            }

        }
    }

    if (backupKeyPtr)
    {
        if (backupKeyWasCopied) {
            memset(backupKeyPtr, 0, (size_t)env->GetArrayLength(backupKey));
        }
        env->ReleaseByteArrayElements(backupKey, backupKeyPtr, JNI_ABORT);
    }
    if (keyPtr)
    {
        if (keyWasCopied) {
            memset(keyPtr, 0, (size_t)env->GetArrayLength(key));
        }
        env->ReleaseByteArrayElements(key, keyPtr, JNI_ABORT);
    }
    if (infoPtr)
    {
        if (infoWasCopied) {
            memset(infoPtr, 0, (size_t)env->GetArrayLength(info));
        }
        env->ReleaseByteArrayElements(info, infoPtr, JNI_ABORT);
    }

    if(ciphertextPtr){
        free(ciphertextPtr);
    }
    if(macPtr){
        free(macPtr);
    }

    return ivRet;
}



JNIEXPORT jstring OLM_UTILITY_FUNC_DEF(decryptBackupKeyJni)(JNIEnv *env, jobject thiz, jbyteArray ciphertext, jbyteArray mac, jbyteArray iv, jbyteArray info, jbyteArray key, jbyteArray backupKey){
    
    jstring errorMessageRetValue = 0;

    jbyte* ciphertextPtr = NULL;
    jbyte* macPtr = NULL;
    jbyte* keyPtr = NULL;
    jbyte* infoPtr = NULL;
    jbyte* ivPtr = NULL;

    jboolean ciphertextWasCopied = JNI_FALSE;
    jboolean macWasCopied = JNI_FALSE;
    jboolean keyWasCopied = JNI_FALSE;
    jboolean infoWasCopied = JNI_FALSE;
    jboolean ivWasCopied = JNI_FALSE;

    void* backupKeyPtr = NULL;

    OlmUtility* utilityPtr = getUtilityInstanceId(env, thiz);

    LOGD("## decryptBackupKeyJni(): IN");

    if (!utilityPtr)
    {
        LOGE(" ## decryptBackupKeyJni(): failure - invalid utility ptr=NULL");
        errorMessageRetValue = env->NewStringUTF("invalid utility ptr=NULL");

    }
    else if(!backupKey || !info || !key || !ciphertext || !mac || !iv)
    {
        LOGE(" ## decryptBackupKeyJni(): failure - invalid setup parameters ");
        errorMessageRetValue = env->NewStringUTF("invalid setup parameters");
    }
    else if (!(ciphertextPtr = env->GetByteArrayElements(ciphertext, &ciphertextWasCopied)) || 
             !(keyPtr = env->GetByteArrayElements(key, &keyWasCopied))                       ||
             !(infoPtr = env->GetByteArrayElements(info, &infoWasCopied))                    ||
             !(macPtr = env->GetByteArrayElements(mac, &macWasCopied))                       ||
             !(ivPtr = env->GetByteArrayElements(iv, &ivWasCopied)) )
    {
        LOGE(" ## decryptBackupKeyJni(): failure - JNI allocation OOM");
        errorMessageRetValue = env->NewStringUTF("JNI allocation OOM");
    }
    else{
        size_t keyLength = (size_t)env->GetArrayLength(key);
        size_t infoLength = (size_t)env->GetArrayLength(info);
        size_t ciphertextLength = (size_t)env->GetArrayLength(ciphertext);
        size_t macLength = (size_t)env->GetArrayLength(mac);
        size_t ivLength = (size_t)env->GetArrayLength(iv);
        size_t backupKeyLength = (size_t)env->GetArrayLength(backupKey);

        backupKeyPtr = malloc(backupKeyLength * sizeof(uint8_t));

        if(!backupKeyPtr)
        {
            LOGE(" ## decryptBackupKeyJni(): failure - JNI allocation OOM");
            errorMessageRetValue = env->NewStringUTF("JNI allocation OOM");
        }
        else
        {
            LOGD(" ## decryptBackupKeyJni(): decrypting key ...");
            size_t result = olm_decrypt_backup_key(
                utilityPtr,
                (void const *)ciphertextPtr, ciphertextLength,
                (void const *)macPtr, macLength,
                (void const *)keyPtr, keyLength,
                (void const *)infoPtr, infoLength,
                (void const *)ivPtr, ivLength,
                backupKeyPtr, backupKeyLength
                
            );

            if (result == olm_error()) 
            {
                const char *errorMsgPtr = olm_utility_last_error(utilityPtr);
                errorMessageRetValue = env->NewStringUTF(errorMsgPtr);
                LOGE("## decryptBackupKeyJni(): failure - olm_encrypt_backup_key Msg=%s",errorMsgPtr);
            }
            else
            {
                env->SetByteArrayRegion(backupKey, 0 , backupKeyLength, (jbyte*)backupKeyPtr);

                LOGD("## decryptBackupKeyJni(): success - result=%lu", static_cast<long unsigned int>(result));
            }

        }
    }

    if (backupKeyPtr)
    {
        free(backupKeyPtr);
    }
    if (keyPtr)
    {
        if (keyWasCopied) {
            memset(keyPtr, 0, (size_t)env->GetArrayLength(key));
        }
        env->ReleaseByteArrayElements(key, keyPtr, JNI_ABORT);
    }
    if (infoPtr)
    {
        if (infoWasCopied) {
            memset(infoPtr, 0, (size_t)env->GetArrayLength(info));
        }
        env->ReleaseByteArrayElements(info, infoPtr, JNI_ABORT);
    }

    if (ciphertextPtr)
    {
        if (ciphertextWasCopied) {
            memset(ciphertextPtr, 0, (size_t)env->GetArrayLength(ciphertext));
        }
        env->ReleaseByteArrayElements(ciphertext, ciphertextPtr, JNI_ABORT);
    }
    if (macPtr)
    {
        if (macWasCopied) {
            memset(macPtr, 0, (size_t)env->GetArrayLength(mac));
        }
        env->ReleaseByteArrayElements(mac, macPtr, JNI_ABORT);
    }
    if (ivPtr)
    {
        if (ivWasCopied) {
            memset(ivPtr, 0, (size_t)env->GetArrayLength(iv));
        }
        env->ReleaseByteArrayElements(iv, ivPtr, JNI_ABORT);
    }

    return errorMessageRetValue;
}



JNIEXPORT jlong OLM_UTILITY_FUNC_DEF(getTimestampJni)(JNIEnv *env, jobject thiz){
    OlmUtility* utilityPtr = getUtilityInstanceId(env, thiz);

    LOGD("## getTimestampJni(): IN new");

    if (!utilityPtr)
    {
        LOGE(" ## getTimestampJni(): failure - invalid utility ptr=NULL");
        return 1;
    }
    else{
        int64_t res = olm_get_timestamp(utilityPtr);

        return (jlong)res;
    }

    return 1;
}