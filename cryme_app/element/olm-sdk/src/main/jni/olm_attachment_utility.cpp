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

#include "olm_attachment_utility.h"

using namespace AndroidOlmSdk;

OlmAttachmentUtility* initializeAttachmentUtilityMemory()
{
    size_t utilitySize = olm_attachment_utility_size();
    OlmAttachmentUtility* attachmentUtilityPtr = (OlmAttachmentUtility*)malloc(utilitySize);

    if (attachmentUtilityPtr)
    {
        attachmentUtilityPtr = olm_attachment_utility(attachmentUtilityPtr);
        LOGD("## initializeAttachmentUtilityMemory(): success - OLM utility size=%lu",static_cast<long unsigned int>(utilitySize));
    }
    else
    {
        LOGE("## initializeAttachmentUtilityMemory(): failure - OOM");
    }

    return attachmentUtilityPtr;
}

JNIEXPORT jlong OLM_ATTACHMENT_UTILITY_FUNC_DEF(createAttachmentUtilityJni)(JNIEnv *env, jobject thiz)
{
    OlmAttachmentUtility* attachmentutilityPtr = initializeAttachmentUtilityMemory();

    LOGD("## createAttachmentUtilityJni(): IN");

    // init account memory allocation
    if (!attachmentutilityPtr)
    {
        LOGE(" ## createAttachmentUtilityJni(): failure - init OOM");
        env->ThrowNew(env->FindClass("java/lang/Exception"), "init OOM");
    }
    else
    {
       LOGD(" ## createAttachmentUtilityJni(): success");
    }

    return (jlong)(intptr_t)attachmentutilityPtr;
}


JNIEXPORT void OLM_ATTACHMENT_UTILITY_FUNC_DEF(releaseAttachmentUtilityJni)(JNIEnv *env, jobject thiz)
{
    OlmAttachmentUtility* attachmentutilityPtr = getAttachmentUtilityInstanceId(env, thiz);

    LOGD("## releaseAttachmentUtilityJni(): IN");

    if (!attachmentutilityPtr)
    {
        LOGE("## releaseAttachmentUtilityJni(): failure - utility ptr=NULL");
    }
    else
    {
        olm_clear_attachment_utility(attachmentutilityPtr);
        free(attachmentutilityPtr);
    }
}

JNIEXPORT jstring OLM_ATTACHMENT_UTILITY_FUNC_DEF(encryptAttachmentJni)
    (JNIEnv *env, jobject thiz, jbyteArray aPlaintextBuffer, jbyteArray info, jbyteArray sessionKey, jobject aEncryptedMsg){

    OlmAttachmentUtility* attachmentutilityPtr = getAttachmentUtilityInstanceId(env, thiz);

    jstring errorMessageRetValue = 0;

    jbyte* messagePtr = NULL;
    jbyte* infoPtr = NULL;
    jbyte* sessionKeyPtr = NULL;
    jboolean messageWasCopied = JNI_FALSE;
    jboolean infoWasCopied = JNI_FALSE;
    jboolean sessionKeyWasCopied = JNI_FALSE;

    jclass encryptedMsgJClass = 0;
    jfieldID ciphertextFieldId;
    jfieldID macFieldId;
    jfieldID keyAesFieldId;
    jfieldID ivAesFieldId;
    jfieldID keyMacFieldId;

    LOGD("## encryptAttachmentJni(): IN");

    if (!attachmentutilityPtr)
    {
        LOGE(" ## encryptAttachmentJni(): failure - invalid attachment utility ptr=NULL");
        errorMessageRetValue = env->NewStringUTF("invalid attachment utility ptr=NULL");
        
    }
    else if (!aPlaintextBuffer || !info || !aEncryptedMsg)
    {
        LOGE(" ## encryptAttachmentJni(): failure - invalid input parameters ");
        errorMessageRetValue = env->NewStringUTF("invalid input parameters");
    }
    else if (!(messagePtr = env->GetByteArrayElements(aPlaintextBuffer, &messageWasCopied)) ||
             !(infoPtr = env->GetByteArrayElements(info, &infoWasCopied))                   ||
             !(sessionKeyPtr = env->GetByteArrayElements(sessionKey, &sessionKeyWasCopied)))
    {
        LOGE(" ## encryptAttachmentJni(): failure - JNI allocation OOM");
        errorMessageRetValue = env->NewStringUTF("JNI allocation OOM");
    }
    else if (!(encryptedMsgJClass = env->GetObjectClass(aEncryptedMsg)))
    {
        LOGE(" ## encryptAttachmentJni(): failure - unable to get attachment encrypted message class");
        errorMessageRetValue = env->NewStringUTF("unable to get attachment encrypted message class");
    }
    else if (!(ciphertextFieldId = env->GetFieldID(encryptedMsgJClass, "ciphertext", "Ljava/lang/String;")) ||
             !(macFieldId = env->GetFieldID(encryptedMsgJClass, "mac", "Ljava/lang/String;"))               ||
             !(keyAesFieldId = env->GetFieldID(encryptedMsgJClass, "keyAes", "Ljava/lang/String;"))         ||
             !(ivAesFieldId = env->GetFieldID(encryptedMsgJClass, "ivAes", "Ljava/lang/String;"))           ||
             !(keyMacFieldId = env->GetFieldID(encryptedMsgJClass, "keyMac", "Ljava/lang/String;")))
    {
        LOGE("## encryptAttachmentJni(): failure - unable to get field(s)");
        errorMessageRetValue = env->NewStringUTF("unable to get field(s)");
    }
    else
    {
        size_t messageLength = (size_t)env->GetArrayLength(aPlaintextBuffer);
        size_t sessionKeyLength = (size_t)env->GetArrayLength(sessionKey);
        size_t infoLength = (size_t)env->GetArrayLength(info);
        size_t ciphertextLength = olm_ciphertext_attachment_length(attachmentutilityPtr, messageLength);
        size_t macLength = olm_mac_attachment_length(attachmentutilityPtr);
        size_t keyAesLength = olm_aes_key_attachment_length(attachmentutilityPtr);
        size_t ivAesLength = olm_aes_iv_attachment_length(attachmentutilityPtr);
        size_t keyMacLength = olm_mac_key_attachment_length(attachmentutilityPtr);
        uint8_t *ciphertextPtr = NULL, *macPtr = NULL, *keyAesPtr = NULL, *ivAesPtr = NULL, *keyMacPtr = NULL;

        if (!(ciphertextPtr = (uint8_t*)malloc(ciphertextLength+1)) ||
            !(macPtr = (uint8_t *)malloc(macLength+1)) ||
            !(keyAesPtr = (uint8_t *)malloc(keyAesLength+1)) ||
            !(ivAesPtr = (uint8_t *)malloc(ivAesLength+1)) ||
            !(keyMacPtr = (uint8_t *)malloc(keyMacLength+1)))
        {
            LOGE("## encryptAttachmentJni(): failure - JNI allocation OOM");
            errorMessageRetValue = env->NewStringUTF("JNI allocation OOM");
        }
        else{
            ciphertextPtr[ciphertextLength] = '\0';
            macPtr[macLength] = '\0';
            keyAesPtr[keyAesLength] = '\0';
            keyMacPtr[keyMacLength] = '\0';
            ivAesPtr[ivAesLength] = '\0';

            size_t result = olm_encrypt_attachment(
                attachmentutilityPtr,
                (const void*)messagePtr, messageLength,
                (const void*)sessionKeyPtr, sessionKeyLength,
                (const void*)infoPtr, infoLength,
                (void *)keyAesPtr, keyAesLength,
                (void *)ivAesPtr, ivAesLength,
                (void *)keyMacPtr, keyMacLength,
                (void *)ciphertextPtr, ciphertextLength,
                (void *)macPtr, macLength
            );

            if (result == olm_error())
            {
                const char * errorMessage = olm_attachment_utility_last_error(attachmentutilityPtr);
                LOGE("## encryptAttachmentJni(): failure - olm_encrypt_attachment Msg=%s", errorMessage);
                LOGE("## encryptAttachmentJni(): failure - size of base64 session key=%zu", sessionKeyLength);
                errorMessageRetValue = env->NewStringUTF(errorMessage);
            }
            else
            {
                jstring ciphertextStr = env->NewStringUTF((char*)ciphertextPtr);
                env->SetObjectField(aEncryptedMsg, ciphertextFieldId, ciphertextStr);

                jstring macStr = env->NewStringUTF((char*)macPtr);
                env->SetObjectField(aEncryptedMsg, macFieldId, macStr);

                jstring keyAesStr = env->NewStringUTF((char*)keyAesPtr);
                env->SetObjectField(aEncryptedMsg, keyAesFieldId, keyAesStr);

                jstring ivAesStr = env->NewStringUTF((char*)ivAesPtr);
                env->SetObjectField(aEncryptedMsg, ivAesFieldId, ivAesStr);

                jstring keyMacStr = env->NewStringUTF((char*)keyMacPtr);
                env->SetObjectField(aEncryptedMsg, keyMacFieldId, keyMacStr);
            }
        }
        
        if(ciphertextPtr){ 
            free(ciphertextPtr);
        }
        if(macPtr){ 
            free(macPtr);
        }
        if(keyAesPtr){ 
            free(keyAesPtr);
        }
        if(ivAesPtr){ 
            free(ivAesPtr);
        }
        if(keyMacPtr){ 
            free(keyMacPtr);
        }
    }

    //free alloc
    if (messagePtr)
    {
        if (messageWasCopied) {
            memset(messagePtr, 0, (size_t)env->GetArrayLength(aPlaintextBuffer));
        }
        env->ReleaseByteArrayElements(aPlaintextBuffer, messagePtr, JNI_ABORT);
    }

    if (infoPtr)
    {
        if (infoWasCopied) {
            memset(infoPtr, 0, (size_t)env->GetArrayLength(info));
        }
        env->ReleaseByteArrayElements(info, infoPtr, JNI_ABORT);
    }

    if (sessionKeyPtr)
    {
        if (sessionKeyWasCopied) {
            memset(sessionKeyPtr, 0, (size_t)env->GetArrayLength(sessionKey));
        }
        env->ReleaseByteArrayElements(sessionKey, sessionKeyPtr, JNI_ABORT);
    }

    return errorMessageRetValue;
}



JNIEXPORT jstring OLM_ATTACHMENT_UTILITY_FUNC_DEF(decryptAttachmentJni)(
    JNIEnv *env, jobject thiz, jobject aEncryptedMsg, jbyteArray plaintext
){
    OlmAttachmentUtility* attachmentutilityPtr = getAttachmentUtilityInstanceId(env, thiz);

    jstring errorMessageRetValue = 0;

    jclass encryptedMsgJClass = 0;
    jfieldID ciphertextFieldId;
    jfieldID macFieldId;
    jfieldID keyAesFieldId;
    jfieldID ivAesFieldId;
    jfieldID keyMacFieldId;

    LOGD("## decryptAttachmentJni(): IN");

    if (!attachmentutilityPtr)
    {
        LOGE(" ## decryptAttachmentJni(): failure - invalid attachment utility ptr=NULL");
        errorMessageRetValue = env->NewStringUTF("invalid attachment utility ptr=NULL");
        
    }
    else if (!plaintext || !aEncryptedMsg)
    {
        LOGE(" ## decryptAttachmentJni(): failure - invalid input parameters ");
        errorMessageRetValue = env->NewStringUTF("invalid input parameters");
    }
    else if (!(encryptedMsgJClass = env->GetObjectClass(aEncryptedMsg)))
    {
        LOGE(" ## decryptAttachmentJni(): failure - unable to get attachment encrypted message class");
        errorMessageRetValue = env->NewStringUTF("unable to get attachment encrypted message class");
    }
    else if (!(ciphertextFieldId = env->GetFieldID(encryptedMsgJClass, "ciphertext", "Ljava/lang/String;")) ||
             !(macFieldId = env->GetFieldID(encryptedMsgJClass, "mac", "Ljava/lang/String;"))               ||
             !(keyAesFieldId = env->GetFieldID(encryptedMsgJClass, "keyAes", "Ljava/lang/String;"))         ||
             !(ivAesFieldId = env->GetFieldID(encryptedMsgJClass, "ivAes", "Ljava/lang/String;"))           ||
             !(keyMacFieldId = env->GetFieldID(encryptedMsgJClass, "keyMac", "Ljava/lang/String;")))
    {
        LOGE("## decryptAttachmentJni(): failure - unable to get field(s)");
        errorMessageRetValue = env->NewStringUTF("unable to get field(s)");
    }
    else
    {

        jstring ciphertextStr = (jstring)env->GetObjectField(aEncryptedMsg, ciphertextFieldId);
        jstring macStr = (jstring)env->GetObjectField(aEncryptedMsg, macFieldId);
        jstring aesKeyStr = (jstring)env->GetObjectField(aEncryptedMsg, keyAesFieldId);
        jstring aesIvStr = (jstring)env->GetObjectField(aEncryptedMsg, ivAesFieldId);
        jstring macKeyStr = (jstring)env->GetObjectField(aEncryptedMsg, keyMacFieldId);

        if(!ciphertextStr || !macStr || !aesKeyStr || !aesIvStr || !macKeyStr){
            LOGE("## decryptAttachmentJni(): failure - unable to get field(s) values");
            errorMessageRetValue = env->NewStringUTF("unable to get field(s) values");
        }
        else
        {
            const char *ciphertextPtr = NULL, *macPtr = NULL, *keyAesPtr = NULL, *ivAesPtr = NULL, *keyMacPtr = NULL;
            jboolean ciphertextWasCopied = JNI_FALSE;
            jboolean macWasCopied = JNI_FALSE;
            jboolean keyAesWasCopied = JNI_FALSE;
            jboolean ivAesWasCopied = JNI_FALSE;
            jboolean keyMacWasCopied = JNI_FALSE;

            size_t plaintextLength = (size_t)env->GetArrayLength(plaintext);
            size_t ciphertextLength = olm_ciphertext_attachment_length(attachmentutilityPtr, plaintextLength);
            size_t macLength = olm_mac_attachment_length(attachmentutilityPtr);
            size_t keyAesLength = olm_aes_key_attachment_length(attachmentutilityPtr);
            size_t ivAesLength = olm_aes_iv_attachment_length(attachmentutilityPtr);
            size_t keyMacLength = olm_mac_key_attachment_length(attachmentutilityPtr);

            ciphertextPtr = env->GetStringUTFChars(ciphertextStr, &ciphertextWasCopied);
            macPtr = env->GetStringUTFChars(macStr, &macWasCopied);
            keyAesPtr = env->GetStringUTFChars(aesKeyStr, &keyAesWasCopied);
            ivAesPtr = env->GetStringUTFChars(aesIvStr, &ivAesWasCopied);
            keyMacPtr = env->GetStringUTFChars(macKeyStr, &keyMacWasCopied);

            if(!ciphertextPtr || !macPtr || !keyAesPtr || !ivAesPtr || !keyMacPtr){
                LOGE("## decryptAttachmentJni(): failure - unable to get field(s) values ptr");
                errorMessageRetValue = env->NewStringUTF("unable to get field(s) values ptr");
            }
            else{

                void * plaintextPtr = NULL;
                plaintextPtr = malloc(plaintextLength*sizeof(uint8_t));
                if(!plaintextPtr){
                    LOGE("## decryptAttachmentJni(): failure - umemory JNI allocation OOM");
                    errorMessageRetValue = env->NewStringUTF("memory JNI allocation OOM");
                }
                else{

                    LOGD("## decryptAttachmentJni(): proceeding to decryption");
                    size_t result = olm_decrypt_attachment(
                        attachmentutilityPtr,
                        (const void *)ciphertextPtr, ciphertextLength,
                        (const void *)macPtr, macLength,
                        (const void *)keyAesPtr, keyAesLength,
                        (const void *)ivAesPtr, ivAesLength,
                        (const void *)keyMacPtr, keyMacLength,
                        (void *)plaintextPtr, plaintextLength
                    );

                    if (result == olm_error())
                    {
                        const char * errorMessage = olm_attachment_utility_last_error(attachmentutilityPtr);
                        LOGE("## decryptAttachmentJni(): failure - olm_encrypt_attachment Msg=%s", errorMessage);
                        errorMessageRetValue = env->NewStringUTF(errorMessage);
                    }
                    else
                    {
                        env->SetByteArrayRegion(plaintext, 0 , plaintextLength, (jbyte*)plaintextPtr);
                        LOGD("## decryptAttachmentJni(): success - result=%lu", static_cast<long unsigned int>(result));
                    }

                }

                if(plaintextPtr){
                    free(plaintextPtr);
                }

            }

            if(ciphertextPtr){

                env->ReleaseStringUTFChars(ciphertextStr, ciphertextPtr);
            }
            if(macPtr){

                env->ReleaseStringUTFChars(macStr, macPtr);
            }
            if(keyAesPtr){

                env->ReleaseStringUTFChars(aesKeyStr, keyAesPtr);
            }
            if(ivAesPtr){

                env->ReleaseStringUTFChars(aesIvStr, ivAesPtr);
            }
            if(keyMacPtr){

                env->ReleaseStringUTFChars(macKeyStr, keyMacPtr);
            }
        }
    }

    return errorMessageRetValue;
}