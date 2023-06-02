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
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
package org.matrix.android.sdk.internal.legacy.riot;

import android.text.TextUtils;

import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

/*
 * IMPORTANT: This class is imported from Riot-Android to be able to perform a migration. Do not use it for any other purpose
 */

/**
 * The user's credentials.
 */
public class Credentials {
    public String userId;

    // This is the server name and not a URI, e.g. "matrix.org". Spec says it's now deprecated
    @Deprecated
    public String homeServer;

    public String accessToken;

    public String refreshToken;

    public String deviceId;

    // Optional data that may contain info to override homeserver and/or identity server
    public WellKnown wellKnown;

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("user_id", userId);
        json.put("home_server", homeServer);
        json.put("access_token", accessToken);
        json.put("refresh_token", TextUtils.isEmpty(refreshToken) ? JSONObject.NULL : refreshToken);
        json.put("device_id", deviceId);

        return json;
    }

    public static Credentials fromJson(JSONObject obj) throws JSONException {
        Credentials creds = new Credentials();
        creds.userId = obj.getString("user_id");
        creds.homeServer = obj.getString("home_server");
        creds.accessToken = obj.getString("access_token");

        if (obj.has("device_id")) {
            creds.deviceId = obj.getString("device_id");
        }

        // refresh_token is mandatory
        if (obj.has("refresh_token")) {
            try {
                creds.refreshToken = obj.getString("refresh_token");
            } catch (Exception e) {
                creds.refreshToken = null;
            }
        } else {
            throw new RuntimeException("refresh_token is required.");
        }

        return creds;
    }

    @Override
    public String toString() {
        return "Credentials{" +
                "userId='" + userId + '\'' +
                ", homeServer='" + homeServer + '\'' +
                ", refreshToken.length='" + (refreshToken != null ? refreshToken.length() : "null") + '\'' +
                ", accessToken.length='" + (accessToken != null ? accessToken.length() : "null") + '\'' +
                '}';
    }

    @Nullable
    public String getUserId() {
        return userId;
    }

    @Nullable
    public String getHomeServer() {
        return homeServer;
    }

    @Nullable
    public String getAccessToken() {
        return accessToken;
    }

    @Nullable
    public String getDeviceId() {
        return deviceId;
    }
}
