package com.aarvo

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.aarvo.network.AarvoApiClient
import kotlinx.coroutines.delay
import org.json.JSONObject

private data class HeroBannerSlide(
    val id: String,
    val titlePrimary: String,
    val titleSecondary: String,
    val subtitle: String,
    val ctaText: String,
    val ctaTarget: String,
    val gradientColors: List<Color>,
    val remoteImageUrl: String? = null,
    val drawableRes: Int? = null
)

@Composable
fun LiveHero(
    api: AarvoApiClient,
    modifier: Modifier = Modifier,
    onCtaClick: (String) -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    var serverSlides by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selected by remember { mutableIntStateOf(0) }

    val previewDefaultSlides = remember {
        listOf(
            HeroBannerSlide(
                id = "preview_verified",
                titlePrimary = "Verified Sellers",
                titleSecondary = "Direct & Genuine",
                subtitle = "Doorstep Delivery • GST Verified Merchants",
                ctaText = "Explore Drops",
                ctaTarget = "categories:All",
                gradientColors = listOf(Color(0xFFFF7043), Color(0xFFFF5722), Color(0xFFE64A19))
            ),
            HeroBannerSlide(
                id = "preview_savings",
                titlePrimary = "Big Savings",
                titleSecondary = "Big Smiles",
                subtitle = "Top Deals • Guaranteed Authenticity",
                ctaText = "Shop Now",
                ctaTarget = "offers",
                gradientColors = listOf(Color(0xFF2874F0), Color(0xFF1D4ED8), Color(0xFF1E40AF)),
                drawableRes = R.drawable.hero_big_savings_woman
            ),
            HeroBannerSlide(
                id = "preview_gadgets",
                titlePrimary = "Smart Gadgets",
                titleSecondary = "Up to 60% OFF",
                subtitle = "Wireless Audio • Smartwatches & Tech",
                ctaText = "Shop Electronics",
                ctaTarget = "categories:Electronics",
                gradientColors = listOf(Color(0xFFFFA726), Color(0xFFFB8C00), Color(0xFFEF6C00)),
                drawableRes = R.drawable.promo_gadgets_trio
            )
        )
    }

    suspend fun loadSlides() {
        serverSlides = try {
            val response = api.heroSlides()
            buildList { for (i in 0 until response.length()) add(response.getJSONObject(i)) }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    LaunchedEffect(api) {
        loadSlides()
        while (true) {
            delay(60_000L)
            loadSlides()
        }
    }

    val combinedSlides = remember(serverSlides, previewDefaultSlides) {
        if (serverSlides.isEmpty()) {
            previewDefaultSlides
        } else {
            val liveMapped = serverSlides.mapIndexed { idx, obj ->
                val rawTitle = obj.optString("title", "AARVO Featured").trim()
                val words = rawTitle.split(" ")
                val splitPoint = (words.size + 1) / 2
                HeroBannerSlide(
                    id = "server_${obj.optString("id", idx.toString())}",
                    titlePrimary = words.take(splitPoint).joinToString(" "),
                    titleSecondary = words.drop(splitPoint).joinToString(" "),
                    subtitle = obj.optString("subtitle", "Doorstep Delivery • Top Deals"),
                    ctaText = obj.optString("cta_label", "Shop Now").ifBlank { "Shop Now" },
                    ctaTarget = obj.optString("cta_target", "categories:All"),
                    gradientColors = listOf(Color(0xFFFF7043), Color(0xFFFF5722), Color(0xFFE64A19)),
                    remoteImageUrl = obj.optString("image_url").takeIf { it.startsWith("http") }
                )
            }
            liveMapped + previewDefaultSlides
        }
    }

    LaunchedEffect(combinedSlides.size) {
        if (selected >= combinedSlides.size) selected = 0
        if (combinedSlides.size <= 1) return@LaunchedEffect
        while (true) {
            delay(5000L)
            selected = (selected + 1) % combinedSlides.size
        }
    }

    val activeSlide = combinedSlides[selected.coerceIn(0, combinedSlides.lastIndex)]

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(192.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(activeSlide.gradientColors))
            .pointerInput(combinedSlides.size) {
                var accum = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount -> accum += dragAmount },
                    onDragEnd = {
                        if (accum < -35f) {
                            selected = (selected + 1) % combinedSlides.size
                        } else if (accum > 35f) {
                            selected = (selected - 1 + combinedSlides.size) % combinedSlides.size
                        }
                        accum = 0f
                    }
                )
            }
            .clickable {
                triggerHaptic(haptics)
                onCtaClick(activeSlide.ctaTarget)
            }
    ) {
        // Right-aligned artwork when present
        Crossfade(targetState = activeSlide.id, label = "heroArt") { slideId ->
            val s = combinedSlides.firstOrNull { it.id == slideId } ?: activeSlide
            when {
                !s.remoteImageUrl.isNullOrBlank() -> {
                    Box(Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = s.remoteImageUrl,
                            contentDescription = s.titlePrimary,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF0F172A).copy(alpha = 0.85f),
                                            Color(0xFF0F172A).copy(alpha = 0.45f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }
                s.drawableRes != null -> {
                    Box(Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = s.drawableRes),
                            contentDescription = s.titlePrimary,
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.50f)
                                .align(Alignment.BottomEnd),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        // Left Headline & #FFD700 CTA Pill
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.62f)
                .padding(start = 18.dp, end = 8.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = activeSlide.titlePrimary,
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 24.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (activeSlide.titleSecondary.isNotBlank()) {
                Text(
                    text = activeSlide.titleSecondary,
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 24.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (activeSlide.subtitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = activeSlide.subtitle,
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = CircleShape,
                color = Color(0xFFFFD700),
                shadowElevation = 2.dp,
                modifier = Modifier.clickable {
                    triggerHaptic(haptics, strong = false)
                    onCtaClick(activeSlide.ctaTarget)
                }
            ) {
                Text(
                    text = "${activeSlide.ctaText} >",
                    color = Color(0xFF0F172A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                )
            }
        }

        // Clickable Pagination Dots
        if (combinedSlides.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                combinedSlides.indices.forEach { index ->
                    val active = index == selected
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (active) 18.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (active) Color(0xFFFFD700) else Color.White.copy(alpha = 0.55f))
                            .clickable {
                                triggerHaptic(haptics)
                                selected = index
                            }
                    )
                }
            }
        }
    }
}
