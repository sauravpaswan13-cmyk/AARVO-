package com.aarvo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.aarvo.network.Msg91ServerApi
import com.msg91.sendotp.OTPWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

class PhoneAuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PhoneAuthScreen() }
    }

    private fun parse(raw: String): Any? = runCatching {
        JSONTokener(raw.trim()).nextValue()
    }.getOrNull()

    private fun looksLikeRequestId(v: String): Boolean =
        v.length in 8..256 &&
            v.count { it == '.' } != 2 &&
            v.matches(Regex("[A-Za-z0-9_-]+"))

    private fun requestId(raw: String): String? {
        fun scan(value: Any?): String? {
            when (value) {
                is JSONObject -> {
                    val keys = value.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        if (key.equals("reqId", true) ||
                            key.equals("requestId", true) ||
                            key.equals("request_id", true)
                        ) {
                            val candidate = value.optString(key).trim()
                            if (looksLikeRequestId(candidate)) return candidate
                        }
                    }

                    val nestedKeys = value.keys()
                    while (nestedKeys.hasNext()) {
                        val nested = scan(value.opt(nestedKeys.next()))
                        if (nested != null) return nested
                    }
                }

                is JSONArray -> {
                    for (i in 0 until value.length()) {
                        val nested = scan(value.opt(i))
                        if (nested != null) return nested
                    }
                }

                is String -> {
                    val candidate = value.trim()
                    if (looksLikeRequestId(candidate)) return candidate
                    val parsed = parse(candidate)
                    if (parsed != null && parsed !is String) return scan(parsed)
                }
            }
            return null
        }

        return scan(parse(raw))
    }

    private fun isError(raw: String): Boolean {
        val value = parse(raw)

        fun bad(item: Any?): Boolean = when (item) {
            is JSONObject -> {
                val type = item.optString("type").lowercase()
                val status = item.optString("status").lowercase()
                val message = item.optString("message").lowercase()
                setOf("false", "0", "failed", "failure", "error", "invalid", "rejected").contains(type) ||
                    setOf("false", "0", "failed", "failure", "error", "invalid", "rejected").contains(status) ||
                    message.contains("authentication failure") ||
                    message.contains("invalid otp")
            }
            is JSONArray -> (0 until item.length()).any { bad(item.opt(it)) }
            else -> false
        }

        return bad(value) || (value == null && raw.lowercase().contains("error"))
    }

    private fun saveSession(token: String, phone: String, role: String) {
        getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE).edit()
            .putBoolean("signed_in", true)
            .putBoolean("guest_mode", false)
            .putString("user_name", phone)
            .putString("user_role", role)
            .putString("auth_token", token)
            .commit()
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    @Composable
    private fun PhoneAuthScreen() {
        val scope = rememberCoroutineScope()
        var phone by remember { mutableStateOf("") }
        var otp by remember { mutableStateOf("") }
        var reqId by remember { mutableStateOf("") }
        var otpMode by remember { mutableStateOf(false) }
        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf("") }
        val widgetId = BuildConfig.MSG91_WIDGET_ID
        val widgetToken = BuildConfig.MSG91_WIDGET_TOKEN
        val server = remember { Msg91ServerApi() }

        suspend fun verifyLogin() {
            val normalized = phone.filter(Char::isDigit).takeLast(10)
            val session = withTimeout(20000) {
                server.verifyOtp(normalized, reqId, otp, "BUYER")
            }
            val token = session.optString("token").trim()
            if (token.isBlank()) {
                throw IllegalStateException("AARVO server did not return a login token.")
            }
            val role = session.optJSONObject("user")?.optString("role", "BUYER")?.uppercase() ?: "BUYER"
            saveSession(token, normalized, role)
            openMain()
        }

        fun sendOtp() {
            error = ""
            otp = ""
            reqId = ""
            loading = true
            otpMode = false
            scope.launch {
                try {
                    val normalized = phone.filter(Char::isDigit).takeLast(10)
                    if (normalized.length != 10) {
                        throw IllegalArgumentException("Enter a valid 10-digit mobile number.")
                    }
                    if (widgetId.isBlank() || widgetToken.isBlank()) {
                        throw IllegalStateException("MSG91 OTP is not configured in this build.")
                    }
                    val result = withTimeout(15000) {
                        runInterruptible(Dispatchers.IO) {
                            OTPWidget.sendOTP(widgetId, widgetToken, "91$normalized")
                        }
                    }
                    if (isError(result)) throw IllegalStateException(result)
                    val id = requestId(result).orEmpty()
                    if (id.isBlank()) {
                        throw IllegalStateException("MSG91 did not return a request ID. Please try Send OTP again.")
                    }
                    reqId = id
                    otpMode = true
                } catch (t: Throwable) {
                    otpMode = false
                    error = t.message ?: "Unable to send OTP. Please try again."
                } finally {
                    loading = false
                }
            }
        }

        fun resend() {
            if (reqId.isBlank() || loading) return
            loading = true
            error = ""
            scope.launch {
                try {
                    val result = withTimeout(15000) {
                        runInterruptible(Dispatchers.IO) {
                            OTPWidget.retryOTP(widgetId, widgetToken, reqId, 11)
                        }
                    }
                    if (isError(result)) throw IllegalStateException(result)
                    requestId(result)?.let { reqId = it }
                } catch (t: Throwable) {
                    error = t.message ?: "Unable to resend OTP."
                } finally {
                    loading = false
                }
            }
        }

        val purple = Color(0xFF4B16D8)
        val deepPurple = Color(0xFF32108E)
        val orange = Color(0xFFFF7A00)
        val page = Color(0xFFF7F5FF)
        val soft = Color(0xFFF0ECFF)

        Surface(Modifier.fillMaxSize(), color = page) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(orange, purple))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("AARVO", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = deepPurple, letterSpacing = 1.6.sp)
                        Text("Shop Smart • Live Better", fontSize = 11.sp, color = Color(0xFF77718B))
                    }
                }
                Spacer(Modifier.height(38.dp))
                Column(Modifier.fillMaxWidth()) {
                    Text(if (!otpMode) "Welcome back" else "Verify your mobile", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF171329))
                    Spacer(Modifier.height(7.dp))
                    Text(
                        if (!otpMode) "Login securely with your mobile number" else "Enter the OTP sent to your mobile number",
                        fontSize = 14.sp,
                        color = Color(0xFF706A80)
                    )
                }
                Spacer(Modifier.height(22.dp))
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(Modifier.padding(22.dp)) {
                        if (!otpMode) {
                            Text("Mobile number", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF28223B))
                            Spacer(Modifier.height(10.dp))
                            Row(
                                Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(18.dp))
                                    .background(soft).border(1.dp, purple.copy(alpha = .16f), RoundedCornerShape(18.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.width(82.dp).padding(start = 16.dp)) {
                                    Text("INDIA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF817A93))
                                    Text("+91", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = deepPurple)
                                }
                                Box(Modifier.width(1.dp).height(34.dp).background(Color(0xFFD8D1EE)))
                                Spacer(Modifier.width(8.dp))
                                OutlinedTextField(
                                    phone,
                                    { phone = it.filter(Char::isDigit).take(10) },
                                    placeholder = { Text("Enter 10-digit mobile number", color = Color(0xFF9A94A7), fontSize = 14.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = purple,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedTextColor = Color(0xFF211A32),
                                        unfocusedTextColor = Color(0xFF211A32),
                                        cursorColor = purple
                                    )
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            Button(
                                ::sendOtp,
                                enabled = !loading && phone.length == 10,
                                shape = RoundedCornerShape(17.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = purple, disabledContainerColor = Color(0xFFD8D2E7)),
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                                else Text("Continue", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }
                            Spacer(Modifier.height(14.dp))
                            Text("New to AARVO? Your account is created securely after verification.", fontSize = 11.sp, color = Color(0xFF827B90))
                        } else {
                            Text("One-time password", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF28223B))
                            Spacer(Modifier.height(10.dp))
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(soft).padding(horizontal = 4.dp)) {
                                OutlinedTextField(
                                    otp,
                                    { otp = it.filter(Char::isDigit).take(8) },
                                    placeholder = { Text("Enter OTP") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = purple,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedTextColor = Color(0xFF211A32),
                                        unfocusedTextColor = Color(0xFF211A32),
                                        cursorColor = purple
                                    )
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            Button(
                                {
                                    loading = true
                                    error = ""
                                    scope.launch {
                                        try {
                                            verifyLogin()
                                        } catch (t: Throwable) {
                                            error = t.message ?: "OTP verification failed."
                                        } finally {
                                            loading = false
                                        }
                                    }
                                },
                                enabled = !loading && reqId.isNotBlank() && otp.length in 4..8,
                                shape = RoundedCornerShape(17.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = purple, disabledContainerColor = Color(0xFFD8D2E7)),
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                                else Text("Verify & Login", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }
                            Spacer(Modifier.height(5.dp))
                            TextButton(::resend, enabled = !loading && reqId.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                                Text("Resend OTP", fontWeight = FontWeight.Bold, color = purple)
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✓ Secure", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5F586D))
                    Text("•", color = orange)
                    Text("✓ Fast", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5F586D))
                    Text("•", color = orange)
                    Text("✓ Trusted", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5F586D))
                }
                Spacer(Modifier.height(10.dp))
                Text("Your mobile number is securely protected", fontSize = 11.sp, color = Color(0xFF8A8495))
                if (error.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        }
    }
}
