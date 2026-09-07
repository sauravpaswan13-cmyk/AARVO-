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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
        if (prefs.getBoolean("signed_in", false) && !prefs.getString("auth_token", null).isNullOrBlank()) {
            openApp(); return
        }
        setContent { AarvoTheme { PhoneAuthScreen(prefs, ::openApp) } }
    }

    private fun openApp() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
}

@Composable
private fun PhoneAuthScreen(prefs: android.content.SharedPreferences, openApp: () -> Unit) {
    val api = remember { AarvoApiClient { prefs.getString("auth_token", null) } }
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

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("AARVO", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(if (otpMode) "Verify your mobile number" else if (registerMode) "Create your AARVO account" else "Login to AARVO", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        if (otpMode) {
            Text("OTP has been sent to +91 ${IndianPhoneValidator.normalize(phone)}")
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(otp, { otp = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("6-digit OTP") })
            if (otpPreview.isNotBlank()) Text("Test OTP: $otpPreview", color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            Button(onClick = {
                loading = true; error = ""
                scope.launch {
                    try {
                        val result = api.verifyPhoneOtp(phone, otp)
                        saveSession(prefs, result); openApp()
                    } catch (t: Throwable) { error = t.message ?: "OTP verification failed" }
                    finally { loading = false }
                }
            }, enabled = !loading && Regex("^[0-9]{6}$").matches(otp), modifier = Modifier.fillMaxWidth()) { if (loading) CircularProgressIndicator() else Text("Verify & continue") }
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
            OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(10) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Mobile number (10 digits)") })
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), label = { Text("Password (8+ characters)") })
            if (registerMode) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Email (optional)") })
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { seller = !seller }) { Text(if (seller) "✓ Register as seller" else "Register as buyer") }
            }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                loading = true; error = ""
                scope.launch {
                    try {
                        if (registerMode) {
                            val result = api.register(email, password, name, if (seller) "SELLER" else "BUYER", phone)
                            otpMode = true; otpPreview = result.optString("otpPreview", "")
                        } else {
                            val result = api.login(phone, password); saveSession(prefs, result); openApp()
                        }
                    } catch (t: Throwable) { error = t.message ?: "Unable to connect to AARVO server." }
                    finally { loading = false }
                }
            }, enabled = !loading && IndianPhoneValidator.isValid(phone) && password.length >= 8 && (!registerMode || name.isNotBlank()), modifier = Modifier.fillMaxWidth()) { if (loading) CircularProgressIndicator() else Text(if (registerMode) "Create account & verify phone" else "Login") }
            TextButton(onClick = { registerMode = !registerMode; error = "" }) { Text(if (registerMode) "Already have an account? Login" else "New to AARVO? Create account") }
        }
        if (!api.isConfigured()) Text("Live API is not configured in this build.", style = MaterialTheme.typography.bodySmall)
    }
}

private fun saveSession(prefs: android.content.SharedPreferences, result: JSONObject) {
    val user = result.getJSONObject("user")
    prefs.edit().putBoolean("signed_in", true).putBoolean("guest_mode", false).putString("user_name", user.optString("display_name", "AARVO User")).putString("user_role", user.optString("role", "BUYER")).putString("auth_token", result.getString("token")).apply()
}
