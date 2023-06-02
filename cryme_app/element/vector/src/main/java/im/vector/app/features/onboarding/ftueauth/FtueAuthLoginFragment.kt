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
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.onboarding.ftueauth

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.autofill.HintConstants
import androidx.core.text.isDigitsOnly
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.facebook.react.bridge.UiThreadUtil
import com.facebook.react.bridge.UiThreadUtil.runOnUiThread
import com.google.android.material.textfield.TextInputLayout
import com.yubico.yubikit.android.YubiKitManager
import com.yubico.yubikit.android.transport.nfc.NfcConfiguration
import com.yubico.yubikit.android.transport.nfc.NfcNotAvailable
import com.yubico.yubikit.android.transport.usb.UsbConfiguration
import com.yubico.yubikit.core.Logger
import com.yubico.yubikit.core.YubiKeyDevice
import com.yubico.yubikit.piv.*
import im.vector.app.R
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.hidePassword
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.databinding.FragmentLoginBinding
import im.vector.app.features.login.*
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.operator.ContentSigner
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.failure.isInvalidPassword
import reactivecircus.flowbinding.android.widget.textChanges
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.*
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * In this screen:
 * In signin mode:
 * - the user is asked for login (or email) and password to sign in to a homeserver.
 * - He also can reset his password
 * In signup mode:
 * - the user is asked for login and password
 */
class FtueAuthLoginFragment @Inject constructor() : AbstractSSOFtueAuthFragment<FragmentLoginBinding>() {

    private var isSignupMode = false

    // Temporary patch for https://github.com/vector-im/riotX-android/issues/1410,
    // waiting for https://github.com/matrix-org/synapse/issues/7576
    private var isNumericOnlyUserIdForbidden = false

    private lateinit var yubikit: YubiKitManager

    private lateinit var yubiKeyPrompt: android.app.AlertDialog
    private lateinit var yubiKeyPromptGen: android.app.AlertDialog
    private lateinit var keyGenWait: android.app.AlertDialog

    private var yubikeySlot = Slot.AUTHENTICATION

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginBinding {
        return FragmentLoginBinding.inflate(inflater, container, false)
    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    private fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }

        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSubmitButton()
        setupForgottenPasswordButton()

        yubikit = YubiKitManager(requireContext())

        yubiKeyPrompt = android.app.AlertDialog.Builder(context)
            .setTitle(R.string.insert_yubikey)
            .setMessage(R.string.hold_yubikey)
            .setOnCancelListener { viewModel.pendingAction.value = null }
            .create()

        yubiKeyPromptGen = android.app.AlertDialog.Builder(context)
            .setTitle(R.string.insert_yubikey_gen)
            .setMessage(R.string.hold_yubikey_gen)
            .setOnCancelListener { viewModel.pendingActionGen.value = null }
            .create()

        keyGenWait = android.app.AlertDialog.Builder(context)
            .setMessage(R.string.wait_key_gen_app)
            .setCancelable(false)
            .create()

        viewModel.yubiKey.observe(viewLifecycleOwner) { device ->
            if (device != null) {
                onYubiKey(device)
            }
        }

        viewModel.pendingAction.observe(viewLifecycleOwner) {
            if (it != null) {
                viewModel.yubiKey.value.let { device ->
                    if (device != null) {
                        onYubiKey(device)
                    } else {
                        yubiKeyPrompt.setMessage(resources.getString(R.string.hold_yubikey))
                        yubiKeyPrompt.show()
                    }
                }
            }
        }

        viewModel.pendingActionGenKeyYubikey.observe(viewLifecycleOwner) {
            if(it != null){
                if(it == "keyGenWait"){
                    if(yubiKeyPromptGen.isShowing){
                        yubiKeyPromptGen.dismiss()
                    }
                    keyGenWait.show()
                }
                else{
                    if(keyGenWait.isShowing){
                        keyGenWait.dismiss()
                    }
                    yubiKeyPromptGen.setMessage(resources.getString(R.string.hold_yubikey_gen))
                    yubiKeyPromptGen.show()
                }
            }
            else{
                if(yubiKeyPromptGen.isShowing){
                    yubiKeyPromptGen.dismiss()
                }
                if(keyGenWait.isShowing){
                    keyGenWait.dismiss()
                }
            }
        }

        viewModel.pendingActionGen.observe(viewLifecycleOwner) {
            if (it != null) {
                viewModel.yubiKey.value.let { device ->
                    if (device != null) {
                        onYubiKey(device)
                    }
                }
            }
        }

        viewModel.pendingActionGenKeyApp.observe(viewLifecycleOwner) {
            if (it != null) {
                lifecycleScope.launch(Dispatchers.Default) {
                    viewModel.handle(OnboardingAction.GenKeyRSA())
                }
            } else {
                if (keyGenWait.isShowing) {
                    keyGenWait.dismiss()
                }
            }
        }

        viewModel.result.observe(viewLifecycleOwner) { result ->
            if (yubiKeyPrompt.isShowing) {
                yubiKeyPrompt.dismiss()
            }
            result.onFailure {
                Logger.e("Error:", it)
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
            }
            viewModel.clearResult()
        }


        viewModel.rsaKeys.observe(viewLifecycleOwner) {
            if (it != null) {
                putKeyFromApp(it.pub, it.priv)
                viewModel.clearKeyRSA()
            }
        }

        views.passwordField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                presubmit()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    override fun onResume() {
        super.onResume()
        onStartUsb()
        try {
            yubikit.startNfcDiscovery(NfcConfiguration(), requireActivity()) { device ->
                viewModel.yubiKey.apply {
                    // Trigger new value, then removal
                    UiThreadUtil.runOnUiThread {
                        value = device
                        postValue(null)
                    }
                }
            }
        } catch (e: NfcNotAvailable) {
            if (e.isDisabled) {
                Timber.i("NFC disabled")
            } else {
                Timber.i("NFC error")
            }
        }
    }

    private fun onStartUsb() {
        yubikit.startUsbDiscovery(UsbConfiguration()) { device ->
            if (device.hasPermission()) {
                viewModel.yubiKey.postValue(device)
            }
            device.setOnClosed {
                viewModel.yubiKey.postValue(null)
            }
        }
    }

    private fun onEndUsb() {
        yubikit.stopUsbDiscovery()
    }

    override fun onPause() {
        yubikit.stopNfcDiscovery(requireActivity())
        onEndUsb()
        if (yubiKeyPrompt.isShowing) {
            yubiKeyPrompt.dismiss()
        }
        super.onPause()
    }


    private fun setupForgottenPasswordButton() {
        views.forgetPasswordButton.isVisible = false
    }

    private fun setupAutoFill(state: OnboardingViewState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (state.signMode) {
                SignMode.Unknown -> error("developer error")
                SignMode.SignUp -> {
                    views.loginTitle.text = getString(R.string.login_signup_to, state.homeServerUrlFromUser.toReducedUrl())
                    views.loginField.setAutofillHints(HintConstants.AUTOFILL_HINT_NEW_USERNAME)
                    views.passwordField.setAutofillHints(HintConstants.AUTOFILL_HINT_NEW_PASSWORD)
                }
                SignMode.SignIn,
                SignMode.SignInWithMatrixId -> {
                    views.loginTitle.text = getString(R.string.login_signin_to, state.homeServerUrlFromUser.toReducedUrl())
                    views.loginField.setAutofillHints(HintConstants.AUTOFILL_HINT_USERNAME)
                    views.passwordField.setAutofillHints(HintConstants.AUTOFILL_HINT_PASSWORD)
                }
            }.exhaustive
        }
    }

    private fun setupSocialLoginButtons(state: OnboardingViewState) {
        views.loginSocialLoginButtons.mode = when (state.signMode) {
            SignMode.Unknown -> error("developer error")
            SignMode.SignUp -> SocialLoginButtonsView.Mode.MODE_SIGN_UP
            SignMode.SignIn,
            SignMode.SignInWithMatrixId -> SocialLoginButtonsView.Mode.MODE_SIGN_IN
        }.exhaustive
    }

    private fun onYubiKey(device: YubiKeyDevice) {
        lifecycleScope.launch {
            withContext(viewModel.singleDispatcher) {
                viewModel.onYubiKeyDevice(device)
            }
        }
    }

    private fun presubmit() {
        if (!isSignupMode) {
            viewModel.handle(OnboardingAction.OnGetChallenge(views.loginField.text.toString()))
            lifecycleScope.launch(Dispatchers.Main) {
                getSecret(requireContext(), R.string.auth_pin_yubikey)?.let { pin ->
                    viewModel.pendingAction.value = {
                        verifyPin(pin.toCharArray())
                        submit(this, pin)
                        ""
                    }
                }
            }
        } else {
            chooseKeyGeneration()
        }
    }

    private fun chooseKeyGeneration() {
        val dialog = AlertDialog.Builder(context!!)
            .setTitle(R.string.key_generation_choice)
            .setMessage(R.string.key_generation_choice_text)
            .setPositiveButton(R.string.key_generation_choice_app) { dialog, _ ->
                dialog.cancel()
                generateKeyUsingApp()
            }
            .setNeutralButton(R.string.key_generation_choice_yubikey) { dialog, _ ->
                dialog.cancel()
                generateKeyUsingYubiKey()
            }
            .create()

        dialog.show()
    }

    private fun generateCertificate(piv: PivSession, publicKey: PublicKey): X509Certificate {
        // Generate a certificate
        val name = X500Name("CN=Generated Matrix Element Key")
        val serverCertGen = X509v3CertificateBuilder(
            name,
            BigInteger("123456789"),
            Date(),
            Date(),
            name,
            SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(publicKey.encoded))
        )
        val certBytes = serverCertGen.build(object : ContentSigner {
            val messageBuffer = ByteArrayOutputStream()
            override fun getAlgorithmIdentifier(): AlgorithmIdentifier = AlgorithmIdentifier(PKCSObjectIdentifiers.sha256WithRSAEncryption)
            override fun getOutputStream(): OutputStream = messageBuffer
            override fun getSignature(): ByteArray {
                return piv.sign(yubikeySlot, KeyType.RSA2048, messageBuffer.toByteArray(), Signature.getInstance("SHA256withRSA"))
            }
        }).encoded

        return CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(certBytes)) as X509Certificate
    }

    private fun generateKeyUsingApp() {
        keyGenWait.show()
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.pendingActionGenKeyApp.value = ""
        }
    }


    private fun generateKeyUsingYubiKey() {
        lifecycleScope.launch(Dispatchers.Main) {
            getSecret(requireContext(), R.string.auth_pin_yubikey)?.let { pin ->
                viewModel.pendingActionGenKeyYubikey.postValue("")
                viewModel.pendingActionGen.value = {
                    try{
                        authenticate(viewModel.mgmtKeyType, viewModel.mgmtKey)
                        viewModel.pendingActionGenKeyYubikey.postValue("keyGenWait")
                        // Generate a key
                        val publicKey = generateKey(yubikeySlot, KeyType.RSA2048, PinPolicy.DEFAULT, TouchPolicy.DEFAULT)
                        verifyPin(pin.toCharArray())
                        putCertificate(yubikeySlot, generateCertificate(this, publicKey))
                        submit(this, pin)
                        ""
                    } catch(e: Exception){
                        if (keyGenWait.isShowing) {
                            keyGenWait.dismiss()
                        }
                        throw e
                    }
                }
            }
        }
    }


    private fun putKeyFromApp(pub: RSAPublicKeySpec, priv: RSAPrivateCrtKeySpec) {
        if (keyGenWait.isShowing) {
            keyGenWait.dismiss()
        }
        val privateKey = KeyFactory.getInstance("RSA").generatePrivate(priv)
        val publicKey: RSAPublicKey = KeyFactory.getInstance("RSA").generatePublic(pub) as RSAPublicKey

        lifecycleScope.launch(Dispatchers.Main) {
            getSecret(requireContext(), R.string.auth_pin_yubikey)?.let { pin ->
                viewModel.pendingAction.value = {
                    authenticate(viewModel.mgmtKeyType, viewModel.mgmtKey)
                    verifyPin(pin.toCharArray())
                    putKey(yubikeySlot, privateKey, PinPolicy.DEFAULT, TouchPolicy.DEFAULT)
                    putCertificate(yubikeySlot, generateCertificate(this, publicKey))
                    submit(this, pin)
                    ""
                }
            }
        }
    }

    private fun uintToByteArray(data: UInt) : ByteArray {
        val buffer = ByteArray(4)
        buffer[3] = (data shr 0).toByte()
        buffer[2] = (data shr 8).toByte()
        buffer[1] = (data shr 16).toByte()
        buffer[0] = (data shr 24).toByte()

        return buffer
    }

    private fun byteArrayToUint(buffer: ByteArray): UInt {
        return (buffer[0].toUInt() shl 24) or
                (buffer[1].toUInt() and 255u shl 16) or
                (buffer[2].toUInt() and 255u shl 8) or
                (buffer[3].toUInt() and 255u)
    }

    private fun submit(piv: PivSession, pin: String) {
        viewModel.pendingActionGenKeyYubikey.postValue(null)
        var x509cert: String
        var signature_final: String
        x509cert = ""
        signature_final = ""

        if (isSignupMode) {
            x509cert = piv.getCertificate(yubikeySlot).encoded.toHexString()
        } else {
            val challengeBytes = ByteBuffer.allocate(8)
            try {
                val inte = viewModel.challengeSigServer!!.toLong(16)
                challengeBytes.putLong(inte)
            } catch (fail: NumberFormatException) {
                Toast.makeText(context, "${R.string.unknown_error}: Unable to parse Challenge. This should not have happened, please report error !", Toast.LENGTH_LONG)
            }
            
            val timstp = viewModel.handleGetTimestamp()
            val message = uintToByteArray(timstp.toUInt())

            if(byteArrayToUint(message) != timstp.toUInt()){
                Timber.i("SOMETHING IS WRONG ${byteArrayToUint(message)} $timstp")
            }

            val publicKey = piv.getCertificate(yubikeySlot).publicKey

            piv.verifyPin(pin.toCharArray())

            val signature = piv.signChallenge(
                yubikeySlot,
                KeyType.fromKey(publicKey),
                challengeBytes.array() + message,
                Signature.getInstance("SHA256withRSA")
            )

            signature_final = signature.toHexString()
        }

        if (yubiKeyPromptGen.isShowing) {
            yubiKeyPromptGen.dismiss()
        }

        runOnUiThread {
            cleanupUi()
        }

        val login = views.loginField.text.toString()
        val password = views.passwordField.text.toString()

        // This can be called by the IME action, so deal with empty cases
        var error = 0
        //error++
        if (login.isEmpty()) {
            views.loginFieldTil.error = getString(
                if (isSignupMode) {
                    R.string.error_empty_field_choose_user_name
                } else {
                    R.string.error_empty_field_enter_user_name
                }
            )
            error++
        }
        if (isSignupMode && isNumericOnlyUserIdForbidden && login.isDigitsOnly()) {
            views.loginFieldTil.error = "The homeserver does not accept username with only digits."
            error++
        }
        if (password.isEmpty()) {
            views.passwordFieldTil.error = getString(
                if (isSignupMode) {
                    R.string.error_empty_field_choose_password
                } else {
                    R.string.error_empty_field_your_password
                }
            )
            error++
        }

        if (error == 0) {
            viewModel.handle(
                OnboardingAction.LoginOrRegister(
                    login,
                    password,
                    x509cert,
                    signature_final,
                    getString(R.string.login_default_session_public_name)
                )
            )
        }
    }

    private fun cleanupUi() {
        views.loginSubmit.hideKeyboard()
        views.loginFieldTil.error = null
        views.passwordFieldTil.error = null
    }

    private fun setupUi(state: OnboardingViewState) {
        views.loginFieldTil.hint = getString(
            when (state.signMode) {
                SignMode.Unknown -> error("developer error")
                SignMode.SignUp -> R.string.login_signup_username_hint
                SignMode.SignIn -> R.string.login_signin_username_hint
                SignMode.SignInWithMatrixId -> R.string.login_signin_matrix_id_hint
            }
        )

        // Handle direct signin first
        if (state.signMode == SignMode.SignInWithMatrixId) {
            views.loginServerIcon.isVisible = false
            views.loginTitle.text = getString(R.string.login_signin_matrix_id_title)
            views.loginNotice.text = getString(R.string.login_signin_matrix_id_notice)
            views.loginPasswordNotice.isVisible = true
        } else {
            val resId = when (state.signMode) {
                SignMode.Unknown -> error("developer error")
                SignMode.SignUp -> R.string.login_signup_to
                SignMode.SignIn -> R.string.login_connect_to
                SignMode.SignInWithMatrixId -> R.string.login_connect_to
            }

            when (state.serverType) {
                ServerType.MatrixOrg -> {
                    views.loginServerIcon.isVisible = true
                    views.loginServerIcon.setImageResource(R.drawable.ic_logo_matrix_org)
                    views.loginTitle.text = getString(resId, state.homeServerUrlFromUser.toReducedUrl())
                    views.loginNotice.text = getString(R.string.login_server_matrix_org_text)
                }
                ServerType.EMS -> {
                    views.loginServerIcon.isVisible = true
                    views.loginServerIcon.setImageResource(R.drawable.ic_logo_element_matrix_services)
                    views.loginTitle.text = getString(resId, "Element Matrix Services")
                    views.loginNotice.text = getString(R.string.login_server_modular_text)
                }
                ServerType.Other -> {
                    views.loginServerIcon.isVisible = false
                    views.loginTitle.text = getString(resId, state.homeServerUrlFromUser.toReducedUrl())
                    views.loginNotice.text = getString(R.string.login_server_other_text)
                }
                ServerType.Unknown -> Unit /* Should not happen */
            }
            views.loginPasswordNotice.isVisible = false

            if (state.loginMode is LoginMode.SsoAndPassword) {
                views.loginSocialLoginContainer.isVisible = true
                views.loginSocialLoginButtons.ssoIdentityProviders = state.loginMode.ssoIdentityProviders?.sorted()
                views.loginSocialLoginButtons.listener = object : SocialLoginButtonsView.InteractionListener {
                    override fun onProviderSelected(id: String?) {
                        viewModel.getSsoUrl(
                            redirectUrl = SSORedirectRouterActivity.VECTOR_REDIRECT_URL,
                            deviceId = state.deviceId,
                            providerId = id
                        )
                            ?.let { openInCustomTab(it) }
                    }
                }
            } else {
                views.loginSocialLoginContainer.isVisible = false
                views.loginSocialLoginButtons.ssoIdentityProviders = null
            }
        }
    }

    private fun setupButtons(state: OnboardingViewState) {
        views.forgetPasswordButton.isVisible = false

        views.loginSubmit.text = getString(
            when (state.signMode) {
                SignMode.Unknown -> error("developer error")
                SignMode.SignUp -> R.string.login_signup_submit
                SignMode.SignIn,
                SignMode.SignInWithMatrixId -> R.string.login_signin
            }
        )
    }

    private fun setupSubmitButton() {
        views.loginSubmit.setOnClickListener {
            presubmit()
        }
        combine(
            views.loginField.textChanges().map { it.trim().isNotEmpty() },
            views.passwordField.textChanges().map { it.isNotEmpty() }
        ) { isLoginNotEmpty, isPasswordNotEmpty ->
            isLoginNotEmpty && isPasswordNotEmpty
        }
            .onEach {
                views.loginFieldTil.error = null
                views.passwordFieldTil.error = null
                views.loginSubmit.isEnabled = it
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetLogin)
    }

    override fun onError(throwable: Throwable) {
        // Show M_WEAK_PASSWORD error in the password field
        if (throwable is Failure.ServerError &&
            throwable.error.code == MatrixError.M_WEAK_PASSWORD
        ) {
            views.passwordFieldTil.error = errorFormatter.toHumanReadable(throwable)
        } else {
            views.loginFieldTil.error = errorFormatter.toHumanReadable(throwable)
        }
    }

    override fun updateWithState(state: OnboardingViewState) {
        isSignupMode = state.signMode == SignMode.SignUp
        isNumericOnlyUserIdForbidden = state.serverType == ServerType.MatrixOrg

        setupUi(state)
        setupAutoFill(state)
        setupSocialLoginButtons(state)
        setupButtons(state)

        when (state.asyncLoginAction) {
            is Loading -> {
                // Ensure password is hidden
                views.passwordField.hidePassword()
            }
            is Fail -> {
                val error = state.asyncLoginAction.error
                if (error is Failure.ServerError &&
                    error.error.code == MatrixError.M_FORBIDDEN &&
                    error.error.message.isEmpty()
                ) {
                    // Login with email, but email unknown
                    views.loginFieldTil.error = getString(R.string.login_login_with_email_error)
                } else {
                    // Trick to display the error without text.
                    views.loginFieldTil.error = " "
                    if (error.isInvalidPassword() && spaceInPassword()) {

                        views.passwordFieldTil.error = getString(R.string.auth_invalid_login_param_space_in_password)
                    } else {
                        views.passwordFieldTil.error = errorFormatter.toHumanReadable(error)
                    }
                }
            }
            // Success is handled by the LoginActivity
            is Success -> Unit
        }

        when (state.asyncRegistration) {
            is Loading -> {
                // Ensure password is hidden
                views.passwordField.hidePassword()
            }
            // Success is handled by the LoginActivity
            is Success -> Unit
        }
    }

    /**
     * Detect if password ends or starts with spaces
     */
    private fun spaceInPassword() = views.passwordField.text.toString().let { it.trim() != it }


    @UiThread
    suspend fun getSecret(context: Context, @StringRes title: Int, @StringRes hint: Int = R.string.auth_pin_yubikey) = suspendCoroutine<String?> { cont ->
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_pin_yubikey_login, null).apply {
            findViewById<TextInputLayout>(R.id.dialog_pin_textinputlayout).hint = context.getString(hint)
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                view.hideKeyboard()
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
}
