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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
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
    val isAuthenticatedBuyer = signedIn && role != "ADMIN" && role != "RIDER"
    val effectiveUserName = if (isAuthenticatedBuyer) userName else userName.ifBlank { "Guest" }
    val effectiveRole = if (isAuthenticatedBuyer) role else "BUYER"
    val effectiveGuestMode = !isAuthenticatedBuyer
    val handleSignOut = {
        prefs.edit()
            .putBoolean("signed_in", false)
            .putBoolean("guest_mode", true)
            .putString("user_role", "BUYER")
            .remove("auth_token")
            .apply()
        signedIn = false
        guestMode = true
        role = "BUYER"
    }
    AarvoApp(
        userName = effectiveUserName,
        role = effectiveRole,
        api = api,
        activity = activity,
        wishlistStore = wishlistStore,
        saveForLaterStore = saveForLaterStore,
        guestMode = effectiveGuestMode,
        onLogin = openOtpLogin,
        onSignOut = handleSignOut,
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme,
        authRefresh = authRefresh
    )
}

@Composable private fun OnboardingScreen(onLogin: () -> Unit, onGuest: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFFFE5CC),
                        Color(0xFFFFF4E8),
                        Color(0xFFFFFDF9)
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
                        .background(Color(0xFFFFEDE0)),
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
                    color = Color(0xFFFF5722),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Shop Smart • Live Better",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1612),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Discover products. Shop freely. Enjoy\nAARVO.",
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    color = Color(0xFF6B5B53),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(26.dp))
                Button(
                    onClick = onLogin,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5722)
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
                    border = BorderStroke(1.5.dp, Color(0xFFFFB299))
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF6B5B53))
                    Spacer(Modifier.width(9.dp))
                    Text(
                        "Continue as Guest",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F1612)
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

private data class SavedDeliveryLocation(
    val id: String,
    val label: String,
    val fullName: String,
    val phone: String,
    val line1: String,
    val city: String,
    val state: String,
    val pincode: String,
    val isDefault: Boolean = false
) {
    fun shortSummary(): String = listOf(line1, city, state).filter { it.isNotBlank() }.joinToString(", ")
}

private data class AarvoAppToast(
    val id: Long,
    val message: String,
    val type: String = "SUCCESS"
)

private fun inferCityStateForPincode(pin: String): Pair<String, String> = when {
    pin.startsWith("800") -> "Patna" to "Bihar"
    pin.startsWith("110") -> "New Delhi" to "Delhi"
    pin.startsWith("400") -> "Mumbai" to "Maharashtra"
    pin.startsWith("560") -> "Bengaluru" to "Karnataka"
    pin.startsWith("500") -> "Hyderabad" to "Telangana"
    pin.startsWith("600") -> "Chennai" to "Tamil Nadu"
    pin.startsWith("700") -> "Kolkata" to "West Bengal"
    pin.startsWith("411") -> "Pune" to "Maharashtra"
    pin.startsWith("302") -> "Jaipur" to "Rajasthan"
    pin.startsWith("226") -> "Lucknow" to "Uttar Pradesh"
    else -> "India" to "PIN $pin"
}

private fun loadSavedLocationsFromPrefs(prefs: android.content.SharedPreferences): List<SavedDeliveryLocation> {
    val raw = prefs.getString("saved_locations_json", null)
    if (!raw.isNullOrBlank()) {
        val parsed = runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    add(
                        SavedDeliveryLocation(
                            id = o.optString("id", "loc_$i"),
                            label = o.optString("label", "Home"),
                            fullName = o.optString("fullName", "AARVO Shopper"),
                            phone = o.optString("phone", ""),
                            line1 = o.optString("line1", ""),
                            city = o.optString("city", ""),
                            state = o.optString("state", ""),
                            pincode = o.optString("pincode", ""),
                            isDefault = o.optBoolean("isDefault", false)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
        if (parsed.isNotEmpty()) return parsed
    }
    return listOf(
        SavedDeliveryLocation(
            id = "local_default_1",
            label = "Home",
            fullName = "AARVO Shopper",
            phone = "",
            line1 = "Boring Road Crossing",
            city = "Patna",
            state = "Bihar",
            pincode = "800001",
            isDefault = true
        )
    )
}

private fun saveLocationsToPrefs(
    prefs: android.content.SharedPreferences,
    locations: List<SavedDeliveryLocation>
) {
    val arr = JSONArray()
    locations.forEach { loc ->
        arr.put(
            JSONObject()
                .put("id", loc.id)
                .put("label", loc.label)
                .put("fullName", loc.fullName)
                .put("phone", loc.phone)
                .put("line1", loc.line1)
                .put("city", loc.city)
                .put("state", loc.state)
                .put("pincode", loc.pincode)
                .put("isDefault", loc.isDefault)
        )
    }
    prefs.edit().putString("saved_locations_json", arr.toString()).apply()
}

@Composable
private fun AarvoFloatingToastBanner(
    toast: AarvoAppToast?,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = toast != null,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        modifier = modifier
    ) {
        toast?.let { current ->
            val (accent, badgeIcon, bgColor) = when (current.type.uppercase()) {
                "ERROR" -> Triple(
                    Color(0xFFE11D48),
                    "!",
                    if (isDarkTheme) Color(0xFF33141B) else Color(0xFFFFF1F2)
                )
                "WARNING" -> Triple(
                    Color(0xFFD97706),
                    "!",
                    if (isDarkTheme) Color(0xFF2E210F) else Color(0xFFFFFBEB)
                )
                "INFO" -> Triple(
                    Color(0xFFFF5722),
                    "i",
                    if (isDarkTheme) Color(0xFF281912) else Color(0xFFFFF7ED)
                )
                else -> Triple(
                    Color(0xFF059669),
                    "✓",
                    if (isDarkTheme) Color(0xFF10281F) else Color(0xFFECFDF5)
                )
            }
            val textColor = if (isDarkTheme) Color(0xFFFAF3EE) else Color(0xFF0F172A)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                shape = RoundedCornerShape(14.dp),
                color = bgColor,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(badgeIcon, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                    Text(
                        text = current.message,
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss notification",
                            tint = accent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
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
    onToggleTheme: () -> Unit = {},
    authRefresh: Int = 0
) {
    val appPrefs = remember { activity.applicationContext.getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE) }
    var selectedTab by remember { mutableIntStateOf(0) }; var accountSection by remember { mutableStateOf("account") }; var query by remember { mutableStateOf("") }; var category by remember { mutableStateOf("All") }; var sortMode by remember { mutableStateOf("Relevance") }; var minRating by remember { mutableStateOf(0.0) }; var minPrice by remember { mutableStateOf<Long?>(null) }; var maxPrice by remember { mutableStateOf<Long?>(null) }; var inStockOnly by remember { mutableStateOf(false) }; var showFilters by remember { mutableStateOf(false) }; var showNotifications by remember { mutableStateOf(false) }
    var showLocationModal by remember { mutableStateOf(false) }
    var activeToast by remember { mutableStateOf<AarvoAppToast?>(null) }
    var savedLocations by remember { mutableStateOf(loadSavedLocationsFromPrefs(appPrefs)) }
    var appliedCouponCode by remember { mutableStateOf<String?>(null) }
    var couponDiscountPaise by remember { mutableStateOf(0L) }
    var showCouponsModal by remember { mutableStateOf(false) }
    var showHelpModal by remember { mutableStateOf(false) }
    var defaultAddressLine by remember {
        mutableStateOf(
            appPrefs.getString("delivery_address_line", null)?.takeIf { it.isNotBlank() }
                ?: savedLocations.firstOrNull { it.isDefault }?.shortSummary()
                ?: "Patna, Bihar"
        )
    }
    var defaultPincode by remember {
        mutableStateOf(
            appPrefs.getString("delivery_pincode", null)?.takeIf { it.isNotBlank() }
                ?: savedLocations.firstOrNull { it.isDefault }?.pincode
                ?: "800001"
        )
    }
    var unreadNotificationCount by remember { mutableIntStateOf(0) }
    fun showAppToast(message: String, type: String = "SUCCESS") {
        activeToast = AarvoAppToast(id = System.currentTimeMillis(), message = message, type = type)
    }
    LaunchedEffect(activeToast?.id) {
        if (activeToast != null) {
            delay(2600L)
            activeToast = null
        }
    }
    fun selectDeliveryLocation(line: String, pin: String) {
        val cleanLine = line.trim().ifBlank { "Selected Location" }
        val cleanPin = pin.trim()
        defaultAddressLine = cleanLine
        defaultPincode = cleanPin
        appPrefs.edit()
            .putString("delivery_address_line", cleanLine)
            .putString("delivery_pincode", cleanPin)
            .apply()
    }
    val voiceSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                query = spoken
                if (selectedTab != 0 && selectedTab != 1) selectedTab = 0
                showAppToast("Searching for \"$spoken\"", "INFO")
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
            runCatching {
                val addrs = api.addresses()
                if (addrs.length() > 0) {
                    val serverList = buildList {
                        for (i in 0 until addrs.length()) {
                            val o = addrs.getJSONObject(i)
                            add(
                                SavedDeliveryLocation(
                                    id = o.optString("id", "srv_$i"),
                                    label = o.optString("label", "Home").ifBlank { "Home" },
                                    fullName = o.optString("full_name", o.optString("fullName", "AARVO Shopper")),
                                    phone = o.optString("phone", ""),
                                    line1 = o.optString("line1", ""),
                                    city = o.optString("city", ""),
                                    state = o.optString("state", ""),
                                    pincode = o.optString("postal_code", o.optString("postalCode", "")),
                                    isDefault = o.optBoolean("is_default", false) || o.optBoolean("isDefault", false)
                                )
                            )
                        }
                    }
                    if (serverList.isNotEmpty()) {
                        savedLocations = serverList
                        saveLocationsToPrefs(appPrefs, serverList)
                        val def = serverList.firstOrNull { it.isDefault } ?: serverList.first()
                        val formatted = def.shortSummary()
                        if (formatted.isNotBlank() || def.pincode.isNotBlank()) {
                            selectDeliveryLocation(formatted.ifBlank { defaultAddressLine }, def.pincode.ifBlank { defaultPincode })
                        }
                    }
                }
            }
        } else {
            userOrderCount = 0
        }
    }
    val cartItems by cartViewModel.items.collectAsState()
    val cartBadgeCount = cartItems.sumOf { cartViewModel.quantity(it.id) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }; var wishlist by remember { mutableStateOf(wishlistStore.load()) }; var recentlyViewed by remember { mutableStateOf<List<Product>>(emptyList()) }
    var localRecentlyViewedIds by remember { mutableStateOf(loadLocalRecentlyViewedIds(appPrefs)) }
    var merchandisingSections by remember { mutableStateOf(loadCachedMerchandisingConfig(appPrefs)) }
    var hydratedPinnedProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var saveForLater by remember { mutableStateOf(saveForLaterStore.load()) }; var showCheckout by remember { mutableStateOf(false) }; var showLoginRequired by remember { mutableStateOf(false) }; var checkoutLoading by remember { mutableStateOf(false) }; var checkoutMessage by remember { mutableStateOf("") }; var products by remember { mutableStateOf<List<Product>>(emptyList()) }; var allProducts by remember { mutableStateOf<List<Product>>(emptyList()) }; var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope()
    LaunchedEffect(api, guestMode, role, selectedTab, authRefresh) {
        if (selectedTab == 0 || allProducts.isEmpty()) {
            try {
                val arr = api.homeMerchandising()
                val (sections, pinned) = parseRemoteMerchandisingSections(arr)
                merchandisingSections = sections
                hydratedPinnedProducts = pinned
                saveCachedMerchandisingJson(appPrefs, arr)
                if (pinned.isNotEmpty()) {
                    allProducts = (allProducts + pinned).distinctBy { it.id }
                    if (query.isBlank() && category == "All") {
                        products = (products + pinned).distinctBy { it.id }
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Throwable) {
            }
        }
    }
    LaunchedEffect(api, guestMode, role, authRefresh) {
        try {
            val catalog = (api.products("", "All").toProductList() + hydratedPinnedProducts).distinctBy { it.id }
            allProducts = catalog
            cartViewModel.restore(catalog)
            if (!guestMode && role == "BUYER") {
                val server = api.serverCart()
                if (server.length() > 0) {
                    catalog.forEach { product ->
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
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            if (allProducts.isEmpty()) {
                allProducts = hydratedPinnedProducts
            }
        }
    }
    LaunchedEffect(api, guestMode, role, allProducts) {
        if (!guestMode && role == "BUYER" && allProducts.isNotEmpty()) {
            recentlyViewed = try {
                val rows = api.recentlyViewed()
                val ids = (0 until rows.length()).map { rows.getJSONObject(it).optInt("productId", rows.getJSONObject(it).optInt("product_id", 0)) }.filter { it > 0 }
                ids.mapNotNull { id -> allProducts.firstOrNull { it.id == id } }.distinctBy { it.id }.take(10)
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Throwable) {
                emptyList()
            }
        } else recentlyViewed = emptyList()
    }
    LaunchedEffect(query, category, api, authRefresh) {
        loading = true
        error = ""
        try {
            val fetched = api.products(query, category).toProductList()
            products = if (query.isBlank() && category == "All") {
                (fetched + hydratedPinnedProducts).distinctBy { it.id }
            } else {
                fetched.distinctBy { it.id }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            if (products.isEmpty()) {
                products = if (query.isBlank() && category == "All") hydratedPinnedProducts else emptyList()
            }
            val msg = t.message.orEmpty()
            error = if (msg.contains("coroutine", ignoreCase = true) || msg.contains("composition", ignoreCase = true)) {
                ""
            } else {
                msg.ifBlank { "Unable to load products." }
            }
        } finally {
            loading = false
        }
    }
    val visibleProducts = remember(products, sortMode, minRating, minPrice, maxPrice, inStockOnly, query) {
        val minPriceCap = minPrice
        val maxPriceCap = maxPrice
        val filtered = products.filter {
            (minRating <= 0.0 || it.rating >= minRating) &&
                (minPriceCap == null || it.pricePaise >= minPriceCap) &&
                (maxPriceCap == null || it.pricePaise <= maxPriceCap) &&
                (!inStockOnly || it.stockQuantity > 0)
        }
        sortProductsByMode(filtered, sortMode, query)
    }
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
    if (showFilters) {
        ProductFilterDialog(
            sortMode = sortMode,
            minRating = minRating,
            minPrice = minPrice,
            maxPrice = maxPrice,
            inStockOnly = inStockOnly,
            isDarkTheme = isDarkTheme,
            onSort = { sortMode = it },
            onRating = { minRating = it },
            onPriceRange = { mn, mx ->
                minPrice = mn
                maxPrice = mx
            },
            onStock = { inStockOnly = it },
            onReset = {
                sortMode = "Relevance"
                minRating = 0.0
                minPrice = null
                maxPrice = null
                inStockOnly = false
                showAppToast("All filters reset", "INFO")
                showFilters = false
            },
            onClose = {
                showFilters = false
            },
            onApplyNotify = { msg -> showAppToast(msg, "INFO") }
        )
    }
    if (showLocationModal) {
        LocationPickerDialog(
            api = api,
            guestMode = guestMode,
            role = role,
            savedLocations = savedLocations,
            activeSummary = defaultAddressLine,
            activePincode = defaultPincode,
            isDarkTheme = isDarkTheme,
            onUpdateLocations = { updated ->
                savedLocations = updated
                saveLocationsToPrefs(appPrefs, updated)
            },
            onSelectActive = { line, pin ->
                selectDeliveryLocation(line, pin)
                showAppToast("Delivery set to $line - $pin", "SUCCESS")
            },
            onToast = ::showAppToast,
            onDismiss = { showLocationModal = false }
        )
    }
    selectedProduct?.let { product ->
        PreviewProductDetailsScreen(
            product = product,
            isSaved = product.id in wishlist,
            onBack = { selectedProduct = null },
            onToggleWishlist = {
                val added = product.id !in wishlist
                wishlist = wishlistStore.toggle(product.id)
                showAppToast(if (added) "Saved to Wishlist" else "Removed from Wishlist", "INFO")
            },
            onAdd = { p ->
                cartViewModel.add(p)
                syncAuthenticatedCart()
                showAppToast("${p.name} added to Cart", "SUCCESS")
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
                selectedTab = 2
            },
            isDarkTheme = isDarkTheme,
            api = api
        )
        return
    }
    if (showCouponsModal) {
        CouponsModalDialog(
            api = api,
            subtotalPaise = cartItems.sumOf { it.pricePaise },
            appliedCouponCode = appliedCouponCode,
            onApplyCoupon = { code, discount ->
                appliedCouponCode = code
                couponDiscountPaise = discount
            },
            onRemoveCoupon = {
                appliedCouponCode = null
                couponDiscountPaise = 0L
            },
            isDarkTheme = isDarkTheme,
            onDismiss = { showCouponsModal = false }
        )
    }
    if (showHelpModal) {
        HelpCenterModalDialog(
            api = api,
            orderCount = userOrderCount,
            onOpenOrders = { selectedTab = 3 },
            isDarkTheme = isDarkTheme,
            onDismiss = { showHelpModal = false }
        )
    }
    if (showLoginRequired) LoginRequiredDialog({ showLoginRequired = false; onLogin() }, { showLoginRequired = false })
    if (showNotifications) { NotificationsScreen(PaddingValues(0.dp), api) { showNotifications = false }; return }
    if (showCheckout) CheckoutDialog(api, (cartItems.sumOf { it.pricePaise } - couponDiscountPaise).coerceAtLeast(0L), checkoutLoading, checkoutMessage, { if (!checkoutLoading) showCheckout = false }) { fullName, phone, line1, city, state, postalCode, paymentMethod ->
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
                drawerContainerColor = if (isDarkTheme) Color(0xFF181311) else Color(0xFFFFFDF9)
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
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
                                color = Color(0xFFFF5722)
                            )
                        }
                        IconButton(onClick = { scope.launch { drawerState.close() } }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF9E8E84)
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
                            containerColor = if (isDarkTheme) Color(0xFF2B1A12) else Color(0xFFFFF4E8)
                        ),
                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF4A2B1D) else Color(0xFFFFDFC4))
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
                                        color = if (isDarkTheme) Color(0xFFFAF3EE) else Color(0xFF1F1612)
                                    )
                                    Text(
                                        "Click to toggle theme",
                                        fontSize = 10.sp,
                                        color = if (isDarkTheme) Color(0xFFCBBDB4) else Color(0xFF6B5B53)
                                    )
                                }
                            }
                            Button(
                                onClick = onToggleTheme,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF5722),
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
                            selectedTab = 5
                        }
                    )

                    // 4. Orders & Delivery
                    DrawerNavRow(
                        icon = "📦",
                        label = "Orders & Delivery",
                        badge = null,
                        textColor = itemTextColor,
                        onClick = {
                            scope.launch { drawerState.close() }
                            selectedTab = 3
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
                            accountSection = "wishlist"
                            selectedTab = 4
                        }
                    )

                    // 6. Coupons & Promos
                    DrawerNavRow(
                        icon = "🎟️",
                        label = "Coupons & Promos",
                        badge = appliedCouponCode,
                        badgeColor = Color(0xFFECFDF5),
                        badgeTextColor = Color(0xFF059669),
                        textColor = itemTextColor,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showCouponsModal = true
                        }
                    )

                    // 7. Help & Support
                    DrawerNavRow(
                        icon = "🎧",
                        label = "Help & 24/7 Support",
                        textColor = itemTextColor,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showHelpModal = true
                        }
                    )

                    // 8. Seller Business Onboarding
                    DrawerNavRow(
                        icon = "🏪",
                        label = "Seller Business Onboarding",
                        textColor = Color(0xFFE64A19),
                        onClick = {
                            scope.launch { drawerState.close() }
                            activity.startActivity(Intent(activity, SellerOnboardingActivity::class.java))
                        }
                    )

                    Spacer(Modifier.weight(1f))
                    androidx.compose.material3.HorizontalDivider(
                        color = if (isDarkTheme) Color(0xFF332620) else Color(0xFFFFE8D6)
                    )
                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = { scope.launch { drawerState.close() }; onSignOut() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkTheme) Color(0xFF2B1F1A) else Color(0xFFFFF4E8),
                            contentColor = if (isDarkTheme) Color(0xFFFAF3EE) else Color(0xFF1F1612)
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
            containerColor = if (isDarkTheme) Color(0xFF120E0C) else Color(0xFFFFFDF9),
            topBar = {
                AarvoPreviewTopBar(
                    query = query,
                    onQueryChange = {
                        query = it
                        if (selectedTab != 0 && selectedTab != 1) selectedTab = 0
                    },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onVoiceClick = {
                        try {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search products on AARVO...")
                            }
                            voiceSpeechLauncher.launch(intent)
                        } catch (_: Exception) {
                            showAppToast("Voice search is not supported on this device", "WARNING")
                        }
                    },
                    isDarkTheme = isDarkTheme,
                    onLocationClick = { showLocationModal = true },
                    deliverySummary = defaultAddressLine,
                    deliveryPincode = defaultPincode,
                    unreadNotificationCount = unreadNotificationCount,
                    onNotificationsClick = { showNotifications = true },
                    cartBadgeCount = cartBadgeCount,
                    onCartClick = { selectedTab = 2 },
                    onLogoClick = { selectedTab = 0; accountSection = "account" },
                    departments = listOf("All", "Electronics", "Fashion", "Mobiles", "Home & Living", "Grocery", "Beauty & Care", "Appliances", "Sports", "Toys & Kids"),
                    selectedCategory = category,
                    onSelectCategory = { cat ->
                        category = cat
                        if (selectedTab != 0 && selectedTab != 1) selectedTab = 0
                    }
                )
            },
            bottomBar = {
                val navBarColor = if (isDarkTheme) Color(0xFF181311) else Color.White
                val navUnselected = if (isDarkTheme) Color(0xFFCBBDB4) else Color(0xFF6B5B53)
                NavigationBar(containerColor = navBarColor) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, "Home") },
                        label = { Text("Home", fontSize = 10.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFFF5722), selectedTextColor = Color(0xFFFF5722), unselectedIconColor = navUnselected, unselectedTextColor = navUnselected, indicatorColor = Color.Transparent)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            GridVectorIcon(
                                tint = if (selectedTab == 1) Color(0xFFFF5722) else navUnselected,
                                modifier = Modifier.size(21.dp)
                            )
                        },
                        label = { Text("Categories", fontSize = 10.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFFF5722), selectedTextColor = Color(0xFFFF5722), unselectedIconColor = navUnselected, unselectedTextColor = navUnselected, indicatorColor = Color.Transparent)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            if (cartBadgeCount > 0) {
                                BadgedBox(badge = { Badge(containerColor = Color(0xFFFF5722), contentColor = Color.White) { Text(cartBadgeCount.toString()) } }) {
                                    Icon(Icons.Default.ShoppingCart, "Cart")
                                }
                            } else {
                                Icon(Icons.Default.ShoppingCart, "Cart")
                            }
                        },
                        label = { Text("Cart", fontSize = 10.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFFF5722), selectedTextColor = Color(0xFFFF5722), unselectedIconColor = navUnselected, unselectedTextColor = navUnselected, indicatorColor = Color.Transparent)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = {
                            PackageVectorIcon(
                                tint = if (selectedTab == 3) Color(0xFFFF5722) else navUnselected,
                                modifier = Modifier.size(21.dp)
                            )
                        },
                        label = { Text("Orders", fontSize = 10.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFFF5722), selectedTextColor = Color(0xFFFF5722), unselectedIconColor = navUnselected, unselectedTextColor = navUnselected, indicatorColor = Color.Transparent)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { accountSection = "account"; selectedTab = 4 },
                        icon = { Icon(Icons.Default.Person, "Account") },
                        label = { Text("Account", fontSize = 10.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFFF5722), selectedTextColor = Color(0xFFFF5722), unselectedIconColor = navUnselected, unselectedTextColor = navUnselected, indicatorColor = Color.Transparent)
                    )
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> PreviewHomeScreen(
                        padding = padding,
                        api = api,
                        query = query,
                        onQueryChange = { query = it },
                        categories = availableCategories,
                        selectedCategory = category,
                        onCategoryChange = { category = it },
                        products = visibleProducts,
                        allProducts = allProducts,
                        recentlyViewed = recentlyViewed,
                        loading = loading,
                        error = error,
                        onAdd = { p ->
                            cartViewModel.add(p)
                            syncAuthenticatedCart()
                            showAppToast("${p.name} added to Cart", "SUCCESS")
                        },
                        onOpen = { opened ->
                            localRecentlyViewedIds = recordLocalRecentlyViewedId(appPrefs, opened.id)
                            selectedProduct = opened
                        },
                        wishlist = wishlist,
                        onToggleWishlist = { id ->
                            val added = id !in wishlist
                            wishlist = wishlistStore.toggle(id)
                            showAppToast(if (added) "Saved to Wishlist" else "Removed from Wishlist", "INFO")
                        },
                        onFilter = { showFilters = true },
                        sortMode = sortMode,
                        onSortChange = { sortMode = it },
                        minRating = minRating,
                        minPrice = minPrice,
                        maxPrice = maxPrice,
                        onPriceRangeChange = { mn, mx ->
                            minPrice = mn
                            maxPrice = mx
                        },
                        inStockOnly = inStockOnly,
                        onOpenSeller = { activity.startActivity(Intent(activity, SellerOnboardingActivity::class.java)) },
                        onOpenOffers = { selectedTab = 5 },
                        onOpenCategories = { selectedTab = 1 },
                        onShowToast = ::showAppToast,
                        isDarkTheme = isDarkTheme,
                        merchandisingSections = merchandisingSections,
                        recentlyViewedIds = localRecentlyViewedIds,
                        cartProductIds = cartItems.map { it.id }
                    )
                    1 -> PreviewCategoriesExplorerScreen(
                        padding = padding,
                        allProducts = allProducts,
                        selectedCategory = category,
                        onSelectCategory = { category = it },
                        wishlist = wishlist,
                        onToggleWishlist = { id ->
                            val added = id !in wishlist
                            wishlist = wishlistStore.toggle(id)
                            showAppToast(if (added) "Saved to Wishlist" else "Removed from Wishlist", "INFO")
                        },
                        onAdd = { p ->
                            cartViewModel.add(p)
                            syncAuthenticatedCart()
                            showAppToast("${p.name} added to Cart", "SUCCESS")
                        },
                        onOpenProduct = { selectedProduct = it },
                        loading = loading,
                        sortMode = sortMode,
                        onSortChange = { sortMode = it },
                        minPrice = minPrice,
                        maxPrice = maxPrice,
                        onPriceRangeChange = { mn, mx ->
                            minPrice = mn
                            maxPrice = mx
                        },
                        onOpenPriceFilter = { showFilters = true },
                        onShowToast = ::showAppToast,
                        isDarkTheme = isDarkTheme
                    )
                    2 -> CartScreen(
                        padding = padding,
                        items = cartItems,
                        allProducts = allProducts,
                        saveForLater = saveForLater,
                        onIncrement = { product -> cartViewModel.increment(product); syncAuthenticatedCart() },
                        onDecrement = { product -> cartViewModel.decrement(product); syncAuthenticatedCart() },
                        onRemoveAll = { id ->
                            cartViewModel.removeAll(id)
                            syncAuthenticatedCart()
                            showAppToast("Removed item from Cart", "INFO")
                        },
                        quantityOf = cartViewModel::quantity,
                        onClear = {
                            cartViewModel.clear()
                            if (!guestMode && role == "BUYER") scope.launch { runCatching { api.clearServerCart() } }
                            showAppToast("Cart cleared", "INFO")
                        },
                        onSaveForLater = { product ->
                            cartViewModel.removeAll(product.id)
                            saveForLater = saveForLaterStore.toggle(product.id)
                            syncAuthenticatedCart()
                            showAppToast("${product.name} saved for later", "INFO")
                        },
                        onMoveToCart = { product ->
                            saveForLater = saveForLaterStore.remove(product.id)
                            cartViewModel.add(product)
                            syncAuthenticatedCart()
                            showAppToast("${product.name} moved to Cart", "SUCCESS")
                        },
                        onRemoveSaved = { id -> saveForLater = saveForLaterStore.remove(id) },
                        onCheckout = {
                            if (guestMode) showLoginRequired = true else { showCheckout = true; checkoutMessage = "" }
                        },
                        onOpenOrders = { selectedTab = 3 },
                        api = api,
                        userName = userName,
                        deliveryAddressLine = defaultAddressLine,
                        deliveryPincode = defaultPincode,
                        userOrderCount = userOrderCount,
                        appliedCouponCode = appliedCouponCode,
                        couponDiscountPaise = couponDiscountPaise,
                        onApplyCoupon = { code, discount ->
                            appliedCouponCode = code
                            couponDiscountPaise = discount
                            showAppToast("Coupon $code applied", "SUCCESS")
                        },
                        onRemoveCoupon = {
                            appliedCouponCode = null
                            couponDiscountPaise = 0L
                            showAppToast("Coupon removed", "INFO")
                        },
                        onOpenCoupons = { showCouponsModal = true },
                        onChangeAddress = { showLocationModal = true },
                        onExploreProducts = { selectedTab = 0 },
                        isDarkTheme = isDarkTheme
                    )
                    3 -> OrdersScreen(padding, api) { selectedTab = 0 }
                    5 -> PreviewOffersScreen(
                        padding = padding,
                        api = api,
                        products = allProducts,
                        appliedCouponCode = appliedCouponCode,
                        onApplyCoupon = { code, discount ->
                            appliedCouponCode = code
                            couponDiscountPaise = discount
                            showAppToast("Coupon $code applied", "SUCCESS")
                        },
                        onRemoveCoupon = {
                            appliedCouponCode = null
                            couponDiscountPaise = 0L
                            showAppToast("Coupon removed", "INFO")
                        },
                        wishlist = wishlist,
                        onToggleWishlist = { id -> wishlist = wishlistStore.toggle(id) },
                        onAdd = { p ->
                            cartViewModel.add(p)
                            syncAuthenticatedCart()
                            showAppToast("${p.name} added to Cart", "SUCCESS")
                        },
                        onOpenProduct = { selectedProduct = it },
                        isDarkTheme = isDarkTheme
                    )
                    else -> AccountScreen(
                        padding = padding,
                        userName = userName,
                        role = role,
                        api = api,
                        activity = activity,
                        guestMode = guestMode,
                        onLogin = onLogin,
                        onSignOut = onSignOut,
                        onWishlist = { accountSection = "wishlist" },
                        onNotifications = { showNotifications = true },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = onToggleTheme,
                        allProducts = allProducts,
                        wishlist = wishlist,
                        onToggleWishlist = { id -> wishlist = wishlistStore.toggle(id) },
                        onOpenProduct = { selectedProduct = it },
                        onAddToCart = { product ->
                            cartViewModel.add(product)
                            syncAuthenticatedCart()
                            showAppToast("${product.name} added to Cart", "SUCCESS")
                        },
                        userOrderCount = userOrderCount,
                        section = accountSection,
                        onSectionChange = { accountSection = it },
                        appliedCouponCode = appliedCouponCode,
                        onOpenCoupons = { showCouponsModal = true },
                        onOpenHelp = { showHelpModal = true },
                        defaultAddressSummary = "$defaultAddressLine - $defaultPincode",
                        onOpenOrdersTab = { selectedTab = 3 },
                        onOpenLocationModal = { showLocationModal = true }
                    )
                }

                AarvoFloatingToastBanner(
                    toast = activeToast,
                    isDarkTheme = isDarkTheme,
                    onDismiss = { activeToast = null },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = padding.calculateBottomPadding())
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
        itemsIndexed(items, key = { index, n -> "notif_${index}_${n.optString("id")}" }) { _, n -> Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(n.optString("title").ifBlank{"AARVO Update"},fontWeight=FontWeight.Bold);Text(n.optString("message").ifBlank{n.optString("body")});if(!n.optBoolean("read"))TextButton(onClick={scope.launch{runCatching{api.markNotificationRead(n.optLong("id"))};reload()}}){Text("Mark as read")}}}}
    }
}

@Composable private fun LoginRequiredDialog(onLogin: () -> Unit, onDismiss: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text("Login Required") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("To complete your purchase, please login or create an account."); Text("You can still browse and add to cart.", style = MaterialTheme.typography.bodySmall) } }, confirmButton = { Button(onClick = onLogin) { Text("Login / Sign Up") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Continue Browsing") } }) }

@Composable private fun ProductFilterDialog(
    sortMode: String,
    minRating: Double,
    minPrice: Long?,
    maxPrice: Long?,
    inStockOnly: Boolean,
    isDarkTheme: Boolean = false,
    onSort: (String) -> Unit,
    onRating: (Double) -> Unit,
    onPriceRange: (Long?, Long?) -> Unit,
    onStock: (Boolean) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    onApplyNotify: (String) -> Unit = {}
) {
    val initialMinRupees = ((minPrice ?: 0L) / 100L).coerceIn(0L, 25000L)
    val initialMaxRupees = ((maxPrice ?: 2500000L) / 100L).coerceIn(initialMinRupees, 25000L)
    var minPriceText by remember(minPrice) { mutableStateOf(minPrice?.let { (it / 100L).toString() } ?: "") }
    var maxPriceText by remember(maxPrice) { mutableStateOf(maxPrice?.let { (it / 100L).toString() } ?: "") }
    var sliderRange by remember(minPrice, maxPrice) {
        mutableStateOf(initialMinRupees.toFloat()..initialMaxRupees.toFloat())
    }
    val headingColor = if (isDarkTheme) Color(0xFFFAF3EE) else Color(0xFF0F172A)
    val mutedColor = if (isDarkTheme) Color(0xFFCBBDB4) else Color(0xFF64748B)
    val presets = listOf(
        Triple("All", null as Long?, null as Long?),
        Triple("<₹500", null as Long?, 50000L),
        Triple("₹500-2K", 50000L, 200000L),
        Triple("₹2K-10K", 200000L, 1000000L),
        Triple("₹10K+", 1000000L, null as Long?)
    )

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterSlidersVectorIcon(tint = Color(0xFFFF5722), modifier = Modifier.size(18.dp))
                Text("Filters & Price Range", fontWeight = FontWeight.Black, fontSize = 17.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Price Range Presets", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = headingColor)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (label, pMin, pMax) ->
                        val active = minPrice == pMin && maxPrice == pMax
                        Surface(
                            shape = CircleShape,
                            color = if (active) Color(0xFFFF5722) else if (isDarkTheme) Color(0xFF211A17) else Color(0xFFFFF4E8),
                            border = if (active) null else BorderStroke(1.dp, Color(0xFFFED7AA)),
                            modifier = Modifier.clickable {
                                minPriceText = pMin?.let { (it / 100L).toString() } ?: ""
                                maxPriceText = pMax?.let { (it / 100L).toString() } ?: ""
                                val lo = ((pMin ?: 0L) / 100L).coerceIn(0L, 25000L).toFloat()
                                val hi = ((pMax ?: 2500000L) / 100L).coerceIn(lo.toLong(), 25000L).toFloat()
                                sliderRange = lo..hi
                                onPriceRange(pMin, pMax)
                            }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color.White else headingColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Min: ₹${sliderRange.start.toInt()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5722)
                        )
                        Text(
                            "Max: ${if (sliderRange.endInclusive >= 25000f) "₹25,000+" else "₹${sliderRange.endInclusive.toInt()}"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5722)
                        )
                    }
                    RangeSlider(
                        value = sliderRange,
                        onValueChange = { range ->
                            sliderRange = range
                            val lo = range.start.toLong()
                            val hi = range.endInclusive.toLong()
                            minPriceText = if (lo <= 0L) "" else lo.toString()
                            maxPriceText = if (hi >= 25000L) "" else hi.toString()
                        },
                        valueRange = 0f..25000f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF5722),
                            activeTrackColor = Color(0xFFFF5722)
                        )
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minPriceText,
                        onValueChange = { raw ->
                            val clean = raw.filter(Char::isDigit).take(6)
                            minPriceText = clean
                            val lo = (clean.toLongOrNull() ?: 0L).coerceIn(0L, 25000L).toFloat()
                            sliderRange = lo..maxOf(lo, sliderRange.endInclusive)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Min ₹", fontSize = 11.sp) }
                    )
                    OutlinedTextField(
                        value = maxPriceText,
                        onValueChange = { raw ->
                            val clean = raw.filter(Char::isDigit).take(6)
                            maxPriceText = clean
                            val hi = (clean.toLongOrNull() ?: 25000L).coerceIn(0L, 25000L).toFloat()
                            sliderRange = minOf(sliderRange.start, hi)..hi
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Max ₹", fontSize = 11.sp) }
                    )
                }

                Text("Sort By", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = headingColor)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Relevance", "Price: Low to High", "Price: High to Low", "Rating: High to Low", "Discount").forEach { option ->
                        val active = sortMode == option
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) Color(0xFF0F172A) else Color.Transparent,
                            border = BorderStroke(1.dp, if (active) Color(0xFF0F172A) else Color(0xFFCBD5E1)),
                            modifier = Modifier.clickable { onSort(option) }
                        ) {
                            Text(
                                text = option,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) Color.White else headingColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Text("Minimum Rating: ${if (minRating == 0.0) "Any" else "${minRating}★"}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = headingColor)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.0, 3.0, 4.0, 4.5).forEach { r ->
                        val active = minRating == r
                        Surface(
                            shape = CircleShape,
                            color = if (active) Color(0xFFFF5722) else Color.Transparent,
                            border = BorderStroke(1.dp, if (active) Color(0xFFFF5722) else Color(0xFFCBD5E1)),
                            modifier = Modifier.clickable { onRating(r) }
                        ) {
                            Text(
                                text = if (r == 0.0) "Any" else "${r}★",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color.White else headingColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onStock(!inStockOnly) }.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = inStockOnly, onCheckedChange = { onStock(it) })
                    Spacer(Modifier.width(4.dp))
                    Text("In stock items only", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = mutedColor)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedMin = minPriceText.trim().toLongOrNull()?.times(100L)
                    val parsedMax = maxPriceText.trim().toLongOrNull()?.times(100L)
                    val validMin = if (parsedMin != null && parsedMax != null && parsedMin > parsedMax) parsedMax else parsedMin
                    val validMax = if (parsedMin != null && parsedMax != null && parsedMin > parsedMax) parsedMin else parsedMax
                    onPriceRange(validMin, validMax)
                    val summary = when {
                        validMin != null && validMax != null -> "Price ₹${validMin / 100} – ₹${validMax / 100}"
                        validMax != null -> "Price under ₹${validMax / 100}"
                        validMin != null -> "Price ₹${validMin / 100}+"
                        else -> "Filters updated"
                    }
                    onApplyNotify(summary)
                    onClose()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
            ) {
                Text("Apply Filters", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onReset) { Text("Reset", color = Color(0xFFE11D48)) }
                TextButton(onClick = onClose) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun LocationPickerDialog(
    api: AarvoApiClient,
    guestMode: Boolean,
    role: String,
    savedLocations: List<SavedDeliveryLocation>,
    activeSummary: String,
    activePincode: String,
    isDarkTheme: Boolean,
    onUpdateLocations: (List<SavedDeliveryLocation>) -> Unit,
    onSelectActive: (String, String) -> Unit,
    onToast: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var quickPincode by remember(activePincode) { mutableStateOf(activePincode) }
    var showAddForm by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf("Home") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var line1 by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf("") }

    val headingColor = if (isDarkTheme) Color(0xFFFAF3EE) else Color(0xFF0F172A)
    val mutedColor = if (isDarkTheme) Color(0xFFCBBDB4) else Color(0xFF64748B)
    val cardBg = if (isDarkTheme) Color(0xFF211A17) else Color(0xFFFFF8F1)
    val cardBorder = if (isDarkTheme) Color(0xFF3A2D27) else Color(0xFFFED7AA)

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LocationPinVectorIcon(tint = Color(0xFFFF5722), modifier = Modifier.size(20.dp))
                Column {
                    Text("Choose your location", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(
                        "Delivery options & speeds may vary by pincode",
                        fontSize = 11.sp,
                        color = mutedColor
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Active delivery banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LocationPinVectorIcon(tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("CURRENT DELIVERY LOCATION", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF059669))
                            Text(
                                "${activeSummary.ifBlank { "Selected Location" }}${if (activePincode.isNotBlank()) " - $activePincode" else ""}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = headingColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Flipkart-style quick 6-digit Pincode check & apply
                Text("Check Delivery Pincode", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = headingColor)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quickPincode,
                        onValueChange = { quickPincode = it.filter(Char::isDigit).take(6) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("Enter 6-digit pincode", fontSize = 12.sp) }
                    )
                    Button(
                        onClick = {
                            val clean = quickPincode.trim()
                            if (!clean.matches(Regex("^[1-9][0-9]{5}$"))) {
                                formError = "Enter a valid 6-digit Indian pincode"
                                onToast("Enter a valid 6-digit pincode", "WARNING")
                            } else {
                                formError = ""
                                scope.launch {
                                    busy = true
                                    val (c, s) = inferCityStateForPincode(clean)
                                    val live = runCatching { api.checkPincodeServiceability(clean) }.getOrNull()
                                    val riderMsg = live?.optString("riderMessage")?.takeIf { it.isNotBlank() && it != "null" }
                                    onSelectActive("$c, $s", clean)
                                    busy = false
                                    if (!riderMsg.isNullOrBlank()) {
                                        onToast(riderMsg, if (live.optBoolean("serviceable", true)) "SUCCESS" else "WARNING")
                                    }
                                    onDismiss()
                                }
                            }
                        },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(if (busy) "..." else "Apply", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Saved Delivery Addresses
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SAVED ADDRESSES (${savedLocations.size})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = mutedColor
                    )
                    Text(
                        if (showAddForm) "✕ Close Form" else "+ Add New Address",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF5722),
                        modifier = Modifier.clickable {
                            showAddForm = !showAddForm
                            formError = ""
                        }
                    )
                }

                if (showAddForm) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDarkTheme) Color(0xFF181311) else Color(0xFFFFFDF9),
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Home", "Work", "Other").forEach { tag ->
                                    val sel = label == tag
                                    Surface(
                                        shape = CircleShape,
                                        color = if (sel) Color(0xFFFF5722) else Color.Transparent,
                                        border = BorderStroke(1.dp, if (sel) Color(0xFFFF5722) else cardBorder),
                                        modifier = Modifier.clickable { label = tag }
                                    ) {
                                        Text(
                                            tag,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (sel) Color.White else headingColor,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(fullName, { fullName = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Full Name *") })
                            OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(10) }, Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), label = { Text("10-digit Mobile *") })
                            OutlinedTextField(line1, { line1 = it }, Modifier.fillMaxWidth(), label = { Text("House / Street / Area *") })
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(city, { city = it }, Modifier.weight(1f), singleLine = true, label = { Text("City *") })
                                OutlinedTextField(state, { state = it }, Modifier.weight(1f), singleLine = true, label = { Text("State *") })
                            }
                            OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), label = { Text("6-digit Pincode *") })
                            if (formError.isNotBlank()) {
                                Text(formError, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = {
                                    when {
                                        fullName.isBlank() -> formError = "Enter recipient name"
                                        !phone.trim().matches(Regex("^[6-9][0-9]{9}$")) -> formError = "Enter valid 10-digit mobile number"
                                        line1.isBlank() || city.isBlank() || state.isBlank() -> formError = "Enter complete address, city & state"
                                        !pin.trim().matches(Regex("^[1-9][0-9]{5}$")) -> formError = "Enter valid 6-digit pincode"
                                        else -> {
                                            formError = ""
                                            scope.launch {
                                                busy = true
                                                var savedId = "local_${System.currentTimeMillis()}"
                                                if (!guestMode && role == "BUYER") {
                                                    runCatching {
                                                        val resp = api.addAddress(
                                                            JSONObject()
                                                                .put("label", label.trim())
                                                                .put("fullName", fullName.trim())
                                                                .put("phone", phone.trim())
                                                                .put("line1", line1.trim())
                                                                .put("line2", "")
                                                                .put("city", city.trim())
                                                                .put("state", state.trim())
                                                                .put("postalCode", pin.trim())
                                                                .put("country", "IN"),
                                                            true
                                                        )
                                                        val srvId = resp.optString("id")
                                                        if (srvId.isNotBlank()) savedId = srvId
                                                    }
                                                }
                                                val newLoc = SavedDeliveryLocation(
                                                    id = savedId,
                                                    label = label.trim(),
                                                    fullName = fullName.trim(),
                                                    phone = phone.trim(),
                                                    line1 = line1.trim(),
                                                    city = city.trim(),
                                                    state = state.trim(),
                                                    pincode = pin.trim(),
                                                    isDefault = true
                                                )
                                                val nextList = listOf(newLoc) + savedLocations.map { it.copy(isDefault = false) }
                                                onUpdateLocations(nextList)
                                                onSelectActive(newLoc.shortSummary(), newLoc.pincode)
                                                busy = false
                                                showAddForm = false
                                                onDismiss()
                                            }
                                        }
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                            ) {
                                Text(if (busy) "Saving..." else "Save & Deliver Here", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                savedLocations.forEach { loc ->
                    val isSelected = (loc.pincode == activePincode && activeSummary.contains(loc.city, ignoreCase = true)) || loc.isDefault
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) cardBg else if (isDarkTheme) Color(0xFF181311) else Color.White,
                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Color(0xFFFF5722) else cardBorder),
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) {
                            scope.launch {
                                busy = true
                                if (!guestMode && role == "BUYER" && !loc.id.startsWith("local_")) {
                                    runCatching { api.setDefaultAddress(loc.id) }
                                }
                                val updated = savedLocations.map { it.copy(isDefault = it.id == loc.id) }
                                onUpdateLocations(updated)
                                onSelectActive(loc.shortSummary(), loc.pincode)
                                busy = false
                                onDismiss()
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFF5722).copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            loc.label.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFF5722),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        loc.fullName.ifBlank { "AARVO Shopper" },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = headingColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isSelected) {
                                        Text("✓ Active", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF059669))
                                    }
                                }
                                Text(
                                    "${loc.line1}, ${loc.city}, ${loc.state} - ${loc.pincode}",
                                    fontSize = 11.sp,
                                    color = mutedColor
                                )
                                if (loc.phone.isNotBlank()) {
                                    Text("Mobile: ${loc.phone}", fontSize = 10.sp, color = mutedColor)
                                }
                            }
                            if (savedLocations.size > 1) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            busy = true
                                            if (!guestMode && role == "BUYER" && !loc.id.startsWith("local_")) {
                                                runCatching { api.deleteAddress(loc.id) }
                                            }
                                            val remaining = savedLocations.filterNot { it.id == loc.id }
                                            val normalized = if (remaining.isNotEmpty() && remaining.none { it.isDefault }) {
                                                remaining.mapIndexed { idx, item -> item.copy(isDefault = idx == 0) }
                                            } else remaining
                                            onUpdateLocations(normalized)
                                            normalized.firstOrNull { it.isDefault }?.let { nextDef ->
                                                onSelectActive(nextDef.shortSummary(), nextDef.pincode)
                                            }
                                            onToast("Address removed", "INFO")
                                            busy = false
                                        }
                                    },
                                    enabled = !busy,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete address", tint = mutedColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Close") }
        }
    )
}

private fun JSONArray.toProductList(): List<Product> = buildList {
    val seenIds = mutableSetOf<Int>()
    for (i in 0 until length()) {
        val o = optJSONObject(i) ?: continue
        runCatching {
            val id = o.optLong("id", 0L).toInt()
            if (id <= 0 || !seenIds.add(id)) return@runCatching null
            val pricePaise = o.getLong("price_paise")
            if (pricePaise < 0L) return@runCatching null
            val originalPricePaise = o.optLong("original_price_paise", pricePaise).coerceAtLeast(0L)
            val reviewsCount = o.optInt("reviews_count", o.optInt("reviewsCount", 0)).coerceAtLeast(0)
            val deliveryFeePaise = if (o.isNull("delivery_fee_paise")) 0L else o.optLong("delivery_fee_paise", 0L).coerceAtLeast(0L)
            val codEnabled = if (o.isNull("cod_enabled")) true else o.optBoolean("cod_enabled", true)
            Product(
                id = id,
                sellerId = o.optString("seller_id", ""),
                sellerName = o.optString("seller_name", "AARVO Seller"),
                name = o.optString("name", "Product").ifBlank { "Product" },
                category = o.optString("category", "General").ifBlank { "General" },
                price = (pricePaise / 100L).toInt(),
                rating = o.optDouble("rating", 0.0).coerceIn(0.0, 5.0),
                emoji = "🛍️",
                description = o.optString("description", ""),
                stockQuantity = o.optInt("stock_quantity", 0).coerceAtLeast(0),
                isPublished = o.optBoolean("is_published", true),
                pricePaise = pricePaise,
                imageUrl = o.optString("image_url").takeIf { it.startsWith("https://") || it.startsWith("http://") },
                originalPricePaise = originalPricePaise,
                reviewsCount = reviewsCount,
                deliveryFeePaise = deliveryFeePaise,
                codEnabled = codEnabled
            )
        }.getOrNull()?.let(::add)
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
                    Triple("Electronics", "💻", Color(0xFFFFF4E8)),
                    Triple("Home & Living", "🛋️", Color(0xFFFEF3C7)),
                    Triple("Appliances", "📺", Color(0xFFE0F2FE)),
                    Triple("Beauty & Health", "✨", Color(0xFFFFE4E6)),
                    Triple("Sports", "👟", Color(0xFFCCFBF1)),
                    Triple("Toys & Kids", "🧸", Color(0xFFFEF9C3)),
                    Triple("Grocery", "🧺", Color(0xFFD1FAE5)),
                    Triple("More", "🔲", Color(0xFFFFEDE0))
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5722))
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFF8A65)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("%", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Top Deals For You", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text("Verified Offers • Great Value • Limited Time", color = Color(0xFFFFE5CC), fontSize = 10.sp)
                        }
                    }
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("→", color = Color(0xFFFF5722), fontWeight = FontWeight.Black, fontSize = 16.sp)
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
                            color = Color(0xFFFF5722)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            ">",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5722)
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
                                    listOf(Color(0xFFFFEDE0), Color(0xFFFFF4E8))
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
                    containerColor = Color(0xFFFF5722),
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
    val appPrefs = remember(context) { context.getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE) }
    val initialPin = remember {
        appPrefs.getString("delivery_pincode", null)?.takeIf { it.matches(Regex("^[1-9][0-9]{5}$")) } ?: "800001"
    }
    var activeSlideIndex by remember(product.id) { mutableIntStateOf(0) }
    var deliveryPincode by remember { mutableStateOf(initialPin) }
    var showPincodeDialog by remember { mutableStateOf(false) }
    var tempPincode by remember { mutableStateOf(initialPin) }

    // Dynamic customer rating and review states (no pre-seeded fake reviews)
    var currentRating by remember(product.id) { mutableStateOf(product.rating) }
    var currentReviewsCount by remember(product.id) { mutableIntStateOf(product.reviewsCount) }
    var showRateDialog by remember { mutableStateOf(false) }
    var reviewStarSelected by remember { mutableIntStateOf(5) }
    var reviewerName by remember { mutableStateOf("") }
    var reviewerComment by remember { mutableStateOf("") }
    var localReviews by remember(product.id) {
        mutableStateOf<List<Triple<String, Int, String>>>(emptyList())
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
                        placeholder = { Text("e.g. Aarav") },
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
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
                        if (tempPincode.matches(Regex("^[1-9][0-9]{5}$"))) {
                            deliveryPincode = tempPincode
                            appPrefs.edit().putString("delivery_pincode", tempPincode).apply()
                            showPincodeDialog = false
                            Toast.makeText(context, "Delivery PIN updated to $deliveryPincode", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Enter a valid 6-digit Indian PIN code", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
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
                            runCatching {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Check out ${product.name} on AARVO: ₹${product.pricePaise / 100}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Product"))
                            }.onFailure {
                                Toast.makeText(context, "Sharing is not available on this device", Toast.LENGTH_SHORT).show()
                            }
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
                    // Add to Cart Button (AARVO orange outline style)
                    OutlinedButton(
                        onClick = { onAdd(product) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFFF5722)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFFFF5722)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFF5722)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Add to Cart",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5722)
                        )
                    }

                    // Buy Now Button (AARVO Vibrant Orange #FF5722)
                    Button(
                        onClick = { onBuyNow(product) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5722),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            "⚡ Buy Now",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
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
                                    listOf(Color(0xFFFFEDE0), Color(0xFFFFF4E8))
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
                                .background(if (isSelected) Color(0xFFFF5722) else Color(0xFFCBD5E1))
                                .clickable { activeSlideIndex = idx }
                        )
                    }
                }
            }

            // Horizontal Thumbnail Preview Strip
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
                                if (isSelected) Color(0xFFFF5722) else Color(0xFFE2E8F0)
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

                    // Ratings Row
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
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 2. Delivery Card with Inset Payment Protection Box
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
                            "Courier Delivery • PIN $deliveryPincode",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (product.deliveryFeePaise <= 0L) "FREE" else "₹${product.deliveryFeePaise / 100}",
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
                            color = Color(0xFFFF5722),
                            modifier = Modifier.clickable { showPincodeDialog = true }
                        )
                    }

                    // Payment eligibility summary based on product configuration
                    Surface(
                        color = if (product.codEnabled) Color(0xFFFFFBEB) else Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (product.codEnabled) Color(0xFFFDE68A) else Color(0xFFBFDBFE)),
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
                                Text(if (product.codEnabled) "💵" else "💳", fontSize = 20.sp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        if (product.codEnabled) "Cash On Delivery Supported" else "Prepaid Online Checkout",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (product.codEnabled) Color(0xFF92400E) else Color(0xFF1E40AF)
                                    )
                                    Text(
                                        if (product.codEnabled) "Pay on delivery at serviceable PIN codes with purchase protection."
                                        else "Instant UPI, Card & NetBanking protected by Razorpay.",
                                        fontSize = 10.sp,
                                        color = if (product.codEnabled) Color(0xFFB45309) else Color(0xFF1D4ED8),
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
                // 1. Doorstep Delivery
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
                        Text("Doorstep Delivery", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text("Trackable courier", fontSize = 9.sp, color = Color(0xFF64748B))
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

                // 3. Payment Method Eligibility
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
                        Text(if (product.codEnabled) "COD Available" else "Online Pay", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text("Safe checkout", fontSize = 9.sp, color = Color(0xFF64748B))
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

                    listOfNotNull(
                        product.category.takeIf { it.isNotBlank() }?.let { "Category: $it" },
                        "Tracked Doorstep Delivery to PIN $deliveryPincode",
                        if (product.codEnabled) "Cash On Delivery (COD) Eligible at Serviceable Pincodes" else "Prepaid Online Checkout via Razorpay",
                        "7 Days Easy Replacement & Return Policy"
                    ).forEach { bullet ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("• ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5722))
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
                        text = product.description.ifBlank { "No additional description provided by the seller." },
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        lineHeight = 17.sp
                    )

                    Spacer(Modifier.height(4.dp))

                    // Inset Seller Card (no fake seller rating or badge)
                    Surface(
                        color = Color(0xFFFFFBF7),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFE5CC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text("SOLD BY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B5B53), letterSpacing = 0.5.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                product.sellerName.ifBlank { "AARVO Seller" },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F1612)
                            )
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
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
                                    "$currentReviewsCount ratings",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 13.sp
                                )
                            }

                            // Right Rating Progress Bars (computed purely from real submitted reviews)
                            if (localReviews.isNotEmpty()) {
                                val totalLocal = localReviews.size.toFloat()
                                val barColors = mapOf(
                                    5 to Color(0xFF047857),
                                    4 to Color(0xFF10B981),
                                    3 to Color(0xFFF59E0B),
                                    2 to Color(0xFFFB923C),
                                    1 to Color(0xFFEF4444)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    (5 downTo 1).forEach { s ->
                                        val pct = localReviews.count { it.second == s } / totalLocal
                                        val color = barColors[s] ?: Color(0xFF047857)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                "$s ★",
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
    onOpenOrders: (() -> Unit)? = null,
    api: AarvoApiClient? = null,
    userName: String = "Shopper",
    deliveryAddressLine: String = "Select delivery address",
    deliveryPincode: String = "800001",
    userOrderCount: Int = 0,
    appliedCouponCode: String? = null,
    couponDiscountPaise: Long = 0L,
    onApplyCoupon: (String, Long) -> Unit = { _, _ -> },
    onRemoveCoupon: () -> Unit = {},
    onOpenCoupons: () -> Unit = {},
    onChangeAddress: () -> Unit = {},
    onExploreProducts: () -> Unit = {},
    isDarkTheme: Boolean = false
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val groupedItems = items.distinctBy { it.id }
    val totalQty = groupedItems.sumOf { quantityOf(it.id) }
    val totalSellingPaise = groupedItems.sumOf { product -> product.pricePaise * quantityOf(product.id) }
    val totalMrpPaise = groupedItems.sumOf { product ->
        val mrpEach = if (product.originalPricePaise > product.pricePaise) product.originalPricePaise else product.pricePaise
        mrpEach * quantityOf(product.id)
    }
    val totalMrpDiscountPaise = (totalMrpPaise - totalSellingPaise).coerceAtLeast(0L)
    val effectiveCouponPaise = if (appliedCouponCode != null) couponDiscountPaise.coerceAtMost(totalSellingPaise) else 0L
    val finalPayablePaise = (totalSellingPaise - effectiveCouponPaise).coerceAtLeast(0L)
    val totalSavedPaise = totalMrpDiscountPaise + effectiveCouponPaise

    var couponInput by remember(appliedCouponCode) { mutableStateOf(appliedCouponCode ?: "") }
    var couponBusy by remember { mutableStateOf(false) }
    var couponFeedback by remember { mutableStateOf("") }
    var couponFeedbackError by remember { mutableStateOf(false) }

    var cartServiceability by remember(deliveryPincode) { mutableStateOf<JSONObject?>(null) }
    var checkingCartEta by remember(deliveryPincode) { mutableStateOf(false) }

    val primaryCartCategory = groupedItems.firstOrNull()?.category.orEmpty()
    val allCartCodEligible = groupedItems.all { it.codEnabled }

    LaunchedEffect(deliveryPincode, primaryCartCategory, allCartCodEligible, api) {
        val cleanPin = deliveryPincode.filter(Char::isDigit).take(6)
        if (api != null && cleanPin.matches(Regex("^[1-9][0-9]{5}$"))) {
            checkingCartEta = true
            cartServiceability = runCatching {
                api.checkPincodeServiceability(
                    deliveryPincode = cleanPin,
                    cod = allCartCodEligible,
                    category = primaryCartCategory
                )
            }.getOrNull()
            checkingCartEta = false
        } else {
            cartServiceability = null
        }
    }

    val cartEtaSummary = when {
        checkingCartEta -> "Checking delivery date for $deliveryPincode…"
        cartServiceability != null && !cartServiceability!!.optBoolean("serviceable", true) ->
            "Currently unserviceable at PIN $deliveryPincode"
        cartServiceability != null -> {
            val daysText = cartServiceability!!.optString("estimatedDaysText").takeIf { it.isNotBlank() } ?: "3-5 days"
            val etd = cartServiceability!!.optString("etd").takeIf { it.isNotBlank() && it != "null" }
            val freight = cartServiceability!!.optDouble("freightCharge", 0.0).takeIf { !it.isNaN() && it > 0.0 } ?: 0.0
            val shipLabel = if (freight > 0.0) "₹${freight.toInt()}" else "FREE"
            if (etd != null) "Delivery by $etd ($daysText) | $shipLabel" else "Delivery in $daysText | $shipLabel"
        }
        else -> "Delivery date calculated at checkout"
    }

    val cardBg = if (isDarkTheme) Color(0xFF181311) else Color.White
    val cardBorder = if (isDarkTheme) Color(0xFF332620) else Color(0xFFFFDFC4)
    val softOrangeBg = if (isDarkTheme) Color(0xFF211A17) else Color(0xFFFFF4E8)
    val headingColor = if (isDarkTheme) Color(0xFFFAF3EE) else Color(0xFF0F172A)
    val mutedColor = if (isDarkTheme) Color(0xFFCBBDB4) else Color(0xFF64748B)
    val dividerColor = if (isDarkTheme) Color(0xFF2B1F1A) else Color(0xFFF1F5F9)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with View My Orders pill + badge matching Screenshot_20261113-085440
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Shopping Cart (${groupedItems.size})",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = headingColor
                        )
                        Text(
                            text = "All items are directly verified for quality and doorstep delivery",
                            fontSize = 11.sp,
                            color = mutedColor,
                            lineHeight = 15.sp
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (onOpenOrders != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = softOrangeBg,
                                border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                                modifier = Modifier.clickable {
                                    triggerHaptic(haptics)
                                    onOpenOrders()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PackageVectorIcon(tint = Color(0xFFFF5722), modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "View My Orders",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFF5722),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        if (items.isNotEmpty()) {
                            Text(
                                text = "Clear Cart",
                                fontSize = 11.sp,
                                color = Color(0xFFE11D48),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    triggerHaptic(haptics)
                                    onClear()
                                }
                            )
                        }
                    }
                }
            }

            // Delivery Address Strip
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = softOrangeBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MapPinVectorIcon(tint = Color(0xFFFF5722), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Deliver to ${userName.ifBlank { "Shopper" }} - $deliveryPincode",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = headingColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = deliveryAddressLine,
                                fontSize = 11.sp,
                                color = mutedColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Change",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5722),
                            modifier = Modifier.clickable {
                                triggerHaptic(haptics)
                                onChangeAddress()
                            }
                        )
                    }
                }
            }

            if (groupedItems.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = softOrangeBg,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.ShoppingCart,
                                        contentDescription = "Empty Cart",
                                        tint = Color(0xFFFF5722),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Text(
                                "Your cart is empty!",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = headingColor
                            )
                            Text(
                                "Add items that fit your style and daily needs from our verified catalog.",
                                fontSize = 12.sp,
                                color = mutedColor,
                                textAlign = TextAlign.Center
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        triggerHaptic(haptics)
                                        onExploreProducts()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                                ) {
                                    Text("Explore Products", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                if (onOpenOrders != null) {
                                    OutlinedButton(
                                        onClick = {
                                            triggerHaptic(haptics)
                                            onOpenOrders()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, cardBorder)
                                    ) {
                                        Text(
                                            "View My Orders",
                                            color = Color(0xFFFF5722),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                items(groupedItems, key = { it.id }) { product ->
                    val quantity = quantityOf(product.id)
                    val linePricePaise = product.pricePaise * quantity
                    val lineMrpPaise = (if (product.originalPricePaise > product.pricePaise) product.originalPricePaise else product.pricePaise) * quantity

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDarkTheme) Color(0xFF261E1A) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, dividerColor),
                                    modifier = Modifier.size(84.dp)
                                ) {
                                    if (!product.imageUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = product.imageUrl,
                                            contentDescription = product.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(product.emoji, fontSize = 30.sp)
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = product.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = headingColor,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Seller: ${product.sellerName.ifBlank { "AARVO Certified Store" }}",
                                        fontSize = 11.sp,
                                        color = mutedColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = formatPaise(linePricePaise),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            color = headingColor
                                        )
                                        if (product.hasDiscount && lineMrpPaise > linePricePaise) {
                                            Text(
                                                text = formatPaise(lineMrpPaise),
                                                fontSize = 11.sp,
                                                color = mutedColor,
                                                textDecoration = TextDecoration.LineThrough
                                            )
                                            Text(
                                                text = "${product.discountPercent}% OFF",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF059669)
                                            )
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        TruckVectorIcon(tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                                        Text(
                                            text = cartEtaSummary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF059669)
                                        )
                                    }
                                }
                            }

                            Box(Modifier.fillMaxWidth().height(1.dp).background(dividerColor))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "QTY:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = mutedColor
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = cardBg,
                                        border = BorderStroke(1.dp, cardBorder)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    triggerHaptic(haptics)
                                                    onDecrement(product)
                                                },
                                                enabled = quantity > 0,
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Text("−", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = headingColor)
                                            }
                                            Text(
                                                quantity.toString(),
                                                modifier = Modifier.padding(horizontal = 8.dp),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                color = headingColor
                                            )
                                            IconButton(
                                                onClick = {
                                                    triggerHaptic(haptics)
                                                    onIncrement(product)
                                                },
                                                enabled = quantity < product.stockQuantity,
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Text("+", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = headingColor)
                                            }
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = {
                                            triggerHaptic(haptics)
                                            onSaveForLater(product)
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "Save for later",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = headingColor
                                        )
                                    }
                                    Text("|", color = dividerColor, fontSize = 12.sp)
                                    TextButton(
                                        onClick = {
                                            triggerHaptic(haptics)
                                            onRemoveAll(product.id)
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove all",
                                            tint = Color(0xFFE11D48),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "Remove",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE11D48)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Apply Coupon Card
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TagVectorIcon(tint = Color(0xFFFF5722), modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Apply Coupon", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = headingColor)
                                }
                                Text(
                                    "View Offers",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5722),
                                    modifier = Modifier.clickable {
                                        triggerHaptic(haptics)
                                        onOpenCoupons()
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = couponInput,
                                    onValueChange = { couponInput = it.uppercase(); couponFeedback = "" },
                                    singleLine = true,
                                    placeholder = { Text("Enter promo code (e.g. AARVO200)", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        val code = couponInput.trim().uppercase()
                                        if (appliedCouponCode != null && appliedCouponCode.equals(code, ignoreCase = true)) {
                                            triggerHaptic(haptics)
                                            onRemoveCoupon()
                                            couponFeedback = "Coupon removed"
                                            couponFeedbackError = false
                                        } else if (code.isNotBlank()) {
                                            couponBusy = true
                                            couponFeedback = ""
                                            scope.launch {
                                                try {
                                                    val res = api?.validateOffer(code, totalSellingPaise)
                                                    val discount = res?.optLong("discountPaise", res.optLong("discount_paise", 0L))
                                                        ?: when (code) {
                                                            "AARVO200" -> 20000L
                                                            "WELCOME100" -> 10000L
                                                            else -> 0L
                                                        }
                                                    if (discount > 0L) {
                                                        triggerHaptic(haptics, strong = true)
                                                        onApplyCoupon(code, discount)
                                                        couponFeedback = "$code applied! Saving ${formatPaise(discount)}"
                                                        couponFeedbackError = false
                                                    } else {
                                                        couponFeedback = "Coupon not applicable to current subtotal"
                                                        couponFeedbackError = true
                                                    }
                                                } catch (t: Throwable) {
                                                    couponFeedback = t.message ?: "Invalid or expired coupon code"
                                                    couponFeedbackError = true
                                                } finally {
                                                    couponBusy = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !couponBusy && couponInput.trim().isNotBlank(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (appliedCouponCode != null && appliedCouponCode.equals(couponInput.trim(), ignoreCase = true)) Color(0xFF059669) else Color(0xFFFF5722)
                                    ),
                                    modifier = Modifier.height(50.dp)
                                ) {
                                    if (couponBusy) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Text(
                                            if (appliedCouponCode != null && appliedCouponCode.equals(couponInput.trim(), ignoreCase = true)) "Remove" else "Apply",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                            if (couponFeedback.isNotBlank()) {
                                Text(
                                    text = couponFeedback,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (couponFeedbackError) Color(0xFFE11D48) else Color(0xFF059669)
                                )
                            } else if (appliedCouponCode != null && effectiveCouponPaise > 0L) {
                                Text(
                                    text = "Applied $appliedCouponCode • Extra ${formatPaise(effectiveCouponPaise)} off",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF059669)
                                )
                            }
                        }
                    }
                }

                // Price Details Card
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "PRICE DETAILS ($totalQty Items)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = mutedColor
                            )
                            Box(Modifier.fillMaxWidth().height(1.dp).background(dividerColor))

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total MRP", fontSize = 13.sp, color = headingColor)
                                Text(formatPaise(totalMrpPaise), fontSize = 13.sp, color = headingColor, fontWeight = FontWeight.SemiBold)
                            }
                            if (totalMrpDiscountPaise > 0L) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Discount on MRP", fontSize = 13.sp, color = headingColor)
                                    Text("- ${formatPaise(totalMrpDiscountPaise)}", fontSize = 13.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                                }
                            }
                            if (appliedCouponCode != null && effectiveCouponPaise > 0L) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Coupon ($appliedCouponCode)", fontSize = 13.sp, color = headingColor)
                                    Text("- ${formatPaise(effectiveCouponPaise)}", fontSize = 13.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Delivery Charges", fontSize = 13.sp, color = headingColor)
                                Text("FREE", fontSize = 13.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                            }

                            Box(Modifier.fillMaxWidth().height(1.dp).background(dividerColor))

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Amount", fontSize = 15.sp, fontWeight = FontWeight.Black, color = headingColor)
                                Text(formatPaise(finalPayablePaise), fontSize = 16.sp, fontWeight = FontWeight.Black, color = headingColor)
                            }

                            if (totalSavedPaise > 0L) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFECFDF5),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "You will save ${formatPaise(totalSavedPaise)} on this order",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF047857),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Saved for later section
            val savedProducts = allProducts.filter { it.id in saveForLater }
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Saved for later (${savedProducts.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = headingColor
                        )
                        if (savedProducts.isEmpty()) {
                            Text(
                                "No saved items yet. Tap 'Save for later' on any cart item.",
                                fontSize = 12.sp,
                                color = mutedColor
                            )
                        } else {
                            savedProducts.forEach { product ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDarkTheme) Color(0xFF211A17) else Color(0xFFFFFBF7),
                                    border = BorderStroke(1.dp, dividerColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isDarkTheme) Color(0xFF2B221D) else Color(0xFFF8FAFC),
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            if (!product.imageUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = product.imageUrl,
                                                    contentDescription = product.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(contentAlignment = Alignment.Center) { Text(product.emoji, fontSize = 20.sp) }
                                            }
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                product.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = headingColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                product.displayPrice,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp,
                                                color = Color(0xFFFF5722)
                                            )
                                        }
                                        TextButton(onClick = {
                                            triggerHaptic(haptics)
                                            onMoveToCart(product)
                                        }) {
                                            Text("Move to cart", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5722))
                                        }
                                        IconButton(onClick = {
                                            triggerHaptic(haptics)
                                            onRemoveSaved(product.id)
                                        }) {
                                            Icon(Icons.Default.Delete, "Remove saved item", tint = Color(0xFFE11D48), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (groupedItems.isNotEmpty()) {
            Surface(
                color = cardBg,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            formatPaise(finalPayablePaise),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = headingColor
                        )
                        Text(
                            if (totalSavedPaise > 0L) "Total saved ${formatPaise(totalSavedPaise)}" else "Proceed to secure checkout",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF059669)
                        )
                    }
                    Button(
                        onClick = {
                            triggerHaptic(haptics, strong = true)
                            onCheckout()
                        },
                        enabled = items.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text("Place Order ›", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
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
                    color = Color(0xFFFFF4E8),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Order Amount:", fontSize = 12.sp, color = Color(0xFF4A3B34))
                        Text(formatPaise(totalPaise), fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFFFF5722))
                    }
                }

                if (addressLoading) Text("Loading saved delivery addresses...", fontSize = 11.sp, color = Color(0xFF6B5B53))
                if (savedAddresses.isNotEmpty()) {
                    Text("Saved Delivery Addresses", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF1F1612))
                    savedAddresses.forEach { address ->
                        val id = address.optString("id")
                        val label = address.optString("label").ifBlank { "Saved address" }
                        val name = address.optString("full_name", address.optString("fullName"))
                        val addressLine = listOf(address.optString("line1"), address.optString("city"), address.optString("state"), address.optString("postal_code", address.optString("postalCode"))).filter { it.isNotBlank() }.joinToString(", ")
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedAddressId == id) Color(0xFFFFF4E8) else Color(0xFFFFFBF7),
                            border = BorderStroke(1.dp, if (selectedAddressId == id) Color(0xFFFF5722) else Color(0xFFEBD9CA)),
                            modifier = Modifier.fillMaxWidth().clickable { applyAddress(address) }
                        ) {
                            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(if (selectedAddressId == id) "✓ " else "○ ", color = if (selectedAddressId == id) Color(0xFFFF5722) else Color(0xFF9E8E84), fontWeight = FontWeight.Bold)
                                Column {
                                    Text("$label — $name", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(addressLine, fontSize = 10.sp, color = Color(0xFF6B5B53))
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
                        color = if (selectedPaymentMethod != "COD") Color(0xFFFF5722) else Color.Transparent,
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
                    color = if (selectedPaymentMethod != "COD") Color(0xFFFFF4E8) else Color.White,
                    border = BorderStroke(if (selectedPaymentMethod != "COD") 2.dp else 1.dp, if (selectedPaymentMethod != "COD") Color(0xFFFF5722) else Color(0xFFE2E8F0)),
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
                        Text(if (selectedPaymentMethod != "COD") "●" else "○", fontSize = 18.sp, color = if (selectedPaymentMethod != "COD") Color(0xFFFF5722) else Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    }
                }

                // Add Payment Method Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFD8B8)),
                    color = Color(0xFFFFF4E8),
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
                            Text("Add Payment Method (Cards / Wallets)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5722))
                        }
                        Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5722))
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
                    containerColor = if (selectedPaymentMethod == "COD") Color(0xFF047857) else Color(0xFFFF5722),
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
    onAddToCart: (Product) -> Unit = {},
    userOrderCount: Int = 0,
    section: String = "account",
    onSectionChange: (String) -> Unit = {},
    appliedCouponCode: String? = null,
    onOpenCoupons: () -> Unit = {},
    onOpenHelp: () -> Unit = {},
    defaultAddressSummary: String = "",
    onOpenOrdersTab: () -> Unit = { onSectionChange("orders") },
    onOpenLocationModal: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showProfile by remember { mutableStateOf(false) }
    var showSupport by remember { mutableStateOf(false) }
    var showPaymentSecurityInfo by remember { mutableStateOf(false) }
    var showPoliciesDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deleteConfirmInput by remember { mutableStateOf("") }
    var deleteBusy by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf("") }

    val cardBg = if (isDarkTheme) Color(0xFF181311) else Color.White
    val cardBorder = if (isDarkTheme) Color(0xFF332620) else Color(0xFFFFDFC4)
    val headingColor = if (isDarkTheme) Color(0xFFFAF3EE) else Color(0xFF1F1612)
    val mutedColor = if (isDarkTheme) Color(0xFFCBBDB4) else Color(0xFF6B5B53)
    val dividerColor = if (isDarkTheme) Color(0xFF2B1F1A) else Color(0xFFFFEFE0)

    if (showProfile) ProfileDialog(api) { showProfile = false }
    if (showSupport) SupportDialog(api) { showSupport = false }
    if (showPaymentSecurityInfo) {
        AlertDialog(
            onDismissRequest = { showPaymentSecurityInfo = false },
            title = { Text("Payment & Security", fontWeight = FontWeight.Bold, color = headingColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Cash On Delivery (COD) on serviceable pincodes.", fontSize = 12.sp, color = mutedColor)
                    Text("• Instant UPI with Google Pay, PhonePe & Paytm.", fontSize = 12.sp, color = mutedColor)
                    Text("• 256-bit bank grade encryption via Razorpay.", fontSize = 12.sp, color = mutedColor)
                    Text("• 7-Day Easy Replacement & Refund Guarantee.", fontSize = 12.sp, color = mutedColor)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPaymentSecurityInfo = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                ) { Text("Got it", color = Color.White) }
            }
        )
    }
    if (showPoliciesDialog) {
        val publicBaseUrl = BuildConfig.AARVO_API_BASE_URL.trimEnd('/')
        AlertDialog(
            onDismissRequest = { showPoliciesDialog = false },
            title = { Text("Privacy, Terms & Returns", fontWeight = FontWeight.Bold, color = headingColor) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Privacy & Data Protection", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = headingColor)
                    Text(
                        "AARVO collects only the mobile number, delivery address, and order records required to fulfill purchases and courier deliveries. We do not sell personal data. You can permanently delete your account from Account Settings at any time.",
                        fontSize = 11.sp,
                        color = mutedColor
                    )
                    Text("Shipping & Delivery", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = headingColor)
                    Text(
                        "Delivery estimates are computed dynamically by pincode, product category, and parcel size.",
                        fontSize = 11.sp,
                        color = mutedColor
                    )
                    Text("7-Day Replacement & Refund Policy", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = headingColor)
                    Text(
                        "Damaged, defective, or wrong items are eligible for return within 7 days of delivery inside My Orders. Refunds are processed to the original payment method within 5–7 business days.",
                        fontSize = 11.sp,
                        color = mutedColor
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$publicBaseUrl/privacy-policy")))
                            }
                        }) { Text("Privacy Web Page", fontSize = 11.sp, color = Color(0xFFFF5722)) }
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$publicBaseUrl/delete-account")))
                            }
                        }) { Text("Account Deletion Info", fontSize = 11.sp, color = Color(0xFFE11D48)) }
                    }
                    Text(
                        "App Version: v${BuildConfig.VERSION_NAME}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = mutedColor
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPoliciesDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                ) { Text("Close", color = Color.White) }
            }
        )
    }
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { if (!deleteBusy) showDeleteAccountDialog = false },
            title = { Text("Delete AARVO Account", fontWeight = FontWeight.Bold, color = Color(0xFFE11D48)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This permanently deactivates your AARVO account, removes your saved addresses and cart, and signs you out. Completed invoices are retained solely for statutory GST compliance.",
                        fontSize = 12.sp,
                        color = mutedColor
                    )
                    OutlinedTextField(
                        value = deleteConfirmInput,
                        onValueChange = { deleteConfirmInput = it; deleteError = "" },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Type DELETE to confirm") }
                    )
                    if (deleteError.isNotBlank()) {
                        Text(deleteError, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            deleteBusy = true
                            deleteError = ""
                            try {
                                if (!guestMode) {
                                    api.deleteAccount()
                                }
                                showDeleteAccountDialog = false
                                Toast.makeText(context, "Your AARVO account has been deleted.", Toast.LENGTH_LONG).show()
                                onSignOut()
                            } catch (t: Throwable) {
                                deleteError = t.message ?: "Unable to delete account right now"
                            } finally {
                                deleteBusy = false
                            }
                        }
                    },
                    enabled = !deleteBusy && deleteConfirmInput.trim().equals("DELETE", ignoreCase = true),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text(if (deleteBusy) "Deleting..." else "Confirm Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }, enabled = !deleteBusy) {
                    Text("Cancel")
                }
            }
        )
    }

    when (section) {
        "orders" -> OrdersScreen(padding, api) { onSectionChange("account") }
        "seller" -> SellerDashboardScreen(padding, api) { onSectionChange("account") }
        "wishlist" -> {
            Column(Modifier.fillMaxSize().padding(padding)) {
                Surface(
                    color = Color(0xFFFF5722),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onSectionChange("account") }) {
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
                // 1. User Profile Card (Warm AARVO Orange Gradient, No Fake Badges)
                item {
                    val displayName = if (guestMode) "Guest User" else userName.ifBlank { "AARVO Shopper" }
                    val initials = displayName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase().ifBlank { "A" }

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
                                        listOf(Color(0xFFFF5722), Color(0xFFE64A19))
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
                                        Text(
                                            displayName,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 17.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            if (guestMode) "Sign in to manage your orders & profile" else "AARVO Account • Manage orders & profile",
                                            fontSize = 12.sp,
                                            color = Color(0xFFFFE5CC)
                                        )
                                    }
                                    if (!guestMode) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color.White.copy(alpha = 0.22f),
                                            modifier = Modifier.clickable { showProfile = true }
                                        ) {
                                            Text(
                                                "Edit",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
                                            contentColor = Color(0xFFFF5722)
                                        )
                                    ) {
                                        Text("Login / Sign Up with OTP", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. 2x2 Quick Action Tiles Grid (Orders, Wishlist, Coupons, Help Center)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        triggerHaptic(haptics)
                                        onOpenOrdersTab()
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = cardBg,
                                border = BorderStroke(1.dp, cardBorder),
                                shadowElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isDarkTheme) Color(0xFF2B1A12) else Color(0xFFFFF4E8),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            PackageVectorIcon(tint = Color(0xFFFF5722), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("Orders", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = headingColor)
                                        Text(
                                            if (userOrderCount > 0) "$userOrderCount Order(s)" else "Track purchases",
                                            fontSize = 10.sp,
                                            color = Color(0xFFFF5722),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        triggerHaptic(haptics)
                                        onSectionChange("wishlist")
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = cardBg,
                                border = BorderStroke(1.dp, cardBorder),
                                shadowElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isDarkTheme) Color(0xFF2B1A12) else Color(0xFFFFF4E8),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Favorite, contentDescription = "Wishlist", tint = Color(0xFFE11D48), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("Wishlist", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = headingColor)
                                        Text("${wishlist.size} items saved", fontSize = 10.sp, color = mutedColor, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        triggerHaptic(haptics)
                                        onOpenCoupons()
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = cardBg,
                                border = BorderStroke(1.dp, cardBorder),
                                shadowElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isDarkTheme) Color(0xFF2B1A12) else Color(0xFFFFF4E8),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            TagVectorIcon(tint = Color(0xFFFF5722), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("Coupons", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = headingColor)
                                        Text(
                                            text = appliedCouponCode?.let { "Applied: $it" } ?: "Tap to apply promo",
                                            fontSize = 10.sp,
                                            color = if (appliedCouponCode != null) Color(0xFF059669) else Color(0xFFFF5722),
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        triggerHaptic(haptics)
                                        onOpenHelp()
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = cardBg,
                                border = BorderStroke(1.dp, cardBorder),
                                shadowElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isDarkTheme) Color(0xFF2B1A12) else Color(0xFFFFF4E8),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            HelpVectorIcon(tint = Color(0xFFFF5722), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("Help Center", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = headingColor)
                                        Text("FAQs & Support", fontSize = 10.sp, color = mutedColor, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. App Appearance / Theme Toggle Card
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
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
                                    color = if (isDarkTheme) Color(0xFF2B1A12) else Color(0xFFFFEDE0),
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
                                        color = headingColor
                                    )
                                    Text(
                                        "Currently using ${if (isDarkTheme) "Dark" else "Light"} theme",
                                        fontSize = 10.sp,
                                        color = mutedColor
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    triggerHaptic(haptics)
                                    onToggleTheme()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
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
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
                        shadowElevation = 1.dp
                    ) {
                        Column {
                            AccountMenuRow(
                                emoji = "👤",
                                title = "My Profile",
                                subtitle = "Personal details & mobile number",
                                isDarkTheme = isDarkTheme,
                                onClick = { showProfile = true }
                            )
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(dividerColor))

                            AccountMenuRow(
                                emoji = "📦",
                                title = "Orders & Delivery Tracking",
                                subtitle = "Live order status, courier tracking & history",
                                badgeText = null,
                                isDarkTheme = isDarkTheme,
                                onClick = onOpenOrdersTab
                            )
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(dividerColor))

                            AccountMenuRow(
                                emoji = "❤️",
                                title = "My Wishlist & Saved Items",
                                subtitle = "${wishlist.size} saved items ready to buy",
                                badgeText = "${wishlist.size} items",
                                isDarkTheme = isDarkTheme,
                                onClick = { onSectionChange("wishlist") }
                            )
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(dividerColor))

                            AccountMenuRow(
                                emoji = "🎟️",
                                title = "Coupons & Promo Codes",
                                subtitle = appliedCouponCode?.let { "Currently applied: $it" } ?: "Browse & apply promo discounts",
                                badgeText = appliedCouponCode ?: "Offers",
                                isDarkTheme = isDarkTheme,
                                onClick = onOpenCoupons
                            )
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(dividerColor))

                            AccountMenuRow(
                                emoji = "📍",
                                title = "Saved Delivery Addresses",
                                subtitle = defaultAddressSummary.ifBlank { "Manage doorstep delivery pins" },
                                isDarkTheme = isDarkTheme,
                                onClick = onOpenLocationModal
                            )
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(dividerColor))

                            AccountMenuRow(
                                emoji = "🏪",
                                title = "Seller Overview & Hub",
                                subtitle = "Dashboard, catalog & onboarding",
                                badgeText = "Dashboard",
                                isDarkTheme = isDarkTheme,
                                onClick = {
                                    if (role == "SELLER") onSectionChange("seller")
                                    else activity.startActivity(Intent(activity, SellerAccountActivity::class.java))
                                }
                            )
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(dividerColor))

                            AccountMenuRow(
                                emoji = "💳",
                                title = "Payment & Security",
                                subtitle = "UPI, Cards & 256-bit bank encryption",
                                isDarkTheme = isDarkTheme,
                                onClick = { showPaymentSecurityInfo = true }
                            )
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(dividerColor))

                            AccountMenuRow(
                                emoji = "🎧",
                                title = "Help & 24/7 Support",
                                subtitle = "FAQs, delivery & instant resolution",
                                isDarkTheme = isDarkTheme,
                                onClick = onOpenHelp
                            )
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(dividerColor))

                            AccountMenuRow(
                                emoji = "✉️",
                                title = "Raise a Support Ticket",
                                subtitle = "Submit issue directly to AARVO support",
                                isDarkTheme = isDarkTheme,
                                onClick = { showSupport = true }
                            )
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(dividerColor))

                            AccountMenuRow(
                                emoji = "📜",
                                title = "Privacy Policy, Terms & Returns",
                                subtitle = "7-day return rules & user data disclosure",
                                isDarkTheme = isDarkTheme,
                                onClick = { showPoliciesDialog = true }
                            )
                            if (!guestMode) {
                                Box(Modifier.fillMaxWidth().height(0.5.dp).background(dividerColor))
                                AccountMenuRow(
                                    emoji = "🗑️",
                                    title = "Delete My Account",
                                    subtitle = "Permanently remove profile & saved data",
                                    isDarkTheme = isDarkTheme,
                                    onClick = {
                                        deleteConfirmInput = ""
                                        deleteError = ""
                                        showDeleteAccountDialog = true
                                    }
                                )
                            }
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
    isDarkTheme: Boolean = false,
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
                    color = if (isDarkTheme) Color(0xFFFAF3EE) else Color(0xFF1F1612)
                )
                Text(
                    subtitle,
                    fontSize = 10.sp,
                    color = if (isDarkTheme) Color(0xFFCBBDB4) else Color(0xFF6B5B53)
                )
            }
            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDarkTheme) Color(0xFF2B1A12) else Color(0xFFFFF4E8),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF4A2B1D) else Color(0xFFFFDFC4))
                ) {
                    Text(
                        badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5722),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
            }
            Text("›", fontSize = 20.sp, color = Color(0xFF9E8E84), fontWeight = FontWeight.SemiBold)
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

@Composable private fun OrdersScreen(padding: PaddingValues, api: AarvoApiClient, onBack: () -> Unit) { var orders by remember { mutableStateOf<List<JSONObject>>(emptyList()) }; var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope(); fun reload() { scope.launch { loading = true; error = ""; try { val a = api.orders(); orders = buildList { for (i in 0 until a.length()) add(a.getJSONObject(i)) } } catch (t: Throwable) { error = t.message ?: "Unable to load orders" } finally { loading = false } } }; LaunchedEffect(Unit) { reload() }; Box(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) { Scaffold(topBar = { TopAppBar(title = { Text("My Orders") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { inner -> LazyColumn(Modifier.fillMaxSize().padding(inner), contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { if (loading) item { CircularProgressIndicator() }; if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }; if (!loading && orders.isEmpty()) item { Text("No orders yet.") }; itemsIndexed(orders, key = { index, order -> "order_${index}_${order.optString("id")}" }) { _, order -> OrderCard(order, api, ::reload) }; item { Spacer(Modifier.height(32.dp)) } } } } }

@Composable
private fun LiveRiderTrackingDialog(orderId: String, order: JSONObject, api: AarvoApiClient, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var events by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }

    LaunchedEffect(orderId) {
        loading = true
        errorMsg = ""
        try {
            val arr = api.orderTracking(orderId)
            events = buildList { for (i in 0 until arr.length()) add(arr.getJSONObject(i)) }
        } catch (t: Throwable) {
            errorMsg = t.message ?: "Unable to fetch latest courier events"
        } finally {
            loading = false
        }
    }

    val trackingObj = order.optJSONObject("tracking_json")
    val currentStatus = trackingObj?.optString("status")?.takeIf { it.isNotBlank() && it != "null" }
        ?: order.optString("status", "PROCESSING")
    val rawCarrier = trackingObj?.optString("carrier")?.takeIf { it.isNotBlank() && it != "null" } ?: ""
    val carrier = if (rawCarrier.isBlank() || rawCarrier.contains("shiprocket", ignoreCase = true)) "Express Courier Partner" else rawCarrier
    val awbCode = trackingObj?.optString("trackingCode")?.takeIf { it.isNotBlank() && it != "null" }
        ?: trackingObj?.optString("awb")?.takeIf { it.isNotBlank() && it != "null" }
        ?: ""
    val etd = trackingObj?.optString("etd")?.takeIf { it.isNotBlank() && it != "null" }
    val trackingUrl = trackingObj?.optString("trackingUrl")?.takeIf { it.startsWith("https://") }
        ?: if (awbCode.isNotBlank()) "https://shiprocket.co/tracking/${Uri.encode(awbCode)}" else ""

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("🚚 Shipment & Courier Tracking", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = Color(0xFFFFF4E8),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFDFC4)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(currentStatus, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFE64A19))
                            Text("Order #$orderId", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                        }
                        Text("Courier: $carrier", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        if (awbCode.isNotBlank()) {
                            Text("AWB / Tracking ID: $awbCode", fontSize = 11.sp, color = Color(0xFF334155))
                        } else {
                            Text("AWB will be generated once the seller packs & schedules pickup.", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                        if (etd != null) {
                            Text("Estimated Delivery: $etd", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF059669))
                        }
                    }
                }

                if (trackingUrl.isNotBlank()) {
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(trackingUrl)))
                            }.onFailure {
                                Toast.makeText(context, "Unable to open tracking link", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Live Courier Tracking", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("SHIPMENT TIMELINE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp))
                        } else if (errorMsg.isNotBlank()) {
                            Text(errorMsg, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        } else if (events.isEmpty()) {
                            Text("No courier scan updates yet. Updates appear automatically after seller pickup.", fontSize = 11.sp, color = Color(0xFF64748B))
                        } else {
                            events.forEach { ev ->
                                val evStatus = ev.optString("status", "UPDATE")
                                val evNote = ev.optString("note", "").takeIf { it.isNotBlank() && it != "null" } ?: ev.optString("carrier", "")
                                val evTime = ev.optString("created_at", "").take(16).replace("T", " ")
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("✓ $evStatus", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    if (evNote.isNotBlank()) {
                                        Text(evNote, fontSize = 10.sp, color = Color(0xFF475569))
                                    }
                                    if (evTime.isNotBlank()) {
                                        Text(evTime, fontSize = 9.sp, color = Color(0xFF94A3B8))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
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
    var detailsDialogOpen by remember { mutableStateOf(false) }
    var riderTrackingOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val status = order.optString("status", "PENDING")
    val payment = order.optString("payment_status", "PENDING")
    val paymentMethod = order.optString("payment_method", "RAZORPAY")
    val id = order.optString("id")

    if (riderTrackingOpen) LiveRiderTrackingDialog(id, order, api) { riderTrackingOpen = false }
    if (reviewOpen) ReviewDialog(api, id, detail, { reviewOpen = false; reload() })
    if (disputeOpen) DisputeDialog(api, id, { disputeOpen = false; reload() })
    if (returnOpen) ReturnDialog(api, id, { returnOpen = false; reload() })
    if (detailsDialogOpen && detail != null) {
        OrderDetailsDialog(order = order, detail = detail!!, onDismiss = { detailsDialogOpen = false })
    }

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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("🚚 Track Shipment", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Text(formatPaise(order.optLong("total_paise", 0L)), style = MaterialTheme.typography.titleLarge)
            Text("Payment: $paymentMethod · $payment")
            Text("Status: $status")
            order.optJSONObject("tracking_json")?.let { Text("Tracking: ${it.optString("status", "Not updated")} ${it.optString("carrier", "")}") }
            if (actionError.isNotBlank()) Text(actionError, color = MaterialTheme.colorScheme.error)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            actionError = ""
                            try {
                                detail = api.order(id)
                                detailsDialogOpen = true
                            } catch (t: Throwable) {
                                actionError = t.message ?: "Unable to load order details"
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy
                ) { Text(if (busy) "Loading..." else "View details") }
                if (status !in setOf("CANCELLED", "DELIVERED")) TextButton(onClick = { scope.launch { busy = true; actionError = ""; try { api.cancelOrder(id); reload() } catch (t: Throwable) { actionError = t.message ?: "Unable to cancel order" } finally { busy = false } } }, enabled = !busy) { Text("Cancel") }
                if (status == "DELIVERED") {
                    TextButton(onClick = { reviewOpen = true }, enabled = !busy) { Text("Review") }
                    TextButton(onClick = { returnOpen = true }, enabled = !busy) { Text("Return") }
                    TextButton(onClick = { scope.launch { busy = true; actionError = ""; try { api.invoice(id); actionError = "Invoice generated successfully." } catch (t: Throwable) { actionError = t.message ?: "Unable to generate invoice" } finally { busy = false } } }, enabled = !busy) { Text("Invoice") }
                }
                if (status != "CANCELLED") TextButton(onClick = { disputeOpen = true }, enabled = !busy) { Text("Report issue") }
            }
        }
    }
}

@Composable
private fun OrderDetailsDialog(order: JSONObject, detail: JSONObject, onDismiss: () -> Unit) {
    val id = order.optString("id")
    val status = order.optString("status", "PENDING")
    val paymentMethod = order.optString("payment_method", "RAZORPAY")
    val paymentStatus = order.optString("payment_status", "PENDING")
    val createdAt = order.optString("created_at", "").take(10)
    val items = detail.optJSONArray("items") ?: JSONArray()
    val shipping = order.optJSONObject("shipping_address_json")
        ?: detail.optJSONObject("order")?.optJSONObject("shipping_address_json")
        ?: detail.optJSONObject("shippingAddress")
    val tracking = detail.optJSONObject("tracking") ?: order.optJSONObject("tracking_json")
    val subtotalPaise = order.optLong("subtotal_paise", detail.optJSONObject("order")?.optLong("subtotal_paise", 0L) ?: 0L)
    val deliveryPaise = order.optLong("delivery_fee_paise", detail.optJSONObject("order")?.optLong("delivery_fee_paise", 0L) ?: 0L)
    val totalPaise = order.optLong("total_paise", detail.optJSONObject("order")?.optLong("total_paise", 0L) ?: 0L)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Order #$id", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = Color(0xFFFFF4E8),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFDFC4)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Status: $status", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE64A19))
                        Text("Payment: $paymentMethod ($paymentStatus)", fontSize = 11.sp, color = Color(0xFF475569))
                        if (createdAt.isNotBlank()) Text("Placed on: $createdAt", fontSize = 10.sp, color = Color(0xFF64748B))
                        tracking?.let { tr ->
                            val rawCarrier = tr.optString("carrier", "")
                            val carrier = if (rawCarrier.isBlank() || rawCarrier.contains("shiprocket", ignoreCase = true)) "Express Courier Partner" else rawCarrier
                            val awb = tr.optString("trackingCode", tr.optString("awb", ""))
                            if (carrier.isNotBlank() || awb.isNotBlank()) {
                                Text("Courier: $carrier${if (awb.isNotBlank()) " • AWB $awb" else ""}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                            }
                        }
                    }
                }

                if (shipping != null) {
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("DELIVERY ADDRESS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            val fullName = shipping.optString("fullName", shipping.optString("name", ""))
                            val phone = shipping.optString("phone", "")
                            val addrLine = listOf(
                                shipping.optString("line1", ""),
                                shipping.optString("line2", ""),
                                shipping.optString("city", ""),
                                shipping.optString("state", ""),
                                shipping.optString("postalCode", "")
                            ).filter { it.isNotBlank() && it != "null" }.joinToString(", ")
                            if (fullName.isNotBlank()) Text(fullName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            if (addrLine.isNotBlank()) Text(addrLine, fontSize = 11.sp, color = Color(0xFF334155))
                            if (phone.isNotBlank()) Text("Phone: $phone", fontSize = 11.sp, color = Color(0xFF475569))
                        }
                    }
                }

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ITEMS (${items.length()})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        for (i in 0 until items.length()) {
                            val item = items.optJSONObject(i) ?: continue
                            val itemName = item.optString("product_name", item.optString("name", "Product"))
                            val qty = item.optInt("quantity", 1).coerceAtLeast(1)
                            val linePaise = item.optLong("line_total_paise", item.optLong("unit_price_paise", 0L) * qty)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("$itemName × $qty", fontSize = 11.sp, color = Color(0xFF0F172A), modifier = Modifier.weight(1f))
                                Text(formatPaise(linePaise), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                        }
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFE2E8F0)))
                        if (subtotalPaise > 0L) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Items Subtotal", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text(formatPaise(subtotalPaise), fontSize = 11.sp, color = Color(0xFF334155))
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Delivery Fee", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(if (deliveryPaise <= 0L) "FREE" else formatPaise(deliveryPaise), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF059669))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Amount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Text(formatPaise(totalPaise), fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF5722))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
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

@Composable private fun SellerDashboardScreen(padding: PaddingValues, api: AarvoApiClient, onBack: () -> Unit) { var products by remember { mutableStateOf<List<JSONObject>>(emptyList()) }; var orders by remember { mutableStateOf<List<JSONObject>>(emptyList()) }; var profile by remember { mutableStateOf<JSONObject?>(null) }; var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf("") }; var showCreate by remember { mutableStateOf(false) }; var editing by remember { mutableStateOf<JSONObject?>(null) }; val scope = rememberCoroutineScope(); fun reload() { scope.launch { loading = true; error = ""; try { val p = api.sellerProducts(); products = buildList { for (i in 0 until p.length()) add(p.getJSONObject(i)) }; val o = api.sellerOrders(); orders = buildList { for (i in 0 until o.length()) add(o.getJSONObject(i)) }; profile = api.sellerProfile() } catch (t: Throwable) { error = t.message ?: "Unable to load seller dashboard" } finally { loading = false } } }; LaunchedEffect(Unit) { reload() }; if (showCreate) SellerProductDialog(api, { showCreate = false; reload() }); editing?.let { SellerProductEditDialog(it, api, { editing = null; reload() }) }; val lowStockCount = products.count { it.optInt("stock_quantity") < 5 }; Box(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) { Scaffold(topBar = { TopAppBar(title = { Text("Seller Dashboard") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { inner -> LazyColumn(Modifier.fillMaxSize().padding(inner), contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { if (loading) item { CircularProgressIndicator() }; if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }; item { Text("Seller status: ${if (profile?.optBoolean("verified", false) == true) "Verified" else "Verification pending"}") }; item { SellerPickupDetailsCard(profile, api) { reload() } }; if (lowStockCount > 0) item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text("⚠️ Low Stock Alert ($lowStockCount)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer); Text("$lowStockCount product(s) in your catalog have less than 5 units remaining.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer) } } }; item { Button(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) { Text("Add Product") } }; item { Text("My Products (${products.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }; itemsIndexed(products, key = { index, p -> "sprod_${index}_${p.optInt("id")}" }) { _, p -> SellerProductRow(p, api, { editing = p }, ::reload) }; item { Text("Recent Orders (${orders.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }; itemsIndexed(orders, key = { index, o -> "sorder_${index}_${o.optString("id")}" }) { _, o -> SellerOrderRow(o, api, ::reload) }; item { Spacer(Modifier.height(32.dp)) } } } } }


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
            Text("Enter the address from where courier partners should collect your products.")
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
    var selectedSizes by remember { mutableStateOf<Set<String>>(emptySet()) }
    var customSizeText by remember { mutableStateOf("") }
    val standardColors = listOf("Black", "White", "Royal Blue", "Red", "Navy", "Green", "Maroon", "Beige", "Yellow", "Pink")
    var selectedColors by remember { mutableStateOf<Set<String>>(emptySet()) }
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
                        color = Color(0xFF1F1612)
                    )
                }
                Text(
                    "Publish catalog products with Meesho & Flipkart standard sizing, colors and genuine customer ratings",
                    fontSize = 11.sp,
                    color = Color(0xFF6B5B53)
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
                                color = if (isSel) Color(0xFFFF5722) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isSel) Color(0xFFFF5722) else Color(0xFFE2E8F0)),
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
                    color = Color(0xFFFFF4E8),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFDFC4)),
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
                                color = Color(0xFFE64A19)
                            )
                            Text(
                                "${selectedSizes.size} selected",
                                fontSize = 10.sp,
                                color = Color(0xFFFF5722),
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
                                    color = if (isChosen) Color(0xFFFF5722) else Color.White,
                                    border = BorderStroke(1.dp, if (isChosen) Color(0xFFFF5722) else Color(0xFFCBD5E1)),
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
                    color = Color(0xFFFFF4E8),
                    border = BorderStroke(1.dp, Color(0xFFFFDFC4)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "✓ AARVO Verified Policy: Zero fake ratings or fake badges. Newly added products start at 0.0 ★ and display real ratings as verified customers submit feedback.",
                        fontSize = 10.sp,
                        color = Color(0xFFE64A19),
                        modifier = Modifier.padding(10.dp),
                        lineHeight = 14.sp
                    )
                }

                // High Quality Product Photo Upload
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    enabled = !busy && selectedImages.size < 5,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
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
        Triple("Electronics", "💻", Color(0xFFFFF4E8)),
        Triple("Home & Living", "🛋️", Color(0xFFFEF3C7)),
        Triple("Appliances", "📺", Color(0xFFE0F2FE)),
        Triple("Beauty & Health", "✨", Color(0xFFFFE4E6)),
        Triple("Sports", "👟", Color(0xFFCCFBF1)),
        Triple("Toys & Kids", "🧸", Color(0xFFFEF9C3)),
        Triple("Grocery", "🧺", Color(0xFFD1FAE5)),
        Triple("More", "🔲", Color(0xFFFFEDE0))
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
                            Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1F1612))
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
        item { Text("TOP DEALS & OFFERS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF5722)) }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5722))) {
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
        itemsIndexed(deals, key = { index, d -> "deal_${index}_${d.optLong("id", d.optLong("productId"))}" }) { _, deal ->
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
                            Text("₹${deal.optLong("dealPricePaise") / 100}", fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF5722))
                            Text("₹${deal.optLong("originalPricePaise") / 100}", fontSize = 12.sp, color = Color.Gray, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                        }
                        Text("Ends ${deal.optString("endsAt").take(10)}", fontSize = 11.sp, color = Color.Gray)
                    }
                    Text("›", fontSize = 26.sp, color = Color(0xFFFF5722), fontWeight = FontWeight.Bold)
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
    badgeColor: Color = Color(0xFFFFEDE0),
    badgeTextColor: Color = Color(0xFFFF5722),
    textColor: Color = Color(0xFF1F1612),
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