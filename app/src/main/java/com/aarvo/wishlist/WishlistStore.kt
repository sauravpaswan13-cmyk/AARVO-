package com.aarvo.wishlist

import android.content.SharedPreferences

/**
 * Small persistent wishlist store for buyer product IDs.
 * Product details remain server-authoritative; only the user's saved IDs are persisted locally.
 * Mutations are serialized so concurrent UI callbacks cannot lose a toggle.
 */
class WishlistStore(private val prefs: SharedPreferences) {
    companion object {
        private const val KEY_PRODUCT_IDS = "wishlist_product_ids"
        private const val SEPARATOR = ","
    }

    fun load(): Set<Int> = prefs.getString(KEY_PRODUCT_IDS, null)
        ?.split(SEPARATOR)
        ?.mapNotNull { it.toIntOrNull() }
        ?.filter { it > 0 }
        ?.toSet()
        ?: emptySet()

    @Synchronized
    fun toggle(productId: Int): Set<Int> {
        require(productId > 0) { "Product ID must be positive" }
        val next = load().toMutableSet().apply {
            if (!add(productId)) remove(productId)
        }.toSet()
        save(next)
        return next
    }

    @Synchronized
    fun clear() {
        prefs.edit().remove(KEY_PRODUCT_IDS).apply()
    }

    private fun save(ids: Set<Int>) {
        prefs.edit()
            .putString(KEY_PRODUCT_IDS, ids.sorted().joinToString(SEPARATOR))
            .apply()
    }
}
