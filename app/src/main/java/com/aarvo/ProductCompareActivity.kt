package com.aarvo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aarvo.network.AarvoApiClient
import com.aarvo.ui.theme.AarvoTheme
import org.json.JSONObject
import kotlinx.coroutines.launch

class ProductCompareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AarvoTheme { CompareScreen(applicationContext) { finish() } } }
    }
}

@Composable
private fun CompareScreen(context: Context, onClose: () -> Unit) {
    val prefs = remember { context.getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE) }
    val api = remember { AarvoApiClient { prefs.getString("auth_token", null) } }
    val ids = remember { intentIds(context).distinct().filter { it > 0 }.take(3) }
    var result by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(ids) {
        if (ids.size < 2) { error = "Select at least 2 products to compare."; loading = false }
        else scope.launch {
            try { result = api.compareProducts(ids) }
            catch (t: Throwable) { error = t.message ?: "Unable to compare products." }
            finally { loading = false }
        }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Compare Products") },
            navigationIcon = { TextButton(onClick = onClose) { Text("Back") } })
    }) { inner ->
        LazyColumn(Modifier.fillMaxSize().padding(inner).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("AARVO Comparison", style = MaterialTheme.typography.headlineSmall); Text("Price • Rating • Stock") }
            if (loading) item { CircularProgressIndicator() }
            if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }
            result?.let { data ->
                item { Text(data.optString("summary", "Product comparison"), style = MaterialTheme.typography.titleMedium) }
                val rows = data.optJSONArray("rows")
                if (rows != null) for (i in 0 until rows.length()) {
                    val row = rows.optJSONObject(i) ?: continue
                    item {
                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                            Text(row.optString("name", "Product"), style = MaterialTheme.typography.titleLarge)
                            Text("Price: " + row.optString("price", "—"))
                            Text("Rating: " + row.optDouble("rating", 0.0) + " ⭐")
                            Text("Stock: " + row.optInt("stock", 0))
                        } }
                    }
                }
            }
        }
    }
}

private fun intentIds(context: Context): List<Int> =
    context.getSharedPreferences("aarvo_compare", Context.MODE_PRIVATE)
        .getString("product_ids", "").orEmpty()
        .split(",").mapNotNull { it.trim().toIntOrNull() }
