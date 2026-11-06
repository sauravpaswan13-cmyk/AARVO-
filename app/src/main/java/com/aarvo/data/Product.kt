package com.aarvo.data

data class Product(
    val id: Int,
    val sellerId: String,
    val sellerName: String,
    val name: String,
    val category: String,
    /** Whole-rupee compatibility value for legacy UI callers. */
    val price: Int,
    val rating: Double,
    val emoji: String,
    val description: String,
    val stockQuantity: Int,
    val isPublished: Boolean = true,
    /** Exact server-authoritative amount in paise. */
    val pricePaise: Long = price.toLong() * 100L,
    val imageUrl: String? = null,
    val originalPricePaise: Long = pricePaise,
    val reviewsCount: Int = 0
) {
    init {
        require(id > 0) { "Product ID must be positive" }
        require(price >= 0) { "Product price cannot be negative" }
        require(pricePaise >= 0) { "Product price in paise cannot be negative" }
        require(stockQuantity >= 0) { "Product stock cannot be negative" }
        require(rating in 0.0..5.0) { "Product rating must be between 0 and 5" }
        require(reviewsCount >= 0) { "Reviews count cannot be negative" }
    }

    /** Exact rupee display without floating-point arithmetic. */
    val displayPrice: String
        get() = "₹${pricePaise / 100}.${(pricePaise % 100).toString().padStart(2, '0')}"

    val hasDiscount: Boolean
        get() = originalPricePaise > pricePaise

    val discountPercent: Int
        get() = if (hasDiscount && originalPricePaise > 0) {
            (((originalPricePaise - pricePaise).toDouble() / originalPricePaise.toDouble()) * 100).toInt()
        } else 0
}
