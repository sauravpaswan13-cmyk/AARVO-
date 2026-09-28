package com.aarvo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import com.aarvo.network.AarvoApiClient
import com.aarvo.ui.theme.AarvoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class RiderDashboardActivity:ComponentActivity(){
    private val notificationPermissionLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
    override fun onCreate(b:Bundle?){
        super.onCreate(b)
        createNotificationChannel()
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        val p=getSharedPreferences("aarvo_prefs",Context.MODE_PRIVATE)
        val api=AarvoApiClient{p.getString("auth_token",null)}
        setContent{AarvoTheme{RiderScreen(api,::finish)}}
    }
    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT>=26){
            val channel=NotificationChannel("aarvo_rider","Rider delivery alerts",NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    fun showRiderNotification(id:String,title:String,body:String){
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        val intent=Intent(this,RiderDashboardActivity::class.java)
        val pending=PendingIntent.getActivity(this,id.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification=NotificationCompat.Builder(this,"aarvo_rider")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title.ifBlank{"AARVO Rider"})
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        getSystemService(NotificationManager::class.java).notify(id.hashCode(),notification)
    }
}

@Composable private fun RiderScreen(api:AarvoApiClient,onBack:()->Unit){
    var jobs by remember{mutableStateOf<List<JSONObject>>(emptyList())}
    var notes by remember{mutableStateOf<List<JSONObject>>(emptyList())}
    var msg by remember{mutableStateOf("")}
    val scope=rememberCoroutineScope()
    val activity=androidx.compose.ui.platform.LocalContext.current as RiderDashboardActivity
    val prefs=activity.getSharedPreferences("aarvo_rider_notifications",Context.MODE_PRIVATE)
    fun reload(){
        scope.launch{
            try{
                val freshNotes=jsonArrayToList(api.riderNotifications())
                jobs=jsonArrayToList(api.riderAssignments())
                notes=freshNotes
                val known=prefs.getStringSet("shown_ids",emptySet())?.toMutableSet() ?: mutableSetOf()
                freshNotes.take(20).forEach{n->
                    val id=n.optString("id")
                    if(id.isNotBlank() && !known.contains(id)){
                        if(!n.optBoolean("read_at",false)) activity.showRiderNotification(id,n.optString("title"),n.optString("body"))
                        known.add(id)
                    }
                }
                prefs.edit().putStringSet("shown_ids",known.toList().takeLast(100).toSet()).apply()
            }catch(t:Throwable){msg=t.message?: "Unable to load rider jobs"}
        }
    }
    LaunchedEffect(Unit){
        reload()
        while(true){delay(15000);reload()}
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Text("AARVO Rider",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Assigned delivery jobs")}
        item{Text("Notifications",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
        items(notes){n->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(n.optString("title"),fontWeight=FontWeight.Bold);Text(n.optString("body"))}}}
        item{Text("My jobs",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
        items(jobs){a->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){
            Text("Order #"+a.optString("order_id"),fontWeight=FontWeight.Bold)
            Text("Status: "+a.optString("status"))
            val seller=a.optString("seller_name").ifBlank{"Seller"}
            Text("Pickup from: "+seller)
            Text("Pickup address: "+listOf(a.optString("pickup_address"),a.optString("pickup_city"),a.optString("pickup_state"),a.optString("pickup_postal_code")).filter{it.isNotBlank()}.joinToString(", "))
            Text("Order value: ₹"+(a.optLong("total_paise")/100)+"."+(a.optLong("total_paise")%100).toString().padStart(2,'0'))
            val address=a.optJSONObject("address_json")
            Text("Deliver to: "+(address?.optString("fullName").orEmpty().ifBlank{"Customer"}))
            Text("Address: "+listOf(address?.optString("line1").orEmpty(),address?.optString("city").orEmpty(),address?.optString("state").orEmpty(),address?.optString("postalCode").orEmpty()).filter{it.isNotBlank()}.joinToString(", "))
            val next=when(a.optString("status")){"ASSIGNED"->"ACCEPTED";"ACCEPTED"->"PICKED_UP";"PICKED_UP"->"OUT_FOR_DELIVERY";"OUT_FOR_DELIVERY"->"DELIVERED";else->""}
            if(next.isNotBlank())Button(onClick={scope.launch{try{api.riderUpdateAssignment(a.optString("id"),next);reload()}catch(t:Throwable){msg="Delivery update failed"}}}){Text("Mark "+next)}
        }}}
        item{if(msg.isNotBlank())Text(msg,color=MaterialTheme.colorScheme.error)}
        item{Button(onClick=onBack,modifier=Modifier.fillMaxWidth()){Text("Close")}}
    }
}
private fun jsonArrayToList(a:org.json.JSONArray):List<JSONObject> = buildList { for(i in 0 until a.length()) add(a.getJSONObject(i)) }
