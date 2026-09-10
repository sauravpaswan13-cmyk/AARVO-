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

class AdminAuthApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
    private val json = "application/json; charset=utf-8".toMediaType()

    suspend fun login(email: String, password: String): JSONObject = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("email", email.trim()).put("password", password)
        val request = Request.Builder()
            .url(BuildConfig.AARVO_API_BASE_URL.trimEnd('/') + "/v1/auth/admin/login")
            .post(payload.toString().toRequestBody(json))
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) {
                val message = runCatching { JSONObject(body).optString("error") }.getOrNull().orEmpty()
                error("API ${response.code}: ${message.ifBlank { "Admin login failed" }}")
            }
            JSONObject(body)
        }
    }
}
