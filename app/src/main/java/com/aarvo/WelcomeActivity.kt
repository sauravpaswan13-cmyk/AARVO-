package com.aarvo

import android.content.Context
import android.content.Intent
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aarvo.ui.theme.AarvoTheme

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AarvoTheme { WelcomeScreen() } }
    }

    @Composable
    private fun WelcomeScreen() {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.aarvo_logo),
                contentDescription = "AARVO logo",
                tint = Color.Unspecified,
                modifier = Modifier.height(88.dp).fillMaxWidth(0.38f)
            )
            Spacer(Modifier.height(8.dp))
            Text("AARVO", color = Color(0xFF2E178C), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineLarge)
            Text("Your One Stop Shopping Destination", color = Color(0xFF5D5D70), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(18.dp))
            Text("🛍️   🛍️   🛍️", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(26.dp))
            Button(onClick = { enterGuest() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Continue as Guest", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { openLogin() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Login / Sign Up", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { enterGuest() }) { Text("Explore as Guest") }
            Spacer(Modifier.height(28.dp))
            Text("Secure Shopping  •  Trusted Support  •  Fast Delivery", color = Color(0xFF666070), style = MaterialTheme.typography.bodySmall)
        }
    }

    private fun prefs() = getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE)

    private fun enterGuest() {
        prefs().edit().putBoolean("onboarded", true).putBoolean("guest_mode", true).putBoolean("signed_in", false).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun openLogin() {
        prefs().edit().putBoolean("onboarded", true).putBoolean("guest_mode", false).apply()
        startActivity(Intent(this, PhoneAuthActivity::class.java))
        finish()
    }
}
