package com.chatho.chauth.api

import android.content.Context
import android.widget.Toast
import com.chatho.chauth.BuildConfig
import com.chatho.chauth.holder.OneSignalHolder
import com.chatho.chauth.util.runInCoroutineScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Exception

class HandleAPI(private val context: Context) {
    companion object {
        private const val ipFetchDomain = "http://ip-api.com"
    }

    fun handleIPLocation(ipAddress: String, callback: (IPLocationResponse?, Boolean) -> Unit) {
        val retrofit = Retrofit.Builder().baseUrl(ipFetchDomain)
            .addConverterFactory(GsonConverterFactory.create()).build()

        val service = retrofit.create(APIService::class.java)

        val url = "json/$ipAddress?fields=1622745"

        runInCoroutineScope(Dispatchers.IO) {
            try {
                val request = service.handleIPGeo(url)

                request.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>
                    ) {
                        val responseBody = response.body()
                        val responseBodyString = responseBody?.string()
                        val jsonResponse =
                            Gson().fromJson(responseBodyString, IPLocationResponse::class.java)

                        if (response.isSuccessful) {
                            callback(jsonResponse, true)
                        } else {
                            callback(null, false)
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        callback(null, false)

                        runInCoroutineScope(Dispatchers.Main) {
                            Toast.makeText(
                                context, "Error: ${t.message}", Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                })
            } catch (e: Exception) {
                callback(null, false)

                runInCoroutineScope(Dispatchers.Main) {
                    Toast.makeText(
                        context, "Fetching IP location failed: ${e.message}", Toast.LENGTH_LONG
                    ).show()
                }
            }

        }
    }

    fun handleNotify(email: String, isAllowed: Boolean) {
        val domain =
            if (OneSignalHolder.backendBuildType == "development") "http://192.168.1.7:5000" else BuildConfig.BACKEND_EXPRESS_API_URL
        val retrofit =
            Retrofit.Builder().baseUrl(domain).addConverterFactory(GsonConverterFactory.create())
                .build()

        val service = retrofit.create(APIService::class.java)

        val requestData = AuthNotifyRequest(email, isAllowed)
        OneSignalHolder.isAllowed = null
        OneSignalHolder.clientIpAddress = null

        runInCoroutineScope(Dispatchers.IO) {
            try {
                val request = service.handleNotify(requestData)

                request.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>
                    ) {
                        if (response.isSuccessful) {
                            val responseBody = response.body()
                            val responseBodyString = responseBody?.string()
                            val authResponse =
                                Gson().fromJson(responseBodyString, AuthNotifyResponse::class.java)

                            runInCoroutineScope(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    if (authResponse.success) authResponse.message else "Error: ${authResponse.message}",
                                    if (authResponse.success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            val errorBody = response.errorBody()
                            val errorBodyString = errorBody?.string()
                            val authResponse =
                                Gson().fromJson(errorBodyString, AuthNotifyResponse::class.java)

                            runInCoroutineScope(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "${response.code()} Error:${authResponse.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        runInCoroutineScope(Dispatchers.Main) {
                            Toast.makeText(
                                context, "Error: ${t.message}", Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                })
            } catch (e: Exception) {
                runInCoroutineScope(Dispatchers.Main) {
                    Toast.makeText(
                        context, "Auth failed: ${e.message}", Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun handleQR(email: String, encodedQR: String, buildType: String) {
        val domain =
            if (buildType == "development") "http://192.168.1.7:5000" else BuildConfig.BACKEND_EXPRESS_API_URL
        val retrofit =
            Retrofit.Builder().baseUrl(domain).addConverterFactory(GsonConverterFactory.create())
                .build()

        val service = retrofit.create(APIService::class.java)

        val requestData = AuthQRRequest(email, encodedQR)

        runInCoroutineScope(Dispatchers.IO) {
            try {
                val request = service.handleQR(requestData)

                request.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>
                    ) {
                        if (response.isSuccessful) {
                            val responseBody = response.body()
                            val responseBodyString = responseBody?.string()
                            val authResponse =
                                Gson().fromJson(responseBodyString, AuthQRResponse::class.java)

                            runInCoroutineScope(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    if (authResponse.success) authResponse.message else "Error: ${authResponse.message}",
                                    if (authResponse.success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            val errorBody = response.errorBody()
                            val errorBodyString = errorBody?.string()
                            val authResponse =
                                Gson().fromJson(errorBodyString, AuthQRResponse::class.java)

                            runInCoroutineScope(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "${response.code()} Error:${authResponse.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        runInCoroutineScope(Dispatchers.Main) {
                            Toast.makeText(
                                context, "Error: ${t.message}", Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                })
            } catch (e: Exception) {
                runInCoroutineScope(Dispatchers.Main) {
                    Toast.makeText(
                        context, "Auth failed: ${e.message}", Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}