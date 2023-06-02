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
 * Copyright 2017 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
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

package org.matrix.olm;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import java.math.BigInteger;
import java.util.Base64;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
/**
 * Olm SDK helper class.
 */
public class OlmRSAUtility {
    private static final String LOG_TAG = "OlmRSAUtility";

    /** Instance Id returned by JNI.
     * This value uniquely identifies this utility instance.
     **/
    private long mNativeId;

    private RSAPublicKeySpec pub = null;
    private RSAPrivateCrtKeySpec priv = null;
    private int tries = 0;
    public static final int MAX_TRIES = 3;


    public OlmRSAUtility() throws OlmException  {
        initRSAUtility();
    }

    /**
     * Create a native RSA utility instance.
     * To be called before any other API call.
     * @exception OlmException the exception
     */
    private void initRSAUtility() throws OlmException {
        try {
            mNativeId = createRSAUtilityJni();
        } catch (Exception e) {
            throw new OlmException(OlmException.EXCEPTION_CODE_RSA_UTILITY_CREATION, e.getMessage());
        }
    }

    private native long createRSAUtilityJni();

    /**
     * Release native instance.<br>
     * Public API for {@link #releaseRSAUtilityJni()}.
     */
    public void releaseRSAUtility() {
        if (0 != mNativeId) {
            releaseRSAUtilityJni();
        }
        mNativeId = 0;
    }
    private native void releaseRSAUtilityJni();

    /**
     * Return true the object resources have been released.<br>
     * @return true the object resources have been released
     */
    public boolean isReleased() {
        return (0 == mNativeId);
    }

    /**
     * Generates new RSA Keys
     * @throws OlmException if any of the key generation did not go well
     */
    public void generateRSAKeys() throws OlmException {

        String errorMessage = "";
        tries += 1;
        if(tries > MAX_TRIES){
            throw new OlmException(OlmException.EXCEPTION_CODE_RSA_UTILITY_KEY_GENERATION, "RSA_KEY_GEN_FAILED_WITH_MAX_TRIES");
        }

        byte[] nBytes = new byte[256];
        byte[] pBytes = new byte[128];
        byte[] qBytes = new byte[128];
        byte[] lcmBytes = new byte[256];

        try {
            errorMessage = genKeyRSAValuesJni(nBytes, pBytes, qBytes, lcmBytes);

        }catch(Exception exc){
            errorMessage = exc.getMessage();
            throw new OlmException(OlmException.EXCEPTION_CODE_RSA_UTILITY_KEY_GENERATION, errorMessage);
        }

        if (!TextUtils.isEmpty(errorMessage)) {
            if(errorMessage.equalsIgnoreCase("OLM_OUT_OF_RANDOMNESS")){
                generateRSAKeys();
            }
            else {
                throw new OlmException(OlmException.EXCEPTION_CODE_RSA_UTILITY_KEY_GENERATION, errorMessage);
            }
        }
        else{
            BigInteger mod = new BigInteger(1, nBytes);
            BigInteger e = new BigInteger("65537");
            BigInteger lcm = new BigInteger(1, lcmBytes);

            BigInteger d = e.modInverse(lcm);

            if(d.bitLength() <= 1024){
                generateRSAKeys();
                //throw new OlmException(OlmException.EXCEPTION_CODE_RSA_UTILITY_KEY_GENERATION, mod.toString());
            }
            else{
                BigInteger p =  new BigInteger(1, pBytes);
                BigInteger q =  new BigInteger(1, qBytes);
                BigInteger tmp = p.multiply(q);

                if(!tmp.equals(mod)){

                    throw new OlmException(OlmException.EXCEPTION_CODE_RSA_UTILITY_KEY_GENERATION, "p*q is different than mod !" + tmp.toString(10) + " and " + mod.toString(10));
                }

                BigInteger one = new BigInteger("1");
                BigInteger g = (p.subtract(one)).gcd(q.subtract(one));
                tmp = (p.subtract(one)).multiply(q.subtract(one));
                tmp = tmp.divide(g);

                if(!tmp.equals(lcm)){
                    throw new OlmException(OlmException.EXCEPTION_CODE_RSA_UTILITY_KEY_GENERATION, "lcm is wrong !" + tmp.toString(10) + " and " + lcm.toString(10));
                }

                BigInteger primeExponentP = d.mod(p.subtract(one));
                BigInteger primeExponentQ = d.mod(q.subtract(one));
                BigInteger crtCoefficient = q.modInverse(p);

                pub = new RSAPublicKeySpec(mod, e);

                priv = new RSAPrivateCrtKeySpec(mod, e, d, p, q, primeExponentP, primeExponentQ, crtCoefficient);

                tries = 0;
            }
        }
    }

    /**
     * @return the RSA public key spec generated
     * with generateRSAKeys() method
     */
    public RSAPublicKeySpec getPublicKey(){
        return pub;
    }

    /**
     * @return the RSA private key spec generated
     * with generateRSAKeys() method
     */
    public RSAPrivateCrtKeySpec getPrivateKey(){
        return priv;
    }

    /**
     * Generates new RSA 2048 Keys
     * @param n the empty modulus
     * @param p the empty factor
     * @param q the empty second factor
     * @param lcm lcm of p-1 and q-1
     * @return error Message
     */
    private native String genKeyRSAValuesJni(byte[] n, byte[] p, byte[] q, byte[] lcm);

    /**
     * Verify the server's accreditation
     * @param accreditation the server's accreditation
     * @param serverURL the server's public URL
     * @param certificate the server's public certificate for signature verification
     * @throws OlmException if any of the parameters are empty or if the accreditation is invalid
     */
    public void verifyAccreditation(String accreditation, String serverURL, String certificate) throws OlmException {
        if((accreditation == null) || (serverURL == null) || (certificate == null)){
            throw new OlmException(OlmException.EXCEPTION_CODE_RSA_UTILITY_INVALID_ACCREDITATION, "Invalid server accreditation");
        }

        String errorMessage = "";

        Certificate cert;
        RSAPublicKey pk;
        byte [] n_copy, e_copy;

        try{
            cert = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certificate.getBytes()));
            pk = (RSAPublicKey) cert.getPublicKey();

            BigInteger n = pk.getModulus();
            BigInteger e = pk.getPublicExponent();

            n_copy = new byte[n.toByteArray().length - 1];
            e_copy = new byte[e.toByteArray().length];
            System.arraycopy(n.toByteArray(), 1, n_copy, 0, n_copy.length);
            System.arraycopy(e.toByteArray(), 0, e_copy, 0, e_copy.length);

            errorMessage = verifyAccreditationJni(OlmUtility.hexStringToByteArray(accreditation),
                                                    serverURL.getBytes("UTF-8"),
                                                    n_copy,
                                                    e_copy);

        }catch(Exception exc){
            errorMessage = exc.getMessage();
            throw new OlmException(OlmException.EXCEPTION_CODE_RSA_UTILITY_INVALID_CERTIFICATE, errorMessage);
        }

        if (!TextUtils.isEmpty(errorMessage)) {
            throw new OlmException(OlmException.EXCEPTION_CODE_RSA_UTILITY_INVALID_ACCREDITATION, errorMessage);
        }
    }


     /**
     * Verify the server's accreditation
     * @param accreditation the server's accreditation
     * @param url the server's public URL
     * @param modulus n
     * @param publicExponent e
     * @return error Message
     */
    private native String verifyAccreditationJni(byte[] accreditation, byte[] url, byte[] modulus, byte[] publicExponent);

}


