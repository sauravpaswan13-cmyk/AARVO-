package com.aarvo

import android.content.Intent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aarvo.data.Product
import com.aarvo.network.AarvoApiClient
import com.aarvo.ui.theme.AarvoTheme
import kotlinx.coroutines.launch
import org.json.JSONArray

class GuestBrowseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("aarvo_prefs", MODE_PRIVATE)
        setContent { AarvoTheme { GuestBrowseScreen(prefs) } }
    }

    private fun openLogin() {
        startActivity(Intent(this, PhoneAuthActivity::class.java))
    }

    @Composable
    private fun GuestBrowseScreen(prefs: android.content.SharedPreferences) {
        val api = remember { AarvoApiClient { prefs.getString("auth_token", null) } }
        var query by remember { mutableStateOf("") }
        var products by remember { mutableStateOf<List<Product>>(emptyList()) }
        var loading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()

        fun loadProducts(search: String) {
            scope.launch {
                loading = true
                error = ""
                try { products = api.products(search, "All").toProductList() }
                catch (t: Throwable) { products = emptyList(); error = t.message ?: "Unable to load products." }
                finally { loading = false }
            }
        }

        LaunchedEffect(Unit) { loadProducts("") }

        Scaffold(topBar = { TopAppBar(title = { Text("AARVO", fontWeight = FontWeight.Bold) }, actions = { TextButton(onClick = { openLogin() }) { Text("Login / Signup") } }) }) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("Shop as Guest", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Browse products without verification. Login is required only when you want to buy.")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(query, { query = it; loadProducts(it) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search products") })
                }
                if (loading) item { CircularProgressIndicator() }
                if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }
                if (!loading && error.isBlank() && products.isEmpty()) item { Text("No published products found.") }
                items(products, key = { it.id }) { product ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${product.emoji}  ${product.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(product.category, style = MaterialTheme.typography.bodySmall)
                            Text(product.displayPrice, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("★ ${product.rating}")
                            Text(product.description)
                            Text("Stock: ${product.stockQuantity}", style = MaterialTheme.typography.bodySmall)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { openLogin() }, modifier = Modifier.fillMaxWidth()) { Text("Login to Buy") }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun JSONArray.toProductList(): List<Product> = buildList {
        for (i in 0 until length()) {
            val o = getJSONObject(i)
            val pricePaise = o.getLong("price_paise")
            add(Product(o.getLong("id").toInt(), o.getString("seller_id"), o.getString("seller_name"), o.getString("name"), o.getString("category"), (pricePaise / 100L).toInt(), o.optDouble("rating", 0.0), "🛍️", o.getString("description"), o.getInt("stock_quantity"), o.optBoolean("is_published", true), pricePaise))
        }
    }
}
