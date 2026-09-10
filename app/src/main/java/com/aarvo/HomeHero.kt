package com.aarvo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import org.json.JSONObject

@Composable
fun LiveHero(api: AarvoApiClient, modifier: Modifier = Modifier) {
    var slides by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(api) {
        loading = true
        slides = try {
            val response = api.heroSlides()
            buildList { for (i in 0 until response.length()) add(response.getJSONObject(i)) }
        } catch (_: Throwable) { emptyList() }
        loading = false
    }
    if (loading) Box(modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    else if (slides.isNotEmpty()) {
        val slide = slides.first()
        Box(modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(24.dp))) {
            AsyncImage(
                model = slide.optString("image_url"),
                contentDescription = slide.optString("title", "AARVO"),
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)))))
            Column(Modifier.align(Alignment.BottomStart).padding(18.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(slide.optString("title", "AARVO Premium Picks"), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                val subtitle = slide.optString("subtitle")
                if (subtitle.isNotBlank()) Text(subtitle, color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodyMedium)
                val cta = slide.optString("cta_label")
                if (cta.isNotBlank()) Text(cta, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}
