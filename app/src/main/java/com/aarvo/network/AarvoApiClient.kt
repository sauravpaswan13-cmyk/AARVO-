package com.aarvo.network

import com.aarvo.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

class AarvoApiClient(
    private val tokenProvider: () -> String? = { null }
) {
    private val baseUrl: String = BuildConfig.AARVO_API_BASE_URL.trimEnd('/')
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun isConfigured(): Boolean = baseUrl.startsWith("https://")

    suspend fun health(): JSONObject = getObject("/health")

    suspend fun login(email: String, password: String): JSONObject = post(
        "/v1/auth/login", JSONObject().put("email", email.trim()).put("password", password)
    )

    suspend fun register(
        email: String,
        password: String,
        displayName: String,
        role: String = "BUYER",
        phone: String = ""
    ): JSONObject = post(
        "/v1/auth/register",
        JSONObject().put("email", email.trim()).put("password", password)
            .put("displayName", displayName.trim()).put("role", role).put("phone", phone.trim())
    )

    suspend fun products(query: String = "", category: String = ""): JSONArray {
        val params = buildList {
            if (query.isNotBlank()) add("q=${URLEncoder.encode(query, "UTF-8")}")
            if (category.isNotBlank() && category != "All") add("category=${URLEncoder.encode(category, "UTF-8")}")
        }.joinToString("&").let { if (it.isBlank()) "" else "?$it" }
        return get("/v1/products$params")
    }

    suspend fun product(productId: Int): JSONObject = getObject("/v1/products/$productId")

    suspend fun createOrder(
        items: JSONArray,
        address: JSONObject,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): JSONObject = withContext(Dispatchers.IO) {
        require(idempotencyKey.length in 8..128) { "Invalid idempotency key" }
        val payload = JSONObject().put("items", items).put("address", address)
        val response = execute(
            Request.Builder().url(baseUrl + "/v1/orders").applyAuth()
                .header("Idempotency-Key", idempotencyKey)
                .post(payload.toString().toRequestBody(jsonMediaType)).build()
        )
        JSONObject(response)
    }

    suspend fun orders(): JSONArray = get("/v1/orders")

    suspend fun order(orderId: String): JSONObject = getObject("/v1/orders/$orderId")

    suspend fun sellerOrders(): JSONArray = get("/v1/seller/orders")

    suspend fun updateOrderTracking(
        orderId: String,
        status: String,
        trackingCode: String = "",
        carrier: String = "",
        note: String = ""
    ): JSONObject = post(
        "/v1/orders/$orderId/tracking",
        JSONObject().put("status", status)
            .put("trackingCode", trackingCode.trim())
            .put("carrier", carrier.trim())
            .put("note", note.trim())
    )

    suspend fun submitReview(orderId: String, productId: Int, rating: Int, reviewText: String = ""): JSONObject = post(
        "/v1/orders/$orderId/reviews",
        JSONObject().put("productId", productId).put("rating", rating).put("reviewText", reviewText.trim())
    )

    suspend fun productReviews(productId: Int): JSONArray = get("/v1/products/$productId/reviews")

    suspend fun openDispute(orderId: String, reason: String, details: String = ""): JSONObject = post(
        "/v1/orders/$orderId/disputes",
        JSONObject().put("reason", reason.trim()).put("details", details.trim())
    )

    suspend fun verifyPayment(
        orderId: String,
        paymentId: String,
        razorpayOrderId: String,
        signature: String
    ): JSONObject = post(
        "/v1/payments/verify",
        JSONObject().put("orderId", orderId)
            .put("razorpayOrderId", razorpayOrderId)
            .put("razorpayPaymentId", paymentId)
            .put("razorpaySignature", signature)
    )

    suspend fun cancelOrder(orderId: String): JSONObject = post(
        "/v1/orders/$orderId/cancel", JSONObject()
    )

    suspend fun sellerProfile(): JSONObject = getObject("/v1/seller/profile")

    suspend fun sellerProducts(): JSONArray = get("/v1/seller/products")

    suspend fun createSellerProduct(
        name: String,
        category: String,
        pricePaise: Long,
        description: String,
        stockQuantity: Int,
        publish: Boolean = false
    ): JSONObject = post(
        "/v1/seller/products",
        JSONObject().put("name", name.trim()).put("category", category.trim())
            .put("pricePaise", pricePaise).put("description", description.trim())
            .put("stockQuantity", stockQuantity).put("publish", publish)
    )

    suspend fun updateInventory(productId: Int, stockQuantity: Int): JSONObject = post(
        "/v1/seller/products/$productId/inventory",
        JSONObject().put("stockQuantity", stockQuantity)
    )

    private suspend fun get(path: String): JSONArray = withContext(Dispatchers.IO) {
        val response = execute(Request.Builder().url(baseUrl + path).applyAuth().get().build())
        JSONArray(response)
    }

    private suspend fun getObject(path: String): JSONObject = withContext(Dispatchers.IO) {
        val response = execute(Request.Builder().url(baseUrl + path).applyAuth().get().build())
        JSONObject(response)
    }

    private suspend fun post(path: String, payload: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val response = execute(
            Request.Builder().url(baseUrl + path).applyAuth()
                .post(payload.toString().toRequestBody(jsonMediaType)).build()
        )
        JSONObject(response)
    }

    private fun execute(request: Request): String {
        require(isConfigured()) { "AARVO_API_BASE_URL is not configured with HTTPS" }
        client.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) {
                val message = runCatching { JSONObject(body).optString("error") }.getOrNull()
                    ?.takeIf { it.isNotBlank() }
                error("API ${response.code}: ${message ?: body.ifBlank { "Request failed" }}")
            }
            return body
        }
    }

    private fun Request.Builder.applyAuth(): Request.Builder {
        tokenProvider()?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
        return this
    }
}
