package com.aarvo

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

@Composable
fun PremiumHomeHeader() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 4.dp
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("AARVO", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text("Premium shopping, made simple", style = MaterialTheme.typography.bodySmall)
                }
                Text("✨", style = MaterialTheme.typography.titleLarge)
            }
        }
        Spacer(Modifier.padding(4.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickCategory("Mobiles", Icons.Default.Search)
            QuickCategory("Fashion", Icons.Default.Favorite)
            QuickCategory("Electronics", Icons.Default.Info)
            QuickCategory("Grocery", Icons.Default.Home)
            QuickCategory("Deals", Icons.Default.Star)
        }
    }
}

@Composable
private fun QuickCategory(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        Modifier.width(92.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label)
            Spacer(Modifier.padding(2.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}


@Composable
fun HomeSearchFirst(query: String, onQueryChange: (String) -> Unit) {
    val popular = listOf("Mobiles", "Fashion", "Electronics", "Grocery", "Beauty", "Home", "Sports")
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE) }
    var recentSearches by remember {
        mutableStateOf(prefs.getStringSet("recent_searches", emptySet())?.toList()?.take(8) ?: emptyList())
    }
    val trimmedQuery = query.trim()
    val suggestions = if (trimmedQuery.length >= 2) {
        popular.filter { it.contains(trimmedQuery, ignoreCase = true) }.take(4)
    } else emptyList()

    LaunchedEffect(trimmedQuery) {
        if (trimmedQuery.length >= 2) {
            delay(800)
            val updated = listOf(trimmedQuery) + recentSearches.filterNot { it.equals(trimmedQuery, ignoreCase = true) }
            recentSearches = updated.take(8)
            prefs.edit().putStringSet("recent_searches", recentSearches.toSet()).apply()
        }
    }

    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            placeholder = { Text("Search for products, brands and more") }
        )
        if (trimmedQuery.isBlank() && recentSearches.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 3.dp
            ) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent searches", fontWeight = FontWeight.Bold)
                        Text(
                            "Clear",
                            modifier = Modifier.clickable {
                                recentSearches = emptyList()
                                prefs.edit().remove("recent_searches").apply()
                            },
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    recentSearches.forEach { recent ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onQueryChange(recent) }.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(recent)
                        }
                    }
                }
            }
        } else if (suggestions.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 3.dp
            ) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    suggestions.forEach { suggestion ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onQueryChange(suggestion) }.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(suggestion, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
