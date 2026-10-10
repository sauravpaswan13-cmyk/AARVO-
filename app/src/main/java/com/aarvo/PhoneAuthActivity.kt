package com.aarvo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
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
        val prefillPhone = intent.getStringExtra("prefill_phone").orEmpty()
        setContent { PhoneAuthScreen(prefillPhone) }
    }

    private fun parse(raw: String): Any? = runCatching { JSONTokener(raw.trim()).nextValue() }.getOrNull()

    private fun looksLikeRequestId(v: String): Boolean =
        v.length in 8..256 && v.count { it == '.' } != 2 && v.matches(Regex("[A-Za-z0-9_-]+"))

    private fun requestId(raw: String): String? {
        fun scan(value: Any?): String? {
            when (value) {
                is JSONObject -> {
                    val keys = value.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        if (key.equals("reqId", true) || key.equals("requestId", true) || key.equals("request_id", true)) {
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
                is JSONArray -> for (i in 0 until value.length()) scan(value.opt(i))?.let { return it }
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
                    message.contains("authentication failure") || message.contains("invalid otp")
            }
            is JSONArray -> (0 until item.length()).any { bad(item.opt(it)) }
            else -> false
        }
        return bad(value) || (value == null && raw.lowercase().contains("error"))
    }

    private fun saveSession(token: String, phone: String, role: String) {
        getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE).edit()
            .putBoolean("onboarded", true).putBoolean("signed_in", true).putBoolean("guest_mode", false)
            .putString("user_name", phone).putString("user_role", role)
            .putString("auth_token", token).commit()
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    @Composable
    private fun PhoneAuthScreen(prefillPhone: String) {
        val scope = rememberCoroutineScope()
        val initialPhone = prefillPhone.filter(Char::isDigit).take(10)
        var phone by remember { mutableStateOf(initialPhone) }
        var otp by remember { mutableStateOf("") }
        var reqId by remember { mutableStateOf("") }
        var otpMode by remember { mutableStateOf(initialPhone.length == 10) }
        var loading by remember { mutableStateOf(initialPhone.length == 10) }
        var error by remember { mutableStateOf("") }
        val widgetId = BuildConfig.MSG91_WIDGET_ID
        val widgetToken = BuildConfig.MSG91_WIDGET_TOKEN
        val server = remember { Msg91ServerApi() }

        suspend fun verifyLogin() {
            val normalized = phone.filter(Char::isDigit).takeLast(10)
            val session = withTimeout(20000) { server.verifyOtp(normalized, reqId, otp, "BUYER") }
            val token = session.optString("token").trim()
            if (token.isBlank()) throw IllegalStateException("AARVO server did not return a login token.")
            val role = session.optJSONObject("user")?.optString("role", "BUYER")?.uppercase() ?: "BUYER"
            saveSession(token, normalized, role)
            openMain()
        }

        fun sendOtp() {
            error = ""; otp = ""; reqId = ""; loading = true
            if (initialPhone.length != 10) otpMode = false
            scope.launch {
                try {
                    val normalized = phone.filter(Char::isDigit).take(10)
                    if (normalized.length != 10) throw IllegalArgumentException("Enter a valid 10-digit mobile number.")
                    if (widgetId.isBlank() || widgetToken.isBlank()) throw IllegalStateException("MSG91 OTP is not configured in this build.")
                    val result = withTimeout(15000) {
                        runInterruptible(Dispatchers.IO) { OTPWidget.sendOTP(widgetId, widgetToken, "91$normalized") }
                    }
                    if (isError(result)) throw IllegalStateException(result)
                    val id = requestId(result).orEmpty()
                    if (id.isBlank()) throw IllegalStateException("MSG91 did not return a request ID. Please try again.")
                    reqId = id; otpMode = true
                } catch (t: Throwable) {
                    otpMode = true
                    val message = t.message.orEmpty()
                    error = if (message.contains("IPBlocked", true) || message.contains("IP Block", true)) {
                        "OTP service has temporarily blocked this network after repeated requests. Please wait and try again; no new OTP request will be sent automatically."
                    } else message.ifBlank { "Unable to send OTP. Please try again." }
                } finally { loading = false }
            }
        }

        fun resend() {
            if (reqId.isBlank() || loading) return
            loading = true; error = ""
            scope.launch {
                try {
                    val result = withTimeout(15000) {
                        runInterruptible(Dispatchers.IO) { OTPWidget.retryOTP(widgetId, widgetToken, reqId, 11) }
                    }
                    if (isError(result)) throw IllegalStateException(result)
                    requestId(result)?.let { reqId = it }
                } catch (t: Throwable) {
                    val message = t.message.orEmpty()
                    error = if (message.contains("IPBlocked", true) || message.contains("IP Block", true)) {
                        "OTP service has temporarily blocked this network. Please wait before requesting another OTP."
                    } else message.ifBlank { "Unable to resend OTP." }
                }
                finally { loading = false }
            }
        }

        LaunchedEffect(initialPhone) {
            if (initialPhone.length == 10 && reqId.isBlank()) sendOtp()
        }

        val purple = Color(AarvoScreenDesign.authPurple)
        val deepPurple = Color(AarvoScreenDesign.authDeepPurple)
        val orange = Color(AarvoScreenDesign.authOrange)
        val page = Color(AarvoScreenDesign.authPageBackground)
        val soft = Color(AarvoScreenDesign.authSoftBackground)

        Surface(Modifier.fillMaxSize(), color = page) {
            Column(Modifier.fillMaxSize().padding(horizontal = AarvoScreenDesign.AUTH_PAGE_HORIZONTAL_PADDING_DP.dp, vertical = AarvoScreenDesign.AUTH_PAGE_VERTICAL_PADDING_DP.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.aarvo_top_logo),
                        contentDescription = "AARVO logo",
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier.size(AarvoScreenDesign.AUTH_LOGO_SIZE_DP.dp)
                    )
                    Spacer(Modifier.width(AarvoScreenDesign.AUTH_LOGO_TEXT_GAP_DP.dp))
                    Column {
                        Text("AARVO", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = deepPurple, letterSpacing = 1.6.sp)
                        Text("Shop Smart • Live Better", fontSize = 11.sp, color = Color(AarvoScreenDesign.authBrandMuted))
                    }
                }
                Spacer(Modifier.height(AarvoScreenDesign.AUTH_HEADER_TOP_GAP_DP.dp))
                Column(Modifier.fillMaxWidth()) {
                    Text(if (!otpMode) "Welcome back" else "Verify your mobile", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color(AarvoScreenDesign.authTextPrimary))
                    Spacer(Modifier.height(7.dp))
                    Text(if (!otpMode) "Login securely with your mobile number" else "Enter the OTP sent to your mobile number", fontSize = 14.sp, color = Color(AarvoScreenDesign.authTextSecondary))
                }
                Spacer(Modifier.height(AarvoScreenDesign.AUTH_CARD_TOP_GAP_DP.dp))
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(AarvoScreenDesign.AUTH_CARD_CORNER_DP.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
                    Column(Modifier.padding(AarvoScreenDesign.AUTH_CARD_PADDING_DP.dp)) {
                        if (!otpMode) {
                            Text("Mobile number", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(AarvoScreenDesign.authFieldLabel)); Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(18.dp)).background(soft).border(1.dp, purple.copy(alpha = .16f), RoundedCornerShape(18.dp)), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.width(82.dp).padding(start = 16.dp)) { Text("INDIA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(AarvoScreenDesign.authLabelMuted)); Text("+91", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = deepPurple) }
                                Box(Modifier.width(1.dp).height(34.dp).background(Color(AarvoScreenDesign.authDivider))); Spacer(Modifier.width(8.dp))
                                OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(10) }, placeholder = { Text("Enter 10-digit mobile number", color = Color(AarvoScreenDesign.authPlaceholder), fontSize = 14.sp) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = purple, unfocusedBorderColor = Color.Transparent, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color(AarvoScreenDesign.authFieldText), unfocusedTextColor = Color(AarvoScreenDesign.authFieldText), cursorColor = purple))
                            }
                            Spacer(Modifier.height(18.dp))
                            Button(::sendOtp, enabled = !loading && phone.length == 10, shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = purple, disabledContainerColor = Color(AarvoScreenDesign.authDisabled)), modifier = Modifier.fillMaxWidth().height(AarvoScreenDesign.AUTH_BUTTON_HEIGHT_DP.dp)) {
                                if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White) else Text("Continue", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }
                            Spacer(Modifier.height(14.dp)); Text("New to AARVO? Your account is created securely after verification.", fontSize = 11.sp, color = Color(AarvoScreenDesign.authNote))
                        } else {
                            Text("One-time password", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(AarvoScreenDesign.authFieldLabel)); Spacer(Modifier.height(10.dp))
                            Text("+91 $phone", fontSize = 13.sp, color = Color(AarvoScreenDesign.authTextSecondary), fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(10.dp))
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(soft).padding(horizontal = 4.dp)) {
                                OutlinedTextField(otp, { otp = it.filter(Char::isDigit).take(8) }, placeholder = { Text("Enter OTP") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = purple, unfocusedBorderColor = Color.Transparent, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color(AarvoScreenDesign.authFieldText), unfocusedTextColor = Color(AarvoScreenDesign.authFieldText), cursorColor = purple))
                            }
                            Spacer(Modifier.height(18.dp))
                            Button({ loading = true; error = ""; scope.launch { try { verifyLogin() } catch (t: Throwable) { error = t.message ?: "OTP verification failed." } finally { loading = false } } }, enabled = !loading && reqId.isNotBlank() && otp.length in 4..8, shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = purple, disabledContainerColor = Color(AarvoScreenDesign.authDisabled)), modifier = Modifier.fillMaxWidth().height(AarvoScreenDesign.AUTH_BUTTON_HEIGHT_DP.dp)) {
                                if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White) else Text("Verify & Login", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }
                            Spacer(Modifier.height(5.dp)); TextButton(::resend, enabled = !loading && reqId.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Resend OTP", fontWeight = FontWeight.Bold, color = purple) }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) { Text("✓ Secure", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(AarvoScreenDesign.authFooter)); Text("•", color = orange); Text("✓ Fast", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(AarvoScreenDesign.authFooter)); Text("•", color = orange); Text("✓ Trusted", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(AarvoScreenDesign.authFooter)) }
                Spacer(Modifier.height(10.dp)); Text("Your mobile number is securely protected", fontSize = 11.sp, color = Color(AarvoScreenDesign.authFooterMuted))
                if (error.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        }
    }
}
