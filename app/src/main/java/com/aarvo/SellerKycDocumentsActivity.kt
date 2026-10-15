package com.aarvo

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aarvo.network.AarvoApiClient
import com.aarvo.ui.theme.AarvoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SellerKycDocumentsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE)
        val api = AarvoApiClient { prefs.getString("auth_token", null) }
        setContent { AarvoTheme { SellerKycDocumentsScreen(this, api, ::finish) } }
    }
}

private data class KycDocument(val type: String, val filename: String, val status: String, val note: String)

@Composable
private fun SellerKycDocumentsScreen(context: Context, api: AarvoApiClient, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var documentType by remember { mutableStateOf("PAN") }
    var documents by remember { mutableStateOf<List<KycDocument>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    suspend fun refresh() {
        documents = withContext(Dispatchers.IO) {
            val arr = api.sellerKycDocuments()
            (0 until arr.length()).map { i ->
                val d = arr.optJSONObject(i) ?: JSONObject()
                KycDocument(
                    d.optString("document_type", "OTHER"),
                    d.optString("original_filename", "Document"),
                    d.optString("status", "SUBMITTED"),
                    d.optString("review_note", "")
                )
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            busy = true
            message = ""
            try {
                val mime = context.contentResolver.getType(uri)?.lowercase()
                    ?: error("File type could not be identified. Please select a PDF, JPG or PNG.")
                if (mime !in setOf("application/pdf", "image/jpeg", "image/png")) error("Only PDF, JPG and PNG files are supported.")
                val filename = queryDisplayName(context, uri)
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val out = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        var total = 0
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            total += n
                            if (total > 5 * 1024 * 1024) error("File size must be 5 MB or less.")
                            out.write(buffer, 0, n)
                        }
                        out.toByteArray()
                    } ?: error("Could not read the selected file.")
                }
                if (bytes.isEmpty()) error("Selected file is empty.")
                val ticket = api.presignSellerKycDocument(filename, mime, bytes.size.toLong(), documentType)
                val uploadUrl = ticket.optString("uploadUrl")
                if (uploadUrl.isBlank()) error("Secure upload link was not returned by the server.")
                val requiredHeaders = ticket.optJSONObject("requiredHeaders")
                    ?: JSONObject().put("Content-Type", mime)
                val putBuilder = Request.Builder().url(uploadUrl)
                val headerKeys = requiredHeaders.keys()
                while (headerKeys.hasNext()) {
                    val header = headerKeys.next()
                    putBuilder.header(header, requiredHeaders.optString(header))
                }
                val putRequest = putBuilder
                    .put(bytes.toRequestBody(mime.toMediaType()))
                    .build()
                val uploadResult = withContext(Dispatchers.IO) {
                    OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
                        .newCall(putRequest).execute().use { response ->
                            if (!response.isSuccessful) error("Private storage upload failed (HTTP ${response.code}). Please retry.")
                            true
                        }
                }
                if (uploadResult) {
                    api.confirmSellerKycDocument(
                        ticket.optString("documentId"), ticket.optString("objectKey"),
                        filename, mime, documentType
                    )
                    refresh()
                    message = "Document uploaded securely. Status: SUBMITTED for admin review."
                }
            } catch (e: Exception) {
                message = e.message ?: "Upload failed. Please try again."
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(Unit) {
        busy = true
        try { refresh() } catch (e: Exception) { message = e.message ?: "Could not load KYC documents." }
        finally { busy = false }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Seller KYC Documents", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Upload documents securely for admin review. Supported formats: PDF, JPG, PNG. Maximum size: 5 MB per file.")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select document type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                listOf("PAN", "ADDRESS_PROOF", "BUSINESS_PROOF", "OTHER").forEach { type ->
                    if (documentType == type) Button(onClick = { documentType = type }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(type.toDisplayLabel()) }
                    else OutlinedButton(onClick = { documentType = type }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(type.toDisplayLabel()) }
                }
                Button(onClick = { picker.launch(arrayOf("application/pdf", "image/jpeg", "image/png")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(if (busy) "Please wait…" else "Choose file and upload")
                }
                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            message = ""
                            try {
                                val result = api.submitSellerKycDocuments()
                                refresh()
                                val count = result.optInt("documentCount", documents.size)
                                message = "KYC submitted successfully. $count document(s) sent for admin review."
                            } catch (e: Exception) {
                                message = if (e.message?.contains("NO_KYC_DOCUMENTS_UPLOADED") == true) {
                                    "Submit karne se pehle kam se kam ek KYC document upload kijiye."
                                } else {
                                    e.message ?: "KYC submission failed. Please retry."
                                }
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (busy) "Submitting…" else "Save & Submit for Admin Review") }
            }
        }
        Text("Uploaded documents", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (documents.isEmpty()) Text(if (busy) "Loading documents…" else "No documents uploaded yet.")
        documents.forEach { doc ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(doc.type.toDisplayLabel(), fontWeight = FontWeight.Bold)
                    Text(doc.filename, style = MaterialTheme.typography.bodyMedium)
                    Text("Status: ${doc.status}", color = when (doc.status) {
                        "ACCEPTED" -> MaterialTheme.colorScheme.primary
                        "REJECTED" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.secondary
                    })
                    if (doc.note.isNotBlank()) Text("Admin note: ${doc.note}")
                }
            }
        }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { scope.launch { busy = true; try { refresh(); message = "Document list refreshed." } catch (e: Exception) { message = e.message ?: "Refresh failed." } finally { busy = false } } }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Refresh") }
            Button(onClick = onBack, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Back") }
        }
    }
}

private fun String.toDisplayLabel(): String = when (this) {
    "PAN" -> "PAN Card"
    "ADDRESS_PROOF" -> "Address Proof"
    "BUSINESS_PROOF" -> "Business Proof"
    else -> "Other Document"
}

private fun queryDisplayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) return cursor.getString(index) ?: "document"
        }
    }
    return "document"
}
