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

package org.matrix.olm;

import android.content.Context;
import android.util.Log;

/**
 * Olm SDK entry point class.<br> An OlmManager instance must be created at first to enable native library load.
 * <br><br>Detailed implementation guide is available at <a href="http://matrix.org/docs/guides/e2e_implementation.html">Implementing End-to-End Encryption in Matrix clients</a>.
 */
public class OlmManager {
    private static final String LOG_TAG = "OlmManager";

    /**
     * Constructor.
     */
    public OlmManager() {
    }

    static {
        try {
            java.lang.System.loadLibrary("olm");
        } catch(UnsatisfiedLinkError e) {
            Log.e(LOG_TAG,"Exception loadLibrary() - Msg="+e.getMessage());
        }
    }

    /**
     * Provide the android library version
     * @return the library version
     */
    public String getVersion() {
        return BuildConfig.OLM_VERSION;
    }

    /**
     * Provide a detailed version.
     * It contains the android and the native libraries versions.
     * @param context the context
     * @return the detailed version
     */
    public String getDetailedVersion(Context context) {
        String gitVersion = context.getResources().getString(R.string.git_olm_revision);
        String date = context.getResources().getString(R.string.git_olm_revision_date);
        return getVersion() + " - olm version (" + getOlmLibVersion() + ") - " + gitVersion + "-" + date;
    }

    /**
     * Provide the native OLM lib version.
     * @return the lib version as a string
     */
    public String getOlmLibVersion(){
        return getOlmLibVersionJni();
    }
    public native String getOlmLibVersionJni();
}

