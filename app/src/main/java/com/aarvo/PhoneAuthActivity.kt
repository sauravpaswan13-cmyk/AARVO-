package com.aarvo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.msg91.sendotp.library.OTPWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

class PhoneAuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PhoneAuthScreen() }
    }

    private fun parseMsg91Result(raw: String): Any? =
        runCatching { JSONTokener(raw.trim()).nextValue() }.getOrNull()

    private fun looksLikeRequestId(value: String): Boolean {
        val v = value.trim()
        if (v.length < 8 || v.length > 256) return false
        if (v.count { it == '.' } == 2) return false
        return v.matches(Regex("[A-Za-z0-9_-]+"))
    }

    private fun msg91RequestId(raw: String): String? {
        fun scan(value: Any?): String? = when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key.equals("reqId", true) || key.equals("requestId", true) || key.equals("request_id", true)) {
                        value.optString(key).trim().takeIf { looksLikeRequestId(it) }?.let { return it }
                    }
                }
                // Compatibility with older MSG91 response shapes where the opaque request id
                // can be returned in "message".
                value.optString("message").trim().takeIf { looksLikeRequestId(it) }?.let { return it }
                val nestedKeys = value.keys()
                while (nestedKeys.hasNext()) {
                    val key = nestedKeys.next()
                    scan(value.opt(key))?.let { return it }
                }
                null
            }
            is JSONArray -> {
                for (i in 0 until value.length()) scan(value.opt(i))?.let { return it }
                null
            }
            is String -> {
                val nested = value.trim()
                if (looksLikeRequestId(nested)) nested
                else runCatching { scan(JSONTokener(nested).nextValue()) }.getOrNull()
            }
            else -> null
        }
        return scan(parseMsg91Result(raw))
    }

    private fun findMsg91AccessToken(raw: String): String? {
        fun scan(value: Any?): String? = when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val candidate = value.optString(key).trim()
                    if ((key.contains("token", true) || key.contains("access", true)) && candidate.count { it == '.' } == 2 && candidate.length > 80) return candidate
                    scan(value.opt(key))?.let { return it }
                }
                null
            }
            is JSONArray -> {
                for (i in 0 until value.length()) scan(value.opt(i))?.let { return it }
                null
            }
            is String -> runCatching { scan(JSONTokener(value.trim()).nextValue()) }.getOrNull()
            else -> null
        }
        return scan(parseMsg91Result(raw))
    }

    private fun msg91IsError(raw: String): Boolean {
        val lower = raw.lowercase()
        return lower.contains("error") && !lower.contains("success")
    }

    private fun saveSession(token: String, phone: String) {
        getSharedPreferences("aarvo_session", Context.MODE_PRIVATE).edit()
            .putBoolean("signed_in", true)
            .putBoolean("guest_mode", false)
            .putString("user_name", phone)
            .putString("role", "user")
            .putString("auth_token", token)
            .apply()
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

        fun sendPhoneOtp() {
            error = ""
            otp = ""
            reqId = ""
            loading = true
            // Important: do not show OTP UI until MSG91 has returned a usable request id.
            otpMode = false
            scope.launch {
                try {
                    val normalizedPhone = phone.filter(Char::isDigit).takeLast(10)
                    if (normalizedPhone.length != 10) throw IllegalArgumentException("Enter a valid 10-digit mobile number.")
                    if (widgetId.isBlank() || widgetToken.isBlank()) throw IllegalStateException("MSG91 OTP is not configured in this build.")
                    val identifier = "91$normalizedPhone"
                    val result = withContext(Dispatchers.IO) {
                        OTPWidget.sendOTP(widgetId, widgetToken, identifier)
                    }
                    if (msg91IsError(result)) throw IllegalStateException(result)
                    val immediateAccessToken = findMsg91AccessToken(result)
                    if (!immediateAccessToken.isNullOrBlank()) {
                        saveSession(immediateAccessToken, normalizedPhone)
                        startActivity(Intent(this@PhoneAuthActivity, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
                        return@launch
                    }
                    val returnedReqId = msg91RequestId(result).orEmpty()
                    if (returnedReqId.isBlank()) throw IllegalStateException("MSG91 did not return a request ID. Please resend OTP.")
                    reqId = returnedReqId
                    otpMode = true
                } catch (t: Throwable) {
                    otpMode = false
                    error = t.message ?: "Unable to send OTP. Please try again."
                } finally {
                    loading = false
                }
            }
        }

        fun verifyWidgetOtp() {
            if (loading || reqId.isBlank() || otp.length != 6) return
            error = ""
            loading = true
            scope.launch {
                try {
                    val normalizedPhone = phone.filter(Char::isDigit).takeLast(10)
                    val result = withContext(Dispatchers.IO) {
                        OTPWidget.verifyOTP(widgetId, widgetToken, reqId, otp)
                    }
                    if (msg91IsError(result)) throw IllegalStateException(result)
                    val accessToken = findMsg91AccessToken(result)
                        ?: throw IllegalStateException("MSG91 verification succeeded without an access token. Please try again.")
                    val session = withContext(Dispatchers.IO) {
                        ApiClient.verifyMsg91AccessToken(normalizedPhone, accessToken)
                    }
                    saveSession(session.token, normalizedPhone)
                    startActivity(Intent(this@PhoneAuthActivity, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
                } catch (t: Throwable) {
                    error = t.message ?: "OTP verification failed."
                } finally {
                    loading = false
                }
            }
        }

        fun retryWidgetOtp() {
            if (reqId.isBlank() || loading) return
            error = ""
            loading = true
            scope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        OTPWidget.retryOTP(widgetId, widgetToken, reqId, 11)
                    }
                    if (msg91IsError(result)) throw IllegalStateException(result)
                    msg91RequestId(result)?.let { reqId = it }
                } catch (t: Throwable) {
                    error = t.message ?: "Unable to resend OTP."
                } finally {
                    loading = false
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("AARVO Login", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(18.dp))
            if (!otpMode) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter(Char::isDigit).take(10) },
                    label = { Text("Mobile number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = ::sendPhoneOtp, enabled = !loading && phone.filter(Char::isDigit).length == 10, modifier = Modifier.fillMaxWidth()) {
                    if (loading) CircularProgressIndicator() else Text("Send OTP")
                }
            } else {
                Text("Enter the 6-digit OTP")
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = otp,
                    onValueChange = { otp = it.filter(Char::isDigit).take(6) },
                    label = { Text("OTP") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = ::verifyWidgetOtp, enabled = !loading && reqId.isNotBlank() && otp.length == 6, modifier = Modifier.fillMaxWidth()) {
                    if (loading) CircularProgressIndicator() else Text("Verify & Login")
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = ::retryWidgetOtp, enabled = !loading && reqId.isNotBlank()) { Text("Resend OTP") }
                }
            }
            if (error.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
