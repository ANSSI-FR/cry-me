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

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;

public class OlmPRGUtility {
    private static final String LOG_TAG = "OlmPRGUtility";
    private static OlmPRGUtility instance = null;
    private static final String NONCE = "Element-CRY.ME-PRG";

    /** Instance Id returned by JNI.
     * This value uniquely identifies this prg utility instance.
     **/
    private long mNativeId = 0;

    /**
     * Private Constructor since this is a singleton class.
     * To get a PRG instance, one must call the static method
     * getInstance()
     * @throws OlmException in case of initilization error
     * @throws UnsupportedEncodingException in case of encoding error
     */
    private OlmPRGUtility() throws OlmException, UnsupportedEncodingException {
        initPRGUtility();
    }

    /**
     * Create a native PRG utility instance.
     * To be called before any other API call.
     * @exception OlmException the exception
     */
    private void initPRGUtility() throws OlmException {
        try {
            mNativeId = createPRGUtilityJni();
            initPRG();
        } catch (Exception e) {
            throw new OlmException(OlmException.EXCEPTION_CODE_PRG_UTILITY_CREATION, e.getMessage());
        }
    }
    private native long createPRGUtilityJni();

    private void initPRG() throws OlmException, UnsupportedEncodingException {
        String errorMessage = "";
        try{
            errorMessage = initPRGJni(NONCE.getBytes("UTF-8"));
            if (!TextUtils.isEmpty(errorMessage)) {
                throw new OlmException(OlmException.EXCEPTION_CODE_PRG_UTILITY_CREATION, errorMessage);
            }
        } catch (Exception e) {
            throw new OlmException(OlmException.EXCEPTION_CODE_PRG_UTILITY_CREATION, e.getMessage());
        }

    }
    private native String initPRGJni(byte[] nounce);


    private void releasePRGUtilityInstance(){
        if (0 != mNativeId) {
            releasePRGUtilityJni();
        }
        mNativeId = 0;
    }

    /**
     * Release native instance.<br>
     * Public API for {@link #releasePRGUtilityJni()}.
     */
    public static void releasePRGUtility() {
        if (null != instance) {
            instance.releasePRGUtilityInstance();
        }
        instance = null;
    }

    /**
     * Return true the object resources have been released.<br>
     * @return true the object resources have been released
     */
    public static boolean isReleased() {
        return (null == instance);
    }

    private native void releasePRGUtilityJni();

    //important singleton function
    public static OlmPRGUtility getInstance() throws OlmException, UnsupportedEncodingException {
        if(instance == null) {
            instance = new OlmPRGUtility();

        }
        return instance;
    }

    public byte[] getRandomBytes(int nbBytes) throws OlmException {
        byte bytes[] = new byte[nbBytes];

        fillWithRandomBytes(bytes);

        return bytes;
    }

    public void fillWithRandomBytes(byte[] arr) throws OlmException {
        String errorMessage = "";

        try {
            errorMessage = fillWithRandomBytesJni(arr);

        }catch(Exception exc){
            errorMessage = exc.getMessage();
            throw new OlmException(OlmException.EXCEPTION_CODE_PRG_RANDOM_BYTES_FILL, errorMessage);
        }

        if (!TextUtils.isEmpty(errorMessage)) {
            throw new OlmException(OlmException.EXCEPTION_CODE_PRG_RANDOM_BYTES_FILL, errorMessage);
        }
    }


    private native String fillWithRandomBytesJni(byte [] arr);

    // getter and setter
}
