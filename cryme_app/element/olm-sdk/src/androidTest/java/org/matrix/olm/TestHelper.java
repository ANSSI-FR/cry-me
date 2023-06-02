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

import java.util.ArrayList;
import java.util.Map;


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Helper class providing helper methods used in the Olm Android SDK unit tests.
 */
public class TestHelper {

    /**
     * Return the identity key {@link OlmAccount#JSON_KEY_IDENTITY_KEY} from the JSON object.
     * @param aIdentityKeysMap result of {@link OlmAccount#identityKeys()}
     * @return identity key string if operation succeed, null otherwise
     */
    static public String getIdentityKey(Map<String, String> aIdentityKeysMap){
        String idKey = null;

        try {
            idKey = aIdentityKeysMap.get(OlmAccount.JSON_KEY_IDENTITY_KEY);
        } catch (Exception e) {
            fail("Exception MSg=" + e.getMessage());
        }
        return idKey;
    }

    /**
     * Return the fingerprint key {@link OlmAccount#JSON_KEY_FINGER_PRINT_KEY} from the JSON object.
     * @param aIdentityKeysMap result of {@link OlmAccount#identityKeys()}
     * @return fingerprint key string if operation succeed, null otherwise
     */
    static public String getFingerprintKey(Map<String, String> aIdentityKeysMap) {
        String fingerprintKey = null;

        try {
            fingerprintKey = aIdentityKeysMap.get(OlmAccount.JSON_KEY_FINGER_PRINT_KEY);
        } catch (Exception e) {
            fail("Exception MSg=" + e.getMessage());
        }
        return fingerprintKey;
    }

    /**
     * Return the first one time key from the JSON object.
     * @param aIdentityKeysMap result of {@link OlmAccount#oneTimeKeys()}
     * @param aKeyPosition the position of the key to be retrieved
     * @return one time key string if operation succeed, null otherwise
     */
    static public String getOneTimeKey(Map<String, Map<String, String>> aIdentityKeysMap, int aKeyPosition) {
        String firstOneTimeKey = null;

        try {
            Map<String, String> generatedKeys = aIdentityKeysMap.get(OlmAccount.JSON_KEY_ONE_TIME_KEY);
            assertNotNull(OlmAccount.JSON_KEY_ONE_TIME_KEY + " object is missing", generatedKeys);

            firstOneTimeKey = (new ArrayList<>(generatedKeys.values())).get(aKeyPosition - 1);
        } catch (Exception e) {
            fail("Exception Msg=" + e.getMessage());
        }
        return firstOneTimeKey;
    }
}
