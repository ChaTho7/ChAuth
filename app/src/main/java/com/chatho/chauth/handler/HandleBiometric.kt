/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.chatho.chauth.handler

import android.app.Activity.RESULT_OK
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.chatho.chauth.R
import com.chatho.chauth.api.HandleAPI
import com.chatho.chauth.holder.OneSignalHolder
import com.chatho.chauth.util.findConstantFieldName
import java.io.IOException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey

class HandleBiometric(private val activity: AppCompatActivity, val handleAPI: HandleAPI) :
    IHandleBiometric {
    private lateinit var keyStore: KeyStore
    private lateinit var keyGenerator: KeyGenerator
    private lateinit var biometricPrompt: BiometricPrompt

    private val keyGuard = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    private var triesForSecuritySetup = 0
    private val securtiySetupActivityResultLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        triesForSecuritySetup += 1
        if (result.resultCode == RESULT_OK && triesForSecuritySetup <= MAX_TRIES_FOR_SECURITY_SETUP) {
            biometricSetup()
        } else {
            Toast.makeText(
                activity, "You have to setup a security method in your device.", Toast.LENGTH_LONG
            ).show()
        }
    }
    private val deviceCredentialActivityResultLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            deviceCredentialOnSucceededCallback(handleAPI)
        } else {
            deviceCredentialOnFailedCallback(handleAPI)
        }
    }

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val DEFAULT_KEY_NAME = "chauth_key"
        private const val MAX_TRIES_FOR_SECURITY_SETUP = 2
    }

    init {
        setupKeyStoreAndKeyGenerator()
        createBiometricPrompt()
    }

    private fun authenticate(cipher: Cipher) {
        val promptInfo = createPromptInfo()

        if (initCipher(cipher)) {
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        } else {
            println("loginWithPassword")
        }
    }

    private fun setupCiphers(): Cipher {
        val defaultCipher: Cipher
        try {
            val cipherString =
                "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"
            defaultCipher = Cipher.getInstance(cipherString)
        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException, is NoSuchPaddingException -> throw RuntimeException(
                    "Failed to get an instance of Cipher", e
                )

                else -> throw e
            }
        }
        return defaultCipher
    }

    private fun createKey() {
        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of enrolled
        // fingerprints has changed.
        try {
            keyStore.load(null)

            val keyProperties = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            val builder = KeyGenParameterSpec.Builder(DEFAULT_KEY_NAME, keyProperties)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC).setUserAuthenticationRequired(true)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setInvalidatedByBiometricEnrollment(true)

            keyGenerator.run {
                init(builder.build())
                generateKey()
            }
        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException, is InvalidAlgorithmParameterException, is CertificateException, is IOException -> throw RuntimeException(
                    e
                )

                else -> throw e
            }
        }
    }

    private fun createBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(activity.applicationContext)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    Toast.makeText(activity, "Use pattern to authenticate...", Toast.LENGTH_SHORT)
                        .show()
                }
                biometricOnErrorCallback(errorCode, errString, handleAPI)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                biometricOnFailedCallback(handleAPI)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                biometricOnSucceededCallback(result, handleAPI)
            }
        }

        biometricPrompt = BiometricPrompt(activity, executor, callback)
    }

    private fun createPromptInfo(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.resources.getString(R.string.biometric_title))
            .setSubtitle(activity.resources.getString(R.string.biometric_subtitle))
            .setConfirmationRequired(false)
            .setNegativeButtonText(activity.resources.getString(R.string.biometric_negative_button_text))
            .setAllowedAuthenticators(BIOMETRIC_STRONG) // Allow PIN/pattern/password authentication.
            .build()
    }

    private fun initCipher(cipher: Cipher): Boolean {
        try {
            keyStore.load(null)
            cipher.init(Cipher.ENCRYPT_MODE, keyStore.getKey(DEFAULT_KEY_NAME, null) as SecretKey)
            return true
        } catch (e: Exception) {
            when (e) {
                is KeyPermanentlyInvalidatedException -> return false
                is KeyStoreException, is CertificateException, is UnrecoverableKeyException, is IOException, is NoSuchAlgorithmException, is InvalidKeyException -> throw RuntimeException(
                    "Failed to init Cipher", e
                )

                else -> throw e
            }
        }
    }

    private fun setupKeyStoreAndKeyGenerator() {
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        } catch (e: KeyStoreException) {
            throw RuntimeException("Failed to get an instance of KeyStore", e)
        }

        try {
            keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE
            )
        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException, is NoSuchProviderException -> throw RuntimeException(
                    "Failed to get an instance of KeyGenerator", e
                )

                else -> throw e
            }
        }
    }

    override fun biometricSetup() {
        val biometricManager = BiometricManager.from(activity)
        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val defaultCipher: Cipher = setupCiphers()

                createKey()
                authenticate(defaultCipher)
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(
                    activity,
                    "You have to setup a biometric method in your device.",
                    Toast.LENGTH_LONG
                ).show()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(
                            Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG
                        )
                    }
                    securtiySetupActivityResultLauncher.launch(intent)
                } else {
                    Toast.makeText(
                        activity,
                        "Authentication not supported on APIs lower that 26..",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            else -> {
                if (keyGuard.isDeviceSecure) {
                    val intent = keyGuard.createConfirmDeviceCredentialIntent(
                        activity.resources.getString(R.string.biometric_title),
                        activity.resources.getString(R.string.biometric_subtitle)
                    )
                    deviceCredentialActivityResultLauncher.launch(intent)
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val intent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                            putExtra(
                                Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                            )
                        }
                        securtiySetupActivityResultLauncher.launch(intent)
                    } else {
                        Toast.makeText(
                            activity,
                            "You have to setup a security method to your device.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun biometricOnSucceededCallback(
        result: BiometricPrompt.AuthenticationResult, handleAPI: HandleAPI
    ) {
        Log.d("MainActivity", "Authentication is successful")
        val authType = findConstantFieldName(
            BiometricPrompt::class.java, "AUTHENTICATION_RESULT_TYPE_", result.authenticationType
        )
        Log.d("MainActivity", "Authentication type: $authType")
        handleAPI.handleNotify("test@test2.com", OneSignalHolder.isAllowed!!)
    }

    override fun biometricOnErrorCallback(
        errorCode: Int, errString: CharSequence, handleAPI: HandleAPI
    ) {
        Log.d("MainActivity", "$errorCode :: $errString")
        handleAPI.handleNotify("test@test2.com", false)
    }

    override fun biometricOnFailedCallback(handleAPI: HandleAPI) {
        Log.d("MainActivity", "Authentication failed for an unknown reason")
        handleAPI.handleNotify("test@test2.com", false)
    }

    override fun deviceCredentialOnSucceededCallback(handleAPI: HandleAPI) {
        Log.d("MainActivity", "Authentication is successful")
        Log.d("MainActivity", "Authentication type: DEVICE CREDENTIAL")
        handleAPI.handleNotify("test@test2.com", OneSignalHolder.isAllowed!!)
    }

    override fun deviceCredentialOnFailedCallback(handleAPI: HandleAPI) {
        handleAPI.handleNotify("test@test2.com", false)
    }
}