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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aarvo.ui.theme.AarvoTheme

/** AARVO premium entry: guest browsing and verified account access remain available. */
class GuestBrowseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AarvoTheme { EntryScreen(::continueAsGuest, ::openLogin) } }
    }

    private fun continueAsGuest() {
        getSharedPreferences("aarvo_prefs", MODE_PRIVATE).edit()
            .putBoolean("onboarded", true)
            .putBoolean("signed_in", true)
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
            .apply()
        startActivity(Intent(this, PhoneAuthActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }
}

@Composable
private fun EntryScreen(onGuest: () -> Unit, onLogin: () -> Unit) {
    val gold = MaterialTheme.colorScheme.primary
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)))) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(shape = CircleShape, color = gold.copy(alpha = .14f), modifier = Modifier.size(92.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.ShoppingBag, "AARVO", tint = gold, modifier = Modifier.size(48.dp)) }
                }
                Spacer(Modifier.height(18.dp))
                Text("AARVO", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp, color = gold)
                Text("SHOP MORE. LIVE BETTER.", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f))
                Spacer(Modifier.height(26.dp))
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Your One-Stop Shopping Destination", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Browse freely. Sign in only when you need your account or want to complete a purchase.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .75f))
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = onGuest, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = MaterialTheme.colorScheme.onPrimary)) {
                            Icon(Icons.Default.Person, null); Spacer(Modifier.size(8.dp)); Text("CONTINUE AS GUEST", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                            Icon(Icons.Default.Lock, null); Spacer(Modifier.size(8.dp)); Text("LOGIN / SIGN UP", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TrustItem(Icons.Default.Security, "SECURE")
                    TrustItem(Icons.Default.ShoppingBag, "QUALITY")
                    TrustItem(Icons.Default.Speed, "FAST")
                }
            }
        }
    }
}

@Composable
private fun TrustItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}
