package com.aarvo.network

import com.aarvo.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class Msg91ServerApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
    private val json = "application/json; charset=utf-8".toMediaType()

    suspend fun verifyOtp(phone: String, reqId: String, otp: String, role: String = "BUYER"): JSONObject = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("phone", phone)
            .put("reqId", reqId)
            .put("otp", otp)
            .put("role", role.uppercase())
        val request = Request.Builder()
            .url(BuildConfig.AARVO_API_BASE_URL.trimEnd('/') + "/v1/auth/verify-msg91-otp")
            .post(payload.toString().toRequestBody(json))
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) {
                val message = runCatching {
                    JSONObject(body).optString("error").takeIf { it.isNotBlank() }
                        ?: JSONObject(body).optString("message").takeIf { it.isNotBlank() }
                }.getOrNull()
                error("API ${response.code}: ${message ?: "OTP verification failed"}")
            }
            JSONObject(body)
        }
    }
}
