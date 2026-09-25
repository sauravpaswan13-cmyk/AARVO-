package com.aarvo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aarvo.data.Product

private data class AarvoCategoryVisual(val label: String, val emoji: String, val backend: String)

@Composable
fun HomeCategoryGrid(onCategoryChange: (String) -> Unit) {
    val categories = listOf(
        AarvoCategoryVisual("Mobiles", "📱", "Mobiles"),
        AarvoCategoryVisual("Fashion", "🧥", "Fashion"),
        AarvoCategoryVisual("Electronics", "💻", "Electronics"),
        AarvoCategoryVisual("Home & Living", "🛋️", "Home"),
        AarvoCategoryVisual("Appliances", "🧺", "Appliances"),
        AarvoCategoryVisual("Beauty & Health", "💄", "Beauty"),
        AarvoCategoryVisual("Sports", "👟", "Sports"),
        AarvoCategoryVisual("Toys & Kids", "🧸", "Toys"),
        AarvoCategoryVisual("Grocery", "🛒", "Grocery"),
        AarvoCategoryVisual("More", "•••", "All")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(horizontal = 5.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        categories.chunked(5).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { category ->
                    Column(
                        modifier = Modifier
                            .width(68.dp)
                            .clickable { onCategoryChange(category.backend) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFF7F0FF), Color(0xFFE8D9FF))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(category.emoji, style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            category.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeDealsBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF6512C1), Color(0xFF8A24D6))))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🏷️", fontSize = 34.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Top Deals For You", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text("Best Offers  •  Lowest Prices  •  Limited Time", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
        }
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFFFD51A)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowForward, contentDescription = "Deals", tint = Color(0xFF5B12B8))
        }
    }
}

@Composable
fun HomeProductDeals(products: List<Product>, onOpen: (Product) -> Unit, onAdd: (Product) -> Unit) {
    if (products.isEmpty()) return
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp)
    ) {
        items(products.take(8), key = { it.id }) { product ->
            Column(
                modifier = Modifier
                    .width(148.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .clickable { onOpen(product) }
                    .padding(10.dp)
            ) {
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .fillMaxWidth()
                        .height(108.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5FA)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(product.emoji, style = MaterialTheme.typography.displaySmall)
                }
                Spacer(Modifier.height(7.dp))
                Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                Text(product.displayPrice, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✓ Best Deal", color = Color(0xFF0A9B50), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HomeMegaSaleBanner() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF6512C1), Color(0xFFB22ACF), Color(0xFFFF4E8A))))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text("AARVO", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Mega Sale", color = Color(0xFFFFE11A), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
                Text("Bigger Discounts  •  Better Deals", color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
            androidx.compose.material3.Button(
                onClick = { },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD51A), contentColor = Color(0xFF5B12B8)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text("Shop Now →", fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(6.dp))
            Text("🛒", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
