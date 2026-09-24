package com.aarvo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AarvoHomeTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    cartCount: Int,
    onCartClick: () -> Unit
) {
    val purple = Color(0xFF5B12B8)
    val purple2 = Color(0xFF8A24D6)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(purple, purple2)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.aarvo_logo),
                    contentDescription = "AARVO logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    "AARVO",
                    color = Color.White,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp
                )
            }
            BadgedBox(badge = { Badge { Text("3") } }) {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            BadgedBox(badge = { if (cartCount > 0) Badge { Text(cartCount.coerceAtMost(99).toString()) } }) {
                IconButton(onClick = onCartClick) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
        }

        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
            placeholder = { Text("Search for products, brands and more...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = purple) },
            trailingIcon = { Icon(Icons.Default.Mic, contentDescription = "Voice search", tint = purple) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )
        Spacer(Modifier.height(2.dp))
    }
}
