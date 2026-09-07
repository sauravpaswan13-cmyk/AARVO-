package com.aarvo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aarvo.network.AarvoApiClient
import com.aarvo.ui.theme.AarvoTheme
import kotlinx.coroutines.launch
import org.json.JSONObject

class AddressBookActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("aarvo_prefs", MODE_PRIVATE)
        setContent { AarvoTheme { AddressBookScreen(AarvoApiClient { prefs.getString("auth_token", null) }, ::finish) } }
    }
}

@androidx.compose.runtime.Composable
private fun AddressBookScreen(api: AarvoApiClient, onBack: () -> Unit) {
    var addresses by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun reload() { scope.launch { loading = true; error = ""; try { val a = api.addresses(); addresses = buildList { for (i in 0 until a.length()) add(a.getJSONObject(i)) } } catch (t: Throwable) { error = t.message ?: "Unable to load addresses" } finally { loading = false } } }
    LaunchedEffect(Unit) { reload() }
    if (showAdd) AddressFormDialog(api, { showAdd = false; reload() })
    Scaffold(topBar = { TopAppBar(title = { Text("Delivery Addresses") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { inner ->
        LazyColumn(Modifier.fillMaxSize().padding(inner).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) { Text("Add new address") } }
            if (loading) item { CircularProgressIndicator() }
            if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }
            if (!loading && addresses.isEmpty()) item { Text("No saved delivery addresses yet.") }
            items(addresses, key = { it.optString("id") }) { address -> AddressCard(api, address, ::reload) }
        }
    }
}

@androidx.compose.runtime.Composable
private fun AddressCard(api: AarvoApiClient, address: JSONObject, reload: () -> Unit) {
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val id = address.optString("id")
    val isDefault = address.optBoolean("is_default", false)
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(if (isDefault) "Default address" else address.optString("label", "Delivery address"), style = MaterialTheme.typography.titleMedium)
        Text(address.optString("full_name")); Text(address.optString("phone")); Text(address.optString("line1")); address.optString("line2").takeIf { it.isNotBlank() }?.let { Text(it) }; Text("${address.optString("city")}, ${address.optString("state")} - ${address.optString("postal_code")}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!isDefault) TextButton(onClick = { scope.launch { busy = true; try { api.setDefaultAddress(id); reload() } finally { busy = false } } }, enabled = !busy) { Text("Set default") }
            TextButton(onClick = { scope.launch { busy = true; try { api.deleteAddress(id); reload() } finally { busy = false } } }, enabled = !busy) { Text("Delete") }
        }
    } }
}

@androidx.compose.runtime.Composable
private fun AddressFormDialog(api: AarvoApiClient, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var line1 by remember { mutableStateOf("") }; var city by remember { mutableStateOf("") }; var state by remember { mutableStateOf("") }; var pin by remember { mutableStateOf("") }; var label by remember { mutableStateOf("Home") }; var error by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!busy) onDone() }, title = { Text("Add delivery address") }, text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("Full name") }); OutlinedTextField(phone, { phone = it }, label = { Text("10-digit mobile") }); OutlinedTextField(label, { label = it }, label = { Text("Label") }); OutlinedTextField(line1, { line1 = it }, label = { Text("Address") }); OutlinedTextField(city, { city = it }, label = { Text("City") }); OutlinedTextField(state, { state = it }, label = { Text("State") }); OutlinedTextField(pin, { pin = it }, label = { Text("PIN code") }); if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
    } }, confirmButton = { Button(onClick = { scope.launch { busy = true; error = ""; try { api.addAddress(JSONObject().put("label", label.trim()).put("fullName", name.trim()).put("phone", phone.trim()).put("line1", line1.trim()).put("line2", "").put("city", city.trim()).put("state", state.trim()).put("postalCode", pin.trim()).put("country", "IN"), false); onDone() } catch (t: Throwable) { error = t.message ?: "Unable to save address" } finally { busy = false } } }, enabled = !busy && name.isNotBlank() && phone.trim().length == 10 && line1.isNotBlank() && city.isNotBlank() && state.isNotBlank() && pin.trim().matches(Regex("^[0-9]{6}$"))) { if (busy) CircularProgressIndicator() else Text("Save address") } }, dismissButton = { TextButton(onClick = onDone, enabled = !busy) { Text("Cancel") } })
}
