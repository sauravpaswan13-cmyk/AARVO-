package com.aarvo

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aarvo.ui.theme.AarvoTheme
import kotlinx.coroutines.delay

/** Legacy fallback entry flow kept aligned with the main AARVO welcome experience. */
class GuestBrowseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContent { AarvoTheme { EntryFlow(::continueBrowsing, ::openLogin) } }
    }

    private fun continueBrowsing() {
        getSharedPreferences("aarvo_prefs", MODE_PRIVATE).edit()
            .putBoolean("onboarded", true)
            .putBoolean("signed_in", false)
            .putBoolean("guest_mode", true)
            .putString("user_name", "")
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
        finish()
    }
}

@Composable
private fun EntryFlow(onBrowse: () -> Unit, onLogin: () -> Unit) {
    var showWelcome by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(1200); showWelcome = true }
    if (showWelcome) WelcomeScreen(onBrowse, onLogin) else SplashScreen()
}

@Composable
private fun SplashScreen() {
    val transition = rememberInfiniteTransition(label = "splash_motion")
    val laserX by transition.animateFloat(
        initialValue = -1.15f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1900, easing = LinearEasing)),
        label = "laser_x"
    )
    val loading by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "loading_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF16004A), Color(0xFF4B0FB8), Color(0xFF14002F))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Laser beam sweeps from the very top across the complete splash.
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(5.dp)
                .align(Alignment.TopCenter)
                .offset(x = (laserX * 420).dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFFFF5BEA), Color(0xFF62E8FF), Color.Transparent)
                    ),
                    RoundedCornerShape(50)
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 28.dp)
        ) {
            androidx.compose.foundation.Image(
                bitmap = LogoUtils.loadTransparentLogo(this@GuestBrowseActivity).asImageBitmap(),
                contentDescription = "AARVO logo",
                modifier = Modifier.size(150.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text("AARVO", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
            Spacer(Modifier.height(10.dp))

            // Laser highlight also travels across the brand line.
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "Shop Smart  •  Live Better",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.34f)
                        .height(3.dp)
                        .offset(x = (laserX * 145).dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White, Color(0xFFFF58D6), Color.Transparent)
                            ),
                            RoundedCornerShape(50)
                        )
                )
            }

            Spacer(Modifier.height(42.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = .18f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(loading)
                        .height(9.dp)
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFFFF55D6), Color(0xFF65E7FF), Color(0xFFFFD35A))),
                            RoundedCornerShape(50)
                        )
                )
            }
            Spacer(Modifier.height(11.dp))
            Text(
                "Loading your world...",
                color = Color.White.copy(alpha = .82f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun WelcomeScreen(onBrowse: () -> Unit, onLogin: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF8F5FF)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFF7F0FF), Color.White, Color(0xFFF1F7FF))
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                androidx.compose.foundation.Image(
                    bitmap = LogoUtils.loadTransparentLogo(LocalContext.current).asImageBitmap(),
                    contentDescription = "AARVO logo",
                    modifier = Modifier.size(82.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text("AARVO", color = Color(0xFF30206F), fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Spacer(Modifier.height(6.dp))
                Text("Your One Stop Shopping Destination", color = Color(0xFF57506B), fontSize = 14.sp, fontWeight = FontWeight.Medium)

                Spacer(Modifier.height(16.dp))
                TrolleyMarketScene()
                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = onBrowse,
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5A16E8), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Continue as Guest", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onLogin,
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Login / Sign Up", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(13.dp))
                Text(
                    "Shop freely • Login when you need account features or checkout",
                    fontSize = 11.sp,
                    color = Color(0xFF6B657A),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun TrolleyMarketScene() {
    val transition = rememberInfiniteTransition(label = "trolley_market_motion")

    // Trolley remains at the same place and gently rocks as if moving forward.
    val trolleyTilt by transition.animateFloat(
        initialValue = -2.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            tween(650, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "trolley_tilt"
    )

    // The market trail visibly slides behind the trolley.
    val marketSlide by transition.animateFloat(
        initialValue = -42f,
        targetValue = 42f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "market_slide"
    )

    val marketItems = listOf("👕", "👜", "📱", "👟")

    Surface(
        modifier = Modifier
            .size(width = 290.dp, height = 178.dp)
            .clip(RoundedCornerShape(30.dp)),
        color = Color(0xFFF8F3FF),
        shape = RoundedCornerShape(30.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // One clear horizontal market lane behind the trolley.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 82.dp)
                    .offset(x = marketSlide.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                marketItems.forEach { item ->
                    Surface(
                        color = Color.White.copy(alpha = .92f),
                        shape = CircleShape,
                        modifier = Modifier.size(47.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(item, fontSize = 26.sp)
                        }
                    }
                }
            }

            // Fixed trolley: only its tiny tilt changes, never its position.
            Text(
                text = "🛒",
                fontSize = 80.sp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .rotate(trolleyTilt)
            )

            // Subtle motion trail makes the forward-moving effect obvious.
            Text(
                text = "•  •  •",
                color = Color(0xFF7A4BE8).copy(alpha = .45f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 72.dp, top = 58.dp)
            )
        }
    }
}

@Composable
private fun TrustItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, contentDescription = null, tint = Color(0xFF4B17B9), modifier = Modifier.size(21.dp)); Spacer(Modifier.height(4.dp)); Text(label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF5D5D68)) }
}
