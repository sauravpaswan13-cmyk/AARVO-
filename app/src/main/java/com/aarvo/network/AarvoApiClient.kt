package com.aarvo.network

import com.aarvo.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class AarvoApiClient(
    private val tokenProvider: () -> String? = { null }
) {
    private val baseUrl: String = BuildConfig.AARVO_API_BASE_URL.trimEnd('/')
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun isConfigured(): Boolean = baseUrl.startsWith("https://")

    fun login(email: String, password: String): JSONObject = post(
        "/v1/auth/login",
        JSONObject().put("email", email.trim()).put("password", password)
    )

    fun register(email: String, password: String, displayName: String, role: String = "BUYER", phone: String = ""): JSONObject = post(
        "/v1/auth/register",
        JSONObject()
            .put("email", email.trim())
            .put("password", password)
            .put("displayName", displayName.trim())
            .put("role", role)
            .put("phone", phone.trim())
    )

    fun products(query: String = "", category: String = ""): JSONArray {
        val params = buildString {
            if (query.isNotBlank()) append("?q=${java.net.URLEncoder.encode(query, "UTF-8")}")
            if (category.isNotBlank()) append(if (contains("?")) "&" else "?").append("category=${java.net.URLEncoder.encode(category, "UTF-8")}")
        }
        return get("/v1/products$params")
    }

    fun createOrder(items: JSONArray, address: JSONObject): JSONObject = post(
        "/v1/orders",
        JSONObject().put("items", items).put("address", address)
    )

    fun verifyPayment(orderId: String, paymentId: String, razorpayOrderId: String, signature: String): JSONObject = post(
        "/v1/payments/verify",
        JSONObject()
            .put("orderId", orderId)
            .put("razorpayOrderId", razorpayOrderId)
            .put("razorpayPaymentId", paymentId)
            .put("razorpaySignature", signature)
    )

    private fun get(path: String): JSONArray {
        require(isConfigured()) { "AARVO_API_BASE_URL is not configured with HTTPS" }
        val request = Request.Builder().url(baseUrl + path).applyAuth().get().build()
        client.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) error("API ${response.code}: $body")
            return JSONArray(body)
        }
    }

    private fun post(path: String, payload: JSONObject): JSONObject {
        require(isConfigured()) { "AARVO_API_BASE_URL is not configured with HTTPS" }
        val request = Request.Builder()
            .url(baseUrl + path)
            .applyAuth()
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) error("API ${response.code}: $body")
            return JSONObject(body)
        }
    }

    private fun Request.Builder.applyAuth(): Request.Builder {
        tokenProvider()?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
        return this
    }
}
