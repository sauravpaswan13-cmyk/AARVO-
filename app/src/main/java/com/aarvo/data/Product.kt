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
    val pricePaise: Long = price.toLong() * 100L
) {
    init {
        require(id > 0) { "Product ID must be positive" }
        require(price >= 0) { "Product price cannot be negative" }
        require(pricePaise >= 0) { "Product price in paise cannot be negative" }
        require(stockQuantity >= 0) { "Product stock cannot be negative" }
        require(rating in 0.0..5.0) { "Product rating must be between 0 and 5" }
    }

    /** Exact rupee display without floating-point arithmetic. */
    val displayPrice: String
        get() = "₹${pricePaise / 100}.${(pricePaise % 100).toString().padStart(2, '0')}"
}
