package com.aarvo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import com.aarvo.ui.theme.AarvoTheme
import kotlinx.coroutines.delay

/** AARVO premium entry flow: branded splash first, then guest/account entry. */
class GuestBrowseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AarvoTheme { EntryFlow(::continueAsGuest, ::openLogin) } }
    }

    private fun continueAsGuest() {
        getSharedPreferences("aarvo_prefs", MODE_PRIVATE).edit()
            .putBoolean("onboarded", true)
            .putBoolean("signed_in", false)
            .putBoolean("guest_mode", true)
            .putString("user_name", "Guest")
            .putString("user_role", "BUYER")
            .remove("auth_token")
            .apply()
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    private fun openLogin() {
        getSharedPreferences("aarvo_prefs", MODE_PRIVATE).edit()
            .putBoolean("onboarded", true)
            .putBoolean("guest_mode", false)
            .putBoolean("signed_in", false)
            .remove("auth_token")
            .apply()
        startActivity(Intent(this, PhoneAuthActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }
}

@Composable
private fun EntryFlow(onGuest: () -> Unit, onLogin: () -> Unit) {
    var showWelcome by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1400)
        showWelcome = true
    }
    if (showWelcome) WelcomeScreen(onGuest, onLogin) else SplashScreen()
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF5B00C9), Color(0xFF28006E), Color(0xFF10004A)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.aarvo_logo),
                contentDescription = "AARVO",
                tint = Color.Unspecified,
                modifier = Modifier.size(132.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text("AARVO", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
            Spacer(Modifier.height(4.dp))
            Text("Shop Smart  •  Live Better", color = Color.White.copy(alpha = .92f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(46.dp))
            Text("Loading your world...", color = Color.White.copy(alpha = .75f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun WelcomeScreen(onGuest: () -> Unit, onLogin: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.aarvo_logo),
                    contentDescription = "AARVO",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text("AARVO", color = Color(0xFF20206B), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text("Your One Stop Shopping Destination", color = Color(0xFF34345E), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(22.dp))
            Surface(
                modifier = Modifier.size(width = 190.dp, height = 150.dp),
                color = Color(0xFFF7F3FF),
                shape = RoundedCornerShape(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.aarvo_logo), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(100.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onGuest,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5A16E8), contentColor = Color.White)
            ) {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Continue as Guest", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(13.dp)) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Login / Sign Up", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onGuest) {
                Text("Explore as Guest", color = Color(0xFF4B17B9), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TrustItem(Icons.Default.Security, "Secure")
                TrustItem(Icons.Default.Lock, "Trusted Shopping")
                TrustItem(Icons.Default.Person, "Easy Access")
            }
        }
    }
}

@Composable
private fun TrustItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = Color(0xFF4B17B9), modifier = Modifier.size(21.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF5D5D68))
    }
}
