package com.aarvo

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.aarvo.data.Product
import com.aarvo.network.AarvoApiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

fun matchesCategoryFilter(productCat: String, selectedCat: String): Boolean {
    if (selectedCat.isBlank() || selectedCat.equals("All", true) || selectedCat.equals("More", true)) return true
    val pCat = productCat.lowercase()
    val sCat = selectedCat.lowercase()
    return when {
        sCat.contains("home") -> pCat.contains("home") || pCat.contains("kitchen") || pCat.contains("living")
        sCat.contains("beauty") -> pCat.contains("beauty") || pCat.contains("personal") || pCat.contains("mouth") || pCat.contains("care")
        sCat.contains("sport") || sCat.contains("footwear") -> pCat.contains("sport") || pCat.contains("footwear") || pCat.contains("shoe")
        sCat.contains("toy") || sCat.contains("kid") || sCat.contains("baby") -> pCat.contains("toy") || pCat.contains("baby") || pCat.contains("kid")
        else -> pCat.startsWith(sCat.take(4)) || pCat.contains(sCat)
    }
}

fun countKeywordOccurrences(text: String, keyword: String): Int {
    if (text.isEmpty() || keyword.isEmpty()) return 0
    var count = 0
    var index = 0
    while (index <= text.length - keyword.length) {
        val foundAt = text.indexOf(keyword, index)
        if (foundAt == -1) break
        count += 1
        index = foundAt + keyword.length
    }
    return count
}

private fun hasWholeWordMatch(text: String, keyword: String): Boolean {
    if (text.isEmpty() || keyword.isEmpty()) return false
    val pattern = Regex("(^|[^a-z0-9])${Regex.escape(keyword)}([^a-z0-9]|$)", RegexOption.IGNORE_CASE)
    return pattern.containsMatchIn(text)
}

fun computeKeywordRelevanceScore(product: Product, query: String): Double {
    val fullQuery = query.trim().lowercase()
    if (fullQuery.isEmpty()) return 0.0
    val tokens = fullQuery.split(Regex("\\s+")).filter { it.isNotBlank() }.distinct()
    if (tokens.isEmpty()) return 0.0

    val name = product.name.lowercase()
    val category = product.category.lowercase()
    val seller = product.sellerName.lowercase()
    val description = product.description.lowercase()

    var score = 0.0

    if (name == fullQuery) score += 60.0
    else if (name.startsWith(fullQuery)) score += 30.0
    else if (name.contains(fullQuery)) score += 20.0

    if (category.contains(fullQuery)) score += 12.0
    if (description.contains(fullQuery)) score += 8.0

    var distinctTokenHits = 0
    for (token in tokens) {
        val nameFreq = countKeywordOccurrences(name, token)
        val catFreq = countKeywordOccurrences(category, token)
        val sellerFreq = countKeywordOccurrences(seller, token)
        val descFreq = countKeywordOccurrences(description, token).coerceAtMost(12)

        if (nameFreq > 0 || catFreq > 0 || sellerFreq > 0 || descFreq > 0) {
            distinctTokenHits += 1
        }

        score += nameFreq * 10.0
        score += catFreq * 6.0
        score += sellerFreq * 4.0
        score += descFreq * 3.0

        if (hasWholeWordMatch(name, token)) score += 5.0
        if (hasWholeWordMatch(category, token)) score += 3.0
    }

    score += distinctTokenHits * 15.0
    if (distinctTokenHits == tokens.size) score += 20.0
    score += product.rating * 0.5

    return score
}

fun sortProductsByMode(products: List<Product>, sortMode: String, query: String): List<Product> {
    return when (sortMode) {
        "Price: Low to High" -> products.sortedBy { it.pricePaise }
        "Price: High to Low" -> products.sortedByDescending { it.pricePaise }
        "Rating: High to Low" -> products.sortedWith(
            compareByDescending<Product> { it.rating }.thenByDescending { it.reviewsCount }
        )
        "Discount" -> products.sortedByDescending { it.discountPercent }
        else -> {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                products
            } else {
                products.sortedWith(
                    compareByDescending<Product> { computeKeywordRelevanceScore(it, trimmed) }
                        .thenByDescending { it.rating }
                        .thenByDescending { it.reviewsCount }
                )
            }
        }
    }
}

@Composable
fun PreviewHomeScreen(
    padding: PaddingValues,
    api: AarvoApiClient,
    query: String,
    onQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    products: List<Product>,
    allProducts: List<Product>,
    recentlyViewed: List<Product>,
    loading: Boolean,
    error: String,
    onAdd: (Product) -> Unit,
    onOpen: (Product) -> Unit,
    wishlist: Set<Int>,
    onToggleWishlist: (Int) -> Unit,
    onFilter: () -> Unit,
    sortMode: String,
    onSortChange: (String) -> Unit = {},
    minRating: Double,
    minPrice: Long? = null,
    maxPrice: Long?,
    onPriceRangeChange: (Long?, Long?) -> Unit = { _, _ -> },
    inStockOnly: Boolean,
    onOpenSeller: () -> Unit,
    onOpenOffers: () -> Unit,
    onOpenCategories: () -> Unit,
    onShowToast: (String, String) -> Unit = { _, _ -> },
    isDarkTheme: Boolean = false
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val bgCol = if (isDarkTheme) Color(0xFF120E0C) else Color(0xFFFFFDF9)
    val cardBg = if (isDarkTheme) Color(0xFF211A17) else Color.White
    val cardBorder = if (isDarkTheme) Color(0xFF332620) else Color(0xFFFFE8D6)
    val textColor = if (isDarkTheme) Color(0xFFFAF3EE) else Color(0xFF0F172A)
    val mutedColor = if (isDarkTheme) Color(0xFFCBBDB4) else Color(0xFF64748B)

    // Precompute query-matched products for Search Results "Refine by Category" counts
    val queryMatched = remember(allProducts, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            allProducts
        } else {
            val tokens = q.split(Regex("\\s+")).filter { it.isNotBlank() }.distinct()
            val allTokensMatched = allProducts.filter { p ->
                val hay = "${p.name} ${p.category} ${p.sellerName} ${p.description}".lowercase()
                tokens.all { hay.contains(it) }
            }
            if (allTokensMatched.isNotEmpty() || tokens.size <= 1) {
                allTokensMatched
            } else {
                allProducts.filter { p ->
                    val hay = "${p.name} ${p.category} ${p.sellerName} ${p.description}".lowercase()
                    tokens.any { hay.contains(it) }
                }
            }
        }
    }

    val searchProducts = remember(queryMatched, products, selectedCategory, query, sortMode, minPrice, maxPrice) {
        val base = if (query.isBlank()) products
        else if (selectedCategory.equals("All", true)) queryMatched
        else queryMatched.filter { matchesCategoryFilter(it.category, selectedCategory) }
        val priceFiltered = base.filter { p ->
            (minPrice == null || p.pricePaise >= minPrice) &&
                (maxPrice == null || p.pricePaise <= maxPrice)
        }
        sortProductsByMode(priceFiltered, sortMode, query)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bgCol)
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (query.isNotBlank()) {
            // =================================================================
            // REAL-TIME SEARCH RESULTS VIEW (Matches MainScreen.tsx 100%)
            // =================================================================
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDarkTheme) Color(0xFF211A17) else Color(0xFFFFF4E8),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF4A2B1D) else Color(0xFFFED7AA))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, null, tint = Color(0xFFFF5722), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Search Results", fontSize = 14.sp, fontWeight = FontWeight.Black, color = textColor)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (loading) "Searching products for \"$query\"..."
                                else "${searchProducts.size} ${if (searchProducts.size == 1) "product" else "products"} found for \"$query\"",
                                fontSize = 11.sp,
                                color = mutedColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (isDarkTheme) Color(0xFF3F1D24) else Color(0xFFFFE4E6),
                            modifier = Modifier.clickable {
                                triggerHaptic(haptics)
                                onQueryChange("")
                                onCategoryChange("All")
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color(0xFFE11D48), modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                            }
                        }
                    }
                }
            }

            // Horizontal "Refine by Category" Chips with live match counts
            item {
                val refineCats = listOf(
                    "All", "Electronics", "Fashion", "Mobiles",
                    "Home & Kitchen", "Grocery", "Beauty & Care",
                    "Appliances", "Sports", "Toys & Kids"
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedCategory != "All") "REFINE BY CATEGORY • $selectedCategory" else "REFINE BY CATEGORY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = mutedColor,
                            letterSpacing = 0.5.sp
                        )
                        if (selectedCategory != "All") {
                            Text(
                                "Show All ✕",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5722),
                                modifier = Modifier.clickable { onCategoryChange("All") }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        refineCats.forEach { chip ->
                            val count = if (chip == "All") queryMatched.size else queryMatched.count { matchesCategoryFilter(it.category, chip) }
                            val isSel = selectedCategory.equals(chip, true)
                            Surface(
                                shape = CircleShape,
                                color = if (isSel) Color(0xFFFF5722) else cardBg,
                                border = if (isSel) null else BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.clickable {
                                    triggerHaptic(haptics)
                                    onCategoryChange(chip)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        chip,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Black else FontWeight.Bold,
                                        color = if (isSel) Color.White else textColor
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSel) Color.White.copy(alpha = 0.25f) else if (isDarkTheme) Color(0xFF2D221D) else Color(0xFFFFF4E8)
                                    ) {
                                        Text(
                                            "$count",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else mutedColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Inline Sort Selector Row ("Relevance" uses keyword frequency matching)
            item {
                val sortOptions = listOf("Relevance", "Price: Low to High", "Price: High to Low", "Rating: High to Low", "Discount")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Sort:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = mutedColor,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                    sortOptions.forEach { option ->
                        val active = sortMode == option
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) Color(0xFF0F172A) else cardBg,
                            border = if (active) null else BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.clickable {
                                triggerHaptic(haptics)
                                onSortChange(option)
                            }
                        ) {
                            Text(
                                text = option,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) Color.White else textColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            item {
                PriceRangeChipBar(
                    minPricePaise = minPrice,
                    maxPricePaise = maxPrice,
                    onPriceRangeChange = { mn, mx, label ->
                        onPriceRangeChange(mn, mx)
                        onShowToast(
                            if (mn == null && mx == null) "Price filter cleared" else "Price filter: $label",
                            "INFO"
                        )
                    },
                    onOpenFullFilter = onFilter,
                    isDarkTheme = isDarkTheme
                )
            }

            if (loading) {
                items(3) {
                    ProductSkeletonGridRow(isDarkTheme = isDarkTheme)
                }
            } else if (searchProducts.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = if (isDarkTheme) Color(0xFF211A17) else Color(0xFFFFF4E8),
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Search, null, tint = Color(0xFFFF5722), modifier = Modifier.size(34.dp))
                            Text(
                                if (selectedCategory != "All") "No $selectedCategory products found" else "No matching products found",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = textColor
                            )
                            Text(
                                "Try another keyword or tap a popular suggestion below.",
                                fontSize = 11.sp,
                                color = mutedColor,
                                textAlign = TextAlign.Center
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Shoes", "Phone", "Kurta", "Neem", "Earbuds", "Blender").forEach { tag ->
                                    Surface(
                                        shape = CircleShape,
                                        color = cardBg,
                                        border = BorderStroke(1.dp, cardBorder),
                                        modifier = Modifier.clickable {
                                            triggerHaptic(haptics)
                                            onQueryChange(tag)
                                            onCategoryChange("All")
                                        }
                                    ) {
                                        Text(
                                            tag,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF5722),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = { onQueryChange(""); onCategoryChange("All") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Clear Search", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                items(searchProducts.chunked(2)) { rowPair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowPair.forEach { product ->
                            Box(Modifier.weight(1f)) {
                                PreviewProductCard(
                                    product = product,
                                    isSaved = product.id in wishlist,
                                    onAdd = { p ->
                                        triggerHaptic(haptics, strong = true)
                                        onAdd(p)
                                        Toast.makeText(context, "${p.name} added to Cart", Toast.LENGTH_SHORT).show()
                                    },
                                    onOpen = {
                                        triggerHaptic(haptics)
                                        scope.launch {
                                            runCatching { api.markProductViewed(product.id) }
                                            onOpen(product)
                                        }
                                    },
                                    onToggleWishlist = { id ->
                                        triggerHaptic(haptics)
                                        onToggleWishlist(id)
                                    },
                                    isDarkTheme = isDarkTheme
                                )
                            }
                        }
                        if (rowPair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        } else {
            // =================================================================
            // HOME DISCOVERY FEED (100% Parity with Screenshot & MainScreen)
            // =================================================================

            // 1. Live Hero Carousel (server slides + Big Savings + Verified Sellers + Smart Gadgets)
            item {
                LiveHero(
                    api = api,
                    modifier = Modifier.fillMaxWidth(),
                    onCtaClick = { target ->
                        triggerHaptic(haptics)
                        when {
                            target.startsWith("categories:") -> {
                                onCategoryChange(target.removePrefix("categories:"))
                                onOpenCategories()
                            }
                            target == "offers" -> onOpenOffers()
                            else -> onOpenCategories()
                        }
                    }
                )
            }

            // 2. Shop by Category (5 Real Square Image Tiles matching Screenshot)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            GridVectorIcon(tint = Color(0xFFFF5722), modifier = Modifier.size(16.dp))
                            Text(
                                "Shop by Category",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = textColor
                            )
                        }
                        Text(
                            "See All >",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5722),
                            modifier = Modifier.clickable {
                                triggerHaptic(haptics)
                                onCategoryChange("All")
                                onOpenCategories()
                            }
                        )
                    }

                    val shopByCats = listOf(
                        Triple("Fashion", "Fashion", R.drawable.cat_fashion_bag to if (isDarkTheme) Color(0xFF2D1920) else Color(0xFFFFF0F5)),
                        Triple("Mobiles", "Mobiles", R.drawable.cat_mobiles_phone to if (isDarkTheme) Color(0xFF162033) else Color(0xFFEEF4FF)),
                        Triple("Electronics", "Electronics", R.drawable.cat_electronics_laptop to if (isDarkTheme) Color(0xFF1A1D33) else Color(0xFFF0F2FF)),
                        Triple("Home & Kitchen", "Home & Living", R.drawable.cat_home_blender to if (isDarkTheme) Color(0xFF2D2114) else Color(0xFFFFF6EB)),
                        Triple("Beauty & Personal Care", "Beauty & Care", R.drawable.cat_beauty_serum to if (isDarkTheme) Color(0xFF2E1B14) else Color(0xFFFFF1EA))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        shopByCats.forEach { (label, filterKey, imgAndBg) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        triggerHaptic(haptics)
                                        onCategoryChange(filterKey)
                                        onOpenCategories()
                                    }
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = imgAndBg.second,
                                    border = BorderStroke(1.dp, cardBorder)
                                ) {
                                    Image(
                                        painter = painterResource(id = imgAndBg.first),
                                        contentDescription = label,
                                        modifier = Modifier.fillMaxSize().padding(3.dp).clip(RoundedCornerShape(11.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 3. "Great Deals On Top Brands" Promo Banner with promo_gadgets_trio
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(114.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFDE047), Color(0xFFFBBF24), Color(0xFFF59E0B))
                            )
                        )
                        .clickable {
                            triggerHaptic(haptics)
                            onOpenOffers()
                        }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.promo_gadgets_trio),
                        contentDescription = "Top Brands Deals",
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.50f)
                            .align(Alignment.CenterEnd),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.62f)
                            .align(Alignment.CenterStart)
                            .background(
                                Brush.horizontalGradient(
                                    0.0f to Color(0xFFFDE047),
                                    0.72f to Color(0xFFFBBF24).copy(alpha = 0.88f),
                                    1.0f to Color.Transparent
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth(0.58f)
                            .padding(start = 16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Great Deals",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                            lineHeight = 19.sp
                        )
                        Text(
                            "On Top Brands",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                            lineHeight = 19.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0F172A),
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                "Shop Now >",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // 4. Recently Viewed Strip (when populated)
            if (recentlyViewed.isNotEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            "Recently Viewed",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(recentlyViewed, key = { it.id }) { product ->
                                Surface(
                                    modifier = Modifier
                                        .size(width = 160.dp, height = 124.dp)
                                        .clickable {
                                            triggerHaptic(haptics)
                                            onOpen(product)
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = cardBg,
                                    border = BorderStroke(1.dp, cardBorder)
                                ) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                        Text(product.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Column {
                                            Text(product.displayPrice, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF5722))
                                            Text("View again ›", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = mutedColor)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (loading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥 Top Deals", fontSize = 15.sp, fontWeight = FontWeight.Black, color = textColor)
                        Text(
                            "See All >",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5722),
                            modifier = Modifier.clickable { onOpenCategories() }
                        )
                    }
                }
                item {
                    ProductSkeletonGridRow(isDarkTheme = isDarkTheme)
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (selectedCategory == "All") "More products for you" else "$selectedCategory Department",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor
                        )
                        Text(
                            "View All >",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5722),
                            modifier = Modifier.clickable { onOpenCategories() }
                        )
                    }
                }
                items(2) {
                    ProductSkeletonGridRow(isDarkTheme = isDarkTheme)
                }
            }

            if (error.isNotBlank()) {
                item { Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }

            // 5. Top Deals Section (First 2 products in 2-col layout) + Inline Price Range Bar
            if (!loading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (minPrice != null || maxPrice != null) "🔥 Top Deals (${products.size})" else "🔥 Top Deals",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (minPrice != null || maxPrice != null) Color(0xFFFF5722) else if (isDarkTheme) Color(0xFF211A17) else Color(0xFFFFF4E8),
                                border = BorderStroke(1.dp, if (minPrice != null || maxPrice != null) Color(0xFFFF5722) else cardBorder),
                                modifier = Modifier.clickable {
                                    triggerHaptic(haptics)
                                    onFilter()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    FilterSlidersVectorIcon(
                                        tint = if (minPrice != null || maxPrice != null) Color.White else Color(0xFFFF5722),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        "Price",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (minPrice != null || maxPrice != null) Color.White else Color(0xFFFF5722)
                                    )
                                }
                            }
                            Text(
                                "See All >",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5722),
                                modifier = Modifier.clickable { onOpenCategories() }
                            )
                        }
                    }
                }

                item {
                    PriceRangeChipBar(
                        minPricePaise = minPrice,
                        maxPricePaise = maxPrice,
                        onPriceRangeChange = { mn, mx, label ->
                            onPriceRangeChange(mn, mx)
                            onShowToast(
                                if (mn == null && mx == null) "Price filter cleared" else "Price filter: $label",
                                "INFO"
                            )
                        },
                        onOpenFullFilter = onFilter,
                        isDarkTheme = isDarkTheme
                    )
                }

                if (products.isEmpty() && error.isBlank()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDarkTheme) Color(0xFF211A17) else Color(0xFFFFF4E8),
                            border = BorderStroke(1.dp, cardBorder)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("No products in this price range", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
                                Button(
                                    onClick = {
                                        onPriceRangeChange(null, null)
                                        onShowToast("Price range filter cleared", "INFO")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Reset Price Filter", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (!loading && products.isNotEmpty()) {
                val topDeals = products.take(2)

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        topDeals.forEach { product ->
                            Box(Modifier.weight(1f)) {
                                PreviewProductCard(
                                    product = product,
                                    isSaved = product.id in wishlist,
                                    onAdd = { p ->
                                        triggerHaptic(haptics, strong = true)
                                        onAdd(p)
                                        Toast.makeText(context, "${p.name} added to Cart", Toast.LENGTH_SHORT).show()
                                    },
                                    onOpen = {
                                        triggerHaptic(haptics)
                                        scope.launch {
                                            runCatching { api.markProductViewed(product.id) }
                                            onOpen(product)
                                        }
                                    },
                                    onToggleWishlist = { id ->
                                        triggerHaptic(haptics)
                                        onToggleWishlist(id)
                                    },
                                    isDarkTheme = isDarkTheme
                                )
                            }
                        }
                        if (topDeals.size == 1) Spacer(Modifier.weight(1f))
                    }
                }

                // 6. "More products for you" Section
                val moreProducts = if (products.size > 2) products.drop(2) else products
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (selectedCategory == "All") "More products for you" else "$selectedCategory Department",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor
                        )
                        Text(
                            "View All >",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5722),
                            modifier = Modifier.clickable { onOpenCategories() }
                        )
                    }
                }

                items(moreProducts.chunked(2)) { rowPair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowPair.forEach { product ->
                            Box(Modifier.weight(1f)) {
                                PreviewProductCard(
                                    product = product,
                                    isSaved = product.id in wishlist,
                                    onAdd = { p ->
                                        triggerHaptic(haptics, strong = true)
                                        onAdd(p)
                                        Toast.makeText(context, "${p.name} added to Cart", Toast.LENGTH_SHORT).show()
                                    },
                                    onOpen = {
                                        triggerHaptic(haptics)
                                        scope.launch {
                                            runCatching { api.markProductViewed(product.id) }
                                            onOpen(product)
                                        }
                                    },
                                    onToggleWishlist = { id ->
                                        triggerHaptic(haptics)
                                        onToggleWishlist(id)
                                    },
                                    isDarkTheme = isDarkTheme
                                )
                            }
                        }
                        if (rowPair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }

                // 7. Floating "↓ More below" pill button
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                        Surface(
                            shape = CircleShape,
                            color = cardBg,
                            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF4A3B34) else Color(0xFFFED7AA)),
                            shadowElevation = 3.dp,
                            modifier = Modifier.clickable {
                                triggerHaptic(haptics)
                                onOpenCategories()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("↓", color = Color(0xFFFF5722), fontWeight = FontWeight.Black, fontSize = 12.sp)
                                Text("More below", color = textColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberProductSkeletonBrush(isDarkTheme: Boolean = false): Brush {
    val transition = rememberInfiniteTransition(label = "productSkeleton")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "productSkeletonOffset"
    )
    val baseColor = if (isDarkTheme) Color(0xFF2B221E) else Color(0xFFFDEEE2)
    val highlightColor = if (isDarkTheme) Color(0xFF3A2E29) else Color(0xFFFFF8F2)
    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim - 260f, 0f),
        end = Offset(translateAnim, 260f)
    )
}

@Composable
fun ProductSkeletonGridRow(
    isDarkTheme: Boolean = false,
    horizontalSpacingDp: Int = 10
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacingDp.dp)
    ) {
        Box(Modifier.weight(1f)) {
            ProductCardSkeleton(isDarkTheme = isDarkTheme)
        }
        Box(Modifier.weight(1f)) {
            ProductCardSkeleton(isDarkTheme = isDarkTheme)
        }
    }
}

@Composable
fun ProductCardSkeleton(isDarkTheme: Boolean = false) {
    val cardBg = if (isDarkTheme) Color(0xFF211A17) else Color.White
    val cardBorder = if (isDarkTheme) Color(0xFF332620) else Color(0xFFFFE8D6)
    val shimmerBrush = rememberProductSkeletonBrush(isDarkTheme = isDarkTheme)
    val subtlePillColor = if (isDarkTheme) Color(0xFF2E2420) else Color(0xFFF6E7DA)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(9.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(142.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmerBrush)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .width(46.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(subtlePillColor)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isDarkTheme) Color(0xFF2E2420) else Color.White.copy(alpha = 0.85f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .width(40.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(subtlePillColor)
                )
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(shimmerBrush)
            )
            Spacer(Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(shimmerBrush)
            )

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(15.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(shimmerBrush)
                )
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(subtlePillColor)
                )
            }

            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(cardBorder))
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(66.dp)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(subtlePillColor)
                )
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimmerBrush)
                )
            }
        }
    }
}

@Composable
fun PreviewProductCard(
    product: Product,
    isSaved: Boolean,
    onAdd: (Product) -> Unit,
    onOpen: () -> Unit,
    onToggleWishlist: (Int) -> Unit,
    isDarkTheme: Boolean = false
) {
    val cardBg = if (isDarkTheme) Color(0xFF211A17) else Color.White
    val cardBorder = if (isDarkTheme) Color(0xFF332620) else Color(0xFFFFE8D6)
    val textColor = if (isDarkTheme) Color(0xFFFAF3EE) else Color(0xFF1E293B)
    val imgBg = if (isDarkTheme) Color(0xFF181311) else Color(0xFFFFF9F2)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(9.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(142.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(imgBg),
                contentAlignment = Alignment.Center
            ) {
                if (!product.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(product.emoji.ifBlank { "🛍️" }, fontSize = 42.sp)
                }

                // Discount Pill - ONLY when genuine discount exists
                if (product.hasDiscount && product.discountPercent > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF059669),
                        modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                    ) {
                        Text(
                            "${product.discountPercent}% OFF",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Genuine Rating Pill Bottom-Left (ONLY if rating > 0)
                if (product.rating > 0.0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF059669),
                        modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                    ) {
                        Text(
                            text = "★ %.1f".format(product.rating),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                // Wishlist Heart Top-Right
                Surface(
                    onClick = { onToggleWishlist(product.id) },
                    shape = CircleShape,
                    color = if (isSaved) Color(0xFFFFF1F2) else Color.White.copy(alpha = 0.90f),
                    shadowElevation = 2.dp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Wishlist",
                            tint = if (isSaved) Color(0xFFEF4444) else Color(0xFF94A3B8),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = product.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp,
                modifier = Modifier.heightIn(min = 30.dp)
            )

            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "₹${product.pricePaise / 100}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor
                )
                if (product.hasDiscount) {
                    Text(
                        text = "₹${product.originalPricePaise / 100}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Footer matching Preview: Free Delivery + Add to Cart button
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(cardBorder))
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (product.deliveryFeePaise <= 0L) "Free Delivery" else "Delivery ₹${product.deliveryFeePaise / 100}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (product.deliveryFeePaise <= 0L) Color(0xFF059669) else Color(0xFF64748B)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkTheme) Color(0xFF2D221D) else Color(0xFFFFF4E8),
                    modifier = Modifier.clickable { onAdd(product) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, null, tint = Color(0xFFFF5722), modifier = Modifier.size(13.dp))
                        Text("Add", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF5722))
                    }
                }
            }
        }
    }
}

private data class PricePreset(val label: String, val minPaise: Long?, val maxPaise: Long?)

@Composable
fun PriceRangeChipBar(
    minPricePaise: Long?,
    maxPricePaise: Long?,
    onPriceRangeChange: (Long?, Long?, String) -> Unit,
    onOpenFullFilter: () -> Unit,
    isDarkTheme: Boolean = false
) {
    val haptics = LocalHapticFeedback.current
    val presets = remember {
        listOf(
            PricePreset("All Prices", null, null),
            PricePreset("Under ₹500", null, 50_000L),
            PricePreset("₹500 – ₹2K", 50_000L, 200_000L),
            PricePreset("₹2K – ₹10K", 200_000L, 1_000_000L),
            PricePreset("₹10,000+", 1_000_000L, null)
        )
    }
    val hasActiveRange = minPricePaise != null || maxPricePaise != null
    val cardBg = if (isDarkTheme) Color(0xFF211A17) else Color.White
    val cardBorder = if (isDarkTheme) Color(0xFF332620) else Color(0xFFFFE8D6)
    val textColor = if (isDarkTheme) Color(0xFFFAF3EE) else Color(0xFF1E293B)

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (hasActiveRange) Color(0xFFFF5722) else if (isDarkTheme) Color(0xFF2B1A12) else Color(0xFFFFF4E8),
            border = if (hasActiveRange) null else BorderStroke(1.dp, cardBorder),
            modifier = Modifier.clickable {
                triggerHaptic(haptics)
                onOpenFullFilter()
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterSlidersVectorIcon(
                    tint = if (hasActiveRange) Color.White else Color(0xFFFF5722),
                    modifier = Modifier.size(12.dp)
                )
                val customLabel = when {
                    minPricePaise != null && maxPricePaise != null -> "₹${minPricePaise / 100}–₹${maxPricePaise / 100}"
                    maxPricePaise != null -> "≤₹${maxPricePaise / 100}"
                    minPricePaise != null -> "₹${minPricePaise / 100}+"
                    else -> "Price Range"
                }
                Text(
                    text = customLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasActiveRange) Color.White else Color(0xFFE64A19),
                    maxLines = 1
                )
            }
        }
        presets.forEach { p ->
            val selected = minPricePaise == p.minPaise && maxPricePaise == p.maxPaise
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selected) Color(0xFF0F172A) else cardBg,
                border = if (selected) null else BorderStroke(1.dp, cardBorder),
                modifier = Modifier.clickable {
                    triggerHaptic(haptics)
                    onPriceRangeChange(p.minPaise, p.maxPaise, p.label)
                }
            ) {
                Text(
                    text = p.label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else textColor,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun PreviewCategoriesExplorerScreen(
    padding: PaddingValues,
    allProducts: List<Product>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    wishlist: Set<Int>,
    onToggleWishlist: (Int) -> Unit,
    onAdd: (Product) -> Unit,
    onOpenProduct: (Product) -> Unit,
    loading: Boolean = false,
    sortMode: String = "Relevance",
    onSortChange: (String) -> Unit = {},
    minPrice: Long? = null,
    maxPrice: Long? = null,
    onPriceRangeChange: (Long?, Long?) -> Unit = { _, _ -> },
    onOpenPriceFilter: () -> Unit = {},
    onShowToast: (String, String) -> Unit = { _, _ -> },
    isDarkTheme: Boolean = false
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val bgCol = if (isDarkTheme) Color(0xFF120E0C) else Color(0xFFFFFDF9)
    val railBg = if (isDarkTheme) Color(0xFF181311) else Color(0xFFFFF8F1)
    val activeBg = if (isDarkTheme) Color(0xFF2E1A12) else Color(0xFFFFF4E8)
    val textColor = if (isDarkTheme) Color(0xFFFAF3EE) else Color(0xFF1F1612)
    val mutedColor = if (isDarkTheme) Color(0xFFCBBDB4) else Color(0xFF64748B)
    val borderCol = if (isDarkTheme) Color(0xFF332620) else Color(0xFFFFE8D6)

    val departmentList = remember {
        listOf(
            "All" to "🛍️",
            "Mobiles" to "📱",
            "Fashion" to "👕",
            "Electronics" to "💻",
            "Home & Living" to "🛋️",
            "Appliances" to "📺",
            "Beauty & Care" to "✨",
            "Sports" to "👟",
            "Toys & Kids" to "🧸",
            "Grocery" to "🧺"
        )
    }

    val filteredProducts = remember(allProducts, selectedCategory, sortMode, minPrice, maxPrice) {
        val list = if (selectedCategory.equals("All", true)) allProducts
        else allProducts.filter { matchesCategoryFilter(it.category, selectedCategory) }
        val priceFiltered = list.filter { p ->
            (minPrice == null || p.pricePaise >= minPrice) &&
                (maxPrice == null || p.pricePaise <= maxPrice)
        }
        sortProductsByMode(priceFiltered, sortMode, "")
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(bgCol)
            .padding(padding)
    ) {
        // Left Vertical Rail (Flipkart 2-pane Style)
        Column(
            modifier = Modifier
                .width(88.dp)
                .fillMaxHeight()
                .background(railBg)
                .verticalScroll(rememberScrollState())
        ) {
            departmentList.forEach { (dept, emoji) ->
                val isSelected = selectedCategory.equals(dept, ignoreCase = true) ||
                    (dept == "Home & Living" && selectedCategory.equals("Home", ignoreCase = true)) ||
                    (dept == "Beauty & Care" && selectedCategory.equals("Beauty", ignoreCase = true))
                Surface(
                    onClick = {
                        triggerHaptic(haptics)
                        onSelectCategory(dept)
                    },
                    color = if (isSelected) activeBg else Color.Transparent,
                    border = if (isSelected) BorderStroke(1.dp, Color(0xFFFF5722).copy(alpha = 0.35f)) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color(0xFFFF5722)
                                    else if (isDarkTheme) Color(0xFF211A17)
                                    else Color(0xFFFFEDE0)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 18.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = dept,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                            color = if (isSelected) Color(0xFFFF5722) else textColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 12.sp,
                            maxLines = 2
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(borderCol))
            }
        }

        // Right Pane: Category Banner + Live 2-Column Products Grid
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = activeBg,
                    border = BorderStroke(1.dp, borderCol)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                if (selectedCategory.equals("All", true)) "ALL COLLECTIONS" else selectedCategory.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = textColor
                            )
                            Text(
                                if (loading && filteredProducts.isEmpty()) "Loading catalog..."
                                else "${filteredProducts.size} items available",
                                fontSize = 10.sp,
                                color = mutedColor
                            )
                        }
                        if (!selectedCategory.equals("All", true)) {
                            Text(
                                "Show All",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5722),
                                modifier = Modifier.clickable { onSelectCategory("All") }
                            )
                        }
                    }
                }
            }

            item {
                val catSortOptions = listOf("Relevance", "Price: Low to High", "Price: High to Low", "Rating: High to Low", "Discount")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    catSortOptions.forEach { option ->
                        val active = sortMode == option
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) Color(0xFF0F172A) else activeBg,
                            border = if (active) null else BorderStroke(1.dp, borderCol),
                            modifier = Modifier.clickable {
                                triggerHaptic(haptics)
                                onSortChange(option)
                            }
                        ) {
                            Text(
                                text = option,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) Color.White else textColor,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            item {
                PriceRangeChipBar(
                    minPricePaise = minPrice,
                    maxPricePaise = maxPrice,
                    onPriceRangeChange = { mn, mx, label ->
                        onPriceRangeChange(mn, mx)
                        onShowToast(
                            if (mn == null && mx == null) "Price filter cleared" else "Price filter: $label",
                            "INFO"
                        )
                    },
                    onOpenFullFilter = onOpenPriceFilter,
                    isDarkTheme = isDarkTheme
                )
            }

            if (loading && filteredProducts.isEmpty()) {
                items(3) {
                    ProductSkeletonGridRow(isDarkTheme = isDarkTheme, horizontalSpacingDp = 8)
                }
            } else if (filteredProducts.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = activeBg,
                        border = BorderStroke(1.dp, borderCol)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("No items in $selectedCategory", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
                            Button(
                                onClick = { onSelectCategory("All") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("View All Products", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(filteredProducts.chunked(2)) { rowPair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPair.forEach { product ->
                            Box(Modifier.weight(1f)) {
                                PreviewProductCard(
                                    product = product,
                                    isSaved = product.id in wishlist,
                                    onAdd = { p ->
                                        triggerHaptic(haptics, strong = true)
                                        onAdd(p)
                                        Toast.makeText(context, "${p.name} added to Cart", Toast.LENGTH_SHORT).show()
                                    },
                                    onOpen = {
                                        triggerHaptic(haptics)
                                        onOpenProduct(product)
                                    },
                                    onToggleWishlist = { id ->
                                        triggerHaptic(haptics)
                                        onToggleWishlist(id)
                                    },
                                    isDarkTheme = isDarkTheme
                                )
                            }
                        }
                        if (rowPair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun PreviewOffersScreen(
    padding: PaddingValues,
    api: AarvoApiClient,
    products: List<Product>,
    appliedCouponCode: String?,
    onApplyCoupon: (String, Long) -> Unit,
    onRemoveCoupon: () -> Unit,
    wishlist: Set<Int>,
    onToggleWishlist: (Int) -> Unit,
    onAdd: (Product) -> Unit,
    onOpenProduct: (Product) -> Unit,
    isDarkTheme: Boolean = false
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var offers by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var liveDeals by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var remainingSeconds by remember { mutableIntStateOf(4 * 3600 + 32 * 60 + 18) }

    LaunchedEffect(Unit) {
        loading = true
        offers = try {
            val res = api.offers()
            buildList { for (i in 0 until res.length()) add(res.getJSONObject(i)) }
        } catch (_: Throwable) { emptyList() }
        liveDeals = try {
            val res = api.productDeals()
            buildList { for (i in 0 until res.length()) add(res.getJSONObject(i)) }
        } catch (_: Throwable) { emptyList() }
        loading = false
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            remainingSeconds = if (remainingSeconds > 0) remainingSeconds - 1 else 4 * 3600
        }
    }

    val hrs = remainingSeconds / 3600
    val mins = (remainingSeconds % 3600) / 60
    val secs = remainingSeconds % 60
    val countdownStr = "%02d:%02d:%02d".format(hrs, mins, secs)

    val bgCol = if (isDarkTheme) Color(0xFF120E0C) else Color(0xFFFFFDF9)
    val cardBg = if (isDarkTheme) Color(0xFF211A17) else Color.White
    val textColor = if (isDarkTheme) Color(0xFFFAF3EE) else Color(0xFF1F1612)
    val borderCol = if (isDarkTheme) Color(0xFF332620) else Color(0xFFFFE8D6)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bgCol)
            .padding(padding),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Live Flash Lightning Sale Countdown Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFF7043), Color(0xFFFF5722), Color(0xFFE64A19))
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.20f)
                        ) {
                            Text(
                                "🔥 FLASH LIGHTNING SALE",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.25f)
                        ) {
                            Text(
                                "⏱ $countdownStr",
                                color = Color(0xFFFEF08A),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text("Up to 75% OFF Clearance", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("Extra instant savings on eligible UPI & verified seller drops", fontSize = 11.sp, color = Color(0xFFFFE5CC))
                }
            }
        }

        // 2. Coupons Section (Connected to api.offers + fallback)
        item {
            val couponList = if (offers.isNotEmpty()) offers else listOf(
                JSONObject().put("code", "AARVO200").put("title", "Save flat ₹200 on orders above ₹999").put("discount_value", 20000L),
                JSONObject().put("code", "WELCOME100").put("title", "Save flat ₹100 on orders above ₹499").put("discount_value", 10000L)
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderCol)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TagVectorIcon(tint = Color(0xFFFF5722), modifier = Modifier.size(16.dp))
                        Text("Available Coupons", fontSize = 13.sp, fontWeight = FontWeight.Black, color = textColor)
                    }
                    couponList.forEach { offer ->
                        val code = offer.optString("code", offer.optString("promo_code")).uppercase()
                        val title = offer.optString("title", offer.optString("description", "Save on your order"))
                        val defaultPaise = offer.optLong("discount_value", 10000L)
                        val isApplied = appliedCouponCode.equals(code, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkTheme) Color(0xFF2B1A12) else Color(0xFFFFF4E8),
                            border = BorderStroke(1.dp, if (isApplied) Color(0xFF059669) else borderCol)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = cardBg,
                                        border = BorderStroke(1.dp, borderCol)
                                    ) {
                                        Text(
                                            code,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = Color(0xFFFF5722),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(title, fontSize = 10.sp, color = if (isDarkTheme) Color(0xFFCBBDB4) else Color(0xFF475569))
                                }
                                Button(
                                    onClick = {
                                        if (isApplied) {
                                            triggerHaptic(haptics)
                                            onRemoveCoupon()
                                            Toast.makeText(context, "Coupon $code removed", Toast.LENGTH_SHORT).show()
                                        } else {
                                            scope.launch {
                                                val discount = runCatching {
                                                    api.validateOffer(code, 100000L).optLong("discountPaise", defaultPaise)
                                                }.getOrDefault(defaultPaise)
                                                triggerHaptic(haptics, strong = true)
                                                onApplyCoupon(code, discount)
                                                Toast.makeText(context, "Coupon $code applied to Cart!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isApplied) Color(0xFF059669) else Color(0xFFFF5722)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(if (isApplied) "Applied ✓" else "Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (loading) {
            item {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = Color(0xFFFF5722))
                }
            }
        }

        // Live Backend Product Deals Banner if present
        if (liveDeals.isNotEmpty()) {
            item {
                Text("Live Seller Deals (${liveDeals.size})", fontSize = 14.sp, fontWeight = FontWeight.Black, color = textColor)
            }
        }

        // 3. Today's Super Steals 2-Column Grid
        item {
            Text("Today's Super Steals", fontSize = 15.sp, fontWeight = FontWeight.Black, color = textColor)
        }

        items(products.take(6).chunked(2)) { rowPair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowPair.forEach { product ->
                    Box(Modifier.weight(1f)) {
                        PreviewProductCard(
                            product = product,
                            isSaved = product.id in wishlist,
                            onAdd = { p ->
                                triggerHaptic(haptics, strong = true)
                                onAdd(p)
                                Toast.makeText(context, "${p.name} added to Cart", Toast.LENGTH_SHORT).show()
                            },
                            onOpen = {
                                triggerHaptic(haptics)
                                onOpenProduct(product)
                            },
                            onToggleWishlist = { id ->
                                triggerHaptic(haptics)
                                onToggleWishlist(id)
                            },
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
                if (rowPair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
