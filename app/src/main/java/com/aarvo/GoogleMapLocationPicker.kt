package com.aarvo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.aarvo.network.AarvoApiClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsApiSettings
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.DragState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale
import kotlin.coroutines.resume

data class GeocodedPlaceResult(
    val formattedAddress: String,
    val line1: String,
    val city: String,
    val state: String,
    val pincode: String,
    val lat: Double,
    val lng: Double
)

private val INDIA_OVERVIEW_CENTER = LatLng(22.5937, 78.9629)
private const val GMP_ATTRIBUTION_ID = "gmp_mcp_codeassist_v1_aistudio"

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

@SuppressLint("MissingPermission")
private suspend fun fetchDeviceLocation(context: Context): Location? = suspendCancellableCoroutine { cont ->
    val fused = LocationServices.getFusedLocationProviderClient(context)
    fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { loc ->
            if (loc != null) {
                if (cont.isActive) cont.resume(loc)
            } else {
                fused.lastLocation
                    .addOnSuccessListener { last -> if (cont.isActive) cont.resume(last) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            }
        }
        .addOnFailureListener {
            fused.lastLocation
                .addOnSuccessListener { last -> if (cont.isActive) cont.resume(last) }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        }
}

private suspend fun resolvePlaceWithBackendOrGeocoder(
    context: Context,
    api: AarvoApiClient?,
    lat: Double? = null,
    lng: Double? = null,
    address: String = ""
): GeocodedPlaceResult? {
    if (api != null && api.isConfigured()) {
        val remote = runCatching { api.geocodeLocation(lat = lat, lng = lng, address = address) }.getOrNull()
        if (remote != null && remote.optBoolean("found", false)) {
            val outLat = remote.optDouble("lat", lat ?: INDIA_OVERVIEW_CENTER.latitude)
            val outLng = remote.optDouble("lng", lng ?: INDIA_OVERVIEW_CENTER.longitude)
            return GeocodedPlaceResult(
                formattedAddress = remote.optString("formattedAddress", "").trim(),
                line1 = remote.optString("line1", "").trim(),
                city = remote.optString("city", "").trim(),
                state = remote.optString("state", "").trim(),
                pincode = remote.optString("postalCode", "").filter(Char::isDigit).take(6),
                lat = outLat,
                lng = outLng
            )
        }
    }

    return withContext(Dispatchers.IO) {
        runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val results = when {
                lat != null && lng != null -> geocoder.getFromLocation(lat, lng, 1)
                address.isNotBlank() -> geocoder.getFromLocationName(address, 1)
                else -> null
            }
            val first = results?.firstOrNull() ?: return@runCatching null
            val formatted = (0..first.maxAddressLineIndex)
                .mapNotNull { idx -> first.getAddressLine(idx)?.trim()?.takeIf { it.isNotBlank() } }
                .joinToString(", ")
            val lineParts = listOfNotNull(
                first.subThoroughfare?.takeIf { it.isNotBlank() },
                first.thoroughfare?.takeIf { it.isNotBlank() },
                first.subLocality?.takeIf { it.isNotBlank() }
            ).distinct()
            val cleanLine1 = lineParts.joinToString(", ").ifBlank {
                formatted.substringBefore(",").trim()
            }
            val cleanPin = first.postalCode.orEmpty().filter(Char::isDigit).take(6)
            GeocodedPlaceResult(
                formattedAddress = formatted.ifBlank { cleanLine1 },
                line1 = cleanLine1,
                city = first.locality ?: first.subAdminArea ?: "",
                state = first.adminArea ?: "",
                pincode = cleanPin,
                lat = first.latitude,
                lng = first.longitude
            )
        }.getOrNull()
    }
}

@Composable
fun InteractiveGoogleMapViewport(
    pickedLatLng: LatLng?,
    pickedSnippet: String,
    isDarkTheme: Boolean,
    hasMapsKey: Boolean,
    gpsBusy: Boolean,
    onRequestGps: () -> Unit,
    onPinMoved: (LatLng) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        runCatching { MapsApiSettings.addInternalUsageAttributionId(context, GMP_ATTRIBUTION_ID) }
    }

    val initialTarget = pickedLatLng ?: INDIA_OVERVIEW_CENTER
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialTarget, if (pickedLatLng != null) 15.5f else 4.2f)
    }
    val markerState = remember { MarkerState(position = initialTarget) }

    LaunchedEffect(pickedLatLng) {
        if (pickedLatLng != null) {
            markerState.position = pickedLatLng
            runCatching {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(pickedLatLng, 15.5f),
                    durationMs = 450
                )
            }
        }
    }

    LaunchedEffect(markerState) {
        snapshotFlow { markerState.dragState }
            .distinctUntilChanged()
            .collect { drag ->
                if (drag == DragState.END) {
                    onPinMoved(markerState.position)
                }
            }
    }

    val borderCol = if (isDarkTheme) Color(0xFF3A2D27) else Color(0xFFFED7AA)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDarkTheme) Color(0xFF1F1815) else Color(0xFFF1F5F9))
    ) {
        if (hasMapsKey) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = false,
                    mapToolbarEnabled = false
                ),
                onMapClick = { tapped ->
                    markerState.position = tapped
                    onPinMoved(tapped)
                }
            ) {
                if (pickedLatLng != null) {
                    Marker(
                        state = markerState,
                        draggable = true,
                        title = "Delivery Pin",
                        snippet = pickedSnippet.ifBlank { "Tap map or drag pin" }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFF5722).copy(alpha = 0.14f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        LocationPinVectorIcon(tint = Color(0xFFFF5722), modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Use Current Location or Search Area",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    if (pickedSnippet.isNotBlank()) pickedSnippet else "Interactive map tiles activate once Google Maps Android API key is set in build secrets.",
                    fontSize = 10.sp,
                    color = if (isDarkTheme) Color(0xFFCBBDB4) else Color(0xFF64748B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Top floating instruction chip
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = (if (isDarkTheme) Color(0xFF181311) else Color.White).copy(alpha = 0.94f),
            border = BorderStroke(1.dp, borderCol),
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text(
                text = if (hasMapsKey) "Tap map or drag pin to set exact delivery spot" else "GPS & Address Lookup Ready",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkTheme) Color(0xFFFAF3EE) else Color(0xFF334155),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Floating Use Current Location (GPS) button inside map viewport
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFFF5722),
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .clickable(enabled = !gpsBusy) { onRequestGps() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (gpsBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.White, strokeWidth = 2.dp)
                }
                Text(
                    text = if (gpsBusy) "Locating…" else "Use Current Location",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun GoogleMapLocationPickerDialog(
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hasMapsKey = remember { BuildConfig.MAPS_API_KEY.isNotBlank() }

    var searchQuery by remember { mutableStateOf("") }
    var searchBusy by remember { mutableStateOf(false) }
    var gpsBusy by remember { mutableStateOf(false) }
    var pickedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var pickedBanner by remember { mutableStateOf("") }

    var quickPincode by remember(activePincode) { mutableStateOf(activePincode) }
    var showAddForm by remember { mutableStateOf(savedLocations.isEmpty()) }
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

    fun applyGeocodedPlace(place: GeocodedPlaceResult, openForm: Boolean = true) {
        pickedLatLng = LatLng(place.lat, place.lng)
        pickedBanner = place.formattedAddress
        if (place.line1.isNotBlank()) line1 = place.line1
        if (place.city.isNotBlank()) city = place.city
        if (place.state.isNotBlank()) state = place.state
        if (place.pincode.matches(Regex("^[1-9][0-9]{5}$"))) {
            pin = place.pincode
            quickPincode = place.pincode
        }
        if (openForm) showAddForm = true
        formError = ""
    }

    fun triggerGpsLookup() {
        scope.launch {
            gpsBusy = true
            formError = ""
            val loc = fetchDeviceLocation(context)
            if (loc == null) {
                gpsBusy = false
                formError = "Unable to detect GPS coordinates. Turn on device location or tap on the map."
                onToast("Turn on GPS or search your area", "WARNING")
                return@launch
            }
            val latLng = LatLng(loc.latitude, loc.longitude)
            pickedLatLng = latLng
            val place = resolvePlaceWithBackendOrGeocoder(context, api, lat = loc.latitude, lng = loc.longitude)
            gpsBusy = false
            if (place != null) {
                applyGeocodedPlace(place, openForm = true)
                onToast("Detected: ${place.city.ifBlank { place.line1 }}", "SUCCESS")
            } else {
                showAddForm = true
                pickedBanner = "Pinned GPS (${String.format(Locale.US, "%.4f, %.4f", loc.latitude, loc.longitude)})"
                onToast("GPS pinned. Enter flat/street & PIN below.", "INFO")
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            triggerGpsLookup()
        } else {
            onToast("Location permission is needed to auto-detect GPS", "WARNING")
        }
    }

    fun runSearch() {
        val q = searchQuery.trim()
        if (q.length < 3) {
            formError = "Enter at least 3 characters to search location"
            return
        }
        scope.launch {
            searchBusy = true
            formError = ""
            val place = resolvePlaceWithBackendOrGeocoder(context, api, address = q)
            searchBusy = false
            if (place != null) {
                applyGeocodedPlace(place, openForm = true)
            } else {
                formError = "Location not found. Try a street name, landmark, city or 6-digit PIN."
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy && !gpsBusy && !searchBusy) onDismiss() },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LocationPinVectorIcon(tint = Color(0xFFFF5722), modifier = Modifier.size(20.dp))
                    Column {
                        Text("Select Delivery Location", fontWeight = FontWeight.Black, fontSize = 15.sp, color = headingColor)
                        Text(
                            "Search area, use GPS, or drop pin on Google Map",
                            fontSize = 10.sp,
                            color = mutedColor
                        )
                    }
                }
                IconButton(onClick = { if (!busy) onDismiss() }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = mutedColor, modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Active delivery summary banner
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
                        LocationPinVectorIcon(
                            tint = if (activePincode.isNotBlank() || activeSummary.isNotBlank()) Color(0xFF059669) else Color(0xFFFF5722),
                            modifier = Modifier.size(16.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                "CURRENT DELIVERY LOCATION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (activePincode.isNotBlank() || activeSummary.isNotBlank()) Color(0xFF059669) else Color(0xFFFF5722)
                            )
                            Text(
                                text = when {
                                    activeSummary.isNotBlank() && activePincode.isNotBlank() -> "$activeSummary - $activePincode"
                                    activeSummary.isNotBlank() -> activeSummary
                                    activePincode.isNotBlank() -> "PIN $activePincode"
                                    else -> "No delivery address selected yet"
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = headingColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Flipkart-style Map Search Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        placeholder = { Text("Search area, street, landmark...", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { runSearch() })
                    )
                    Button(
                        onClick = { runSearch() },
                        enabled = !searchBusy && !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text(if (searchBusy) "..." else "Go", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Interactive Google Map + GPS trigger
                InteractiveGoogleMapViewport(
                    pickedLatLng = pickedLatLng,
                    pickedSnippet = pickedBanner,
                    isDarkTheme = isDarkTheme,
                    hasMapsKey = hasMapsKey,
                    gpsBusy = gpsBusy,
                    onRequestGps = {
                        if (hasLocationPermission(context)) {
                            triggerGpsLookup()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    onPinMoved = { moved ->
                        pickedLatLng = moved
                        scope.launch {
                            val resolved = resolvePlaceWithBackendOrGeocoder(context, api, lat = moved.latitude, lng = moved.longitude)
                            if (resolved != null) {
                                applyGeocodedPlace(resolved, openForm = true)
                            }
                        }
                    }
                )

                if (pickedBanner.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDarkTheme) Color(0xFF10281F) else Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Pinned: $pickedBanner",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF065F46),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }

                // Quick 6-digit Pincode Check & Apply
                Text("Or Enter 6-Digit Delivery Pincode", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = headingColor)
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
                        placeholder = { Text("Enter 6-digit pincode", fontSize = 11.5.sp) }
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
                                    val geocoded = resolvePlaceWithBackendOrGeocoder(context, api, address = "$clean India")
                                    val cityLabel = if (geocoded != null && (geocoded.city.isNotBlank() || geocoded.state.isNotBlank())) {
                                        listOf(geocoded.city, geocoded.state).filter { it.isNotBlank() }.joinToString(", ")
                                    } else {
                                        val (c, s) = inferCityStateForPincode(clean)
                                        "$c, $s"
                                    }
                                    val live = runCatching { api.checkPincodeServiceability(clean) }.getOrNull()
                                    val riderMsg = live?.optString("riderMessage")?.takeIf { it.isNotBlank() && it != "null" }
                                    onSelectActive(cityLabel, clean)
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
                        if (showAddForm) "✕ Hide Form" else "+ Add Full Address",
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
                            OutlinedTextField(line1, { line1 = it }, Modifier.fillMaxWidth(), label = { Text("House / Building / Street / Area *") })
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
                    val isSelected = (loc.pincode.isNotBlank() && loc.pincode == activePincode && activeSummary.contains(loc.city, ignoreCase = true)) || loc.isDefault
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) cardBg else if (isDarkTheme) Color(0xFF181311) else Color.White,
                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Color(0xFFFF5722) else cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !busy) {
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
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
                                        val nextDef = normalized.firstOrNull { it.isDefault }
                                        if (nextDef != null) {
                                            onSelectActive(nextDef.shortSummary(), nextDef.pincode)
                                        } else {
                                            onSelectActive("", "")
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Close") }
        }
    )
}

@Composable
fun GoogleMapCoordinatePickerDialog(
    api: AarvoApiClient,
    isDarkTheme: Boolean = false,
    onPicked: (GeocodedPlaceResult) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hasMapsKey = remember { BuildConfig.MAPS_API_KEY.isNotBlank() }

    var searchQuery by remember { mutableStateOf("") }
    var pickedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var resolvedPlace by remember { mutableStateOf<GeocodedPlaceResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf("") }

    fun handleResolved(res: GeocodedPlaceResult?) {
        busy = false
        if (res != null) {
            resolvedPlace = res
            pickedLatLng = LatLng(res.lat, res.lng)
            statusMsg = res.formattedAddress
        } else {
            statusMsg = "Could not resolve address. Try another search or move pin."
        }
    }

    fun triggerGps() {
        scope.launch {
            busy = true
            val loc = fetchDeviceLocation(context)
            if (loc == null) {
                busy = false
                statusMsg = "GPS unavailable. Enable location services or search area."
                return@launch
            }
            val res = resolvePlaceWithBackendOrGeocoder(context, api, lat = loc.latitude, lng = loc.longitude)
            handleResolved(res)
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) triggerGps() else statusMsg = "Location permission denied."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick Location on Google Map", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Search area or city...", fontSize = 11.sp) }
                    )
                    OutlinedButton(
                        onClick = {
                            if (searchQuery.trim().length >= 3) {
                                scope.launch {
                                    busy = true
                                    val res = resolvePlaceWithBackendOrGeocoder(context, api, address = searchQuery.trim())
                                    handleResolved(res)
                                }
                            }
                        },
                        enabled = !busy
                    ) { Text("Find") }
                }

                InteractiveGoogleMapViewport(
                    pickedLatLng = pickedLatLng,
                    pickedSnippet = statusMsg,
                    isDarkTheme = isDarkTheme,
                    hasMapsKey = hasMapsKey,
                    gpsBusy = busy,
                    onRequestGps = {
                        if (hasLocationPermission(context)) {
                            triggerGps()
                        } else {
                            permLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    onPinMoved = { latLng ->
                        pickedLatLng = latLng
                        scope.launch {
                            busy = true
                            val res = resolvePlaceWithBackendOrGeocoder(context, api, lat = latLng.latitude, lng = latLng.longitude)
                            handleResolved(res)
                        }
                    }
                )

                if (statusMsg.isNotBlank()) {
                    Text(statusMsg, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    resolvedPlace?.let {
                        onPicked(it)
                        onDismiss()
                    }
                },
                enabled = resolvedPlace != null && !busy
            ) {
                Text("Use This Address")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
