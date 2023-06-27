package com.chatho.chauth.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface APIService {
    @Headers("Content-Type: application/json")
    @POST("/api/auth/notify/checkNotify")
    fun handleNotify(@Body authRequest: AuthNotifyRequest): Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("/api/auth/qr/checkQR")
    fun handleQR(@Body authRequest: AuthQRRequest): Call<ResponseBody>

    @GET
    fun handleIPGeo(@Url url: String): Call<ResponseBody>
}

data class IPLocationResponse(
    val status: String,
    val continent: String,
    val country: String,
    val regionName: String,
    val city: String,
    val district: String,
    val lat: Double,
    val lon: Double,
    val isp: String
)

data class AuthNotifyRequest(val email: String, val isAllowed: Boolean)
data class AuthNotifyResponse(val message: String, val success: Boolean)

data class AuthQRRequest(val email: String, val encodedQR: String)
data class AuthQRResponse(val message: String, val success: Boolean)