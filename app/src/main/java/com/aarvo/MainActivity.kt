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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aarvo.cart.CartViewModel
import com.aarvo.data.Product
import com.aarvo.data.ProductRepository
import com.aarvo.ui.theme.AarvoTheme

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

    when {
        !onboarded -> OnboardingScreen(onDone = {
            prefs.edit().putBoolean("onboarded", true).apply()
            onboarded = true
        })
        !signedIn -> SignInScreen(onSignedIn = { name ->
            userName = name
            prefs.edit().putBoolean("signed_in", true).putString("user_name", name).apply()
            signedIn = true
        })
        else -> AarvoApp(userName = userName, onSignOut = {
            prefs.edit().putBoolean("signed_in", false).apply()
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
        Text("Discover useful products, save favourites and checkout in a simple shopping experience.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Get started") }
    }
}

@Composable
private fun SignInScreen(onSignedIn: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Welcome to AARVO", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Your name") })
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Email") })
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onSignedIn(name.trim().ifBlank { "AARVO User" }) }, enabled = name.isNotBlank() && email.contains("@"), modifier = Modifier.fillMaxWidth()) { Text("Continue") }
        Spacer(Modifier.height(8.dp))
        Text("Demo authentication: account data is stored locally on this device.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AarvoApp(userName: String, onSignOut: () -> Unit, cartViewModel: CartViewModel = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var wishlist by remember { mutableStateOf(setOf<Int>()) }
    var showCheckout by remember { mutableStateOf(false) }
    var orderPlaced by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    val cartItems by cartViewModel.items.collectAsState()
    val repository = remember { ProductRepository() }
    val products = remember(query, category) { repository.products(query, category) }

    if (selectedProduct != null) {
        ProductDetailsScreen(selectedProduct!!, selectedProduct!!.id in wishlist, { selectedProduct = null }, {
            wishlist = if (selectedProduct!!.id in wishlist) wishlist - selectedProduct!!.id else wishlist + selectedProduct!!.id
        }, cartViewModel::add)
        return
    }

    if (showCheckout) CheckoutDialog(cartItems.sumOf { it.price }, address, { address = it }, { showCheckout = false }, {
        showCheckout = false
        orderPlaced = true
        cartViewModel.clear()
    })

    if (orderPlaced) AlertDialog(onDismissRequest = { orderPlaced = false }, title = { Text("Order placed") }, text = { Text("Your AARVO order has been created successfully. Order tracking will be connected to the backend in the next phase.") }, confirmButton = { TextButton(onClick = { orderPlaced = false }) { Text("Done") } })

    Scaffold(
        topBar = { TopAppBar(title = { Text("AARVO", fontWeight = FontWeight.Bold) }, actions = { BadgedBox(badge = { if (cartItems.isNotEmpty()) Badge { Text(cartItems.size.toString()) } }) { IconButton(onClick = { selectedTab = 1 }) { Icon(Icons.Default.ShoppingCart, "Cart") } } }) },
        bottomBar = { NavigationBar {
            NavigationBarItem(selectedTab == 0, { selectedTab = 0 }, { Icon(Icons.Default.Home, "Home") }, label = { Text("Home") })
            NavigationBarItem(selectedTab == 1, { selectedTab = 1 }, { BadgedBox(badge = { if (cartItems.isNotEmpty()) Badge { Text(cartItems.size.toString()) } }) { Icon(Icons.Default.ShoppingCart, "Cart") } }, label = { Text("Cart") })
            NavigationBarItem(selectedTab == 2, { selectedTab = 2 }, { Icon(Icons.Default.Person, "Profile") }, label = { Text("Profile") })
        } }
    ) { padding ->
        when (selectedTab) {
            0 -> HomeScreen(padding, query, { query = it }, repository.categories(), category, { category = it }, products, cartViewModel::add, { selectedProduct = it }, wishlist, { id -> wishlist = if (id in wishlist) wishlist - id else wishlist + id })
            1 -> CartScreen(padding, cartItems, cartViewModel::remove, cartViewModel::clear) { showCheckout = true }
            else -> ProfileScreen(padding, userName, address, onSignOut)
        }
    }
}

@Composable
private fun HomeScreen(padding: PaddingValues, query: String, onQueryChange: (String) -> Unit, categories: List<String>, selectedCategory: String, onCategoryChange: (String) -> Unit, products: List<Product>, onAdd: (Product) -> Unit, onOpen: (Product) -> Unit, wishlist: Set<Int>, onToggleWishlist: (Int) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Shop smart. Live better.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Discover products you'll love.", style = MaterialTheme.typography.bodyMedium) }
        item { OutlinedTextField(query, onQueryChange, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search products") }) }
        item { Text("Categories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(categories) { item -> TextButton(onClick = { onCategoryChange(item) }) { Text(if (item == selectedCategory) "✓ $item" else item) } } } }
        item { Text("Popular products", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        if (products.isEmpty()) item { Text("No products found. Try another search.") }
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
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Checkout") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Order total: ₹$total", fontWeight = FontWeight.Bold); OutlinedTextField(address, onAddressChange, label = { Text("Delivery address") }, minLines = 3); Text("Secure online payment will be connected to the live server checkout before production launch.", style = MaterialTheme.typography.bodySmall) } }, confirmButton = { Button(onClick = onPlaceOrder, enabled = address.isNotBlank()) { Text("Place order") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun ProfileScreen(padding: PaddingValues, userName: String, address: String, onSignOut: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("My Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text(userName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); Text("AARVO account") } }
        Text("Orders", style = MaterialTheme.typography.titleMedium); Text("Order history will appear here after your purchases.")
        Text("Saved address", style = MaterialTheme.typography.titleMedium); Text(if (address.isBlank()) "No address saved yet." else address)
        Text("Seller/Admin tools and backend sync are planned for the next phase.", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onSignOut) { Text("Sign out") }
    }
}
