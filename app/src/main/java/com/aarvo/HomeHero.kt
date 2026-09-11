package com.aarvo

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aarvo.network.AarvoApiClient
import kotlinx.coroutines.delay
import org.json.JSONObject

@Composable
fun LiveHero(api: AarvoApiClient, modifier: Modifier = Modifier) {
    var slides by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf(0) }

    suspend fun loadSlides() {
        slides = try {
            val response = api.heroSlides()
            buildList { for (i in 0 until response.length()) add(response.getJSONObject(i)) }
        } catch (_: Throwable) {
            emptyList()
        }
        if (selected >= slides.size) selected = 0
    }

    LaunchedEffect(api) {
        loading = true
        loadSlides()
        loading = false
    }

    // Keep the home hero genuinely live: refresh content periodically and
    // rotate through all active admin-published slides automatically.
    LaunchedEffect(api, slides.size) {
        if (slides.size <= 1) return@LaunchedEffect
        while (true) {
            delay(4500)
            selected = (selected + 1) % slides.size
        }
    }

    LaunchedEffect(api) {
        while (true) {
            delay(60_000)
            loadSlides()
        }
    }

    when {
        loading -> Box(
            modifier.fillMaxWidth().height(205.dp),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }

        slides.isEmpty() -> Spacer(modifier.fillMaxWidth().height(16.dp))

        else -> {
            val slide = slides[selected.coerceIn(0, slides.lastIndex)]
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(205.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(enabled = slide.optString("cta_target").isNotBlank()) {
                        // Target routing can be wired by the host screen later;
                        // the hero remains tappable only when an admin target exists.
                    }
            ) {
                Crossfade(targetState = slide.optString("image_url"), label = "heroImage") { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = slide.optString("title", "AARVO"),
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(
                    Modifier.matchParentSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                        )
                    )
                )
                Column(
                    Modifier.align(Alignment.BottomStart).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        slide.optString("title", "AARVO Premium Picks"),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    val subtitle = slide.optString("subtitle")
                    if (subtitle.isNotBlank()) {
                        Text(
                            subtitle,
                            color = Color.White.copy(alpha = 0.92f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    val cta = slide.optString("cta_label")
                    if (cta.isNotBlank()) {
                        Text(
                            cta,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (slides.size > 1) {
                    Row(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        slides.indices.forEach { index ->
                            Box(
                                Modifier
                                    .size(if (index == selected) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index == selected) Color.White else Color.White.copy(alpha = 0.45f)
                                    )
                            )
                            if (index != slides.lastIndex) Spacer(Modifier.width(5.dp))
                        }
                    }
                }
            }
        }
    }
}
