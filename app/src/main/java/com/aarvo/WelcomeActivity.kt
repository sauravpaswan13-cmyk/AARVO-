package com.aarvo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AarvoMobileEntry() }
    }

    @Composable
    private fun AarvoMobileEntry() {
        var phone by remember { mutableStateOf("") }
        val bg = Color(0xFF041A16)
        val panel = Color(0xFF092820)
        val gold = Color(0xFFFFC72C)
        val goldSoft = Color(0xFFFFE08A)
        val white = Color(0xFFF9FBF8)
        val muted = Color(0xFFA9BDB7)
        val border = Color(0xFF24473E)

        Surface(Modifier.fillMaxSize(), color = bg) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(270.dp).background(Brush.verticalGradient(listOf(Color(0xFF0B3D31), bg))))
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(gold, Color(0xFFD99000)))), contentAlignment = Alignment.Center) { Text("A", color = Color(0xFF071A14), fontSize = 29.sp, fontWeight = FontWeight.Black) }
                        Spacer(Modifier.width(12.dp))
                        Column { Text("AARVO", color = white, fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp); Text("BETTER CHOICE  •  BRIGHTER LIFE", color = goldSoft, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                        Spacer(Modifier.weight(1f)); Icon(Icons.Default.Lock, null, tint = goldSoft, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.height(58.dp))
                    Column(Modifier.fillMaxWidth()) { Text("Sign in to AARVO", color = white, fontSize = 30.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(8.dp)); Text("Enter your mobile number to continue shopping securely.", color = muted, fontSize = 14.sp, lineHeight = 21.sp) }
                    Spacer(Modifier.height(26.dp))
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = panel), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                        Column(Modifier.padding(22.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Phone, null, tint = gold, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(9.dp)); Text("Mobile number", color = white, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF061C17)).border(1.dp, if (phone.length == 10) gold else border, RoundedCornerShape(18.dp)), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.width(76.dp).padding(start = 17.dp)) { Text("INDIA", color = muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp); Text("+91", color = gold, fontSize = 18.sp, fontWeight = FontWeight.Black) }
                                Box(Modifier.width(1.dp).height(36.dp).background(border))
                                OutlinedTextField(value = phone, onValueChange = { phone = it.filter(Char::isDigit).take(10) }, placeholder = { Text("10-digit mobile number", color = Color(0xFF6E8981), fontSize = 14.sp) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.weight(1f).fillMaxHeight(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = white, unfocusedTextColor = white, cursorColor = gold))
                            }
                            Spacer(Modifier.height(18.dp))
                            Button(onClick = { startActivity(Intent(this@WelcomeActivity, PhoneAuthActivity::class.java).apply { putExtra("prefill_phone", phone) }) }, enabled = phone.length == 10, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color(0xFF071A14), disabledContainerColor = Color(0xFF28463E), disabledContentColor = Color(0xFF779089))) { Text("Continue to Verification", fontSize = 16.sp, fontWeight = FontWeight.Black); Spacer(Modifier.width(8.dp)); Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(19.dp)) }
                            Spacer(Modifier.height(13.dp)); Text("Your existing AARVO OTP verification will be used on the next screen.", color = muted, fontSize = 11.sp, lineHeight = 17.sp)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Lock, null, tint = Color(0xFF718A83), modifier = Modifier.size(13.dp)); Spacer(Modifier.width(5.dp)); Text("Secure mobile verification  •  AARVO", color = Color(0xFF718A83), fontSize = 10.sp) }
                }
            }
        }
    }
}
