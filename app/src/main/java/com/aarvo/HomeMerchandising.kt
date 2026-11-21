package com.aarvo

import android.content.SharedPreferences
import com.aarvo.data.Product
import org.json.JSONArray
import org.json.JSONObject

data class HomeMerchandisingSectionConfig(
    val id: String,
    val title: String,
    val subtitle: String,
    val badgeText: String,
    val enabled: Boolean,
    val maxItems: Int,
    val sortOrder: Int,
    val pinnedProductIds: List<Int>
)

data class StillLookingEntry(
    val product: Product,
    val reasonLabel: String
)

data class BrandSpotlightEntry(
    val product: Product,
    val headline: String,
    val caption: String,
    val isAdminCurated: Boolean
)

data class MerchandisingProductPick(
    val product: Product,
    val isAdminCurated: Boolean
)

private const val PREF_MERCHANDISING_JSON = "aarvo_home_merchandising_v2"
private const val PREF_RECENTLY_VIEWED_IDS = "aarvo_recently_viewed_ids_v1"

fun defaultHomeMerchandisingSections(): List<HomeMerchandisingSectionConfig> = listOf(
    HomeMerchandisingSectionConfig(
        id = "still_looking",
        title = "Still looking for these?",
        subtitle = "Based on items you viewed, saved & carted",
        badgeText = "",
        enabled = true,
        maxItems = 3,
        sortOrder = 0,
        pinnedProductIds = emptyList()
    ),
    HomeMerchandisingSectionConfig(
        id = "brands_spotlight",
        title = "Brands in Spotlight",
        subtitle = "Featured storefronts & top offers",
        badgeText = "SPOTLIGHT",
        enabled = true,
        maxItems = 6,
        sortOrder = 1,
        pinnedProductIds = emptyList()
    ),
    HomeMerchandisingSectionConfig(
        id = "top_value_deals",
        title = "Top Value Deals",
        subtitle = "Budget-friendly & best-value picks",
        badgeText = "VALUE PICKS",
        enabled = true,
        maxItems = 3,
        sortOrder = 2,
        pinnedProductIds = emptyList()
    ),
    HomeMerchandisingSectionConfig(
        id = "suggested_for_you",
        title = "Suggested For You",
        subtitle = "Curated picks from your favorite categories",
        badgeText = "FOR YOU",
        enabled = true,
        maxItems = 3,
        sortOrder = 3,
        pinnedProductIds = emptyList()
    ),
    HomeMerchandisingSectionConfig(
        id = "more_products",
        title = "More products for you",
        subtitle = "Explore verified AARVO marketplace listings",
        badgeText = "DISCOVER",
        enabled = true,
        maxItems = 8,
        sortOrder = 4,
        pinnedProductIds = emptyList()
    )
)

fun parseMerchandisingProductJson(o: JSONObject): Product? = runCatching {
    val id = o.optLong("id", 0L).toInt()
    if (id <= 0) return@runCatching null
    val pricePaise = o.optLong("price_paise", -1L)
    if (pricePaise < 0L) return@runCatching null
    val originalPricePaise = if (o.isNull("original_price_paise")) {
        pricePaise
    } else {
        o.optLong("original_price_paise", pricePaise).coerceAtLeast(0L)
    }
    val reviewsCount = o.optInt("reviews_count", o.optInt("reviewsCount", 0)).coerceAtLeast(0)
    val deliveryFeePaise = if (o.isNull("delivery_fee_paise")) 0L else o.optLong("delivery_fee_paise", 0L).coerceAtLeast(0L)
    val codEnabled = if (o.isNull("cod_enabled")) true else o.optBoolean("cod_enabled", true)
    val rawImageUrl = if (o.isNull("image_url")) "" else o.optString("image_url", "")
    val rawDescription = if (o.isNull("description")) "" else o.optString("description", "")
    val rawRating = if (o.isNull("rating")) 0.0 else o.optDouble("rating", 0.0)
    val safeRating = if (rawRating.isNaN()) 0.0 else rawRating.coerceIn(0.0, 5.0)
    Product(
        id = id,
        sellerId = if (o.isNull("seller_id")) "" else o.optString("seller_id", ""),
        sellerName = if (o.isNull("seller_name")) "AARVO Seller" else o.optString("seller_name", "AARVO Seller").ifBlank { "AARVO Seller" },
        name = o.optString("name", "Product").ifBlank { "Product" },
        category = o.optString("category", "General").ifBlank { "General" },
        price = (pricePaise / 100L).toInt(),
        rating = safeRating,
        emoji = "🛍️",
        description = rawDescription,
        stockQuantity = o.optInt("stock_quantity", 0).coerceAtLeast(0),
        isPublished = if (o.isNull("is_published")) true else o.optBoolean("is_published", true),
        pricePaise = pricePaise,
        imageUrl = rawImageUrl.takeIf { it.startsWith("https://") || it.startsWith("http://") },
        originalPricePaise = originalPricePaise,
        reviewsCount = reviewsCount,
        deliveryFeePaise = deliveryFeePaise,
        codEnabled = codEnabled
    )
}.getOrNull()

fun parseRemoteMerchandisingSections(
    arr: JSONArray,
    fallback: List<HomeMerchandisingSectionConfig> = defaultHomeMerchandisingSections()
): Pair<List<HomeMerchandisingSectionConfig>, List<Product>> {
    if (arr.length() == 0) return fallback to emptyList()
    val fallbackById = fallback.associateBy { it.id }.toMutableMap()
    val parsed = mutableListOf<HomeMerchandisingSectionConfig>()
    val hydratedProducts = mutableListOf<Product>()
    val seenHydratedIds = mutableSetOf<Int>()

    for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        val id = obj.optString("id", obj.optString("section_key", "")).trim()
        if (id.isBlank()) continue
        val base = fallbackById.remove(id)
        val rawPins = obj.optJSONArray("pinnedProductIds")
        val pinnedIds = buildList {
            if (rawPins != null) {
                for (j in 0 until rawPins.length()) {
                    val pid = rawPins.optString(j, "").trim().toIntOrNull()
                        ?: rawPins.optInt(j, 0).takeIf { it > 0 }
                    if (pid != null && pid > 0 && pid !in this) add(pid)
                }
            }
        }
        val rawPinnedProducts = obj.optJSONArray("pinnedProducts")
        if (rawPinnedProducts != null) {
            for (j in 0 until rawPinnedProducts.length()) {
                val po = rawPinnedProducts.optJSONObject(j) ?: continue
                val p = parseMerchandisingProductJson(po) ?: continue
                if (seenHydratedIds.add(p.id)) hydratedProducts.add(p)
            }
        }
        val title = if (obj.isNull("title")) "" else obj.optString("title", "").trim()
        val subtitle = if (obj.isNull("subtitle")) "" else obj.optString("subtitle", "")
        val rawBadge = if (obj.isNull("badgeText")) "" else obj.optString("badgeText", "").trim()
        val badgeText = if (id == "still_looking" || rawBadge.equals("pink", ignoreCase = true)) "" else rawBadge
        parsed.add(
            HomeMerchandisingSectionConfig(
                id = id,
                title = title.ifBlank { base?.title ?: id },
                subtitle = subtitle,
                badgeText = badgeText,
                enabled = if (obj.isNull("enabled")) base?.enabled ?: true else obj.optBoolean("enabled", true),
                maxItems = obj.optInt("maxItems", base?.maxItems ?: 3).coerceIn(2, 16),
                sortOrder = obj.optInt("sortOrder", base?.sortOrder ?: i),
                pinnedProductIds = pinnedIds
            )
        )
    }

    fallbackById.values.forEach { parsed.add(it) }
    return parsed.sortedWith(compareBy<HomeMerchandisingSectionConfig> { it.sortOrder }.thenBy { it.id }) to hydratedProducts
}

fun loadCachedMerchandisingConfig(prefs: SharedPreferences): List<HomeMerchandisingSectionConfig> {
    val raw = prefs.getString(PREF_MERCHANDISING_JSON, null)?.takeIf { it.isNotBlank() } ?: return defaultHomeMerchandisingSections()
    return runCatching {
        parseRemoteMerchandisingSections(JSONArray(raw), defaultHomeMerchandisingSections()).first
    }.getOrDefault(defaultHomeMerchandisingSections())
}

fun saveCachedMerchandisingJson(prefs: SharedPreferences, arr: JSONArray) {
    prefs.edit().putString(PREF_MERCHANDISING_JSON, arr.toString()).apply()
}

fun loadLocalRecentlyViewedIds(prefs: SharedPreferences): List<Int> {
    val raw = prefs.getString(PREF_RECENTLY_VIEWED_IDS, null)?.takeIf { it.isNotBlank() } ?: return emptyList()
    return raw.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }.distinct().take(20)
}

fun recordLocalRecentlyViewedId(prefs: SharedPreferences, productId: Int): List<Int> {
    if (productId <= 0) return loadLocalRecentlyViewedIds(prefs)
    val next = (listOf(productId) + loadLocalRecentlyViewedIds(prefs).filter { it != productId }).take(20)
    prefs.edit().putString(PREF_RECENTLY_VIEWED_IDS, next.joinToString(",")).apply()
    return next
}

fun selectStillLookingEntries(
    catalog: List<Product>,
    recentlyViewedIds: List<Int>,
    wishlistIds: Set<Int>,
    cartProductIds: List<Int>,
    limit: Int = 3,
    pinnedIds: List<Int> = emptyList()
): List<StillLookingEntry> {
    val byId = catalog.associateBy { it.id }
    val picked = mutableListOf<StillLookingEntry>()
    val seen = mutableSetOf<Int>()

    fun pushUnique(product: Product?, label: String) {
        if (product == null || !seen.add(product.id) || picked.size >= limit) return
        picked.add(StillLookingEntry(product = product, reasonLabel = label))
    }

    pinnedIds.forEach { pushUnique(byId[it], "Featured") }
    recentlyViewedIds.forEach { pushUnique(byId[it], "Recently Viewed") }
    wishlistIds.forEach { pushUnique(byId[it], "Saved in Wishlist") }
    cartProductIds.forEach { pushUnique(byId[it], "In Your Cart") }

    return picked.take(limit)
}

fun selectBrandsInSpotlight(
    catalog: List<Product>,
    limit: Int = 6,
    pinnedIds: List<Int> = emptyList()
): List<BrandSpotlightEntry> {
    val byId = catalog.associateBy { it.id }
    val chosen = mutableListOf<Pair<Product, Boolean>>()
    val seen = mutableSetOf<Int>()

    pinnedIds.forEach { id ->
        val p = byId[id]
        if (p != null && chosen.size < limit && seen.add(p.id)) {
            chosen.add(p to true)
        }
    }

    return chosen.take(limit).map { (product, isAdminCurated) ->
        val pct = product.discountPercent
        val rupees = product.pricePaise / 100L
        val headline = when {
            pct >= 45 -> "Up to ${pct}% Off"
            rupees <= 999L -> "Just ₹$rupees"
            else -> "Shop now"
        }
        val shortCat = product.category.substringBefore('/').trim().ifBlank { product.category }
        BrandSpotlightEntry(
            product = product,
            headline = headline,
            caption = shortCat,
            isAdminCurated = isAdminCurated
        )
    }
}

fun selectTopValueDeals(
    catalog: List<Product>,
    limit: Int = 3,
    pinnedIds: List<Int> = emptyList()
): List<MerchandisingProductPick> {
    val byId = catalog.associateBy { it.id }
    val picked = mutableListOf<MerchandisingProductPick>()
    val seen = mutableSetOf<Int>()

    pinnedIds.forEach { id ->
        val p = byId[id]
        if (p != null && picked.size < limit && seen.add(p.id)) {
            picked.add(MerchandisingProductPick(product = p, isAdminCurated = true))
        }
    }

    return picked.take(limit)
}

fun selectSuggestedForYou(
    catalog: List<Product>,
    recentlyViewedIds: List<Int>,
    wishlistIds: Set<Int>,
    cartProductIds: List<Int>,
    limit: Int = 3,
    pinnedIds: List<Int> = emptyList()
): List<MerchandisingProductPick> {
    val byId = catalog.associateBy { it.id }
    val picked = mutableListOf<MerchandisingProductPick>()
    val seen = mutableSetOf<Int>()

    pinnedIds.forEach { id ->
        val p = byId[id]
        if (p != null && picked.size < limit && seen.add(p.id)) {
            picked.add(MerchandisingProductPick(product = p, isAdminCurated = true))
        }
    }

    return picked.take(limit)
}

fun selectMoreProducts(
    catalog: List<Product>,
    limit: Int = 8,
    pinnedIds: List<Int> = emptyList()
): List<MerchandisingProductPick> {
    val byId = catalog.associateBy { it.id }
    val picked = mutableListOf<MerchandisingProductPick>()
    val seen = mutableSetOf<Int>()

    pinnedIds.forEach { id ->
        val p = byId[id]
        if (p != null && picked.size < limit && seen.add(p.id)) {
            picked.add(MerchandisingProductPick(product = p, isAdminCurated = true))
        }
    }

    return picked.take(limit)
}
