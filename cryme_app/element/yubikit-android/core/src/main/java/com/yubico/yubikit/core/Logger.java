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
 * Copyright (C) 2019 Yubico.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yubico.yubikit.core;


import javax.annotation.Nullable;

/**
 * Helper class allows to customize logs within the SDK
 * SDK has only 2 levels of logging: debug information and error
 * If a Logger implementation is not provided the SDK won't produce any logs
 */
public abstract class Logger {
    /**
     * Specifies how debug messages are logged.
     * <p>
     * If this method is not overridden, then debug messages will not be logged.
     *
     * @param message the message can to be logged
     */
    protected void logDebug(String message) {
    }

    /**
     * Specifies how error messages (with exceptions) are logged.
     * <p>
     * If this method is not overridden, then error messages will not be logged.
     *
     * @param message   the message can to be logged
     * @param throwable the exception that can to be logged or counted
     */
    protected void logError(String message, Throwable throwable) {
    }

    @Nullable
    private static Logger instance = null;

    /**
     * Set the Logger implementation to use. Override the logDebug and logError methods to produce
     * logs. Call with null to disable logging.
     *
     * @param logger the Logger implementation to use
     */
    public static void setLogger(@Nullable Logger logger) {
        instance = logger;
    }

    /**
     * Log a debug message.
     */
    public static void d(String message) {
        if (instance != null) {
            instance.logDebug(message);
        }
    }

    /**
     * Log an error message, together with an exception.
     */
    public static void e(String message, Throwable throwable) {
        if (instance != null) {
            instance.logError(message, throwable);
        }
    }
}
