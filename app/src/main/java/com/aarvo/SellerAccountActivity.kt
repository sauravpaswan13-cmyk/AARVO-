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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aarvo.network.AarvoApiClient
import com.aarvo.ui.theme.AarvoTheme
import kotlinx.coroutines.launch

class SellerAccountActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE)
        val api = AarvoApiClient { prefs.getString("auth_token", null) }
        setContent {
            AarvoTheme {
                SellerAccountScreen(
                    api = api,
                    onVerified = { token, phone, storeName ->
                        prefs.edit()
                            .putBoolean("signed_in", true)
                            .putBoolean("guest_mode", false)
                            .putString("auth_token", token)
                            .putString("user_role", "SELLER")
                            .putString("seller_phone", phone)
                            .putString("seller_store_name", storeName)
                            .apply()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun SellerAccountScreen(
    api: AarvoApiClient,
    onVerified: (String, String, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var storeName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var otpMode by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    fun startRegistration() {
        scope.launch {
            loading = true
            message = ""
            try {
                api.register(password = password, displayName = ownerName, role = "SELLER", phone = phone)
                api.resendPhoneOtp(phone)
                otpMode = true
                message = "OTP sent to +91 $phone. Enter it below to activate your Seller account."
            } catch (e: Exception) {
                message = e.message ?: "Seller registration failed. Please try again."
            } finally {
                loading = false
            }
        }
    }

    fun verify() {
        scope.launch {
            loading = true
            message = ""
            try {
                val session = api.verifyPhoneOtp(phone, otp)
                val token = session.optString("token").trim()
                if (token.isBlank()) error("Seller session token was not returned")
                onVerified(token, phone, storeName.trim())
            } catch (e: Exception) {
                message = e.message ?: "OTP verification failed."
            } finally {
                loading = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("AARVO Seller Account", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Premium Seller Central onboarding — mobile verification keeps the account secure.")

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (otpMode) "Verify Seller Mobile" else "Create Seller Account", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                if (!otpMode) {
                    OutlinedTextField(ownerName, { ownerName = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Owner name") })
                    OutlinedTextField(storeName, { storeName = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Store / business name") })
                    OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(10) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Mobile number") })
                    OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Create password (8+ characters)") })
                    Spacer(Modifier.height(4.dp))
                    Button(onClick = { startRegistration() }, enabled = !loading && ownerName.isNotBlank() && storeName.isNotBlank() && phone.length == 10 && password.length >= 8, modifier = Modifier.fillMaxWidth()) {
                        Text(if (loading) "Creating account…" else "Continue Seller Registration")
                    }
                } else {
                    Text("We sent a 6-digit OTP to +91 $phone")
                    OutlinedTextField(otp, { otp = it.filter(Char::isDigit).take(8) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Enter OTP") })
                    Button(onClick = { verify() }, enabled = !loading && otp.length >= 4, modifier = Modifier.fillMaxWidth()) {
                        Text(if (loading) "Verifying…" else "Verify & Activate Seller Account")
                    }
                    Button(onClick = {
                        scope.launch {
                            loading = true
                            message = ""
                            try { api.resendPhoneOtp(phone); message = "A new OTP has been sent." }
                            catch (e: Exception) { message = e.message ?: "Could not resend OTP." }
                            finally { loading = false }
                        }
                    }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Resend OTP") }
                }

                if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall)
            }
        }

        Text("After verification: Seller Central → store profile → products → inventory → orders → earnings/payouts. KYC, bank and real-money payout activation remain provider/account dependent.", style = MaterialTheme.typography.bodyMedium)
    }
}
