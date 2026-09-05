package com.aarvo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aarvo.cart.CartViewModel
import com.aarvo.data.Product
import com.aarvo.ui.theme.AarvoTheme
import com.aarvo.network.AarvoApiClient
import kotlinx.coroutines.launch
import org.json.JSONArray

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AarvoTheme { AarvoRoot(applicationContext) } }
    }
}

@Composable
private fun AarvoRoot(context: Context) {
    val prefs = remember { context.getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE) }
    var onboarded by remember { mutableStateOf(prefs.getBoolean("onboarded", false)) }
    var signedIn by remember { mutableStateOf(prefs.getBoolean("signed_in", false)) }
    var userName by remember { mutableStateOf(prefs.getString("user_name", "") ?: "") }
    val api = remember { AarvoApiClient { prefs.getString("auth_token", null) } }

    when {
        !onboarded -> OnboardingScreen(onDone = {
            prefs.edit().putBoolean("onboarded", true).apply()
            onboarded = true
        })
        !signedIn -> SignInScreen(api, onSignedIn = { name, token ->
            userName = name
            prefs.edit().putBoolean("signed_in", true).putString("user_name", name).putString("auth_token", token).apply()
            signedIn = true
        })
        else -> AarvoApp(userName = userName, api = api, onSignOut = {
            prefs.edit().putBoolean("signed_in", false).remove("auth_token").apply()
            signedIn = false
        })
    }
}

@Composable
private fun OnboardingScreen(onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("AARVO", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Shop smart. Live better.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("A real marketplace for buyers and sellers, with server-authoritative products, orders and payments.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Get started") }
    }
}

@Composable
private fun SignInScreen(api: AarvoApiClient, onSignedIn: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var seller by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var registerMode by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(if (registerMode) "Create your AARVO account" else "Welcome to AARVO", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        if (registerMode) {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Full name") })
            Spacer(Modifier.height(10.dp))
        }
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Email") })
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), label = { Text("Password (8+ characters)") })
        if (registerMode) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Phone") })
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { seller = !seller }) { Text(if (seller) "✓ Register as seller" else "Register as buyer") }
        }
        Spacer(Modifier.height(12.dp))
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                loading = true; error = ""
                scope.launch {
                    try {
                        val result = if (registerMode) api.register(email, password, name, if (seller) "SELLER" else "BUYER", phone) else api.login(email, password)
                        val user = result.getJSONObject("user")
                        onSignedIn(user.optString("display_name", name), result.getString("token"))
                    } catch (t: Throwable) { error = t.message ?: "Unable to connect to AARVO server." }
                    finally { loading = false }
                }
            },
            enabled = !loading && email.contains("@") && password.length >= 8 && (!registerMode || name.isNotBlank()),
            modifier = Modifier.fillMaxWidth()
        ) { if (loading) CircularProgressIndicator() else Text(if (registerMode) "Create account" else "Sign in") }
        TextButton(onClick = { registerMode = !registerMode; error = "" }) { Text(if (registerMode) "Already have an account? Sign in" else "New to AARVO? Create account") }
        if (!api.isConfigured()) Text("Live API is not configured in this build. No demo account is used.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AarvoApp(userName: String, api: AarvoApiClient, onSignOut: () -> Unit, cartViewModel: CartViewModel = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var wishlist by remember { mutableStateOf(setOf<Int>()) }
    var showCheckout by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val cartItems by cartViewModel.items.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(query, category, api) {
        loading = true; error = ""
        try { products = api.products(query, category).toProductList() }
        catch (t: Throwable) { products = emptyList(); error = t.message ?: "Unable to load products." }
        finally { loading = false }
    }

    if (selectedProduct != null) {
        ProductDetailsScreen(selectedProduct!!, selectedProduct!!.id in wishlist, { selectedProduct = null }, {
            wishlist = if (selectedProduct!!.id in wishlist) wishlist - selectedProduct!!.id else wishlist + selectedProduct!!.id
        }, cartViewModel::add)
        return
    }

    if (showCheckout) CheckoutDialog(cartItems.sumOf { it.price }, address, { address = it }, { showCheckout = false }, { showCheckout = false })

    Scaffold(
        topBar = { TopAppBar(title = { Text("AARVO", fontWeight = FontWeight.Bold) }, actions = { BadgedBox(badge = { if (cartItems.isNotEmpty()) Badge { Text(cartItems.size.toString()) } }) { IconButton(onClick = { selectedTab = 1 }) { Icon(Icons.Default.ShoppingCart, "Cart") } } }) },
        bottomBar = { NavigationBar {
            NavigationBarItem(selectedTab == 0, { selectedTab = 0 }, { Icon(Icons.Default.Home, "Home") }, label = { Text("Home") })
            NavigationBarItem(selectedTab == 1, { selectedTab = 1 }, { BadgedBox(badge = { if (cartItems.isNotEmpty()) Badge { Text(cartItems.size.toString()) } }) { Icon(Icons.Default.ShoppingCart, "Cart") } }, label = { Text("Cart") })
            NavigationBarItem(selectedTab == 2, { selectedTab = 2 }, { Icon(Icons.Default.Person, "Profile") }, label = { Text("Profile") })
        } }
    ) { padding ->
        when (selectedTab) {
            0 -> HomeScreen(padding, query, { query = it }, listOf("All", "Fashion", "Electronics", "Home", "Beauty"), category, { category = it }, products, loading, error, cartViewModel::add, { selectedProduct = it }, wishlist, { id -> wishlist = if (id in wishlist) wishlist - id else wishlist + id })
            1 -> CartScreen(padding, cartItems, cartViewModel::remove, cartViewModel::clear) { showCheckout = true }
            else -> ProfileScreen(padding, userName, address, onSignOut)
        }
    }
}

private fun JSONArray.toProductList(): List<Product> = buildList {
    for (i in 0 until length()) {
        val o = getJSONObject(i)
        add(Product(o.getLong("id").toInt(), o.getString("seller_id"), o.getString("seller_name"), o.getString("name"), o.getString("category"), o.getInt("price_paise") / 100, o.optDouble("rating", 0.0), "🛍️", o.getString("description"), o.getInt("stock_quantity"), o.optBoolean("is_published", true)))
    }
}

@Composable
private fun HomeScreen(padding: PaddingValues, query: String, onQueryChange: (String) -> Unit, categories: List<String>, selectedCategory: String, onCategoryChange: (String) -> Unit, products: List<Product>, loading: Boolean, error: String, onAdd: (Product) -> Unit, onOpen: (Product) -> Unit, wishlist: Set<Int>, onToggleWishlist: (Int) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Shop smart. Live better.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Products come from the live marketplace API.", style = MaterialTheme.typography.bodyMedium) }
        item { OutlinedTextField(query, onQueryChange, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search products") }) }
        item { Text("Categories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(categories) { item -> TextButton(onClick = { onCategoryChange(item) }) { Text(if (item == selectedCategory) "✓ $item" else item) } } } }
        if (loading) item { CircularProgressIndicator() }
        if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }
        if (!loading && error.isBlank() && products.isEmpty()) item { Text("No published products found.") }
        else items(products, key = { it.id }) { product -> ProductCard(product, product.id in wishlist, onAdd, onOpen, onToggleWishlist) }
    }
}

@Composable
private fun ProductCard(product: Product, isSaved: Boolean, onAdd: (Product) -> Unit, onOpen: (Product) -> Unit, onToggleWishlist: (Int) -> Unit) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${product.emoji}  ${product.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = { onToggleWishlist(product.id) }) { Icon(if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Wishlist") }
        }
        Text(product.category, style = MaterialTheme.typography.bodySmall)
        Text("₹${product.price}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("★ ${product.rating}")
        Text(product.description, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = { onOpen(product) }) { Text("View details") }; TextButton(onClick = { onAdd(product) }) { Text("Add to cart") } }
    } }
}

@Composable
private fun ProductDetailsScreen(product: Product, isSaved: Boolean, onBack: () -> Unit, onToggleWishlist: () -> Unit, onAdd: (Product) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Product details") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${product.emoji}  ${product.name}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(product.category); Text("₹${product.price}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("★ ${product.rating}")
            Text(product.description, style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { onAdd(product) }) { Text("Add to cart") }; TextButton(onClick = onToggleWishlist) { Text(if (isSaved) "Remove from wishlist" else "Save to wishlist") } }
        }
    }
}

@Composable
private fun CartScreen(padding: PaddingValues, items: List<Product>, onRemove: (Product) -> Unit, onClear: () -> Unit, onCheckout: () -> Unit) {
    val total = items.sumOf { it.price }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Your Cart", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); if (items.isNotEmpty()) TextButton(onClick = onClear) { Text("Clear") } } }
        if (items.isEmpty()) item { Text("Your cart is empty. Add something you like from Home.") }
        else {
            items(items) { product -> Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(product.name, fontWeight = FontWeight.SemiBold); Text("₹${product.price}") }; IconButton(onClick = { onRemove(product) }) { Icon(Icons.Default.Delete, "Remove") } } } }
            item { Text("Total: ₹$total", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Button(onClick = onCheckout) { Text("Proceed to checkout") } }
        }
    }
}

@Composable
private fun CheckoutDialog(total: Int, address: String, onAddressChange: (String) -> Unit, onDismiss: () -> Unit, onPlaceOrder: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Checkout") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Current cart value: ₹$total", fontWeight = FontWeight.Bold); OutlinedTextField(address, onAddressChange, label = { Text("Delivery address") }, minLines = 3); Text("Payment is intentionally not simulated. Live order creation will be enabled with the payment SDK after gateway configuration.", style = MaterialTheme.typography.bodySmall) } }, confirmButton = { Button(onClick = onPlaceOrder, enabled = false) { Text("Payment setup required") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable
private fun ProfileScreen(padding: PaddingValues, userName: String, address: String, onSignOut: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("My Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text(userName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); Text("AARVO account") } }
        Text("Orders", style = MaterialTheme.typography.titleMedium); Text("Verified orders will appear here once checkout is connected.")
        Text("Saved address", style = MaterialTheme.typography.titleMedium); Text(if (address.isBlank()) "No address saved yet." else address)
        Text("Seller publishing requires server-side verification and payout readiness.", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onSignOut) { Text("Sign out") }
    }
}
