package com.aarvo.network

import com.aarvo.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.TimeUnit

/** Client for the authentication phone-verification contract. */
class PhoneVerificationApi(private val tokenProvider: () -> String? = { null }) {
    private val baseUrl = BuildConfig.AARVO_API_BASE_URL.trimEnd('/')
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private fun url(path: String): String {
        val uri = URI(baseUrl)
        require(uri.scheme.equals("https", true) && !uri.host.isNullOrBlank() && uri.query == null && uri.fragment == null) {
            "AARVO live API is not configured in this build."
        }
        require(path.startsWith('/') && !path.contains("..")) { "Invalid API path" }
        return baseUrl + path
    }

    suspend fun verify(verificationId: String, code: String): JSONObject = request(
        "/v1/auth/phone/verify",
        JSONObject().put("verificationId", verificationId.trim()).put("code", code.trim())
    )

    suspend fun resend(verificationId: String): JSONObject = request(
        "/v1/auth/phone/resend",
        JSONObject().put("verificationId", verificationId.trim())
    )

    private suspend fun request(path: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        require(body.optString("verificationId").isNotBlank()) { "Verification ID is required" }
        if (path.endsWith("/verify")) require(body.optString("code").matches(Regex("\\d{6}"))) { "Enter the 6-digit OTP" }
        val builder = Request.Builder().url(url(path)).post(body.toString().toRequestBody(jsonType))
        tokenProvider()?.takeIf { it.isNotBlank() }?.let { builder.header("Authorization", "Bearer $it") }
        client.newCall(builder.build()).execute().use { response ->
            val raw = response.body.string()
            if (!response.isSuccessful) {
                val message = runCatching { JSONObject(raw).optString("error") }.getOrNull().orEmpty()
                error("API ${response.code}: ${message.ifBlank { "Phone verification failed" }}")
            }
            JSONObject(raw)
        }
    }
}
