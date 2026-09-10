package com.aarvo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aarvo.ui.theme.AarvoTheme

class SellerAccountActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AarvoTheme { SellerAccountScreen() } }
    }
}

@androidx.compose.runtime.Composable
private fun SellerAccountScreen() {
    var storeName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("AARVO Seller Account", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Start selling on AARVO with your own store dashboard.")

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Seller Registration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(ownerName, { ownerName = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Owner name") })
                OutlinedTextField(storeName, { storeName = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Store / business name") })
                OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Mobile number") })
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { },
                    enabled = ownerName.isNotBlank() && storeName.isNotBlank() && phone.length >= 10,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Continue Seller Registration") }
            }
        }

        Text("Next steps: KYC & tax details • Bank / payout details • Product listing • Orders • Earnings", style = MaterialTheme.typography.bodyMedium)
    }
}
