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

package com.yubico.yubikit.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.yubico.yubikit.core.YubiKeyDevice;
import com.yubico.yubikit.core.application.CommandState;
import com.yubico.yubikit.core.util.Callback;
import com.yubico.yubikit.core.util.Pair;

import javax.annotation.Nullable;

/**
 * Action to be performed by a {@link YubiKeyPromptActivity} when a YubiKey is attached.
 * Extend this class to handle an attached YubiKey from a YubiKeyPromptActivity.
 * <p>
 * See also {@link YubiKeyPromptConnectionAction} for an alternative which handles YubiKeys for a
 * specific connection type.
 */
public abstract class YubiKeyPromptAction {
    /**
     * A special result code which will reset the dialog state to continue processing additional YubiKeys.
     */
    public static final int RESULT_CONTINUE = Activity.RESULT_FIRST_USER + 100;

    /**
     * A result Pair used to keep the dialog open to continue processing YubiKeys.
     */
    public static final Pair<Integer, Intent> CONTINUE = new Pair<>(RESULT_CONTINUE, new Intent());
    /**
     * Called when a YubiKey is connected.
     * <p>
     * Subclasses should override this method to react to a connected YubiKey.
     * Use the callback to signal when the method is done handling the YubiKey, with a result
     * (a pair of resultCode, Intent) to return to the caller, closing the dialog.
     * Use the special {@link #CONTINUE} result to leave the dialog open, without returning to the
     * caller, and continue to process additional YubiKeys.
     * The CommandState can be used to update the dialog UI based on status of the
     * operation, and is cancelled if the user presses the cancel button.
     *
     * @param device       A YubiKeyDevice
     * @param extras       the extras the Activity was called with
     * @param commandState a CommandState that is hooked up to the activity.
     * @param callback     a callback to invoke to provide the result of the operation, as a Pair of result code and Intent with extras
     */
    abstract void onYubiKey(YubiKeyDevice device, Bundle extras, CommandState commandState, Callback<Pair<Integer, Intent>> callback);
}
