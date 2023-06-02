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

package org.matrix.olm;

import android.text.TextUtils;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPublicKey;

public class OlmAttachmentUtility {
    private static final String LOG_TAG = "OlmAttachmentUtility";

    private static final String INFO_TAG = "attachment";
    private static final int KEY_SIZE = 43;
    private static final int CIPHER_SIZE = 6;

    /** Instance Id returned by JNI.
     * This value uniquely identifies this utility instance.
     **/
    private long mNativeId;

    public OlmAttachmentUtility() throws OlmException  {
        initAttachmentUtility();
    }

    /**
     * Create a native utility instance.
     * To be called before any other API call.
     * @exception OlmException the exception
     */
    private void initAttachmentUtility() throws OlmException {
        try {
            mNativeId = createAttachmentUtilityJni();
        } catch (Exception e) {
            throw new OlmException(OlmException.EXCEPTION_CODE_ATTACHMENT_UTILITY_CREATION, e.getMessage());
        }
    }

    private native long createAttachmentUtilityJni();

    /**
     * Release native instance.<br>
     * Public API for {@link #releaseAttachmentUtilityJni()}.
     */
    public void releaseAttachmentUtility() {
        if (0 != mNativeId) {
            releaseAttachmentUtilityJni();
        }
        mNativeId = 0;
    }
    private native void releaseAttachmentUtilityJni();

    /**
     * Return true the object resources have been released.<br>
     * @return true the object resources have been released
     */
    public boolean isReleased() {
        return (0 == mNativeId);
    }



    public OlmAttachmentMessage encryptAttachment(byte[] plaintext, String sessionKey) throws OlmException, UnsupportedEncodingException {
        if((plaintext == null) || (sessionKey == null)){
            throw new OlmException(OlmException.EXCEPTION_CODE_ATTACHMENT_UTILITY_INVALID_INPUT, "Invalid empty input");
        }

        String errorMessage = "";
        OlmAttachmentMessage olmAttachmentMessage = new OlmAttachmentMessage();
        try{

            errorMessage = encryptAttachmentJni(
                plaintext,
                INFO_TAG.getBytes("UTF-8"),
                sessionKey.getBytes("UTF-8"),
                olmAttachmentMessage
            );

        }catch(Exception exc){
            errorMessage = exc.getMessage();
            throw new OlmException(OlmException.EXCEPTION_CODE_ATTACHMENT_UTILITY_ENCRYPTION_ERROR, errorMessage);
        }

        if (!TextUtils.isEmpty(errorMessage)) {
            throw new OlmException(OlmException.EXCEPTION_CODE_ATTACHMENT_UTILITY_ENCRYPTION_ERROR, errorMessage);
        }

        int length = olmAttachmentMessage.ciphertext.getBytes("UTF-8").length;
        byte[] bytes = ByteBuffer.allocate(4).putInt(length).array();
        olmAttachmentMessage.ciphertextLength = Base64.encodeToString(bytes, Base64.NO_PADDING | Base64.NO_WRAP);
        return olmAttachmentMessage;
    }

    private native String encryptAttachmentJni(byte[] plaintext, byte[] info, byte[] sessionKey, OlmAttachmentMessage olmAttachmentMessage);

    public int getCiphertextLength(String length){
        ByteBuffer wrapped = ByteBuffer.wrap(Base64.decode(length, Base64.DEFAULT));
        int num = wrapped.getInt();
        return num;
    }

    public OlmAttachmentMessage getAttachmentEncryptionInfo(String keys, String iv, byte[] ciphertext, String mac) throws OlmException, UnsupportedEncodingException {
        if(keys == null || iv == null || ciphertext == null || mac == null){
            throw new OlmException(OlmException.EXCEPTION_CODE_ATTACHMENT_UTILITY_INVALID_INPUT, "Invalid empty input");
        }
        if(keys.length() != KEY_SIZE*2+CIPHER_SIZE){
            throw new OlmException(OlmException.EXCEPTION_CODE_ATTACHMENT_UTILITY_INVALID_KEY_SIZE, "Invalid key size");
        }

        OlmAttachmentMessage msg = new OlmAttachmentMessage();
        msg.ciphertext = new String(ciphertext, "UTF-8");
        msg.mac = mac;
        msg.ivAes = iv;
        msg.keyAes = keys.substring(0, KEY_SIZE);
        msg.keyMac = keys.substring(KEY_SIZE, 2*KEY_SIZE);
        msg.ciphertextLength = keys.substring(2*KEY_SIZE);

        ByteBuffer wrapped = ByteBuffer.wrap(Base64.decode(msg.ciphertextLength, Base64.DEFAULT));
        int length = wrapped.getInt();
        msg.ciphertext = msg.ciphertext.substring(0, length);

        return msg;

    }

    public byte[] decryptAttachment(OlmAttachmentMessage olmAttachmentMessage) throws OlmException, UnsupportedEncodingException {
        if(olmAttachmentMessage == null){
            throw new OlmException(OlmException.EXCEPTION_CODE_ATTACHMENT_UTILITY_INVALID_INPUT, "Invalid empty input");
        }

        String errorMessage = "";
        int plaintextLength = Base64.decode(olmAttachmentMessage.ciphertext, Base64.DEFAULT).length;
        byte[] plaintext = new byte[plaintextLength];
        try{
            errorMessage = decryptAttachmentJni(
                olmAttachmentMessage, plaintext
            );

        }catch(Exception exc){
            errorMessage = exc.getMessage();
            throw new OlmException(OlmException.EXCEPTION_CODE_ATTACHMENT_UTILITY_DECRYPTION_ERROR, errorMessage);
        }

        if (!TextUtils.isEmpty(errorMessage)) {
            throw new OlmException(OlmException.EXCEPTION_CODE_ATTACHMENT_UTILITY_DECRYPTION_ERROR, errorMessage);
        }

        return plaintext;
    }

    private native String decryptAttachmentJni(OlmAttachmentMessage olmAttachmentMessage, byte[] plaintext);

}
