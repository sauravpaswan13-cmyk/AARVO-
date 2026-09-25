package com.aarvo

// CI verification marker: account settings UI update requires Android build validation.

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aarvo.cart.CartViewModel
import com.aarvo.cart.SaveForLaterStore
import com.aarvo.data.Product
import com.aarvo.network.AarvoApiClient
import com.aarvo.payment.PaymentBridge
import com.aarvo.ui.theme.AarvoTheme
import com.aarvo.wishlist.WishlistStore
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    private var razorpayCheckout: Checkout? = null
    private var paymentCallback: ((String?, String?) -> Unit)? = null
    private val authRefresh = mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AarvoTheme {
                AarvoRoot(this@MainActivity, applicationContext, authRefresh.intValue)
            }
        }
    }
    override fun onResume() { super.onResume(); authRefresh.intValue++ }
    fun startRazorpayPayment(options: JSONObject, callback: (String?, String?) -> Unit) { PaymentBridge.clear(); paymentCallback = callback; try { val checkout = Checkout(); razorpayCheckout = checkout; checkout.setKeyID(options.getString("key")); checkout.open(this, options) } catch (t: Throwable) { paymentCallback = null; callback(null, t.message ?: "Unable to open payment checkout") } }
    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) { PaymentBridge.capture(paymentData); val callback = paymentCallback; paymentCallback = null; callback?.invoke(razorpayPaymentId, null) }
    override fun onPaymentError(code: Int, description: String?, paymentData: PaymentData?) { PaymentBridge.capture(paymentData); val callback = paymentCallback; paymentCallback = null; callback?.invoke(null, description ?: "Payment failed (code $code)") }
}

@Composable private fun AarvoRoot(activity: MainActivity, context: Context, authRefresh: Int) {
    val prefs = remember { context.getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE) }
    val wishlistStore = remember { WishlistStore(prefs) }
    val saveForLaterStore = remember { SaveForLaterStore(prefs) }
    var onboarded by remember { mutableStateOf(prefs.getBoolean("onboarded", false)) }
    var signedIn by remember(authRefresh) { mutableStateOf(prefs.getBoolean("signed_in", false)) }
    var guestMode by remember(authRefresh) { mutableStateOf(prefs.getBoolean("guest_mode", false)) }
    var userName by remember(authRefresh) { mutableStateOf(prefs.getString("user_name", "") ?: "") }
    var role by remember(authRefresh) { mutableStateOf(prefs.getString("user_role", "BUYER") ?: "BUYER") }
    val api = remember { AarvoApiClient { prefs.getString("auth_token", null) } }
    val openOtpLogin = { prefs.edit().putBoolean("onboarded", true).apply(); activity.startActivity(Intent(activity, PhoneAuthActivity::class.java)) }
    LaunchedEffect(signedIn, role) {
        if (signedIn && role == "ADMIN") activity.startActivity(Intent(activity, AdminDashboardActivity::class.java))
        if (signedIn && role == "RIDER") activity.startActivity(Intent(activity, RiderDashboardActivity::class.java))
    }
    when {
        guestMode -> GuestHomeScreen(api = api, onLogin = openOtpLogin, onExitGuest = { prefs.edit().putBoolean("guest_mode", false).apply(); guestMode = false })
        signedIn && role != "ADMIN" && role != "RIDER" -> AarvoApp(userName, role, api, activity, wishlistStore, saveForLaterStore, false, openOtpLogin, { prefs.edit().putBoolean("signed_in", false).putBoolean("guest_mode", false).remove("auth_token").remove("user_role").apply(); signedIn = false; guestMode = false })
        !onboarded -> OnboardingScreen(
            onLogin = openOtpLogin,
            onGuest = { prefs.edit().putBoolean("onboarded", true).putBoolean("guest_mode", true).putBoolean("signed_in", false).putString("user_role", "BUYER").remove("auth_token").apply(); onboarded = true; signedIn = false; role = "BUYER"; guestMode = true }
        )
        else -> OnboardingScreen(onLogin = openOtpLogin, onGuest = { prefs.edit().putBoolean("onboarded", true).putBoolean("guest_mode", true).putBoolean("signed_in", false).putString("user_role", "BUYER").remove("auth_token").apply(); onboarded = true; signedIn = false; role = "BUYER"; guestMode = true })
    }
}

@Composable private fun OnboardingScreen(onLogin: () -> Unit, onGuest: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        scheme.primaryContainer,
                        scheme.background,
                        scheme.secondaryContainer.copy(alpha = 0.45f)
                    )
                )
            )
            .padding(horizontal = 22.dp, vertical = 28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                color = scheme.surface.copy(alpha = 0.97f),
                tonalElevation = 8.dp,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(92.dp),
                        shape = CircleShape,
                        color = scheme.primaryContainer,
                        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.16f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.aarvo_logo),
                                contentDescription = "AARVO logo",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(68.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "AARVO",
                        color = scheme.primary,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Shop smart. Live better.",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Discover products, explore freely, and sign in only when you need your AARVO account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onLogin,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null)
                        Spacer(Modifier.size(9.dp))
                        Text("Login / Sign Up", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    androidx.compose.material3.OutlinedButton(
                        onClick = onGuest,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, scheme.primary.copy(alpha = 0.55f))
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(Modifier.size(9.dp))
                        Text("Continue as Guest", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SECURE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = scheme.primary)
                        Text("  •  ", fontSize = 10.sp, color = scheme.onSurfaceVariant)
                        Text("SIMPLE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = scheme.primary)
                        Text("  •  ", fontSize = 10.sp, color = scheme.onSurfaceVariant)
                        Text("AARVO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = scheme.primary)
                    }
                }
            }
        }
    }
}

@Composable private fun GuestHomeScreen(api: AarvoApiClient, onLogin: () -> Unit, onExitGuest: () -> Unit) {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    LaunchedEffect(api) {
        loading = true
        error = ""
        try {
            products = api.products("", "All").toProductList().distinctBy { it.id }
        } catch (t: Throwable) {
            products = emptyList()
            error = "Products are temporarily unavailable."
        } finally {
            loading = false
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AARVO") },
                actions = { TextButton(onClick = onLogin) { Text("Login / Sign Up") } }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Home, "Home") }, label = { Text("Home") })
                NavigationBarItem(selected = false, onClick = onLogin, icon = { Icon(Icons.Default.Person, "Account") }, label = { Text("Account") })
                NavigationBarItem(selected = false, onClick = onLogin, icon = { Icon(Icons.Default.ShoppingCart, "Cart") }, label = { Text("Cart") })
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Welcome to AARVO", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Browse as Guest", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Login / Sign Up to Buy") }
            }
            if (loading) item { CircularProgressIndicator() }
            if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }
            items(products, key = { it.id }) { product ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(product.name, fontWeight = FontWeight.SemiBold)
                        Text(formatPaise(product.pricePaise), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            item {
                TextButton(onClick = onExitGuest, modifier = Modifier.fillMaxWidth()) { Text("Back to Login / Guest") }
            }
        }
    }
}

@Composable private fun SignInScreen(api: AarvoApiClient, onSignedIn: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var seller by remember { mutableStateOf(false) }; var phone by remember { mutableStateOf("") }; var registerMode by remember { mutableStateOf(false) }; var loading by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope()
    val loginPhoneValid = Regex("^[6-9][0-9]{9}$").matches(email.trim())
    val registrationEmailValid = email.trim().isBlank() || email.trim().contains('@')
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(if (registerMode) "Create your AARVO account" else "Welcome to AARVO", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(16.dp))
        if (registerMode) { OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Full name") }); Spacer(Modifier.height(10.dp)) }
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(if (registerMode) "Email (optional)" else "Mobile number") }); Spacer(Modifier.height(10.dp)); OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), label = { Text("Password (8+ characters)") })
        if (registerMode) { Spacer(Modifier.height(10.dp)); OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Phone for OTP verification") }); TextButton(onClick = { seller = !seller }) { Text(if (seller) "✓ Register as seller" else "Register as buyer") } }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp))
        val canSubmit = !loading && password.length >= 8 && if (registerMode) name.isNotBlank() && registrationEmailValid && phone.trim().length >= 10 else loginPhoneValid
        Button(onClick = { loading = true; error = ""; scope.launch { try { val result = if (registerMode) api.register(email, password, name, if (seller) "SELLER" else "BUYER", phone) else api.login(email, password); val user = result.getJSONObject("user"); onSignedIn(user.optString("display_name", name), result.getString("token"), user.optString("role", "BUYER")) } catch (t: Throwable) { error = t.message ?: "Unable to connect to AARVO server." } finally { loading = false } } }, enabled = canSubmit, modifier = Modifier.fillMaxWidth()) { if (loading) CircularProgressIndicator() else Text(if (registerMode) "Create account" else "Sign in") }
        TextButton(onClick = { registerMode = !registerMode; error = "" }) { Text(if (registerMode) "Already have an account? Sign in" else "New to AARVO? Create account") }; if (!api.isConfigured()) Text("Live API is not configured in this build. No demo account is used.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun AarvoApp(userName: String, role: String, api: AarvoApiClient, activity: MainActivity, wishlistStore: WishlistStore, saveForLaterStore: SaveForLaterStore, guestMode: Boolean, onLogin: () -> Unit, onSignOut: () -> Unit) {
    // Keep CartViewModel creation inside the composable. This avoids a composable remember() in a default parameter during the Guest entry transition.
    val cartViewModel = remember { CartViewModel(activity.applicationContext.getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE)) }
    var selectedTab by remember { mutableIntStateOf(0) }; var query by remember { mutableStateOf("") }; var category by remember { mutableStateOf("All") }; var sortMode by remember { mutableStateOf("Relevance") }; var minRating by remember { mutableStateOf(0.0) }; var maxPrice by remember { mutableStateOf<Long?>(null) }; var inStockOnly by remember { mutableStateOf(false) }; var showFilters by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }; var wishlist by remember { mutableStateOf(wishlistStore.load()) }; var recentlyViewed by remember { mutableStateOf<List<Product>>(emptyList()) }
    var saveForLater by remember { mutableStateOf(saveForLaterStore.load()) }; var showCheckout by remember { mutableStateOf(false) }; var showLoginRequired by remember { mutableStateOf(false) }; var checkoutLoading by remember { mutableStateOf(false) }; var checkoutMessage by remember { mutableStateOf("") }; var products by remember { mutableStateOf<List<Product>>(emptyList()) }; var allProducts by remember { mutableStateOf<List<Product>>(emptyList()) }; var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf("") }; val cartItems by cartViewModel.items.collectAsState(); val scope = rememberCoroutineScope()
    LaunchedEffect(api, guestMode, role) {
        try {
            allProducts = api.products("", "All").toProductList()
            cartViewModel.restore(allProducts)
            if (!guestMode && role == "BUYER") {
                val server = api.serverCart()
                if (server.length() > 0) {
                    allProducts.forEach { product ->
                        val row = (0 until server.length()).asSequence()
                            .map { server.getJSONObject(it) }
                            .firstOrNull { it.optInt("productId") == product.id }
                        cartViewModel.setQuantity(product, row?.optInt("quantity", 0) ?: 0)
                    }
                } else {
                    cartViewModel.distinctItems().forEach { product ->
                        api.setServerCartItem(product.id, cartViewModel.quantity(product.id))
                    }
                }
            }
        } catch (_: Throwable) {
            allProducts = emptyList()
        }
    }
    LaunchedEffect(api, guestMode, role, allProducts) {
        if (!guestMode && role == "BUYER" && allProducts.isNotEmpty()) {
            recentlyViewed = runCatching {
                val rows = api.recentlyViewed()
                val ids = (0 until rows.length()).map { rows.getJSONObject(it).optInt("productId", rows.getJSONObject(it).optInt("product_id", 0)) }.filter { it > 0 }
                ids.mapNotNull { id -> allProducts.firstOrNull { it.id == id } }.distinctBy { it.id }.take(10)
            }.getOrDefault(emptyList())
        } else recentlyViewed = emptyList()
    }
    LaunchedEffect(query, category, api) { loading = true; error = ""; try { products = api.products(query, category).toProductList() } catch (t: Throwable) { products = emptyList(); error = t.message ?: "Unable to load products." } finally { loading = false } }
    val visibleProducts = remember(products, sortMode, minRating, maxPrice, inStockOnly) { products.filter { (minRating <= 0.0 || it.rating >= minRating) && (maxPrice == null || it.pricePaise <= maxPrice!!) && (!inStockOnly || it.stockQuantity > 0) }.let { list -> when (sortMode) { "Price: Low to High" -> list.sortedBy { it.pricePaise }; "Price: High to Low" -> list.sortedByDescending { it.pricePaise }; "Rating: High to Low" -> list.sortedByDescending { it.rating }; else -> list } } }
    fun syncAuthenticatedCart() {
        if (guestMode || role != "BUYER") return
        scope.launch {
            runCatching {
                val server = api.serverCart()
                val localIds = cartViewModel.distinctItems().map { it.id }.toSet()
                for (i in 0 until server.length()) {
                    val row = server.getJSONObject(i)
                    val id = row.optInt("productId")
                    if (id > 0 && id !in localIds) api.removeServerCartItem(id)
                }
                cartViewModel.distinctItems().forEach { product ->
                    api.setServerCartItem(product.id, cartViewModel.quantity(product.id))
                }
            }
        }
    }
    val availableCategories = remember(allProducts) { listOf("All") + allProducts.map { it.category.trim() }.filter { it.isNotBlank() }.distinct().sorted() }
    if (showFilters) ProductFilterDialog(sortMode, minRating, maxPrice, inStockOnly, { sortMode = it; showFilters = false }, { minRating = it }, { maxPrice = it }, { inStockOnly = it }, { showFilters = false })
    if (selectedProduct != null) { val product = selectedProduct!!; ProductDetailsScreen(product, product.id in wishlist, { selectedProduct = null }, { wishlist = wishlistStore.toggle(product.id) }, cartViewModel::add); return }
    if (showLoginRequired) LoginRequiredDialog({ showLoginRequired = false; onLogin() }, { showLoginRequired = false })
    if (showCheckout) CheckoutDialog(api, cartItems.sumOf { it.pricePaise }, checkoutLoading, checkoutMessage, { if (!checkoutLoading) showCheckout = false }) { fullName, phone, line1, city, state, postalCode ->
        checkoutLoading = true; checkoutMessage = "Creating secure order..."; scope.launch { try { val items = JSONArray().apply { cartViewModel.distinctItems().forEach { product -> put(JSONObject().put("productId", product.id).put("quantity", cartViewModel.quantity(product.id))) } }; val address = JSONObject().apply { put("fullName", fullName.trim()); put("phone", phone.trim()); put("line1", line1.trim()); put("line2", ""); put("city", city.trim()); put("state", state.trim()); put("postalCode", postalCode.trim()); put("country", "IN") }; val order = api.createOrder(items, address); val options = JSONObject().apply { put("key", order.getString("keyId")); put("amount", order.getLong("amountPaise")); put("currency", order.getString("currency")); put("name", "AARVO"); put("description", "AARVO marketplace order"); put("order_id", order.getString("gatewayOrderId")); put("prefill", JSONObject().put("name", fullName.trim()).put("contact", phone.trim())); put("notes", JSONObject().put("order_id", order.getString("orderId"))) }; if (order.optString("paymentMode") == "TEST") { checkoutMessage = "Test checkout complete. Order confirmed."; cartViewModel.clear(); showCheckout = false; checkoutLoading = false } else { checkoutMessage = "Opening secure payment..."; activity.startRazorpayPayment(options) { paymentId, paymentError -> scope.launch { if (paymentId != null) { val signature = PaymentBridge.lastSignature; val gatewayOrderId = PaymentBridge.lastOrderId ?: order.getString("gatewayOrderId"); if (!signature.isNullOrBlank()) { try { api.verifyPayment(order.getString("orderId"), paymentId, gatewayOrderId, signature); checkoutMessage = "Payment verified. Order confirmed."; cartViewModel.clear(); showCheckout = false } catch (t: Throwable) { checkoutMessage = t.message ?: "Payment verification failed. Order was not confirmed." } } else checkoutMessage = "Payment completed but verification data was missing. Order remains unconfirmed." } else { checkoutMessage = paymentError ?: "Payment cancelled or failed."; try { api.cancelOrder(order.getString("orderId"), "BUYER_PAYMENT_CANCELLED") } catch (_: Throwable) { } }; checkoutLoading = false; PaymentBridge.clear() } } } } catch (t: Throwable) { checkoutLoading = false; checkoutMessage = t.message ?: "Unable to create order." } } }
    Scaffold(topBar = { AarvoHomeTopBar(query, { query = it }, cartItems.size) { selectedTab = 1 } }, bottomBar = { NavigationBar { NavigationBarItem(selectedTab == 0, { selectedTab = 0 }, { Icon(Icons.Default.Home, "Home") }, label = { Text("Home") }); NavigationBarItem(selectedTab == 1, { selectedTab = 1 }, { BadgedBox(badge = { if (cartItems.isNotEmpty()) Badge { Text(cartItems.size.toString()) } }) { Icon(Icons.Default.ShoppingCart, "Cart") } }, label = { Text("Cart") }); NavigationBarItem(selectedTab == 2, { selectedTab = 2 }, { Icon(Icons.Default.Favorite, "Wishlist") }, label = { Text("Wishlist") }); NavigationBarItem(selectedTab == 3, { selectedTab = 3 }, { Icon(Icons.Default.Person, "Account") }, label = { Text("Account") }) } }) { padding -> when (selectedTab) {
        0 -> if (guestMode) GuestSafeHome(padding, query, { query = it }, visibleProducts, loading, error, cartViewModel::add, { selectedProduct = it }) else HomeScreen(padding, api, query, { query = it }, availableCategories, category, { category = it }, visibleProducts, recentlyViewed, loading, error, cartViewModel::add, { selectedProduct = it }, wishlist, { id -> wishlist = wishlistStore.toggle(id) }, { showFilters = true }, sortMode, minRating, maxPrice, inStockOnly)
        1 -> CartScreen(padding, cartItems, allProducts, saveForLater, { product -> cartViewModel.increment(product); syncAuthenticatedCart() }, { product -> cartViewModel.decrement(product); syncAuthenticatedCart() }, { id -> cartViewModel.removeAll(id); syncAuthenticatedCart() }, cartViewModel::quantity, { cartViewModel.clear(); if (!guestMode && role == "BUYER") scope.launch { runCatching { api.clearServerCart() } } }, { product -> cartViewModel.removeAll(product.id); saveForLater = saveForLaterStore.toggle(product.id); syncAuthenticatedCart() }, { product -> saveForLater = saveForLaterStore.remove(product.id); cartViewModel.add(product); syncAuthenticatedCart() }, { id -> saveForLater = saveForLaterStore.remove(id) }) { if (guestMode) showLoginRequired = true else { showCheckout = true; checkoutMessage = "" } }
        2 -> WishlistScreen(padding, allProducts, wishlist, { id -> wishlist = wishlistStore.toggle(id) }, { selectedProduct = it }, cartViewModel::add)
        else -> AccountScreen(padding, userName, role, api, activity, guestMode, onLogin, onSignOut, { selectedTab = 2 }, { /* notifications opened from account can be added without leaving account */ })
    } } }

@Composable private fun NotificationsScreen(padding: PaddingValues, api: AarvoApiClient, onBack: () -> Unit) {
    var items by remember { mutableStateOf<List<JSONObject>>(emptyList()) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope()
    fun reload() { scope.launch { try { val a=api.notifications(); items=buildList { for(i in 0 until a.length()) add(a.getJSONObject(i)) } } catch(t:Throwable){ error=t.message ?: "Unable to load notifications" } } }
    LaunchedEffect(Unit){ reload() }
    LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Back")};Text("Notifications",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}}
        if(error.isNotBlank()) item{Text(error,color=MaterialTheme.colorScheme.error)}
        if(items.isEmpty() && error.isBlank()) item{Text("No new notifications.")}
        items(items.distinctBy { it.optLong("id") },key={it.optLong("id")}){n->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(n.optString("title").ifBlank{"AARVO Update"},fontWeight=FontWeight.Bold);Text(n.optString("message").ifBlank{n.optString("body")});if(!n.optBoolean("read"))TextButton(onClick={scope.launch{runCatching{api.markNotificationRead(n.optLong("id"))};reload()}}){Text("Mark as read")}}}}
    }
}

@Composable private fun LoginRequiredDialog(onLogin: () -> Unit, onDismiss: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text("Login Required") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("To complete your purchase, please login or create an account."); Text("You can still browse and add to cart.", style = MaterialTheme.typography.bodySmall) } }, confirmButton = { Button(onClick = onLogin) { Text("Login / Sign Up") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Continue Browsing") } }) }

@Composable private fun ProductFilterDialog(sortMode: String, minRating: Double, maxPrice: Long?, inStockOnly: Boolean, onSort: (String) -> Unit, onRating: (Double) -> Unit, onMaxPrice: (Long?) -> Unit, onStock: (Boolean) -> Unit, onClose: () -> Unit) { var priceText by remember(maxPrice) { mutableStateOf(if (maxPrice == null) "" else (maxPrice!! / 100).toString()) }; AlertDialog(onDismissRequest = onClose, title = { Text("Filters & Sorting") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Sort", fontWeight = FontWeight.Bold); listOf("Relevance", "Price: Low to High", "Price: High to Low", "Rating: High to Low").forEach { option -> TextButton(onClick = { onSort(option) }) { Text(if (sortMode == option) "✓ $option" else option) } }; Text("Minimum rating: ${if (minRating == 0.0) "Any" else "${minRating}★"}", fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf(0.0, 3.0, 4.0, 4.5).forEach { r -> TextButton(onClick = { onRating(r) }) { Text(if (minRating == r) "✓ ${if (r == 0.0) "Any" else r.toString()}★" else if (r == 0.0) "Any" else "${r}★") } } }; OutlinedTextField(priceText, { priceText = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Maximum price ₹ (optional)") }); TextButton(onClick = { onMaxPrice(priceText.trim().toLongOrNull()?.times(100)); onClose() }) { Text("Apply price") }; TextButton(onClick = { onStock(!inStockOnly) }) { Text(if (inStockOnly) "✓ In stock only" else "In stock only") } } }, confirmButton = { Button(onClick = onClose) { Text("Done") } }) }

private fun JSONArray.toProductList(): List<Product> = buildList { for (i in 0 until length()) { val o = getJSONObject(i); val pricePaise = o.getLong("price_paise"); add(Product(o.getLong("id").toInt(), o.getString("seller_id"), o.getString("seller_name"), o.getString("name"), o.getString("category"), (pricePaise / 100L).toInt(), o.optDouble("rating", 0.0), "🛍️", o.getString("description"), o.getInt("stock_quantity"), o.optBoolean("is_published", true), pricePaise)) } }.distinctBy { it.id }

@Composable private fun WishlistScreen(padding: PaddingValues, products: List<Product>, wishlist: Set<Int>, onToggle: (Int) -> Unit, onOpen: (Product) -> Unit, onAdd: (Product) -> Unit) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val saved = products.filter { it.id in wishlist }
    Column(Modifier.fillMaxSize().padding(padding)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("My Wishlist", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("${saved.size} saved products") }
            Button(onClick = {
                context.getSharedPreferences("aarvo_compare", Context.MODE_PRIVATE).edit().putString("product_ids", selected.joinToString(",")).apply()
                context.startActivity(Intent(context, ProductCompareActivity::class.java))
            }, enabled = selected.size in 2..3) { Text("Compare (${selected.size})") }
        }
        if (saved.isEmpty()) {
            Text("No saved products yet. Tap the heart on any product to save it.", Modifier.padding(16.dp))
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(saved.distinctBy { it.id }, key = { it.id }) { product ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = product.id in selected, onCheckedChange = { checked ->
                                    selected = if (checked && selected.size < 3) selected + product.id else selected - product.id
                                })
                                Column(Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Bold)
                                    Text(product.displayPrice + " • ⭐ " + product.rating)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = { onOpen(product) }) { Text("View") }
                                TextButton(onClick = { onAdd(product) }) { Text("Add to cart") }
                                TextButton(onClick = { onToggle(product.id); selected = selected - product.id }) { Text("Remove") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun GuestSafeHome(
    padding: PaddingValues,
    query: String,
    onQueryChange: (String) -> Unit,
    products: List<Product>,
    loading: Boolean,
    error: String,
    onAdd: (Product) -> Unit,
    onOpen: (Product) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Welcome to AARVO", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text("Browse freely as Guest. Login is only needed for account features and checkout.", style = MaterialTheme.typography.bodyMedium)
                }
            }
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
        item { Text("Featured Products", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold) }
        if (loading) item { Box(Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }
        if (!loading && products.isEmpty() && error.isBlank()) item { Text("No products available right now.") }

        items(products.distinctBy { it.id }, key = { it.id }) { product ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(product.category, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(product.displayPrice, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    if (product.description.isNotBlank()) {
                        Text(product.description, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onOpen(product) }) { Text("View") }
                        Button(onClick = { onAdd(product) }) { Text("Add to Cart") }
                    }
                }
            }
        }
    }
}

@Composable private fun HomeScreen(padding: PaddingValues, api: AarvoApiClient, query: String, onQueryChange: (String) -> Unit, categories: List<String>, selectedCategory: String, onCategoryChange: (String) -> Unit, products: List<Product>, recentlyViewed: List<Product>, loading: Boolean, error: String, onAdd: (Product) -> Unit, onOpen: (Product) -> Unit, wishlist: Set<Int>, onToggleWishlist: (Int) -> Unit, onFilter: () -> Unit, sortMode: String, minRating: Double, maxPrice: Long?, inStockOnly: Boolean) {
    val scope = rememberCoroutineScope()
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Reference layout: sticky branded header/search lives in Scaffold; content scrolls below it.
        item { LiveHero(api = api, modifier = Modifier.fillMaxWidth()) }
        item { HomeCategoryGrid { chosen ->
            val match = categories.firstOrNull { it.equals(chosen, ignoreCase = true) }
            onCategoryChange(match ?: chosen)
        } }
        item { HomeDealsBanner() }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Deals & Products", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onFilter) { Text("Filters") }
            }
        }
        item { HomeProductDeals(products, onOpen, onAdd) }
        item { HomeMegaSaleBanner() }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("All Products", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (sortMode != "Relevance") Text("• " + sortMode, style = MaterialTheme.typography.bodySmall)
                if (minRating > 0) Text("• " + minRating + "★+", style = MaterialTheme.typography.bodySmall)
                if (maxPrice != null) Text("• ≤ ₹" + (maxPrice / 100), style = MaterialTheme.typography.bodySmall)
                if (inStockOnly) Text("• In stock", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onFilter) { Text("Filters & Sort") }
                if (sortMode != "Relevance") Text("• \$sortMode", style = MaterialTheme.typography.bodySmall)
                if (minRating > 0) Text("• \${minRating}★+", style = MaterialTheme.typography.bodySmall)
                if (maxPrice != null) Text("• ≤ ₹\${maxPrice / 100}", style = MaterialTheme.typography.bodySmall)
                if (inStockOnly) Text("• In stock", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (recentlyViewed.isNotEmpty()) {
            item {
                Text("Recently Viewed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(recentlyViewed.distinctBy { it.id }, key = { it.id }) { product ->
                        Card(onClick = { onOpen(product) }, modifier = Modifier.size(width = 190.dp, height = 150.dp)) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("${product.emoji}  ${product.name}", fontWeight = FontWeight.SemiBold, maxLines = 2)
                                Text(product.category, style = MaterialTheme.typography.bodySmall)
                                Text(product.displayPrice, fontWeight = FontWeight.Bold)
                                Text("View again", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
        if (loading) item { CircularProgressIndicator() }
        if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }
        if (!loading && error.isBlank() && products.isEmpty()) item { Text("No products match your current search or filters.") }
        items(products.distinctBy { it.id }, key = { it.id }) { product -> ProductCard(product, product.id in wishlist, onAdd, { scope.launch { runCatching { api.markProductViewed(product.id) }; onOpen(product) } }, onToggleWishlist) }
    }
}

@Composable private fun ProductCard(product: Product, isSaved: Boolean, onAdd: (Product) -> Unit, onOpen: (Product) -> Unit, onToggleWishlist: (Int) -> Unit) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${product.emoji}  ${product.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); IconButton(onClick = { onToggleWishlist(product.id) }) { Icon(if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Wishlist") } }; Text(product.category, style = MaterialTheme.typography.bodySmall); Text(product.displayPrice, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("★ ${product.rating}"); Text(product.description); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = { onOpen(product) }) { Text("View details") }; TextButton(onClick = { onAdd(product) }) { Text("Add to cart") } } } } }

@Composable private fun ProductDetailsScreen(product: Product, isSaved: Boolean, onBack: () -> Unit, onToggleWishlist: () -> Unit, onAdd: (Product) -> Unit) { Scaffold(topBar = { TopAppBar(title = { Text("Product details") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { padding -> Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("${product.emoji}  ${product.name}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(product.category); Text(product.displayPrice, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("★ ${product.rating}"); Text(product.description); Text("Stock available: ${product.stockQuantity}"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { onAdd(product) }) { Text("Add to cart") }; TextButton(onClick = onToggleWishlist) { Text(if (isSaved) "Remove from wishlist" else "Save to wishlist") } } } } }

@Composable private fun CartScreen(
    padding: PaddingValues,
    items: List<Product>,
    allProducts: List<Product>,
    saveForLater: Set<Int>,
    onIncrement: (Product) -> Unit,
    onDecrement: (Product) -> Unit,
    onRemoveAll: (Int) -> Unit,
    quantityOf: (Int) -> Int,
    onClear: () -> Unit,
    onSaveForLater: (Product) -> Unit,
    onMoveToCart: (Product) -> Unit,
    onRemoveSaved: (Int) -> Unit,
    onCheckout: () -> Unit
) {
    val totalPaise = items.sumOf { product -> product.pricePaise * quantityOf(product.id) }
    val groupedItems = items.distinctBy { it.id }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Your Cart",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (items.isNotEmpty()) {
                    TextButton(onClick = onClear) { Text("Clear") }
                }
            }
        }

        if (groupedItems.isEmpty()) {
            item { Text("Your cart is empty. Add something you like from Home.") }
        } else {
            items(groupedItems, key = { it.id }) { product ->
                val quantity = quantityOf(product.id)
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.SemiBold)
                                Text(product.displayPrice)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { onSaveForLater(product) }) {
                                    Text("Save for later")
                                }
                                IconButton(onClick = { onRemoveAll(product.id) }) {
                                    Icon(Icons.Default.Delete, "Remove all")
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onDecrement(product) },
                                enabled = quantity > 0
                            ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                            Text(
                                quantity.toString(),
                                Modifier.padding(horizontal = 12.dp),
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { onIncrement(product) },
                                enabled = quantity < product.stockQuantity
                            ) { Text("+") }
                        }
                        Text("Subtotal: ${formatPaise(product.pricePaise * quantity)}")
                    }
                }
            }
        }

        item {
            Text(
                "Saved for later",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            val savedProducts = allProducts.filter { it.id in saveForLater }
            if (savedProducts.isEmpty()) {
                Text("No saved items yet.")
            } else {
                savedProducts.forEach { product ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.SemiBold)
                                Text(product.displayPrice)
                                Text(
                                    "Saved for later",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(onClick = { onMoveToCart(product) }) {
                                Text("Move to cart")
                            }
                            IconButton(onClick = { onRemoveSaved(product.id) }) {
                                Icon(Icons.Default.Delete, "Remove saved item")
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Total: ${formatPaise(totalPaise)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onCheckout,
                modifier = Modifier.fillMaxWidth(),
                enabled = items.isNotEmpty()
            ) {
                Text("Proceed to secure checkout")
            }
        }
    }
}

@Composable private fun CheckoutDialog(api: AarvoApiClient, totalPaise: Long, loading: Boolean, message: String, onDismiss: () -> Unit, onPlaceOrder: (String, String, String, String, String, String) -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var line1 by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var savedAddresses by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selectedAddressId by remember { mutableStateOf("") }
    var addressLoading by remember { mutableStateOf(true) }
    fun applyAddress(address: JSONObject) {
        selectedAddressId = address.optString("id")
        fullName = address.optString("full_name", address.optString("fullName"))
        phone = address.optString("phone")
        line1 = address.optString("line1")
        city = address.optString("city")
        state = address.optString("state")
        postalCode = address.optString("postal_code", address.optString("postalCode"))
    }
    LaunchedEffect(api) {
        addressLoading = true
        try {
            val response = api.addresses()
            savedAddresses = buildList { for (i in 0 until response.length()) add(response.getJSONObject(i)) }
            savedAddresses.firstOrNull { it.optBoolean("is_default", false) || it.optBoolean("isDefault", false) }?.let(::applyAddress)
        } catch (_: Throwable) { savedAddresses = emptyList() }
        finally { addressLoading = false }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Secure checkout") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cart value: ${formatPaise(totalPaise)}", fontWeight = FontWeight.Bold)
            if (addressLoading) Text("Loading saved delivery addresses...")
            if (savedAddresses.isNotEmpty()) {
                Text("Saved delivery addresses", fontWeight = FontWeight.SemiBold)
                savedAddresses.forEach { address ->
                    val id = address.optString("id")
                    val label = address.optString("label").ifBlank { "Saved address" }
                    val name = address.optString("full_name", address.optString("fullName"))
                    val addressLine = listOf(address.optString("line1"), address.optString("city"), address.optString("state"), address.optString("postal_code", address.optString("postalCode"))).filter { it.isNotBlank() }.joinToString(", ")
                    TextButton(onClick = { applyAddress(address) }, modifier = Modifier.fillMaxWidth()) { Text(if (selectedAddressId == id) "✓ $label — $name, $addressLine" else "$label — $name, $addressLine") }
                }
                Text("You can edit the selected address below before payment.", style = MaterialTheme.typography.bodySmall)
            } else Text("No saved address yet. Enter your delivery address below; you can save addresses from My Account.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(fullName, { fullName = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Full name") })
            OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Phone") })
            OutlinedTextField(line1, { line1 = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Address") })
            OutlinedTextField(city, { city = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("City") })
            OutlinedTextField(state, { state = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("State") })
            OutlinedTextField(postalCode, { postalCode = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("PIN code") })
            if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
            Text("Payment is processed by Razorpay. AARVO verifies it on the server before confirming the order.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { Button(onClick = { onPlaceOrder(fullName, phone, line1, city, state, postalCode) }, enabled = !loading && fullName.isNotBlank() && Regex("^[6-9][0-9]{9}$").matches(phone.trim()) && line1.isNotBlank() && city.isNotBlank() && state.isNotBlank() && Regex("^[0-9]{6}$").matches(postalCode.trim())) { if (loading) CircularProgressIndicator() else Text("Pay securely") } }, dismissButton = { TextButton(onClick = onDismiss, enabled = !loading) { Text("Close") } })
}

@Composable private fun AccountScreen(padding: PaddingValues, userName: String, role: String, api: AarvoApiClient, activity: MainActivity, guestMode: Boolean, onLogin: () -> Unit, onSignOut: () -> Unit, onWishlist: () -> Unit, onNotifications: () -> Unit) {
    var section by remember { mutableStateOf("account") }
    var showProfile by remember { mutableStateOf(false) }; var showSupport by remember { mutableStateOf(false) }
    if (showProfile) ProfileDialog(api) { showProfile = false }; if (showSupport) SupportDialog(api) { showSupport = false }
    when (section) {
        "orders" -> OrdersScreen(padding, api) { section = "account" }
        "seller" -> SellerDashboardScreen(padding, api) { section = "account" }
        else -> {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(56.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                                    }
                                }
                                Spacer(Modifier.size(14.dp))
                                Column {
                                    Text(
                                        if (guestMode) "Welcome to AARVO" else userName.ifBlank { "AARVO Customer" },
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        if (guestMode) "Login to manage your account" else "Manage your AARVO account",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (guestMode) {
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
                                    Text("Login / Sign Up with OTP", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (!guestMode) {
                    item {
                        Text("My Account", Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    item {
                        AccountOptionRow(Icons.Default.Person, "My Profile", "Personal details and account information") { showProfile = true }
                    }
                    item {
                        AccountOptionRow(Icons.Default.ShoppingCart, "My Orders & Tracking", "View orders, delivery status and order history") { section = "orders" }
                    }
                    item {
                        AccountOptionRow(Icons.Default.Home, "Saved Addresses", "Add, edit and manage delivery addresses") {
                            activity.startActivity(Intent(activity, AddressBookActivity::class.java))
                        }
                    }
                    item {
                        AccountOptionRow(Icons.Default.Favorite, "Wishlist", "Your saved products") { onWishlist() }
                    }
                    item {
                        Text("Sell on AARVO", Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    item {
                        AccountOptionRow(Icons.Default.Person, "Become a Seller", "Create your store and start selling") {
                            activity.startActivity(Intent(activity, SellerAccountActivity::class.java))
                        }
                    }
                    if (role == "SELLER") {
                        item {
                            AccountOptionRow(Icons.Default.ShoppingCart, "Seller Dashboard", "Products, inventory, orders and fulfilment") { section = "seller" }
                        }
                        item {
                            AccountOptionRow(Icons.Default.Person, "Seller Business Onboarding", "Business, tax, pickup and return details") {
                                activity.startActivity(Intent(activity, SellerOnboardingActivity::class.java))
                            }
                        }
                    }
                    item {
                        Text("More", Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    item {
                        AccountOptionRow(Icons.Default.Person, "Help & Support", "Get help with your AARVO orders and account") { showSupport = true }
                    }
                    item {
                        AccountOptionRow(Icons.Default.Home, "Payment & Security", "Secure checkout and account protection") { }
                    }
                    item {
                        androidx.compose.material3.Divider()
                        TextButton(
                            onClick = onSignOut,
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text("Sign out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    item {
                        androidx.compose.material3.Divider()
                        Text("Guest Account", Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    item {
                        AccountOptionRow(Icons.Default.ShoppingCart, "Cart & Shopping", "Browse products and keep items in your cart") { }
                    }
                    item {
                        AccountOptionRow(Icons.Default.Home, "Delivery Addresses", "Login when you are ready to purchase") { onLogin() }
                    }
                }
            }
        }
    }
}

@Composable private fun SupportDialog(api: AarvoApiClient, onDone: () -> Unit) {
    var subject by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!busy) onDone() }, title = { Text("AARVO Help & Support") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(subject, { subject = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Subject") })
            OutlinedTextField(details, { details = it }, Modifier.fillMaxWidth(), minLines = 4, label = { Text("Describe your issue") })
            if (message.isNotBlank()) Text(message, color = if (message.startsWith("Ticket")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        } },
        confirmButton = { Button(onClick = {
            scope.launch { busy = true; message = ""; try { val result = api.createSupportTicket(subject, details); message = "Ticket #" + result.optString("id") + " created successfully." } catch (t: Throwable) { message = t.message ?: "Unable to create support ticket" } finally { busy = false } }
        }, enabled = !busy && subject.trim().isNotBlank() && details.trim().length >= 5) { Text(if (busy) "Sending..." else "Create Ticket") } },
        dismissButton = { TextButton(onClick = onDone, enabled = !busy) { Text("Close") } })
}

@Composable private fun AccountOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    androidx.compose.material3.Divider()
}

@Composable private fun ProfileDialog(api: AarvoApiClient, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }; var saving by remember { mutableStateOf(false) }; var message by remember { mutableStateOf("") }; val scope=rememberCoroutineScope()
    LaunchedEffect(Unit){ try { val p=api.profile(); name=p.optString("display_name"); email=p.optString("email"); phone=p.optString("phone") } catch(t:Throwable){ message=t.message ?: "Unable to load profile" } finally { loading=false } }
    AlertDialog(onDismissRequest={if(!saving)onDone()},title={Text("My Profile",fontWeight=FontWeight.Bold)},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        if(loading) CircularProgressIndicator()
        OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("Full name")},singleLine=true)
        OutlinedTextField(email,{email=it},Modifier.fillMaxWidth(),label={Text("Email (optional)")},singleLine=true)
        OutlinedTextField(phone,{},Modifier.fillMaxWidth(),label={Text("Verified mobile")},singleLine=true,enabled=false)
        if(message.isNotBlank()) Text(message,color=MaterialTheme.colorScheme.error)
    }},confirmButton={Button(onClick={scope.launch{saving=true;message="";try{api.updateProfile(name,email);onDone()}catch(t:Throwable){message=t.message?:"Unable to save profile"}finally{saving=false}}},enabled=!loading&&!saving&&name.isNotBlank()){Text(if(saving)"Saving..." else "Save changes")}},dismissButton={TextButton(onClick=onDone,enabled=!saving){Text("Close")}})}

@Composable private fun OrdersScreen(padding: PaddingValues, api: AarvoApiClient, onBack: () -> Unit) { var orders by remember { mutableStateOf<List<JSONObject>>(emptyList()) }; var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope(); fun reload() { scope.launch { loading = true; error = ""; try { val a = api.orders(); orders = buildList { for (i in 0 until a.length()) add(a.getJSONObject(i)) } } catch (t: Throwable) { error = t.message ?: "Unable to load orders" } finally { loading = false } } }; LaunchedEffect(Unit) { reload() }; Scaffold(topBar = { TopAppBar(title = { Text("My Orders") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { inner -> LazyColumn(Modifier.fillMaxSize().padding(inner), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { if (loading) item { CircularProgressIndicator() }; if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }; if (!loading && orders.isEmpty()) item { Text("No orders yet.") }; items(orders.distinctBy { it.optString("id") }, key = { it.optString("id") }) { order -> OrderCard(order, api, ::reload) } } } }

@Composable private fun OrderCard(order: JSONObject, api: AarvoApiClient, reload: () -> Unit) { var busy by remember { mutableStateOf(false) }; var detail by remember { mutableStateOf<JSONObject?>(null) }; var actionError by remember { mutableStateOf("") }; var reviewOpen by remember { mutableStateOf(false) }; var disputeOpen by remember { mutableStateOf(false) }; var returnOpen by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope(); val status = order.optString("status", "PENDING"); val payment = order.optString("payment_status", "PENDING"); val id = order.optString("id"); if (reviewOpen) ReviewDialog(api, id, detail, { reviewOpen = false; reload() }); if (disputeOpen) DisputeDialog(api, id, { disputeOpen = false; reload() }); if (returnOpen) ReturnDialog(api, id, { returnOpen = false; reload() }); Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Order #$id", fontWeight = FontWeight.Bold); Text(formatPaise(order.optLong("total_paise", 0L)), style = MaterialTheme.typography.titleLarge); Text("Payment: $payment"); Text("Status: $status"); order.optJSONObject("tracking_json")?.let { Text("Tracking: ${it.optString("status", "Not updated")} ${it.optString("carrier", "")}") }; if (actionError.isNotBlank()) Text(actionError, color = MaterialTheme.colorScheme.error); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = { scope.launch { busy = true; actionError = ""; try { detail = api.order(id) } catch (t: Throwable) { actionError = t.message ?: "Unable to load order details" } finally { busy = false } } }, enabled = !busy) { Text(if (busy) "Loading..." else "View details") }; if (status !in setOf("CANCELLED", "DELIVERED")) TextButton(onClick = { scope.launch { busy = true; actionError = ""; try { api.cancelOrder(id); reload() } catch (t: Throwable) { actionError = t.message ?: "Unable to cancel order" } finally { busy = false } } }, enabled = !busy) { Text("Cancel") }; if (status == "DELIVERED") { TextButton(onClick = { reviewOpen = true }, enabled = !busy) { Text("Review") }; TextButton(onClick = { returnOpen = true }, enabled = !busy) { Text("Return") }; TextButton(onClick = { scope.launch { busy = true; actionError = ""; try { api.invoice(id); actionError = "Invoice generated successfully." } catch (t: Throwable) { actionError = t.message ?: "Unable to generate invoice" } finally { busy = false } } }, enabled = !busy) { Text("Invoice") } }; if (status != "CANCELLED") TextButton(onClick = { disputeOpen = true }, enabled = !busy) { Text("Report issue") } }; detail?.let { d -> Text("Items: ${d.optJSONArray("items")?.length() ?: 0}"); Text("Delivery status: ${d.optJSONObject("tracking")?.optString("status", status) ?: status}"); d.optJSONArray("trackingEvents")?.let { events -> Text("Tracking events: ${events.length()}") } } } } }

@Composable private fun ReviewDialog(api: AarvoApiClient, orderId: String, detail: JSONObject?, onDone: () -> Unit) { var rating by remember { mutableIntStateOf(5) }; var text by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope(); val items = detail?.optJSONArray("items"); val productId = items?.optJSONObject(0)?.optInt("product_id", items.optJSONObject(0)?.optInt("productId", 0) ?: 0) ?: 0; AlertDialog(onDismissRequest = { if (!busy) onDone() }, title = { Text("Rate your order") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Rating: ${"★".repeat(rating)}${"☆".repeat(5 - rating)}"); Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { (1..5).forEach { value -> TextButton(onClick = { rating = value }) { Text(value.toString()) } } }; OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), minLines = 3, label = { Text("Review (optional)") }); if (productId == 0) Text("Open order details first so AARVO can identify the purchased product.", color = MaterialTheme.colorScheme.error); if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error) } }, confirmButton = { Button(onClick = { scope.launch { busy = true; error = ""; try { api.submitReview(orderId, productId, rating, text); onDone() } catch (t: Throwable) { error = t.message ?: "Unable to submit review" } finally { busy = false } } }, enabled = !busy && productId > 0) { if (busy) CircularProgressIndicator() else Text("Submit review") } }, dismissButton = { TextButton(onClick = onDone, enabled = !busy) { Text("Close") } }) }

@Composable private fun ReturnDialog(api: AarvoApiClient, orderId: String, onDone: () -> Unit) {
    var reason by remember { mutableStateOf("DAMAGED") }
    var details by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!busy) onDone() }, title = { Text("Return request") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Reason")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("DAMAGED", "WRONG_ITEM", "NOT_AS_DESCRIBED", "OTHER").forEach { value ->
                    TextButton(onClick = { reason = value }) { Text(if (reason == value) "✓ $value" else value) }
                }
            }
            OutlinedTextField(details, { details = it }, Modifier.fillMaxWidth(), minLines = 3, label = { Text("Describe the reason") })
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { Button(onClick = { scope.launch { busy = true; error = ""; try { api.returnOrder(orderId, reason, details); onDone() } catch (t: Throwable) { error = t.message ?: "Unable to request return" } finally { busy = false } } }, enabled = !busy && details.trim().length >= 5) { if (busy) CircularProgressIndicator() else Text("Request return") } },
        dismissButton = { TextButton(onClick = onDone, enabled = !busy) { Text("Close") } })
}

@Composable private fun DisputeDialog(api: AarvoApiClient, orderId: String, onDone: () -> Unit) { var reason by remember { mutableStateOf("ITEM_NOT_RECEIVED") }; var details by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope(); AlertDialog(onDismissRequest = { if (!busy) onDone() }, title = { Text("Report an order issue") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Reason"); Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("ITEM_NOT_RECEIVED", "DAMAGED", "WRONG_ITEM", "OTHER").forEach { value -> TextButton(onClick = { reason = value }) { Text(if (reason == value) "✓ $value" else value) } } }; OutlinedTextField(details, { details = it }, Modifier.fillMaxWidth(), minLines = 3, label = { Text("Describe the issue") }); if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error) } }, confirmButton = { Button(onClick = { scope.launch { busy = true; error = ""; try { api.openDispute(orderId, reason, details); onDone() } catch (t: Throwable) { error = t.message ?: "Unable to open issue" } finally { busy = false } } }, enabled = !busy && details.trim().length >= 5) { if (busy) CircularProgressIndicator() else Text("Submit issue") } }, dismissButton = { TextButton(onClick = onDone, enabled = !busy) { Text("Close") } }) }

@Composable private fun SellerDashboardScreen(padding: PaddingValues, api: AarvoApiClient, onBack: () -> Unit) { var products by remember { mutableStateOf<List<JSONObject>>(emptyList()) }; var orders by remember { mutableStateOf<List<JSONObject>>(emptyList()) }; var profile by remember { mutableStateOf<JSONObject?>(null) }; var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf("") }; var showCreate by remember { mutableStateOf(false) }; var editing by remember { mutableStateOf<JSONObject?>(null) }; val scope = rememberCoroutineScope(); fun reload() { scope.launch { loading = true; error = ""; try { val p = api.sellerProducts(); products = buildList { for (i in 0 until p.length()) add(p.getJSONObject(i)) }; val o = api.sellerOrders(); orders = buildList { for (i in 0 until o.length()) add(o.getJSONObject(i)) }; profile = api.sellerProfile() } catch (t: Throwable) { error = t.message ?: "Unable to load seller dashboard" } finally { loading = false } } }; LaunchedEffect(Unit) { reload() }; if (showCreate) SellerProductDialog(api, { showCreate = false; reload() }); editing?.let { SellerProductEditDialog(it, api, { editing = null; reload() }) }; Scaffold(topBar = { TopAppBar(title = { Text("Seller Dashboard") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { inner -> LazyColumn(Modifier.fillMaxSize().padding(inner), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { if (loading) item { CircularProgressIndicator() }; if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }; item { Text("Seller status: ${if (profile?.optBoolean("verified", false) == true) "Verified" else "Verification pending"}") }; item { Button(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) { Text("Add Product") } }; item { Text("My Products (${products.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }; items(products.distinctBy { it.optInt("id") }, key = { it.optInt("id") }) { p -> SellerProductRow(p, api, { editing = p }, ::reload) }; item { Text("Recent Orders (${orders.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }; items(orders.distinctBy { it.optString("id") }, key = { it.optString("id") }) { o -> SellerOrderRow(o, api, ::reload) } } } }

@Composable private fun SellerProductRow(p: JSONObject, api: AarvoApiClient, onEdit: () -> Unit, reload: () -> Unit) { var stock by remember(p.optInt("id")) { mutableStateOf(p.optInt("stock_quantity").toString()) }; var busy by remember { mutableStateOf(false) }; var confirmDelete by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope(); if (confirmDelete) AlertDialog(onDismissRequest = { if (!busy) confirmDelete = false }, title = { Text("Delete product?") }, text = { Text("This product will be removed from your seller catalog. Continue?") }, confirmButton = { Button(onClick = { scope.launch { busy = true; try { api.deleteSellerProduct(p.optInt("id")); confirmDelete = false; reload() } catch (t: Throwable) { error = t.message ?: "Unable to delete product" } finally { busy = false } } }, enabled = !busy) { Text("Delete") } }, dismissButton = { TextButton(onClick = { confirmDelete = false }, enabled = !busy) { Text("Keep") } }); Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(p.optString("name"), fontWeight = FontWeight.SemiBold); Text("${formatPaise(p.optLong("price_paise"))} • ${if (p.optBoolean("is_published")) "Published" else "Draft"}"); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { TextButton(onClick = onEdit, enabled = !busy) { Text("Edit") }; TextButton(onClick = { confirmDelete = true }, enabled = !busy) { Text("Delete") }; if (!p.optBoolean("is_published")) TextButton(onClick = { scope.launch { busy = true; try { api.publishSellerProduct(p.optInt("id")); reload() } catch (t: Throwable) { error = t.message ?: "Unable to publish product" } finally { busy = false } } }, enabled = !busy) { Text("Publish") } }; if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(stock, { stock = it }, Modifier.weight(1f), singleLine = true, label = { Text("Stock") }); Button(onClick = { scope.launch { busy = true; try { api.updateInventory(p.optInt("id"), stock.toIntOrNull() ?: 0); reload() } catch (t: Throwable) { error = t.message ?: "Unable to update stock" } finally { busy = false } } }, enabled = !busy) { Text("Save") } } } } }

@Composable private fun SellerProductEditDialog(p: JSONObject, api: AarvoApiClient, onDone: () -> Unit) { var name by remember { mutableStateOf(p.optString("name")) }; var category by remember { mutableStateOf(p.optString("category")) }; var price by remember { mutableStateOf((p.optLong("price_paise") / 100.0).toString()) }; var description by remember { mutableStateOf(p.optString("description")) }; var error by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope(); AlertDialog(onDismissRequest = { if (!busy) onDone() }, title = { Text("Edit product") }, text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { OutlinedTextField(name, { name = it }, label = { Text("Name") }); OutlinedTextField(category, { category = it }, label = { Text("Category") }); OutlinedTextField(price, { price = it }, label = { Text("Price ₹") }); OutlinedTextField(description, { description = it }, label = { Text("Description") }); if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error) } }, confirmButton = { Button(onClick = { scope.launch { busy = true; error = ""; try { api.updateSellerProduct(p.optInt("id"), name, category, parseRupeesToPaise(price), description); onDone() } catch (t: Throwable) { error = t.message ?: "Unable to update product" } finally { busy = false } } }, enabled = !busy && name.isNotBlank() && category.isNotBlank() && description.isNotBlank() && parseRupeesToPaiseOrNull(price)?.let { it > 0 } == true) { if (busy) CircularProgressIndicator() else Text("Save changes") } }, dismissButton = { TextButton(onClick = onDone, enabled = !busy) { Text("Close") } }) }

@Composable private fun SellerProductDialog(api: AarvoApiClient, onDone: () -> Unit) { var name by remember { mutableStateOf("") }; var category by remember { mutableStateOf("") }; var price by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }; var stock by remember { mutableStateOf("0") }; var error by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope(); AlertDialog(onDismissRequest = { if (!busy) onDone() }, title = { Text("Add product") }, text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { OutlinedTextField(name, { name = it }, label = { Text("Name") }); OutlinedTextField(category, { category = it }, label = { Text("Category") }); OutlinedTextField(price, { price = it }, label = { Text("Price ₹") }); OutlinedTextField(description, { description = it }, label = { Text("Description") }); OutlinedTextField(stock, { stock = it }, label = { Text("Stock") }); if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error) } }, confirmButton = { Button(onClick = { scope.launch { busy = true; try { val pricePaise = parseRupeesToPaise(price); api.createSellerProduct(name, category, pricePaise, description, stock.toIntOrNull() ?: 0, false); onDone() } catch (t: Throwable) { error = t.message ?: "Unable to create product" } finally { busy = false } } }, enabled = !busy && name.isNotBlank() && category.isNotBlank() && parseRupeesToPaiseOrNull(price)?.let { it > 0 } == true && description.isNotBlank()) { Text("Save draft") } }, dismissButton = { TextButton(onClick = onDone, enabled = !busy) { Text("Close") } }) }

@Composable private fun SellerOrderRow(o: JSONObject, api: AarvoApiClient, reload: () -> Unit) { var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope(); val id = o.optString("id"); val status = o.optString("status"); val nextStatus = when (status) { "PAID" -> "PACKED"; "PACKED" -> "SHIPPED"; "SHIPPED" -> "OUT_FOR_DELIVERY"; "OUT_FOR_DELIVERY" -> "DELIVERED"; else -> "" }; Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text("Order #$id", fontWeight = FontWeight.SemiBold); Text("Status: $status • Payment: ${o.optString("payment_status")}"); Text("Seller amount: ${formatPaise(o.optJSONArray("items")?.let { arr -> (0 until arr.length()).sumOf { arr.getJSONObject(it).optLong("sellerAmountPaise") } } ?: 0L)}"); if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error); if (nextStatus.isNotBlank()) TextButton(onClick = { scope.launch { busy = true; error = ""; try { api.updateOrderTracking(id, nextStatus, note = "Seller marked order $nextStatus"); reload() } catch (t: Throwable) { error = t.message ?: "Unable to update order status" } finally { busy = false } } }, enabled = !busy) { Text("Mark $nextStatus") } } } }

private fun formatPaise(paise: Long): String = "₹${paise / 100}.${(paise % 100).toString().padStart(2, '0')}"
private fun parseRupeesToPaise(value: String): Long = parseRupeesToPaiseOrNull(value) ?: error("Enter a valid price")
private fun parseRupeesToPaiseOrNull(value: String): Long? = value.trim().let { if (!it.matches(Regex("\\d{1,9}(\\.\\d{1,2})?"))) null else { val parts = it.split('.'); val rupees = parts[0].toLongOrNull() ?: return@let null; val paise = (parts.getOrNull(1)?.padEnd(2, '0') ?: "00").toLongOrNull() ?: return@let null; (rupees * 100L + paise).takeIf { amount -> amount > 0L } } }
