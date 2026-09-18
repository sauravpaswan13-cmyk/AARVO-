from pathlib import Path
import re

p = Path('app/src/main/java/com/aarvo/MainActivity.kt')
s = p.read_text(encoding='utf-8')

old_call = '!onboarded -> OnboardingScreen { prefs.edit().putBoolean("onboarded", true).apply(); onboarded = true }'
new_call = '!onboarded -> OnboardingScreen(onDone = { prefs.edit().putBoolean("onboarded", true).apply(); onboarded = true }, onGuest = { prefs.edit().putBoolean("onboarded", true).putBoolean("guest_mode", true).apply(); onboarded = true; guestMode = true })'
s = s.replace(old_call, new_call)

old_fn = '''@Composable private fun OnboardingScreen(onDone: () -> Unit) { Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) { Text("AARVO", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)); Text("Shop smart. Live better.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text("A real marketplace for buyers and sellers, with server-authoritative products, orders and payments."); Spacer(Modifier.height(24.dp)); Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Get started") } } }'''
new_fn = '''@Composable private fun OnboardingScreen(onDone: () -> Unit, onGuest: () -> Unit) { Box(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp)) { Column(Modifier.fillMaxWidth().align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) { Text("AARVO", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)); Text("Shop smart. Live better.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)); Text("Browse products freely or continue with your AARVO account.", textAlign = androidx.compose.ui.text.style.TextAlign.Center); Spacer(Modifier.height(30.dp)); Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Login / Continue") }; Spacer(Modifier.height(14.dp)); androidx.compose.material3.OutlinedButton(onClick = onGuest, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Continue as Guest") } } } }'''
s = s.replace(old_fn, new_fn)

imports = [
    'import androidx.compose.foundation.background',
    'import androidx.compose.foundation.clickable',
    'import androidx.compose.foundation.shape.CircleShape',
    'import androidx.compose.foundation.shape.RoundedCornerShape',
    'import androidx.compose.ui.Alignment',
    'import androidx.compose.ui.draw.clip',
    'import androidx.compose.ui.graphics.Brush',
    'import androidx.compose.ui.unit.sp'
]
anchor = 'import androidx.compose.foundation.layout.Arrangement'
for imp in reversed(imports):
    if imp not in s:
        s = s.replace(anchor, imp + '\\n' + anchor, 1)

marker = 'item { PremiumHomeHeader() }; item { LiveHero(api = AarvoApiClient(), modifier = Modifier.fillMaxWidth()) };'
replacement = 'item { HomeSearchFirst(query, onQueryChange) }; item { PremiumHomeHeader() }; item { LiveHero(api = AarvoApiClient(), modifier = Modifier.fillMaxWidth()) };'
s = s.replace(marker, replacement, 1)

pattern = r'@Composable private fun HomeScreen\\(.*?\\n\\n@Composable private fun ProductCard'
match = re.search(pattern, s, re.S)
if match:
    home = '''@Composable private fun HomeScreen(padding: PaddingValues, query: String, onQueryChange: (String) -> Unit, categories: List<String>, selectedCategory: String, onCategoryChange: (String) -> Unit, products: List<Product>, loading: Boolean, error: String, onAdd: (Product) -> Unit, onOpen: (Product) -> Unit, wishlist: Set<Int>, onToggleWishlist: (Int) -> Unit, onFilter: () -> Unit, sortMode: String, minRating: Double, maxPrice: Long?, inStockOnly: Boolean) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Shop smart. Live better.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Live marketplace • smart discovery") }
        item { HomeSearchFirst(query, onQueryChange) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = onFilter) { Text("Filters & Sort") }; if (sortMode != "Relevance") Text("• $sortMode", style = MaterialTheme.typography.bodySmall); if (minRating > 0) Text("• ${minRating}★+", style = MaterialTheme.typography.bodySmall); if (maxPrice != null) Text("• ≤ ₹${maxPrice / 100}", style = MaterialTheme.typography.bodySmall); if (inStockOnly) Text("• In stock", style = MaterialTheme.typography.bodySmall) } }
        item {
            Text("Shop by Category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(categories) { item -> Category3DCard(item, item == selectedCategory) { onCategoryChange(item) } }
            }
        }
        if (loading) item { CircularProgressIndicator() }
        if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }
        if (!loading && error.isBlank() && products.isEmpty()) item { Text("No products match your current search or filters.") }
        items(products, key = { it.id }) { product -> ProductCard(product, product.id in wishlist, onAdd, onOpen, onToggleWishlist) }
    }
}

@Composable private fun Category3DCard(label: String, selected: Boolean, onClick: () -> Unit) {
    val key = label.trim().lowercase()
    val icon = when (key) {
        "all" -> "🛍️"
        "fashion", "clothing", "apparel" -> "👕"
        "electronics", "mobile", "mobiles" -> "📱"
        "home", "home & kitchen", "kitchen" -> "🏠"
        "beauty", "personal care" -> "💄"
        "grocery", "groceries" -> "🛒"
        "sports" -> "⚽"
        "books" -> "📚"
        "toys" -> "🧸"
        else -> "🛍️"
    }
    Column(Modifier.width(104.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().height(82.dp).clip(RoundedCornerShape(18.dp)).background(
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)))
            ),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(54.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                Text(icon, fontSize = 34.sp)
            }
            if (selected) Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopEnd).padding(7.dp))
        }
        Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable private fun HomeSearchFirst(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(query, onQueryChange, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search for products, brands and more...") })
}

@Composable private fun ProductCard'''
    s = s[:match.start()] + home + s[match.end():]

p.write_text(s, encoding='utf-8')
print('AARVO: search above hero + category-wise 3D visuals applied')
