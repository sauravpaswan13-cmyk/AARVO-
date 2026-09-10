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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aarvo.network.AarvoApiClient
import com.msg91.sendotp.OTPWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

class PhoneAuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PhoneAuthScreen() }
    }

    private fun parseMsg91Result(raw: String): Any? = runCatching { JSONTokener(raw.trim()).nextValue() }.getOrNull()

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
                if (looksLikeRequestId(nested)) nested else runCatching { scan(JSONTokener(nested).nextValue()) }.getOrNull()
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
                    if (key.equals("access-token", true) || key.equals("access_token", true) || key.equals("accessToken", true)) {
                        if (candidate.isNotBlank()) return candidate
                    }
                }
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
            is String -> runCatching { scan(JSONTokener(value.trim()).nextValue()) }.getOrNull()
            else -> null
        }
        return scan(parseMsg91Result(raw))
    }

    private fun msg91IsError(raw: String): Boolean {
        val parsed = parseMsg91Result(raw)
        fun bad(value: Any?): Boolean = when (value) {
            is JSONObject -> {
                val type = value.optString("type").trim().lowercase()
                val status = value.optString("status").trim().lowercase()
                val message = value.optString("message").trim().lowercase()
                val badValues = setOf("false", "0", "failed", "failure", "error", "invalid", "rejected")
                badValues.contains(type) || badValues.contains(status) || message.contains("authentication failure") || message.contains("invalid otp")
            }
            is JSONArray -> (0 until value.length()).any { bad(value.opt(it)) }
            else -> false
        }
        return bad(parsed) || (parsed == null && raw.lowercase().contains("error"))
    }

    private fun saveSession(token: String, phone: String) {
        getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE).edit()
            .putBoolean("signed_in", true)
            .putBoolean("guest_mode", false)
            .putString("user_name", phone)
            .putString("user_role", "BUYER")
            .putString("auth_token", token)
            .commit()
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
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
        val api = remember { AarvoApiClient { getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE).getString("auth_token", null) } }

        suspend fun finishMsg91Login(normalizedPhone: String, accessToken: String) {
            val session = withContext(Dispatchers.IO) {
                withTimeout(15000) { api.verifyMsg91AccessToken(normalizedPhone, accessToken.trim()) }
            }
            val sessionToken = session.optString("token").trim()
            if (sessionToken.isBlank()) throw IllegalStateException("AARVO server did not return a login token.")
            saveSession(sessionToken, normalizedPhone)
            openMain()
        }

        fun sendPhoneOtp() {
            error = ""
            otp = ""
            reqId = ""
            loading = true
            otpMode = false
            scope.launch {
                try {
                    val normalizedPhone = phone.filter(Char::isDigit).takeLast(10)
                    if (normalizedPhone.length != 10) throw IllegalArgumentException("Enter a valid 10-digit mobile number.")
                    if (widgetId.isBlank() || widgetToken.isBlank()) throw IllegalStateException("MSG91 OTP is not configured in this build.")

                    val result = withTimeout(15000) {
                        runInterruptible(Dispatchers.IO) { OTPWidget.sendOTP(widgetId, widgetToken, "91$normalizedPhone") }
                    }
                    if (msg91IsError(result)) throw IllegalStateException(result)
                    val returnedReqId = msg91RequestId(result).orEmpty()
                    if (returnedReqId.isBlank()) throw IllegalStateException("MSG91 did not return a request ID. Please try Send OTP again.")
                    reqId = returnedReqId
                    otpMode = true
                } catch (t: Throwable) {
                    otpMode = false
                    error = if (t is kotlinx.coroutines.TimeoutCancellationException) "MSG91 OTP request timed out. Please try again." else t.message ?: "Unable to send OTP. Please try again."
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
                    val result = withTimeout(15000) {
                        runInterruptible(Dispatchers.IO) { OTPWidget.verifyOTP(widgetId, widgetToken, reqId, otp) }
                    }
                    if (msg91IsError(result)) throw IllegalStateException(result)
                    val accessToken = findMsg91AccessToken(result)
                        ?: throw IllegalStateException("MSG91 verification succeeded without an access token. Please try again.")
                    finishMsg91Login(normalizedPhone, accessToken)
                } catch (t: Throwable) {
                    error = when (t) {
                        is kotlinx.coroutines.TimeoutCancellationException -> "MSG91 verification timed out. Please try Verify OTP again."
                        else -> t.message ?: "OTP verification failed."
                    }
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
                    val result = withTimeout(15000) {
                        runInterruptible(Dispatchers.IO) { OTPWidget.retryOTP(widgetId, widgetToken, reqId, 11) }
                    }
                    if (msg91IsError(result)) throw IllegalStateException(result)
                    msg91RequestId(result)?.let { reqId = it }
                } catch (t: Throwable) {
                    error = if (t is kotlinx.coroutines.TimeoutCancellationException) "MSG91 resend timed out. Please try again." else t.message ?: "Unable to resend OTP."
                } finally {
                    loading = false
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("AARVO Login", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(18.dp))
            if (!otpMode) {
                OutlinedTextField(value = phone, onValueChange = { phone = it.filter(Char::isDigit).take(10) }, label = { Text("Mobile number") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Button(onClick = ::sendPhoneOtp, enabled = !loading && phone.filter(Char::isDigit).length == 10, modifier = Modifier.fillMaxWidth()) { if (loading) CircularProgressIndicator() else Text("Send OTP") }
            } else {
                Text("Enter the 6-digit OTP")
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = otp, onValueChange = { otp = it.filter(Char::isDigit).take(6) }, label = { Text("OTP") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Button(onClick = ::verifyWidgetOtp, enabled = !loading && reqId.isNotBlank() && otp.length == 6, modifier = Modifier.fillMaxWidth()) { if (loading) CircularProgressIndicator() else Text("Verify OTP & Login") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = ::retryWidgetOtp, enabled = !loading && reqId.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Resend OTP") }
            }
            if (error.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
