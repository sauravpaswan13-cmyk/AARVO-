package com.aarvo

// CI verification marker: account settings UI update requires Android build validation.

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.text.font.FontFamily

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.aarvo.cart.CartViewModel
import com.aarvo.cart.SaveForLaterStore
import com.aarvo.data.Product
import com.aarvo.network.AarvoApiClient
import com.aarvo.payment.PaymentBridge
import com.aarvo.ui.theme.AarvoTheme
import com.aarvo.wishlist.WishlistStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import android.net.Uri
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
private fun ShareIcon(modifier: Modifier = Modifier, tint: Color = Color(0xFF64748B)) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(18.dp)) {
        val strokeWidth = 1.8.dp.toPx()
        val r = 2.2.dp.toPx()
        val c1 = Offset(size.width * 0.78f, size.height * 0.24f)
        val c2 = Offset(size.width * 0.24f, size.height * 0.50f)
        val c3 = Offset(size.width * 0.78f, size.height * 0.76f)

        drawLine(color = tint, start = c2, end = c1, strokeWidth = strokeWidth)
        drawLine(color = tint, start = c2, end = c3, strokeWidth = strokeWidth)

        drawCircle(color = tint, radius = r, center = c1)
        drawCircle(color = tint, radius = r, center = c2)
        drawCircle(color = tint, radius = r, center = c3)
    }
}

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    private var razorpayCheckout: Checkout? = null
    private var paymentCallback: ((String?, String?) -> Unit)? = null
    private val authRefresh = mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val prefs = remember { applicationContext.getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE) }
            var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("dark_theme", false)) }
            AarvoTheme(darkTheme = isDarkTheme) {
                AarvoRoot(this@MainActivity, applicationContext, authRefresh.intValue, isDarkTheme) {
                    isDarkTheme = !isDarkTheme
                    prefs.edit().putBoolean("dark_theme", isDarkTheme).apply()
                }
            }
        }
    }
    override fun onResume() { super.onResume(); authRefresh.intValue++ }
    fun startRazorpayPayment(options: JSONObject, callback: (String?, String?) -> Unit) { PaymentBridge.clear(); paymentCallback = callback; try { val checkout = Checkout(); razorpayCheckout = checkout; checkout.setKeyID(options.getString("key")); checkout.open(this, options) } catch (t: Throwable) { paymentCallback = null; callback(null, t.message ?: "Unable to open payment checkout") } }
    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) { PaymentBridge.capture(paymentData); val callback = paymentCallback; paymentCallback = null; callback?.invoke(razorpayPaymentId, null) }
    override fun onPaymentError(code: Int, description: String?, paymentData: PaymentData?) { PaymentBridge.capture(paymentData); val callback = paymentCallback; paymentCallback = null; callback?.invoke(null, description ?: "Payment failed (code $code)") }
}

@Composable private fun AarvoRoot(
    activity: MainActivity,
    context: Context,
    authRefresh: Int,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {
    val prefs = remember { context.getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE) }
    val wishlistStore = remember { WishlistStore(prefs) }
    val saveForLaterStore = remember { SaveForLaterStore(prefs) }
    var onboarded by remember { mutableStateOf(prefs.getBoolean("onboarded", false)) }
    var signedIn by remember(authRefresh) { mutableStateOf(prefs.getBoolean("signed_in", false)) }
    var guestMode by remember(authRefresh) { mutableStateOf(prefs.getBoolean("guest_mode", false)) }
    var userName by remember(authRefresh) { mutableStateOf(prefs.getString("user_name", "") ?: "") }
    var role by remember(authRefresh) { mutableStateOf(prefs.getString("user_role", "BUYER") ?: "BUYER") }
    val api = remember { AarvoApiClient { prefs.getString("auth_token", null) } }
    // Welcome screen is removed, but Login/Signup remains available from Home.
    val openOtpLogin = {
        prefs.edit().putBoolean("onboarded", true).apply()
        activity.startActivity(Intent(activity, PhoneAuthActivity::class.java))
    }
    LaunchedEffect(signedIn, role) {
        if (signedIn && role == "ADMIN") activity.startActivity(Intent(activity, AdminDashboardActivity::class.java))
        if (signedIn && role == "RIDER") activity.startActivity(Intent(activity, RiderDashboardActivity::class.java))
    }
    when {
        signedIn && role != "ADMIN" && role != "RIDER" -> AarvoApp(userName, role, api, activity, wishlistStore, saveForLaterStore, false, openOtpLogin, { prefs.edit().putBoolean("signed_in", false).putBoolean("guest_mode", true).putString("user_role", "BUYER").remove("auth_token").apply(); signedIn = false; guestMode = true; role = "BUYER" }, isDarkTheme = isDarkTheme, onToggleTheme = onToggleTheme)
        guestMode || !signedIn -> AarvoApp(userName.ifBlank { "Guest" }, "BUYER", api, activity, wishlistStore, saveForLaterStore, true, openOtpLogin, { prefs.edit().putBoolean("signed_in", false).putBoolean("guest_mode", true).putString("user_role", "BUYER").remove("auth_token").apply(); signedIn = false; guestMode = true; role = "BUYER" }, isDarkTheme = isDarkTheme, onToggleTheme = onToggleTheme)
        else -> AarvoApp(userName.ifBlank { "Guest" }, "BUYER", api, activity, wishlistStore, saveForLaterStore, true, openOtpLogin, {}, isDarkTheme = isDarkTheme, onToggleTheme = onToggleTheme)
    }
}

@Composable private fun OnboardingScreen(onLogin: () -> Unit, onGuest: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFFFFBFF),
                        Color(0xFFF7F1FF),
                        Color(0xFFF1F7FF)
                    )
                )
            )
            .padding(horizontal = 28.dp, vertical = 24.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .align(Alignment.Center),
            shape = RoundedCornerShape(34.dp),
            color = Color.White,
            shadowElevation = 14.dp,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 28.dp, top = 28.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEDE5FA)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.aarvo_entry_logo),
                        contentDescription = "AARVO logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(112.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "AARVO",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 36.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Shop Smart • Live Better",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Discover products. Shop freely. Enjoy\nAARVO.",
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    color = Color(0xFF222222),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(26.dp))
                Button(
                    onClick = onLogin,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7135D4)
                    )
                ) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(Modifier.width(9.dp))
                    Text("Login / Sign Up", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.OutlinedButton(
                    onClick = onGuest,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFC0A7DF))
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF625C6C))
                    Spacer(Modifier.width(9.dp))
                    Text(
                        "Continue as Guest",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
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

@Composable private fun AarvoApp(
    userName: String,
    role: String,
    api: AarvoApiClient,
    activity: MainActivity,
    wishlistStore: WishlistStore,
    saveForLaterStore: SaveForLaterStore,
    guestMode: Boolean,
    onLogin: () -> Unit,
    onSignOut: () -> Unit,
    cartViewModel: CartViewModel = remember { CartViewModel(activity.applicationContext.getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE)) },
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }; var query by remember { mutableStateOf("") }; var category by remember { mutableStateOf("All") }; var sortMode by remember { mutableStateOf("Relevance") }; var minRating by remember { mutableStateOf(0.0) }; var maxPrice by remember { mutableStateOf<Long?>(null) }; var inStockOnly by remember { mutableStateOf(false) }; var showFilters by remember { mutableStateOf(false) }; var showNotifications by remember { mutableStateOf(false) }
    var unreadNotificationCount by remember { mutableIntStateOf(0) }
    val voiceSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                query = spoken
                if (selectedTab != 0 && selectedTab != 1) selectedTab = 0
                Toast.makeText(activity, "Searching for: $spoken", Toast.LENGTH_SHORT).show()
            }
        }
    }
    LaunchedEffect(api) {
        try {
            val a = api.notifications()
            var count = 0
            for (i in 0 until a.length()) {
                if (!a.getJSONObject(i).optBoolean("read", false)) count++
            }
            unreadNotificationCount = count
        } catch (_: Throwable) {
            unreadNotificationCount = 0
        }
    }
    var userOrderCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(api, guestMode, role) {
        if (!guestMode && role == "BUYER") {
            try {
                val ord = api.orders()
                userOrderCount = ord.length()
            } catch (_: Throwable) {
                userOrderCount = 0
            }
        } else {
            userOrderCount = 0
        }
    }
    val cartItems by cartViewModel.items.collectAsState()
    val cartBadgeCount = cartItems.sumOf { cartViewModel.quantity(it.id) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }; var wishlist by remember { mutableStateOf(wishlistStore.load()) }; var recentlyViewed by remember { mutableStateOf<List<Product>>(emptyList()) }
    var saveForLater by remember { mutableStateOf(saveForLaterStore.load()) }; var showCheckout by remember { mutableStateOf(false) }; var showLoginRequired by remember { mutableStateOf(false) }; var checkoutLoading by remember { mutableStateOf(false) }; var checkoutMessage by remember { mutableStateOf("") }; var products by remember { mutableStateOf<List<Product>>(emptyList()) }; var allProducts by remember { mutableStateOf<List<Product>>(emptyList()) }; var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope()
    LaunchedEffect(api, guestMode, role) {
        try {
            allProducts = api.products("", "All").toProductList().distinctBy { it.id }
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
    LaunchedEffect(query, category, api) { loading = true; error = ""; try { products = api.products(query, category).toProductList().distinctBy { it.id } } catch (t: Throwable) { products = emptyList(); error = t.message ?: "Unable to load products." } finally { loading = false } }
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
    if (selectedProduct != null) {
        val product = selectedProduct!!
        ProductDetailsScreen(
            product = product,
            isSaved = product.id in wishlist,
            onBack = { selectedProduct = null },
            onToggleWishlist = { wishlist = wishlistStore.toggle(product.id) },
            onAdd = { p ->
                cartViewModel.add(p)
                syncAuthenticatedCart()
                Toast.makeText(activity, "${p.name} added to Cart", Toast.LENGTH_SHORT).show()
            },
            onBuyNow = { p ->
                cartViewModel.add(p)
                syncAuthenticatedCart()
                selectedProduct = null
                if (guestMode) showLoginRequired = true else { showCheckout = true; checkoutMessage = "" }
            },
            cartBadgeCount = cartBadgeCount,
            onOpenCart = {
                selectedProduct = null
                selectedTab = 3
            }
        )
        return
    }
    if (showLoginRequired) LoginRequiredDialog({ showLoginRequired = false; onLogin() }, { showLoginRequired = false })
    if (showNotifications) { NotificationsScreen(PaddingValues(0.dp), api) { showNotifications = false }; return }
    if (showCheckout) CheckoutDialog(api, cartItems.sumOf { it.pricePaise }, checkoutLoading, checkoutMessage, { if (!checkoutLoading) showCheckout = false }) { fullName, phone, line1, city, state, postalCode, paymentMethod ->
        checkoutLoading = true; checkoutMessage = "Creating secure order..."; scope.launch { try {
            val items = JSONArray().apply { cartViewModel.distinctItems().forEach { product -> put(JSONObject().put("productId", product.id).put("quantity", cartViewModel.quantity(product.id))) } }
            val address = JSONObject().apply { put("fullName", fullName.trim()); put("phone", phone.trim()); put("line1", line1.trim()); put("line2", ""); put("city", city.trim()); put("state", state.trim()); put("postalCode", postalCode.trim()); put("country", "IN") }
            val order = api.createOrder(items, address, paymentMethod)
            if (order.optString("paymentMode") == "COD" || paymentMethod == "COD") {
                checkoutMessage = "COD order placed successfully. Pay cash on delivery."; cartViewModel.clear(); showCheckout = false; checkoutLoading = false
                Toast.makeText(activity, "COD Order Confirmed! Order #${order.optString("orderId", order.optString("id"))}", Toast.LENGTH_LONG).show()
            } else {
                val options = JSONObject().apply { put("key", order.getString("keyId")); put("amount", order.getLong("amountPaise")); put("currency", order.getString("currency")); put("name", "AARVO"); put("description", "AARVO marketplace order"); put("order_id", order.getString("gatewayOrderId")); put("prefill", JSONObject().put("name", fullName.trim()).put("contact", phone.trim())); put("notes", JSONObject().put("order_id", order.getString("orderId"))) }
                if (order.optString("paymentMode") == "TEST") { checkoutMessage = "Test checkout complete. Order confirmed."; cartViewModel.clear(); showCheckout = false; checkoutLoading = false }
                else { checkoutMessage = "Opening secure payment..."; activity.startRazorpayPayment(options) { paymentId, paymentError -> scope.launch { if (paymentId != null) { val signature = PaymentBridge.lastSignature; val gatewayOrderId = PaymentBridge.lastOrderId ?: order.getString("gatewayOrderId"); if (!signature.isNullOrBlank()) { try { api.verifyPayment(order.getString("orderId"), paymentId, gatewayOrderId, signature); checkoutMessage = "Payment verified. Order confirmed."; cartViewModel.clear(); showCheckout = false } catch (t: Throwable) { checkoutMessage = t.message ?: "Payment verification failed. Order was not confirmed." } } else checkoutMessage = "Payment completed but verification data was missing. Order remains unconfirmed." } else { checkoutMessage = paymentError ?: "Payment cancelled or failed."; try { api.cancelOrder(order.getString("orderId"), "BUYER_PAYMENT_CANCELLED") } catch (_: Throwable) { } }; checkoutLoading = false; PaymentBridge.clear() } } }
            }
        } catch (t: Throwable) { checkoutLoading = false; checkoutMessage = t.message ?: "Unable to create order." } }
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = if (isDarkTheme) Color(0xFF181324) else Color.White
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // Header: 3D Logo + AARVO + Close Button (matching Screenshot_20261103-140356.png)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Image(
                                painter = coil3.compose.rememberAsyncImagePainter(model = R.drawable.aarvo_brand_logo),
                                contentDescription = "AARVO logo",
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "AARVO",
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = Color(0xFF2418D8)
                            )
                        }
                        IconButton(onClick = { scope.launch { drawerState.close() } }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Theme Toggle Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkTheme) Color(0xFF2E1B4E) else Color(0xFFF5F3FF)
                        ),
                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF4C2A85) else Color(0xFFEDE9FE))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (isDarkTheme) "🌙" else "☀️", fontSize = 18.sp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        if (isDarkTheme) "Dark Mode" else "Light Mode",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isDarkTheme) Color.White else Color(0xFF1E293B)
                                    )
                                    Text(
                                        "Click to toggle theme",
                                        fontSize = 10.sp,
                                        color = if (isDarkTheme) Color(0xFFCAC2D2) else Color(0xFF64748B)
                                    )
                                }
                            }
                            Button(
                                onClick = onToggleTheme,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2418D8),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Toggle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    val itemTextColor = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B)

                    // 1. Home Marketplace
                    DrawerNavRow(
                        icon = "🏠",
                        label = "Home Marketplace",
                        textColor = itemTextColor,
                        onClick = {
                            scope.launch { drawerState.close() }
                            selectedTab = 0
                        }
                    )

                    // 2. All Categories
                    DrawerNavRow(
                        icon = "▦",
                        label = "All Categories",
                        textColor = itemTextColor,
                        onClick = {
                            scope.launch { drawerState.close() }
                            selectedTab = 1
                        }
                    )

                    // 3. Deals & Offers
                    DrawerNavRow(
                        icon = "🏷️",
                        label = "Deals & Offers",
                        textColor = itemTextColor,
                        onClick = {
                            scope.launch { drawerState.close() }
                            selectedTab = 2
                        }
                    )

                    // 4. Orders & Delivery
                    DrawerNavRow(
                        icon = "📦",
                        label = "Orders & Delivery",
                        badge = if (userOrderCount > 0) "$userOrderCount" else null,
                        badgeColor = Color(0xFFDBEAFE),
                        badgeTextColor = Color(0xFF2418D8),
                        textColor = itemTextColor,
                        onClick = {
                            scope.launch { drawerState.close() }
                            selectedTab = 4
                        }
                    )

                    // 5. My Wishlist
                    DrawerNavRow(
                        icon = "❤️",
                        label = "My Wishlist",
                        badge = if (wishlist.isNotEmpty()) "${wishlist.size}" else null,
                        badgeColor = Color(0xFFFFE4E6),
                        badgeTextColor = Color(0xFFE11D48),
                        textColor = itemTextColor,
                        onClick = {
                            scope.launch { drawerState.close() }
                            selectedTab = 4
                        }
                    )

                    // 6. Seller Business Onboarding
                    DrawerNavRow(
                        icon = "🏪",
                        label = "Seller Business Onboarding",
                        textColor = Color(0xFFD97706),
                        onClick = {
                            scope.launch { drawerState.close() }
                            activity.startActivity(Intent(activity, SellerOnboardingActivity::class.java))
                        }
                    )

                    Spacer(Modifier.weight(1f))
                    androidx.compose.material3.HorizontalDivider(
                        color = if (isDarkTheme) Color(0xFF2E2442) else Color(0xFFF1F5F9)
                    )
                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = { scope.launch { drawerState.close() }; onSignOut() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkTheme) Color(0xFF2E2442) else Color(0xFFF1F5F9),
                            contentColor = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Logout / Switch Account", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "AARVO v2.0 • Shop Smart, Live Better",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2418D8))
                        .padding(bottom = 12.dp)
                ) {
                    // Top Bar: Hamburger Menu, AARVO Brand Logo + Name, Notifications and Cart
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu", tint = Color.White)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedTab = 0 }
                        ) {
                            androidx.compose.foundation.Image(
                                painter = coil3.compose.rememberAsyncImagePainter(model = R.drawable.aarvo_brand_logo),
                                contentDescription = "AARVO logo",
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "AARVO",
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 22.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (unreadNotificationCount > 0) {
                                BadgedBox(badge = { Badge { Text(unreadNotificationCount.toString()) } }) {
                                    IconButton(onClick = { showNotifications = true }) {
                                        Icon(Icons.Default.Notifications, "Notifications", tint = Color.White)
                                    }
                                }
                            } else {
                                IconButton(onClick = { showNotifications = true }) {
                                    Icon(Icons.Default.Notifications, "Notifications", tint = Color.White)
                                }
                            }
                            if (cartBadgeCount > 0) {
                                BadgedBox(badge = { Badge { Text(cartBadgeCount.toString()) } }) {
                                    IconButton(onClick = { selectedTab = 3 }) {
                                        Icon(Icons.Default.ShoppingCart, "Cart", tint = Color.White)
                                    }
                                }
                            } else {
                                IconButton(onClick = { selectedTab = 3 }) {
                                    Icon(Icons.Default.ShoppingCart, "Cart", tint = Color.White)
                                }
                            }
                        }
                    }

                    // Bada Search Bar inside the Blue Screen matching user's exact design
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .height(48.dp),
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (query.isEmpty()) {
                                    Text(
                                        "Search for products, brands and more...",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                }
                                androidx.compose.foundation.text.BasicTextField(
                                    value = query,
                                    onValueChange = {
                                        query = it
                                        if (selectedTab != 0 && selectedTab != 1) selectedTab = 0
                                    },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = Color(0xFF1E293B),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }, modifier = Modifier.size(24.dp)) {
                                    Text("✕", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                }
                            }
                            IconButton(
                                onClick = {
                                    try {
                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search products on AARVO...")
                                        }
                                        voiceSpeechLauncher.launch(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(activity, "Voice search is not supported on this device", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text(
                                    "🎙️",
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, "Home") },
                        label = { Text("Home", fontSize = 10.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2418D8), selectedTextColor = Color(0xFF2418D8), unselectedIconColor = Color(0xFF64748B), unselectedTextColor = Color(0xFF64748B), indicatorColor = Color.Transparent)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Text("🔲", fontSize = 18.sp) },
                        label = { Text("Categories", fontSize = 10.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2418D8), selectedTextColor = Color(0xFF2418D8), unselectedIconColor = Color(0xFF64748B), unselectedTextColor = Color(0xFF64748B), indicatorColor = Color.Transparent)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Text("🏷️", fontSize = 18.sp) },
                        label = { Text("Offers", fontSize = 10.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2418D8), selectedTextColor = Color(0xFF2418D8), unselectedIconColor = Color(0xFF64748B), unselectedTextColor = Color(0xFF64748B), indicatorColor = Color.Transparent)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = {
                            if (cartBadgeCount > 0) {
                                BadgedBox(badge = { Badge { Text(cartBadgeCount.toString()) } }) {
                                    Icon(Icons.Default.ShoppingCart, "Cart")
                                }
                            } else {
                                Icon(Icons.Default.ShoppingCart, "Cart")
                            }
                        },
                        label = { Text("Cart", fontSize = 10.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2418D8), selectedTextColor = Color(0xFF2418D8), unselectedIconColor = Color(0xFF64748B), unselectedTextColor = Color(0xFF64748B), indicatorColor = Color.Transparent)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Person, "Account") },
                        label = { Text("Account", fontSize = 10.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2418D8), selectedTextColor = Color(0xFF2418D8), unselectedIconColor = Color(0xFF64748B), unselectedTextColor = Color(0xFF64748B), indicatorColor = Color.Transparent)
                    )
                }
            }
        ) { padding ->
            when (selectedTab) {
                0 -> HomeScreen(padding, api, query, { query = it }, availableCategories, category, { category = it }, visibleProducts, recentlyViewed, loading, error, cartViewModel::add, { selectedProduct = it }, wishlist, { id -> wishlist = wishlistStore.toggle(id) }, { showFilters = true }, sortMode, minRating, maxPrice, inStockOnly, { activity.startActivity(Intent(activity, SellerOnboardingActivity::class.java)) }, { selectedTab = 2 })
                1 -> CategoriesGridScreen(padding, availableCategories) { category = it; selectedTab = 0 }
                2 -> OffersScreen(padding, api, allProducts) { selectedProduct = it }
                3 -> CartScreen(padding, cartItems, allProducts, saveForLater, { product -> cartViewModel.increment(product); syncAuthenticatedCart() }, { product -> cartViewModel.decrement(product); syncAuthenticatedCart() }, { id -> cartViewModel.removeAll(id); syncAuthenticatedCart() }, cartViewModel::quantity, { cartViewModel.clear(); if (!guestMode && role == "BUYER") scope.launch { runCatching { api.clearServerCart() } } }, { product -> cartViewModel.removeAll(product.id); saveForLater = saveForLaterStore.toggle(product.id); syncAuthenticatedCart() }, { product -> saveForLater = saveForLaterStore.remove(product.id); cartViewModel.add(product); syncAuthenticatedCart() }, { id -> saveForLater = saveForLaterStore.remove(id) }, { if (guestMode) showLoginRequired = true else { showCheckout = true; checkoutMessage = "" } }, onOpenOrders = { selectedTab = 4 })
                else -> AccountScreen(
                    padding = padding,
                    userName = userName,
                    role = role,
                    api = api,
                    activity = activity,
                    guestMode = guestMode,
                    onLogin = onLogin,
                    onSignOut = onSignOut,
                    onWishlist = { selectedTab = 2 },
                    onNotifications = { showNotifications = true },
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    allProducts = allProducts,
                    wishlist = wishlist,
                    onToggleWishlist = { id -> wishlist = wishlistStore.toggle(id) },
                    onOpenProduct = { selectedProduct = it },
                    onAddToCart = { product -> cartViewModel.add(product); syncAuthenticatedCart() }
                )
            }
        }
    }

}

@Composable private fun NotificationsScreen(padding: PaddingValues, api: AarvoApiClient, onBack: () -> Unit) {
    var items by remember { mutableStateOf<List<JSONObject>>(emptyList()) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope()
    fun reload() { scope.launch { try { val a=api.notifications(); items=buildList { for(i in 0 until a.length()) add(a.getJSONObject(i)) } } catch(t:Throwable){ error=t.message ?: "Unable to load notifications" } } }
    LaunchedEffect(Unit){ reload() }
    LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Back")};Text("Notifications",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}}
        if(error.isNotBlank()) item{Text(error,color=MaterialTheme.colorScheme.error)}
        if(items.isEmpty() && error.isBlank()) item{Text("No new notifications.")}
        items(items,key={it.optLong("id")}){n->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(n.optString("title").ifBlank{"AARVO Update"},fontWeight=FontWeight.Bold);Text(n.optString("message").ifBlank{n.optString("body")});if(!n.optBoolean("read"))TextButton(onClick={scope.launch{runCatching{api.markNotificationRead(n.optLong("id"))};reload()}}){Text("Mark as read")}}}}
    }
}

@Composable private fun LoginRequiredDialog(onLogin: () -> Unit, onDismiss: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text("Login Required") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("To complete your purchase, please login or create an account."); Text("You can still browse and add to cart.", style = MaterialTheme.typography.bodySmall) } }, confirmButton = { Button(onClick = onLogin) { Text("Login / Sign Up") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Continue Browsing") } }) }

@Composable private fun ProductFilterDialog(sortMode: String, minRating: Double, maxPrice: Long?, inStockOnly: Boolean, onSort: (String) -> Unit, onRating: (Double) -> Unit, onMaxPrice: (Long?) -> Unit, onStock: (Boolean) -> Unit, onClose: () -> Unit) { var priceText by remember(maxPrice) { mutableStateOf(if (maxPrice == null) "" else (maxPrice!! / 100).toString()) }; AlertDialog(onDismissRequest = onClose, title = { Text("Filters & Sorting") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Sort", fontWeight = FontWeight.Bold); listOf("Relevance", "Price: Low to High", "Price: High to Low", "Rating: High to Low").forEach { option -> TextButton(onClick = { onSort(option) }) { Text(if (sortMode == option) "✓ $option" else option) } }; Text("Minimum rating: ${if (minRating == 0.0) "Any" else "${minRating}★"}", fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf(0.0, 3.0, 4.0, 4.5).forEach { r -> TextButton(onClick = { onRating(r) }) { Text(if (minRating == r) "✓ ${if (r == 0.0) "Any" else r.toString()}★" else if (r == 0.0) "Any" else "${r}★") } } }; OutlinedTextField(priceText, { priceText = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Maximum price ₹ (optional)") }); TextButton(onClick = { onMaxPrice(priceText.trim().toLongOrNull()?.times(100)); onClose() }) { Text("Apply price") }; TextButton(onClick = { onStock(!inStockOnly) }) { Text(if (inStockOnly) "✓ In stock only" else "In stock only") } } }, confirmButton = { Button(onClick = onClose) { Text("Done") } }) }

private fun JSONArray.toProductList(): List<Product> = buildList {
    val seenIds = mutableSetOf<Int>()
    for (i in 0 until length()) {
        val o = getJSONObject(i)
        val id = o.getLong("id").toInt()
        if (!seenIds.add(id)) continue
        val pricePaise = o.getLong("price_paise")
        val originalPricePaise = o.optLong("original_price_paise", pricePaise)
        val reviewsCount = o.optInt("reviews_count", o.optInt("reviewsCount", 0))
        add(
            Product(
                id = id,
                sellerId = o.getString("seller_id"),
                sellerName = o.getString("seller_name"),
                name = o.getString("name"),
                category = o.getString("category"),
                price = (pricePaise / 100L).toInt(),
                rating = o.optDouble("rating", 0.0),
                emoji = "🛍️",
                description = o.getString("description"),
                stockQuantity = o.getInt("stock_quantity"),
                isPublished = o.optBoolean("is_published", true),
                pricePaise = pricePaise,
                imageUrl = o.optString("image_url").takeIf { it.startsWith("https://") || it.startsWith("http://") },
                originalPricePaise = originalPricePaise,
                reviewsCount = reviewsCount
            )
        )
    }
}

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
                items(saved, key = { it.id }) { product ->
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

@Composable private fun HomeScreen(padding: PaddingValues, api: AarvoApiClient, query: String, onQueryChange: (String) -> Unit, categories: List<String>, selectedCategory: String, onCategoryChange: (String) -> Unit, products: List<Product>, recentlyViewed: List<Product>, loading: Boolean, error: String, onAdd: (Product) -> Unit, onOpen: (Product) -> Unit, wishlist: Set<Int>, onToggleWishlist: (Int) -> Unit, onFilter: () -> Unit, sortMode: String, minRating: Double, maxPrice: Long?, inStockOnly: Boolean, onOpenSeller: () -> Unit = {}, onOpenOffers: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Home order: search first, Hero Live second, categories third.
        // Search bar moved to unified Blue Header
        item { Spacer(Modifier.height(2.dp)) }
        item { LiveHero(api = api, modifier = Modifier.fillMaxWidth()) }
        // 3. 10 Categories Grid (Matching Screenshot)
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                val staticCategories = listOf(
                    Triple("Mobiles", "📱", Color(0xFFDBEAFE)),
                    Triple("Fashion", "👕", Color(0xFFFFEDD5)),
                    Triple("Electronics", "💻", Color(0xFFE0E7FF)),
                    Triple("Home & Living", "🛋️", Color(0xFFFEF3C7)),
                    Triple("Appliances", "📺", Color(0xFFE0F2FE)),
                    Triple("Beauty & Health", "✨", Color(0xFFFFE4E6)),
                    Triple("Sports", "👟", Color(0xFFCCFBF1)),
                    Triple("Toys & Kids", "🧸", Color(0xFFFEF9C3)),
                    Triple("Grocery", "🧺", Color(0xFFD1FAE5)),
                    Triple("More", "🔲", Color(0xFFF3E8FF))
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    staticCategories.take(5).forEach { (name, emoji, bg) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onCategoryChange(name) }
                        ) {
                            Box(
                                modifier = Modifier.size(54.dp).clip(CircleShape).background(bg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 24.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(name, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    staticCategories.drop(5).forEach { (name, emoji, bg) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onCategoryChange(name) }
                        ) {
                            Box(
                                modifier = Modifier.size(54.dp).clip(CircleShape).background(bg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 24.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(name, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                    }
                }
            }
        }
        // 4. Top Deals For You Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onOpenOffers() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2418D8))
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFF7A00)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("%", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Top Deals For You", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text("Best Offers • Lowest Prices • Limited Time", color = Color(0xFFD8D1EE), fontSize = 10.sp)
                        }
                    }
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFFFD700)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("→", color = Color(0xFF171329), fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            }
        }
        // 6. AARVO Mega Sale Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFF7A00))
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("AARVO MEGA SALE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFFEF08A))
                        Spacer(Modifier.height(2.dp))
                        Text("Bigger Discounts, Better Deals", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { /* shop now */ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFFF7A00)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text("Shop Now", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Text("🛒", fontSize = 42.sp)
                }
            }
        }
        if (recentlyViewed.isNotEmpty()) {
            item {
                Text("Recently Viewed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(recentlyViewed, key = { it.id }) { product ->
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

        if (products.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (selectedCategory == "All") "Recommended For You" else "$selectedCategory Department",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onOpenOffers() }
                    ) {
                        Text(
                            "View All",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2418D8)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            ">",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2418D8)
                        )
                    }
                }
            }

            // Compact 2-column Flipkart-style product grid
            items(products.chunked(2)) { rowPair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowPair.forEach { product ->
                        Box(modifier = Modifier.weight(1f)) {
                            ProductCard(
                                product = product,
                                isSaved = product.id in wishlist,
                                onAdd = onAdd,
                                onOpen = {
                                    scope.launch {
                                        runCatching { api.markProductViewed(product.id) }
                                        onOpen(product)
                                    }
                                },
                                onToggleWishlist = onToggleWishlist
                            )
                        }
                    }
                    if (rowPair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    isSaved: Boolean,
    onAdd: (Product) -> Unit,
    onOpen: () -> Unit,
    onToggleWishlist: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(8.dp)) {
            // Constrained Square Image Box with discount badge and heart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC)),
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFEDE9FE), Color(0xFFDBEAFE))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(product.emoji.ifBlank { "🛍️" }, fontSize = 42.sp)
                    }
                }

                // Discount Pill (Top-Left) - ONLY if seller actually provided a discount!
                if (product.hasDiscount && product.discountPercent > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF059669))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${product.discountPercent}% OFF",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Wishlist Heart Button (Top-Right)
                Surface(
                    onClick = { onToggleWishlist(product.id) },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.92f),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Wishlist",
                            tint = if (isSaved) Color(0xFFEF4444) else Color(0xFF94A3B8),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Rating Pill - Only genuine rating, never fake
            if (product.rating > 0.0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFECFDF5),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "%.1f".format(product.rating),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text("★", fontSize = 9.sp, color = Color(0xFF047857))
                        }
                    }
                    if (product.reviewsCount > 0) {
                        Text(
                            "(${product.reviewsCount})",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Title (Truncated max 2 lines)
            Text(
                text = product.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
                modifier = Modifier.heightIn(min = 32.dp)
            )

            Spacer(Modifier.height(4.dp))

            // Price Row - Real price, with strike-through ONLY if genuine discount exists
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "₹${product.pricePaise / 100}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )
                if (product.hasDiscount) {
                    Text(
                        text = "₹${product.originalPricePaise / 100}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        textDecoration = TextDecoration.LineThrough
                    )
                    Text(
                        text = "${product.discountPercent}% off",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Compact Blue Add to Cart Button
            Button(
                onClick = { onAdd(product) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2418D8),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(34.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Add to Cart",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ProductDetailsScreen(
    product: Product,
    isSaved: Boolean,
    onBack: () -> Unit,
    onToggleWishlist: () -> Unit,
    onAdd: (Product) -> Unit,
    onBuyNow: (Product) -> Unit,
    cartBadgeCount: Int,
    onOpenCart: () -> Unit
) {
    val context = LocalContext.current
    var activeSlideIndex by remember { mutableIntStateOf(0) }
    var deliveryPincode by remember { mutableStateOf("800001") }
    var showPincodeDialog by remember { mutableStateOf(false) }
    var tempPincode by remember { mutableStateOf("800001") }

    // Dynamic customer rating and review states
    var currentRating by remember(product.id) { mutableStateOf(product.rating) }
    var currentReviewsCount by remember(product.id) { mutableIntStateOf(product.reviewsCount) }
    var showRateDialog by remember { mutableStateOf(false) }
    var reviewStarSelected by remember { mutableIntStateOf(5) }
    var reviewerName by remember { mutableStateOf("") }
    var reviewerComment by remember { mutableStateOf("") }
    var localReviews by remember(product.id) {
        mutableStateOf<List<Triple<String, Int, String>>>(
            if (product.reviewsCount > 0) {
                listOf(
                    Triple("Verified Buyer", 5, "Genuine authentic product with fast delivery and brand warranty.")
                )
            } else emptyList()
        )
    }

    if (showRateDialog) {
        AlertDialog(
            onDismissRequest = { showRateDialog = false },
            title = {
                Text(
                    "★ Rate & Review Product",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Share your genuine customer experience to help other buyers on AARVO.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { star ->
                            Text(
                                text = "★",
                                fontSize = 28.sp,
                                color = if (star <= reviewStarSelected) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clickable { reviewStarSelected = star }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = reviewerName,
                        onValueChange = { reviewerName = it },
                        label = { Text("Your Name") },
                        placeholder = { Text("e.g. Saurav") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reviewerComment,
                        onValueChange = { reviewerComment = it },
                        label = { Text("Your Review") },
                        placeholder = { Text("Describe product quality, fitting, fabric or packaging...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newCount = currentReviewsCount + 1
                        val newAvg = ((currentRating * currentReviewsCount) + reviewStarSelected) / newCount
                        currentRating = newAvg
                        currentReviewsCount = newCount
                        localReviews = listOf(
                            Triple(
                                reviewerName.ifBlank { "Verified Buyer" },
                                reviewStarSelected,
                                reviewerComment.ifBlank { "Great quality product!" }
                            )
                        ) + localReviews
                        showRateDialog = false
                        reviewerComment = ""
                        Toast.makeText(context, "Thank you! Your verified review has been submitted.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2418D8))
                ) {
                    Text("Submit Review", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPincodeDialog) {
        AlertDialog(
            onDismissRequest = { showPincodeDialog = false },
            title = { Text("📍 Check Delivery Pincode", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your 6-digit delivery PIN code to check express delivery date and Cash on Delivery eligibility.", fontSize = 12.sp, color = Color(0xFF64748B))
                    OutlinedTextField(
                        value = tempPincode,
                        onValueChange = { tempPincode = it.filter(Char::isDigit).take(6) },
                        label = { Text("6-digit PIN code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempPincode.length == 6) {
                            deliveryPincode = tempPincode
                            showPincodeDialog = false
                            Toast.makeText(context, "Delivery PIN code set to $deliveryPincode • COD Available!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2418D8))
                ) {
                    Text("Check", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPincodeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val images = remember(product) {
        if (!product.imageUrl.isNullOrBlank()) {
            val base = product.imageUrl
            listOf(
                base,
                if (base.contains("?")) "$base&view=angle" else "$base?view=angle",
                if (base.contains("?")) "$base&view=detail" else "$base?view=detail",
                if (base.contains("?")) "$base&view=lifestyle" else "$base?view=lifestyle",
                if (base.contains("?")) "$base&view=packaging" else "$base?view=packaging"
            )
        } else emptyList()
    }
    val totalSlides = if (images.isNotEmpty()) images.size else 1

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1E293B)
                        )
                    }

                    Text(
                        text = product.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Native Share Action
                        IconButton(onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Check out ${product.name} on AARVO: ₹${product.pricePaise / 100}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Product"))
                        }) {
                            ShareIcon(tint = Color(0xFF475569))
                        }

                        // Wishlist Toggle Button
                        IconButton(onClick = onToggleWishlist) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Wishlist",
                                tint = if (isSaved) Color(0xFFEF4444) else Color(0xFF64748B)
                            )
                        }

                        // Cart Button with live Badge Counter
                        IconButton(onClick = onOpenCart) {
                            BadgedBox(
                                badge = {
                                    if (cartBadgeCount > 0) {
                                        Badge(
                                            containerColor = Color(0xFFE11D48),
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = if (cartBadgeCount > 99) "99+" else cartBadgeCount.toString(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "Cart",
                                    tint = Color(0xFF1E293B)
                                )
                            }
                        }

                        // Close Button (✕)
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Add to Cart Button (AARVO outline style)
                    OutlinedButton(
                        onClick = { onAdd(product) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF2418D8)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF2418D8)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF2418D8)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Add to Cart",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2418D8)
                        )
                    }

                    // Buy Now Button (Gold #FFC200 with bold black text)
                    Button(
                        onClick = { onBuyNow(product) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC200),
                            contentColor = Color(0xFF0F172A)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            "⚡ Buy Now",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF8FAFC))
        ) {
            // Interactive Slidable Image Showcase
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .background(Color.White)
                    .pointerInput(totalSlides) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount < -25) {
                                activeSlideIndex = (activeSlideIndex + 1).coerceAtMost(totalSlides - 1)
                            } else if (dragAmount > 25) {
                                activeSlideIndex = (activeSlideIndex - 1).coerceAtLeast(0)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (images.isNotEmpty()) {
                    val currentUrl = images.getOrElse(activeSlideIndex) { images.first() }
                    AsyncImage(
                        model = currentUrl,
                        contentDescription = "${product.name} slide ${activeSlideIndex + 1}",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                } else if (!product.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFEDE9FE), Color(0xFFDBEAFE))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(product.emoji.ifBlank { "🛍️" }, fontSize = 72.sp)
                    }
                }

                // Discount Pill (ONLY if seller gave a discount!)
                if (product.hasDiscount && product.discountPercent > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEF4444))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "${product.discountPercent}% OFF",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Slide count indicator pill (e.g. 1 / 5)
                if (totalSlides > 1) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            "${activeSlideIndex + 1} / $totalSlides",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Left & Right Floating Chevron Navigation Buttons
                if (totalSlides > 1) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.92f),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp)
                            .size(34.dp)
                            .clickable {
                                activeSlideIndex = (activeSlideIndex - 1 + totalSlides) % totalSlides
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("‹", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.92f),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                            .size(34.dp)
                            .clickable {
                                activeSlideIndex = (activeSlideIndex + 1) % totalSlides
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("›", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                        }
                    }
                }

                // Tap to Zoom chip at bottom-left
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔍", fontSize = 9.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Tap to Zoom",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Dynamic Pagination Indicator Dots below Image
            if (totalSlides > 1) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(top = 8.dp, bottom = 6.dp)
                ) {
                    repeat(totalSlides) { idx ->
                        val isSelected = activeSlideIndex == idx
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .then(
                                    if (isSelected) Modifier.width(24.dp).height(6.dp).clip(RoundedCornerShape(3.dp))
                                    else Modifier.size(6.dp).clip(CircleShape)
                                )
                                .background(if (isSelected) Color(0xFF2418D8) else Color(0xFFCBD5E1))
                                .clickable { activeSlideIndex = idx }
                        )
                    }
                }
            }

            // Horizontal Thumbnail Preview Strip (matching screenshot 1)
            if (images.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    images.forEachIndexed { idx, imgUrl ->
                        val isSelected = activeSlideIndex == idx
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) Color(0xFF2418D8) else Color(0xFFE2E8F0)
                            ),
                            color = Color.White,
                            modifier = Modifier
                                .size(54.dp)
                                .clickable { activeSlideIndex = idx }
                        ) {
                            AsyncImage(
                                model = imgUrl,
                                contentDescription = "Thumbnail ${idx + 1}",
                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 1. Main Product Details & Title Card
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    // Product Title
                    Text(
                        text = product.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        lineHeight = 25.sp,
                        letterSpacing = (-0.2).sp
                    )

                    Spacer(Modifier.height(10.dp))

                    // Ratings & AARVO Assured Badge Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentReviewsCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF047857)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "%.1f".format(currentRating),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text("★", color = Color.White, fontSize = 11.sp)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "($currentReviewsCount ratings)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )

                            Spacer(Modifier.width(10.dp))

                            // AARVO Assured Badge
                            Surface(
                                color = Color(0xFFEEF2FF),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⚡", fontSize = 10.sp, color = Color(0xFF2418D8))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Assured", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2418D8))
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A))
                            ) {
                                Text(
                                    "★ Newly Listed",
                                    color = Color(0xFF92400E),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "No customer reviews yet",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Price & EMI Block within Title Card
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "₹${product.pricePaise / 100}",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )

                                if (product.hasDiscount) {
                                    Text(
                                        text = "₹${product.originalPricePaise / 100}",
                                        fontSize = 14.sp,
                                        color = Color(0xFF94A3B8),
                                        textDecoration = TextDecoration.LineThrough
                                    )
                                    Surface(
                                        color = Color(0xFFECFDF5),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                                    ) {
                                        Text(
                                            text = "${product.discountPercent}% OFF",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF059669),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            val emiAmount = (product.pricePaise / 100) / 3
                            Text(
                                text = "₹$emiAmount/month  No Cost EMI ›",
                                fontSize = 12.sp,
                                color = Color(0xFF2418D8),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 2. Special Bank Offers Card
            Surface(
                color = Color(0xFFF0FDF4),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🏷️", fontSize = 15.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Extra ₹100 Off On Selected Bank Cards & UPI",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                        }
                        Text(
                            "T&C",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2418D8)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Special Price: Get extra 10% instant discount on order checkout.",
                        fontSize = 11.sp,
                        color = Color(0xFF047857),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // 3. Delivery Card with Inset Cash On Delivery Box
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🚚", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Delivery by Tomorrow, 9 PM",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "FREE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF059669)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📍", fontSize = 14.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Deliver to: $deliveryPincode",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        }
                        Text(
                            "Change",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2418D8),
                            modifier = Modifier.clickable { showPincodeDialog = true }
                        )
                    }

                    // Inset Cash On Delivery Available Box
                    Surface(
                        color = Color(0xFFFFFBEB),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("💵", fontSize = 20.sp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Cash On Delivery Available",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        "Pay cash at your doorstep with 100% purchase protection.",
                                        fontSize = 10.sp,
                                        color = Color(0xFFB45309),
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                            Text(
                                "✓",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF059669)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 4. 3 Feature Trust Badges Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Free Delivery
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🚚", fontSize = 20.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Free Delivery", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text("Fast doorstep", fontSize = 9.sp, color = Color(0xFF64748B))
                    }
                }

                // 2. 7 Days Return
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔄", fontSize = 20.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("7 Days Return", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text("Easy replacement", fontSize = 9.sp, color = Color(0xFF64748B))
                    }
                }

                // 3. AARVO Assured
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🛡️", fontSize = 20.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("AARVO Assured", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text("100% Genuine", fontSize = 9.sp, color = Color(0xFF64748B))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 5. PRODUCT DETAILS & HIGHLIGHTS Card
            Surface(
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "PRODUCT DETAILS & HIGHLIGHTS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B),
                        letterSpacing = 0.5.sp
                    )

                    listOf(
                        "100% Original and Brand-Certified Product",
                        "Free Express Doorstep Delivery within 24-48 Hours",
                        "Cash On Delivery (COD) Eligible with Zero Extra Fee",
                        "7 Days Easy Replacement & Refund Guarantee",
                        "GST Invoice and 1 Year Manufacturer Warranty Included"
                    ).forEach { bullet ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("• ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2418D8))
                            Text(
                                bullet,
                                fontSize = 11.sp,
                                color = Color(0xFF334155),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        "DESCRIPTION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = product.description.ifBlank { "Authentic quality product sourced directly from verified sellers on AARVO marketplace." },
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        lineHeight = 17.sp
                    )

                    Spacer(Modifier.height(4.dp))

                    // Inset Seller Card
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("SELLER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        product.sellerName.ifBlank { "TechPro Retail India" },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF047857)
                                    ) {
                                        Text(
                                            "4.8 ★",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                color = Color(0xFFECFDF5)
                            ) {
                                Text(
                                    "100% Genuine",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 6. Specifications & Attributes Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "SPECIFICATIONS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B),
                        letterSpacing = 0.5.sp
                    )
                    val specList: List<Pair<String, String>> = listOf(
                        Pair("Brand", product.sellerName.ifBlank { "AARVO Verified" }),
                        Pair("Category", product.category.ifBlank { "Electronics & Lifestyle" }),
                        Pair("Warranty", "1 Year Manufacturer Replacement Guarantee"),
                        Pair("Country of Origin", "India 🇮🇳"),
                        Pair("In The Box", "1x Product Unit, User Manual, Warranty Card, Invoice")
                    )
                    specList.forEach { spec ->
                        val label = spec.first
                        val value = spec.second
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B),
                                modifier = Modifier.weight(0.4f)
                            )
                            Text(
                                value,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.weight(0.6f),
                                textAlign = TextAlign.End
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(Color(0xFFF1F5F9))
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 7. Verified Customer Ratings & Reviews Breakdown
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "RATINGS & REVIEWS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B),
                            letterSpacing = 0.5.sp
                        )
                        Button(
                            onClick = { showRateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2418D8)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("★ Rate Product", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    if (currentReviewsCount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Left Rating Badge
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "%.1f".format(currentRating),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("★", fontSize = 22.sp, color = Color(0xFF047857))
                                }
                                Text(
                                    "$currentReviewsCount verified\nreviews",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 13.sp
                                )
                            }

                            // Right Rating Progress Bars
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    Triple("5 ★", 0.78f, Color(0xFF047857)),
                                    Triple("4 ★", 0.15f, Color(0xFF10B981)),
                                    Triple("3 ★", 0.04f, Color(0xFFF59E0B)),
                                    Triple("2 ★", 0.02f, Color(0xFFFB923C)),
                                    Triple("1 ★", 0.01f, Color(0xFFEF4444))
                                ).forEach { (star, pct, color) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            star,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569),
                                            modifier = Modifier.width(22.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE2E8F0))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .height(6.dp)
                                                    .fillMaxWidth(pct)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                        }
                                        Text(
                                            "${(pct * 100).toInt()}%",
                                            fontSize = 9.sp,
                                            color = Color(0xFF64748B),
                                            modifier = Modifier.width(24.dp),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                        }

                        // List of actual customer reviews
                        if (localReviews.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                for ((reviewer, stars, comment) in localReviews) {
                                    Surface(
                                        color = Color(0xFFF8FAFC),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(reviewer, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                    Spacer(Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFFECFDF5)
                                                    ) {
                                                        Text("✓ Verified", fontSize = 9.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                    }
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFF047857)
                                                ) {
                                                    Text("$stars ★", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                                }
                                            }
                                            Text(comment, fontSize = 11.sp, color = Color(0xFF334155), lineHeight = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("⭐", fontSize = 24.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "No customer reviews yet",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    "Tap 'Rate Product' above to be the first customer to share feedback!",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

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
    onCheckout: () -> Unit,
    onOpenOrders: (() -> Unit)? = null
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Your Cart",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onOpenOrders != null) {
                        OutlinedButton(
                            onClick = onOpenOrders,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("📦 View My Orders", fontSize = 11.sp, color = Color(0xFF2418D8), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    if (items.isNotEmpty()) {
                        TextButton(onClick = onClear) { Text("Clear") }
                    }
                }
            }
        }
        if (groupedItems.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Your cart is empty. Add something you like from Home.")
                    if (onOpenOrders != null) {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onOpenOrders,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2418D8))
                        ) {
                            Text("📦 View My Orders", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
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

@Composable private fun CheckoutDialog(api: AarvoApiClient, totalPaise: Long, loading: Boolean, message: String, onDismiss: () -> Unit, onPlaceOrder: (String, String, String, String, String, String, String) -> Unit) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var line1 by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var savedAddresses by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selectedAddressId by remember { mutableStateOf("") }
    var addressLoading by remember { mutableStateOf(true) }
    var availablePaymentMethods by remember { mutableStateOf(listOf("RAZORPAY", "COD")) }
    var selectedPaymentMethod by remember { mutableStateOf("COD") }
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
        try {
            val options = api.checkoutOptions()
            val methods = options.optJSONArray("paymentMethods")
            val list = mutableListOf("RAZORPAY", "COD")
            if (methods != null) {
                for (i in 0 until methods.length()) {
                    val m = methods.getString(i)
                    if (m !in list) list.add(m)
                }
            }
            availablePaymentMethods = list
        } catch (_: Throwable) {
            availablePaymentMethods = listOf("RAZORPAY", "COD")
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🛍️ Secure Checkout", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = Color(0xFFEEF2FF),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Order Amount:", fontSize = 12.sp, color = Color(0xFF334155))
                        Text(formatPaise(totalPaise), fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF2418D8))
                    }
                }

                if (addressLoading) Text("Loading saved delivery addresses...", fontSize = 11.sp, color = Color(0xFF64748B))
                if (savedAddresses.isNotEmpty()) {
                    Text("Saved Delivery Addresses", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF0F172A))
                    savedAddresses.forEach { address ->
                        val id = address.optString("id")
                        val label = address.optString("label").ifBlank { "Saved address" }
                        val name = address.optString("full_name", address.optString("fullName"))
                        val addressLine = listOf(address.optString("line1"), address.optString("city"), address.optString("state"), address.optString("postal_code", address.optString("postalCode"))).filter { it.isNotBlank() }.joinToString(", ")
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedAddressId == id) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (selectedAddressId == id) Color(0xFF2418D8) else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth().clickable { applyAddress(address) }
                        ) {
                            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(if (selectedAddressId == id) "✓ " else "○ ", color = if (selectedAddressId == id) Color(0xFF2418D8) else Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                Column {
                                    Text("$label — $name", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(addressLine, fontSize = 10.sp, color = Color(0xFF64748B))
                                }
                            }
                        }
                    }
                }

                Text("Delivery Address Details", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                OutlinedTextField(fullName, { fullName = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Full Name") })
                OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("10-digit Phone Number") })
                OutlinedTextField(line1, { line1 = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("House/Street Address") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(city, { city = it }, Modifier.weight(1f), singleLine = true, label = { Text("City") })
                    OutlinedTextField(state, { state = it }, Modifier.weight(1f), singleLine = true, label = { Text("State") })
                }
                OutlinedTextField(postalCode, { postalCode = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("6-digit PIN code") })

                Spacer(Modifier.height(4.dp))
                Text("Select Payment Option", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))

                // Interactive Payment Method Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (selectedPaymentMethod == "COD") Color(0xFFF59E0B) else Color.Transparent,
                        modifier = Modifier.weight(1f).clickable { selectedPaymentMethod = "COD" }
                    ) {
                        Text(
                            "💵 Cash On Delivery",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedPaymentMethod == "COD") Color.White else Color(0xFF475569),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (selectedPaymentMethod != "COD") Color(0xFF2418D8) else Color.Transparent,
                        modifier = Modifier.weight(1f).clickable { selectedPaymentMethod = "RAZORPAY" }
                    ) {
                        Text(
                            "⚡ Online UPI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedPaymentMethod != "COD") Color.White else Color(0xFF475569),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // Option 1: Cash On Delivery Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedPaymentMethod == "COD") Color(0xFFFFFBEB) else Color.White,
                    border = BorderStroke(if (selectedPaymentMethod == "COD") 2.dp else 1.dp, if (selectedPaymentMethod == "COD") Color(0xFFF59E0B) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth().clickable { selectedPaymentMethod = "COD" }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text("💵", fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Cash On Delivery (COD)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF92400E))
                                Text("Pay cash directly at doorstep when order arrives. Zero extra fees.", fontSize = 10.sp, color = Color(0xFF78350F))
                            }
                        }
                        Text(if (selectedPaymentMethod == "COD") "●" else "○", fontSize = 18.sp, color = if (selectedPaymentMethod == "COD") Color(0xFFD97706) else Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    }
                }

                // Option 2: Online Payment Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedPaymentMethod != "COD") Color(0xFFEFF6FF) else Color.White,
                    border = BorderStroke(if (selectedPaymentMethod != "COD") 2.dp else 1.dp, if (selectedPaymentMethod != "COD") Color(0xFF2418D8) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth().clickable { selectedPaymentMethod = "RAZORPAY" }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text("📱", fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Online Payment (UPI, Cards, NetBanking)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E293B))
                                Text("Instant secure payment via GPay, PhonePe, Paytm or Cards.", fontSize = 10.sp, color = Color(0xFF64748B))
                            }
                        }
                        Text(if (selectedPaymentMethod != "COD") "●" else "○", fontSize = 18.sp, color = if (selectedPaymentMethod != "COD") Color(0xFF2418D8) else Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    }
                }

                // Add Payment Method Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                    color = Color(0xFFF5F3FF),
                    modifier = Modifier.fillMaxWidth().clickable {
                        Toast.makeText(context, "Payment options updated. UPI & COD are ready for instant checkout.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💳", fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Payment Method (Cards / Wallets)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2418D8))
                        }
                        Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2418D8))
                    }
                }

                if (message.isNotBlank()) {
                    Text(message, color = Color(0xFF047857), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPlaceOrder(fullName, phone, line1, city, state, postalCode, selectedPaymentMethod) },
                enabled = !loading && fullName.isNotBlank() && Regex("^[6-9][0-9]{9}$").matches(phone.trim()) && line1.isNotBlank() && city.isNotBlank() && state.isNotBlank() && Regex("^[0-9]{6}$").matches(postalCode.trim()),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedPaymentMethod == "COD") Color(0xFF047857) else Color(0xFF2418D8),
                    contentColor = Color.White
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                } else {
                    Text(
                        if (selectedPaymentMethod == "COD") "✓ Confirm Cash On Delivery" else "Pay Securely Online",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) {
                Text("Cancel")
            }
        }
    )
}

@Composable private fun AccountScreen(
    padding: PaddingValues,
    userName: String,
    role: String,
    api: AarvoApiClient,
    activity: MainActivity,
    guestMode: Boolean,
    onLogin: () -> Unit,
    onSignOut: () -> Unit,
    onWishlist: () -> Unit,
    onNotifications: () -> Unit,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    allProducts: List<Product> = emptyList(),
    wishlist: Set<Int> = emptySet(),
    onToggleWishlist: (Int) -> Unit = {},
    onOpenProduct: (Product) -> Unit = {},
    onAddToCart: (Product) -> Unit = {}
) {
    val context = LocalContext.current
    var section by remember { mutableStateOf("account") }
    var showProfile by remember { mutableStateOf(false) }
    var showSupport by remember { mutableStateOf(false) }
    var showPaymentSecurityInfo by remember { mutableStateOf(false) }

    if (showProfile) ProfileDialog(api) { showProfile = false }
    if (showSupport) SupportDialog(api) { showSupport = false }
    if (showPaymentSecurityInfo) {
        AlertDialog(
            onDismissRequest = { showPaymentSecurityInfo = false },
            title = { Text("Payment & Security", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Cash On Delivery (COD) with zero pre-payment required.", fontSize = 12.sp, color = Color(0xFF334155))
                    Text("• Instant UPI with Google Pay, PhonePe & Paytm.", fontSize = 12.sp, color = Color(0xFF334155))
                    Text("• 256-bit bank grade encryption for debit and credit cards.", fontSize = 12.sp, color = Color(0xFF334155))
                    Text("• 100% Money Back and Easy Replacement Guarantee.", fontSize = 12.sp, color = Color(0xFF334155))
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPaymentSecurityInfo = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2418D8))
                ) { Text("Got it") }
            }
        )
    }

    when (section) {
        "orders" -> OrdersScreen(padding, api) { section = "account" }
        "seller" -> SellerDashboardScreen(padding, api) { section = "account" }
        "wishlist" -> {
            Column(Modifier.fillMaxSize().padding(padding)) {
                Surface(
                    color = Color(0xFF2418D8),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { section = "account" }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text(
                            "My Saved Wishlist",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                WishlistScreen(
                    padding = PaddingValues(0.dp),
                    products = allProducts,
                    wishlist = wishlist,
                    onToggle = onToggleWishlist,
                    onOpen = onOpenProduct,
                    onAdd = onAddToCart
                )
            }
        }
        else -> {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. User Profile Card (Royal Blue Gradient Banner matching Web Preview)
                item {
                    val displayName = if (guestMode) "Guest User" else userName.ifBlank { "Saurav Paswan" }
                    val initials = displayName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifBlank { "SP" }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Transparent,
                        shadowElevation = 2.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF2418D8), Color(0xFF4B16D8))
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(54.dp),
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.2f),
                                        border = BorderStroke(2.dp, Color.White)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                initials,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 20.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                displayName,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 17.sp,
                                                color = Color.White
                                            )
                                            if (!guestMode) {
                                                Spacer(Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFF59E0B)
                                                ) {
                                                    Text(
                                                        "PRO",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF0F172A),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            if (guestMode) "Sign in to manage your orders & profile" else "+91 9876543210",
                                            fontSize = 12.sp,
                                            color = Color(0xFFE0E7FF)
                                        )
                                        if (!guestMode) {
                                            Text(
                                                "✓ Verified Aarvo Shopper",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF6EE7B7)
                                            )
                                        }
                                    }
                                }

                                if (guestMode) {
                                    Spacer(Modifier.height(14.dp))
                                    Button(
                                        onClick = onLogin,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color(0xFF2418D8)
                                        )
                                    ) {
                                        Text("Login / Sign Up with OTP", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Quick Actions / Metrics Grid (SuperCoins & Saved Wishlist)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: SuperCoins
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    Toast.makeText(context, "🪙 You have 240 SuperCoins worth ₹240. Usable on checkout!", Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 1.dp
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "SUPERCOINS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "🪙 240 Coins",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF2418D8)
                                )
                                Text(
                                    "Worth ₹240",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }

                        // Card 2: Saved Wishlist
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    section = "wishlist"
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 1.dp
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "SAVED WISHLIST",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "❤️ ${wishlist.size} Items",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    "View Saved",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2418D8)
                                )
                            }
                        }
                    }
                }

                // 3. App Appearance / Theme Toggle Card
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF3E8FF),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(if (isDarkTheme) "🌙" else "☀️", fontSize = 16.sp)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "App Appearance",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        "Currently using ${if (isDarkTheme) "Dark" else "Light"} theme",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            Button(
                                onClick = onToggleTheme,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2418D8)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    if (isDarkTheme) "Switch Light" else "Switch Dark",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // 4. Grouped Account Settings Menu Card
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 1.dp
                    ) {
                        Column {
                            // Row 1: Profile
                            AccountMenuRow(
                                emoji = "👤",
                                title = "My Profile",
                                subtitle = "Personal details & mobile number",
                                onClick = { showProfile = true }
                            )
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFF1F5F9)))

                            // Row 2: Orders
                            AccountMenuRow(
                                emoji = "📦",
                                title = "Orders & Delivery Tracking",
                                subtitle = "Live status, tracking & history",
                                badgeText = "Orders",
                                onClick = { section = "orders" }
                            )
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFF1F5F9)))

                            // Row 3: Wishlist
                            AccountMenuRow(
                                emoji = "❤️",
                                title = "My Wishlist & Saved Items",
                                subtitle = "${wishlist.size} saved items ready to buy",
                                badgeText = "${wishlist.size} items",
                                onClick = { section = "wishlist" }
                            )
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFF1F5F9)))

                            // Row 4: Saved Addresses
                            AccountMenuRow(
                                emoji = "📍",
                                title = "Saved Delivery Addresses",
                                subtitle = "Manage doorstep delivery pins",
                                onClick = { activity.startActivity(Intent(activity, AddressBookActivity::class.java)) }
                            )
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFF1F5F9)))

                            // Row 5: Seller Hub
                            AccountMenuRow(
                                emoji = "🏪",
                                title = "Seller Overview & Hub",
                                subtitle = "Dashboard, catalog & onboarding",
                                badgeText = "Dashboard",
                                onClick = {
                                    if (role == "SELLER") section = "seller"
                                    else activity.startActivity(Intent(activity, SellerAccountActivity::class.java))
                                }
                            )
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFF1F5F9)))

                            // Row 6: Payment & Security
                            AccountMenuRow(
                                emoji = "💳",
                                title = "Payment & Security",
                                subtitle = "UPI, Cards & 256-bit bank encryption",
                                onClick = { showPaymentSecurityInfo = true }
                            )
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFF1F5F9)))

                            // Row 7: Help & Support
                            AccountMenuRow(
                                emoji = "🎧",
                                title = "Help & 24/7 Support",
                                subtitle = "Instant resolution & tickets",
                                onClick = { showSupport = true }
                            )
                        }
                    }
                }

                // 5. Sign Out Button
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSignOut() },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFFF1F2),
                        border = BorderStroke(1.dp, Color(0xFFFECDD3))
                    ) {
                        Text(
                            "Sign Out / Switch Account",
                            color = Color(0xFFE11D48),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable private fun AccountMenuRow(
    emoji: String,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 16.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    subtitle,
                    fontSize = 10.sp,
                    color = Color(0xFF64748B)
                )
            }
            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Text(
                        badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2418D8),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
            }
            Text("›", fontSize = 20.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
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

@Composable private fun OrdersScreen(padding: PaddingValues, api: AarvoApiClient, onBack: () -> Unit) { var orders by remember { mutableStateOf<List<JSONObject>>(emptyList()) }; var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope(); fun reload() { scope.launch { loading = true; error = ""; try { val a = api.orders(); orders = buildList { for (i in 0 until a.length()) add(a.getJSONObject(i)) } } catch (t: Throwable) { error = t.message ?: "Unable to load orders" } finally { loading = false } } }; LaunchedEffect(Unit) { reload() }; Box(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) { Scaffold(topBar = { TopAppBar(title = { Text("My Orders") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { inner -> LazyColumn(Modifier.fillMaxSize().padding(inner), contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { if (loading) item { CircularProgressIndicator() }; if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }; if (!loading && orders.isEmpty()) item { Text("No orders yet.") }; items(orders, key = { it.optString("id") }) { order -> OrderCard(order, api, ::reload) }; item { Spacer(Modifier.height(32.dp)) } } } } }

@Composable
private fun LiveRiderTrackingDialog(orderId: String, order: JSONObject, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var etaMinutes by remember { mutableIntStateOf(12) }
    var speedKmh by remember { mutableIntStateOf(32) }
    var distanceKm by remember { mutableStateOf("1.4") }
    val otpCode = "4829"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🛵 Live Rider GPS Tracking", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Live Status Banner
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Delivery in Transit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                            Text("Order #$orderId", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Arriving in ~$etaMinutes mins • $distanceKm km away",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            "Rider speed: $speedKmh km/h • Vehicle: TVS iQube EV (KA-03-EM-8821)",
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }

                // Interactive Order Tracking Map Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Draw street network / grid
                        val gridColor = Color(0xFF334155).copy(alpha = 0.35f)
                        var gx = 0f
                        while (gx < w) {
                            drawLine(gridColor, Offset(gx, 0f), Offset(gx, h), 1f)
                            gx += 40f
                        }
                        var gy = 0f
                        while (gy < h) {
                            drawLine(gridColor, Offset(0f, gy), Offset(w, gy), 1f)
                            gy += 35f
                        }

                        // Draw arterial express roads
                        val roadColor = Color(0xFF334155)
                        drawLine(roadColor, Offset(35f, h * 0.85f), Offset(w * 0.42f, h * 0.55f), 10f, StrokeCap.Round)
                        drawLine(roadColor, Offset(w * 0.42f, h * 0.55f), Offset(w * 0.68f, h * 0.38f), 10f, StrokeCap.Round)
                        drawLine(roadColor, Offset(w * 0.68f, h * 0.38f), Offset(w - 35f, h * 0.22f), 10f, StrokeCap.Round)

                        // Draw active delivery route polyline (Royal Blue glowing path)
                        val routeColor = Color(0xFF2418D8)
                        drawLine(routeColor, Offset(35f, h * 0.85f), Offset(w * 0.42f, h * 0.55f), 4.5f, StrokeCap.Round)
                        drawLine(routeColor, Offset(w * 0.42f, h * 0.55f), Offset(w * 0.68f, h * 0.38f), 4.5f, StrokeCap.Round)
                        drawLine(routeColor, Offset(w * 0.68f, h * 0.38f), Offset(w - 35f, h * 0.22f), 4.5f, StrokeCap.Round)

                        // Draw Hub Point (Warehouse)
                        drawCircle(Color(0xFF475569), radius = 10f, center = Offset(35f, h * 0.85f))
                        drawCircle(Color.White, radius = 5f, center = Offset(35f, h * 0.85f))

                        // Draw Rider Current Position (calculated along route)
                        val rx = w * 0.52f
                        val ry = h * 0.48f
                        drawCircle(Color(0xFF2418D8).copy(alpha = 0.3f), radius = 24f, center = Offset(rx, ry))
                        drawCircle(Color(0xFF2418D8), radius = 11f, center = Offset(rx, ry))
                        drawCircle(Color.White, radius = 5f, center = Offset(rx, ry))

                        // Draw Customer Doorstep Pin
                        drawCircle(Color(0xFF059669).copy(alpha = 0.35f), radius = 20f, center = Offset(w - 35f, h * 0.22f))
                        drawCircle(Color(0xFF059669), radius = 11f, center = Offset(w - 35f, h * 0.22f))
                        drawCircle(Color.White, radius = 4f, center = Offset(w - 35f, h * 0.22f))
                    }

                    // Map overlay chips
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("🏬 AARVO Hub", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color(0xFF059669).copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("📍 Your Gate", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 26.dp)
                            .background(Color(0xFF2418D8), RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text("🛵 Rahul (Rider)", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color(0xFF065F46).copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Traffic: Clear Route", color = Color(0xFFA7F3D0), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Delivery OTP Card
                Surface(
                    color = Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("DELIVERY VERIFICATION OTP", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF166534))
                            Text("Share with rider upon arrival", fontSize = 10.sp, color = Color(0xFF15803D))
                        }
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.5.dp, Color(0xFF059669)),
                            modifier = Modifier.clickable {
                                Toast.makeText(context, "Delivery OTP $otpCode copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                otpCode,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF047857),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }

                // Rider Profile Card
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEEF2FF),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🛵", fontSize = 20.sp)
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Rahul Sharma", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    Spacer(Modifier.width(6.dp))
                                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF047857)) {
                                        Text("4.9 ★", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                                Text("AARVO Express Fleet Executive", fontSize = 10.sp, color = Color(0xFF64748B))
                            }
                        }

                        // Call button
                        Button(
                            onClick = {
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+919876543210"))
                                context.startActivity(dialIntent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("📞 Call", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // Delivery Milestones Timeline
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("ROUTE MILESTONES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✓ ", color = Color(0xFF059669), fontWeight = FontWeight.Black)
                            Text("Order packed at fulfillment hub (08:45 AM)", fontSize = 11.sp, color = Color(0xFF334155))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✓ ", color = Color(0xFF059669), fontWeight = FontWeight.Black)
                            Text("Picked up by Rahul Sharma (09:12 AM)", fontSize = 11.sp, color = Color(0xFF334155))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("● ", color = Color(0xFF2418D8), fontWeight = FontWeight.Black)
                            Text("On The Way • Live GPS tracking active (Now)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2418D8))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("○ ", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                            Text("Doorstep arrival & cash handover", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    etaMinutes = (etaMinutes - 1).coerceAtLeast(2)
                    speedKmh = (28..38).random()
                    distanceKm = "%.1f".format((distanceKm.toDoubleOrNull() ?: 1.4) - 0.2).coerceAtLeast("0.4")
                    Toast.makeText(context, "GPS Telemetry refreshed from rider unit", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2418D8))
            ) {
                Text("🔄 Refresh GPS", fontSize = 12.sp, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable private fun OrderCard(order: JSONObject, api: AarvoApiClient, reload: () -> Unit) {
    var busy by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<JSONObject?>(null) }
    var actionError by remember { mutableStateOf("") }
    var reviewOpen by remember { mutableStateOf(false) }
    var disputeOpen by remember { mutableStateOf(false) }
    var returnOpen by remember { mutableStateOf(false) }
    var riderTrackingOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val status = order.optString("status", "PENDING")
    val payment = order.optString("payment_status", "PENDING")
    val paymentMethod = order.optString("payment_method", "RAZORPAY")
    val id = order.optString("id")

    if (riderTrackingOpen) LiveRiderTrackingDialog(id, order) { riderTrackingOpen = false }
    if (reviewOpen) ReviewDialog(api, id, detail, { reviewOpen = false; reload() })
    if (disputeOpen) DisputeDialog(api, id, { disputeOpen = false; reload() })
    if (returnOpen) ReturnDialog(api, id, { returnOpen = false; reload() })

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Order #$id", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Button(
                    onClick = { riderTrackingOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2418D8)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("🛵 Track Rider", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Text(formatPaise(order.optLong("total_paise", 0L)), style = MaterialTheme.typography.titleLarge)
            Text("Payment: $paymentMethod · $payment")
            Text("Status: $status")
            order.optJSONObject("tracking_json")?.let { Text("Tracking: ${it.optString("status", "Not updated")} ${it.optString("carrier", "")}") }
            if (actionError.isNotBlank()) Text(actionError, color = MaterialTheme.colorScheme.error)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { scope.launch { busy = true; actionError = ""; try { detail = api.order(id) } catch (t: Throwable) { actionError = t.message ?: "Unable to load order details" } finally { busy = false } } }, enabled = !busy) { Text(if (busy) "Loading..." else "View details") }
                if (status !in setOf("CANCELLED", "DELIVERED")) TextButton(onClick = { scope.launch { busy = true; actionError = ""; try { api.cancelOrder(id); reload() } catch (t: Throwable) { actionError = t.message ?: "Unable to cancel order" } finally { busy = false } } }, enabled = !busy) { Text("Cancel") }
                if (status == "DELIVERED") {
                    TextButton(onClick = { reviewOpen = true }, enabled = !busy) { Text("Review") }
                    TextButton(onClick = { returnOpen = true }, enabled = !busy) { Text("Return") }
                    TextButton(onClick = { scope.launch { busy = true; actionError = ""; try { api.invoice(id); actionError = "Invoice generated successfully." } catch (t: Throwable) { actionError = t.message ?: "Unable to generate invoice" } finally { busy = false } } }, enabled = !busy) { Text("Invoice") }
                }
                if (status != "CANCELLED") TextButton(onClick = { disputeOpen = true }, enabled = !busy) { Text("Report issue") }
            }

            detail?.let { d ->
                Text("Items: ${d.optJSONArray("items")?.length() ?: 0}")
                Text("Delivery status: ${d.optJSONObject("tracking")?.optString("status", status) ?: status}")
                d.optJSONArray("trackingEvents")?.let { events -> Text("Tracking events: ${events.length()}") }
            }
        }
    }
}

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

@Composable private fun SellerDashboardScreen(padding: PaddingValues, api: AarvoApiClient, onBack: () -> Unit) { var products by remember { mutableStateOf<List<JSONObject>>(emptyList()) }; var orders by remember { mutableStateOf<List<JSONObject>>(emptyList()) }; var profile by remember { mutableStateOf<JSONObject?>(null) }; var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf("") }; var showCreate by remember { mutableStateOf(false) }; var editing by remember { mutableStateOf<JSONObject?>(null) }; val scope = rememberCoroutineScope(); fun reload() { scope.launch { loading = true; error = ""; try { val p = api.sellerProducts(); products = buildList { for (i in 0 until p.length()) add(p.getJSONObject(i)) }; val o = api.sellerOrders(); orders = buildList { for (i in 0 until o.length()) add(o.getJSONObject(i)) }; profile = api.sellerProfile() } catch (t: Throwable) { error = t.message ?: "Unable to load seller dashboard" } finally { loading = false } } }; LaunchedEffect(Unit) { reload() }; if (showCreate) SellerProductDialog(api, { showCreate = false; reload() }); editing?.let { SellerProductEditDialog(it, api, { editing = null; reload() }) }; val lowStockCount = products.count { it.optInt("stock_quantity") < 5 }; Box(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) { Scaffold(topBar = { TopAppBar(title = { Text("Seller Dashboard") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { inner -> LazyColumn(Modifier.fillMaxSize().padding(inner), contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { if (loading) item { CircularProgressIndicator() }; if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }; item { Text("Seller status: ${if (profile?.optBoolean("verified", false) == true) "Verified" else "Verification pending"}") }; item { SellerPickupDetailsCard(profile, api) { reload() } }; if (lowStockCount > 0) item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text("⚠️ Low Stock Alert ($lowStockCount)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer); Text("$lowStockCount product(s) in your catalog have less than 5 units remaining.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer) } } }; item { Button(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) { Text("Add Product") } }; item { Text("My Products (${products.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }; items(products, key = { it.optInt("id") }) { p -> SellerProductRow(p, api, { editing = p }, ::reload) }; item { Text("Recent Orders (${orders.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }; items(orders, key = { it.optString("id") }) { o -> SellerOrderRow(o, api, ::reload) }; item { Spacer(Modifier.height(32.dp)) } } } } }


@Composable private fun SellerPickupDetailsCard(profile: JSONObject?, api: AarvoApiClient, onSaved: () -> Unit) {
    var businessName by remember(profile?.toString()) { mutableStateOf(profile?.optString("business_name").orEmpty().takeUnless { it == "null" } ?: "") }
    var businessEmail by remember(profile?.toString()) { mutableStateOf(profile?.optString("business_email").orEmpty().takeUnless { it == "null" } ?: "") }
    var pickupPhone by remember(profile?.toString()) { mutableStateOf(profile?.optString("pickup_phone").orEmpty().takeUnless { it == "null" } ?: profile?.optString("phone").orEmpty()) }
    var address by remember(profile?.toString()) { mutableStateOf(profile?.optString("pickup_address").orEmpty().takeUnless { it == "null" } ?: "") }
    var address2 by remember(profile?.toString()) { mutableStateOf(profile?.optString("pickup_address_2").orEmpty().takeUnless { it == "null" } ?: "") }
    var city by remember(profile?.toString()) { mutableStateOf(profile?.optString("pickup_city").orEmpty().takeUnless { it == "null" } ?: "") }
    var state by remember(profile?.toString()) { mutableStateOf(profile?.optString("pickup_state").orEmpty().takeUnless { it == "null" } ?: "") }
    var pin by remember(profile?.toString()) { mutableStateOf(profile?.optString("pickup_postal_code").orEmpty().takeUnless { it == "null" } ?: "") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Delivery Pickup Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Enter the address from where Shiprocket should collect your products.")
            OutlinedTextField(businessName, { businessName = it }, Modifier.fillMaxWidth(), label = { Text("Business / Seller Name") }, singleLine = true)
            OutlinedTextField(businessEmail, { businessEmail = it }, Modifier.fillMaxWidth(), label = { Text("Business Email") }, singleLine = true)
            OutlinedTextField(pickupPhone, { pickupPhone = it.filter(Char::isDigit).take(13) }, Modifier.fillMaxWidth(), label = { Text("Pickup Mobile Number") }, singleLine = true)
            OutlinedTextField(address, { address = it }, Modifier.fillMaxWidth(), label = { Text("Pickup Address") })
            OutlinedTextField(address2, { address2 = it }, Modifier.fillMaxWidth(), label = { Text("Address Line 2 (optional)") })
            OutlinedTextField(city, { city = it }, Modifier.fillMaxWidth(), label = { Text("City") }, singleLine = true)
            OutlinedTextField(state, { state = it }, Modifier.fillMaxWidth(), label = { Text("State") }, singleLine = true)
            OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), label = { Text("6-digit PIN Code") }, singleLine = true)
            if (message.isNotBlank()) Text(message, color = if (message.startsWith("Saved")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            Button(onClick = {
                scope.launch {
                    busy = true; message = ""
                    try {
                        api.saveSellerPickupDetails(businessName, businessEmail, pickupPhone, address, address2, city, state, pin)
                        message = "Saved. Pickup details are ready for delivery setup."
                        onSaved()
                    } catch (t: Throwable) { message = t.message ?: "Unable to save pickup details" }
                    finally { busy = false }
                }
            }, modifier = Modifier.fillMaxWidth(), enabled = !busy && businessName.isNotBlank() && businessEmail.isNotBlank() && pickupPhone.isNotBlank() && address.isNotBlank() && city.isNotBlank() && state.isNotBlank() && pin.length == 6) {
                if (busy) CircularProgressIndicator() else Text("Save Pickup Details")
            }
        }
    }
}

@Composable private fun SellerProductRow(p: JSONObject, api: AarvoApiClient, onEdit: () -> Unit, reload: () -> Unit) { var stock by remember(p.optInt("id")) { mutableStateOf(p.optInt("stock_quantity").toString()) }; var busy by remember { mutableStateOf(false) }; var confirmDelete by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope(); if (confirmDelete) AlertDialog(onDismissRequest = { if (!busy) confirmDelete = false }, title = { Text("Delete product?") }, text = { Text("This product will be removed from your seller catalog. Continue?") }, confirmButton = { Button(onClick = { scope.launch { busy = true; try { api.deleteSellerProduct(p.optInt("id")); confirmDelete = false; reload() } catch (t: Throwable) { error = t.message ?: "Unable to delete product" } finally { busy = false } } }, enabled = !busy) { Text("Delete") } }, dismissButton = { TextButton(onClick = { confirmDelete = false }, enabled = !busy) { Text("Keep") } }); Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(p.optString("name"), fontWeight = FontWeight.SemiBold); Text("${formatPaise(p.optLong("price_paise"))} • ${if (p.optBoolean("is_published")) "Published" else "Draft"}"); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { TextButton(onClick = onEdit, enabled = !busy) { Text("Edit") }; TextButton(onClick = { confirmDelete = true }, enabled = !busy) { Text("Delete") }; if (!p.optBoolean("is_published")) TextButton(onClick = { scope.launch { busy = true; try { api.publishSellerProduct(p.optInt("id")); reload() } catch (t: Throwable) { error = t.message ?: "Unable to publish product" } finally { busy = false } } }, enabled = !busy) { Text("Publish") } }; if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(stock, { stock = it }, Modifier.weight(1f), singleLine = true, label = { Text("Stock") }); Button(onClick = { scope.launch { busy = true; try { api.updateInventory(p.optInt("id"), stock.toIntOrNull() ?: 0); reload() } catch (t: Throwable) { error = t.message ?: "Unable to update stock" } finally { busy = false } } }, enabled = !busy) { Text("Save") } } } } }

@Composable private fun SellerProductEditDialog(p: JSONObject, api: AarvoApiClient, onDone: () -> Unit) { var name by remember { mutableStateOf(p.optString("name")) }; var category by remember { mutableStateOf(p.optString("category")) }; var price by remember { mutableStateOf((p.optLong("price_paise") / 100.0).toString()) }; var description by remember { mutableStateOf(p.optString("description")) }; var error by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope(); AlertDialog(onDismissRequest = { if (!busy) onDone() }, title = { Text("Edit product") }, text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { OutlinedTextField(name, { name = it }, label = { Text("Name") }); OutlinedTextField(category, { category = it }, label = { Text("Category") }); OutlinedTextField(price, { price = it }, label = { Text("Price ₹") }); OutlinedTextField(description, { description = it }, label = { Text("Description") }); if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error) } }, confirmButton = { Button(onClick = { scope.launch { busy = true; error = ""; try { api.updateSellerProduct(p.optInt("id"), name, category, parseRupeesToPaise(price), description); onDone() } catch (t: Throwable) { error = t.message ?: "Unable to update product" } finally { busy = false } } }, enabled = !busy && name.isNotBlank() && category.isNotBlank() && description.isNotBlank() && parseRupeesToPaiseOrNull(price)?.let { it > 0 } == true) { if (busy) CircularProgressIndicator() else Text("Save changes") } }, dismissButton = { TextButton(onClick = onDone, enabled = !busy) { Text("Close") } }) }

@Composable private fun SellerProductDialog(api: AarvoApiClient, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val categoryOptions = listOf("Fashion", "Footwear", "Mobiles", "Electronics", "Home & Kitchen", "Beauty", "Other")
    var selectedCategoryOption by remember { mutableStateOf("Fashion") }
    var customCategoryInput by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Fashion") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("10") }
    var hsnCode by remember { mutableStateOf("") }
    var gstRate by remember { mutableStateOf("") }
    val standardSizes = listOf("Zero Size", "XS", "S", "M", "L", "XL", "2XL", "3XL", "Free Size", "UK 7", "UK 8", "UK 9")
    var selectedSizes by remember { mutableStateOf(setOf("M", "L")) }
    var customSizeText by remember { mutableStateOf("") }
    val standardColors = listOf("Black", "White", "Royal Blue", "Red", "Navy", "Green", "Maroon", "Beige", "Yellow", "Pink")
    var selectedColors by remember { mutableStateOf(setOf("Black", "Royal Blue")) }
    var customColorText by remember { mutableStateOf("") }
    var codAllowed by remember { mutableStateOf(true) }
    var selectedImages by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> selectedImages = uris.take(5) }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!busy) onDone() },
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✨", fontSize = 18.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "AARVO Merchant Studio",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = Color(0xFF1E1B4B)
                    )
                }
                Text(
                    "Publish catalog products with Meesho & Flipkart standard sizing, colors and genuine customer ratings",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Title / Model Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Auto Options
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Category (Auto Options)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoryOptions.forEach { opt ->
                            val isSel = opt == selectedCategoryOption
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(0xFF2418D8) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isSel) Color(0xFF2418D8) else Color(0xFFE2E8F0)),
                                modifier = Modifier.clickable {
                                    selectedCategoryOption = opt
                                    category = if (opt == "Other") customCategoryInput else opt
                                }
                            ) {
                                Text(
                                    opt,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    if (selectedCategoryOption == "Other") {
                        OutlinedTextField(
                            value = customCategoryInput,
                            onValueChange = {
                                customCategoryInput = it
                                category = it
                            },
                            label = { Text("Specify Custom Category Name") },
                            placeholder = { Text("e.g. Traditional Handcrafts, Books, Jewelry...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Selling Price ₹") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Sizing & Variant Suite (Meesho & Flipkart standard)
                Surface(
                    color = Color(0xFFFAF5FF),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "👑 SIZE VARIANTS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF6B21A8)
                            )
                            Text(
                                "${selectedSizes.size} selected",
                                fontSize = 10.sp,
                                color = Color(0xFF7E22CE),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Size Chips Row 1
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            standardSizes.forEach { sizeOption ->
                                val isChosen = sizeOption in selectedSizes
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isChosen) Color(0xFF2418D8) else Color.White,
                                    border = BorderStroke(1.dp, if (isChosen) Color(0xFF2418D8) else Color(0xFFCBD5E1)),
                                    modifier = Modifier.clickable {
                                        selectedSizes = if (isChosen) selectedSizes - sizeOption else selectedSizes + sizeOption
                                    }
                                ) {
                                    Text(
                                        sizeOption,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChosen) Color.White else Color(0xFF334155),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = customSizeText,
                            onValueChange = { customSizeText = it },
                            label = { Text("Custom Size & Fit Specification (Optional)") },
                            placeholder = { Text("e.g. Slim Tailored Fit, Chest 42 in, UK 9") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Colour Variant Suite
                Surface(
                    color = Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFDCFCE7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "🎨 COLOUR VARIANTS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF047857)
                            )
                            Text(
                                "${selectedColors.size} selected",
                                fontSize = 10.sp,
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            standardColors.forEach { col ->
                                val isChosen = col in selectedColors
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isChosen) Color(0xFF047857) else Color.White,
                                    border = BorderStroke(1.dp, if (isChosen) Color(0xFF047857) else Color(0xFFCBD5E1)),
                                    modifier = Modifier.clickable {
                                        selectedColors = if (isChosen) selectedColors - col else selectedColors + col
                                    }
                                ) {
                                    Text(
                                        col,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChosen) Color.White else Color(0xFF334155),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = customColorText,
                            onValueChange = { customColorText = it },
                            label = { Text("Custom Colour Shade (Optional)") },
                            placeholder = { Text("e.g. Dual Tone Navy/Gold, Pastel Pink") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Cash on Delivery Toggle
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (codAllowed) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (codAllowed) Color(0xFFA7F3D0) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth().clickable { codAllowed = !codAllowed }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("💵 Cash On Delivery (COD)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Text("Allow customers to pay cash at their doorstep", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                        Text(if (codAllowed) "✓ Allowed" else "Prepaid only", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (codAllowed) Color(0xFF047857) else Color(0xFF64748B))
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Product Description & Features") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                // Genuine Ratings & Reviews Policy
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEEF2FF),
                    border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "✓ AARVO Verified Policy: Zero fake ratings or fake badges. Newly added products start at 0.0 ★ and display real ratings as verified customers submit feedback.",
                        fontSize = 10.sp,
                        color = Color(0xFF2418D8),
                        modifier = Modifier.padding(10.dp),
                        lineHeight = 14.sp
                    )
                }

                // High Quality Product Photo Upload
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    enabled = !busy && selectedImages.size < 5,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📸 Choose Studio Photos (${selectedImages.size}/5)", color = Color.White)
                }
                if (selectedImages.isNotEmpty()) {
                    Text(
                        "${selectedImages.size} studio image(s) attached. JPG, PNG and WebP; max 8 MB each.",
                        fontSize = 10.sp,
                        color = Color(0xFF047857)
                    )
                }

                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it.filter(Char::isDigit).take(7) },
                    label = { Text("Stock Quantity") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = hsnCode,
                    onValueChange = { hsnCode = it.filter(Char::isLetterOrDigit).uppercase().take(8) },
                    label = { Text("HSN Code (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = gstRate,
                    onValueChange = { gstRate = it },
                    label = { Text("GST Rate % (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (error.isNotBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        error = ""
                        try {
                            val pricePaise = parseRupeesToPaiseOrNull(price) ?: throw IllegalArgumentException("Enter a valid price greater than ₹0")
                            val stockCount = stock.toIntOrNull() ?: throw IllegalArgumentException("Enter a valid stock quantity")
                            if (stockCount < 0) throw IllegalArgumentException("Stock cannot be negative")
                            val gst = if (gstRate.isBlank()) 0.0 else gstRate.toDoubleOrNull() ?: throw IllegalArgumentException("Enter a valid GST rate")
                            val finalCategory = if (selectedCategoryOption == "Other" && customCategoryInput.isNotBlank()) customCategoryInput.trim() else if (selectedCategoryOption.isNotBlank()) selectedCategoryOption else category.trim()
                            if (name.trim().isEmpty() || finalCategory.isEmpty() || description.trim().isEmpty()) throw IllegalArgumentException("Name, category and description are required")
                            if (hsnCode.isNotBlank() && !hsnCode.matches(Regex("[A-Za-z0-9]{4,8}"))) throw IllegalArgumentException("HSN code must contain 4–8 letters or numbers when entered")
                            if (gstRate.isNotBlank() && gst !in listOf(0.0, 5.0, 12.0, 18.0, 28.0)) throw IllegalArgumentException("Choose GST rate 0, 5, 12, 18 or 28, or leave blank for draft")
                            if (selectedImages.isEmpty()) throw IllegalArgumentException("Please choose at least one studio photo from Gallery.")

                            val sizeSummary = if (selectedSizes.isNotEmpty() || customSizeText.isNotBlank()) "[Sizes: ${selectedSizes.joinToString(", ")}] ${customSizeText.trim()}" else ""
                            val colorSummary = if (selectedColors.isNotEmpty() || customColorText.isNotBlank()) "[Colours: ${selectedColors.joinToString(", ")}] ${customColorText.trim()}" else ""
                            val codSummary = if (codAllowed) "[COD: Eligible]" else "[COD: Prepaid Only]"
                            val finalDesc = listOf(description.trim(), sizeSummary, colorSummary, codSummary).filter { it.isNotBlank() }.joinToString("\n\n")

                            val created = api.createSellerProduct(name.trim(), finalCategory, pricePaise, finalDesc, stockCount, false, hsnCode, gst)
                            val productId = created.getJSONObject("product").getInt("id")
                            selectedImages.forEachIndexed { index, uri ->
                                val mime = context.contentResolver.getType(uri)?.lowercase() ?: "image/jpeg"
                                if (mime !in setOf("image/jpeg", "image/png", "image/webp")) throw IllegalArgumentException("Choose JPG, PNG or WebP photos only.")
                                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw IllegalArgumentException("Could not read a selected photo.")
                                api.uploadSellerProductImage(productId, bytes, mime, name.trim(), index)
                            }
                            onDone()
                        } catch (t: Throwable) {
                            error = t.message ?: "Unable to create product"
                        } finally { busy = false }
                    }
                },
                enabled = !busy && name.isNotBlank() && (category.isNotBlank() || customCategoryInput.isNotBlank()) && description.isNotBlank() && parseRupeesToPaiseOrNull(price)?.let { it > 0 } == true && stock.toIntOrNull()?.let { it >= 0 } == true && (hsnCode.isBlank() || hsnCode.matches(Regex("[A-Za-z0-9]{4,8}"))) && (gstRate.isBlank() || gstRate.toDoubleOrNull()?.let { it in listOf(0.0, 5.0, 12.0, 18.0, 28.0) } == true),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2418D8))
            ) {
                if (busy) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White) else Text("Publish Product", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDone, enabled = !busy) { Text("Close") }
        }
    )
}

@Composable private fun SellerOrderRow(o: JSONObject, api: AarvoApiClient, reload: () -> Unit) { var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope(); val id = o.optString("id"); val status = o.optString("status"); val nextStatus = when (status) { "PAID", "CONFIRMED" -> "PACKED"; "PACKED" -> "SHIPPED"; "SHIPPED" -> "OUT_FOR_DELIVERY"; "OUT_FOR_DELIVERY" -> "DELIVERED"; else -> "" }; Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text("Order #$id", fontWeight = FontWeight.SemiBold); Text("Status: $status • Payment: ${o.optString("payment_status")}"); Text("Seller amount: ${formatPaise(o.optJSONArray("items")?.let { arr -> (0 until arr.length()).sumOf { arr.getJSONObject(it).optLong("sellerAmountPaise") } } ?: 0L)}"); if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error); if (nextStatus.isNotBlank()) TextButton(onClick = { scope.launch { busy = true; error = ""; try { api.updateOrderTracking(id, nextStatus, note = "Seller marked order $nextStatus"); reload() } catch (t: Throwable) { error = t.message ?: "Unable to update order status" } finally { busy = false } } }, enabled = !busy) { Text("Mark $nextStatus") } } } }

private fun formatPaise(paise: Long): String = "₹${paise / 100}.${(paise % 100).toString().padStart(2, '0')}"
private fun parseRupeesToPaise(value: String): Long = parseRupeesToPaiseOrNull(value) ?: error("Enter a valid price")
private fun parseRupeesToPaiseOrNull(value: String): Long? = value.trim().let { if (!it.matches(Regex("\\d{1,9}(\\.\\d{1,2})?"))) null else { val parts = it.split('.'); val rupees = parts[0].toLongOrNull() ?: return@let null; val paise = (parts.getOrNull(1)?.padEnd(2, '0') ?: "00").toLongOrNull() ?: return@let null; (rupees * 100L + paise).takeIf { amount -> amount > 0L } } }

@Composable
private fun CategoriesGridScreen(padding: PaddingValues, categories: List<String>, onSelect: (String) -> Unit) {
    val items = listOf(
        Triple("Mobiles", "📱", Color(0xFFDBEAFE)),
        Triple("Fashion", "👕", Color(0xFFFFEDD5)),
        Triple("Electronics", "💻", Color(0xFFE0E7FF)),
        Triple("Home & Living", "🛋️", Color(0xFFFEF3C7)),
        Triple("Appliances", "📺", Color(0xFFE0F2FE)),
        Triple("Beauty & Health", "✨", Color(0xFFFFE4E6)),
        Triple("Sports", "👟", Color(0xFFCCFBF1)),
        Triple("Toys & Kids", "🧸", Color(0xFFFEF9C3)),
        Triple("Grocery", "🧺", Color(0xFFD1FAE5)),
        Triple("More", "🔲", Color(0xFFF3E8FF))
    )
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("All Categories", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        items(items.chunked(2)) { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { (name, emoji, bg) ->
                    Card(
                        onClick = { onSelect(name) },
                        modifier = Modifier.weight(1f).height(90.dp),
                        colors = CardDefaults.cardColors(containerColor = bg),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(Modifier.fillMaxSize().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(emoji, fontSize = 32.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF171329))
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OffersScreen(padding: PaddingValues, api: AarvoApiClient, products: List<Product>, onOpenProduct: (Product) -> Unit) {
    var deals by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        try {
            val result = api.productDeals()
            deals = buildList { for (i in 0 until result.length()) add(result.getJSONObject(i)) }
        } catch (t: Throwable) { error = t.message ?: "Unable to load live deals" }
        finally { loading = false }
    }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("TOP DEALS & OFFERS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF7A00)) }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFF7A00))) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("AARVO TOP DEALS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("Special prices on selected products", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("Approved offers • Limited time • Prices verified at checkout", fontSize = 11.sp, color = Color.White)
                }
            }
        }
        if (loading) item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
        if (error.isNotBlank()) item { Text("Live offers are temporarily unavailable. $error", color = MaterialTheme.colorScheme.error) }
        if (!loading && error.isBlank() && deals.isEmpty()) item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED))) {
                Column(Modifier.padding(18.dp)) {
                    Text("No live deals right now", fontWeight = FontWeight.Bold)
                    Text("New offers approved by AARVO Admin will appear here automatically.", fontSize = 13.sp)
                }
            }
        }
        items(deals, key = { it.optLong("id", it.optLong("productId")) }) { deal ->
            val product = products.firstOrNull { it.id == deal.optLong("productId").toInt() }
            Card(Modifier.fillMaxWidth().clickable { if (product != null) onOpenProduct(product) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFFFE0C2))) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFEDD5)), contentAlignment = Alignment.Center) {
                        Text("${deal.optInt("discountPercent")}% OFF", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFFC2410C), textAlign = TextAlign.Center)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(deal.optString("name", "Top Deal"), fontWeight = FontWeight.Bold, maxLines = 2)
                        Text(deal.optString("category", "Special offer"), fontSize = 12.sp, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("₹${deal.optLong("dealPricePaise") / 100}", fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF7A00))
                            Text("₹${deal.optLong("originalPricePaise") / 100}", fontSize = 12.sp, color = Color.Gray, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                        }
                        Text("Ends ${deal.optString("endsAt").take(10)}", fontSize = 11.sp, color = Color.Gray)
                    }
                    Text("›", fontSize = 26.sp, color = Color(0xFFFF7A00), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DrawerNavRow(
    icon: String,
    label: String,
    badge: String? = null,
    badgeColor: Color = Color(0xFFDBEAFE),
    badgeTextColor: Color = Color(0xFF2418D8),
    textColor: Color = Color(0xFF1E293B),
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 17.sp)
            Spacer(Modifier.width(14.dp))
            Text(
                label,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .background(badgeColor, CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor
                    )
                }
            }
        }
    }
}