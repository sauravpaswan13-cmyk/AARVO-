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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aarvo.ui.theme.AarvoTheme
import kotlinx.coroutines.delay

/** AARVO branded entry flow: reference-style splash followed by the welcome/guest gate. */
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
        delay(1500)
        showWelcome = true
    }
    if (showWelcome) WelcomeScreen(onGuest, onLogin) else SplashScreen()
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0xFF5A00C9), Color(0xFF2D0079), Color(0xFF10004B))
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.aarvo_logo),
                contentDescription = "AARVO logo",
                tint = Color.Unspecified,
                modifier = Modifier.size(138.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "AARVO",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.5.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Shop Smart  •  Live Better",
                color = Color.White.copy(alpha = .94f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(44.dp))
            Surface(color = Color.White.copy(alpha = .18f), shape = CircleShape, modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("", modifier = Modifier.size(1.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Loading your world...", color = Color.White.copy(alpha = .78f), fontSize = 12.sp)
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
                    contentDescription = "AARVO logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(58.dp)
                )
                Spacer(Modifier.size(7.dp))
                Text(
                    "AARVO",
                    color = Color(0xFF22236D),
                    fontSize = 31.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                "Your One Stop Shopping Destination",
                color = Color(0xFF3D3D5B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(20.dp))

            Surface(
                modifier = Modifier.size(width = 210.dp, height = 164.dp),
                color = Color(0xFFF7F2FF),
                shape = RoundedCornerShape(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Icon(Icons.Default.ShoppingBag, null, tint = Color(0xFFFF9D24), modifier = Modifier.size(74.dp))
                        Icon(Icons.Default.ShoppingBag, null, tint = Color(0xFF5A16E8), modifier = Modifier.size(96.dp))
                        Icon(Icons.Default.ShoppingBag, null, tint = Color(0xFFFF4B9B), modifier = Modifier.size(62.dp))
                    }
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
            OutlinedButton(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(13.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Login / Sign Up", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onGuest) {
                Text("Explore as Guest", color = Color(0xFF4B17B9), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TrustItem(Icons.Default.Security, "Secure")
                TrustItem(Icons.Default.LocalShipping, "Trusted Shopping")
                TrustItem(Icons.Default.Lock, "Safe Payments")
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