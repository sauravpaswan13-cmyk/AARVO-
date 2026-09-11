package com.aarvo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { PhoneAuthScreen() } }
    private fun parse(raw: String): Any? = runCatching { JSONTokener(raw.trim()).nextValue() }.getOrNull()
    private fun looksLikeRequestId(v: String): Boolean = v.length in 8..256 && v.count { it == '.' } != 2 && v.matches(Regex("[A-Za-z0-9_-]+"))
    private fun requestId(raw: String): String? {
        fun scan(v: Any?): String? = when (v) {
            is JSONObject -> { val keys=v.keys(); while(keys.hasNext()){val k=keys.next(); if(k.equals("reqId",true)||k.equals("requestId",true)||k.equals("request_id",true)) v.optString(k).trim().takeIf(::looksLikeRequestId)?.let{return it};}; val nested=v.keys(); while(nested.hasNext()) scan(v.opt(nested.next()))?.let{return it}; null }
            is JSONArray -> { for(i in 0 until v.length()) scan(v.opt(i))?.let{return it}; null }
            is String -> v.trim().takeIf(::looksLikeRequestId) ?: runCatching{scan(parse(v))}.getOrNull()
            else -> null
        }; return scan(parse(raw))
    }
    private fun isError(raw: String): Boolean { val v=parse(raw); fun bad(x:Any?):Boolean=when(x){is JSONObject->{val type=x.optString("type").lowercase();val status=x.optString("status").lowercase();val msg=x.optString("message").lowercase();setOf("false","0","failed","failure","error","invalid","rejected").contains(type)||setOf("false","0","failed","failure","error","invalid","rejected").contains(status)||msg.contains("authentication failure")||msg.contains("invalid otp")};is JSONArray->(0 until x.length()).any{bad(x.opt(it))};else->false};return bad(v)||(v==null&&raw.lowercase().contains("error")) }
    private fun saveSession(token:String,phone:String,role:String){getSharedPreferences("aarvo_prefs",Context.MODE_PRIVATE).edit().putBoolean("signed_in",true).putBoolean("guest_mode",false).putString("user_name",phone).putString("user_role",role).putString("auth_token",token).commit()}
    private fun openMain(){startActivity(Intent(this,MainActivity::class.java).apply{flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK});finish()}

    @Composable private fun PhoneAuthScreen(){
        val scope=rememberCoroutineScope();var phone by remember{mutableStateOf("")};var otp by remember{mutableStateOf("")};var reqId by remember{mutableStateOf("")};var otpMode by remember{mutableStateOf(false)};var loading by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
        val widgetId=BuildConfig.MSG91_WIDGET_ID;val widgetToken=BuildConfig.MSG91_WIDGET_TOKEN;val server=remember{Msg91ServerApi()}
        suspend fun verifyLogin(){val normalized=phone.filter(Char::isDigit).takeLast(10);val session=withTimeout(20000){server.verifyOtp(normalized,reqId,otp,"BUYER")};val token=session.optString("token").trim();if(token.isBlank())throw IllegalStateException("AARVO server did not return a login token.");val role=session.optJSONObject("user")?.optString("role","BUYER")?.uppercase()?:"BUYER";saveSession(token,normalized,role);openMain()}
        fun sendOtp(){error="";otp="";reqId="";loading=true;otpMode=false;scope.launch{try{val normalized=phone.filter(Char::isDigit).takeLast(10);if(normalized.length!=10)throw IllegalArgumentException("Enter a valid 10-digit mobile number.");if(widgetId.isBlank()||widgetToken.isBlank())throw IllegalStateException("MSG91 OTP is not configured in this build.");val result=withTimeout(15000){runInterruptible(Dispatchers.IO){OTPWidget.sendOTP(widgetId,widgetToken,"91$normalized")}};if(isError(result))throw IllegalStateException(result);val id=requestId(result).orEmpty();if(id.isBlank())throw IllegalStateException("MSG91 did not return a request ID. Please try Send OTP again.");reqId=id;otpMode=true}catch(t:Throwable){otpMode=false;error=t.message?:"Unable to send OTP. Please try again."}finally{loading=false}}}
        fun resend(){if(reqId.isBlank()||loading)return;loading=true;error="";scope.launch{try{val result=withTimeout(15000){runInterruptible(Dispatchers.IO){OTPWidget.retryOTP(widgetId,widgetToken,reqId,11)}};if(isError(result))throw IllegalStateException(result);requestId(result)?.let{reqId=it}}catch(t:Throwable){error=t.message?:"Unable to resend OTP."}finally{loading=false}}}

        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){
            Column(Modifier.fillMaxSize().padding(horizontal=22.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){
                Text("AARVO",fontSize=34.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=2.sp,color=MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp));Text(if(!otpMode)"Welcome back" else "Verify your mobile",fontSize=25.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(5.dp));Text(if(!otpMode)"Sign in securely to continue shopping" else "Enter the verification code sent to your mobile",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=14.sp)
                Spacer(Modifier.height(24.dp))
                Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),elevation=CardDefaults.cardElevation(defaultElevation=5.dp)){
                    Column(Modifier.padding(20.dp)){if(!otpMode){Text("Mobile number",fontWeight=FontWeight.SemiBold,fontSize=14.sp);Spacer(Modifier.height(9.dp));Row(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant),verticalAlignment=Alignment.CenterVertically){Text("🇮🇳  +91",Modifier.padding(start=16.dp),fontWeight=FontWeight.SemiBold);VerticalDivider(Modifier.height(30.dp).padding(start=12.dp,end=12.dp));OutlinedTextField(phone,{phone=it.filter(Char::isDigit).take(10)},placeholder={Text("Enter mobile number")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone),modifier=Modifier.weight(1f),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=MaterialTheme.colorScheme.primary,unfocusedBorderColor=androidx.compose.ui.graphics.Color.Transparent,focusedContainerColor=androidx.compose.ui.graphics.Color.Transparent,unfocusedContainerColor=androidx.compose.ui.graphics.Color.Transparent))};Spacer(Modifier.height(16.dp));Button(::sendOtp,enabled=!loading&&phone.length==10,shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().height(52.dp)){if(loading)CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp)else Text("Continue",fontWeight=FontWeight.Bold,fontSize=16.sp)}}else{Text("One-time password",fontWeight=FontWeight.SemiBold,fontSize=14.sp);Spacer(Modifier.height(9.dp));OutlinedTextField(otp,{otp=it.filter(Char::isDigit).take(8)},placeholder={Text("Enter OTP")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth(),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=MaterialTheme.colorScheme.primary));Spacer(Modifier.height(16.dp));Button({loading=true;error="";scope.launch{try{verifyLogin()}catch(t:Throwable){error=t.message?:"OTP verification failed."}finally{loading=false}}},enabled=!loading&&reqId.isNotBlank()&&otp.length in 4..8,shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().height(52.dp)){if(loading)CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp)else Text("Verify & Login",fontWeight=FontWeight.Bold,fontSize=16.sp)}Spacer(Modifier.height(8.dp));TextButton(::resend,enabled=!loading&&reqId.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("Resend OTP")}}}
                }
                Spacer(Modifier.height(16.dp));Text("Your mobile number is securely protected",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(error.isNotBlank()){Spacer(Modifier.height(10.dp));Text(error,color=MaterialTheme.colorScheme.error,fontSize=13.sp)}
            }
        }
    }
}
