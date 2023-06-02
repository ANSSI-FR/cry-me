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

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Olm SDK helper class.
 */
public class OlmUtility {
    private static final String LOG_TAG = "OlmUtility";

    public static final int RANDOM_KEY_SIZE = 32;

    public static final int MAC_SIZE = 32;
    public static final int AES_RANDOM_SIZE = 16;

    /** Instance Id returned by JNI.
     * This value uniquely identifies this utility instance.
     **/
    private long mNativeId;

    public OlmUtility() throws OlmException  {
        initUtility();
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String byteArraytoHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }


    /* s must be an even-length string. */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    /**
     * Create a native utility instance.
     * To be called before any other API call.
     * @exception OlmException the exception
     */
    private void initUtility() throws OlmException {
        try {
            mNativeId = createUtilityJni();
        } catch (Exception e) {
            throw new OlmException(OlmException.EXCEPTION_CODE_UTILITY_CREATION, e.getMessage());
        }
    }

    private native long createUtilityJni();

    /**
     * Release native instance.<br>
     * Public API for {@link #releaseUtilityJni()}.
     */
    public void releaseUtility() {
        if (0 != mNativeId) {
            releaseUtilityJni();
        }
        mNativeId = 0;
    }
    private native void releaseUtilityJni();

    /**
     * Verify an weisig25519 signature.<br>
     * An exception is thrown if the operation fails.
     * @param aSignature the base64-encoded message signature to be checked.
     * @param aFingerprintKey the weisig25519 key (fingerprint key)
     * @param aMessage the signed message
     * @exception OlmException the failure reason
     */
    public void verifyWeiSig25519Signature(String aSignature, String aFingerprintKey, String aMessage) throws OlmException {
        String errorMessage;
        byte[] messageBuffer = null;

        try {
            if (TextUtils.isEmpty(aSignature) || TextUtils.isEmpty(aFingerprintKey) || TextUtils.isEmpty(aMessage)) {
                Log.e(LOG_TAG, "## verifyWeiSig25519Signature(): invalid input parameters");
                errorMessage = "JAVA sanity check failure - invalid input parameters";
            } else {
                messageBuffer = aMessage.getBytes("UTF-8");
                errorMessage =  verifyWeiSig25519SignatureJni(aSignature.getBytes("UTF-8"), aFingerprintKey.getBytes("UTF-8"), messageBuffer);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "## verifyWeiSig25519Signature(): failed " + e.getMessage());
            errorMessage = e.getMessage();
        } finally {
            if (messageBuffer != null) {
                Arrays.fill(messageBuffer, (byte) 0);
            }
        }

        if (!TextUtils.isEmpty(errorMessage)) {
            throw new OlmException(OlmException.EXCEPTION_CODE_UTILITY_VERIFY_SIGNATURE, errorMessage);
        }
    }

    /**
     * Verify an weisig25519 signature.
     * Return a human readable error message in case of verification failure.
     * @param aSignature the base64-encoded message signature to be checked.
     * @param aFingerprintKey the weisig25519 key
     * @param aMessage the signed message
     * @return null if validation succeed, the error message string if operation failed
     */
    private native String verifyWeiSig25519SignatureJni(byte[] aSignature, byte[] aFingerprintKey, byte[] aMessage);

    /**
     * Compute the hash(SHA-256) value of the string given in parameter(aMessageToHash).<br>
     * The hash value is the returned by the method.
     * @param aMessageToHash message to be hashed
     * @return hash value if operation succeed, null otherwise
     */
     public String sha3(String aMessageToHash) {
         String hashRetValue = null;

         if (null != aMessageToHash) {
             byte[] messageBuffer = null;
             try {
                 messageBuffer = aMessageToHash.getBytes("UTF-8");
                 hashRetValue = new String(sha3Jni(messageBuffer), "UTF-8");
             } catch (Exception e) {
                 Log.e(LOG_TAG, "## sha3(): failed " + e.getMessage());
             } finally {
                 if (null != messageBuffer) {
                     Arrays.fill(messageBuffer, (byte) 0);
                 }
             }
         }

         return hashRetValue;
     }

     /**
      * Compute the hash value of the string given in parameter(aMessageToHash).<br>
      * The hash value is the returned by the method.
      * @param aMessageToHash message to be hashed
      * @return hash value if operation succeed, null otherwise
      */
     public String sha(String aMessageToHash) {
         String hashRetValue = null;

         if (null != aMessageToHash) {
             byte[] messageBuffer = null;
             try {
                 messageBuffer = aMessageToHash.getBytes("UTF-8");
                 hashRetValue = OlmUtility.byteArraytoHexString(shaJni(messageBuffer));
             } catch (Exception e) {
                 Log.e(LOG_TAG, "## sha(): failed " + e.getMessage());
             } finally {
                 if (null != messageBuffer) {
                     Arrays.fill(messageBuffer, (byte) 0);
                 }
             }
         }

        return hashRetValue;
    }

    /**
     * Compute the digest (SHA 256) for the message passed in parameter.<br>
     * The digest value is the function return value.
     * An exception is thrown if the operation fails.
     * @param aMessage the message
     * @return digest of the message.
     **/
    private native byte[] sha3Jni(byte[] aMessage);

    private native byte[] shaJni(byte[] aMessage);


    /**
     * Compute PBKDF2 on password and salt to generate key.<br>
     * The key value is returned by the method.
     * @param password the password
     * @param salt the salt
     * @param dklen the size of the key to derive
     * @param c number of iterations
     * @return the derived key.
     */
    public byte[] computePbkdf2(String password, String salt, int dklen, int c) {
        byte[] derivedKey = null;

        if ((null != password) && (null != salt)) {
            byte[] passwordBuffer = null;
            byte[] saltBuffer = null;
            try {
                passwordBuffer = password.getBytes("UTF-8");
                saltBuffer = hexStringToByteArray(salt);
                derivedKey = pbkdf2Jni(passwordBuffer, saltBuffer, dklen, c);
            } catch (Exception e) {
                Log.e(LOG_TAG, "## computePbkdf2(): failed " + e.getMessage());
            } finally {
                if (null != passwordBuffer) {
                    Arrays.fill(passwordBuffer, (byte) 0);
                }
                if (null != saltBuffer) {
                    Arrays.fill(saltBuffer, (byte) 0);
                }
            }
        }
        return derivedKey;
    }

    /**
     * PBKDF2: Key Derivation mechanism from password
     * Computes key of dklen bytes (raw bytes) from password and salt using
     * PBKDF2 with c iterations.
     * An exception is thrown if the operation fails.
     * @param password the password
     * @param salt the salt
     * @param dklen the size of the key to derive
     * @param c number of iterations
     * @return the derived key.
     **/
    private native byte[] pbkdf2Jni(byte[] password, byte[] salt, int dklen, int c);

    public OlmBackupKeyEncryption encryptBackupKey(String backupKey, String encryptionKey, String info){
        OlmBackupKeyEncryption msg = new OlmBackupKeyEncryption();

        if ((null != backupKey) && (null != encryptionKey) && (null != info)) {
            byte[] backupKeyBuffer = null;
            byte[] encryptionKeyBuffer = null;
            byte [] infoBuffer = null;
            try {
                infoBuffer = info.getBytes();
                backupKeyBuffer = hexStringToByteArray(backupKey);
                encryptionKeyBuffer = hexStringToByteArray(encryptionKey);

                byte[] ciphertext = new byte[backupKeyBuffer.length];
                byte[] mac = new byte[MAC_SIZE];
                byte[] iv = new byte[AES_RANDOM_SIZE];

                iv = encryptBackupKeyJni(backupKeyBuffer, encryptionKeyBuffer, infoBuffer, ciphertext, mac);
                if(iv == null){
                    throw new OlmException(OlmException.EXCEPTION_CODE_UTILITY_BACKUP_ENCRYPTION_ERROR, "failed to encrypt key");
                }

                msg.keyCiphertext = byteArraytoHexString(ciphertext);
                msg.keyMac = byteArraytoHexString(mac);
                msg.keyIv = byteArraytoHexString(iv);

            } catch (Exception e) {
                Log.e(LOG_TAG, "## encryptBackupKey(): failed " + e.getMessage());
            } finally {
                if (null != backupKeyBuffer) {
                    Arrays.fill(backupKeyBuffer, (byte) 0);
                }
                if (null != encryptionKeyBuffer) {
                    Arrays.fill(encryptionKeyBuffer, (byte) 0);
                }
            }
        }
        return msg;

    }

    private native byte[] encryptBackupKeyJni(byte[] backupKey,  byte[] key, byte[] info, byte[] ciphertext, byte[] mac);

    public String decryptBackupKey(OlmBackupKeyEncryption backup, String encryptionKey, String info) throws OlmException {

        String backupKey = "";
        String errorMessage = "";

        if ((null != backup) && (null != encryptionKey) && (null != info)) {
            byte[] ciphertextBuffer = null;
            byte[] macBuffer = null;
            byte[] encryptionKeyBuffer = null;
            byte [] infoBuffer = null;
            byte [] ivBuffer = null;
            try {
                infoBuffer = info.getBytes();
                encryptionKeyBuffer = hexStringToByteArray(encryptionKey);
                macBuffer = hexStringToByteArray(backup.keyMac);
                ciphertextBuffer = hexStringToByteArray(backup.keyCiphertext);
                ivBuffer = hexStringToByteArray(backup.keyIv);

                byte[] backupKeyBuffer = new byte[ciphertextBuffer.length];

                errorMessage = decryptBackupKeyJni(ciphertextBuffer, macBuffer, ivBuffer, infoBuffer, encryptionKeyBuffer, backupKeyBuffer);
                if (!TextUtils.isEmpty(errorMessage)) {
                    throw new OlmException(OlmException.EXCEPTION_CODE_UTILITY_BACKUP_DECRYPTION_ERROR, errorMessage);
                }

                backupKey = byteArraytoHexString(backupKeyBuffer);

            } catch (Exception e) {
                Log.e(LOG_TAG, "## encryptBackupKey(): failed " + e.getMessage());
                errorMessage = e.getMessage();
            } finally {
                if (null != encryptionKeyBuffer) {
                    Arrays.fill(encryptionKeyBuffer, (byte) 0);
                }
                if (null != ivBuffer) {
                    Arrays.fill(ivBuffer, (byte) 0);
                }
                if (null != macBuffer) {
                    Arrays.fill(macBuffer, (byte) 0);
                }
                if (null != ciphertextBuffer) {
                    Arrays.fill(ciphertextBuffer, (byte) 0);
                }
            }
        }

        if (!TextUtils.isEmpty(errorMessage)) {
            throw new OlmException(OlmException.EXCEPTION_CODE_UTILITY_BACKUP_DECRYPTION_ERROR, errorMessage);
        }

        return backupKey;

    }

    private native String decryptBackupKeyJni(byte[] ciphertext,  byte[] mac, byte[] iv, byte[] info, byte[] key, byte[] backupKey);

    public long getTimestamp(){
        return getTimestampJni();
    }

    private native long getTimestampJni();

    /**
     * Helper method to compute a string based on random integers.
     * @return bytes buffer containing randoms integer values
     */
    public static byte[] getRandomKey() {
        OlmPRGUtility prg = null;
        try {
            prg = OlmPRGUtility.getInstance();
            byte[] buffer = new byte[RANDOM_KEY_SIZE];
            prg.fillWithRandomBytes(buffer);

            // the key is saved as string
            // so avoid the UTF8 marker bytes
            for(int i = 0; i < RANDOM_KEY_SIZE; i++) {
                buffer[i] = (byte)(buffer[i] & 0x7F);
            }
            return buffer;


        } catch(Exception e) {

            SecureRandom secureRandom = new SecureRandom();
            byte[] buffer = new byte[RANDOM_KEY_SIZE];
            secureRandom.nextBytes(buffer);

            // the key is saved as string
            // so avoid the UTF8 marker bytes
            for(int i = 0; i < RANDOM_KEY_SIZE; i++) {
                buffer[i] = (byte)(buffer[i] & 0x7F);
            }
            return buffer;
        }
    }

    /**
     * Return true the object resources have been released.<br>
     * @return true the object resources have been released
     */
    public boolean isReleased() {
        return (0 == mNativeId);
    }

    /**
     * Build a string-string dictionary from a jsonObject.<br>
     * @param jsonObject the object to parse
     * @return the map
     */
    public static Map<String, String> toStringMap(JSONObject jsonObject) {
        if (null != jsonObject) {
            HashMap<String, String> map = new HashMap<>();
            Iterator<String> keysItr = jsonObject.keys();
            while(keysItr.hasNext()) {
                String key = keysItr.next();
                try {
                    Object value = jsonObject.get(key);

                    if (value instanceof String) {
                        map.put(key, (String) value);
                    } else {
                        Log.e(LOG_TAG, "## toStringMap(): unexpected type " + value.getClass());
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "## toStringMap(): failed " + e.getMessage());
                }
            }

            return map;
        }

        return null;
    }

    /**
     * Build a string-string dictionary of string dictionary from a jsonObject.<br>
     * @param jsonObject the object to parse
     * @return the map
     */
    public static Map<String, Map<String, String>> toStringMapMap(JSONObject jsonObject) {
        if (null != jsonObject) {
            HashMap<String, Map<String, String>> map = new HashMap<>();

            Iterator<String> keysItr = jsonObject.keys();
            while(keysItr.hasNext()) {
                String key = keysItr.next();
                try {
                    Object value = jsonObject.get(key);

                    if (value instanceof JSONObject) {
                        map.put(key, toStringMap((JSONObject) value));
                    } else {
                        Log.e(LOG_TAG, "## toStringMapMap(): unexpected type " + value.getClass());
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "## toStringMapMap(): failed " + e.getMessage());
                }
            }

            return map;
        }

        return null;
    }
}

