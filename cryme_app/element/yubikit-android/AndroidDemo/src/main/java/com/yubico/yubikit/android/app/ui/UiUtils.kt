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

package com.yubico.yubikit.android.app.ui

import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputLayout
import com.yubico.yubikit.android.app.R
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@UiThread
suspend fun getSecret(context: Context, @StringRes title: Int, @StringRes hint: Int = R.string.pin) = suspendCoroutine<String?> { cont ->
    val view = LayoutInflater.from(context).inflate(R.layout.dialog_pin, null).apply {
        findViewById<TextInputLayout>(R.id.dialog_pin_textinputlayout).hint = context.getString(hint)
    }
    val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                cont.resume(view.findViewById<EditText>(R.id.dialog_pin_edittext).text.toString())
            }
            .setNeutralButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .setOnCancelListener {
                cont.resume(null)
            }
            .create()
    dialog.show()
}