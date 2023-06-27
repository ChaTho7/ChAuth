package com.chatho.chauth.handler

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.nio.charset.StandardCharsets
import java.util.Base64

class HandleEncoding {
    companion object {
        fun encodeQRCode(qrCode: String): String {
            val qrSecret = "ChAuthSecret"

            val hmacSHA512 = Mac.getInstance("HmacSHA512")
            val secretKey =
                SecretKeySpec(qrSecret.toByteArray(StandardCharsets.UTF_8), "HmacSHA512")
            hmacSHA512.init(secretKey)

            val hmacResult = hmacSHA512.doFinal(qrCode.toByteArray(StandardCharsets.UTF_8))
            val encodedResult = Base64.getEncoder().encodeToString(hmacResult)

            return encodedResult
        }
    }

}