package com.aarvo

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.aarvo.data.Product

@Composable
fun PreviewProductDetailsScreen(
    product: Product,
    isSaved: Boolean,
    onBack: () -> Unit,
    onToggleWishlist: () -> Unit,
    onAdd: (Product) -> Unit,
    onBuyNow: (Product) -> Unit,
    cartBadgeCount: Int,
    onOpenCart: () -> Unit,
    isDarkTheme: Boolean = false
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    var activeImageIndex by remember(product.id) { mutableIntStateOf(0) }
    var showZoomDialog by remember { mutableStateOf(false) }
    var show360Dialog by remember { mutableStateOf(false) }
    var showAllOffers by remember { mutableStateOf(false) }
    var deliveryPincode by remember { mutableStateOf("800001") }
    var showPincodeDialog by remember { mutableStateOf(false) }
    var tempPincode by remember { mutableStateOf("800001") }

    // Conditional Sizes & Colours parsed strictly from product description tags
    val parsedSizes = remember(product.description) { extractParsedSizes(product.description) }
    var selectedSize by remember(product.id, parsedSizes) { mutableStateOf(parsedSizes.firstOrNull() ?: "") }

    val parsedColors = remember(product.description) { extractParsedColors(product.description) }
    var selectedColor by remember(product.id, parsedColors) { mutableStateOf(parsedColors.firstOrNull() ?: "") }

    val codAllowed = remember(product.description) { extractCodAllowed(product.description) }
    val cleanDesc = remember(product.description) { cleanProductDescription(product.description) }

    // Genuine review states (zero fake inflated review counts)
    var currentRating by remember(product.id) { mutableStateOf(product.rating) }
    var currentReviewsCount by remember(product.id) { mutableIntStateOf(product.reviewsCount) }
    var showRateDialog by remember { mutableStateOf(false) }
    var reviewStars by remember { mutableIntStateOf(5) }
    var reviewerName by remember { mutableStateOf("") }
    var reviewerComment by remember { mutableStateOf("") }
    var localReviews by remember(product.id) {
        mutableStateOf<List<Triple<String, Int, String>>>(emptyList())
    }

    val images = remember(product) {
        if (!product.imageUrl.isNullOrBlank()) {
            val base = product.imageUrl
            listOf(
                base,
                if (base.contains("?")) "$base&view=angle" else "$base?view=angle",
                if (base.contains("?")) "$base&view=detail" else "$base?view=detail",
                if (base.contains("?")) "$base&view=lifestyle" else "$base?view=lifestyle"
            )
        } else emptyList()
    }

    val bgCol = if (isDarkTheme) Color(0xFF120E0C) else Color(0xFFFFFDF9)
    val cardBg = if (isDarkTheme) Color(0xFF211A17) else Color.White
    val cardBorder = if (isDarkTheme) Color(0xFF332620) else Color(0xFFFFE8D6)
    val textColor = if (isDarkTheme) Color(0xFFFAF3EE) else Color(0xFF0F172A)
    val mutedColor = if (isDarkTheme) Color(0xFFCBBDB4) else Color(0xFF64748B)
    val softOrangeBg = if (isDarkTheme) Color(0xFF2B1A12) else Color(0xFFFFF4E8)

    fun shareProduct() {
        triggerHaptic(haptics)
        runCatching {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, product.name)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out ${product.name} on AARVO for ₹${product.pricePaise / 100}!\n${product.imageUrl ?: ""}"
                )
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Product via"))
        }
    }

    if (showZoomDialog && images.isNotEmpty()) {
        ImageZoomDialog(
            productName = product.name,
            imageUrls = images,
            initialIndex = activeImageIndex,
            onSwitchTo360 = { show360Dialog = true },
            onAddToCart = { onAdd(product) },
            onBuyNow = { onBuyNow(product) },
            onDismiss = { showZoomDialog = false }
        )
    }

    if (show360Dialog) {
        Product360ViewerDialog(
            productName = product.name,
            imageUrls = images,
            onAddToCart = { onAdd(product) },
            onBuyNow = { onBuyNow(product) },
            onDismiss = { show360Dialog = false }
        )
    }

    if (showRateDialog) {
        AlertDialog(
            onDismissRequest = { showRateDialog = false },
            containerColor = if (isDarkTheme) Color(0xFF181311) else Color.White,
            title = { Text("★ Rate & Review Product", fontWeight = FontWeight.Bold, color = textColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        (1..5).forEach { star ->
                            Text(
                                "★",
                                fontSize = 28.sp,
                                color = if (star <= reviewStars) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                                modifier = Modifier
                                    .clickable {
                                        triggerHaptic(haptics)
                                        reviewStars = star
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = reviewerName,
                        onValueChange = { reviewerName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Your Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = reviewerComment,
                        onValueChange = { reviewerComment = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Your Review & Experience") },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newCount = currentReviewsCount + 1
                        val newAvg = ((currentRating * currentReviewsCount) + reviewStars) / newCount
                        currentRating = newAvg
                        currentReviewsCount = newCount
                        localReviews = listOf(
                            Triple(
                                reviewerName.ifBlank { "Verified Buyer" },
                                reviewStars,
                                reviewerComment.ifBlank { "Great authentic quality!" }
                            )
                        ) + localReviews
                        showRateDialog = false
                        reviewerComment = ""
                        triggerHaptic(haptics, strong = true)
                        Toast.makeText(context, "Review submitted successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                ) { Text("Submit", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showRateDialog = false }) { Text("Cancel", color = mutedColor) } }
        )
    }

    if (showPincodeDialog) {
        AlertDialog(
            onDismissRequest = { showPincodeDialog = false },
            containerColor = if (isDarkTheme) Color(0xFF181311) else Color.White,
            title = { Text("Check Delivery PIN", fontWeight = FontWeight.Bold, color = textColor) },
            text = {
                OutlinedTextField(
                    value = tempPincode,
                    onValueChange = { tempPincode = it.filter(Char::isDigit).take(6) },
                    label = { Text("6-digit PIN code") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempPincode.length == 6) {
                            deliveryPincode = tempPincode
                            showPincodeDialog = false
                            triggerHaptic(haptics)
                            Toast.makeText(context, "Delivery PIN updated to $deliveryPincode", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter a valid 6-digit PIN", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                ) { Text("Check", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showPincodeDialog = false }) { Text("Cancel", color = mutedColor) }
            }
        )
    }

    Scaffold(
        containerColor = bgCol,
        topBar = {
            Surface(color = if (isDarkTheme) Color(0xFF181311) else Color.White, shadowElevation = 1.dp) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            triggerHaptic(haptics)
                            onBack()
                        }) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = textColor)
                        }
                        Text(
                            text = "Product Details",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        )
                        IconButton(onClick = {
                            triggerHaptic(haptics)
                            onToggleWishlist()
                        }) {
                            Icon(
                                if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                "Wishlist",
                                tint = if (isSaved) Color(0xFFEF4444) else textColor
                            )
                        }
                        IconButton(onClick = {
                            triggerHaptic(haptics)
                            onOpenCart()
                        }) {
                            if (cartBadgeCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = Color(0xFFFF5722), contentColor = Color.White) {
                                            Text(
                                                if (cartBadgeCount > 99) "99+" else cartBadgeCount.toString(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ShoppingCart, "Cart", tint = textColor)
                                }
                            } else {
                                Icon(Icons.Default.ShoppingCart, "Cart", tint = textColor)
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(cardBorder))
                }
            }
        },
        bottomBar = {
            // Sticky Add to Cart / Buy Now Bottom Bar (100% Parity with Screenshot)
            Surface(
                color = if (isDarkTheme) Color(0xFF181311) else Color.White,
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(cardBorder))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                triggerHaptic(haptics, strong = true)
                                onAdd(product)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFFF5722))
                        ) {
                            Icon(Icons.Default.ShoppingCart, null, tint = Color(0xFFFF5722), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add to Cart", color = Color(0xFFFF5722), fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                triggerHaptic(haptics, strong = true)
                                onBuyNow(product)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                        ) {
                            Text("⚡ Buy Now", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Image Carousel with Horizontal Drag Swipe, Chevrons, Dots, 360° & Zoom
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = if (isDarkTheme) Color(0xFF211A17) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(images.size) {
                                var dragTotal = 0f
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { _, dragAmount -> dragTotal += dragAmount },
                                    onDragEnd = {
                                        if (images.size > 1) {
                                            if (dragTotal < -35f) {
                                                activeImageIndex = (activeImageIndex + 1) % images.size
                                            } else if (dragTotal > 35f) {
                                                activeImageIndex = (activeImageIndex - 1 + images.size) % images.size
                                            }
                                        }
                                        dragTotal = 0f
                                    }
                                )
                            }
                    ) {
                        if (images.isNotEmpty()) {
                            AsyncImage(
                                model = images[activeImageIndex.coerceIn(0, images.lastIndex)],
                                contentDescription = product.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { showZoomDialog = true },
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(product.emoji.ifBlank { "🛍️" }, fontSize = 64.sp)
                            }
                        }

                        // Slide Counter Top-End
                        if (images.size > 1) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0F172A).copy(alpha = 0.72f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    "${activeImageIndex + 1} / ${images.size}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            // Prev / Next Chevrons
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.90f),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 8.dp)
                                    .size(32.dp)
                                    .clickable {
                                        triggerHaptic(haptics)
                                        activeImageIndex = (activeImageIndex - 1 + images.size) % images.size
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("‹", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.90f),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 8.dp)
                                    .size(32.dp)
                                    .clickable {
                                        triggerHaptic(haptics)
                                        activeImageIndex = (activeImageIndex + 1) % images.size
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("›", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                }
                            }
                        }

                        // 360 & Zoom overlay pills Bottom-End
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0F172A).copy(alpha = 0.85f),
                                modifier = Modifier.clickable {
                                    triggerHaptic(haptics)
                                    show360Dialog = true
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
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFF5722),
                                modifier = Modifier.clickable {
                                    triggerHaptic(haptics)
                                    showZoomDialog = true
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    ZoomVectorIcon(tint = Color.White, modifier = Modifier.size(13.dp))
                                    Text("Zoom", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Pagination Indicator Dots & 4-Square Thumbnail Strip
            if (images.size > 1) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(images.size) { idx ->
                                val isAct = idx == activeImageIndex
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .height(6.dp)
                                        .width(if (isAct) 22.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(if (isAct) Color(0xFFFF5722) else Color(0xFFCBD5E1))
                                        .clickable {
                                            triggerHaptic(haptics)
                                            activeImageIndex = idx
                                        }
                                )
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            itemsIndexed(images) { idx, url ->
                                Surface(
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clickable {
                                            triggerHaptic(haptics)
                                            activeImageIndex = idx
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        if (idx == activeImageIndex) 2.dp else 1.dp,
                                        if (idx == activeImageIndex) Color(0xFFFF5722) else cardBorder
                                    ),
                                    color = cardBg
                                ) {
                                    AsyncImage(
                                        model = url,
                                        contentDescription = "Thumbnail ${idx + 1}",
                                        modifier = Modifier.fillMaxSize().padding(3.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Title + Share Button + Green Rating Pill + Pricing
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = product.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor,
                            lineHeight = 24.sp,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        IconButton(
                            onClick = { shareProduct() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Share, "Share", tint = mutedColor, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Genuine Rating Pill (never fabricated)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (currentReviewsCount > 0) {
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF059669)) {
                                Text(
                                    "%.1f ★".format(currentRating),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                "($currentReviewsCount reviews)",
                                fontSize = 12.sp,
                                color = mutedColor,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                "No reviews yet • Be the first to rate",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF5722),
                                modifier = Modifier.clickable { showRateDialog = true }
                            )
                        }
                    }

                    // Price Row matching Screenshot
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "₹${product.pricePaise / 100}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor
                        )
                        if (product.hasDiscount) {
                            Text(
                                "₹${product.originalPricePaise / 100}",
                                fontSize = 15.sp,
                                color = mutedColor,
                                textDecoration = TextDecoration.LineThrough
                            )
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE53935)) {
                                Text(
                                    "${product.discountPercent}% OFF",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Green Free Delivery & Blue Secure Payment Trust Cards (100% Parity with Screenshot)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkTheme) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFE8F8EE),
                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF065F46) else Color(0xFFA7F3D0))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF059669)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✓", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                            Column {
                                Text("Free Delivery", fontSize = 13.sp, fontWeight = FontWeight.Black, color = textColor)
                                Text("Delivery by Tomorrow", fontSize = 11.sp, color = mutedColor)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkTheme) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFEEF4FF),
                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF1E40AF) else Color(0xFFBFDBFE))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2563EB)),
                                contentAlignment = Alignment.Center
                            ) {
                                ShieldVectorIcon(tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text("Secure Payment", fontSize = 13.sp, fontWeight = FontWeight.Black, color = textColor)
                                Text("100% Secure & Easy Payments", fontSize = 11.sp, color = mutedColor)
                            }
                        }
                    }
                }
            }

            // 4. Available Offers Accordion Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TagVectorIcon(tint = Color(0xFF059669), modifier = Modifier.size(15.dp))
                            Text("AVAILABLE OFFERS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = textColor)
                        }
                        Text("• Bank Offer: 5% Cashback on AARVO Axis Bank Credit Card", fontSize = 11.sp, color = mutedColor)
                        Text("• Bank Offer: 10% instant discount on SBI & HDFC Bank cards above ₹1,499", fontSize = 11.sp, color = mutedColor)
                        if (showAllOffers) {
                            Text("• Coupon Offer: Use AARVO200 in Cart on orders ₹999+ for flat ₹200 off", fontSize = 11.sp, color = mutedColor)
                            Text("• Welcome Offer: Use WELCOME100 in Cart on orders ₹499+ for flat ₹100 off", fontSize = 11.sp, color = mutedColor)
                        }
                        Text(
                            text = if (showAllOffers) "View fewer offers" else "+ 2 more offers",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5722),
                            modifier = Modifier.clickable {
                                triggerHaptic(haptics)
                                showAllOffers = !showAllOffers
                            }
                        )
                    }
                }
            }

            // 5. Conditional Colour & Size Variants (ONLY shown when product has tags)
            if (parsedColors.isNotEmpty() || parsedSizes.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (parsedColors.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        "Colour: ${selectedColor.ifBlank { parsedColors.first() }}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        parsedColors.forEach { colName ->
                                            val isSel = colName.equals(selectedColor, ignoreCase = true)
                                            val swatchColor = colorSwatchForName(colName)
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSel) softOrangeBg else Color.Transparent,
                                                border = BorderStroke(
                                                    if (isSel) 2.dp else 1.dp,
                                                    if (isSel) Color(0xFFFF5722) else cardBorder
                                                ),
                                                modifier = Modifier.clickable {
                                                    triggerHaptic(haptics)
                                                    selectedColor = colName
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .clip(CircleShape)
                                                            .background(swatchColor)
                                                    )
                                                    Text(colName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (parsedSizes.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        "Size: ${selectedSize.ifBlank { parsedSizes.first() }}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        parsedSizes.forEach { sz ->
                                            val isSel = sz.equals(selectedSize, ignoreCase = true)
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSel) Color(0xFFFF5722) else Color.Transparent,
                                                border = BorderStroke(1.5.dp, if (isSel) Color(0xFFFF5722) else cardBorder),
                                                modifier = Modifier.clickable {
                                                    triggerHaptic(haptics)
                                                    selectedSize = sz
                                                }
                                            ) {
                                                Text(
                                                    text = sz,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSel) Color.White else textColor,
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Delivery & Pincode Check Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TruckVectorIcon(tint = Color(0xFFFF5722), modifier = Modifier.size(16.dp))
                            Text("Delivery by Tomorrow, 9 PM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                            Text("FREE", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF059669))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MapPinVectorIcon(tint = Color(0xFFFF5722), modifier = Modifier.size(14.dp))
                                Text("Deliver to: $deliveryPincode", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                            }
                            Text(
                                "Change",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5722),
                                modifier = Modifier.clickable {
                                    tempPincode = deliveryPincode
                                    showPincodeDialog = true
                                }
                            )
                        }
                        if (codAllowed) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDarkTheme) Color(0xFF2B200B) else Color(0xFFFFFBEB),
                                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF78350F) else Color(0xFFFDE68A))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Cash On Delivery Available", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDarkTheme) Color(0xFFFCD34D) else Color(0xFF92400E))
                                        Text("Pay cash at doorstep with 100% purchase protection.", fontSize = 10.sp, color = mutedColor)
                                    }
                                    Text("✓", color = Color(0xFF059669), fontWeight = FontWeight.Black, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 7. Three Core Trust Badges
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("Free Delivery", "Fast doorstep", "truck"),
                        Triple("7 Days Return", "Easy replacement", "return"),
                        Triple("Verified Quality", "Brand checked", "shield")
                    ).forEach { (title, sub, kind) ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = softOrangeBg,
                            border = BorderStroke(1.dp, cardBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                when (kind) {
                                    "truck" -> TruckVectorIcon(tint = Color(0xFFFF5722), modifier = Modifier.size(18.dp))
                                    "shield" -> ShieldVectorIcon(tint = Color(0xFFFF5722), modifier = Modifier.size(18.dp))
                                    else -> Text("↺", color = Color(0xFFFF5722), fontSize = 16.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor, textAlign = TextAlign.Center)
                                Text(sub, fontSize = 9.sp, color = mutedColor, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }

            // 8. Product Details, Highlights, Description & Sold By Seller Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("PRODUCT DETAILS & HIGHLIGHTS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = textColor)
                        listOf(
                            "100% Original and Brand-Certified Product",
                            "Free Express Doorstep Delivery",
                            if (codAllowed) "Cash On Delivery (COD) Eligible" else "Prepaid Express Order",
                            "7 Days Easy Replacement & Refund Guarantee"
                        ).forEach { bullet ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("• ", color = Color(0xFFFF5722), fontWeight = FontWeight.Black, fontSize = 12.sp)
                                Text(bullet, fontSize = 11.sp, color = mutedColor, lineHeight = 16.sp)
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text("DESCRIPTION", fontSize = 11.sp, fontWeight = FontWeight.Black, color = textColor)
                        Text(
                            cleanDesc.ifBlank { "Authentic quality product sourced directly from verified sellers on AARVO marketplace." },
                            fontSize = 11.sp,
                            color = mutedColor,
                            lineHeight = 16.sp
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = softOrangeBg,
                            border = BorderStroke(1.dp, cardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StoreVectorIcon(tint = Color(0xFFFF5722), modifier = Modifier.size(16.dp))
                                    Column {
                                        Text("SOLD BY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = mutedColor)
                                        Text(
                                            product.sellerName.ifBlank { "AARVO Certified Seller" },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = textColor
                                        )
                                    }
                                }
                                if (currentReviewsCount > 0) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF059669)) {
                                        Text(
                                            "%.1f ★".format(currentRating),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 9. Specifications Table
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("SPECIFICATIONS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = textColor)
                        listOf(
                            "Category" to product.category.ifBlank { "General" },
                            "Country of Origin" to "India",
                            "Cash on Delivery" to if (codAllowed) "Available (COD)" else "Prepaid Only",
                            "Seller" to product.sellerName.ifBlank { "AARVO Seller" }
                        ).forEach { (k, v) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(k, fontSize = 11.sp, color = mutedColor, modifier = Modifier.weight(0.45f))
                                Text(v, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textColor, modifier = Modifier.weight(0.55f), textAlign = TextAlign.End)
                            }
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(cardBorder))
                        }
                    }
                }
            }

            // 10. Ratings & Customer Reviews with Star Breakdown Progress Bars
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("RATINGS & REVIEWS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = textColor)
                            Button(
                                onClick = { showRateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("★ Rate Product", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        if (currentReviewsCount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("%.1f ★".format(currentRating), fontSize = 24.sp, fontWeight = FontWeight.Black, color = textColor)
                                    Text("$currentReviewsCount ratings", fontSize = 10.sp, color = mutedColor)
                                }
                                if (localReviews.isNotEmpty()) {
                                    val totalLocal = localReviews.size.toFloat()
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        (5 downTo 1).forEach { s ->
                                            val pct = localReviews.count { it.second == s } / totalLocal
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("$s ★", fontSize = 10.sp, color = mutedColor, modifier = Modifier.width(24.dp))
                                                Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(cardBorder)) {
                                                    Box(Modifier.fillMaxWidth(pct).height(5.dp).clip(CircleShape).background(Color(0xFF059669)))
                                                }
                                                Text("${(pct * 100).toInt()}%", fontSize = 9.sp, color = mutedColor, modifier = Modifier.width(26.dp), textAlign = TextAlign.End)
                                            }
                                        }
                                    }
                                }
                            }
                            localReviews.forEach { (revName, revStars, revComment) ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = softOrangeBg,
                                    border = BorderStroke(1.dp, cardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("$revName • ✓ Verified Buyer", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF059669)) {
                                                Text("$revStars ★", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                            }
                                        }
                                        Text(revComment, fontSize = 11.sp, color = mutedColor)
                                    }
                                }
                            }
                        } else {
                            Text("No customer reviews yet. Tap 'Rate Product' to be the first to review!", fontSize = 11.sp, color = mutedColor)
                        }
                    }
                }
            }
        }
    }
}
