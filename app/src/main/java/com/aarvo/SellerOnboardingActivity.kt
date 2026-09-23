package com.aarvo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aarvo.network.AarvoApiClient
import com.aarvo.ui.theme.AarvoTheme
import kotlinx.coroutines.launch
import org.json.JSONObject

class SellerOnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE)
        val api = AarvoApiClient { prefs.getString("auth_token", null) }
        setContent { AarvoTheme { SellerOnboardingScreen(api, ::finish) } }
    }
}

@Composable
private fun SellerOnboardingScreen(api: AarvoApiClient, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var businessType by remember { mutableStateOf("") }
    var businessCategory by remember { mutableStateOf("") }
    var businessEmail by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var gstRegistered by remember { mutableStateOf(false) }
    var accountHolder by remember { mutableStateOf("") }
    var bankAccount by remember { mutableStateOf("") }
    var ifsc by remember { mutableStateOf("") }
    var pickupAddress by remember { mutableStateOf("") }
    var pickupCity by remember { mutableStateOf("") }
    var pickupState by remember { mutableStateOf("") }
    var pickupPostal by remember { mutableStateOf("") }
    var returnDays by remember { mutableStateOf("7") }
    var shippingModel by remember { mutableStateOf("SELLER_FULFILLED") }
    var payoutPreference by remember { mutableStateOf("BANK") }

    fun load() = scope.launch {
        loading = true
        try {
            val o = api.sellerOnboarding()
            businessName=o.optString("business_name")
            businessType=o.optString("business_type")
            businessCategory=o.optString("business_category")
            businessEmail=o.optString("business_email")
            pan=o.optString("pan"); gstin=o.optString("gstin")
            gstRegistered=o.optString("gst_status","NOT_REGISTERED")=="REGISTERED"
            accountHolder=o.optString("account_holder_name")
            ifsc=o.optString("ifsc"); pickupAddress=o.optString("pickup_address")
            pickupCity=o.optString("pickup_city"); pickupState=o.optString("pickup_state")
            pickupPostal=o.optString("pickup_postal_code")
            returnDays=o.optInt("return_window_days",7).toString()
            shippingModel=o.optString("shipping_model","SELLER_FULFILLED")
            payoutPreference=o.optString("payout_preference","BANK")
            message="Status: "+o.optString("onboarding_status","DRAFT")
        } catch (e: Exception) {
            message=e.message ?: "Unable to load seller onboarding."
        } finally { loading=false }
    }
    LaunchedEffect(Unit) { load() }

    fun save(submit: Boolean) = scope.launch {
        saving=true; message=""
        try {
            val payload=JSONObject()
                .put("businessName",businessName).put("businessType",businessType)
                .put("businessCategory",businessCategory).put("businessEmail",businessEmail)
                .put("pan",pan).put("gstStatus",if(gstRegistered) "REGISTERED" else "NOT_REGISTERED").put("gst",if(gstRegistered) gstin else "").put("accountHolderName",accountHolder)
                .put("bankAccountNumber",bankAccount).put("ifsc",ifsc)
                .put("payoutPreference",payoutPreference).put("pickupAddress",pickupAddress)
                .put("pickupCity",pickupCity).put("pickupState",pickupState)
                .put("pickupPostalCode",pickupPostal)
                .put("returnWindowDays",returnDays.toIntOrNull() ?: -1)
                .put("shippingModel",shippingModel).put("submit",submit)
            val result=api.saveSellerOnboarding(payload)
            val status=result.optJSONObject("onboarding")?.optString("onboarding_status","DRAFT") ?: "DRAFT"
            message=(if(submit) "Onboarding submitted for admin review." else "Seller onboarding saved.")+" Status: "+status
        } catch(e: Exception) {
            message=e.message ?: "Unable to save seller onboarding."
        } finally { saving=false }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("Seller Business Onboarding", style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold)
        Text("Complete store, tax, pickup and return details. Admin approval is required before seller verification.")
        if(loading) Text("Loading saved onboarding…")

        OutlinedTextField(businessName,{businessName=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("Business name")})
        OutlinedTextField(businessType,{businessType=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("Business type")})
        OutlinedTextField(businessCategory,{businessCategory=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("Business category")})
        OutlinedTextField(businessEmail,{businessEmail=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("Business email (optional)")})
        OutlinedTextField(pan,{pan=it.uppercase()},Modifier.fillMaxWidth(),singleLine=true,label={Text("PAN")})
        Text("GST Status", fontWeight=FontWeight.SemiBold)
        Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) { RadioButton(selected=gstRegistered,onClick={gstRegistered=true}); Text("GST Registered") }
        Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) { RadioButton(selected=!gstRegistered,onClick={gstRegistered=false; gstin=""}); Text("No GST / Not Registered") }
        if(gstRegistered) OutlinedTextField(gstin,{gstin=it.uppercase()},Modifier.fillMaxWidth(),singleLine=true,label={Text("GSTIN")})
        OutlinedTextField(accountHolder,{accountHolder=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("Account holder")})
        OutlinedTextField(bankAccount,{bankAccount=it.filter(Char::isDigit)},Modifier.fillMaxWidth(),singleLine=true,label={Text("Bank account number")})
        OutlinedTextField(ifsc,{ifsc=it.uppercase()},Modifier.fillMaxWidth(),singleLine=true,label={Text("IFSC code")})
        OutlinedTextField(pickupAddress,{pickupAddress=it},Modifier.fillMaxWidth(),label={Text("Pickup address")})
        OutlinedTextField(pickupCity,{pickupCity=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("Pickup city")})
        OutlinedTextField(pickupState,{pickupState=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("Pickup state")})
        OutlinedTextField(pickupPostal,{pickupPostal=it.filter(Char::isDigit).take(6)},Modifier.fillMaxWidth(),singleLine=true,label={Text("Pickup pincode")})
        OutlinedTextField(returnDays,{returnDays=it.filter(Char::isDigit).take(2)},Modifier.fillMaxWidth(),singleLine=true,label={Text("Return window (days)")})
        OutlinedTextField(shippingModel,{shippingModel=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("Shipping model")})
        OutlinedTextField(payoutPreference,{payoutPreference=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("Payout preference")})

        Button(onClick={save(false)},enabled=!saving&&!loading,modifier=Modifier.fillMaxWidth()){Text(if(saving)"Saving…" else "Save Draft")}
        Button(onClick={save(true)},enabled=!saving&&!loading,modifier=Modifier.fillMaxWidth()){Text("Submit for Admin Review")}
        Button(onClick=onDone,enabled=!saving,modifier=Modifier.fillMaxWidth()){Text("Back")}
        if(message.isNotBlank()) Text(message,color=MaterialTheme.colorScheme.primary)
    }
}
