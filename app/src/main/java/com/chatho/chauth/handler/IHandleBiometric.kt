package com.chatho.chauth.handler

import androidx.biometric.BiometricPrompt
import com.chatho.chauth.api.HandleAPI

interface IHandleBiometric {
     fun biometricSetup()
     fun biometricOnSucceededCallback(result: BiometricPrompt.AuthenticationResult, handleAPI: HandleAPI)
     fun biometricOnErrorCallback(errorCode: Int, errString: CharSequence, handleAPI: HandleAPI)
     fun biometricOnFailedCallback(handleAPI: HandleAPI)
     fun deviceCredentialOnSucceededCallback(handleAPI: HandleAPI)
     fun deviceCredentialOnFailedCallback(handleAPI: HandleAPI)
}