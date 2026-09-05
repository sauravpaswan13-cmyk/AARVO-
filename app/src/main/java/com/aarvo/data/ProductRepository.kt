package com.aarvo.data

class ProductRepository {
    fun categories(): List<String> = listOf("All", "Fashion", "Electronics", "Home", "Beauty")

    fun products(query: String = "", category: String = "All"): List<Product> {
        val normalizedQuery = query.trim().lowercase()
        return sampleProducts.filter { product ->
            val matchesCategory = category == "All" || product.category == category
            val matchesQuery = normalizedQuery.isBlank() ||
                product.name.lowercase().contains(normalizedQuery) ||
                product.category.lowercase().contains(normalizedQuery)
            matchesCategory && matchesQuery
        }
    }
}
