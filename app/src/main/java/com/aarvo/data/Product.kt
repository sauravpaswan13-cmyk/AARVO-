package com.aarvo.data

data class Product(
    val id: Int,
    val name: String,
    val category: String,
    val price: Int,
    val rating: Double,
    val emoji: String,
    val description: String
)

val sampleProducts = listOf(
    Product(1, "Everyday Sneakers", "Fashion", 1499, 4.6, "👟", "Comfortable everyday sneakers with a clean modern look."),
    Product(2, "Wireless Earbuds", "Electronics", 999, 4.4, "🎧", "Compact wireless earbuds with clear sound for daily use."),
    Product(3, "Classic Backpack", "Fashion", 799, 4.5, "🎒", "A lightweight backpack for work, college and travel."),
    Product(4, "Smart Watch", "Electronics", 1999, 4.3, "⌚", "A versatile smartwatch for notifications, activity and style."),
    Product(5, "Home Lamp", "Home", 699, 4.2, "💡", "Warm ambient lighting for desks, bedrooms and living spaces."),
    Product(6, "Daily Face Wash", "Beauty", 299, 4.5, "🧴", "A gentle daily cleanser for a fresh, comfortable feel.")
)
