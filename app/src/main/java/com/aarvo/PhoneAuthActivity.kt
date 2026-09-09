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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aarvo.network.AarvoApiClient
import com.aarvo.network.IndianPhoneValidator
import com.aarvo.ui.theme.AarvoTheme
import com.msg91.sendotp.OTPWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

private fun widgetResultText(raw: Any?): String = when (raw) {
    is String -> raw
    else -> raw?.toString() ?: ""
}

private fun widgetField(raw: String, vararg keys: String): String {
    val json = runCatching { JSONObject(raw) }.getOrNull() ?: return ""
    for (key in keys) {
        val value = json.optString(key, "").trim()
        if (value.isNotBlank()) return value
    }
    val data = json.optJSONObject("data")
    if (data != null) {
        for (key in keys) {
            val value = data.optString(key, "").trim()
            if (value.isNotBlank()) return value
        }
    }
    return ""
}

@Composable
private fun PhoneAuthScreen(api: AarvoApiClient, prefs: android.content.SharedPreferences, openApp: () -> Unit) {
    var registerMode by remember { mutableStateOf(false) }
    var otpMode by remember { mutableStateOf(false) }
    var adminVerified by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var seller by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var reqId by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun sendWidgetOtp() {
        if (BuildConfig.MSG91_WIDGET_TOKEN.isBlank()) {
            error = "OTP service token is not configured in this build."
            return
        }
        loading = true
        error = ""
        scope.launch {
            try {
                val normalizedPhone = IndianPhoneValidator.isValidOrThrow(phone)
                if (registerMode) {
                    api.register(email, password, name, if (seller) "SELLER" else "BUYER", normalizedPhone)
                }
                val raw = withContext(Dispatchers.IO) {
                    OTPWidget.sendOTP(BuildConfig.MSG91_WIDGET_ID, BuildConfig.MSG91_WIDGET_TOKEN, "91$normalizedPhone")
                }
                val text = widgetResultText(raw)
                // MSG91 returns reqId separately from the human-readable message.
                // Never treat the message text as the request ID.
                val id = widgetField(text, "reqId", "reqid", "requestId", "request_id")
                if (id.isBlank()) {
                    error = widgetField(text, "error", "message").ifBlank { "Unable to send OTP" }
                } else {
                    reqId = id
                    otpMode = true
                }
            } catch (t: Throwable) {
                error = t.message ?: "Unable to send OTP"
            } finally {
                loading = false
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        if (adminVerified) {
            Text("AARVO Admin", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Role: ADMIN", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Admin account verified successfully. You have administrator access to AARVO.")
            Spacer(Modifier.height(20.dp))
            Button(onClick = openApp, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Continue to AARVO", fontWeight = FontWeight.Bold) }
        } else {
            Text("AARVO", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(20.dp))

            if (otpMode) {
                Text("Verify your mobile number", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("OTP sent to +91 $phone")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(otp, { otp = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("6-digit OTP") })
                if (error.isNotBlank()) { Spacer(Modifier.height(6.dp)); Text(error, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    loading = true; error = ""
                    scope.launch {
                        try {
                            val normalizedPhone = IndianPhoneValidator.isValidOrThrow(phone)
                            val raw = withContext(Dispatchers.IO) {
                                OTPWidget.verifyOTP(BuildConfig.MSG91_WIDGET_ID, BuildConfig.MSG91_WIDGET_TOKEN, reqId, otp)
                            }
                            val text = widgetResultText(raw)
                            val accessToken = widgetField(text, "access-token", "accessToken", "access_token", "token")
                            if (accessToken.isBlank()) {
                                error = widgetField(text, "error", "message").ifBlank { "Invalid OTP" }
                            } else {
                                val result = api.verifyMsg91AccessToken(normalizedPhone, accessToken)
                                saveSession(prefs, result)
                                val userRole = result.optJSONObject("user")?.optString("role", "BUYER")?.uppercase() ?: "BUYER"
                                if (userRole == "ADMIN") adminVerified = true else openApp()
                            }
                        } catch (t: Throwable) { error = t.message ?: "OTP verification failed" }
                        finally { loading = false }
                    }
                }, enabled = !loading && otp.length == 6 && reqId.isNotBlank(), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    if (loading) CircularProgressIndicator() else Text("Verify & Continue", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = {
                    if (!loading && reqId.isNotBlank()) scope.launch {
                        loading = true; error = ""
                        try {
                            val raw = withContext(Dispatchers.IO) {
                                OTPWidget.retryOTP(BuildConfig.MSG91_WIDGET_ID, BuildConfig.MSG91_WIDGET_TOKEN, reqId, 11)
                            }
                            val text = widgetResultText(raw)
                            val newReqId = widgetField(text, "reqId", "reqid", "requestId", "request_id")
                            if (newReqId.isNotBlank()) reqId = newReqId
                            else error = widgetField(text, "error", "message").ifBlank { "Unable to resend OTP" }
                        } catch (t: Throwable) { error = t.message ?: "Unable to resend OTP" }
                        finally { loading = false }
                    }
                }) { Text("Resend OTP") }
            } else {
                Text(if (registerMode) "Create your AARVO account" else "Login with mobile OTP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                if (registerMode) {
                    OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Full name") })
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(10) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Mobile number") }, prefix = { Text("+91  ") })
                Spacer(Modifier.height(8.dp))

                if (registerMode) {
                    OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Create password (8+ characters)") })
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Email (optional)") })
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { seller = !seller }) { Text(if (seller) "✓ Register as seller" else "Register as buyer") }
                } else {
                    Text("Password ki zarurat nahi hai. Mobile number par OTP se direct login hoga.", style = MaterialTheme.typography.bodyMedium)
                }

                if (error.isNotBlank()) { Spacer(Modifier.height(4.dp)); Text(error, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(12.dp))
                Button(onClick = { sendWidgetOtp() }, enabled = !loading && IndianPhoneValidator.isValid(phone) && (!registerMode || (name.isNotBlank() && password.length >= 8)), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    if (loading) CircularProgressIndicator() else Text(if (registerMode) "Create account & verify OTP" else "Send OTP & Login", fontWeight = FontWeight.Bold)
                }

                if (!registerMode) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { registerMode = true; error = "" }, modifier = Modifier.fillMaxWidth()) { Text("New to AARVO? Create account") }
                    TextButton(onClick = {
                        prefs.edit().putBoolean("signed_in", false).putBoolean("guest_mode", true).putString("user_name", "Guest").putString("user_role", "BUYER").remove("auth_token").apply()
                        openApp()
                    }, modifier = Modifier.fillMaxWidth()) { Text("Continue as Guest", fontWeight = FontWeight.Bold) }
                } else {
                    TextButton(onClick = { registerMode = false; error = "" }) { Text("Already have an account? Login with OTP") }
                }
            }
            if (!api.isConfigured()) Text("Live API is not configured in this build.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun saveSession(prefs: android.content.SharedPreferences, result: JSONObject) {
    val user = result.getJSONObject("user")
    prefs.edit().putBoolean("signed_in", true).putBoolean("guest_mode", false).putString("user_name", user.optString("display_name", "AARVO User")).putString("user_role", user.optString("role", "BUYER")).putString("auth_token", result.getString("token")).apply()
}
