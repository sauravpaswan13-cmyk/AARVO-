package com.aarvo

import android.content.Context
import android.os.Bundle
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aarvo.data.Product
import com.aarvo.network.AarvoApiClient
import com.aarvo.ui.theme.AarvoTheme
import kotlinx.coroutines.launch

class AarvoAiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AarvoTheme { AiShoppingScreen(applicationContext) { finish() } } }
    }
}

@Composable
private fun AiShoppingScreen(context: Context, onClose: () -> Unit) {
    val prefs = remember { context.getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE) }
    val api = remember { AarvoApiClient { prefs.getString("auth_token", null) } }
    var input by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("Namaste! Main AARVO AI hoon. Product, budget, comparison ya order/support ke baare mein poochiye.") }
    var suggestions by remember { mutableStateOf<List<Product>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("AARVO AI", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            TextButton(onClick = onClose) { Text("Close") }
        }
        Spacer(Modifier.height(8.dp))
        Text("Smart shopping • Search • Compare • Reviews • Deals • Support", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, label = { Text("Try: ₹20,000 ke andar best phone dikhao") })
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                loading = true
                error = ""
                scope.launch {
                    try {
                        val result = api.aiAssistant(input)
                        answer = result.optString("reply", "AARVO AI ne response diya.")
                        suggestions = result.optJSONArray("products")?.let { arr ->
                            buildList {
                                for (i in 0 until arr.length()) {
                                    val o = arr.optJSONObject(i) ?: continue
                                    add(Product(o.optInt("id"), o.optString("seller_id"), o.optString("seller_name"), o.optString("name"), o.optString("category"), (o.optLong("price_paise") / 100L).toInt(), o.optDouble("rating"), "", o.optString("description"), o.optInt("stock_quantity"), o.optBoolean("is_published", true), o.optLong("price_paise")))
                                }
                            }
                        } ?: emptyList()
                    } catch (t: Throwable) {
                        error = t.message ?: "AI service unavailable."
                    } finally {
                        loading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = input.isNotBlank() && !loading
        ) { if (loading) CircularProgressIndicator() else Text("Ask AARVO AI") }
        Spacer(Modifier.height(14.dp))
        Card(Modifier.fillMaxWidth()) { Text(answer, Modifier.padding(16.dp)) }
        if (error.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(error, color = MaterialTheme.colorScheme.error) }
        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(12.dp)); Text("AI Suggestions", fontWeight = FontWeight.Bold)
            LazyColumn { items(suggestions.size) { index -> val p = suggestions[index]; Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) { Column(Modifier.padding(12.dp)) { Text(p.name, fontWeight = FontWeight.Bold); Text(p.displayPrice + " • ⭐ " + p.rating); Text(p.category) } } } }
        }
    }
}
