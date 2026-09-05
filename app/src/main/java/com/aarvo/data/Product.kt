package com.aarvo.data

data class Product(
    val id: Int,
    val sellerId: String,
    val sellerName: String,
    val name: String,
    val category: String,
    val price: Int,
    val rating: Double,
    val emoji: String,
    val description: String,
    val stockQuantity: Int,
    val isPublished: Boolean = true
)

/**
 * Seed catalog used only until the live catalog API is connected.
 * Production purchases must always use server-authoritative product, price and stock data.
 */
val sampleProducts = listOf(
    Product(1, "seller_aarvo_demo", "AARVO Seller", "Everyday Sneakers", "Fashion", 1499, 4.6, "👟", "Comfortable everyday sneakers with a clean modern look.", 25),
    Product(2, "seller_aarvo_demo", "AARVO Seller", "Wireless Earbuds", "Electronics", 999, 4.4, "🎧", "Compact wireless earbuds with clear sound for daily use.", 40),
    Product(3, "seller_aarvo_demo", "AARVO Seller", "Classic Backpack", "Fashion", 799, 4.5, "🎒", "A lightweight backpack for work, college and travel.", 30),
    Product(4, "seller_aarvo_demo", "AARVO Seller", "Smart Watch", "Electronics", 1999, 4.3, "⌚", "A versatile smartwatch for notifications, activity and style.", 18),
    Product(5, "seller_aarvo_demo", "AARVO Seller", "Home Lamp", "Home", 699, 4.2, "💡", "Warm ambient lighting for desks, bedrooms and living spaces.", 22),
    Product(6, "seller_aarvo_demo", "AARVO Seller", "Daily Face Wash", "Beauty", 299, 4.5, "🧴", "A gentle daily cleanser for a fresh, comfortable feel.", 50)
)
