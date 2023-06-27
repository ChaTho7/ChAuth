package com.chatho.chauth.handler

import androidx.biometric.BiometricPrompt

interface IHandleBiometric {
     fun biometricSetup()
     fun biometricOnSucceededCallback(result: BiometricPrompt.AuthenticationResult)
     fun biometricOnErrorCallback(errorCode: Int, errString: CharSequence)
     fun biometricOnFailedCallback()
}