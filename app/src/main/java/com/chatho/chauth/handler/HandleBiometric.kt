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

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
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

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val DEFAULT_KEY_NAME = "chauth_key"
    }

    init {
        setupKeyStoreAndKeyGenerator()
        createBiometricPrompt()
    }

    private fun authenticate(cipher: Cipher, keyName: String) {
        val promptInfo = createPromptInfo()

        if (initCipher(cipher, keyName)) {
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

    private fun createKey(keyName: String) {
        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of enrolled
        // fingerprints has changed.
        try {
            keyStore.load(null)

            val keyProperties = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            val builder = KeyGenParameterSpec.Builder(keyName, keyProperties)
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
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG) // Allow PIN/pattern/password authentication.
            .build()
    }

    private fun initCipher(cipher: Cipher, keyName: String): Boolean {
        try {
            keyStore.load(null)
            cipher.init(Cipher.ENCRYPT_MODE, keyStore.getKey(keyName, null) as SecretKey)
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
        if (BiometricManager.from(activity)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        ) {
            val defaultCipher: Cipher = setupCiphers()

            createKey(DEFAULT_KEY_NAME)
            authenticate(defaultCipher, DEFAULT_KEY_NAME)
        } else {
            Toast.makeText(activity, "Biometric is not available...", Toast.LENGTH_LONG).show()
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
        handleAPI.handleNotify("test@test2.com", OneSignalHolder.isAllowed!!) {
            OneSignalHolder.isAllowed = null
            OneSignalHolder.clientIpAddress = null
        }
    }

    override fun biometricOnErrorCallback(
        errorCode: Int, errString: CharSequence, handleAPI: HandleAPI
    ) {
        Log.d("MainActivity", "$errorCode :: $errString")
        handleAPI.handleNotify("test@test2.com", false) {
            OneSignalHolder.isAllowed = null
            OneSignalHolder.clientIpAddress = null
        }
    }

    override fun biometricOnFailedCallback(handleAPI: HandleAPI) {
        Log.d("MainActivity", "Authentication failed for an unknown reason")
        handleAPI.handleNotify("test@test2.com", false) {
            OneSignalHolder.isAllowed = null
            OneSignalHolder.clientIpAddress = null
        }
    }
}