package com.aarvo
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aarvo.network.AarvoApiClient
import com.aarvo.ui.theme.AarvoTheme
import kotlinx.coroutines.launch
import org.json.JSONObject
class RiderDashboardActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);val p=getSharedPreferences("aarvo_prefs",Context.MODE_PRIVATE);val api=AarvoApiClient{p.getString("auth_token",null)};setContent{AarvoTheme{RiderScreen(api,::finish)}}}}
@Composable private fun RiderScreen(api:AarvoApiClient,onBack:()->Unit){var jobs by remember{mutableStateOf<List<JSONObject>>(emptyList())};var notes by remember{mutableStateOf<List<JSONObject>>(emptyList())};var msg by remember{mutableStateOf("")};val scope=rememberCoroutineScope();fun reload(){scope.launch{try{jobs=jsonArrayToList(api.riderAssignments());notes=jsonArrayToList(api.riderNotifications())}catch(t:Throwable){msg=t.message?:"Unable to load rider jobs"}}};LaunchedEffect(Unit){reload()};LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text("AARVO Rider",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Assigned delivery jobs",style=MaterialTheme.typography.bodyMedium)}Button(onClick=onBack){Icon(Icons.Default.ArrowBack,"Back");Spacer(Modifier.width(4.dp));Text("Close")}}};item{Text("Notifications",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)};items(notes){n->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(n.optString("title"),fontWeight=FontWeight.Bold);Text(n.optString("body"))}}};item{Text("My jobs",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)};items(jobs){a->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text("Order #"+a.optString("order_id"),fontWeight=FontWeight.Bold);Text("Status: "+a.optString("status"));Text("Order value: ₹"+(a.optLong("total_paise")/100)+"."+(a.optLong("total_paise")%100).toString().padStart(2,'0'));val address=a.optJSONObject("address_json");Text("Deliver to: "+(address?.optString("fullName").orEmpty().ifBlank{"Customer"}));Text("Address: "+listOf(address?.optString("line1").orEmpty(),address?.optString("city").orEmpty(),address?.optString("state").orEmpty(),address?.optString("postalCode").orEmpty()).filter{it.isNotBlank()}.joinToString(", "));val next=when(a.optString("status")){"ASSIGNED"->"ACCEPTED";"ACCEPTED"->"PICKED_UP";"PICKED_UP"->"OUT_FOR_DELIVERY";"OUT_FOR_DELIVERY"->"DELIVERED";else->""};if(next.isNotBlank())Button(onClick={scope.launch{try{api.riderUpdateAssignment(a.optString("id"),next);reload()}catch(t:Throwable){msg="Delivery update failed"}}}){Text("Mark "+next)}}}};item{if(msg.isNotBlank())Text(msg,color=MaterialTheme.colorScheme.error)};item{Button(onClick=onBack,modifier=Modifier.fillMaxWidth()){Text("Close")}}}}

private fun jsonArrayToList(a:org.json.JSONArray):List<JSONObject> = buildList { for(i in 0 until a.length()) add(a.getJSONObject(i)) }
