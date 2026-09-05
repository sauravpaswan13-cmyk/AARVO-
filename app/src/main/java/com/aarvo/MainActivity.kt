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
import com.aarvo.network.AarvoApiClient
import com.aarvo.payment.PaymentBridge
import com.aarvo.ui.theme.AarvoTheme
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    private lateinit var razorpayCheckout: Checkout
    private var paymentCallback: ((String?, String?) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        razorpayCheckout = Checkout()
        setContent { AarvoTheme { AarvoRoot(this, applicationContext) } }
    }

    fun startRazorpayPayment(options: JSONObject, callback: (String?, String?) -> Unit) {
        PaymentBridge.clear(); paymentCallback = callback
        try { razorpayCheckout.setKeyID(options.getString("key")); razorpayCheckout.open(this, options) }
        catch (t: Throwable) { paymentCallback = null; callback(null, t.message ?: "Unable to open payment checkout") }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        PaymentBridge.capture(paymentData); val callback = paymentCallback; paymentCallback = null; callback?.invoke(razorpayPaymentId, null)
    }
    override fun onPaymentError(code: Int, description: String?, paymentData: PaymentData?) {
        PaymentBridge.capture(paymentData); val callback = paymentCallback; paymentCallback = null; callback?.invoke(null, description ?: "Payment failed (code $code)")
    }
}

@Composable
private fun AarvoRoot(activity: MainActivity, context: Context) {
    val prefs = remember { context.getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE) }
    var onboarded by remember { mutableStateOf(prefs.getBoolean("onboarded", false)) }
    var signedIn by remember { mutableStateOf(prefs.getBoolean("signed_in", false)) }
    var userName by remember { mutableStateOf(prefs.getString("user_name", "") ?: "") }
    var role by remember { mutableStateOf(prefs.getString("user_role", "BUYER") ?: "BUYER") }
    val api = remember { AarvoApiClient { prefs.getString("auth_token", null) } }
    when {
        !onboarded -> OnboardingScreen { prefs.edit().putBoolean("onboarded", true).apply(); onboarded = true }
        !signedIn -> SignInScreen(api) { name, token, userRole ->
            userName = name; role = userRole
            prefs.edit().putBoolean("signed_in", true).putString("user_name", name).putString("user_role", userRole).putString("auth_token", token).apply()
            signedIn = true
        }
        else -> AarvoApp(userName, role, api, activity) {
            prefs.edit().putBoolean("signed_in", false).remove("auth_token").remove("user_role").apply(); signedIn = false
        }
    }
}

@Composable
private fun OnboardingScreen(onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("AARVO", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp)); Text("Shop smart. Live better.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp)); Text("A real marketplace for buyers and sellers, with server-authoritative products, orders and payments.")
        Spacer(Modifier.height(24.dp)); Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Get started") }
    }
}

@Composable
private fun SignInScreen(api: AarvoApiClient, onSignedIn: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    var seller by remember { mutableStateOf(false) }; var phone by remember { mutableStateOf("") }; var registerMode by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(if (registerMode) "Create your AARVO account" else "Welcome to AARVO", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        if (registerMode) { OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Full name") }); Spacer(Modifier.height(10.dp)) }
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Email") }); Spacer(Modifier.height(10.dp))
        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), label = { Text("Password (8+ characters)") })
        if (registerMode) { Spacer(Modifier.height(10.dp)); OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Phone") }); Spacer(Modifier.height(8.dp)); TextButton(onClick = { seller = !seller }) { Text(if (seller) "✓ Register as seller" else "Register as buyer") } }
        Spacer(Modifier.height(12.dp)); if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp))
        Button(onClick = {
            loading = true; error = ""; scope.launch { try {
                val result = if (registerMode) api.register(email, password, name, if (seller) "SELLER" else "BUYER", phone) else api.login(email, password)
                val user = result.getJSONObject("user"); onSignedIn(user.optString("display_name", name), result.getString("token"), user.optString("role", "BUYER"))
            } catch (t: Throwable) { error = t.message ?: "Unable to connect to AARVO server." } finally { loading = false } }
        }, enabled = !loading && email.contains("@") && password.length >= 8 && (!registerMode || (name.isNotBlank() && (!seller || phone.trim().length >= 10))), modifier = Modifier.fillMaxWidth()) { if (loading) CircularProgressIndicator() else Text(if (registerMode) "Create account" else "Sign in") }
        TextButton(onClick = { registerMode = !registerMode; error = "" }) { Text(if (registerMode) "Already have an account? Sign in" else "New to AARVO? Create account") }
        if (!api.isConfigured()) Text("Live API is not configured in this build. No demo account is used.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AarvoApp(userName: String, role: String, api: AarvoApiClient, activity: MainActivity, onSignOut: () -> Unit, cartViewModel: CartViewModel = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }; var query by remember { mutableStateOf("") }; var category by remember { mutableStateOf("All") }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }; var wishlist by remember { mutableStateOf(setOf<Int>()) }; var showCheckout by remember { mutableStateOf(false) }
    var checkoutLoading by remember { mutableStateOf(false) }; var checkoutMessage by remember { mutableStateOf("") }; var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf("") }; val cartItems by cartViewModel.items.collectAsState(); val scope = rememberCoroutineScope()
    LaunchedEffect(query, category, api) { loading = true; error = ""; try { products = api.products(query, category).toProductList() } catch (t: Throwable) { products = emptyList(); error = t.message ?: "Unable to load products." } finally { loading = false } }
    if (selectedProduct != null) { ProductDetailsScreen(selectedProduct!!, selectedProduct!!.id in wishlist, { selectedProduct = null }, { wishlist = if (selectedProduct!!.id in wishlist) wishlist - selectedProduct!!.id else wishlist + selectedProduct!!.id }, cartViewModel::add); return }
    if (showCheckout) CheckoutDialog(cartItems.sumOf { it.price }, checkoutLoading, checkoutMessage, { if (!checkoutLoading) showCheckout = false }) { fullName, phone, line1, city, state, postalCode ->
        checkoutLoading = true; checkoutMessage = "Creating secure order..."; scope.launch {
            try {
                val items = JSONArray().apply { cartItems.forEach { put(JSONObject().put("productId", it.id).put("quantity", 1)) } }
                val address = JSONObject().apply { put("fullName", fullName.trim()); put("phone", phone.trim()); put("line1", line1.trim()); put("line2", ""); put("city", city.trim()); put("state", state.trim()); put("postalCode", postalCode.trim()); put("country", "IN") }
                val order = api.createOrder(items, address)
                val options = JSONObject().apply { put("key", order.getString("keyId")); put("amount", order.getLong("amountPaise")); put("currency", order.getString("currency")); put("name", "AARVO"); put("description", "AARVO marketplace order"); put("order_id", order.getString("gatewayOrderId")); put("prefill", JSONObject().put("name", fullName.trim()).put("contact", phone.trim())); put("notes", JSONObject().put("order_id", order.getString("orderId"))) }
                checkoutMessage = "Opening secure payment..."
                activity.startRazorpayPayment(options) { paymentId, paymentError -> scope.launch {
                    if (paymentId != null) {
                        val signature = PaymentBridge.lastSignature; val gatewayOrderId = PaymentBridge.lastOrderId ?: order.getString("gatewayOrderId")
                        if (!signature.isNullOrBlank()) try { api.verifyPayment(order.getString("orderId"), paymentId, gatewayOrderId, signature); checkoutMessage = "Payment verified. Order confirmed."; cartViewModel.clear(); showCheckout = false }
                        catch (t: Throwable) { checkoutMessage = t.message ?: "Payment verification failed. Order was not confirmed." }
                        else checkoutMessage = "Payment completed but verification data was missing. Order remains unconfirmed."
                    } else { checkoutMessage = paymentError ?: "Payment cancelled or failed."; try { api.cancelOrder(order.getString("orderId")) } catch (_: Throwable) { } }
                    checkoutLoading = false; PaymentBridge.clear()
                } }
            } catch (t: Throwable) { checkoutLoading = false; checkoutMessage = t.message ?: "Unable to create order." }
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text(if (role == "SELLER") "AARVO Seller" else "AARVO", fontWeight = FontWeight.Bold) }, actions = { BadgedBox(badge = { if (cartItems.isNotEmpty()) Badge { Text(cartItems.size.toString()) } }) { IconButton(onClick = { selectedTab = 1 }) { Icon(Icons.Default.ShoppingCart, "Cart") } } }) }, bottomBar = { NavigationBar {
        NavigationBarItem(selectedTab == 0, { selectedTab = 0 }, { Icon(Icons.Default.Home, "Home") }, label = { Text("Home") })
        NavigationBarItem(selectedTab == 1, { selectedTab = 1 }, { BadgedBox(badge = { if (cartItems.isNotEmpty()) Badge { Text(cartItems.size.toString()) } }) { Icon(Icons.Default.ShoppingCart, "Cart") } }, label = { Text("Cart") })
        NavigationBarItem(selectedTab == 2, { selectedTab = 2 }, { Icon(Icons.Default.Person, "Profile") }, label = { Text("Account") })
    } }) { padding -> when (selectedTab) {
        0 -> HomeScreen(padding, query, { query = it }, listOf("All", "Fashion", "Electronics", "Home", "Beauty"), category, { category = it }, products, loading, error, cartViewModel::add, { selectedProduct = it }, wishlist, { id -> wishlist = if (id in wishlist) wishlist - id else wishlist + id })
        1 -> CartScreen(padding, cartItems, cartViewModel::remove, cartViewModel::clear) { showCheckout = true; checkoutMessage = "" }
        else -> AccountScreen(padding, userName, role, api, onSignOut)
    } }
}

private fun JSONArray.toProductList(): List<Product> = buildList { for (i in 0 until length()) { val o = getJSONObject(i); add(Product(o.getLong("id").toInt(), o.getString("seller_id"), o.getString("seller_name"), o.getString("name"), o.getString("category"), o.getLong("price_paise").toInt() / 100, o.optDouble("rating", 0.0), "🛍️", o.getString("description"), o.getInt("stock_quantity"), o.optBoolean("is_published", true))) } }

@Composable
private fun HomeScreen(padding: PaddingValues, query: String, onQueryChange: (String) -> Unit, categories: List<String>, selectedCategory: String, onCategoryChange: (String) -> Unit, products: List<Product>, loading: Boolean, error: String, onAdd: (Product) -> Unit, onOpen: (Product) -> Unit, wishlist: Set<Int>, onToggleWishlist: (Int) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Shop smart. Live better.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Products come from the live marketplace API.") }
        item { OutlinedTextField(query, onQueryChange, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search products") }) }
        item { Text("Categories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(categories) { item -> TextButton(onClick = { onCategoryChange(item) }) { Text(if (item == selectedCategory) "✓ $item" else item) } } } }
        if (loading) item { CircularProgressIndicator() }; if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }; if (!loading && error.isBlank() && products.isEmpty()) item { Text("No published products found.") } else items(products, key = { it.id }) { product -> ProductCard(product, product.id in wishlist, onAdd, onOpen, onToggleWishlist) }
    }
}

@Composable
private fun ProductCard(product: Product, isSaved: Boolean, onAdd: (Product) -> Unit, onOpen: (Product) -> Unit, onToggleWishlist: (Int) -> Unit) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${product.emoji}  ${product.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); IconButton(onClick = { onToggleWishlist(product.id) }) { Icon(if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Wishlist") } }; Text(product.category, style = MaterialTheme.typography.bodySmall); Text("₹${product.price}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("★ ${product.rating}"); Text(product.description); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = { onOpen(product) }) { Text("View details") }; TextButton(onClick = { onAdd(product) }) { Text("Add to cart") } } } } }

@Composable
private fun ProductDetailsScreen(product: Product, isSaved: Boolean, onBack: () -> Unit, onToggleWishlist: () -> Unit, onAdd: (Product) -> Unit) { Scaffold(topBar = { TopAppBar(title = { Text("Product details") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { padding -> Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("${product.emoji}  ${product.name}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(product.category); Text("₹${product.price}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("★ ${product.rating}"); Text(product.description); Text("Stock available: ${product.stockQuantity}"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { onAdd(product) }) { Text("Add to cart") }; TextButton(onClick = onToggleWishlist) { Text(if (isSaved) "Remove from wishlist" else "Save to wishlist") } } } } }

@Composable
private fun CartScreen(padding: PaddingValues, items: List<Product>, onRemove: (Product) -> Unit, onClear: () -> Unit, onCheckout: () -> Unit) { val total = items.sumOf { it.price }; LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Your Cart", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); if (items.isNotEmpty()) TextButton(onClick = onClear) { Text("Clear") } } }; if (items.isEmpty()) item { Text("Your cart is empty. Add something you like from Home.") } else { items(items) { product -> Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(product.name, fontWeight = FontWeight.SemiBold); Text("₹${product.price}") }; IconButton(onClick = { onRemove(product) }) { Icon(Icons.Default.Delete, "Remove") } } } }; item { Text("Total: ₹$total", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Button(onClick = onCheckout) { Text("Proceed to secure checkout") } } } } }

@Composable
private fun CheckoutDialog(total: Int, loading: Boolean, message: String, onDismiss: () -> Unit, onPlaceOrder: (String, String, String, String, String, String) -> Unit) {
    var fullName by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var line1 by remember { mutableStateOf("") }; var city by remember { mutableStateOf("") }; var state by remember { mutableStateOf("") }; var postalCode by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Secure checkout") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Cart value: ₹$total", fontWeight = FontWeight.Bold); OutlinedTextField(fullName, { fullName = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Full name") }); OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Phone") }); OutlinedTextField(line1, { line1 = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Address") }); OutlinedTextField(city, { city = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("City") }); OutlinedTextField(state, { state = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("State") }); OutlinedTextField(postalCode, { postalCode = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("PIN code") }); if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary); Text("Payment is processed by Razorpay. AARVO verifies it on the server before confirming the order.", style = MaterialTheme.typography.bodySmall) } }, confirmButton = { Button(onClick = { onPlaceOrder(fullName, phone, line1, city, state, postalCode) }, enabled = !loading && fullName.isNotBlank() && phone.trim().length >= 10 && line1.isNotBlank() && city.isNotBlank() && state.isNotBlank() && postalCode.trim().length >= 5) { if (loading) CircularProgressIndicator() else Text("Pay securely") } }, dismissButton = { TextButton(onClick = onDismiss, enabled = !loading) { Text("Close") } })
}

@Composable
private fun AccountScreen(padding: PaddingValues, userName: String, role: String, api: AarvoApiClient, onSignOut: () -> Unit) {
    var section by remember { mutableStateOf("account") }
    when (section) {
        "orders" -> OrdersScreen(padding, api) { section = "account" }
        "seller" -> SellerDashboardScreen(padding, api) { section = "account" }
        else -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("My Account", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(userName) }
            item { Button(onClick = { section = "orders" }, modifier = Modifier.fillMaxWidth()) { Text("My Orders & Tracking") } }
            if (role == "SELLER") item { Button(onClick = { section = "seller" }, modifier = Modifier.fillMaxWidth()) { Text("Seller Dashboard") } }
            item { Text("Buyer payments are server-verified before an order becomes confirmed.", style = MaterialTheme.typography.bodySmall) }
            item { TextButton(onClick = onSignOut) { Text("Sign out") } }
        }
    }
}

@Composable
private fun OrdersScreen(padding: PaddingValues, api: AarvoApiClient, onBack: () -> Unit) {
    var orders by remember { mutableStateOf<List<JSONObject>>(emptyList()) }; var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope()
    fun reload() { scope.launch { loading = true; error = ""; try { val a = api.orders(); orders = buildList { for (i in 0 until a.length()) add(a.getJSONObject(i)) } } catch (t: Throwable) { error = t.message ?: "Unable to load orders" } finally { loading = false } } }
    LaunchedEffect(Unit) { reload() }
    Scaffold(topBar = { TopAppBar(title = { Text("My Orders") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { inner ->
        LazyColumn(Modifier.fillMaxSize().padding(inner), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loading) item { CircularProgressIndicator() }; if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }
            if (!loading && orders.isEmpty()) item { Text("No orders yet.") }
            items(orders, key = { it.optString("id") }) { order -> OrderCard(order, api, ::reload) }
        }
    }
}

@Composable
private fun OrderCard(order: JSONObject, api: AarvoApiClient, reload: () -> Unit) {
    var busy by remember { mutableStateOf(false) }; var detail by remember { mutableStateOf<JSONObject?>(null) }; val scope = rememberCoroutineScope()
    val status = order.optString("status", "PENDING"); val payment = order.optString("payment_status", "PENDING"); val id = order.optString("id")
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Order #$id", fontWeight = FontWeight.Bold); Text("₹${order.optLong("total_paise", 0L) / 100}", style = MaterialTheme.typography.titleLarge); Text("Payment: $payment"); Text("Status: $status")
        order.optJSONObject("tracking_json")?.let { Text("Tracking: ${it.optString("status", "Not updated")} ${it.optString("carrier", "")}") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = { scope.launch { busy = true; try { detail = api.order(id) } finally { busy = false } } }, enabled = !busy) { Text("View details") }; if (status !in setOf("CANCELLED", "DELIVERED")) TextButton(onClick = { scope.launch { busy = true; try { api.cancelOrder(id); reload() } finally { busy = false } } }, enabled = !busy) { Text("Cancel") } }
        detail?.let { d -> Text("Items: ${d.optJSONArray("items")?.length() ?: 0}"); Text("Delivery status: ${d.optJSONObject("tracking")?.optString("status", status) ?: status}") }
    } }
}

@Composable
private fun SellerDashboardScreen(padding: PaddingValues, api: AarvoApiClient, onBack: () -> Unit) {
    var products by remember { mutableStateOf<List<JSONObject>>(emptyList()) }; var orders by remember { mutableStateOf<List<JSONObject>>(emptyList()) }; var profile by remember { mutableStateOf<JSONObject?>(null) }; var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf("") }; var showCreate by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope()
    fun reload() { scope.launch { loading = true; error = ""; try { val p = api.sellerProducts(); products = buildList { for (i in 0 until p.length()) add(p.getJSONObject(i)) }; val o = api.sellerOrders(); orders = buildList { for (i in 0 until o.length()) add(o.getJSONObject(i)) }; profile = api.sellerProfile() } catch (t: Throwable) { error = t.message ?: "Unable to load seller dashboard" } finally { loading = false } } }
    LaunchedEffect(Unit) { reload() }
    if (showCreate) SellerProductDialog(api, { showCreate = false; reload() })
    Scaffold(topBar = { TopAppBar(title = { Text("Seller Dashboard") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { inner ->
        LazyColumn(Modifier.fillMaxSize().padding(inner), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loading) item { CircularProgressIndicator() }; if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }
            item { Text("Seller status: ${if (profile?.optBoolean("verified", false) == true) "Verified" else "Verification pending"}") }
            item { Button(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) { Text("Add Product") } }
            item { Text("My Products (${products.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(products, key = { it.optInt("id") }) { p -> SellerProductRow(p, api, reload) }
            item { Text("Recent Orders (${orders.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(orders, key = { it.optString("id") }) { o -> SellerOrderRow(o, api, reload) }
        }
    }
}

@Composable
private fun SellerProductRow(p: JSONObject, api: AarvoApiClient, reload: () -> Unit) { var stock by remember(p.optInt("id")) { mutableStateOf(p.optInt("stock_quantity").toString()) }; val scope = rememberCoroutineScope(); Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text(p.optString("name"), fontWeight = FontWeight.SemiBold); Text("₹${p.optLong("price_paise") / 100} • ${if (p.optBoolean("is_published")) "Published" else "Draft"}"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(stock, { stock = it }, Modifier.weight(1f), singleLine = true, label = { Text("Stock") }); Button(onClick = { scope.launch { api.updateInventory(p.optInt("id"), stock.toIntOrNull() ?: 0); reload() } }) { Text("Save") } } } } }

@Composable
private fun SellerOrderRow(o: JSONObject, api: AarvoApiClient, reload: () -> Unit) { var busy by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope(); val id = o.optString("id"); Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text("Order #$id", fontWeight = FontWeight.SemiBold); Text("Status: ${o.optString("status")} • Payment: ${o.optString("payment_status")}"); Text("Seller amount: ₹${(o.optJSONArray("items")?.let { arr -> (0 until arr.length()).sumOf { arr.getJSONObject(it).optLong("sellerAmountPaise") } } ?: 0L) / 100}"); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { TextButton(onClick = { scope.launch { busy = true; try { api.updateOrderTracking(id, "SHIPPED", note = "Seller marked order shipped"); reload() } finally { busy = false } } }, enabled = !busy) { Text("Mark shipped") }; TextButton(onClick = { scope.launch { busy = true; try { api.updateOrderTracking(id, "DELIVERED", note = "Seller marked order delivered"); reload() } finally { busy = false } } }, enabled = !busy) { Text("Mark delivered") } } } } }

@Composable
private fun SellerProductDialog(api: AarvoApiClient, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }; var category by remember { mutableStateOf("") }; var price by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }; var stock by remember { mutableStateOf("0") }; var error by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!busy) onDone() }, title = { Text("Add product") }, text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { OutlinedTextField(name, { name = it }, label = { Text("Name") }); OutlinedTextField(category, { category = it }, label = { Text("Category") }); OutlinedTextField(price, { price = it }, label = { Text("Price ₹") }); OutlinedTextField(description, { description = it }, label = { Text("Description") }); OutlinedTextField(stock, { stock = it }, label = { Text("Stock") }); if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error) } }, confirmButton = { Button(onClick = { scope.launch { busy = true; try { api.createSellerProduct(name, category, (price.toDouble() * 100).toLong(), description, stock.toIntOrNull() ?: 0, false); onDone() } catch (t: Throwable) { error = t.message ?: "Unable to create product" } finally { busy = false } } }, enabled = !busy && name.isNotBlank() && category.isNotBlank() && price.toDoubleOrNull()?.let { it > 0 } == true && description.isNotBlank()) { Text("Save draft") } }, dismissButton = { TextButton(onClick = onDone, enabled = !busy) { Text("Close") } })
}
