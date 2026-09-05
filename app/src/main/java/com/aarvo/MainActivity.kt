package com.aarvo

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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
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
        setContent { AarvoTheme { AarvoApp() } }
    }
}

@Composable
private fun AarvoApp(cartViewModel: CartViewModel = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    val cartItems by cartViewModel.items.collectAsState()
    val repository = remember { ProductRepository() }
    val products = remember(query, category) { repository.products(query, category) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AARVO", fontWeight = FontWeight.Bold) },
                actions = {
                    BadgedBox(badge = { if (cartItems.isNotEmpty()) Badge { Text(cartItems.size.toString()) } }) {
                        IconButton(onClick = { selectedTab = 1 }) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        BadgedBox(badge = { if (cartItems.isNotEmpty()) Badge { Text(cartItems.size.toString()) } }) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                        }
                    },
                    label = { Text("Cart") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> HomeScreen(
                padding = padding,
                query = query,
                onQueryChange = { query = it },
                categories = repository.categories(),
                selectedCategory = category,
                onCategoryChange = { category = it },
                products = products,
                onAdd = cartViewModel::add
            )
            1 -> CartScreen(padding, cartItems, cartViewModel::remove, cartViewModel::clear)
            else -> ProfileScreen(padding)
        }
    }
}

@Composable
private fun HomeScreen(
    padding: PaddingValues,
    query: String,
    onQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    products: List<Product>,
    onAdd: (Product) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Shop smart. Live better.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Discover products you'll love.", style = MaterialTheme.typography.bodyMedium)
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search products") }
            )
        }
        item {
            Text("Categories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { item ->
                    TextButton(onClick = { onCategoryChange(item) }) {
                        Text(if (item == selectedCategory) "✓ $item" else item)
                    }
                }
            }
        }
        item { Text("Popular products", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        if (products.isEmpty()) {
            item { Text("No products found. Try another search.") }
        } else {
            items(products, key = { it.id }) { product -> ProductCard(product, onAdd) }
        }
    }
}

@Composable
private fun ProductCard(product: Product, onAdd: (Product) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("${product.emoji}  ${product.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(product.category, style = MaterialTheme.typography.bodySmall)
            Text("₹${product.price}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("★ ${product.rating}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text(product.description, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { onAdd(product) }) { Text("Add to cart") }
        }
    }
}

@Composable
private fun CartScreen(
    padding: PaddingValues,
    items: List<Product>,
    onRemove: (Product) -> Unit,
    onClear: () -> Unit
) {
    val total = items.sumOf { it.price }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Your Cart", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (items.isNotEmpty()) TextButton(onClick = onClear) { Text("Clear") }
            }
        }
        if (items.isEmpty()) {
            item { Text("Your cart is empty. Add something you like from Home.") }
        } else {
            items(items) { product ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.SemiBold)
                            Text("₹${product.price}")
                        }
                        IconButton(onClick = { onRemove(product) }) {
                            Icon(Icons.Default.Remove, contentDescription = "Remove")
                        }
                    }
                }
            }
            item {
                Text("Total: ₹$total", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Button(onClick = { }) { Text("Proceed to checkout") }
            }
        }
    }
}

@Composable
private fun ProfileScreen(padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("My Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth()) { Text("Welcome to AARVO", Modifier.padding(18.dp), style = MaterialTheme.typography.titleMedium) }
        TextButton(onClick = { }) { Text("Orders") }
        TextButton(onClick = { }) { Text("Saved addresses") }
        TextButton(onClick = { }) { Text("Help & support") }
    }
}
