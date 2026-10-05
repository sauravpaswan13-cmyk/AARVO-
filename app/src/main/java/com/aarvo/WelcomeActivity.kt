package com.aarvo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        setContent {
            val context = LocalContext.current
            val logo = remember { LogoUtils.loadTransparentLogo(this@WelcomeActivity).asImageBitmap() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8F4FC)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(14.dp, RoundedCornerShape(42.dp)),
                        shape = RoundedCornerShape(42.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp, vertical = 54.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(132.dp)
                                    .shadow(8.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEDE5FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = logo,
                                    contentDescription = "AARVO original logo",
                                    modifier = Modifier.size(112.dp)
                                )
                            }

                            Spacer(Modifier.height(52.dp))

                            Text(
                                text = "AARVO",
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                letterSpacing = 2.sp
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "Shop Smart • Live Better",
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(28.dp))

                            Text(
                                text = "Discover products. Shop freely. Enjoy\nAARVO.",
                                fontSize = 18.sp,
                                color = Color(0xFF202020),
                                lineHeight = 28.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(42.dp))

                            Button(
                                onClick = { openLogin(context) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(78.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF7137D6),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(31.dp)
                                )
                                Spacer(Modifier.width(18.dp))
                                Text(
                                    text = "Login / Sign Up",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(24.dp))

                            OutlinedButton(
                                onClick = { enterGuest(context) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(78.dp),
                                shape = RoundedCornerShape(28.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    2.dp,
                                    Color(0xFFB99ADD)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF202020)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = Color(0xFF6B6870)
                                )
                                Spacer(Modifier.width(18.dp))
                                Text(
                                    text = "Continue as Guest",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(26.dp))

                    Text(
                        text = "A premium shopping experience, made simple.",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    private fun prefs() = getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE)

    private fun enterGuest(context: Context) {
        prefs().edit()
            .putBoolean("onboarded", true)
            .putBoolean("guest_mode", true)
            .putBoolean("signed_in", false)
            .putString("user_role", "BUYER")
            .remove("auth_token")
            .apply()

        context.startActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    private fun openLogin(context: Context) {
        prefs().edit()
            .putBoolean("onboarded", true)
            .putBoolean("guest_mode", false)
            .apply()

        context.startActivity(Intent(context, PhoneAuthActivity::class.java))
        finish()
    }
}
