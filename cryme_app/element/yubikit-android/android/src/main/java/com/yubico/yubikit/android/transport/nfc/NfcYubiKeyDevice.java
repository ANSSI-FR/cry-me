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

package com.yubico.yubikit.android.transport.nfc;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.Ndef;

import com.yubico.yubikit.core.Transport;
import com.yubico.yubikit.core.YubiKeyConnection;
import com.yubico.yubikit.core.YubiKeyDevice;
import com.yubico.yubikit.core.util.Callback;
import com.yubico.yubikit.core.util.Result;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class NfcYubiKeyDevice implements YubiKeyDevice {
    private final AtomicBoolean removed = new AtomicBoolean();
    private final ExecutorService executorService;
    private final Tag tag;
    private final int timeout;

    /**
     * Instantiates session for nfc tag interaction
     *
     * @param tag     the tag that has been discovered
     * @param timeout timeout, in milliseconds, to use for NFC communication
     */
    public NfcYubiKeyDevice(Tag tag, int timeout, ExecutorService executorService) {
        this.executorService = executorService;
        this.tag = tag;
        this.timeout = timeout;
    }

    /**
     * @return NFC tag that has been discovered
     */
    public Tag getTag() {
        return tag;
    }

    private NfcSmartCardConnection openIso7816Connection() throws IOException {
        IsoDep card = IsoDep.get(tag);
        if (card == null) {
            throw new IOException("the tag does not support ISO-DEP");
        }
        card.setTimeout(timeout);
        card.connect();
        return new NfcSmartCardConnection(card);
    }

    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
    public byte[] readNdef() throws IOException {
        try (Ndef ndef = Ndef.get(tag)) {
            if (ndef != null) {
                ndef.connect();
                NdefMessage message = ndef.getNdefMessage();
                if (message != null) {
                    return message.toByteArray();
                }
            }
        } catch (FormatException e) {
            throw new IOException(e);
        }
        throw new IOException("NDEF data missing or invalid");
    }

    /**
     * Closes the device and waits for physical removal.
     * This method signals that we are done with the device and can be used to wait for the user to
     * physically remove the YubiKey from NFC scan range, to avoid triggering NFC YubiKey detection
     * multiple times in quick succession.
     */
    public void remove(Runnable onRemoved) {
        removed.set(true);
        executorService.submit(() -> {
            try {
                IsoDep isoDep = IsoDep.get(tag);
                isoDep.connect();
                while (isoDep.isConnected()) {
                    //noinspection BusyWait
                    Thread.sleep(250);
                }
            } catch (InterruptedException | IOException e) {
                // Ignore
            }
            onRemoved.run();
        });
    }

    @Override
    public Transport getTransport() {
        return Transport.NFC;
    }

    @Override
    public boolean supportsConnection(Class<? extends YubiKeyConnection> connectionType) {
        return connectionType.isAssignableFrom(NfcSmartCardConnection.class);
    }

    private <T extends YubiKeyConnection> T openConnection(Class<T> connectionType) throws IOException {
        if (connectionType.isAssignableFrom(NfcSmartCardConnection.class)) {
            return connectionType.cast(openIso7816Connection());
        }
        throw new IllegalStateException("The connection type is not supported by this session");
    }

    @Override
    public <T extends YubiKeyConnection> void requestConnection(Class<T> connectionType, Callback<Result<T, IOException>> callback) {
        if (removed.get()) {
            callback.invoke(Result.failure(new IOException("Can't requestConnection after calling remove()")));
        } else
            executorService.submit(() -> {
                try (T connection = openConnection(connectionType)) {
                    callback.invoke(Result.success(connection));
                } catch (IOException e) {
                    callback.invoke(Result.failure(e));
                }
            });
    }
}
