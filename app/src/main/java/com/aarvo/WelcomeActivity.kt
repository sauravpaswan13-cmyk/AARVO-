package com.aarvo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
        setContent { MobileEntryScreen() }
    }

    @Composable
    private fun MobileEntryScreen() {
        var phone by remember { mutableStateOf("") }
        val purple = Color(0xFF4B16D8)
        val deepPurple = Color(0xFF32108E)
        val orange = Color(0xFFFF7A00)
        val page = Color(0xFFF7F5FF)
        val soft = Color(0xFFF0ECFF)
        val ink = Color(0xFF171329)
        val muted = Color(0xFF706A80)

        Surface(Modifier.fillMaxSize(), color = page) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(15.dp))
                            .background(Brush.linearGradient(listOf(orange, purple))),
                        contentAlignment = Alignment.Center
                    ) { Text("A", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("AARVO", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = deepPurple, letterSpacing = 1.7.sp)
                        Text("Shop Smart • Live Better", fontSize = 11.sp, color = Color(0xFF77718B))
                    }
                }

                Spacer(Modifier.height(62.dp))
                Column(Modifier.fillMaxWidth()) {
                    Text("Enter your mobile number", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, color = ink)
                    Spacer(Modifier.height(8.dp))
                    Text("Use your mobile number to continue securely with AARVO", fontSize = 14.sp, color = muted)
                }
                Spacer(Modifier.height(26.dp))

                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(Modifier.padding(22.dp)) {
                        Text("Mobile number", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF28223B))
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(18.dp)).background(soft),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.width(82.dp).padding(start = 16.dp)) {
                                Text("INDIA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF817A93))
                                Text("+91", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = deepPurple)
                            }
                            Box(Modifier.width(1.dp).height(34.dp).background(Color(0xFFD8D1EE)))
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it.filter(Char::isDigit).take(10) },
                                placeholder = { Text("Enter 10-digit mobile number", color = Color(0xFF9A94A7), fontSize = 14.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = purple,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = ink,
                                    unfocusedTextColor = ink,
                                    cursorColor = purple
                                )
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = {
                                startActivity(Intent(this@WelcomeActivity, PhoneAuthActivity::class.java).apply {
                                    putExtra("prefill_phone", phone)
                                })
                            },
                            enabled = phone.length == 10,
                            shape = RoundedCornerShape(17.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = purple, disabledContainerColor = Color(0xFFD8D2E7)),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text("Continue to Verification", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("Your existing OTP verification will be used on the next screen.", fontSize = 11.sp, color = muted)
                    }
                }

                Spacer(Modifier.weight(1f))
                Text("Secure mobile verification • Protected by AARVO", fontSize = 11.sp, color = Color(0xFF8A8495))
            }
        }
    }
}
