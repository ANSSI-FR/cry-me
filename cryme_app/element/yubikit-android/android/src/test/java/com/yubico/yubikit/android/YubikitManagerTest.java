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
package com.yubico.yubikit.android;

import android.app.Activity;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.yubico.yubikit.android.transport.nfc.NfcConfiguration;
import com.yubico.yubikit.android.transport.nfc.NfcNotAvailable;
import com.yubico.yubikit.android.transport.nfc.NfcYubiKeyDevice;
import com.yubico.yubikit.android.transport.nfc.NfcYubiKeyManager;
import com.yubico.yubikit.android.transport.usb.UsbConfiguration;
import com.yubico.yubikit.android.transport.usb.UsbYubiKeyDevice;
import com.yubico.yubikit.android.transport.usb.UsbYubiKeyManager;
import com.yubico.yubikit.core.util.Callback;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.annotation.Config;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@Config(manifest = Config.NONE)
public class YubikitManagerTest {
    private UsbYubiKeyManager mockUsb = Mockito.mock(UsbYubiKeyManager.class);
    private NfcYubiKeyManager mockNfc = Mockito.mock(NfcYubiKeyManager.class);
    private Activity mockActivity = Mockito.mock(Activity.class);

    private UsbYubiKeyDevice usbSession = Mockito.mock(UsbYubiKeyDevice.class);
    private NfcYubiKeyDevice nfcSession = Mockito.mock(NfcYubiKeyDevice.class);

    private final CountDownLatch signal = new CountDownLatch(2);
    private YubiKitManager yubiKitManager = new YubiKitManager(mockUsb, mockNfc);

    @Before
    public void setUp() throws NfcNotAvailable {
        Mockito.doAnswer(new UsbListenerInvocation(usbSession)).when(mockUsb).enable(Mockito.any(), Mockito.any(Callback.class));
        Mockito.doAnswer(new NfcListenerInvocation(nfcSession)).when(mockNfc).enable(Mockito.any(), Mockito.any(), Mockito.any(Callback.class));
    }

    @Test
    public void discoverSession() throws NfcNotAvailable {
        yubiKitManager.startNfcDiscovery(new NfcConfiguration(), mockActivity, new NfcListener());
        yubiKitManager.startUsbDiscovery(new UsbConfiguration(), new UsbListener());

        // wait until listener will be invoked
        try {
            signal.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail();
        }

        yubiKitManager.stopUsbDiscovery();
        yubiKitManager.stopNfcDiscovery(mockActivity);
        Mockito.verify(mockUsb).disable();
        Mockito.verify(mockNfc).disable(mockActivity);

        // expected to discover 2 sessions
        Assert.assertEquals(0, signal.getCount());
    }

    @Test
    public void discoverUsbSession() throws NfcNotAvailable {
        UsbConfiguration configuration = new UsbConfiguration();
        yubiKitManager.startUsbDiscovery(configuration, new UsbListener());

        Mockito.verify(mockUsb).enable(Mockito.eq(configuration), Mockito.any(Callback.class));
        Mockito.verify(mockNfc, Mockito.never()).enable(Mockito.any(), Mockito.any(), Mockito.any());

        // wait until listener will be invoked
        try {
            signal.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail();
        }

        yubiKitManager.stopUsbDiscovery();
        Mockito.verify(mockUsb).disable();
        Mockito.verify(mockNfc, Mockito.never()).disable(mockActivity);

        // expected to discover 1 session
        Assert.assertEquals(1, signal.getCount());
    }

    @Test
    public void discoverNfcSession() throws NfcNotAvailable {
        NfcConfiguration configuration = new NfcConfiguration();
        yubiKitManager.startNfcDiscovery(configuration, mockActivity, new NfcListener());

        Mockito.verify(mockUsb, Mockito.never()).enable(Mockito.any(), Mockito.any(Callback.class));
        Mockito.verify(mockNfc).enable(Mockito.eq(mockActivity), Mockito.eq(configuration), Mockito.any());

        // wait until listener will be invoked
        try {
            signal.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail();
        }

        yubiKitManager.stopNfcDiscovery(mockActivity);
        Mockito.verify(mockUsb, Mockito.never()).disable();
        Mockito.verify(mockNfc).disable(mockActivity);

        // expected to discover 1 session
        Assert.assertEquals(1, signal.getCount());
    }

    private class UsbListener implements Callback<UsbYubiKeyDevice> {
        @Override
        public void invoke(UsbYubiKeyDevice value) {
            signal.countDown();
        }
    }

    private class NfcListener implements Callback<NfcYubiKeyDevice> {
        @Override
        public void invoke(NfcYubiKeyDevice value) {
            signal.countDown();
        }
    }

    private class UsbListenerInvocation implements Answer {
        private UsbYubiKeyDevice session;

        private UsbListenerInvocation(UsbYubiKeyDevice session) {
            this.session = session;
        }

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            Callback<? super UsbYubiKeyDevice> internalListener = invocation.getArgument(1);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    internalListener.invoke(session);
                }
            }, 100); // emulating that discovery of session took some time
            return null;
        }
    }

    private class NfcListenerInvocation implements Answer {
        private NfcYubiKeyDevice session;

        private NfcListenerInvocation(NfcYubiKeyDevice session) {
            this.session = session;
        }

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            Callback<? super NfcYubiKeyDevice> internalListener = invocation.getArgument(2);
            internalListener.invoke(session);
            return null;
        }
    }
}
