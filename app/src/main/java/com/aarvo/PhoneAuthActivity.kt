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
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { PhoneAuthScreen() } }

    private fun parse(raw: String): Any? = runCatching { JSONTokener(raw.trim()).nextValue() }.getOrNull()
    private fun looksLikeRequestId(v: String): Boolean = v.length in 8..256 && v.count { it == '.' } != 2 && v.matches(Regex("[A-Za-z0-9_-]+"))
    private fun requestId(raw: String): String? {
        fun scan(v: Any?): String? = when (v) {
            is JSONObject -> { val keys = v.keys(); while (keys.hasNext()) { val k = keys.next(); if (k.equals("reqId", true) || k.equals("requestId", true) || k.equals("request_id", true)) v.optString(k).trim().takeIf(::looksLikeRequestId)?.let { return it } }; val nested = v.keys(); while (nested.hasNext()) scan(v.opt(nested.next()))?.let { return it }; null }
            is JSONArray -> (0 until v.length()).forEach { scan(v.opt(it))?.let { return it } }.let { null }
            is String -> v.trim().takeIf(::looksLikeRequestId) ?: runCatching { scan(parse(v)) }.getOrNull()
            else -> null
        }
        return scan(parse(raw))
    }
    private fun isError(raw: String): Boolean {
        val v = parse(raw)
        fun bad(x: Any?): Boolean = when (x) {
            is JSONObject -> { val type=x.optString("type").lowercase(); val status=x.optString("status").lowercase(); val msg=x.optString("message").lowercase(); setOf("false","0","failed","failure","error","invalid","rejected").contains(type) || setOf("false","0","failed","failure","error","invalid","rejected").contains(status) || msg.contains("authentication failure") || msg.contains("invalid otp") }
            is JSONArray -> (0 until x.length()).any { bad(x.opt(it)) }
            else -> false
        }
        return bad(v) || (v == null && raw.lowercase().contains("error"))
    }
    private fun saveSession(token: String, phone: String, role: String) { getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE).edit().putBoolean("signed_in", true).putBoolean("guest_mode", false).putString("user_name", phone).putString("user_role", role).putString("auth_token", token).commit() }
    private fun openMain() { startActivity(Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }); finish() }

    @Composable private fun PhoneAuthScreen() {
        val scope = rememberCoroutineScope(); var phone by remember { mutableStateOf("") }; var otp by remember { mutableStateOf("") }; var reqId by remember { mutableStateOf("") }; var otpMode by remember { mutableStateOf(false) }; var loading by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }
        val widgetId = BuildConfig.MSG91_WIDGET_ID; val widgetToken = BuildConfig.MSG91_WIDGET_TOKEN; val server = remember { Msg91ServerApi() }

        suspend fun verifyLogin() {
            val normalized = phone.filter(Char::isDigit).takeLast(10)
            val session = withTimeout(20000) { server.verifyOtp(normalized, reqId, otp, "BUYER") }
            val token = session.optString("token").trim(); if (token.isBlank()) throw IllegalStateException("AARVO server did not return a login token.")
            val role = session.optJSONObject("user")?.optString("role", "BUYER")?.uppercase() ?: "BUYER"
            saveSession(token, normalized, role); openMain()
        }
        fun sendOtp() { error=""; otp=""; reqId=""; loading=true; otpMode=false; scope.launch { try { val normalized=phone.filter(Char::isDigit).takeLast(10); if(normalized.length!=10) throw IllegalArgumentException("Enter a valid 10-digit mobile number."); if(widgetId.isBlank()||widgetToken.isBlank()) throw IllegalStateException("MSG91 OTP is not configured in this build."); val result=withTimeout(15000){runInterruptible(Dispatchers.IO){OTPWidget.sendOTP(widgetId,widgetToken,"91$normalized")}}; if(isError(result)) throw IllegalStateException(result); val id=requestId(result).orEmpty(); if(id.isBlank()) throw IllegalStateException("MSG91 did not return a request ID. Please try Send OTP again."); reqId=id; otpMode=true } catch(t:Throwable){otpMode=false; error=t.message ?: "Unable to send OTP. Please try again."} finally{loading=false} } }
        fun resend() { if(reqId.isBlank()||loading)return; loading=true; error=""; scope.launch { try { val result=withTimeout(15000){runInterruptible(Dispatchers.IO){OTPWidget.retryOTP(widgetId,widgetToken,reqId,11)}}; if(isError(result)) throw IllegalStateException(result); requestId(result)?.let{reqId=it} } catch(t:Throwable){error=t.message ?: "Unable to resend OTP."} finally{loading=false} } }

        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement=Arrangement.Center) {
            Text("AARVO Login", style=MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(8.dp))
            if(!otpMode){ Text("Login securely with your mobile number."); Spacer(Modifier.height(18.dp)); OutlinedTextField(phone,{phone=it.filter(Char::isDigit).take(10)},label={Text("Mobile number")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone),modifier=Modifier.fillMaxWidth()); Spacer(Modifier.height(12.dp)); Button(::sendOtp,enabled=!loading&&phone.length==10,modifier=Modifier.fillMaxWidth()){if(loading)CircularProgressIndicator() else Text("Send OTP")} }
            else { Text("Enter the OTP sent to your mobile number."); Spacer(Modifier.height(10.dp)); OutlinedTextField(otp,{otp=it.filter(Char::isDigit).take(8)},label={Text("OTP")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth()); Spacer(Modifier.height(12.dp)); Button({loading=true;error="";scope.launch{try{verifyLogin()}catch(t:Throwable){error=t.message ?: "OTP verification failed."}finally{loading=false}}},enabled=!loading&&reqId.isNotBlank()&&otp.length in 4..8,modifier=Modifier.fillMaxWidth()){if(loading)CircularProgressIndicator() else Text("Verify OTP & Login")}; Spacer(Modifier.height(8.dp)); Button(::resend,enabled=!loading&&reqId.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("Resend OTP")} }
            if(error.isNotBlank()){Spacer(Modifier.height(12.dp));Text(error,color=MaterialTheme.colorScheme.error)}
        }
    }
}
