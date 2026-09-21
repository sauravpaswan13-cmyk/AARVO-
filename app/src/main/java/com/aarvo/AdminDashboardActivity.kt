package com.aarvo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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
import org.json.JSONArray
import org.json.JSONObject

class AdminDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE)
        val api = AarvoApiClient { prefs.getString("auth_token", null) }
        if (prefs.getString("user_role", "")?.uppercase() != "ADMIN") { finish(); return }
        setContent { AarvoTheme { AdminScreen(api, ::finish) } }
    }
}

@Composable
private fun AdminScreen(api: AarvoApiClient, onBack: () -> Unit) {
    var sellers by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var riders by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var orders by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var issues by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var onboarding by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    var selectedRiderId by remember { mutableStateOf("") }
    var riderName by remember { mutableStateOf("") }
    var riderPhone by remember { mutableStateOf("") }
    var riderPassword by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            try {
                sellers = jsonArrayToList(api.adminSellers())
                riders = jsonArrayToList(api.adminRiders())
                orders = jsonArrayToList(api.adminOrders())
                issues = jsonArrayToList(api.adminDisputes())
                onboarding = jsonArrayToList(api.adminSellerOnboarding("SUBMITTED"))
                message = ""
            } catch (t: Throwable) { message = t.message ?: "Admin data unavailable" }
        }
    }
    LaunchedEffect(Unit) { reload() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("AARVO Admin Control Center", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Seller • Order • Rider • Issue management")
        }
        item { Text("Create delivery rider", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item { OutlinedTextField(riderName, { riderName = it }, Modifier.fillMaxWidth(), label = { Text("Rider name") }, singleLine = true) }
        item { OutlinedTextField(riderPhone, { riderPhone = it.filter(Char::isDigit).take(10) }, Modifier.fillMaxWidth(), label = { Text("Mobile") }, singleLine = true) }
        item { OutlinedTextField(riderPassword, { riderPassword = it }, Modifier.fillMaxWidth(), label = { Text("Password (8+)") }, singleLine = true) }
        item {
            Button(
                onClick = {
                    scope.launch {
                        try { api.adminCreateRider(riderName, riderPhone, riderPassword); message = "Rider created"; riderName = ""; riderPhone = ""; riderPassword = ""; reload() }
                        catch (t: Throwable) { message = t.message ?: "Create failed" }
                    }
                },
                enabled = riderName.isNotBlank() && riderPhone.length == 10 && riderPassword.length >= 8,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create Rider") }
        }
        if (message.isNotBlank()) item { Text(message, color = MaterialTheme.colorScheme.error) }

        item { Text("Seller Onboarding: ${onboarding.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(onboarding, key = { it.optString("seller_id") }) { seller ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(seller.optString("business_name"), fontWeight = FontWeight.Bold)
                    Text("${seller.optString("display_name")} • ${seller.optString("phone")}")
                    Text("${seller.optString("business_category")} • ${seller.optString("pickup_city")}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { scope.launch { try { api.adminReviewSellerOnboarding(seller.optString("seller_id"), "APPROVE"); message = "Seller onboarding approved"; reload() } catch (t: Throwable) { message = t.message ?: "Approval failed" } } }) { Text("Approve") }
                        OutlinedButton(onClick = { scope.launch { try { api.adminReviewSellerOnboarding(seller.optString("seller_id"), "REJECT", "Admin requested corrections"); message = "Seller onboarding rejected"; reload() } catch (t: Throwable) { message = t.message ?: "Rejection failed" } } }) { Text("Reject") }
                    }
                }
            }
        }

        item { Text("Sellers: ${sellers.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(sellers, key = { it.optString("seller_id") }) { seller ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(seller.optString("display_name"), fontWeight = FontWeight.Bold)
                    Text(seller.optString("phone"))
                    if (!seller.optBoolean("verified")) TextButton(onClick = { scope.launch { try { api.adminVerifySeller(seller.optString("seller_id"), true); reload() } catch (t: Throwable) { message = t.message ?: "Seller verification failed" } } }) { Text("Verify seller") }
                    else Text("Verified")
                }
            }
        }

        item { Text("Riders: ${riders.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(riders, key = { it.optString("rider_id") }) { rider ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text(rider.optString("display_name"), fontWeight = FontWeight.Bold); Text(rider.optString("phone")) } }
        }

        item { Text("Orders: ${orders.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(orders, key = { it.optString("id") }) { order ->
            val orderId = order.optString("id")
            val status = order.optString("status")
            val assignable = status == "PAID" || status == "PACKED" || status == "SHIPPED"
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Order #$orderId", fontWeight = FontWeight.Bold)
                    Text("$status • ₹${order.optLong("total_paise") / 100}")
                    if (order.optString("payment_status") == "CAPTURED") {
                        OutlinedButton(onClick = { scope.launch { try { api.adminRefundOrder(orderId); message = "Refund processed"; reload() } catch (t: Throwable) { message = "Refund failed: ${t.message ?: "unknown error"}" } } }) { Text("Full Refund") }
                    }
                    if (riders.isNotEmpty()) {
                        var expanded by remember(orderId) { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expanded = true }, enabled = assignable) { Text(if (selectedRiderId.isBlank()) "Choose rider" else "Rider selected") }
                            DropdownMenu(expanded, { expanded = false }) {
                                riders.forEach { rider ->
                                    DropdownMenuItem(
                                        text = { Text("${rider.optString("display_name")} • ${rider.optString("phone")}") },
                                        onClick = { selectedRiderId = rider.optString("rider_id"); expanded = false }
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = { scope.launch { try { api.adminAssignRider(orderId, selectedRiderId); message = "Rider assigned"; reload() } catch (t: Throwable) { message = t.message ?: "Assignment failed" } } },
                            enabled = selectedRiderId.isNotBlank() && assignable
                        ) { Text("Assign rider") }
                    }
                }
            }
        }

        item { Text("Issues: ${issues.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(issues, key = { it.optString("id") }) { dispute ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Order #${dispute.optString("order_id")}", fontWeight = FontWeight.Bold)
                    Text("${dispute.optString("reason")} • ${dispute.optString("status")}")
                    if (dispute.optString("status") == "OPEN") TextButton(onClick = { scope.launch { try { api.adminResolveDispute(dispute.optString("id"), "UNDER_REVIEW", "Admin review started"); reload() } catch (t: Throwable) { message = t.message ?: "Issue update failed" } } }) { Text("Review") }
                }
            }
        }
        item { Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Continue") } }
    }
}

private fun jsonArrayToList(array: JSONArray): List<JSONObject> = buildList {
    for (i in 0 until array.length()) add(array.getJSONObject(i))
}
