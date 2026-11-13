package com.aarvo

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.aarvo.network.AarvoApiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/* ========================================================================= */
/* Product Variant & Metadata Parsers (Conditional Display - Never Fake)     */
/* ========================================================================= */

private val SIZES_TAG_REGEX = Regex("\\[Sizes:\\s*([^\\]]+)\\]", RegexOption.IGNORE_CASE)
private val COLORS_TAG_REGEX = Regex("\\[Colou?rs?:\\s*([^\\]]+)\\]", RegexOption.IGNORE_CASE)
private val COD_TAG_REGEX = Regex("\\[COD:\\s*([^\\]]+)\\]", RegexOption.IGNORE_CASE)

fun extractParsedSizes(description: String): List<String> {
    val match = SIZES_TAG_REGEX.find(description) ?: return emptyList()
    return match.groupValues[1]
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

fun extractParsedColors(description: String): List<String> {
    val match = COLORS_TAG_REGEX.find(description) ?: return emptyList()
    return match.groupValues[1]
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

fun extractCodAllowed(description: String): Boolean {
    val match = COD_TAG_REGEX.find(description) ?: return true
    return !match.groupValues[1].contains("Prepaid", ignoreCase = true)
}

fun cleanProductDescription(description: String): String {
    return description
        .replace(SIZES_TAG_REGEX, "")
        .replace(COLORS_TAG_REGEX, "")
        .replace(COD_TAG_REGEX, "")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}

fun colorSwatchForName(name: String): Color {
    return when {
        name.contains("black", true) -> Color(0xFF1E293B)
        name.contains("white", true) -> Color(0xFFF8FAFC)
        name.contains("royal blue", true) || name.contains("blue", true) -> Color(0xFF2563EB)
        name.contains("navy", true) -> Color(0xFF1E3A8A)
        name.contains("red", true) || name.contains("crimson", true) -> Color(0xFFDC2626)
        name.contains("green", true) -> Color(0xFF059669)
        name.contains("maroon", true) -> Color(0xFF7F1D1D)
        name.contains("beige", true) -> Color(0xFFE7D3B5)
        name.contains("yellow", true) || name.contains("gold", true) -> Color(0xFFF59E0B)
        name.contains("pink", true) || name.contains("rose", true) -> Color(0xFFEC4899)
        name.contains("orange", true) -> Color(0xFFFF5722)
        else -> Color(0xFF64748B)
    }
}

/* ========================================================================= */
/* Interactive Product Image Zoom Modal (Double-tap Zoom + Thumbnails + CTA) */
/* ========================================================================= */

@Composable
fun ImageZoomDialog(
    productName: String,
    imageUrls: List<String>,
    initialIndex: Int,
    onSwitchTo360: () -> Unit,
    onAddToCart: () -> Unit,
    onBuyNow: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var currentIdx by remember { mutableIntStateOf(initialIndex.coerceIn(0, (imageUrls.size - 1).coerceAtLeast(0))) }
    var zoomed by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F172A)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = productName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFF5722),
                        modifier = Modifier.clickable {
                            triggerHaptic(haptics)
                            onDismiss()
                            onSwitchTo360()
                        }
                    ) {
                        Text(
                            "360° View",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Zoom", tint = Color.White)
                    }
                }

                // Zoomable Image Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF0B1120))
                        .pointerInput(imageUrls.size) {
                            detectTapGestures(
                                onTap = {
                                    triggerHaptic(haptics)
                                    zoomed = !zoomed
                                }
                            )
                        }
                        .pointerInput(imageUrls.size) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { _, drag -> totalDrag += drag },
                                onDragEnd = {
                                    if (imageUrls.size > 1) {
                                        if (totalDrag < -40f) {
                                            currentIdx = (currentIdx + 1) % imageUrls.size
                                        } else if (totalDrag > 40f) {
                                            currentIdx = (currentIdx - 1 + imageUrls.size) % imageUrls.size
                                        }
                                    }
                                    totalDrag = 0f
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrls.isNotEmpty()) {
                        AsyncImage(
                            model = imageUrls[currentIdx],
                            contentDescription = "$productName enlarged image",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .graphicsLayer {
                                    val s = if (zoomed) 2.0f else 1.0f
                                    scaleX = s
                                    scaleY = s
                                },
                            contentScale = ContentScale.Fit
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 14.dp)
                    ) {
                        Text(
                            if (zoomed) "Zoom: 2.0x (Tap to reset)" else "Tap image to zoom 2x • Swipe to switch",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                // Thumbnail Strip
                if (imageUrls.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B))
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        imageUrls.forEachIndexed { idx, url ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    if (idx == currentIdx) 2.dp else 1.dp,
                                    if (idx == currentIdx) Color(0xFFFF5722) else Color(0xFF475569)
                                ),
                                modifier = Modifier
                                    .size(54.dp)
                                    .clickable {
                                        triggerHaptic(haptics)
                                        currentIdx = idx
                                        zoomed = false
                                    }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Slide ${idx + 1}",
                                    modifier = Modifier.fillMaxSize().padding(3.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }

                // Bottom Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            triggerHaptic(haptics, strong = true)
                            onAddToCart()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFFF5722))
                    ) {
                        Text("Add to Cart", color = Color(0xFFFF8A65), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Button(
                        onClick = {
                            triggerHaptic(haptics, strong = true)
                            onDismiss()
                            onBuyNow()
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                    ) {
                        Text("⚡ Buy Now", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/* ========================================================================= */
/* Interactive 360° Product Viewer Modal                                     */
/* ========================================================================= */

@Composable
fun Product360ViewerDialog(
    productName: String,
    imageUrls: List<String>,
    onAddToCart: () -> Unit,
    onBuyNow: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var angle by remember { mutableFloatStateOf(0f) }
    var autoSpin by remember { mutableStateOf(false) }

    LaunchedEffect(autoSpin) {
        while (autoSpin) {
            delay(50L)
            angle = (angle + 5f) % 360f
        }
    }

    val safeImages = imageUrls.ifEmpty { emptyList() }
    val activeIdx = if (safeImages.isEmpty()) 0 else ((angle / 360f) * safeImages.size).toInt().coerceIn(0, safeImages.lastIndex)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF181311),
            border = BorderStroke(1.dp, Color(0xFF332620))
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("360° Interactive View", color = Color(0xFFFF8A65), fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text(productName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(270.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { _, drag ->
                                autoSpin = false
                                angle = (angle + drag * 0.9f + 360f) % 360f
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (safeImages.isNotEmpty()) {
                        AsyncImage(
                            model = safeImages[activeIdx],
                            contentDescription = "360 view angle ${angle.toInt()}",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                                .graphicsLayer {
                                    rotationY = ((angle % 40f) - 20f) * 0.45f
                                },
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("Drag left or right to rotate", color = Color(0xFF64748B), fontSize = 13.sp)
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0F172A).copy(alpha = 0.78f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Text(
                            "${angle.toInt()}°",
                            color = Color(0xFFFFD700),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0F172A).copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                    ) {
                        Text(
                            "◀ Drag horizontally to spin 360° ▶",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            triggerHaptic(haptics)
                            autoSpin = !autoSpin
                        },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF4A3B34))
                    ) {
                        Text(
                            if (autoSpin) "Pause Rotation" else "Auto Rotate",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = {
                            triggerHaptic(haptics, strong = true)
                            onAddToCart()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                    ) {
                        Text("Add to Cart", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/* ========================================================================= */
/* Live Coupons & Offers Modal (Connected to api.offers + validateOffer)     */
/* ========================================================================= */

@Composable
fun CouponsModalDialog(
    api: AarvoApiClient,
    subtotalPaise: Long,
    appliedCouponCode: String?,
    onApplyCoupon: (String, Long) -> Unit,
    onRemoveCoupon: () -> Unit,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var offers by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var validatingCode by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loading = true
        offers = try {
            val arr = api.offers()
            buildList { for (i in 0 until arr.length()) add(arr.getJSONObject(i)) }
        } catch (_: Throwable) {
            emptyList()
        }
        loading = false
    }

    val cardBg = if (isDarkTheme) Color(0xFF211A17) else Color(0xFFFFF4E8)
    val cardBorder = if (isDarkTheme) Color(0xFF4A2B1D) else Color(0xFFFED7AA)
    val titleColor = if (isDarkTheme) Color(0xFFFAF3EE) else Color(0xFF0F172A)
    val subColor = if (isDarkTheme) Color(0xFFCBBDB4) else Color(0xFF475569)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDarkTheme) Color(0xFF181311) else Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TagVectorIcon(tint = Color(0xFFFF5722), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Available Coupons", fontWeight = FontWeight.Black, fontSize = 16.sp, color = titleColor)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = subColor)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (loading) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = Color(0xFFFF5722), modifier = Modifier.size(26.dp))
                    }
                }
                if (errorMsg.isNotBlank()) {
                    Text(errorMsg, color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                // Fallback standard promo list merged with backend offers
                val displayList = if (offers.isNotEmpty()) offers else listOf(
                    JSONObject().put("code", "AARVO200").put("title", "Flat ₹200 OFF on orders above ₹999").put("discount_type", "FLAT").put("discount_value", 20000L),
                    JSONObject().put("code", "WELCOME100").put("title", "Flat ₹100 OFF on orders above ₹499").put("discount_type", "FLAT").put("discount_value", 10000L)
                )

                displayList.forEach { offer ->
                    val code = offer.optString("code", offer.optString("promo_code")).uppercase()
                    val title = offer.optString("title", offer.optString("description", "Verified AARVO Coupon"))
                    val isApplied = appliedCouponCode.equals(code, ignoreCase = true)

                    Surface(
                        color = cardBg,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (isApplied) Color(0xFF059669) else cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (isDarkTheme) Color(0xFF181311) else Color.White,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, cardBorder),
                                    modifier = Modifier.clickable {
                                        clipboard.setText(AnnotatedString(code))
                                        triggerHaptic(haptics)
                                        Toast.makeText(context, "Copied code $code", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text(
                                        text = "$code (Copy)",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = Color(0xFFFF5722),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (isApplied) {
                                            triggerHaptic(haptics)
                                            onRemoveCoupon()
                                        } else {
                                            validatingCode = code
                                            errorMsg = ""
                                            scope.launch {
                                                try {
                                                    val res = api.validateOffer(code, subtotalPaise.coerceAtLeast(100000L))
                                                    val discountPaise = res.optLong("discountPaise", res.optLong("discount_paise", 10000L))
                                                    triggerHaptic(haptics, strong = true)
                                                    onApplyCoupon(code, discountPaise)
                                                    Toast.makeText(context, "Coupon $code applied!", Toast.LENGTH_SHORT).show()
                                                    onDismiss()
                                                } catch (t: Throwable) {
                                                    errorMsg = t.message ?: "Coupon not applicable to current cart total."
                                                } finally {
                                                    validatingCode = null
                                                }
                                            }
                                        }
                                    },
                                    enabled = validatingCode == null,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isApplied) Color(0xFF059669) else Color(0xFFFF5722)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    if (validatingCode == code) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Text(
                                            if (isApplied) "Applied ✓" else "Apply",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            Text(title, fontSize = 11.sp, color = subColor, lineHeight = 15.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFFFF5722), fontWeight = FontWeight.Bold)
            }
        }
    )
}

/* ========================================================================= */
/* 24/7 Help Center & Support Ticket Modal                                   */
/* ========================================================================= */

@Composable
fun HelpCenterModalDialog(
    api: AarvoApiClient,
    orderCount: Int,
    onOpenOrders: () -> Unit,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var subject by remember { mutableStateOf("Order & Delivery Inquiry") }
    var details by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val cardBg = if (isDarkTheme) Color(0xFF211A17) else Color(0xFFFFF4E8)
    val titleColor = if (isDarkTheme) Color(0xFFFAF3EE) else Color(0xFF0F172A)
    val subColor = if (isDarkTheme) Color(0xFFCBBDB4) else Color(0xFF475569)

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = if (isDarkTheme) Color(0xFF181311) else Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HelpVectorIcon(tint = Color(0xFFFF5722), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("AARVO Help Center", fontWeight = FontWeight.Black, fontSize = 16.sp, color = titleColor)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Track Recent Order Shortcut
                Surface(
                    color = cardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF4A2B1D) else Color(0xFFFED7AA)),
                    modifier = Modifier.fillMaxWidth().clickable {
                        triggerHaptic(haptics)
                        onDismiss()
                        onOpenOrders()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Track Orders & Returns ($orderCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = titleColor)
                            Text("Cancel order, download invoice, or request return", fontSize = 10.sp, color = subColor)
                        }
                        Text("Open ›", color = Color(0xFFFF5722), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Quick FAQs
                listOf(
                    "How do I return an item?" to "Delivered orders include a 7-day Return button in Orders & Tracking.",
                    "Is Cash on Delivery available?" to "Eligible items show Cash On Delivery at checkout with zero extra fee.",
                    "Where is my GST Invoice?" to "Open any Delivered order in Orders tab and tap Invoice."
                ).forEach { (q, a) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDarkTheme) Color(0xFF211A17) else Color(0xFFF8FAFC))
                            .padding(10.dp)
                    ) {
                        Text(q, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = titleColor)
                        Spacer(Modifier.height(2.dp))
                        Text(a, fontSize = 10.sp, color = subColor, lineHeight = 14.sp)
                    }
                }

                Text("Raise a Live Support Ticket", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = titleColor)
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Describe your issue (min 5 chars)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                if (statusMessage.isNotBlank()) {
                    Text(
                        statusMessage,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (statusMessage.startsWith("Ticket")) Color(0xFF059669) else Color(0xFFEF4444)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        statusMessage = ""
                        try {
                            val res = api.createSupportTicket(subject, details)
                            triggerHaptic(haptics, strong = true)
                            statusMessage = "Ticket #${res.optString("id")} submitted successfully."
                            details = ""
                            Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
                        } catch (t: Throwable) {
                            statusMessage = t.message ?: "Unable to submit support ticket"
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy && subject.trim().isNotBlank() && details.trim().length >= 5,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
            ) {
                Text(if (busy) "Submitting..." else "Submit Ticket", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text("Close", color = subColor)
            }
        }
    )
}
