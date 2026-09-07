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
import java.net.URI
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

    fun isConfigured(): Boolean = runCatching {
        val uri = URI(baseUrl)
        uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank() && uri.query == null && uri.fragment == null
    }.getOrDefault(false)

    private fun buildUrl(path: String): String {
        require(isConfigured()) { "AARVO live API is not configured in this build. Set aarvoApiBaseUrl to an HTTPS API URL." }
        require(path.startsWith('/')) { "API path must start with /" }
        require(!path.contains("..")) { "API path contains an invalid segment" }
        return baseUrl + path
    }

    suspend fun health(): JSONObject = getObject("/health")

    suspend fun login(phone: String, password: String): JSONObject {
        val normalizedPhone = IndianPhoneValidator.isValidOrThrow(phone)
        require(password.length >= 8) { "Password must be at least 8 characters" }
        return post("/v1/auth/login", JSONObject().put("phone", normalizedPhone).put("password", password))
    }

    suspend fun register(
        email: String = "",
        password: String,
        displayName: String,
        role: String = "BUYER",
        phone: String
    ): JSONObject {
        val normalizedRole = role.trim().uppercase()
        val normalizedPhone = IndianPhoneValidator.isValidOrThrow(phone)
        require(password.length >= 8) { "Password must be at least 8 characters" }
        require(displayName.trim().isNotBlank()) { "Display name is required" }
        require(normalizedRole in setOf("BUYER", "SELLER")) { "Invalid account role" }
        val payload = JSONObject()
            .put("password", password)
            .put("displayName", displayName.trim())
            .put("role", normalizedRole)
            .put("phone", normalizedPhone)
        if (email.trim().isNotBlank()) {
            require(email.trim().contains('@')) { "Enter a valid email or leave it blank" }
            payload.put("email", email.trim())
        }
        return post("/v1/auth/register", payload)
    }

    suspend fun verifyPhoneOtp(phone: String, otp: String): JSONObject {
        val normalizedPhone = IndianPhoneValidator.isValidOrThrow(phone)
        require(Regex("^[0-9]{4,8}$").matches(otp.trim())) { "Enter the OTP" }
        return post("/v1/auth/verify-phone", JSONObject().put("phone", normalizedPhone).put("otp", otp.trim()))
    }

    suspend fun resendPhoneOtp(phone: String): JSONObject {
        val normalizedPhone = IndianPhoneValidator.isValidOrThrow(phone)
        return post("/v1/auth/resend-phone-otp", JSONObject().put("phone", normalizedPhone))
    }

    suspend fun products(query: String = "", category: String = ""): JSONArray {
        val params = buildList {
            if (query.isNotBlank()) add("q=${URLEncoder.encode(query, "UTF-8")}")
            if (category.isNotBlank() && category != "All") add("category=${URLEncoder.encode(category, "UTF-8")}")
        }.joinToString("&").let { if (it.isBlank()) "" else "?$it" }
        return get("/v1/products$params")
    }

    suspend fun product(productId: Int): JSONObject { require(productId > 0) { "Product ID must be positive" }; return getObject("/v1/products/$productId") }
    suspend fun productReviews(productId: Int): JSONArray { require(productId > 0) { "Product ID must be positive" }; return get("/v1/products/$productId/reviews") }

    suspend fun createOrder(items: JSONArray, address: JSONObject, idempotencyKey: String = UUID.randomUUID().toString()): JSONObject = withContext(Dispatchers.IO) {
        require(!tokenProvider().isNullOrBlank()) { "Login or verify your mobile number before purchasing." }
        require(items.length() > 0) { "Order must contain at least one item" }; require(address.length() > 0) { "Delivery address is required" }; require(idempotencyKey.length in 8..128) { "Invalid idempotency key" }
        val response = execute(Request.Builder().url(buildUrl("/v1/orders")).applyAuth().header("Idempotency-Key", idempotencyKey).post(JSONObject().put("items", items).put("address", address).toString().toRequestBody(jsonMediaType)).build()); JSONObject(response)
    }
    suspend fun orders(): JSONArray = get("/v1/orders")
    suspend fun order(orderId: String): JSONObject { require(orderId.trim().isNotBlank()) { "Order ID is required" }; return getObject("/v1/orders/${orderId.trim()}") }
    suspend fun orderTracking(orderId: String): JSONArray { require(orderId.trim().isNotBlank()) { "Order ID is required" }; return get("/v1/orders/${orderId.trim()}/tracking") }
    suspend fun cancelOrder(orderId: String, reason: String = "BUYER_CANCELLED"): JSONObject { require(orderId.trim().isNotBlank()) { "Order ID is required" }; return post("/v1/orders/${orderId.trim()}/cancel", JSONObject().put("reason", reason.trim())) }
    suspend fun submitReview(orderId: String, productId: Int, rating: Int, reviewText: String = ""): JSONObject { require(orderId.trim().isNotBlank()) { "Order ID is required" }; require(productId > 0) { "Product ID must be positive" }; require(rating in 1..5) { "Rating must be between 1 and 5" }; return post("/v1/orders/${orderId.trim()}/reviews", JSONObject().put("productId", productId).put("rating", rating).put("reviewText", reviewText.trim())) }
    suspend fun openDispute(orderId: String, reason: String, details: String = ""): JSONObject { require(orderId.trim().isNotBlank()) { "Order ID is required" }; require(reason.trim().isNotBlank()) { "Dispute reason is required" }; return post("/v1/orders/${orderId.trim()}/disputes", JSONObject().put("reason", reason.trim()).put("details", details.trim())) }
    suspend fun verifyPayment(orderId: String, paymentId: String, razorpayOrderId: String, signature: String): JSONObject { require(orderId.trim().isNotBlank()) { "Order ID is required" }; require(paymentId.trim().isNotBlank()) { "Payment ID is required" }; require(razorpayOrderId.trim().isNotBlank()) { "Gateway order ID is required" }; require(signature.trim().isNotBlank()) { "Payment signature is required" }; return post("/v1/payments/verify", JSONObject().put("orderId", orderId.trim()).put("razorpayOrderId", razorpayOrderId.trim()).put("razorpayPaymentId", paymentId.trim()).put("razorpaySignature", signature.trim())) }
    suspend fun sellerProfile(): JSONObject = getObject("/v1/seller/profile")
    suspend fun sellerProducts(): JSONArray = get("/v1/seller/products")
    suspend fun createSellerProduct(name: String, category: String, pricePaise: Long, description: String, stockQuantity: Int, publish: Boolean = false): JSONObject { require(name.trim().isNotBlank()) { "Product name is required" }; require(category.trim().isNotBlank()) { "Product category is required" }; require(pricePaise > 0) { "Product price must be positive" }; require(description.trim().isNotBlank()) { "Product description is required" }; require(stockQuantity >= 0) { "Product stock cannot be negative" }; return post("/v1/seller/products", JSONObject().put("name", name.trim()).put("category", category.trim()).put("pricePaise", pricePaise).put("description", description.trim()).put("stockQuantity", stockQuantity).put("publish", publish)) }
    suspend fun updateInventory(productId: Int, stockQuantity: Int): JSONObject { require(productId > 0) { "Product ID must be positive" }; require(stockQuantity >= 0) { "Product stock cannot be negative" }; return post("/v1/seller/products/$productId/inventory", JSONObject().put("stockQuantity", stockQuantity)) }
    suspend fun sellerOrders(): JSONArray = get("/v1/seller/orders")
    suspend fun updateOrderTracking(orderId: String, status: String, trackingCode: String = "", carrier: String = "", note: String = ""): JSONObject { require(orderId.trim().isNotBlank()) { "Order ID is required" }; require(status.trim().isNotBlank()) { "Tracking status is required" }; return post("/v1/orders/${orderId.trim()}/tracking", JSONObject().put("status", status.trim().uppercase()).put("trackingCode", trackingCode.trim()).put("carrier", carrier.trim()).put("note", note.trim())) }

    private suspend fun get(path: String): JSONArray = withContext(Dispatchers.IO) { JSONArray(execute(Request.Builder().url(buildUrl(path)).applyAuth().get().build())) }
    private suspend fun getObject(path: String): JSONObject = withContext(Dispatchers.IO) { JSONObject(execute(Request.Builder().url(buildUrl(path)).applyAuth().get().build())) }
    private suspend fun post(path: String, payload: JSONObject): JSONObject = withContext(Dispatchers.IO) { JSONObject(execute(Request.Builder().url(buildUrl(path)).applyAuth().post(payload.toString().toRequestBody(jsonMediaType)).build())) }
    private fun execute(request: Request): String { client.newCall(request).execute().use { response -> val body = response.body.string(); if (!response.isSuccessful) { val message = runCatching { JSONObject(body).optString("error").takeIf { it.isNotBlank() } ?: JSONObject(body).optString("message").takeIf { it.isNotBlank() } }.getOrNull(); error("API ${response.code}: ${message ?: "Request failed"}") }; return body } }
    private fun Request.Builder.applyAuth(): Request.Builder { tokenProvider()?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }; return this }
}
