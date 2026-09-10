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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.aarvo.network.AdminAuthApi
import kotlinx.coroutines.launch

class AdminLoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AdminLoginScreen() }
    }

    @Composable
    private fun AdminLoginScreen() {
        val scope = rememberCoroutineScope()
        val api = remember { AdminAuthApi() }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf("") }

        fun login() {
            if (loading) return
            error = ""
            loading = true
            scope.launch {
                try {
                    val session = api.login(email, password)
                    val token = session.optString("token").trim()
                    val role = session.optJSONObject("user")?.optString("role", "")?.uppercase().orEmpty()
                    if (token.isBlank() || role != "ADMIN") throw IllegalStateException("Admin authorization failed.")
                    getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE).edit()
                        .putBoolean("signed_in", true)
                        .putBoolean("guest_mode", false)
                        .putString("user_name", session.optJSONObject("user")?.optString("display_name", "AARVO Admin"))
                        .putString("user_role", "ADMIN")
                        .putString("auth_token", token)
                        .apply()
                    startActivity(Intent(this@AdminLoginActivity, AdminDashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                } catch (t: Throwable) {
                    error = t.message ?: "Admin login failed."
                } finally {
                    loading = false
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("AARVO Owner Admin", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("Private administrator access. Buyer/Seller OTP cannot create an ADMIN account.")
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Admin email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Admin password") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = ::login, enabled = !loading && email.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                if (loading) CircularProgressIndicator() else Text("Sign in as Admin")
            }
            if (error.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
