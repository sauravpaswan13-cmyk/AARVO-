package com.aarvo.data

enum class UserRole { BUYER, SELLER, ADMIN }

enum class OrderStatus {
    PENDING_PAYMENT,
    PAID,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED,
    DISPUTED
}

enum class PaymentStatus { CREATED, AUTHORIZED, CAPTURED, FAILED, REFUNDED }

data class SellerProfile(
    val sellerId: String,
    val displayName: String,
    val phone: String,
    val verified: Boolean,
    val payoutAccountReady: Boolean
)

data class DeliveryAddress(
    val fullName: String,
    val phone: String,
    val line1: String,
    val line2: String = "",
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String = "IN"
)

data class OrderLine(
    val productId: Int,
    val sellerId: String,
    val quantity: Int,
    val unitPrice: Int
)

data class MarketplaceOrder(
    val orderId: String,
    val buyerId: String,
    val lines: List<OrderLine>,
    val subtotal: Int,
    val deliveryFee: Int,
    val platformFee: Int,
    val total: Int,
    val paymentStatus: PaymentStatus,
    val status: OrderStatus,
    val deliveryAddress: DeliveryAddress
)

/** Server-authoritative marketplace contract. The mobile app never decides final price, stock or payout. */
interface MarketplaceApi {
    suspend fun getProducts(query: String = "", category: String? = null): List<Product>
    suspend fun getProduct(productId: Int): Product
    suspend fun createOrder(lines: List<OrderLine>, address: DeliveryAddress): MarketplaceOrder
    suspend fun getOrders(): List<MarketplaceOrder>
    suspend fun getSellerProfile(): SellerProfile?
    suspend fun publishProduct(product: Product): Product
    suspend fun updateInventory(productId: Int, stockQuantity: Int): Product
}
