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
 * Copyright (C) 2020 Yubico.
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
package com.yubico.yubikit.core.application;

import com.yubico.yubikit.core.Logger;

/**
 * Provides control over an ongoing YubiKey operation.
 * <p>
 * Override onKeepAliveMessage to react to keepalive messages send periodically from the YubiKey.
 * Call {@link #cancel()} to cancel an ongoing operation.
 */
public class CommandState {
    public static final byte STATUS_PROCESSING = 1;
    public static final byte STATUS_UPNEEDED = 2;

    private boolean cancelled = false;

    /**
     * Override this method to handle keep-alive messages sent from the YubiKey.
     * The default implementation will log the event.
     *
     * @param status The keep alive status byte
     */
    public void onKeepAliveStatus(byte status) {
        Logger.d(String.format("received keepalive status: %x", status));
    }

    /**
     * Cancel an ongoing CTAP2 command, by sending a CTAP cancel command. This will cause the
     * YubiKey to return a CtapError with the error code 0x2d (ERR_KEEPALIVE_CANCEL).
     */
    public final synchronized void cancel() {
        cancelled = true;
    }

    /* Internal use only */
    public final synchronized boolean waitForCancel(long ms) {
        if (!cancelled && ms > 0) {
            try {
                wait(ms);
            } catch (InterruptedException e) {
                Logger.d("Thread interrupted, cancelling command");
                cancelled = true;
                Thread.currentThread().interrupt();
            }
        }
        return cancelled;
    }
}
