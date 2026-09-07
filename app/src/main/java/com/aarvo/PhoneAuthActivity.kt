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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aarvo.network.AarvoApiClient
import com.aarvo.network.IndianPhoneValidator
import com.aarvo.ui.theme.AarvoTheme
import kotlinx.coroutines.launch
import org.json.JSONObject

class PhoneAuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("aarvo_prefs", MODE_PRIVATE)
        val api = AarvoApiClient { prefs.getString("auth_token", null) }
        setContent { AarvoTheme { PhoneAuthScreen(api, prefs) { openApp() } } }
    }

    private fun openApp() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
}

@androidx.compose.runtime.Composable
private fun PhoneAuthScreen(api: AarvoApiClient, prefs: android.content.SharedPreferences, openApp: () -> Unit) {
    var registerMode by remember { mutableStateOf(false) }
    var otpMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var seller by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var otpPreview by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("AARVO", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(20.dp))
        if (otpMode) {
            Text("Verify your mobile number", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("OTP sent to +91 $phone")
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(otp, { otp = it.filter(Char::isDigit).take(8) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("6-digit OTP") })
            if (otpPreview.isNotBlank()) { Spacer(Modifier.height(6.dp)); Text("Test OTP: $otpPreview", color = MaterialTheme.colorScheme.primary) }
            if (error.isNotBlank()) { Spacer(Modifier.height(6.dp)); Text(error, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                loading = true; error = ""
                scope.launch {
                    try { val result = api.verifyPhoneOtp(phone, otp); saveSession(prefs, result); openApp() }
                    catch (t: Throwable) { error = t.message ?: "OTP verification failed" }
                    finally { loading = false }
                }
            }, enabled = !loading && otp.length == 6, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                if (loading) CircularProgressIndicator() else Text("Verify & Continue", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = {
                if (!loading) scope.launch {
                    loading = true; error = ""
                    try { val result = api.resendPhoneOtp(phone); otpPreview = result.optString("otpPreview", "") }
                    catch (t: Throwable) { error = t.message ?: "Unable to resend OTP" }
                    finally { loading = false }
                }
            }) { Text("Resend OTP") }
        } else {
            if (registerMode) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Full name") })
                Spacer(Modifier.height(8.dp))
            }
            OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(10) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Mobile number") }, prefix = { Text("+91  ") })
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), label = { Text("Password") })
            if (registerMode) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Email (optional)") })
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { seller = !seller }) { Text(if (seller) "✓ Register as seller" else "Register as buyer") }
            }
            if (error.isNotBlank()) { Spacer(Modifier.height(4.dp)); Text(error, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                loading = true; error = ""
                scope.launch {
                    try {
                        if (registerMode) {
                            api.register(email, password, name, if (seller) "SELLER" else "BUYER", phone)
                            val otpResult = api.resendPhoneOtp(phone)
                            otpPreview = otpResult.optString("otpPreview", "")
                            otpMode = true
                        } else {
                            val result = api.login(phone, password)
                            if (result.optBoolean("requiresPhoneVerification", false)) {
                                val otpResult = api.resendPhoneOtp(phone)
                                otpPreview = otpResult.optString("otpPreview", "")
                                otpMode = true
                            } else {
                                saveSession(prefs, result); openApp()
                            }
                        }
                    } catch (t: Throwable) { error = t.message ?: "Unable to connect to AARVO server." }
                    finally { loading = false }
                }
            }, enabled = !loading && IndianPhoneValidator.isValid(phone) && password.length >= 8 && (!registerMode || name.isNotBlank()), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                if (loading) CircularProgressIndicator() else Text(if (registerMode) "Create account & verify" else "Continue", fontWeight = FontWeight.Bold)
            }
            if (!registerMode) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { registerMode = true; error = "" }, modifier = Modifier.fillMaxWidth()) { Text("New to AARVO? Create account") }
                Text("By continuing, you agree to AARVO's Terms & Privacy Policy", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = {
                    prefs.edit().putBoolean("signed_in", false).putBoolean("guest_mode", true).putString("user_name", "Guest").putString("user_role", "BUYER").remove("auth_token").apply()
                    openApp()
                }, modifier = Modifier.fillMaxWidth()) { Text("Continue as Guest", fontWeight = FontWeight.Bold) }
            } else {
                TextButton(onClick = { registerMode = false; error = "" }) { Text("Already have an account? Login") }
            }
        }
        if (!api.isConfigured()) Text("Live API is not configured in this build.", style = MaterialTheme.typography.bodySmall)
    }
}

private fun saveSession(prefs: android.content.SharedPreferences, result: JSONObject) {
    val user = result.getJSONObject("user")
    prefs.edit().putBoolean("signed_in", true).putBoolean("guest_mode", false).putString("user_name", user.optString("display_name", "AARVO User")).putString("user_role", user.optString("role", "BUYER")).putString("auth_token", result.getString("token")).apply()
}
