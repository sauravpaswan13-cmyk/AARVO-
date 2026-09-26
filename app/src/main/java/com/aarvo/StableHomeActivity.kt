package com.aarvo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aarvo.ui.theme.AarvoTheme

class StableHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE)
        val signedIn = prefs.getBoolean("signed_in", false)
        setContent { AarvoTheme { StableHome(signedIn) } }
    }

    @Composable
    private fun StableHome(signedIn: Boolean) {
        var query by remember { mutableStateOf("") }
        val categories = listOf("Fashion", "Mobiles", "Electronics", "Beauty", "Home", "Shoes")
        Scaffold(
            topBar = {
                Surface(shadowElevation = 4.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painterResource(R.drawable.aarvo_logo), "AARVO", tint = Color.Unspecified, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("AARVO", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4B16D8))
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = {}) { Icon(Icons.Default.ShoppingCart, "Cart") }
                        IconButton(onClick = {}) { Icon(Icons.Default.Person, "Account") }
                    }
                }
            }
        ) { inner ->
            Column(Modifier.fillMaxSize().padding(inner).padding(horizontal = 14.dp)) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text("Search products on AARVO") },
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier.fillMaxWidth().height(190.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF4B16D8), Color(0xFFB32CDE), Color(0xFF157BFF))),
                            RoundedCornerShape(24.dp)
                        ).padding(22.dp)
                ) {
                    Column(Modifier.align(Alignment.CenterStart)) {
                        Text("AARVO", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Premium Shopping", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(if (signedIn) "Welcome back" else "Browse freely as Guest", color = Color.White.copy(alpha = .9f))
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("Categories", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(categories.size) { i ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF0ECFF)
                        ) {
                            Text(categories[i], modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                fontWeight = FontWeight.Bold, color = Color(0xFF32108E))
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Welcome to AARVO", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(6.dp))
                        Text("Your shopping home is ready. Product browsing and account features can continue without the startup crash.")
                    }
                }
            }
        }
    }
}
