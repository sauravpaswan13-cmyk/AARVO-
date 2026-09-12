from pathlib import Path
import re

PATH = Path('app/src/main/java/com/aarvo/MainActivity.kt')
text = PATH.read_text()

imports = '''import androidx.compose.foundation.Image\nimport androidx.compose.foundation.shape.RoundedCornerShape\nimport androidx.compose.ui.Alignment\nimport androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.res.painterResource\nimport androidx.compose.ui.unit.sp\n'''
anchor = 'import androidx.compose.foundation.layout.Arrangement\n'
if 'import androidx.compose.foundation.Image' not in text:
    text = text.replace(anchor, imports + anchor, 1)

home = r'''@Composable private fun HomeScreen(padding: PaddingValues, query: String, onQueryChange: (String) -> Unit, categories: List<String>, selectedCategory: String, onCategoryChange: (String) -> Unit, products: List<Product>, loading: Boolean, error: String, onAdd: (Product) -> Unit, onOpen: (Product) -> Unit, wishlist: Set<Int>, onToggleWishlist: (Int) -> Unit, onFilter: () -> Unit, sortMode: String, minRating: Double, maxPrice: Long?, inStockOnly: Boolean) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.aarvo_logo), "AARVO logo", Modifier.size(42.dp))
                Spacer(Modifier.size(6.dp))
                Text("AARVO", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF22236D), modifier = Modifier.weight(1f))
                Text("AI", fontWeight = FontWeight.Bold, color = Color(0xFF22236D))
            }
        }
        item { OutlinedTextField(query, onQueryChange, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search for products, brands and more...") }) }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Big Savings", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("Bigger Smiles!", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Up to 70% OFF", color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { if (categories.size > 1) onCategoryChange(categories[1]) }, shape = RoundedCornerShape(12.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF4B16D7))) { Text("Shop Now", fontWeight = FontWeight.Bold) }
                }
            }
        }
        item {
            Text("Categories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                items(categories) { item ->
                    Card(shape = RoundedCornerShape(16.dp), onClick = { onCategoryChange(item) }) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (item == "All") "🛍️" else "🛒", fontSize = 25.sp)
                            Text(item, fontSize = 11.sp, fontWeight = if (item == selectedCategory) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Top Deals", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = onFilter) { Text("View All") }
            }
        }
        if (loading) item { CircularProgressIndicator() }
        if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }
        if (!loading && error.isBlank() && products.isEmpty()) item { Text("No products available yet.") }
        items(products, key = { it.id }) { product -> ProductCard(product, product.id in wishlist, onAdd, onOpen, onToggleWishlist) }
    }
}

@Composable private fun ProductCard(product: Product, isSaved: Boolean, onAdd: (Product) -> Unit, onOpen: (Product) -> Unit, onToggleWishlist: (Int) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(product.emoji, fontSize = 48.sp, modifier = Modifier.padding(4.dp))
                Column(Modifier.weight(1f)) {
                    Text(product.name, fontWeight = FontWeight.Bold, maxLines = 2)
                    Text(product.category, fontSize = 11.sp, color = Color.Gray)
                    Text("★ ${product.rating}", color = Color(0xFFE58B00), fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { onToggleWishlist(product.id) }) { Icon(if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Wishlist") }
            }
            Text(product.displayPrice, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(product.description, maxLines = 2, fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(6.dp))
            Button(onClick = { onAdd(product) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF5A16E8))) { Text("Add to Cart", fontWeight = FontWeight.Bold) }
            TextButton(onClick = { onOpen(product) }, modifier = Modifier.fillMaxWidth()) { Text("View details") }
        }
    }
}
'''

account = r'''@Composable private fun AccountScreen(padding: PaddingValues, userName: String, role: String, api: AarvoApiClient, activity: MainActivity, guestMode: Boolean, onLogin: () -> Unit, onSignOut: () -> Unit) {
    var section by remember { mutableStateOf("account") }
    when (section) {
        "orders" -> OrdersScreen(padding, api) { section = "account" }
        "seller" -> SellerDashboardScreen(padding, api) { section = "account" }
        else -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.aarvo_logo), "AARVO logo", Modifier.size(44.dp))
                    Spacer(Modifier.size(8.dp))
                    Column(Modifier.weight(1f)) { Text("AARVO", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF22236D)); Text("My Account", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                }
            }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("👤", fontSize = 38.sp)
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) { Text(if (guestMode) "Guest User" else userName.ifBlank { "AARVO User" }, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(if (guestMode) "Browse as guest" else "✓ Verified account", fontSize = 12.sp, color = Color(0xFF4B16D7)) }
                        if (guestMode) TextButton(onClick = onLogin) { Text("Login") }
                    }
                }
            }
            if (guestMode) {
                item { Text("Browse freely. Login when you want to buy or use account features.", color = Color.Gray) }
                item { Button(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(18.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF5A16E8))) { Text("Login / Sign Up with OTP", fontWeight = FontWeight.Bold) } }
            } else {
                item { AccountRow("📦", "My Orders", "Orders & Tracking") { section = "orders" } }
                item { AccountRow("❤️", "Wishlist", "Saved products") { } }
                item { AccountRow("📍", "Address Book", "Delivery addresses") { activity.startActivity(Intent(activity, AddressBookActivity::class.java)) } }
                item { AccountRow("💳", "Payment Methods", "Secure payments") { } }
                item { AccountRow("⭐", "My Reviews & Ratings", "Your shopping feedback") { } }
                item { AccountRow("❓", "Help & Support", "Get help with AARVO") { } }
                item { AccountRow("⚙️", "Settings", "Account preferences") { } }
                item { Button(onClick = { activity.startActivity(Intent(activity, SellerAccountActivity::class.java)) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(18.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF5A16E8))) { Text("Become a Seller", fontWeight = FontWeight.Bold) } }
                if (role == "SELLER") item { Button(onClick = { section = "seller" }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Text("Seller Dashboard") } }
                item { Text("Buyer payments are server-verified before an order becomes confirmed.", style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                item { TextButton(onClick = onSignOut) { Text("Sign out", color = Color.Red, fontWeight = FontWeight.Bold) } }
            }
        }
    }
}

@Composable private fun AccountRow(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), onClick = onClick) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 22.sp)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, fontSize = 11.sp, color = Color.Gray) }
            Text("›", fontSize = 26.sp, color = Color.Gray)
        }
    }
}
'''

text, n1 = re.subn(r'@Composable private fun HomeScreen\(.*?(?=@Composable private fun ProductDetailsScreen)', home, text, count=1, flags=re.S)
if n1 != 1:
    raise SystemExit('HomeScreen replacement failed')
text, n2 = re.subn(r'@Composable private fun AccountScreen\(.*?(?=@Composable private fun OrdersScreen)', account, text, count=1, flags=re.S)
if n2 != 1:
    raise SystemExit('AccountScreen replacement failed')
PATH.write_text(text)
print('reference clone UI applied')
