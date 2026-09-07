package com.aarvo

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aarvo.ui.theme.AarvoTheme

/**
 * AARVO entry screen. Users can either browse the marketplace as a guest or
 * enter the verified login/signup flow. Guest browsing never creates an auth token.
 */
class GuestBrowseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AarvoTheme {
                EntryScreen(
                    onGuest = ::continueAsGuest,
                    onLogin = ::openLogin
                )
            }
        }
    }

    private fun continueAsGuest() {
        val prefs = getSharedPreferences("aarvo_prefs", MODE_PRIVATE)
        prefs.edit()
            .putBoolean("onboarded", true)
            .putBoolean("signed_in", true)
            .putBoolean("guest_mode", true)
            .putString("user_name", "Guest")
            .putString("user_role", "BUYER")
            .remove("auth_token")
            .apply()
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    private fun openLogin() {
        getSharedPreferences("aarvo_prefs", MODE_PRIVATE).edit()
            .putBoolean("onboarded", true)
            .putBoolean("guest_mode", false)
            .apply()
        startActivity(
            Intent(this, PhoneAuthActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }
}

@Composable
private fun EntryScreen(onGuest: () -> Unit, onLogin: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("AARVO", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Shop smart. Live better.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("Explore products without verification, or sign in to use your AARVO account and purchase securely.")
        Spacer(Modifier.height(28.dp))
        Button(onClick = onGuest, modifier = Modifier.fillMaxWidth()) {
            Text("Continue as Guest")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Login / Signup")
        }
    }
}
