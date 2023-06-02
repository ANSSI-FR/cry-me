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

package com.yubico.yubikit.android.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.yubico.yubikit.android.R;
import com.yubico.yubikit.android.YubiKitManager;
import com.yubico.yubikit.android.transport.nfc.NfcConfiguration;
import com.yubico.yubikit.android.transport.nfc.NfcNotAvailable;
import com.yubico.yubikit.android.transport.nfc.NfcYubiKeyManager;
import com.yubico.yubikit.android.transport.usb.UsbConfiguration;
import com.yubico.yubikit.core.Logger;
import com.yubico.yubikit.core.YubiKeyDevice;
import com.yubico.yubikit.core.application.CommandState;

import javax.annotation.Nullable;

/**
 * A dialog for interacting with a YubiKey.
 * To use, start this activity with a subclass of {@link YubiKeyPromptAction} passed using the
 * ARG_ACTION_CLASS extra in the intent. This can be done by using the {@link #createIntent} method:
 * <pre>
 * {@code
 * Intent intent = YubiKeyPromptActivity.createIntent(context, MyConnectionAction.class);
 * startActivityForResult(intent, requestCode);
 * }
 * </pre>
 * <p>
 * The dialog can be customized by passing additional values in the intent.
 */
public class YubiKeyPromptActivity extends Activity {
    /**
     * Helper method to create an Intent to start the YubiKeyPromptActivity with a ConnectionAction.
     *
     * @param context  the Context to use for Intent creation
     * @param action   the ConnectionAction to use for handing YubiKey connections.
     * @param titleRes a string resource to use for the title of the dialog.
     * @return an Intent which can be passed to startActivity().
     */
    public static Intent createIntent(Context context, Class<? extends YubiKeyPromptAction> action, @StringRes int titleRes) {
        Intent intent = createIntent(context, action);
        intent.putExtra(ARG_TITLE_ID, titleRes);
        return intent;
    }

    /**
     * Helper method to create an Intent to start the YubiKeyPromptActivity with a ConnectionAction.
     *
     * @param context the Context to use for Intent creation
     * @param action  the ConnectionAction to use for handing YubiKey connections.
     * @return an Intent which can be passed to startActivity().
     */
    public static Intent createIntent(Context context, Class<? extends YubiKeyPromptAction> action) {
        Intent intent = new Intent(context, YubiKeyPromptActivity.class);
        intent.putExtra(ARG_ACTION_CLASS, action);
        return intent;
    }

    /**
     * The YubiKeyPromptAction subclass to use when a YubiKey is attached.
     */
    public static final String ARG_ACTION_CLASS = "ACTION_CLASS";

    /**
     * Whether or not to listen for YubiKeys over USB (default: true).
     */
    public static final String ARG_ALLOW_USB = "ALLOW_USB";

    /**
     * Whether or not to listen for YubiKeys over NFC (default: true).
     */
    public static final String ARG_ALLOW_NFC = "ALLOW_NFC";

    /**
     * A string resource to use as the title of the dialog.
     */
    public static final String ARG_TITLE_ID = "TITLE_ID";

    /**
     * A layout resource to use as the content of the dialog.
     */
    public static final String ARG_CONTENT_VIEW_ID = "CONTENT_VIEW_ID";

    /**
     * A view ID of a Button to use for cancelling the action.
     */
    public static final String ARG_CANCEL_BUTTON_ID = "CANCEL_BUTTON_ID";

    /**
     * A view ID of a Button to use to enable NFC, if NFC is disabled.
     */
    public static final String ARG_ENABLE_NFC_BUTTON_ID = "ENABLE_NFC_BUTTON_ID";

    /**
     * A view ID of a TextView where helpful information is displayed.
     */
    public static final String ARG_HELP_TEXT_VIEW_ID = "HELP_TEXT_VIEW_ID";

    private final MyCommandState commandState = new MyCommandState();

    private YubiKitManager yubiKit;
    private YubiKeyPromptAction action;

    private boolean hasNfc = true;
    private int usbSessionCounter = 0;
    private boolean isDone = false;
    protected Button cancelButton;
    protected Button enableNfcButton;
    protected TextView helpTextView;

    private boolean allowUsb;
    private boolean allowNfc;

    /**
     * Get the YubiKitManager used by this activity.
     *
     * @return a YubiKitManager
     */
    protected YubiKitManager getYubiKitManager() {
        return yubiKit;
    }

    /**
     * Get a CommandState for use with some blocking YubiKey actions.
     * The dialog will react to KEEPALIVE_UPNEEDED, and the state will be cancelled if the user presses the cancel button.
     *
     * @return a CommandState
     */
    protected CommandState getCommandState() {
        return commandState;
    }

    protected boolean isNfcEnabled() {
        return hasNfc;
    }

    /**
     * Called when a YubiKey is attached.
     * <p>
     * If {@link #provideResult(int, Intent)} has been called once this method returns, the Activity will finish.
     *
     * @param device a connected YubiKey
     */
    protected void onYubiKeyDevice(YubiKeyDevice device, Runnable onDone) {
        action.onYubiKey(device, getIntent().getExtras(), commandState, value -> {
            if (value.first == YubiKeyPromptAction.RESULT_CONTINUE) {
                // Keep processing additional YubiKeys
                if (commandState.awaitingTouch) {
                    // Reset the help text if touch was prompted for
                    runOnUiThread(() -> helpTextView.setText(hasNfc ? R.string.yubikit_prompt_plug_in_or_tap : R.string.yubikit_prompt_plug_in));
                    commandState.awaitingTouch = false;
                }
            } else {
                provideResult(value.first, value.second);
            }
            onDone.run();
        });
    }

    /**
     * Provides a result to return to the caller of the Activity.
     * Internally this calls {@link #setResult(int, Intent)} with the given arguments, as well as informing this
     * Activity that it should finish once it is done handling any connected YubiKey.
     *
     * @param resultCode The result code to propagate back to the originating
     *                   activity, often RESULT_CANCELED or RESULT_OK
     * @param data       The data to propagate back to the originating activity.
     */
    protected void provideResult(int resultCode, Intent data) {
        setResult(resultCode, data);
        isDone = true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Handle options
        Bundle args = getIntent().getExtras();

        allowUsb = args.getBoolean(ARG_ALLOW_USB, true);
        allowNfc = args.getBoolean(ARG_ALLOW_NFC, true);

        // Get the action to perform on YubiKey connected
        Class<?> actionType = (Class<?>) args.getSerializable(ARG_ACTION_CLASS);
        try {
            if (actionType != null && YubiKeyPromptAction.class.isAssignableFrom(actionType)) {
                action = (YubiKeyPromptAction) actionType.newInstance();
            } else {
                throw new IllegalStateException("Missing or invalid ConnectionAction class");
            }
        } catch (IllegalStateException | IllegalAccessException | InstantiationException e) {
            Logger.e("Unable to instantiate ConnectionAction", e);
            finish();
        }

        // Set up the view
        setContentView(args.getInt(ARG_CONTENT_VIEW_ID, R.layout.yubikit_yubikey_prompt_content));

        if (args.containsKey(ARG_TITLE_ID)) {
            setTitle(args.getInt(ARG_TITLE_ID));
        }

        // We draw our own title
        TextView titleText = findViewById(R.id.yubikit_prompt_title);
        if (titleText != null) {
            titleText.setText(getTitle());
        }

        helpTextView = findViewById(args.getInt(ARG_HELP_TEXT_VIEW_ID, R.id.yubikit_prompt_help_text_view));
        cancelButton = findViewById(args.getInt(ARG_CANCEL_BUTTON_ID, R.id.yubikit_prompt_cancel_btn));
        cancelButton.setFocusable(false);
        cancelButton.setOnClickListener(v -> {
            commandState.cancel();
            setResult(Activity.RESULT_CANCELED);
            finish();
        });

        yubiKit = new YubiKitManager(this);
        if (allowUsb) {
            yubiKit.startUsbDiscovery(new UsbConfiguration(), device -> {
                usbSessionCounter++;
                device.setOnClosed(() -> {
                    usbSessionCounter--;
                    if (usbSessionCounter == 0) {
                        runOnUiThread(() -> helpTextView.setText(hasNfc ? R.string.yubikit_prompt_plug_in_or_tap : R.string.yubikit_prompt_plug_in));
                    }
                });
                runOnUiThread(() -> helpTextView.setText(R.string.yubikit_prompt_wait));
                onYubiKeyDevice(device, YubiKeyPromptActivity.this::finishIfDone);
            });
        }

        if (allowNfc) {
            enableNfcButton = findViewById(args.getInt(ARG_ENABLE_NFC_BUTTON_ID, R.id.yubikit_prompt_enable_nfc_btn));
            enableNfcButton.setFocusable(false);
            enableNfcButton.setOnClickListener(v -> {
                startActivity(new Intent(NfcYubiKeyManager.NFC_SETTINGS_ACTION));
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (allowNfc) {
            enableNfcButton.setVisibility(View.GONE);
            try {
                yubiKit.startNfcDiscovery(new NfcConfiguration(), this, device -> {
                    onYubiKeyDevice(device, () -> {
                        runOnUiThread(() -> helpTextView.setText(R.string.yubikit_prompt_remove));
                        device.remove(this::finishIfDone);
                    });
                });
            } catch (NfcNotAvailable e) {
                hasNfc = false;
                helpTextView.setText(R.string.yubikit_prompt_plug_in);
                if (e.isDisabled()) {
                    enableNfcButton.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    protected void onPause() {
        if (allowNfc) {
            yubiKit.stopNfcDiscovery(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (allowUsb) {
            yubiKit.stopUsbDiscovery();
        }
        super.onDestroy();
    }


    private void finishIfDone() {
        if (isDone) {
            finish();
        }
    }

    private class MyCommandState extends CommandState {
        boolean awaitingTouch = false;

        @Override
        public void onKeepAliveStatus(byte status) {
            if (!awaitingTouch && status == CommandState.STATUS_UPNEEDED) {
                awaitingTouch = true;
                runOnUiThread(() -> helpTextView.setText(R.string.yubikit_prompt_uv));
            }
        }
    }
}
