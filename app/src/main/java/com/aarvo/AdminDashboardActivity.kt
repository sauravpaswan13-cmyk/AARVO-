package com.aarvo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aarvo.ui.theme.AarvoTheme

class AdminDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AarvoTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("AARVO Admin", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Role: ADMIN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("You are signed in with the AARVO administrator account.")
                            Text("Admin-only backend permissions are enforced server-side.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text("Admin controls", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Seller approval, catalog moderation, categories/brands, orders and refunds can be connected here as their admin APIs are enabled.")
                    Button(onClick = { finish() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Continue to AARVO")
                    }
                }
            }
        }
    }
}
